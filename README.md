# Novel Backend

## Setup

### Environment Variables

This project uses environment variables for sensitive configuration. Follow these steps:

1. Copy the `.env.example` file to `.env`:
   ```powershell
   Copy-Item .env.example .env
   ```

2. Edit `.env` and replace the placeholder values with your actual credentials:
   - `GEMINI_API_KEY`: Your Google Gemini API key
   - `JWT_SECRET`: A secure random string (at least 256 bits for HS256)

3. The `.env` file is already added to `.gitignore` and will not be committed to version control.

### Running the Application

```powershell
./gradlew bootRun
```

## Important Notes

- **Never commit the `.env` file** - it contains sensitive credentials
- Always use `.env.example` as a template for new developers
- Make sure to set environment variables in production deployments

