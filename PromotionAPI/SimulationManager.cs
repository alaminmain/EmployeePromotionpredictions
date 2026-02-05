using System.Globalization;
using CsvHelper;
using CsvHelper.Configuration;
using Microsoft.Data.Sqlite;

namespace PromotionAPI.Services
{
    public class Prediction
    {
        public string EmpId { get; set; } = "";
        public string Name { get; set; } = "";
        public string FromGrade { get; set; } = "";
        public string ToGrade { get; set; } = "";
        public string NewDesignation { get; set; } = "";
        public DateTime PredictedDate { get; set; }
    }

    public class Employee
    {
        public string EmpId { get; set; } = "";
        public string Name { get; set; } = "";
        public DateTime DOB { get; set; }
        public DateTime RetirementDate { get; set; }
        public DateTime JoiningDate { get; set; }
        public int GradeNo { get; set; }
        public int DesgNo { get; set; }
        public string DesignationName { get; set; } = "";
        public DateTime? LastPromotionDate { get; set; }
        public string Status { get; set; } = "";
        public int? SeniorId { get; set; }

        // Simulation state
        public int SimGradeNo { get; set; }
        public int SimDesgNo { get; set; }
        public int SimSL_No { get; set; }
        public string SimTrack { get; set; } = "GENERAL";
        public DateTime SimLastPromoDate { get; set; }
        public bool IsRetired { get; set; }
    }

    public class OrgPost
    {
        public int SL_No { get; set; }
        public int GradeNo { get; set; }
        public int DesgNo { get; set; }
        public string DesignationName { get; set; } = "";
        public string? Track { get; set; }
        public int TotalPost { get; set; }
        public string? YearsNeededRaw { get; set; }
        public string? FeederPostsRaw { get; set; }
        public string? PromotionQuotaRaw { get; set; }

        public int GetYearsRequired()
        {
            if (string.IsNullOrWhiteSpace(YearsNeededRaw)) return 0;
            var digits = new string(YearsNeededRaw.TakeWhile(c => char.IsDigit(c) || c == ' ').Where(char.IsDigit).ToArray());
            return int.TryParse(digits, out int val) ? val : 0;
        }

        public List<int> GetFeederPostIds()
        {
            if (string.IsNullOrWhiteSpace(FeederPostsRaw) || FeederPostsRaw == "N/A")
                return new List<int>();

            var cleaned = FeederPostsRaw.Replace(" and ", ";").Replace(",", ";");
            return cleaned.Split(';')
                .Select(s => s.Trim())
                .Where(s => int.TryParse(s, out _))
                .Select(int.Parse)
                .ToList();
        }

        public int GetPromotionQuota()
        {
            if (string.IsNullOrWhiteSpace(PromotionQuotaRaw)) return 0;
            var clean = PromotionQuotaRaw.Replace("%", "").Trim();
            if (int.TryParse(clean, out int val))
                return val > 100 ? 100 : val;
            return 0;
        }
    }

    public class EmployeeMap : ClassMap<Employee>
    {
        public EmployeeMap()
        {
            Map(m => m.EmpId).Name("emp_id");
            Map(m => m.Name).Name("emp_nm");
            Map(m => m.DOB).Name("dob").TypeConverterOption.Format("M/d/yyyy");
            Map(m => m.RetirementDate).Name("retr_dt").TypeConverterOption.Format("M/d/yyyy");
            Map(m => m.JoiningDate).Name("jjoin_date").TypeConverterOption.Format("M/d/yyyy");
            Map(m => m.GradeNo).Name("grade_no");
            Map(m => m.DesgNo).Name("desg_no");
            Map(m => m.DesignationName).Name("desg_nm");
            Map(m => m.LastPromotionDate).Name("lastpromotiondate").Optional().TypeConverterOption.Format("M/d/yyyy");
            Map(m => m.Status).Name("status");
            Map(m => m.SeniorId).Name("senior_id").Optional();
        }
    }

    public class OrgPostMap : ClassMap<OrgPost>
    {
        public OrgPostMap()
        {
            Map(m => m.SL_No).Name("SL_No");
            Map(m => m.GradeNo).Name("grade_no");
            Map(m => m.DesgNo).Name("desg_no");
            Map(m => m.DesignationName).Name("desg_nm");
            Map(m => m.Track).Name("IT_or_General");
            Map(m => m.TotalPost).Name("TotalPost");
            Map(m => m.YearsNeededRaw).Name("YearNeedtobepromoted");
            Map(m => m.FeederPostsRaw).Name("PromotionFromPostSL");
            Map(m => m.PromotionQuotaRaw).Name("PreviousPostPercentise");
        }
    }

