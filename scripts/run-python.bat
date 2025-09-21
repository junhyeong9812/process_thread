@echo off
echo Starting Python FastAPI...
cd python-fastapi
call venv\Scripts\activate.bat
python -m uvicorn app.main:app --reload --port 8000