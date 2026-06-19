# VisionBoard API Rest Server
### (VisionBoard Administration Service)

This is a backend server for the VisionBoard system. It provides RESTful APIs to manage tournaments, rounds, matches and users. 
The server is built using Ktor, a Kotlin framework for building asynchronous servers.

The server uses a PostgreSQL database to store data, and the Exposed library to interact with the database. 
The server also includes CORS support to allow cross-origin requests from the frontend application.

It also includes a set of unit tests to ensure the correctness of the APIs and services.

This API also integrates with external services such as Lichess.org or Firebase. Lichess to broadcast the tournaments of VisionBoard and Firebase to manage the authentication of users.

This server has also a communication layer as a client using gRPC to communicate with the VisionBoard Detection Service, which is responsible for detecting the moves of the players in the tournaments. This connection is capable of editing and adding moves to the matches of the tournaments.

The server has also been configured to run in a Docker container, making it easy to deploy and run in different environments.

### Features
- RESTful APIs for managing tournaments, rounds, matches and users.
- Integration with Lichess.org for broadcasting tournaments.
- Integration with Firebase for user authentication.
- gRPC client for communicating with the VisionBoard Detection Service.
- CORS support for cross-origin requests.
- Unit tests for ensuring the correctness of the APIs and services.
- Docker support for easy deployment and running in different environments.

### Installation
To run the VisionBoard API Rest Server, you need to have Docker installed on your machine.

Once you have Docker installed, you need to get this repository:

```bash
git clone repo_url
```
And then, get the serviceAccountKey.json file from the Firebase project and place it in the root directory of the project.

Then, you can build and run the Docker container using the following command:

```bash
docker build -t visionboard-api .
docker run -d --network host --name visionboard-api -e DB_URL="db_url" -e DB_USER="db_username" -e DB_PASSWORD="db_password" -e LICHESS_API_TOKEN="api_token_of_lichess" -e SECRET_KEY="secret_key_autentication_of_detection_service" visionboard-api 
```

