# Roomly Work

Aplicație Java/Spring Boot pentru planificarea și monitorizarea muncii într-un hotel: angajați, manageri, checker, plan săptămânal, camere, istoric, salarizare estimativă și grafice.

## Pornire locală

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Deschide: <http://localhost:8080>

Conturi demo:

- `mariana` / `demo1234`
- `manager` / `manager1234`
- `checker` / `checker1234`
- `angajator` / `admin1234`

## Pornire cu Docker + PostgreSQL

```powershell
docker compose up --build
```

Aplicația pornește pe <http://localhost:8080>, iar baza PostgreSQL pe portul `5432`.

## Module principale

- Employee workflow: plan personal, istoric lunar, camere, cereri, profil, salarizare estimativă.
- Manager workflow: calendar echipă, editare ture, copiere săptămână, repartizare camere, aprobări.
- Checker workflow: verificare camere, defecte, eliberare pentru recepție.
- Admin workflow: angajați, roluri, echipe, activare/dezactivare, invitații, setări hotel, task-uri și sporuri.

## Tehnologii

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Flyway migrations
- H2 pentru local demo
- PostgreSQL pentru deployment
- Frontend vanilla HTML/CSS/JS cu PWA manifest
