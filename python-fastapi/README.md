# Process Thread - Python FastAPI

프로세스와 스레드 동작 방식을 탐구하는 Python FastAPI 구현체

## 프로젝트 개요

이 프로젝트는 Python 환경에서 프로세스와 스레드의 동작 방식을 실습하고 학습하기 위한 FastAPI 기반 애플리케이션입니다. multiprocessing, threading, asyncio를 활용하여 동시성과 병렬성을 구현하고, Java Spring Boot 구현체와 성능을 비교합니다.

## 학습 목표

### Process 레벨
- Python multiprocessing을 통한 프로세스 생성 및 관리
- 프로세스 간 통신 (IPC) - Queue, Pipe, Shared Memory
- GIL(Global Interpreter Lock) 우회를 통한 진정한 병렬 처리
- 프로세스 풀(Pool)을 통한 효율적인 리소스 관리

### Thread 레벨
- threading 모듈을 통한 전통적인 스레드 프로그래밍
- asyncio를 통한 비동기 프로그래밍
- GIL의 영향과 I/O 바운드 vs CPU 바운드 작업
- concurrent.futures를 통한 고수준 동시성 구현

### FastAPI 통합
- 비동기 엔드포인트와 동기 엔드포인트 비교
- BackgroundTasks를 통한 백그라운드 작업 처리
- WebSocket을 통한 실시간 통신
- Streaming Response를 통한 대용량 데이터 처리

## 기술 스택

- **Language**: Python 3.11+
- **Framework**: FastAPI
- **ASGI Server**: Uvicorn
- **Testing**: pytest, pytest-asyncio
- **Process Management**: multiprocessing, psutil
- **Async**: asyncio, aiofiles, httpx
- **Monitoring**: Prometheus, Grafana

## 설치 방법

### 필수 요구사항
- Python 3.11 이상
- pip 또는 poetry

### 가상환경 설정 및 의존성 설치
```bash
# 가상환경 생성
python -m venv venv

# 가상환경 활성화 (Windows)
venv\Scripts\activate.bat

# 가상환경 활성화 (Linux/Mac)
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt

# 개발 의존성 포함 설치
pip install -e ".[dev]"
```

## 프로젝트 구조
```
python-fastapi/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI 애플리케이션 진입점
│   ├── core/
│   │   ├── config.py              # 애플리케이션 설정
│   │   ├── dependencies.py        # 의존성 주입
│   │   └── exceptions.py          # 커스텀 예외
│   ├── api/
│   │   └── v1/
│   │       ├── api.py             # API 라우터 집합
│   │       └── endpoints/
│   │           ├── process.py     # 프로세스 관련 엔드포인트
│   │           ├── thread.py      # 스레드 관련 엔드포인트
│   │           └── comparison.py  # 비교 분석 엔드포인트
│   ├── process/
│   │   ├── creation/              # 프로세스 생성
│   │   ├── communication/         # IPC 구현
│   │   ├── monitoring/            # 프로세스 모니터링
│   │   └── management/            # 프로세스 관리
│   ├── thread/
│   │   ├── lifecycle/             # 스레드 생명주기
│   │   ├── synchronization/       # 동기화 메커니즘
│   │   ├── pool/                  # 스레드 풀
│   │   └── async_patterns/        # 비동기 패턴
│   └── comparison/
│       ├── benchmarks/            # 벤치마크 구현
│       └── metrics/               # 메트릭 수집
├── tests/
│   ├── conftest.py                # pytest 설정 및 fixtures
│   ├── unit/                      # 단위 테스트
│   ├── integration/               # 통합 테스트
│   └── benchmark/                 # 성능 벤치마크
├── docs/
│   ├── api/                       # API 문서
│   └── tutorials/                 # 튜토리얼
├── scripts/
│   └── *.bat                      # Windows 실행 스크립트
├── requirements.txt               # 프로덕션 의존성
├── requirements-dev.txt           # 개발 의존성
├── pyproject.toml                 # 프로젝트 메타데이터
├── .env.example                   # 환경변수 예제
└── README.md                      # 프로젝트 문서
```

