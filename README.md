# Customer Management System

A comprehensive customer management system built with Spring Boot, featuring user authentication, content management, real-time chat, and administrative functions.

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: MySQL 8.0
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript
- **Security**: Spring Security
- **Real-time Communication**: WebSocket, STOMP
- **Build Tool**: Maven
- **Testing**: JUnit 5, Spring Boot Test

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MySQL 8.0 or higher

## Getting Started

### 1. Database Setup

Create a MySQL database for development:

```sql
CREATE DATABASE cms_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configuration

Update the database configuration in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cms_dev
    username: your_username
    password: your_password
```

### 3. Running the Application

```bash
# Clone the repository
git clone <repository-url>
cd customer-management-system

# Run the application
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

### 4. Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/cms/
│   │   ├── controller/     # REST controllers and web controllers
│   │   ├── service/        # Business logic services
│   │   ├── repository/     # Data access layer
│   │   ├── entity/         # JPA entities
│   │   ├── dto/           # Data transfer objects
│   │   ├── config/        # Configuration classes
│   │   └── CustomerManagementSystemApplication.java
│   └── resources/
│       ├── templates/      # Thymeleaf templates
│       ├── static/         # Static web assets (CSS, JS, images)
│       └── application.yml # Application configuration
└── test/
    └── java/com/example/cms/ # Test classes
```

## Features

- User registration and authentication
- Personal profile management
- Content creation and management
- Comment system
- Real-time chat with administrators
- Administrative dashboard
- Search and filtering capabilities
- Notification system
- Performance monitoring

## Development

### Profiles

- `dev`: Development profile with H2 database and debug logging
- `test`: Test profile with in-memory H2 database
- `prod`: Production profile with MySQL and optimized settings

### Running with Different Profiles

```bash
# Development
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License.