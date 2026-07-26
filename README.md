# Job Portal API

A REST API for a job portal, built with Spring Boot. Employers can post jobs and manage applications; jobseekers can browse jobs, apply, and build a profile. Built as a Java/Spring Boot rewrite of an earlier PHP/MySQL project, aimed at demonstrating backend fundamentals for interviews.

## Tech Stack

- Java 21, Spring Boot
- Spring Security + JWT (role-based access control)
- Spring Data JPA + MySQL
- BCrypt password hashing
- Bean Validation (Jakarta Validation)
- Maven

## Features

- **Authentication**: JWT-based login/signup with BCrypt password hashing
- **Role-based access control**: separate permissions for `employer` and `jobseeker` roles
- **Job postings**: employers create/edit/delete their own jobs; anyone (authenticated) can browse
- **Applications**: jobseekers apply to jobs; employers manage applications for their own postings
- **Application workflow**: enforced status transitions — `PENDING → SHORTLISTED/REJECTED`, `SHORTLISTED → HIRED/REJECTED`; `HIRED`/`REJECTED` are final states
- **Auto-notifications**: applicants are automatically notified when their application status changes
- **Profiles**: jobseekers manage a profile (skills, bio, resume path); employers can view (not edit) any jobseeker's profile
- **Input validation**: field-level validation on all create/update requests, with clean JSON error responses
- **Centralized exception handling**: validation errors, duplicate-data conflicts, and other runtime errors all return consistent, structured JSON

## Project Structure

Single flat package (`com.sakshi.jobportal`) containing:
- **Entities**: `User`, `Job`, `Application`, `Profile`, `Notification`
- **Controllers**: `UserController`, `JobController`, `ApplicationController`, `ProfileController`, `NotificationController`
- **Request DTOs**: `RegisterRequestDTO`, `JobRequestDTO`, `ApplicationRequestDTO`, `ApplicationStatusUpdateDTO`, `ProfileRequestDTO`
- **Response DTOs**: `UserResponseDTO` (excludes password hash from API responses)
- **Security**: `SecurityConfig`, `JwtFilter`, `JwtUtil`
- **Error handling**: `GlobalExceptionHandler`

## Getting Started

### Prerequisites
- Java 21
- MySQL running locally
- Maven

### Setup
1. Create a MySQL database named `jobportal`.
2. Configure `src/main/resources/application.properties` with your MySQL username/password and a JWT secret (at least 32 characters).
3. Run the app:
   ```
   mvn spring-boot:run
   ```
4. The API will be available at `http://localhost:8080`.

## API Endpoints

### Users (`/api/users`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/users` | Public | Register (signup) |
| POST | `/api/users/login` | Public | Login, returns JWT |
| GET | `/api/users` | Authenticated | List all users |
| PUT | `/api/users/{id}` | Authenticated | Update user |
| DELETE | `/api/users/{id}` | Authenticated | Delete user |

### Jobs (`/api/jobs`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/jobs` | Authenticated | List all jobs |
| POST | `/api/jobs` | Employer | Create a job posting |
| PUT | `/api/jobs/{id}` | Employer (own jobs only) | Update a job posting |
| DELETE | `/api/jobs/{id}` | Employer (own jobs only) | Delete a job posting |

### Applications (`/api/applications`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/applications` | Authenticated | List own applications (jobseeker) or applications to own jobs (employer) |
| POST | `/api/applications` | Jobseeker | Apply to a job |
| PUT | `/api/applications/{id}` | Employer (own jobs only) | Update application status (enforces valid transitions; triggers a notification to the applicant) |
| DELETE | `/api/applications/{id}` | Jobseeker (own applications only) | Withdraw an application |

### Profiles (`/api/profiles`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/profiles/create` | Authenticated | Create own profile (one per user) |
| GET | `/api/profiles/me` | Authenticated | Get own profile |
| GET | `/api/profiles/user/{userId}` | Own profile, or any profile if employer | View a profile |
| PUT | `/api/profiles/update/{id}` | Owner only | Update own profile |
| DELETE | `/api/profiles/delete/{id}` | Owner only | Delete own profile |

### Notifications (`/api/notifications`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/notifications/me` | Authenticated | Get own notifications |
| PUT | `/api/notifications/read/{id}` | Owner only | Mark a notification as read |
| DELETE | `/api/notifications/delete/{id}` | Owner only | Delete a notification |

## Application Status Workflow

```
PENDING ──▶ SHORTLISTED ──▶ HIRED
   │              │
   └──────────────┴──────▶ REJECTED
```

`HIRED` and `REJECTED` are final states — no further transitions are allowed once reached.

## Known Limitations / Next Steps

- No frontend yet — this is a backend-only API, tested via Postman.
- Auto-notification currently covers application status changes only; job-posted broadcast notifications are not yet implemented.
- Exception handling uses a broad `RuntimeException` catch-all rather than specific custom exceptions (e.g. `UserNotFoundException`) — a production-grade improvement for later.
