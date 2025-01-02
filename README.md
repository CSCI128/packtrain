# Grading Admin

The 128 grading management and migration website monorepo.

# Setup TODO
- Authorization & authentication things on backend (auth provider https://goauthentik.io)
    - get it set up with spring and the school azure
- Setup proxy for local development
- Docker compose things
- OpenAPI stuff

# Development Environment

All development environments will have a Docker Compose which launches
them, but to run them individually, read the next sections for directions.

To run the project all at once, run: `docker compose up -d`

## Frontend

Install [JDK 23](https://www.oracle.com/java/technologies/downloads/#jdk23-mac) and [Docker Desktop](https://www.docker.com).

To run the backend on Linux/Mac, run: `./mvnw spring-boot:run`

## Backend
Install [Node.js](https://nodejs.org/en) and navigate to the `grading-admin-web` directory.

Run `vite` to launch the development server.
