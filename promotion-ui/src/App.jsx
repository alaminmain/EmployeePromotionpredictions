import { useState, useEffect } from "react";
import "./App.css";

const API_BASE = "http://localhost:5223";

function App() {
  const [employees, setEmployees] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [showDropdown, setShowDropdown] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [predictions, setPredictions] = useState([]);
  const [targetDate, setTargetDate] = useState("2036-07-01");
  const [loading, setLoading] = useState(false);
  const [viewMode, setViewMode] = useState("overview"); // 'overview', 'timeline', 'report', 'yearly'
  const [overviewData, setOverviewData] = useState([]);
  const [yearlyData, setYearlyData] = useState([]);

  // Load employees
  const loadEmployees = () => {
    fetch(`${API_BASE}/api/employees`)
      .then((res) => res.json())
      .then((data) => setEmployees(data))
      .catch((err) => console.error("Failed to load employees:", err));
  };
 
  const loadOverview = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/employees/overview`);
      const data = await res.json();
      setOverviewData(data);
    } catch (err) {
      console.error("Failed to load overview data:", err);
    }
  };
 
  const loadYearlyReport = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/reports/promotions-per-year`);
      const data = await res.json();
      setYearlyData(data);
    } catch (err) {
      console.error("Failed to load yearly report:", err);
    }
  };

  useEffect(() => {
    loadEmployees();
    loadOverview();
    loadYearlyReport();
  }, []);
 
  // Rebuild simulation
  const handleRebuild = async () => {
    if (!window.confirm("This will rebuild the entire promotion database. Continue?")) return;
    
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/simulation/run`, { method: "POST" });
      if (res.ok) {
        alert("Simulation rebuilt successfully!");
        setSearchTerm("");
        setSelectedEmployee(null);
        setPredictions([]);
        loadEmployees(); // Reload list
      } else {
        alert("Failed to rebuild simulation.");
      }
    } catch (err) {
      console.error("Rebuild error:", err);
      alert("Error triggering rebuild.");
    }
    setLoading(false);
  };

  // Load predictions when employee selected
  const loadPredictions = async (empId) => {
    if (!empId) return;
    setLoading(true);
    try {
      const endpoint =
        viewMode === "report"
          ? `${API_BASE}/api/report/${empId}?date=${targetDate}`
          : `${API_BASE}/api/predictions/${empId}`;

      const res = await fetch(endpoint);
      const data = await res.json();
      // Match camelCase from API
      setPredictions(data.predictions || data.promotionPath || []);
      setSelectedEmployee(data);
    } catch (err) {
      console.error("Failed to load predictions:", err);
    }
    setLoading(false);
  };

  const filteredEmployees = employees.filter(
    (emp) =>
      emp.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      emp.empId.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleSelectEmployee = (emp) => {
    setSearchTerm(`${emp.name} (${emp.empId})`);
    setShowDropdown(false);
    loadPredictions(emp.empId);
  };

  return (
    <div className="app">
      <header className="header">
        <div className="header-content">
          <div>
            <h1>🏆 Employee Promotion Predictions</h1>
            <p>Select an employee to view their predicted career path</p>
          </div>
          <button className="rebuild-btn" onClick={handleRebuild} disabled={loading}>
            {loading ? "⚙️ Rebuilding..." : "🔄 Rebuild Simulation"}
          </button>
        </div>
      </header>

      <div className="controls">
        <div className="control-group search-container">
          <label>Search Employee</label>
          <div className="search-box">
            <div className="input-wrapper">
              <input
                type="text"
                placeholder="Type name or ID..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setShowDropdown(true);
                }}
                onFocus={() => setShowDropdown(true)}
                className="search-input"
              />
              {searchTerm && (
                <button 
                  className="clear-search" 
                  onClick={() => {
                    setSearchTerm("");
                    setSelectedEmployee(null);
                    setPredictions([]);
                  }}
                >
                  ✕
                </button>
              )}
            </div>
            {showDropdown && filteredEmployees.length > 0 && (
              <ul className="search-dropdown">
                {filteredEmployees.slice(0, 100).map((emp) => (
                  <li key={emp.empId} onClick={() => handleSelectEmployee(emp)}>
                    <div className="emp-search-name">{emp.name}</div>
                    <div className="emp-search-meta">
                      {emp.empId} • {emp.currentGrade}
                    </div>
                  </li>
                ))}
              </ul>
            )}
            {showDropdown && searchTerm && filteredEmployees.length === 0 && (
              <div className="search-no-results">No employees found</div>
            )}
          </div>
        </div>

        <div className="control-group">
          <label>View Mode</label>
          <div className="toggle-buttons">
            <button
              className={viewMode === "overview" ? "active" : ""}
              onClick={() => setViewMode("overview")}
            >
              📋 Overview
            </button>
            <button
              className={viewMode === "timeline" ? "active" : ""}
              onClick={() => setViewMode("timeline")}
            >
              📊 Timeline
            </button>
            <button
              className={viewMode === "report" ? "active" : ""}
              onClick={() => setViewMode("report")}
            >
              📄 Report
            </button>
            <button
              className={viewMode === "yearly" ? "active" : ""}
              onClick={() => setViewMode("yearly")}
            >
              📅 Yearly Report
            </button>
          </div>
        </div>

        {viewMode === "report" && (
          <div className="control-group">
            <label>Target Date</label>
            <input
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              className="date-input"
            />
            <button
              onClick={() =>
                selectedEmployee && loadPredictions(selectedEmployee.EmployeeId)
              }
              className="refresh-btn"
            >
              🔄 Refresh
            </button>
          </div>
        )}
      </div>

      {loading && <div className="loading">Loading predictions...</div>}

      {viewMode === "overview" && !loading && (
        <EmployeeOverview 
          data={overviewData} 
          onSelect={(empId) => {
            const emp = employees.find(e => e.empId === empId);
            if (emp) handleSelectEmployee(emp);
            setViewMode("timeline");
          }} 
        />
      )}
 
      {viewMode === "yearly" && !loading && (
        <YearlyReport data={yearlyData} />
      )}

      {selectedEmployee && !loading && viewMode !== "overview" && viewMode !== "yearly" && (
        <div className="content">
          {viewMode === "timeline" ? (
            <Timeline employee={selectedEmployee} predictions={predictions} />
          ) : (
            <Report
              employee={selectedEmployee}
              predictions={predictions}
              targetDate={targetDate}
            />
          )}
        </div>
      )}
    </div>
  );
}
 
// Mini Sparkline component
function Sparkline({ points }) {
  if (!points || points.length < 2) return <div className="no-data">N/A</div>;
  
  // Grade 1 is top (high), Grade 20 is bottom (low)
  // We flip it so Grade 1 looks like a peak
  const maxGrade = 20;
  const width = 120;
  const height = 40;
  const padding = 5;
 
  const mappedPoints = points.map((p, i) => ({
    x: (i / (points.length - 1)) * (width - 2 * padding) + padding,
    y: ((p - 1) / (maxGrade - 1)) * (height - 2 * padding) + padding // Lower grade # = higher Y (visually)
  }));
 
  // Generate SVG path string
  let d = `M ${mappedPoints[0].x} ${mappedPoints[0].y}`;
  for (let i = 1; i < mappedPoints.length; i++) {
    d += ` L ${mappedPoints[i].x} ${mappedPoints[i].y}`;
  }
 
  return (
    <svg width={width} height={height} className="sparkline">
      <path d={d} fill="none" stroke="#10b981" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      {mappedPoints.map((p, i) => (
        <circle key={i} cx={p.x} cy={p.y} r="2" fill="#10b981" />
      ))}
    </svg>
  );
}
 
// Employee Overview Component
function EmployeeOverview({ data, onSelect }) {
  const [filter, setFilter] = useState("");
  
  const filtered = data.filter(emp => 
    emp.name.toLowerCase().includes(filter.toLowerCase()) ||
    emp.empId.toLowerCase().includes(filter.toLowerCase())
  );
 
  return (
    <div className="overview-container">
      <div className="overview-header">
        <h2>Workforce Promotion Overview</h2>
        <input 
          type="text" 
          placeholder="Filter overview list..." 
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="overview-filter"
        />
      </div>
      <div className="overview-grid">
        <table className="overview-table">
          <thead>
            <tr>
              <th>Employee</th>
              <th>ID</th>
              <th>Grade Progression (40 Years)</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(emp => (
              <tr key={emp.empId}>
                <td><strong>{emp.name}</strong></td>
                <td><span className="emp-id-badge">{emp.empId}</span></td>
                <td>
                  <div className="sparkline-wrapper">
                    <Sparkline points={emp.gradePath} />
                    <div className="sparkline-labels">
                      <span>{emp.gradePath[0]}</span>
                      <span>→</span>
                      <span>{emp.gradePath[emp.gradePath.length - 1]}</span>
                    </div>
                  </div>
                </td>
                <td>
                  <button className="view-details-btn" onClick={() => onSelect(emp.empId)}>
                    View Path
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// Timeline Component
function Timeline({ employee, predictions }) {
  // Helper to calculate years between two ISO date strings (approx)
  const calculateYearsBetween = (date1, date2) => {
    if (!date1 || !date2) return null;
    const d1 = new Date(date1);
    const d2 = new Date(date2);
    const diffTime = Math.abs(d2 - d1);
    const diffYears = diffTime / (1000 * 60 * 60 * 24 * 365.25);
    return diffYears.toFixed(1);
  };

  return (
    <div className="timeline-container">
      <h2>Career Promotion Path</h2>
      <div className="employee-info">
        <span className="emp-name">
          {employee.employeeName || employee.name}
        </span>
        <span className="emp-id">ID: {employee.employeeId || employee.empId}</span>
      </div>

      <div className="staircase-wrapper">
        <div className="staircase">
          {predictions.map((pred, idx) => {
            let timeCount = null;
            if (idx > 0 && pred.predictedDate && predictions[idx - 1].predictedDate) {
              timeCount = calculateYearsBetween(predictions[idx - 1].predictedDate, pred.predictedDate);
            }
            
            return (
              <div
                key={idx}
                className={`stair-step ${pred.newDesignation === "Retirement" ? "retirement" : ""}`}
                style={{ "--step": idx }}
              >
                {idx > 0 && <div className="riser"></div>}
                <div className="tread"></div>

                <div className="stair-content">
                  <div className="stair-date">
                    {pred.predictedDate}
                    {timeCount !== null && (
                      <span className="time-count">(+{timeCount} yrs)</span>
                    )}
                  </div>
                  <div className="stair-position">{pred.newDesignation}</div>
                  <div className="stair-grades">
                    <span className="grade-badge">{pred.fromGrade}</span>
                    <span className="arrow">→</span>
                    <span className="grade-badge highlight">{pred.toGrade}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {predictions.length === 0 && (
        <div className="no-predictions">
          No promotions predicted for this employee.
        </div>
      )}
    </div>
  );
}

// Report Component
function Report({ employee, predictions, targetDate }) {
  const handlePrint = () => window.print();

  return (
    <div className="report-container">
      <div className="report">
        <div className="report-header">
          <h2>PROMOTION PREDICTION REPORT</h2>
          <div className="report-date">
            Generated: {new Date().toLocaleDateString()}
          </div>
        </div>

        <table className="report-info">
          <tbody>
            <tr>
              <td>Employee Name:</td>
              <td>
                <strong>{employee.employeeName || employee.name}</strong>
              </td>
            </tr>
            <tr>
              <td>Employee ID:</td>
              <td>{employee.employeeId || employee.empId}</td>
            </tr>
            <tr>
              <td>Current Grade:</td>
              <td>{employee.currentGrade}</td>
            </tr>
            <tr>
              <td>Target Date:</td>
              <td>{targetDate}</td>
            </tr>
            <tr>
              <td>Total Promotions:</td>
              <td>
                <strong>{predictions.length}</strong>
              </td>
            </tr>
          </tbody>
        </table>

        <h3>Promotion Path</h3>
        <table className="report-table">
          <thead>
            <tr>
              <th>Step</th>
              <th>Date</th>
              <th>From</th>
              <th>To</th>
              <th>New Position</th>
            </tr>
          </thead>
          <tbody>
            {predictions.map((pred, idx) => (
              <tr key={idx}>
                <td>{pred.step}</td>
                <td>{pred.predictedDate}</td>
                <td>{pred.fromGrade}</td>
                <td>{pred.toGrade}</td>
                <td>{pred.newDesignation}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {predictions.length > 0 && (
          <div className="final-position">
            <strong>Final Position by {targetDate}:</strong>{" "}
            {predictions[predictions.length - 1].newDesignation}
          </div>
        )}
      </div>

      <button className="print-btn" onClick={handlePrint}>
        🖨️ Print Report
      </button>
    </div>
  );
}

// Yearly Report Component
function YearlyReport({ data }) {
  const handlePrint = () => window.print();

  return (
    <div className="report-container">
      <div className="report">
        <div className="report-header">
          <h2>YEARLY PROMOTION OVERVIEW</h2>
          <div className="report-date">
            Generated: {new Date().toLocaleDateString()}
          </div>
        </div>

        <table className="report-table">
          <thead>
            <tr>
              <th>Year</th>
              <th>Total Promotions Predicted</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item, idx) => (
              <tr key={idx}>
                <td><strong>{item.year}</strong></td>
                <td>{item.count}</td>
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan="2" style={{ textAlign: "center" }}>No data available</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <button className="print-btn" onClick={handlePrint}>
        🖨️ Print Report
      </button>
    </div>
  );
}

export default App;
