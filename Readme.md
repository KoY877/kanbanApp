# Kanban Board API

A modern REST API for managing Kanban boards, users, and team collaboration.
Built with **Spring Boot**, following clean architecture and best practices.

---

## Live Demo

* Frontend: https://your-app.vercel.app
* Backend API: https://your-api.onrender.com
* Swagger UI: http://localhost:8081/swagger-ui.html

---

## Features

* User registration & authentication (JWT)
* Access token (15 min) + Refresh token (7 days, httpOnly cookie)
* Secure token rotation with reuse detection
* Access token blacklist (revocation before expiry)
* Kanban board management
* Custom columns with WIP limits
* Team collaboration (members & roles)
* Full CRUD operations
* Cascade deletion
* Password encryption (BCrypt)
* Rate limiting (Bucket4j)
* Interactive API documentation (Swagger UI)
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
| Spring Boot 3.5   | Backend framework         |
| Spring Security   | Authentication & security |
| Spring Data JPA   | ORM & persistence         |
| MySQL             | Database                  |
| Docker            | Containerization          |
| Swagger (OpenAPI) | API documentation         |
| Bucket4j          | Rate limiting             |

---

## Authentication

This API uses **JWT Bearer tokens** with a secure httpOnly cookie for refresh tokens.

### Flow

1. Call `POST /auth/login` or `POST /auth/register`
2. The response body contains an `accessToken` (valid 15 minutes)
3. A `refreshToken` is set as an **httpOnly cookie** (valid 7 days) — never in the JSON body
4. Use the `accessToken` in the `Authorization` header: `Bearer <token>`
5. When the access token expires, call `POST /auth/refresh` — the cookie is sent automatically
6. To end the session, call `POST /auth/logout`

### Headers

```http
Authorization: Bearer <accessToken>
```

---

## API Overview

### Authentication

* `POST /auth/login`
* `POST /auth/register`
* `POST /auth/refresh`
* `POST /auth/logout`
* `POST /auth/change-password`
* `POST /auth/revoke-all-tokens`

### Boards

* `GET /boards`
* `POST /boards`
* `PUT /boards/{id}`
* `DELETE /boards/{id}`

### Columns

* `POST /board/kanban-column`
* `GET /board/kanban-column`
* `PUT /board/kanban-column/{id}`
* `DELETE /board/kanban-column/{id}`

### Tasks

* `GET /tasks`
* `POST /tasks`
* `PUT /tasks/{id}`
* `DELETE /tasks/{id}`

### Members

* `POST /members`
* `GET /members`
* `PUT /members/{id}`
* `DELETE /members/{id}`

### Users

* `GET /user`
* `GET /user/{id}`
* `PATCH /user/profile/{id}`
* `DELETE /user/{id}`

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
