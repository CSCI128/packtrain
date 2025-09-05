# 🐴📦 packtrain

> packtrain: a line or succession of pack animals, as mules or burros, used to transport food and supplies over terrain unsuitable for wagons or other vehicles.

packtrain is software for grading and student extension management. It
has a simple interface for instructors to create classes, manage student
extension requests, and migrate grades from external websites to Canvas.
Additionally, it provides full-feature functionality for students requesting
extensions on work.

# Development Environment

Install [Docker](https://www.docker.com) and [Docker Compose](https://docs.docker.com/compose/) and
then copy `.env.sample` to `.env` and generate the following tokens as follows:

```
echo "PG_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> .env
echo "AUTHENTIK_SECRET_KEY=$(openssl rand -base64 60 | tr -d '\n')" >> .env
echo "AUTHENTIK_BOOTSTRAP_PASSWORD=$(openssl rand -base64 36 | tr -d '\n')" >> .env
echo "AUTHENTIK_BOOTSTRAP_TOKEN=$(openssl rand -base64 36 | tr -d '\n')" >> .env
echo "MINIO_ROOT_PASSWORD=$(openssl rand -base64 36 | tr -d '\n')" >> .env
touch certificates/certs/passwords
echo "CA_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> certificates/certs/passwords
echo "KEY_PASS=$(openssl rand -base64 36 | tr -d '\n')" >> certificates/certs/passwords
```

Generate certificates (only needs to be done once per setup): `docker compose up certificates`

Build all services: `docker compose build`

Run the compose: `docker compose up -d`

To run without backend and frontend containers running, run: `docker compose up localhost.dev-local -d`

## Backend

Install [JDK 23](https://www.oracle.com/java/technologies/downloads/#jdk23-mac).

To run the backend on Linux/Mac, enter the `packtrain-service` directory and run: `./mvnw spring-boot:run`

To run the backend tests, run: `./mvnw test "-Dspring.profiles.active=test"`

If you run into an issue building on Windows, make sure that your user has permission to create symlinks.
[StackOverflow thread about this issue](https://stackoverflow.com/a/65504258).

We use [Checkstyle](https://checkstyle.sourceforge.io) with the [Google standard](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/google_checks.xml) to format our code.

Install the Checkstyle for IntelliJ IDEA plugin and then go to `Settings` > `Tools` > `Checkstyle` and add a new configuration file, pointing to the `checkstyle.xml` in the root of the `packtrain-service` directory.

Also, in `Settings` > `Editor` > `Code Style`, you should set hard wrap to `120` and check `Wrap on typing`.

We recommend installing the free version of SonarQube for more intelligent and advanced language suggestions.

## Frontend

Install [Node.js](https://nodejs.org/en) and navigate to the `packtrain-frontend` directory.

Run `npm i` to install all the dependencies.

Run `npm run dev` to launch the development server.

Additionally, copy `.env.sample` to `.env` in **all** of the `packtrain-frontend/apps` directories.

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

### Creating users

To create a user, copy the `user.json.template` in `authentik-sidecar/src/users` to `user.json` and fill in values.

Recreate authentik and you can now login with the information you provided in that user file.

## Certificates

Make sure that you trust the `localhost.dev` certificates when running the backend and frontend.

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

Additionally, add `localhost.dev 127.0.0.1` to your hosts file (google for your specific setup).

Then you can start (and restart) the frontend and backend independently of the docker environment.

## Regenerating Authentik

If you run into issues with Authentik (or we change a config param that requires you to regenerate),
you can regenerate the Authentik config by running:

```bash
docker compose build authentik-sidecar
docker compose run --rm authentik-sidecar --recreate
```

## Postman

1. Create a new collection called `API`.
2. Click the **Auth** tab and change `Auth Type` to `OAuth 2.0`.
3. Name the `Token Name` as anything.
4. Toggle "Authorize using browser" under `Callback URL`.
5. Set `Auth URL` to `https://localhost.dev/auth/application/o/authorize/`.
6. Set `Access Token URL` to `https://localhost.dev/auth/application/o/token/`
7. Set `Client ID` to `grading_admin_provider`.
8. Leave everything else blank and hit **Get New Access Token** at the bottom of the page.

## Issues

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
