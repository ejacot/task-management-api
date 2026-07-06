# Roomly Work — Hotel Workforce Management

A full-stack hotel workforce application for weekly planning, room productivity, time tracking and earnings visibility.

The responsive PWA works on desktop, Android and iOS, backed by a Java 21 / Spring Boot API.

## Easiest way to start on Windows

Double-click `start-local.cmd`. The script starts the application with demo data and opens:

```text
http://localhost:8080
```

Demo accounts:

- employee: `mariana` / `demo1234`
- manager: `manager` / `manager1234`
- checker: `checker` / `checker1234`
- employer: `angajator` / `admin1234`

Data is saved in the local `data` directory. No Docker or PostgreSQL setup is required for this mode.

## Highlights

- Java 21 and Spring Boot 3
- PostgreSQL persistence with versioned Flyway migrations
- Stateless HTTP Basic authentication with BCrypt password hashing
- Hotel-scoped access and role-ready users (`EMPLOYER`, `MANAGER`, `CHECKER`, `EMPLOYEE`)
- Weekly shift plans and configurable hourly/room work types
- Room-to-hour conversion, time tracking, breaks and approval statuses
- Monthly hours, room totals, gross and clearly labelled estimated net pay
- Multi-employee weekly planning, absences and in-app notifications
- Separate Normal, Junior Suite and President room counts
- Room-list photo attachments and manager/checker approval
- Effective-dated hourly pay rates and configurable premium defaults
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

## Main endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/hotel/bootstrap` | Load the current hotel, profile, plan, logs and metrics |
| `POST` | `/api/hotel/logs` | Register hourly or room-based work |
| `PUT` | `/api/hotel/logs/{id}/submit` | Submit a work entry for approval |
| `POST` | `/api/auth/register` | Register a basic user account |

## Tests

```bash
./mvnw verify
```

The integration suite verifies migrations, authentication, validation and rejection of anonymous access. The UI is also smoke-tested in a real browser at desktop and mobile breakpoints.

## Future improvements

- Manager planning and employee invitation screens
- Checker workflow and room-level assignments
- Effective-dated pay rates, premiums and monthly approval
- Notification center and queued offline writes
- German payroll estimation from official, versioned rules

## License

MIT
