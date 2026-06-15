# Environment Variables

The backend reads secrets from environment variables. Do not commit real secret
values to the repository.

## Required variables

- `DB_PASSWORD`: MySQL password used by the Spring Boot backend and
  `x_scraper_v2.py`.
- `JWT_SECRET`: JWT signing secret. Use a random value of at least 32 characters.

## Optional variables

- `DEEPSEEK_API_KEY`: Enables DeepSeek chat. Without it, the chat endpoint
  returns an unavailable message without sending a network request.
- `KAGGLE_API_KEY`: Enables Kaggle dataset downloads. Without it, batch import
  falls back to the built-in content library.

## PowerShell example

```powershell
$env:DB_PASSWORD="<your-mysql-password>"
$env:JWT_SECRET="<at-least-32-random-characters>"
$env:DEEPSEEK_API_KEY="<optional>"
$env:KAGGLE_API_KEY="<optional>"

Set-Location backend
mvn spring-boot:run
```

The `.env.example` file is a reference template. Spring Boot does not
automatically load `.env` files in this project.
