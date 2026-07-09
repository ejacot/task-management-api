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

## Ce este pregatit pentru productie

- Autentificare cu Bearer token, lockout dupa incercari gresite si secret configurabil prin `ROOMLY_TOKEN_SECRET`.
- Import Excel cu preview inainte de salvare.
- Coada `outbound_messages` pentru email, SMS si push. Pentru trimitere reala trebuie conectat un provider: SMTP/SendGrid pentru email, Twilio/Vonage pentru SMS si VAPID pentru Web Push.
- PWA instalabila pe telefon, cu service worker si manifest.
- Calcul salarial orientativ cu sporuri de duminica, noapte si sarbatori legale germane/Bavaria.
- Workflow camere: repartizare, terminat de angajat, verificat, defect, defect in lucru, rezolvat, eliberat.

## Checklist deploy real

1. Copiaza `.env.example` in `.env` si schimba toate parolele/secretele.
2. Ruleaza PostgreSQL cu backup automat.
3. Porneste aplicatia cu `docker compose --env-file .env up --build -d`.
4. Configureaza HTTPS prin reverse proxy, de exemplu Caddy/Nginx.
5. Conecteaza worker/provider pentru randurile din `outbound_messages`.
6. Ruleaza testele inainte de deploy: `.\mvnw.cmd test`.
