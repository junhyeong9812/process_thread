@echo off
echo Running all tests...

REM Run Java tests
echo Running Java tests...
cd java-spring
call gradlew.bat test

REM Run Python tests
echo Running Python tests...
cd ../python-fastapi
call venv\Scripts\activate.bat
pytest

echo All tests completed!
pause