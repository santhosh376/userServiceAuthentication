## User Service Authentication

User Service is a Spring Boot–based backend application that provides user registration, authentication, and session management using JWTs and a MySQL database.

### Features

- **User registration (sign-up)**: Create new users with unique email addresses and securely hashed passwords.
- **User login**: Authenticate existing users and issue a signed JWT token for subsequent requests.
- **Session tracking**: Persist issued tokens as `Session` records with expiration timestamps.
- **Token validation**: Validate JWT tokens issued by the service and check whether they are still valid.

---

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3 (Web, Data JPA)
- **Database**: MySQL
- **Database migrations**: Flyway
- **ORM**: Spring Data JPA / Hibernate
- **JWT**: JJWT (`io.jsonwebtoken`)
- **Build tool**: Maven
- **Utilities**: Lombok

---

## Project Structure (Core Packages)

- **`org.example.userservice`**
  - Main Spring Boot application entry point (`UserServiceApplication`).

- **`org.example.userservice.controllers`**
  - **`AuthController`**: Exposes REST endpoints for:
    - `POST /auth/sign_up`
    - `POST /auth/login`
    - `GET /auth/validate`

- **`org.example.userservice.services`**
  - **`AuthService`**: Core business logic for:
    - Registering new users.
    - Authenticating users during login.
    - Generating JWT tokens.
    - Persisting and validating sessions.

- **`org.example.userservice.model`**
  - **`BaseModel`**: Common base entity with ID.
  - **`User`**: Represents application users (email, password, roles).
  - **`Role`**: Represents a user role (e.g. `ADMIN`, `USER`).
  - **`Session`**: Represents a login session with a JWT token, expiry, and status.
  - **`SessionStatus`**: Enum describing session state (e.g. active, ended).

- **`org.example.userservice.repository`**
  - **`UserRepository`**: JPA repository for `User` entities, includes `findByEmail`.
  - **`SessionRepository`**: JPA repository for `Session` entities.

- **`org.example.userservice.dtos`**
  - **`SignUpRequestDto` / `SignUpResponseDto`**
  - **`LoginRequestDto` / `LoginResponseDto`**
  - **`RequestStatus`**: Enum used to indicate success or failure in responses.

- **`org.example.userservice.configs`**
  - **`MyBeans`**: Shared application beans such as password encoder.
  - Other configuration classes for application-level setup.

- **`org.example.userservice.exception`**
  - **`UserAlreadyExistException`**
  - **`UserNotFoundException`**
  - **`WrongPasswordException`**

---

## Database Schema (Core Tables)

Flyway migrations in `src/main/resources/db/migration` create the core tables:

- **`user`**
  - Stores user accounts, including email and hashed password.

- **`role`**
  - Stores available roles.

- **`user_roles`**
  - Join table mapping users to roles (many-to-many).

- **`session`**
  - Stores issued JWT tokens with:
    - Token value
    - Expiration timestamp
    - Associated user
    - Session status

Additional tables may exist for internal infrastructure, but the above are the key application tables for user and session management.

---

## API Endpoints

All endpoints are prefixed with `/auth`.

### 1. Sign Up

- **URL**: `POST /auth/sign_up`
- **Description**: Register a new user.
- **Request body** (`application/json`):

  ```json
  {
    "email": "user@example.com",
    "password": "plainTextPassword"
  }
  ```

- **Response body** (`application/json`):

  ```json
  {
    "requestStatus": "SUCCESS"
  }
  ```

  or

  ```json
  {
    "requestStatus": "FAILURE"
  }
  ```

- **Behavior**:
  - Returns failure if a user with the same email already exists.
  - On success, stores the user with a BCrypt-hashed password.

---

### 2. Login

- **URL**: `POST /auth/login`
- **Description**: Authenticate a user and obtain a JWT token.
- **Request body** (`application/json`):

  ```json
  {
    "email": "user@example.com",
    "password": "plainTextPassword"
  }
  ```

- **Successful response**:
  - **HTTP status**: `200 OK`
  - **Headers**:
    - `AUTH_TOKEN: <jwt-token>`
  - **Body** (`application/json`):

    ```json
    {
      "requestStatus": "SUCCESS"
    }
    ```

- **Failure response**:
  - **HTTP status**: e.g. `400 BAD_REQUEST`
  - **Body**:

    ```json
    {
      "requestStatus": "FAILURE"
    }
    ```

- **Behavior**:
  - Looks up the user by email.
  - Verifies the password using BCrypt.
  - On success:
    - Generates a signed JWT containing user details.
    - Persists a `Session` record with an expiration time (e.g. 30 days).
    - Returns the token in the `AUTH_TOKEN` header.

---

### 3. Validate Token

- **URL**: `GET /auth/validate?token=<jwt-token>`
- **Description**: Validate a previously issued JWT token.
- **Query parameter**:
  - `token` – the JWT token to validate.
- **Response body** (`application/json`):

  ```json
  true
  ```

  or

  ```json
  false
  ```

- **Behavior**:
  - Parses and verifies the JWT token.
  - Returns `true` if the token is valid and not expired, otherwise `false`.

---

## Configuration

Application configuration is primarily managed via `src/main/resources/application.properties`.

Typical properties include:

- **Server**
  - `server.port=8080`

- **Database**
  - `spring.datasource.url=jdbc:mysql://localhost:3306/userService`
  - `spring.datasource.username=<db_username>`
  - `spring.datasource.password=<db_password>`
  - `spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver`

- **JPA / Hibernate**
  - `spring.jpa.hibernate.ddl-auto=validate` (or another appropriate strategy)
  - Additional JPA settings as required.

You can override these properties via environment variables or command-line arguments when starting the application.

---

## Running the Application Locally

### Prerequisites

- **JDK**: Java 21
- **Build tool**: Maven 3.x
- **Database**: MySQL running locally with:
  - A database named `userService` (or adjust the URL accordingly).
  - A user and password matching the configured datasource credentials.

### Steps

1. **Configure the database**

   - Create a MySQL database:

     ```sql
     CREATE DATABASE userService;
     ```

   - Ensure the user and password in `application.properties` (or environment variables) are valid for your MySQL instance.

2. **Build the project**

   ```bash
   mvn clean package
   ```

3. **Run with Maven**

   ```bash
   mvn spring-boot:run
   ```

   or run the built JAR:

   ```bash
   java -jar target/userService-0.0.1-SNAPSHOT.jar
   ```

4. **Verify**

   - The application should start on the configured port (default `8080`).
   - You can then call the `/auth/sign_up`, `/auth/login`, and `/auth/validate` endpoints using a REST client (e.g. Postman, curl).

---

## Database Migrations

- **Tool**: Flyway
- **Location**: `src/main/resources/db/migration`
- **Behavior**:
  - On startup, Flyway automatically applies pending migration scripts.
  - Migrations define the schema for users, roles, sessions, and supporting tables.

---

## Testing

- **Test framework**: JUnit with Spring Boot test support.
- **Location**: `src/test/java/org/example/userservice`
- **Usage**:

  ```bash
  mvn test
  ```

---

## Further Improvements

- **Error handling**: Centralized exception handling for cleaner error responses.
- **Validation**: Input validation on DTOs (e.g. email format, password strength).
- **Observability**: Logging, metrics, and tracing to monitor authentication flows.
- **Configuration**: Externalize sensitive configuration (secrets, keys) using environment variables or a secret manager.

