# URL Shortener

A small URL shortener built with Java, Spring Boot and PostgreSQL. There is a simple page for creating and copying short links, and an API for updating, deleting and checking them.

## Run locally

You need JDK 21 or newer and Docker. Set `JAVA_HOME` to your JDK and start Docker before running these commands from the project folder.

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

On Linux or macOS, use `./mvnw` instead of `.\mvnw.cmd`.

Open [localhost:8080](http://localhost:8080), paste a URL and click **Shorten**. **Copy** copies the short link. Opening it redirects to the original address.

PostgreSQL runs on port `5432`. The default database is `url_shortener`, with `shortener` as both the username and password. These defaults are for local development. The app also accepts `DB_URL`, `DB_USER` and `DB_PASSWORD` environment variables.

## API

| Method | Path | What it does |
| --- | --- | --- |
| POST | `/shorten` | Create a short link |
| GET | `/shorten/{code}` | Get the original URL and count an access |
| PUT | `/shorten/{code}` | Update the original URL |
| DELETE | `/shorten/{code}` | Delete the link |
| GET | `/shorten/{code}/stats` | Get link details and `accessCount` |
| GET | `/s/{code}` | Redirect to the original URL and count an access |

POST and PUT accept JSON:

```json
{
  "url": "https://example.com/some/long/path"
}
```

Only HTTP and HTTPS URLs are accepted. Short codes are six random letters and digits. Creating a link returns `201`, updating or reading it returns `200`, and deleting it returns `204`. Invalid URLs return `400`; missing links return `404`.

Reading stats does not increase the access count. Redirects use `302` and are not cached, so changing the original URL takes effect on the next visit.

## Tests

Keep PostgreSQL running, then run:

```powershell
.\mvnw.cmd test
```

The tests cover the API, validation, redirects and concurrent access counting. They use the configured database, so use a local development database.

Links are stored in a Docker volume and survive container restarts. Run `docker compose stop` to stop the database without removing it.

There is no authentication. The app currently runs locally; sharing a `localhost` link will not make it accessible from another computer.
