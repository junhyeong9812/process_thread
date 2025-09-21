#!/bin/bash

echo "Starting Process Thread Project"

# Check Java version
java --version
if [ $? -ne 0 ]; then
    echo "Java 21 is required"
    exit 1
fi

# Check Python version
python --version
if [ $? -ne 0 ]; then
    echo "Python 3.11+ is required"
    exit 1
fi

# Start Java Spring Boot
echo "Starting Java Spring Boot..."
cd java-spring
./gradlew bootRun &
JAVA_PID=$!

# Start Python FastAPI
echo "Starting Python FastAPI..."
cd ../python-fastapi
source venv/bin/activate
uvicorn app.main:app --reload --port 8000 &
PYTHON_PID=$!

echo "Java Spring running on http://localhost:8080"
echo "Python FastAPI running on http://localhost:8000"
echo "Press Ctrl+C to stop both services"

# Wait for interrupt
trap "kill $JAVA_PID $PYTHON_PID" INT
wait