## 실행 방법

### 개발 서버 실행
```bash
# 자동 리로드 모드로 실행
uvicorn app.main:app --reload --port 8000

# 또는 Python 모듈로 실행
python -m uvicorn app.main:app --reload --port 8000

# Windows 배치 스크립트 사용
scripts\run-python.bat
```

### 프로덕션 서버 실행
```bash
# 멀티 워커로 실행
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4

# Gunicorn 사용 (Linux/Mac)
gunicorn app.main:app -w 4 -k uvicorn.workers.UvicornWorker
```

## API 문서
애플리케이션 실행 후 다음 주소에서 API 문서를 확인할 수 있습니다:

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **OpenAPI JSON**: http://localhost:8000/openapi.json

## 주요 API 엔드포인트

### Process Management
- `POST /api/python/process/create` - 새 프로세스 생성
- `GET /api/python/process/list` - 실행 중인 프로세스 목록
- `GET /api/python/process/{pid}/status` - 프로세스 상태 조회
- `DELETE /api/python/process/{pid}` - 프로세스 종료
- `POST /api/python/process/pool/submit` - 프로세스 풀에 작업 제출

### Thread Management
- `POST /api/python/thread/create` - 새 스레드 생성
- `GET /api/python/thread/async-demo` - async/await 데모
- `POST /api/python/thread/pool/submit` - 스레드 풀에 작업 제출
- `GET /api/python/thread/state/{tid}` - 스레드 상태 조회

### Comparison & Benchmarks
- `GET /api/python/comparison/metrics` - 비교 메트릭 조회
- `POST /api/python/comparison/benchmark/run` - 벤치마크 실행
- `GET /api/python/comparison/results` - 벤치마크 결과 조회

## 테스트 전략

### TDD (Test-Driven Development) 워크플로우
1. **테스트 작성** - 실패하는 테스트 먼저 작성
2. **구현** - 테스트를 통과할 최소한의 코드 작성
3. **리팩토링** - 코드 개선
4. **반복** - 다음 기능으로 이동

### 테스트 실행
```bash
# 모든 테스트 실행
pytest

# 커버리지와 함께 실행
pytest --cov=app --cov-report=html

# 특정 테스트 파일 실행
pytest tests/test_process.py

# 특정 테스트 클래스 실행
pytest tests/test_process.py::TestProcessCreation

# 벤치마크 테스트만 실행
pytest tests/benchmark/ --benchmark-only

# 비동기 테스트 실행
pytest tests/test_async.py -v

# 병렬 테스트 실행
pytest -n 4
```

### 테스트 구조 예제
```python
# tests/unit/test_process_creation.py
import pytest
from unittest.mock import Mock, patch
from app.process.creation.process_creator import ProcessCreator


class TestProcessCreation:
    """프로세스 생성 테스트"""
    
    @pytest.fixture
    def creator(self):
        """ProcessCreator fixture"""
        return ProcessCreator()
    
    def test_create_simple_process(self, creator):
        """단순 프로세스 생성 테스트"""
        # Given
        command = ["python", "-c", "print('Hello')"]
        
        # When
        process = creator.create(command)
        
        # Then
        assert process is not None
        assert process.pid > 0
        assert process.is_alive()
    
    @pytest.mark.asyncio
    async def test_async_process_creation(self, creator):
        """비동기 프로세스 생성 테스트"""
        # Given
        command = ["python", "-c", "print('Async')"]
        
        # When
        process = await creator.create_async(command)
        
        # Then
        assert process is not None
    
    @patch('subprocess.Popen')
    def test_process_creation_with_mock(self, mock_popen, creator):
        """Mock을 사용한 프로세스 생성 테스트"""
        # Given
        mock_process = Mock()
        mock_process.pid = 12345
        mock_popen.return_value = mock_process
        
        # When
        process = creator.create(["test"])
        
        # Then
        mock_popen.assert_called_once()
        assert process.pid == 12345
```

