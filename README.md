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
echo "MINIO_ROOT_PASSWORD=$(openssl rand -base64 36 | tr -d '\n')" >> .env
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

## Backend

Install [JDK 23](https://www.oracle.com/java/technologies/downloads/#jdk23-mac) and [Docker Desktop](https://www.docker.com).

To run the backend on Linux/Mac, run: `./mvnw spring-boot:run`

If you run into an issue building on windows, make sure that your user has permission to create symlinks.
[StackOverflow thread about this issue](https://stackoverflow.com/a/65504258).

### Running Backend Tests

`./mvnw test "-Dspring.profiles.active=test"`

## Frontend

Install [Node.js](https://nodejs.org/en) and navigate to the `grading-admin-web` directory.

Run `npm i` to install all the dependencies.

Run `npm run dev` to launch the development server.

To rebuild TypeScript types from OpenAPI, run `npm run types`.

## Authentik

If authentik doesn't start correctly, follow this guide.

You can sign in at `https://localhost.dev/auth/` with the username `akadmin` and the password that you set in the `.env` file.

If everything is working, `https://localhost.dev/auth/` should be the authentik login URL.

Create a new application, it can be any name, but it must have the slug `grading-admin`.
Create a new provider called `grading-admin` with the following settings.

Client type: `public`

Redirect URIs: `regex`, `https://localhost.dev/.*`
`strict`, `https://oauth.pstmn.io/v1/callback`

Finally, assign that provider to the application that you just created.

## Creating users

To create a user, copy the `user.json.template` in `authentik-sidecar/src/users` to `user.json` and fill in values.

Recreate authentik and you can now login with the information you provided in that user file.

## Running locally

You can run locally from your IDE (so you can have debugging support) with all the services!

To do this, make sure that you trust the `localhost.dev` certificate.

This is done with
macOS:

```bash
keytool -import -alias grading-admin-ca -keystore $(/usr/libexec/java_home)/lib/security/cacerts -file certificates/certs/localhost-root/localhost-root.CA.pem
```

Windows:

```powershell
keytool -import -alias grading-admin-ca -keystore "C:\Program Files\Java\jdk-23/lib/security/cacerts" -file certificates/certs/localhost-root/localhost-root.CA.pem
```

(If you get an "access denied" error, run this in an admin PowerShell instance.
You also may need to adjust the path to your `cacerts` file)

And then saying 'yes' to the prompt asking to trust that certificate.

Then make sure to define the `PG_PASS` env variable in your IDE's run config.
(in Intellij it is under `Modify Options` -> `Environmental Varibles`. Then set `PG_PASS=<whatever the .env file says>`)

Finally, start all the dependencies with

```bash
docker compose up localhost.dev-local -d
```

This starts all the local services and proxies them behind `https://localhost.dev`.

Then you can start (and restart) the frontend and backend independently of the docker environment.

### Issues

If you run into issues relating to your DB password not working,
ensure that there are no other postgres instances running on your computer.

You can check this on macOS by running

```bash
sudo lsof -i tcp:5432
```

Then you can kill those processes by running

```bash
sudo kill -9 <pid of process to kill>
```

## Regenerating Authentik

If you run into issues with Authentik (or we change a config param that requires you to regenerate),
you can regenerate the Authentik config by running:

```bash
docker compose build authentik-sidecar
docker compose run --rm authentik-sidecar --recreate
```

## Postman

1. Create a new collection (called anything, should do something like `API`).
2. Click the **Auth** tab and change `Auth Type` to `OAuth 2.0`.
3. Name the `Token Name` as anything.
4. Toggle "Authorize using browser" under `Callback URL`.
5. Set `Auth URL` to `https://localhost.dev/auth/application/o/authorize/`.
6. Set `Access Token URL` to `https://localhost.dev/auth/application/o/token/`
7. Set `Client ID` to `grading_admin_provider`.
8. Leave everything else blank and hit **Get New Access Token** at the bottom of the page.

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
