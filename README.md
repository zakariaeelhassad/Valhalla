# ⚽ Valhalla - Fantasy Football Application

A full-stack **Fantasy Football** web application built with Angular (frontend) and Spring Boot (backend) that allows users to create and manage virtual teams with real players, simulate matches, and track points.

## 📋 Quick Links

- [Setup Instructions](#setup)
- [Running the App](#running-the-app)
- [Testing](#testing)
- [Docker](#docker)
- [Project Diagrams](#project-diagrams)
- [Screenshots](#screenshots)

## 🛠️ Technology Stack

| Component | Technology |
|-----------|-----------|
| **Frontend** | Angular 21.2.5, TypeScript, Vitest |
| **Backend** | Spring Boot 3.2.2, Java 17, Spring Security |
| **Database** | PostgreSQL 16 |
| **DevOps** | Docker, Docker Compose, GitHub Actions |

## 📦 Prerequisites

- **Node.js** 18+ and **npm** 9+
- **Java 17** and **Maven** 3.6+
- **PostgreSQL 14+** (or Docker for PostgreSQL)
- **Docker** & **Docker Compose** (optional, for containerization)

## <a name="setup"></a>🚀 Setup Instructions

### 1. Clone & Navigate to Project
```bash
git clone https://github.com/yourusername/valhalla.git
cd Valhalla
```

### 2. Backend Setup

**Option A: Host Machine**
```bash
cd backend
# Create PostgreSQL database: aji_t9aser_db
mvn clean install
mvn spring-boot:run
# Runs on http://localhost:8080
```

**Option B: Docker Compose**
```bash
cd backend
docker compose up --build -d
# Runs on http://localhost:8081
```

### 3. Frontend Setup
```bash
cd angular-frontend
npm install
ng serve
# Runs on http://localhost:4200
```

## <a name="running-the-app"></a>🎮 Running the Application

### Development Mode (Host)

**Terminal 1: Backend**
```bash
cd backend && mvn spring-boot:run
```

**Terminal 2: Frontend**
```bash
cd angular-frontend && ng serve --open
```

### Production Build
```bash
cd angular-frontend && ng build --prod
cd backend && mvn clean package -DskipTests
```

## <a name="testing"></a>🧪 Running Tests

**Backend Tests**
```bash
cd backend
mvn clean test
```

**Frontend Tests**
```bash
cd angular-frontend
npx vitest run
```

## <a name="docker"></a>🐳 Docker Setup

Start the full stack with PostgreSQL:

```bash
cd backend
docker compose up --build -d
docker compose ps          # Check status
curl http://localhost:8081/api/health  # Health check
docker compose down        # Stop containers
```

## ⭐ Features

- 🔐 User authentication with JWT tokens
- ⚽ Create and manage virtual teams
- 📊 Player selection with budget constraints
- 🔄 Team lineup management & substitutions
- 📈 Points tracking by gameweek
- 🏆 Global leaderboard & rankings
- 🎨 Responsive Angular UI
- 📱 Profile management (images + info)
- 🎯 Match simulation with event commentary

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login user |
| GET | `/api/auth/profile` | Get user profile |
| GET | `/api/teams/{userId}` | Get user's team |
| GET | `/api/matches` | Get all matches |
| GET | `/api/points` | Get points breakdown |
| GET | `/api/leaderboard` | Get rankings |
| GET | `/api/health` | Health check |

**Full API docs** (when backend running): `http://localhost:8080/swagger-ui.html`

## 📂 Project Structure

```
Valhalla/
├── README.md
├── backend/                    # Spring Boot application
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── src/
├── angular-frontend/           # Angular application
│   ├── package.json
│   ├── angular.json
│   └── src/
└── .github/
    └── workflows/ci-cd.yml     # GitHub Actions
```

## 🤝 Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit a pull request

**Code Standards:**
- Backend: Follow Java conventions, write unit tests, run `mvn clean test`
- Frontend: Follow Angular style guide, write `.spec.ts` tests, run `npx vitest run`

## 📊 Project Diagrams

This section is reserved for project diagrams and architecture visuals.

**To add diagrams:**
1. Create a `docs/diagrams` folder
2. Add your diagram files (.png, .jpg, .svg)
3. Reference them here with markdown syntax

Example:
```markdown
![System Architecture](docs/diagrams/architecture.png)
![Database Schema](docs/diagrams/database-schema.png)
![Auth Flow](docs/diagrams/auth-flow.png)
```

## 📸 Screenshots

This section is reserved for application screenshots.

**To add screenshots:**
1. Create a `docs/screenshots` folder
2. Take screenshots of the app pages
3. Reference them here with markdown syntax

Example:
```markdown
### Home Page
![Home](docs/screenshots/home.png)

### Login
![Login](docs/screenshots/login.png)

### Dashboard
![Dashboard](docs/screenshots/dashboard.png)

### Team Selection
![Team Selection](docs/screenshots/team-selection.png)

### Points
![Points](docs/screenshots/points.png)

### Profile
![Profile](docs/screenshots/profile.png)
```
