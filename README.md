# KanbanApp

A fullstack Kanban board application: Angular frontend, Spring Boot REST API, MariaDB database. Manage boards, columns with WIP limits, tasks, and team members, secured with JWT authentication.

---

## Tech Stack

| Layer    | Technology                                                |
| -------- | ---------------------------------------------------------- |
| Frontend | Angular 21, Bootstrap 5, ng-bootstrap, FontAwesome          |
| Backend  | Java 23, Spring Boot 3.5, Spring Security, Spring Data JPA  |
| Database | MariaDB (MySQL-compatible)                                  |
| Auth     | JWT (access + refresh tokens)                                |
| Docs     | Swagger / OpenAPI                                            |
| Other    | Docker & Docker Compose, Bucket4j (rate limiting)            |

---

## Project Structure

```
KanbanApp/
├── backend/          # Spring Boot REST API
├── frontend/         # Angular application
└── docker-compose.yaml
```

See [backend/Readme.md](backend/Readme.md) and [frontend/README.md](frontend/README.md) for module-specific details.

---

## Features

* User registration & authentication (JWT access + httpOnly refresh cookie)
* Secure token rotation with reuse detection
* Kanban boards with custom columns and WIP limits
* Task management (full CRUD)
* Team collaboration with member roles
* Rate limiting and password encryption (BCrypt)
* Interactive API documentation (Swagger UI)
* Dockerized backend, frontend, and database

---

## Getting Started

### Prerequisites

* Docker & Docker Compose
* Node.js 20+ and Angular CLI 21 (for local frontend development)
* Java 23 and Maven (for local backend development)

### 1. Clone the repository

```bash
git clone <repository-url>
cd KanbanApp
```

### 2. Configure environment variables

Copy the example env file and fill in your own values:

```bash
cp .env.example .env
```

Required variables: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.

### 3. Run with Docker Compose

```bash
docker-compose up -d
```

This starts the MariaDB database, the Spring Boot backend, and the Angular frontend.

* Frontend: `http://localhost:4200`
* Backend API: `http://localhost:8084`
* Swagger UI: `http://localhost:8084/swagger-ui.html`

---

## Local Development

### Backend

```bash
cd backend
cp .env.example .env   # fill in DB and JWT values
./mvnw spring-boot:run
```

Run tests:

```bash
./mvnw test
```

### Frontend

```bash
cd frontend
npm install
ng serve
```

Navigate to `http://localhost:4200`.

---

## Author

**Koumodjo Y. Monni**
Junior Backend Developer
