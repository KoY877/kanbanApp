# Kanban Board API

Complete REST API for managing Kanban boards with users, customizable columns, and team management. Built with Spring Boot 3.5 and Java 23.

## Features

- **User Management** - Registration, authentication via secret API key
- **Kanban Boards** - Create and manage multiple boards per user
- **Customizable Columns** - Work organization with WIP (Work In Progress) limits
- **Team Management** - Add members with roles to boards
- **Cascade Deletion** - Automatic deletion of related entities
- **Security** - Password encryption with BCrypt
- **API Documentation** - Integrated Swagger/OpenAPI interface
- **Docker Ready** - Docker Compose configuration included

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|-------|
| **Java** | 23 | Development language |
| **Spring Boot** | 3.5.11 | Main framework |
| **Spring Data JPA** | 3.x | Persistence and ORM |
| **Spring Security** | 3.x | Authentication & security |
| **Spring Validation** | 3.x | Input validation |
| **MariaDB/MySQL** | Latest | Database |
| **Lombok** | Latest | Boilerplate code reduction |
| **SpringDoc OpenAPI** | 2.6.0 | Swagger documentation |
| **Maven** | 3.x | Dependency manager |
| **Docker Compose** | - | Services orchestration |

## Prerequisites

- **Java JDK 23** or higher
- **Maven 3.6+**
- **Docker & Docker Compose** (for the database)
- **Git**

## Installation

### 1. Clone the repository

```bash
git clone <repository-url>
cd todoapp_spring_boot
```

### 2. Start Docker services

```bash
docker-compose -f src/main/resources/docker-compose.yaml up -d
```

This starts:
- **MariaDB** on port `3309`
- **Adminer** (DB interface) on port `9082`

### 3. Verify database connection

Access Adminer: http://localhost:9082

**Credentials**:
- System: `MySQL`
- Server: `mariadb-latest:3306`
- Username: `appTodo`
- Password: `***************`
- Database: `tododb`

### 4. Build and run the application

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The API will be accessible at: **http://localhost:8081/api**

## API Documentation

### Swagger UI

Once the application is running, access the interactive documentation:

**http://localhost:8081/api/swagger-ui.html**

![Swagger UI](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

### OpenAPI Spec

OpenAPI specification URL: **http://localhost:8081/api/v3/api-docs**

## Main Endpoints

### Users (`/api/user`)

| Method | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/user` | Create a new user | No |
| `GET` | `/user` | Get all users | No |
| `POST` | `/user/validate` | Validate credentials (login) | No |
| `DELETE` | `/user/{secret}` | Delete a user and their boards | No |

**Example - User creation:**
```json
POST /api/user
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePassword123"
}
```

**Response:**
```json
{
  "id": "uuid-123",
  "username": "john_doe",
  "email": "john@example.com",
  "secret": "api-secret-key-456"
}
```

---

### Boards (`/api/board`)

| Method | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/board` | Create a new board | No |
| `GET` | `/board` | Get all boards | No |
| `GET` | `/board/all` | Get boards for a user | `api-secret` |
| `GET` | `/board/{id}` | Get a board by ID | No |
| `PUT` | `/board/{id}` | Update a board | No |
| `PATCH` | `/board/{id}` | Partial update | No |
| `DELETE` | `/board/{id}` | Delete a board | No |

**Example - Board creation:**
```json
POST /api/board
Content-Type: application/json

{
  "name": "Project Alpha",
  "userId": "user-uuid-123",
  "selectedTask": "task-456",
  "globalOption": "agile",
  "columns": [
    { "columnName": "To Do" },
    { "columnName": "In Progress" }
  ],
  "added_columns": [
    { 
      "columnName": "Done", 
      "limitWorkInProgress": 10 
    }
  ],
  "members": [
    { 
      "memberEmail": "dev@example.com", 
      "role": "Developer" 
    }
  ]
}
```

---

### Kanban Columns (`/api/board/kanban-column`)