    public class SimulationManager
    {
        private readonly string _dbPath;
        private readonly string _empCsv;
        private readonly string _orgCsv;

        public SimulationManager(string dbPath, string empCsv, string orgCsv)
        {
            _dbPath = dbPath;
            _empCsv = empCsv;
            _orgCsv = orgCsv;
        }

        public void Run()
        {
            var employees = LoadEmployees(_empCsv);
            var orgPosts = LoadOrgPosts(_orgCsv);

            var activeEmployees = employees
                .Where(e => e.Status == "Regular" || e.Status == "Prl")
                .ToList();

            InitializeEmployeeTrack(activeEmployees, orgPosts);
            SetupDatabase();

            var predictions = RunSimulation(activeEmployees, orgPosts);
            SavePredictions(predictions);
        }

        private List<Employee> LoadEmployees(string path)
        {
            var config = new CsvConfiguration(CultureInfo.InvariantCulture)
            {
                MissingFieldFound = null,
                HeaderValidated = null,
            };

            using var reader = new StreamReader(path);
            using var csv = new CsvReader(reader, config);
            csv.Context.RegisterClassMap<EmployeeMap>();

            var records = csv.GetRecords<Employee>().ToList();
            foreach (var emp in records)
            {
                emp.SimGradeNo = emp.GradeNo;
                emp.SimDesgNo = emp.DesgNo;
                emp.SimLastPromoDate = emp.LastPromotionDate ?? emp.JoiningDate;
                emp.IsRetired = false;
            }
            return records;
        }

        private List<OrgPost> LoadOrgPosts(string path)
        {
            var config = new CsvConfiguration(CultureInfo.InvariantCulture)
            {
                MissingFieldFound = null,
                HeaderValidated = null,
            };

            using var reader = new StreamReader(path);
            using var csv = new CsvReader(reader, config);
            csv.Context.RegisterClassMap<OrgPostMap>();
            return csv.GetRecords<OrgPost>().ToList();
        }

        private void InitializeEmployeeTrack(List<Employee> employees, List<OrgPost> orgPosts)
        {
            var postLookup = orgPosts.ToDictionary(p => (p.GradeNo, p.DesgNo), p => p);
            foreach (var emp in employees)
            {
                if (postLookup.TryGetValue((emp.GradeNo, emp.DesgNo), out var post))
                {
                    emp.SimSL_No = post.SL_No;
                    emp.SimTrack = post.Track?.ToUpper() == "IT" ? "IT" : "GENERAL";
                }
                else
                {
                    emp.SimSL_No = 999;
                    emp.SimTrack = "GENERAL";
                }
            }
        }

        private void SetupDatabase()
        {
            using var conn = new SqliteConnection($"Data Source={_dbPath}");
            conn.Open();
            string sql = @"
                DROP TABLE IF EXISTS Predictions;
                CREATE TABLE Predictions (
                    Id INTEGER PRIMARY KEY AUTOINCREMENT,
                    EmpId TEXT,
                    Name TEXT,
                    FromGrade TEXT,
                    ToGrade TEXT,
                    NewDesignation TEXT,
                    PredictedDate TEXT,
                    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP
                );
                CREATE INDEX idx_empid ON Predictions(EmpId);
                CREATE INDEX idx_date ON Predictions(PredictedDate);";
            using var cmd = new SqliteCommand(sql, conn);
            cmd.ExecuteNonQuery();
        }

