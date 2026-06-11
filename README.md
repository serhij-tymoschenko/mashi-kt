# mashi

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

* [Ktor Documentation](https://ktor.io/docs/home.html)
* [Ktor GitHub page](https://github.com/ktorio/ktor)
* [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). [Request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).

## Features

Here's a list of features included in this project:

| Name                                                                                  | Description                                                                        |
|---------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| [CORS](https://start.ktor.io/p/io.ktor/server-cors)                                   | Enables Cross-Origin Resource Sharing (CORS)                                       |
| [AsyncAPI](https://start.ktor.io/p/com.asyncapi/server-asyncapi)                      | Generates and serves AsyncAPI documentation                                        |
| [HttpsRedirect](https://start.ktor.io/p/io.ktor/server-https-redirect)                | Redirects insecure HTTP requests to the respective HTTPS endpoint                  |
| [AutoHeadResponse](https://start.ktor.io/p/io.ktor/server-auto-head-response)         | Provides automatic responses for HEAD requests                                     |
| [Request Validation](https://start.ktor.io/p/io.ktor/server-request-validation)       | Adds validation for incoming requests                                              |
| [Content Negotiation](https://start.ktor.io/p/io.ktor/server-content-negotiation)     | Provides automatic content conversion according to Content-Type and Accept headers |
| [kotlinx.serialization](https://start.ktor.io/p/io.ktor/server-kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library                     |
| [Dependency Injection](https://start.ktor.io/p/io.ktor/server-di)                     | Enables dependency injection for your server                                       |
| [Koin](https://start.ktor.io/p/io.insert-koin/server-koin)                            | Provides dependency injection                                                      |
| [PostgreSQL](https://start.ktor.io/p/org.jetbrains/server-postgres)                   | Adds Postgres database support                                                     |

## Building & Running

To build or run the project, use one of the following tasks:

| Task              | Description       |
|-------------------|-------------------|
| `./gradlew test`  | Run the tests     |
| `./gradlew build` | Build the project |
| `./gradlew run`   | Run the server    |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
