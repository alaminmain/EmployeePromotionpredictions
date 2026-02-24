using Microsoft.Data.Sqlite;
using PromotionAPI.Services;
using Serilog;
using Serilog.Events;

Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Debug()
    .MinimumLevel.Override("Microsoft", LogEventLevel.Information)
    .Enrich.FromLogContext()
    .WriteTo.Console()
    .WriteTo.File(Path.Combine(AppContext.BaseDirectory, "Logs/promotion_api-.txt"), rollingInterval: RollingInterval.Day)
    .CreateLogger();

try
{
    Log.Information("Starting web application");
    var builder = WebApplication.CreateBuilder(args);

    // Use serilog for host logging
    builder.Host.UseSerilog();

    // Add CORS for React frontend
    builder.Services.AddCors(options =>
    {
        options.AddDefaultPolicy(policy =>
        {
            policy.AllowAnyOrigin()
                  .AllowAnyMethod()
                  .AllowAnyHeader();
        });
    });

    var app = builder.Build();

    app.UseCors();

    // Database path (relative to parent directory)
    // Database Path and CSV Files - Load relative to the execution directory
    var baseDir = AppContext.BaseDirectory;
    var dbPath = Path.Combine(baseDir, "Promotion.db");
    var empCsv = Path.Combine(baseDir, "Files", "EmpList.csv");
    var orgCsv = Path.Combine(baseDir, "Files", "Final_Complete_Master_List.csv");
    var empSlCsv = Path.Combine(baseDir, "Files", "EmpListSL.csv");

    // Ensure database and Predictions table exist before any requests
    try
    {
        using (var conn = new SqliteConnection($"Data Source={dbPath}"))
        {
            Log.Information("Initializing database at path: {DbPath}", dbPath);
            conn.Open();
            var sql = @"
            CREATE TABLE IF NOT EXISTS Predictions (
                Id INTEGER PRIMARY KEY AUTOINCREMENT,
                EmpId TEXT,
                Name TEXT,
                FromGrade TEXT,
                ToGrade TEXT,
                NewDesignation TEXT,
                PredictedDate TEXT,
                CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX IF NOT EXISTS idx_empid ON Predictions(EmpId);
            CREATE INDEX IF NOT EXISTS idx_date ON Predictions(PredictedDate);";
            using var cmd = new SqliteCommand(sql, conn);
            cmd.ExecuteNonQuery();
            Log.Information("Database initialization completed successfully.");
        }
    }
    catch (Exception ex)
    {
        Log.Fatal(ex, "Failed to initialize the database!");
    }

    // =====================================================
    // API ENDPOINTS
    // =====================================================

    // GET /api/employees - All active employees
    app.MapGet("/api/employees", () =>
    {
        try
        {
            var employees = new List<object>();

            using var conn = new SqliteConnection($"Data Source={dbPath}");
            conn.Open();

            var sql = @"SELECT EmpId, Name, MIN(FromGrade) as FromGrade 
                    FROM Predictions 
                    GROUP BY EmpId, Name
                    ORDER BY Name";

            using var cmd = new SqliteCommand(sql, conn);
            using var reader = cmd.ExecuteReader();

            while (reader.Read())
            {
                employees.Add(new
                {
                    EmpId = reader["EmpId"]?.ToString(),
                    Name = reader["Name"]?.ToString(),
                    CurrentGrade = reader["FromGrade"]?.ToString()
                });
            }

            return Results.Ok(employees);
        }
        catch (Exception ex)
        {
            Log.Error(ex, "Error occurred while fetching employees");
            return Results.Problem(ex.Message);
        }
    });

    // GET /api/employees/overview - All employees with grade progression (for mini-charts)
    app.MapGet("/api/employees/overview", () =>
    {
        var overview = new List<object>();

        using var conn = new SqliteConnection($"Data Source={dbPath}");
        conn.Open();

        // Select distinct employees and their grade history ordered by date
        var sql = @"SELECT EmpId, Name, GROUP_CONCAT(ToGrade, ',') as GradePath
                FROM (
                    SELECT EmpId, Name, ToGrade 
                    FROM Predictions 
                    ORDER BY EmpId, PredictedDate
                )
                GROUP BY EmpId, Name
                ORDER BY Name";

        using var cmd = new SqliteCommand(sql, conn);
        using var reader = cmd.ExecuteReader();

        while (reader.Read())
        {
            overview.Add(new
            {
                EmpId = reader["EmpId"]?.ToString(),
                Name = reader["Name"]?.ToString(),
                // Extract numbers from "G-1", "G-2" etc. for the chart
                GradePath = reader["GradePath"]?.ToString()?.Split(',')
                    .Select(g => int.TryParse(g.Replace("G-", ""), out int v) ? v : 0)
                    .Where(v => v > 0)
                    .ToList()
            });
        }

        return Results.Ok(overview);
    });

    // GET /api/employees/{id} - Single employee details
    app.MapGet("/api/employees/{id}", (string id) =>
    {
        using var conn = new SqliteConnection($"Data Source={dbPath}");
        conn.Open();

        var sql = @"SELECT EmpId, Name, FromGrade 
                FROM Predictions 
                WHERE EmpId = @id 
                ORDER BY PredictedDate 
                LIMIT 1";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@id", id);
        using var reader = cmd.ExecuteReader();

        if (reader.Read())
        {
            return Results.Ok(new
            {
                EmpId = reader["EmpId"]?.ToString(),
                Name = reader["Name"]?.ToString(),
                CurrentGrade = reader["FromGrade"]?.ToString()
            });
        }

        return Results.NotFound();
    });

    // GET /api/predictions/{empId} - Promotion predictions for employee
    app.MapGet("/api/predictions/{empId}", (string empId) =>
    {
        var predictions = new List<object>();

        using var conn = new SqliteConnection($"Data Source={dbPath}");
        conn.Open();

        var sql = @"SELECT Id, EmpId, Name, FromGrade, ToGrade, NewDesignation, PredictedDate 
                FROM Predictions 
                WHERE EmpId = @id 
                ORDER BY PredictedDate";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@id", empId);
        using var reader = cmd.ExecuteReader();

        int step = 1;
        while (reader.Read())
        {
            predictions.Add(new
            {
                Step = step++,
                EmpId = reader["EmpId"]?.ToString(),
                Name = reader["Name"]?.ToString(),
                FromGrade = reader["FromGrade"]?.ToString(),
                ToGrade = reader["ToGrade"]?.ToString(),
                NewDesignation = reader["NewDesignation"]?.ToString(),
                PredictedDate = reader["PredictedDate"]?.ToString()
            });
        }

        return Results.Ok(new { EmployeeId = empId, Predictions = predictions });
    });

    // GET /api/report/{empId}?date=2030-06-01 - Promotions by target date
    app.MapGet("/api/report/{empId}", (string empId, string? date) =>
    {
        var targetDate = string.IsNullOrEmpty(date) ? DateTime.MaxValue : DateTime.Parse(date);
        var predictions = new List<object>();
        string? employeeName = null;
        string? currentGrade = null;

        using var conn = new SqliteConnection($"Data Source={dbPath}");
        conn.Open();

        var sql = @"SELECT Id, EmpId, Name, FromGrade, ToGrade, NewDesignation, PredictedDate 
                FROM Predictions 
                WHERE EmpId = @id 
                ORDER BY PredictedDate";

        using var cmd = new SqliteCommand(sql, conn);
        cmd.Parameters.AddWithValue("@id", empId);
        using var reader = cmd.ExecuteReader();

        int step = 1;
        while (reader.Read())
        {
            var predDate = DateTime.Parse(reader["PredictedDate"]?.ToString() ?? "9999-12-31");
            if (predDate > targetDate) break;

            employeeName ??= reader["Name"]?.ToString();
            currentGrade ??= reader["FromGrade"]?.ToString();

            predictions.Add(new
            {
                Step = step++,
                FromGrade = reader["FromGrade"]?.ToString(),
                ToGrade = reader["ToGrade"]?.ToString(),
                NewDesignation = reader["NewDesignation"]?.ToString(),
                PredictedDate = reader["PredictedDate"]?.ToString()
            });
        }

        var finalPosition = predictions.LastOrDefault();

        return Results.Ok(new
        {
            EmployeeId = empId,
            EmployeeName = employeeName,
            CurrentGrade = currentGrade,
            TargetDate = date,
            TotalPromotions = predictions.Count,
            FinalPosition = finalPosition,
            PromotionPath = predictions
        });
    });

    // GET /api/reports/promotions-per-year - Count of promotions grouped by year
    app.MapGet("/api/reports/promotions-per-year", () =>
    {
        var report = new List<object>();

        using var conn = new SqliteConnection($"Data Source={dbPath}");
        conn.Open();

        var sql = @"SELECT substr(PredictedDate, 1, 4) as Year, COUNT(*) as PromotionCount 
                FROM Predictions 
                WHERE NewDesignation != 'Retirement'
                GROUP BY substr(PredictedDate, 1, 4)
                ORDER BY Year";

        using var cmd = new SqliteCommand(sql, conn);
        using var reader = cmd.ExecuteReader();

        while (reader.Read())
        {
            report.Add(new
            {
                Year = reader["Year"]?.ToString(),
                Count = Convert.ToInt32(reader["PromotionCount"])
            });
        }

        return Results.Ok(report);
    });

    // Health check
    app.MapGet("/", () => "Promotion API is running!");

    // Run Simulation
    app.MapPost("/api/simulation/run", () =>
    {
        try
        {
            Log.Information("Starting promotion prediction simulation...");
            var manager = new SimulationManager(dbPath, empCsv, orgCsv, empSlCsv);
            manager.Run();
            Log.Information("Promotion prediction simulation completed successfully.");
            return Results.Ok(new { message = "Simulation completed successfully" });
        }
        catch (Exception ex)
        {
            Log.Error(ex, "An error occurred during simulation execution.");
            return Results.Problem(ex.Message);
        }
    });

    Log.Information($"Database: {dbPath}");
    Log.Information("API Endpoints mapped successfully.");

    app.Run();
}
catch (Exception ex)
{
    Log.Fatal(ex, "Application terminated unexpectedly");
}
finally
{
    Log.CloseAndFlush();
}
