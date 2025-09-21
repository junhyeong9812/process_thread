@echo off
echo Setting up Process Thread Project

REM Setup Java project
echo Setting up Java Spring...
cd java-spring
call gradlew.bat build

REM Setup Python project
echo Setting up Python FastAPI...
cd ../python-fastapi
python -m venv venv
call venv\Scripts\activate.bat
pip install -r requirements.txt

echo Setup complete!
pause