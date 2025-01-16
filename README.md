# Grading Admin

The 128 grading management and migration website monorepo.

# Development Environment

All development environments will have a Docker Compose which launches
them, but to run them individually, read the next sections for directions.

Rename `.env.sample` to `.env` and set `PG_PASS` and `AUTHENTIK_SECRET_KEY` in `.env` as follows:
```
echo "PG_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> .env
echo "AUTHENTIK_SECRET_KEY=$(openssl rand -base64 60 | tr -d '\n')" >> .env
echo "AUTHENTIK_BOOTSTRAP_PASSWORD=$(openssl rand -base64 36 | tr -d '\n')" >> .env
echo "AUTHENTIK_BOOTSTRAP_TOKEN=$(openssl rand -base64 36 | tr -d '\n')" >> .env
```
Create a file called `passwords` in the `certificates/certs/` directory. Run the following commands to populate that file:
```
echo "CA_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> certificates/certs/passwords
echo "KEY_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> certificates/certs/passwords
```

Generate certificates before running the full compose (only needs to be done once per setup): `docker compose up certificates`

Build all services: `docker compose build`

Run the compose: `docker compose up -d`

Add `localhost.dev 127.0.0.1` to your hosts file.
(the process varies for windows and macos, I would google how to edit the hosts file for your specific platform)

## Frontend

Install [JDK 23](https://www.oracle.com/java/technologies/downloads/#jdk23-mac) and [Docker Desktop](https://www.docker.com).

To run the backend on Linux/Mac, run: `./mvnw spring-boot:run`

## Backend
Install [Node.js](https://nodejs.org/en) and navigate to the `grading-admin-web` directory.

Run `vite` to launch the development server.

### Authentik

If authentik doesn't start correctly, follow this guide

You can sign in at `https://localhost.dev/auth/` with the username `akadmin` and the password that you set in the `.env` file.

If everything is working, `https://localhost.dev/auth/` should be the authentik login URL.

Create a new application, it can be any name, but it must have the slug `grading-admin`.
Create a new provider called `grading-admin` with the following settings.

Client type: `public`

Redirect URIs: `regex`, `https://localhost.dev/.*`
`strict`, `https://oauth.pstmn.io/v1/callback`

Finally, assign that provider to the application that you just created.



## Services

### Primary Services

#### Backend

This is the REST API for the grading admin application.

This is where all the business logic lives.

All requests must be made with Bearer JWTs.
These JWTs are validated against Authentik.
Importantly, the service does not do any authentication.

#### Frontend

This is the web user interface for the grading admin application.

It gets an access key from Authentik via OAuth, then makes authenticated requests against the rest API.

#### Authentik

This is the Authentication server / identity broker that manages the integration between all identity providers 
(like Canvas / Azure).
It also is the issuer for all tokens and manages what scopes users have access to.

### Utilities

#### Authentik-Sidecar

This is a simple Python script that makes back channel requests against Authentik to configure it.

If it detects that something is already configured, then it does not take any action.

In the future, this will also pre-seed it with users with varying scopes.

#### Certificates

This is a simple bash script that creates a self-signed certificate chain for the application.

If it detects that certs have already been generated, then it takes no action.

