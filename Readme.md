# Kanban Board API

A modern REST API for managing Kanban boards, users, and team collaboration.
Built with **Spring Boot**, following clean architecture and best practices.

---

## Live Demo

* Frontend: https://your-app.vercel.app
* Backend API: https://your-api.onrender.com
* Swagger UI: http://localhost:8081/api/swagger-ui.html

---

## Features

* User registration & authentication
* Kanban board management
* Custom columns with WIP limits
* Team collaboration (members & roles)
* Full CRUD operations
* Cascade deletion
* Password encryption (BCrypt)
* Interactive API documentation (Swagger)
* Docker support

---

## Architecture

The project follows a **layered architecture**:

```bash
Controller → Service → Repository → Database
```

* **Controller** → handles HTTP requests
* **Service** → business logic
* **Repository** → data access
* **DTO** → request/response mapping

---

## Tech Stack

| Technology        | Description               |
| ----------------- | ------------------------- |
| Java 23           | Programming language      |
| Spring Boot       | Backend framework         |
| Spring Security   | Authentication & security |
| Spring Data JPA   | ORM & persistence         |
| MariaDB           | Database                  |
| Docker            | Containerization          |
| Swagger (OpenAPI) | API documentation         |

---

## Authentication

> Current version uses an API key (`api-secret`)
> Migration to **JWT authentication** in progress

Example header:

```http
api-secret: your-secret-key
```

---

## API Overview

### Authentication

* `POST /auth/login`
* `POST /auth/register`

### Boards

* `GET /board`
* `POST /board`
* `PUT /board/{id}`
* `DELETE /board/{id}`

### Columns

* `POST /kanban-column`
* `GET /kanban-column`
* `PUT /kanban-column/{id}`
* `DELETE /kanban-column/{id}`

### Members

* `POST /member`
* `GET /member`
* `PUT /member/{id}`
* `DELETE /member/{id}`

---

## Screenshots

> Add screenshots of your Angular frontend and Swagger UI here

---

## Getting Started

### 1. Clone the repository

```bash
git clone <your-repository-url>
cd kanban-board-api
```

### 2. Start database (Docker)

```bash
docker-compose up -d
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

API available at:
`http://localhost:8081/`

---

## Testing

```bash
./mvnw test
```

---

### Setup

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env

## Why this project?

This project demonstrates:

* REST API design
* Backend architecture (Spring Boot)
* Team-based application logic
* Fullstack readiness (Angular + Spring Boot)

---

## Author

**Koumodjo Y. Monni**
Junior Backend Developer

---

## Support

If you like this project, feel free to ⭐ the repository!
