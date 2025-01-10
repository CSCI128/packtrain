# Grading Admin

The 128 grading management and migration website monorepo.

# Development Environment

All development environments will have a Docker Compose which launches
them, but to run them individually, read the next sections for directions.

Rename `.env.sample` to `.env` and set `PG_PASS` and `AUTHENTIK_SECRET_KEY` in `.env` as follows:
```
echo "PG_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> .env
echo "AUTHENTIK_SECRET_KEY=$(openssl rand -base64 60 | tr -d '\n')" >> .env
```
Create a file called `passwords` in the `certificates/certs/` directory. Run the following commands to populate that file:
```
echo "CA_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> .env
echo "KEY_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> .env
```

Generate certificates before running the full compose (only needs to be done once per setup): `docker compose up certificates`

Build all services: `docker compose build`

Run the compose: `docker compose up -d`

## Frontend

Install [JDK 23](https://www.oracle.com/java/technologies/downloads/#jdk23-mac) and [Docker Desktop](https://www.docker.com).

To run the backend on Linux/Mac, run: `./mvnw spring-boot:run`

## Backend
Install [Node.js](https://nodejs.org/en) and navigate to the `grading-admin-web` directory.

Run `vite` to launch the development server.

### Authentik

Setup authentik at this address: `https://localhost.dev/auth/if/flow/initial-setup/`

If everything is working, `https://localhost.dev/auth/` should be the authentik login URL.
