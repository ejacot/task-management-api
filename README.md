# Task Management API

A secure REST API for personal task management, built as a backend engineering portfolio project.

It also includes a responsive browser interface, so the repository is a complete application rather than an API-only demo.

## Easiest way to start on Windows

Double-click `start-local.cmd`. The script starts the application with a local embedded database and opens:

```text
http://localhost:8080
```

Create an account in the browser, then add, edit, complete and delete tasks. Data is saved in the local `data` directory. No Docker or PostgreSQL setup is required for this mode. Keep the terminal window open while using the application; close it with `Ctrl+C`.

## Highlights

- Java 21 and Spring Boot 3
- PostgreSQL persistence with versioned Flyway migrations
- Stateless HTTP Basic authentication with BCrypt password hashing
- Per-user data isolation: users can only access their own tasks
- CRUD operations, status filtering, pagination, sorting and validation
- OpenAPI/Swagger documentation
- Integration tests with MockMvc and H2 in PostgreSQL compatibility mode
- Docker Compose for a one-command local environment
- GitHub Actions CI
- Responsive frontend built with HTML, CSS and vanilla JavaScript

## Architecture

The code follows a pragmatic layered architecture:

```text
HTTP request -> Controller -> Service -> Repository -> PostgreSQL
                    |            |
                 validation   ownership rules
```

Database changes are managed through migrations in `src/main/resources/db/migration`.

## Run locally

Requirements: Java 21 and Docker. For the no-Docker Windows option, use `start-local.cmd` as described above.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. Swagger UI is available at
`http://localhost:8080/swagger-ui.html` and health information at
`http://localhost:8080/actuator/health`.

To run the complete containerized stack:

```bash
./mvnw clean package
docker compose up --build
```

## API example

Register a user:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"change-me-123"}'
```

Create a task:

```bash
curl -u demo:change-me-123 -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Prepare technical interview","priority":"HIGH","dueDate":"2026-08-01"}'
```

List tasks, optionally filtered and paginated:

```bash
curl -u demo:change-me-123 \
  "http://localhost:8080/api/tasks?status=TODO&page=0&size=10&sort=dueDate,asc"
```

Update and delete:

```bash
curl -u demo:change-me-123 -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Prepare technical interview","status":"IN_PROGRESS","priority":"HIGH","dueDate":"2026-08-01"}'

curl -u demo:change-me-123 -X DELETE http://localhost:8080/api/tasks/1
```

## Main endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a user |
| `GET` | `/api/tasks` | List the authenticated user's tasks |
| `POST` | `/api/tasks` | Create a task |
| `GET` | `/api/tasks/{id}` | Read one task |
| `PUT` | `/api/tasks/{id}` | Replace one task |
| `DELETE` | `/api/tasks/{id}` | Delete one task |

## Tests

```bash
./mvnw verify
```

The integration suite verifies registration, authentication, task lifecycle and rejection of anonymous access.

## Future improvements

- JWT access and refresh tokens
- Optimistic locking for concurrent updates
- Testcontainers-based PostgreSQL integration tests
- Rate limiting and audit events

## License

MIT