| Method | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/kanban-column` | Create a column |
| `GET` | `/kanban-column` | List all columns |
| `GET` | `/kanban-column/{id}` | Get a column |
| `PUT` | `/kanban-column/{id}` | Update a column |
| `DELETE` | `/kanban-column/{id}` | Delete a column |

**Example - Column creation:**
```json
POST /api/board/kanban-column
Content-Type: application/json

{
  "columnName": "In Review",
  "limitWorkInProgress": 5,
  "boardId": "board-uuid-789"
}
```

---

### Members (`/api/board/member`)

| Method | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/member` | Add a member to a board |
| `GET` | `/member` | List all members |
| `GET` | `/member/{id}` | Get a member |
| `PUT` | `/member/{id}` | Update a member |
| `DELETE` | `/member/{id}` | Delete a member |

**Example - Add member:**
```json
POST /api/board/member
Content-Type: application/json

{
  "memberEmail": "alice@example.com",
  "role": "Product Owner",
  "boardId": "board-uuid-789"
}
```

## Project Structure

```
src/
├── main/
│   ├── java/com/kanban/kanbanapp/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java      # Security configuration
│   │   │   └── WebConfig.java           # CORS configuration
│   │   ├── controller/
│   │   │   ├── BoardController.java     # Board endpoints
│   │   │   ├── KanbanColumnController.java
│   │   │   ├── MemberController.java
│   │   │   └── UserController.java
│   │   ├── Data_Transfer_Object/
│   │   │   ├── BoardCreateRequest.java  # Request DTOs
│   │   │   └── ...
│   │   ├── Model/
│   │   │   ├── Board.java               # JPA entities
│   │   │   ├── KanbanColumn.java
│   │   │   ├── Member.java
│   │   │   └── User.java
│   │   ├── repository/
│   │   │   ├── BoardRepository.java     # Spring Data repositories
│   │   │   └── ...
│   │   ├── service/
│   │   │   ├── board/
│   │   │   │   └── boardService.java    # Business logic
│   │   │   └── user/
│   │   │       └── UserServiceImpl.java
│   │   └── KanbanAppApplication.java    # Entry point
│   └── resources/
│       ├── application.properties       # App configuration
│       └── docker-compose.yaml          # Docker services
└── test/
    └── java/com/kanban/kanbanapp/
        └── KanbanappApplicationTests.java
```

## Configuration

### application.properties

File: `src/main/resources/application.properties`

```properties
# Application
spring.application.name=tododb
server.port=8081
server.servlet.context-path=/api

# Database
spring.datasource.url=jdbc:mysql://localhost:3309/tododb
spring.datasource.username=appTodo
spring.datasource.password= ******************
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
```

### Change application port

Modify in `application.properties`:
```properties
server.port=8080  # New port
```

## Authentication

Some endpoints require authentication via header:

```http
GET /api/board/all
api-secret: your-user-secret-key
```

The secret key is returned when:
- Registering (`POST /api/user`)
- Validating credentials (`POST /api/user/validate`)

## Tests

```bash
# Run unit tests
./mvnw test

# Run tests with report
./mvnw test jacoco:report
```

## Cascade Deletion

The application automatically handles cascade deletions:

- **Delete a Board** → deletes all its columns and members
- **Delete a User** → deletes all their boards (and their columns/members)
- **Delete a Column** → does not delete the parent board
- **Delete a Member** → does not delete the parent board

## Troubleshooting

### Database connection error

Check that Docker is running:
```bash
docker ps
```

Restart services:
```bash
docker-compose -f src/main/resources/docker-compose.yaml restart
```

### Port already in use

If port 8081 is occupied, modify `server.port` in `application.properties`.

### Hibernate Logs

For debugging, enable SQL logs:
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

## License

This project is for educational/demonstration purposes.

## Author

Developed with ❤️ by Junior Developer Koumodjo Y. Monni

---

## Useful Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Security](https://spring.io/projects/spring-security)
- [SpringDoc OpenAPI](https://springdoc.org/)
