@echo off
echo Starting Process Thread Project

REM Check Java version
java --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Java 21 is required
    exit /b 1
)

REM Check Python version
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Python 3.11+ is required
    exit /b 1
)

REM Start Java Spring Boot
echo Starting Java Spring Boot...
cd java-spring
start /b cmd /c gradlew.bat bootRun

REM Start Python FastAPI
echo Starting Python FastAPI...
cd ../python-fastapi
call venv\Scripts\activate.bat
start /b cmd /c python -m uvicorn app.main:app --reload --port 8000

echo.
echo Java Spring running on http://localhost:8080
echo Python FastAPI running on http://localhost:8000
echo Press Ctrl+C to stop both services
pause