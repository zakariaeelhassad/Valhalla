# Spring Boot Backend Application

A robust Spring Boot backend application with REST API capabilities, security, validation, and testing infrastructure.

## 🚀 Features

- **Spring Web**: RESTful API endpoints
- **Spring Security**: Authentication and authorization framework
- **Spring Data JPA**: Database operations with Hibernate
- **Lombok**: Reduced boilerplate code
- **Validation**: Request validation using Bean Validation
- **H2 Database**: In-memory database for development
- **Spring Boot DevTools**: Hot reload during development
- **Comprehensive Testing**: Unit and integration testing support

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.6+** (or use the Maven wrapper included)
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code recommended)

## 🛠️ Setup Instructions

### 1. Clone or Navigate to the Project

```bash
cd C:\Users\youco\IdeaProjects\AjiT9aser\backend
```

### 2. Build the Project

Using Maven wrapper (recommended):
```bash
mvnw clean install
```

Or using system Maven:
```bash
mvn clean install
```

### 3. Run the Application

Using Maven wrapper:
```bash
mvnw spring-boot:run
```

Or using system Maven:
```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

## 🧪 Testing the Application

### Using Postman

1. **Health Check Endpoint**
   - **Method**: GET
   - **URL**: `http://localhost:8080/api/health`
   - **Expected Response**:
     ```json
     {
       "status": "UP",
       "timestamp": "2026-02-16T14:25:00",
       "message": "Backend application is running successfully"
     }
     ```

### Using cURL

```bash
curl http://localhost:8081/api/health
```

## 🐳 Docker (Backend Only)

Run backend + PostgreSQL with Docker Compose:

```bash
cd backend
docker compose up --build -d
```

Check containers status:

```bash
docker compose ps
```

Check backend logs:

```bash
docker compose logs backend --tail=200
```

Health check:

```bash
curl http://localhost:8080/api/health
```

Stop and remove containers:

```bash
docker compose down
```

### H2 Database Console

Access the H2 console for database inspection:
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (leave empty)

## 📁 Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/backend/
│   │   │   ├── BackendApplication.java       # Main application entry point
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java       # Security configuration
│   │   │   ├── controller/
│   │   │   │   └── HealthController.java     # REST controllers
│   │   │   ├── service/                      # Business logic layer
│   │   │   ├── repository/                   # Data access layer
│   │   │   ├── model/                        # Entity classes
│   │   │   ├── dto/                          # Data Transfer Objects
│   │   │   ├── exception/                    # Custom exceptions
│   │   │   └── security/                     # Security components
│   │   └── resources/
│   │       ├── application.properties        # Main configuration
│   │       └── application-dev.properties    # Development profile
│   └── test/
│       └── java/com/example/backend/
│           └── BackendApplicationTests.java  # Test classes
├── pom.xml                                   # Maven dependencies
├── .gitignore                                # Git ignore rules
└── README.md                                 # This file
```

## 🔧 Running Tests

Run all tests:
```bash
mvnw test
```

Run tests with coverage:
```bash
mvnw clean test jacoco:report
```

## 📝 Configuration

### Application Properties

Key configurations in `application.properties`:
- **Server Port**: 8080
- **Database**: H2 in-memory
- **Active Profile**: dev
- **JPA**: Auto DDL update, SQL logging enabled

### Profiles

- **dev**: Development profile with detailed logging and error messages
- **prod**: Production profile (to be configured)

## 🔐 Security

The application uses Spring Security with:
- **CORS**: Enabled for `localhost:4200` and `localhost:3000`
- **CSRF**: Disabled for stateless API
- **Session Management**: Stateless (JWT-ready)
- **Password Encoding**: BCrypt

**Note**: Current security is permissive for development. Implement proper authentication before production deployment.

## 📦 Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2.2 | Framework |
| Java | 17 | Language |
| Spring Web | - | REST API |
| Spring Security | - | Security |
| Spring Data JPA | - | Database |
| H2 Database | - | In-memory DB |
| Lombok | - | Code generation |
| Validation | - | Bean validation |

## 🎯 Next Steps

1. **Add Authentication**: Implement JWT-based authentication
2. **Create Entities**: Define your domain models in the `model` package
3. **Build Services**: Implement business logic in the `service` package
4. **Create Repositories**: Add JPA repositories for data access
5. **Add DTOs**: Create request/response objects in the `dto` package
6. **Exception Handling**: Implement global exception handling
7. **API Documentation**: Add Swagger/OpenAPI documentation
8. **Database Migration**: Switch to PostgreSQL/MySQL for production
9. **Write Tests**: Add comprehensive unit and integration tests

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For issues or questions, please create an issue in the repository.
