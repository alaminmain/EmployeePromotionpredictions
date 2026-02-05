# 🏆 Employee Promotion Predictions

A modern web application that predicts employee career paths over 40 years based on organizational structure, seniority rules, and promotion quotas.

![.NET](https://img.shields.io/badge/.NET-10.0-purple)
![React](https://img.shields.io/badge/React-18+-blue)
![SQLite](https://img.shields.io/badge/SQLite-3-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 📋 Features

### 🔮 Prediction Engine

- **40-Year Career Simulation**: Projects promotions from current date to retirement
- **Seniority-Based Selection**: Uses `GradeNo → SeniorId → EmpId` priority
- **IT/General Track Support**: Separate career paths that merge at senior levels
- **Retirement Milestones**: Explicit tracking of career endpoints

### 📊 Workforce Overview

- **Promotion Sparklines**: Mini-charts showing grade progression at a glance
- **Searchable Employee List**: Filter by name or ID
- **Responsive Design**: Works on desktop and mobile

### 📄 Report Generation

- **Print-Ready Reports**: Clean, professional format
- **Target Date Filtering**: View predictions up to a specific date
- **Career Path Timeline**: Step-by-step promotion visualization

---

## 🏗️ Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────┐
│   React UI      │────▶│  ASP.NET API     │────▶│  SQLite DB  │
│   (Vite)        │◀────│  (Minimal API)   │◀────│             │
│   Port: 5173    │     │  Port: 5050      │     │ Promotion.db│
└─────────────────┘     └──────────────────┘     └─────────────┘
```

---

## 🗄️ Database Schema

### Predictions Table

| Column           | Type     | Description                           |
| ---------------- | -------- | ------------------------------------- |
| `Id`             | INTEGER  | Primary Key (Auto-increment)          |
| `EmpId`          | TEXT     | Employee ID                           |
| `Name`           | TEXT     | Employee Name                         |
| `FromGrade`      | TEXT     | Starting Grade (e.g., "G-10")         |
| `ToGrade`        | TEXT     | Promoted Grade (e.g., "G-9")          |
| `NewDesignation` | TEXT     | New Position Title                    |
| `PredictedDate`  | TEXT     | Predicted Promotion Date (YYYY-MM-DD) |
| `CreatedAt`      | DATETIME | Record Creation Timestamp             |

### Indexes

- `idx_empid` on `EmpId` - Fast employee lookups
- `idx_date` on `PredictedDate` - Date-based queries

---

## 📁 Project Structure

```
EMPPromotion/
├── PromotionAPI/              # ASP.NET Minimal API
│   ├── Program.cs             # API endpoints
│   ├── SimulationManager.cs   # Promotion engine logic
│   └── PromotionAPI.csproj
├── promotion-ui/              # React Frontend
│   ├── src/
│   │   ├── App.jsx            # Main components
│   │   └── App.css            # Styles
│   └── package.json
├── EmpList.csv                # Employee data source
├── Final_Complete_Master_List.csv  # Org structure
├── Promotion.db               # SQLite database
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- .NET 10.0 SDK
- Node.js 18+
- npm or yarn

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/alaminmain/EmployeePromotionpredictions.git
   cd EmployeePromotionpredictions
   ```

2. **Start the API**

   ```bash
   cd PromotionAPI
   dotnet run --urls "http://localhost:5050"
   ```

3. **Start the Frontend** (in a new terminal)

   ```bash
   cd promotion-ui
   npm install
   npm run dev
   ```

4. **Open in browser**
   Navigate to `http://localhost:5173`

---

## 🔧 API Endpoints

| Method | Endpoint                              | Description                    |
| ------ | ------------------------------------- | ------------------------------ |
| GET    | `/api/employees`                      | List all employees             |
| GET    | `/api/employees/overview`             | Employee list with grade paths |
| GET    | `/api/employees/{id}`                 | Single employee details        |
| GET    | `/api/predictions/{empId}`            | Predictions for employee       |
| GET    | `/api/report/{empId}?date=YYYY-MM-DD` | Report by target date          |
| POST   | `/api/simulation/run`                 | Rebuild predictions            |

---

## 📐 Seniority Rules

The promotion engine selects candidates based on:

1. **Grade Number** (Lower = More Senior)
2. **Senior ID** (Official seniority rank)
3. **Employee ID** (Tie-breaker)

Additional factors:

- Years in current post (eligibility)
- Track isolation (IT vs General)
- Promotion quotas per post

---

## 🎨 Screenshots

### Workforce Overview

- List of all employees with promotion sparklines
- Visual grade progression over 40 years

### Career Timeline

- Step-by-step promotion path
- Retirement milestone highlighting

### Print Report

- Professional, clean report format
- Target date filtering

---

## 📊 Data Sources

| File                             | Description                                         |
| -------------------------------- | --------------------------------------------------- |
| `EmpList.csv`                    | Employee master data (ID, name, DOB, dates, grades) |
| `Final_Complete_Master_List.csv` | Organizational posts and promotion rules            |

---

## 🛠️ Technologies

- **Backend**: ASP.NET Core Minimal API, C# 12
- **Frontend**: React 18, Vite, CSS3
- **Database**: SQLite with Microsoft.Data.Sqlite
- **CSV Parsing**: CsvHelper

---

## 📝 License

This project is licensed under the MIT License.

---

## 👨‍💻 Author

**Al Amin**

- GitHub: [@alaminmain](https://github.com/alaminmain)

---

_Built with ❤️ for workforce planning and career development insights._
