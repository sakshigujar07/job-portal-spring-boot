# Job Portal – Spring Boot REST API

A job portal backend where employers post jobs and job seekers apply to them. Built with Spring Boot, Spring Security (JWT), JPA/Hibernate and MySQL.

> The React frontend is in a separate repository: [job-portal-frontend](https://github.com/sakshigujar07/job-portal-frontend)

## Features

**For everyone**
- Register and log in. Passwords are stored with BCrypt. Login returns a JWT token.
- Role based access: `employer` and `jobseeker`.

**For employers**
- Create, edit and delete their own jobs (title, description, salary, vacancy, employment type, work arrangement, deadline, notes).
- Company profile linked to their jobs.
- Paginated list of their own jobs, and a job count for the dashboard.
- See applications for their own jobs and move them through the hiring steps.

**For job seekers**
- Browse jobs and apply with an optional resume (PDF, DOC or DOCX, up to 5 MB).
- Cannot apply twice to the same job.
- Get a notification when an application status changes.
- Delete their own applications.

**Application status flow**

```
PENDING  ->  SHORTLISTED  ->  HIRED
   |              |
   +-> REJECTED <-+
```

`HIRED` and `REJECTED` are final. Any other change is rejected with `400 Bad Request`.

**Security rules that are enforced**
- A job seeker cannot create a job (`403 Forbidden`).
- An employer can only update or delete their own jobs, and only update applications for their own jobs (`403 Forbidden`).
- A job seeker can only delete their own applications (`403 Forbidden`).
- Validation errors and access errors return clear JSON/text messages through a global exception handler.

## Tech stack

- Java 24, Spring Boot 4.1
- Spring Web, Spring Data JPA (Hibernate), Spring Security, Bean Validation
- JWT (jjwt)
- MySQL 8
- Maven (wrapper included)

## Getting started

### 1. What you need
- Java 24 (or the version set in `pom.xml`)
- MySQL 8 running on `localhost:3306`

### 2. Clone the project
```
git clone https://github.com/sakshigujar07/job-portal-spring-boot.git
cd job-portal-spring-boot
```

### 3. Create the database
In MySQL (Workbench or command line):
```sql
CREATE DATABASE jobportal;
```
The tables are created automatically when the app starts (`spring.jpa.hibernate.ddl-auto=update`).

### 4. Add your secrets
Secrets are **not** in this repository. The app reads them from a local file that git ignores.

1. Go to `src/main/resources`.
2. Copy `application-local.properties.example` and name the copy `application-local.properties`.
3. Open it and fill in your own values:
   ```
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   jwt.secret=A_RANDOM_STRING_OF_AT_LEAST_32_CHARACTERS
   ```

`application.properties` already sets `spring.profiles.active=local`, so this file is loaded automatically. If your MySQL user is not `root`, also add `spring.datasource.username=your_user` to your local file.

### 5. Run
```
./mvnw spring-boot:run
```
On Windows you can also use `mvnw.cmd spring-boot:run`, or run `JobportalApplication` from your IDE.

The API starts on `http://localhost:8080`.

Uploaded resumes are saved in `uploads/resumes` (created automatically, not committed to git).

## API overview

Send the token on protected requests:
```
Authorization: Bearer <token>
```

### Users
| Method | Endpoint | Notes |
|---|---|---|
| POST | `/api/users` | Register (name, email, password, role, contact, address) |
| POST | `/api/users/login` | Returns a JWT token |
| PUT | `/api/users/{id}` | Update your own account |
| DELETE | `/api/users/{id}` | Delete your own account |

### Jobs
| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/jobs?page=0&size=10` | Paginated job list |
| GET | `/api/jobs/{id}` | Job details |
| POST | `/api/jobs` | Employer only |
| PUT | `/api/jobs/{id}` | Employer, own jobs only |
| DELETE | `/api/jobs/{id}` | Employer, own jobs only |
| GET | `/api/jobs/my?page=0&size=10` | Employer: own jobs, paginated |
| GET | `/api/jobs/my/count` | Employer: number of own jobs |

### Applications
| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/applications` | Job seeker: own applications. Employer: applications for own jobs |
| POST | `/api/applications` | Job seeker only. `multipart/form-data` with `jobId` and optional `resume` |
| PUT | `/api/applications/{id}` | Employer only. Body: `{ "status": "SHORTLISTED" }` |
| DELETE | `/api/applications/{id}` | Job seeker, own applications only |

Companies and notifications also have their own controllers (`CompanyController` and the notification endpoints).

A Postman collection is included in the repository root.

## Tests

```
./mvnw test
```

Tests cover:
- Login (`UserControllerLoginTest`)
- Job creation: employer success, job seeker forbidden, no token, blank title validation (`JobControllerCreateTest`)

The tests start the Spring context, so MySQL must be running and `application-local.properties` must exist.

## Author

Sakshi Gujar – [GitHub](https://github.com/sakshigujar07)
