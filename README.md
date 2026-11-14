# Journal App

A personal journal with a REST API and a small web page. Each user has private entries that can be tagged, searched and paged through. Entry titles and content are encrypted before they are stored.

Built with Java 25, Spring Boot 4, Spring Security and MongoDB.

## Features

- **Accounts:** sign-up, login with JWT access tokens, refresh tokens, logout on all devices.
- **Journal entries:** create, read, update, delete; tags; keyword, tag and date filters; pagination.
- **Encryption at rest:** titles and content are stored with AES-256-GCM, using a separate key per user.
- **Admin:** list users and create further admins; the first admin is created from environment variables.
- **Safety:** validated input, consistent JSON errors, rate-limited login and sign-up.
- **Tooling:** API docs (Swagger UI), health endpoint, Docker setup, tests that run against a real MongoDB.

## Run it

### With Docker

Needs Docker. Starts the app and its own MongoDB.

```bash
cp .env.example .env
```

Fill in `JWT_SECRET` and `JOURNAL_ENCRYPTION_KEY` in `.env` (the file says how to generate them), then:

```bash
docker compose up --build
```

### Without Docker

Needs JDK 25 and a MongoDB you can reach. Fill in `.env` as above, including `SPRING_MONGODB_URI`, then:

```bash
set -a; source .env; set +a; ./mvnw spring-boot:run
```

Either way, the app is then at:

| Address | What |
|---|---|
| http://localhost:8080/ | The web page |
| http://localhost:8080/swagger-ui.html | API docs, with a button to try requests |
| http://localhost:8080/actuator/health | Health check (includes the database) |

## Configuration

Everything is set through environment variables. `.env.example` lists them with instructions.

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `SPRING_MONGODB_URI` | yes | `mongodb://localhost:27017/test` | MongoDB connection string |
| `JWT_SECRET` | yes, in practice | random per start | Signs access tokens; at least 32 characters. Without it, every restart signs everyone out. |
| `JOURNAL_ENCRYPTION_KEY` | recommended | off | 32 random bytes, base64. Without it, entries are stored unencrypted. |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | no | none | Creates this admin at startup if the user does not exist |
| `JWT_EXPIRATION_MS` | no | `900000` (15 minutes) | Lifetime of an access token |
| `JWT_REFRESH_EXPIRATION_DAYS` | no | `7` | Lifetime of a refresh token |
| `RATELIMIT_AUTH_MAX_REQUESTS` | no | `10` | Login, sign-up and refresh calls allowed per client address per window |
| `RATELIMIT_AUTH_WINDOW_SECONDS` | no | `60` | Length of that window |

Keep a backup of `JOURNAL_ENCRYPTION_KEY`. If it is lost or changed, entries written with it can no longer be read.

## API

Send the access token on every non-public request as `Authorization: Bearer <token>`.

| Method and path | Access | What it does |
|---|---|---|
| `POST /public/createUser` | anyone | Sign up with `userName` and `password` (8 to 72 characters) |
| `POST /public/login` | anyone | Returns `token` (access) and `refreshToken` |
| `POST /public/refresh` | anyone | Trades a `refreshToken` for a new pair; each refresh token works once |
| `GET /public/health-check` | anyone | Returns `OK` |
| `GET /user` | logged in | The current user |
| `PUT /user` | logged in | Change `userName` and/or `password`; a new password signs out every device |
| `POST /user/logout` | logged in | Signs out every device |
| `DELETE /user` | logged in | Deletes the account and its entries |
| `GET /journal` | logged in | Own entries, newest first; see filters below |
| `POST /journal` | logged in | Create an entry: `title`, optional `content` and `tags` |
| `GET /journal/id/{id}` | logged in | One entry |
| `PUT /journal/id/{id}` | logged in | Update any of `title`, `content`, `tags` |
| `DELETE /journal/id/{id}` | logged in | Delete an entry |
| `GET /admin/allUsers` | admin | All users |
| `POST /admin/create-admin-users` | admin | Create another admin |

`GET /journal` takes these optional query parameters:

| Parameter | Meaning |
|---|---|
| `q` | Keyword in the title or content, case-insensitive |
| `tag` | Exact tag, case-insensitive |
| `from`, `to` | Dates such as `2026-01-31`, inclusive |
| `page`, `size` | Page number from 0, and page size (default 20, at most 100) |

It returns `{ "content": [...], "page", "size", "totalElements", "totalPages" }`.

Errors come back as JSON with `title`, `status` and `detail`; validation errors add an `errors` object with one message per field.

## Security notes

- **Passwords** are stored as BCrypt hashes and never returned.
- **Access tokens** are short-lived. Logging out or changing the password invalidates every token the user holds.
- **Refresh tokens** are stored hashed and rotate on every use.
- **Encryption** is at rest, with the key held by the server. It protects a leaked database or backup; it is not end-to-end encryption, and someone who controls the running server can read entries. Tags and dates are stored in plain text so the database can filter on them.
- **Keyword search** runs in the app after decryption, so it loads all of the user's entries that match the tag and date filters. That is fine for a personal journal and would need a different design for very large ones.
- **Rate limiting** is kept in memory per app instance. Behind a proxy, set `SERVER_FORWARD_HEADERS_STRATEGY=native` so the real client address is counted.
- **API docs** are public by default. Set `SPRINGDOC_API_DOCS_ENABLED=false` and `SPRINGDOC_SWAGGER_UI_ENABLED=false` to turn them off.

## Tests

```bash
./mvnw verify
```

The tests download and start their own MongoDB the first time, so they need no database or Docker.

## Upgrading from the earlier version

- **Database setting renamed.** Spring Boot 4 reads `spring.mongodb.uri` (or `SPRING_MONGODB_URI`). The old `spring.data.mongodb.uri` key is ignored, so an existing `application.yml` must be updated or the app will try to connect to `localhost`.
- **No profile needed.** The separate `dev` and `prod` security setups are gone; the same rules apply everywhere.
- **Existing data is migrated at startup.** Entries used to be listed on the user document; each entry now stores its owner instead. The first start moves old data to the new shape. Old entries stay unencrypted until they are next edited.
- **MongoDB transactions are no longer used**, so a standalone MongoDB works.
- **Login changed.** Basic auth is replaced by `POST /public/login` and bearer tokens.
- **Responses changed.** IDs are plain strings, `GET /journal` returns a page object instead of a bare list, and creating a user or entry returns `201`.
