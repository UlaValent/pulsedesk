# PulseDesk

PulseDesk is a Spring Boot backend that accepts customer comments, sends them to the Hugging Face Inference API to decide whether a comment should be turned into a ticket, and if so,
generates a basic structured ticket data.

Current capabilities:
- Create and list comments
- Run AI triage for each new comment
- Auto-create a ticket when triage says yes
- List tickets and fetch a ticket by id
- Return structured API errors for common failures (400, 404)

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- H2 in-memory database
- Hugging Face Inference API

## Get a Hugging Face API Key

1. Create or sign in to your Hugging Face account at https://huggingface.co/
2. Open Settings, then Access Tokens.
3. Create a token with inference permissions.
4. Copy the token value (it usually starts with hf_).

## Configuration

Application config is in src/main/resources/application.properties.

Important properties:
- huggingface.api.key=${HUGGINGFACE_API_KEY:}
- huggingface.model.url=https://api-inference.huggingface.co/models/google/flan-t5-large

The API key is read from the HUGGINGFACE_API_KEY environment variable.

### Set environment variable

PowerShell (current terminal session):

```powershell
$env:HUGGINGFACE_API_KEY="hf_your_real_token_here"
```

PowerShell (persist for your user profile):

```powershell
setx HUGGINGFACE_API_KEY "hf_your_real_token_here"
```

After setx, open a new terminal before running the app.

## Run Locally

From the project root:

```powershell
./mvnw.cmd spring-boot:run
```

App base URL: http://localhost:8080

## API Endpoints

### 1) Create comment (auto-triage on submit)

```bash
curl -X POST "http://localhost:8080/comments" \
  -H "Content-Type: application/json" \
  -d '{"text":"The app crashes on login","source":"web"}'
```

### 2) List comments

```bash
curl "http://localhost:8080/comments"
```

Note: each comment includes ticketId. If ticketId is null, no ticket was created.

### 3) List tickets

```bash
curl "http://localhost:8080/tickets"
```

### 4) Get ticket by id

```bash
curl "http://localhost:8080/tickets/1"
```

### 5) Example 400 (empty comment text)

```bash
curl -X POST "http://localhost:8080/comments" \
  -H "Content-Type: application/json" \
  -d '{"text":"","source":"web"}'
```

### 6) Example 404 (ticket not found)

```bash
curl "http://localhost:8080/tickets/99999"
```

## Security Notes

- Never commit a real Hugging Face token.
- Use .env only for local development.
- Keep .env.example committed as a template only.