## 코드 품질 관리

### 코드 포맷팅
```bash
# Black으로 코드 포맷팅
black app tests

# 특정 파일만 포맷팅
black app/main.py
```

### 린팅
```bash
# Ruff로 린팅
ruff app tests

# 자동 수정
ruff --fix app tests
```

### 타입 체킹
```bash
# mypy로 타입 체킹
mypy app

# 엄격한 모드
mypy --strict app
```

## 환경 설정

### 환경 변수 (.env)
```env
# Application
APP_NAME=process-thread-fastapi
VERSION=1.0.0
DEBUG=true

# Server
HOST=0.0.0.0
PORT=8000
WORKERS=4

# Process Configuration
MAX_PROCESSES=10
PROCESS_TIMEOUT=30
PROCESS_POOL_SIZE=4

# Thread Configuration
MAX_THREADS=100
THREAD_POOL_SIZE=10

# Monitoring
ENABLE_METRICS=true
METRICS_PORT=9090
```

## 모니터링

### Prometheus 메트릭
애플리케이션은 다음 메트릭을 노출합니다:
- `process_created_total` - 생성된 프로세스 총 개수
- `thread_created_total` - 생성된 스레드 총 개수
- `request_duration_seconds` - 요청 처리 시간
- `active_processes_gauge` - 활성 프로세스 수
- `active_threads_gauge` - 활성 스레드 수

### Health Check
```bash
# Health check endpoint
curl http://localhost:8000/health

# Prometheus metrics
curl http://localhost:8000/metrics
```

## Docker 지원

### 이미지 빌드
```bash
# Docker 이미지 빌드
docker build -f docker/python.Dockerfile -t process-thread-python .
```

### 컨테이너 실행
```bash
# 단일 컨테이너 실행
docker run -p 8000:8000 process-thread-python

# Docker Compose로 전체 스택 실행
docker-compose up
```

## 성능 벤치마킹

### 로컬 벤치마크 실행
```bash
# pytest-benchmark 사용
pytest tests/benchmark/ --benchmark-only

# locust로 부하 테스트
locust -f tests/load/locustfile.py --host=http://localhost:8000
```

### 벤치마크 시나리오

1. **프로세스 생성 벤치마크**
    - 단일 프로세스 생성 시간
    - 병렬 프로세스 생성
    - 프로세스 풀 성능

2. **스레드 벤치마크**
    - threading vs asyncio 비교
    - GIL 영향 측정
    - I/O 바운드 vs CPU 바운드

3. **API 엔드포인트 벤치마크**
    - 동시 요청 처리
    - 응답 시간 (P50, P95, P99)
    - 처리량 (RPS)

## 문제 해결

### 자주 발생하는 문제

#### 1. 포트 충돌
```bash
# 포트 사용 확인 (Windows)
netstat -ano | findstr :8000

# 프로세스 종료 (Windows)
taskkill /PID <PID> /F
```

#### 2. 가상환경 활성화 실패
```bash
# Windows PowerShell 실행 정책 변경
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

#### 3. asyncio 관련 에러
```python
# 이벤트 루프 관련 에러 해결
import asyncio
import sys

if sys.platform == 'win32':
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
```

## 기여 방법

1. Fork 저장소
2. 기능 브랜치 생성 (`git checkout -b feature/amazing-feature`)
3. 변경사항 커밋 (`git commit -m 'Add amazing feature'`)
4. 브랜치에 Push (`git push origin feature/amazing-feature`)
5. Pull Request 생성

## 라이센스
MIT License

## 문의사항
프로젝트 관련 문의사항은 Issues를 통해 남겨주세요.

## 참고 자료
- [FastAPI 공식 문서](https://fastapi.tiangolo.com)
- [Python asyncio 문서](https://docs.python.org/3/library/asyncio.html)
- [Python multiprocessing 문서](https://docs.python.org/3/library/multiprocessing.html)
- [pytest 문서](https://docs.pytest.org)