        private List<Prediction> RunSimulation(List<Employee> employees, List<OrgPost> orgPosts)
        {
            var predictions = new List<Prediction>();
            int startYear = DateTime.Today.Year;
            if (DateTime.Today.Month > 6) startYear++;

            for (int year = startYear; year <= startYear + 40; year++)
            {
                var currentDate = new DateTime(year, 6, 1);

                // A. Retirements
                var retiringNow = employees.Where(e => !e.IsRetired && e.RetirementDate <= currentDate && e.RetirementDate > currentDate.AddYears(-1)).ToList();
                foreach (var emp in retiringNow)
                {
                    emp.IsRetired = true;
                    predictions.Add(new Prediction
                    {
                        EmpId = emp.EmpId,
                        Name = emp.Name,
                        FromGrade = $"G-{emp.SimGradeNo}",
                        ToGrade = "RETIRED",
                        NewDesignation = "Retirement",
                        PredictedDate = emp.RetirementDate
                    });
                }

                // B. Promotions
                foreach (var targetPost in orgPosts.OrderBy(p => p.SL_No))
                {
                    var quota = targetPost.GetPromotionQuota();
                    if (quota == 0) continue;

                    var feederIds = targetPost.GetFeederPostIds();
                    if (!feederIds.Any()) continue;

                    int currentOccupants = employees.Count(e => !e.IsRetired && e.SimSL_No == targetPost.SL_No);
                    int maxPromotable = (int)Math.Ceiling(targetPost.TotalPost * (quota / 100.0));
                    int totalVacancy = targetPost.TotalPost - currentOccupants;

                    if (totalVacancy <= 0) continue;

                    int promoSlots = Math.Min(totalVacancy, Math.Max(0, maxPromotable - currentOccupants));
                    if (promoSlots <= 0) continue;

                    var feederPosts = orgPosts.Where(p => feederIds.Contains(p.SL_No)).ToList();
                    var candidates = new List<Employee>();
                    foreach (var feeder in feederPosts)
                    {
                        candidates.AddRange(employees.Where(e => !e.IsRetired && e.SimSL_No == feeder.SL_No));
                    }

                    var targetTrack = targetPost.Track?.ToUpper() == "IT" ? "IT" : "GENERAL";
                    bool isMergePoint = targetPost.GradeNo <= 2 && targetTrack == "GENERAL";

                    if (!isMergePoint)
                    {
                        candidates = candidates.Where(e => e.SimTrack == targetTrack).ToList();
                    }

                    int yearsNeeded = targetPost.GetYearsRequired();
                    var eligible = candidates
                        .Where(e => (currentDate - e.SimLastPromoDate).TotalDays / 365.25 >= yearsNeeded)
                        .OrderBy(e => e.GradeNo)                   // Rule 1: Grade No (lower = more senior)
                        .ThenBy(e => e.SeniorId ?? int.MaxValue)   // Rule 2: Senior ID
                        .ThenBy(e => e.EmpId)                      // Rule 3: Emp ID
                        .Take(promoSlots)
                        .ToList();

                    foreach (var winner in eligible)
                    {
                        predictions.Add(new Prediction
                        {
                            EmpId = winner.EmpId,
                            Name = winner.Name,
                            FromGrade = $"G-{winner.SimGradeNo}",
                            ToGrade = $"G-{targetPost.GradeNo}",
                            NewDesignation = targetPost.DesignationName,
                            PredictedDate = currentDate
                        });

                        winner.SimGradeNo = targetPost.GradeNo;
                        winner.SimSL_No = targetPost.SL_No;
                        winner.SimTrack = isMergePoint ? "GENERAL" : targetTrack;
                        winner.SimLastPromoDate = currentDate;
                    }
                }
            }
            return predictions;
        }

        private void SavePredictions(List<Prediction> predictions)
        {
            using var conn = new SqliteConnection($"Data Source={_dbPath}");
            conn.Open();
            using var transaction = conn.BeginTransaction();
            string sql = @"INSERT INTO Predictions (EmpId, Name, FromGrade, ToGrade, NewDesignation, PredictedDate) 
                           VALUES (@eid, @name, @from, @to, @desg, @date)";

            foreach (var p in predictions)
            {
                using var cmd = new SqliteCommand(sql, conn);
                cmd.Transaction = transaction;
                cmd.Parameters.AddWithValue("@eid", p.EmpId);
                cmd.Parameters.AddWithValue("@name", p.Name);
                cmd.Parameters.AddWithValue("@from", p.FromGrade);
                cmd.Parameters.AddWithValue("@to", p.ToGrade);
                cmd.Parameters.AddWithValue("@desg", p.NewDesignation);
                cmd.Parameters.AddWithValue("@date", p.PredictedDate.ToString("yyyy-MM-dd"));
                cmd.ExecuteNonQuery();
            }
            transaction.Commit();
        }
    }
}
