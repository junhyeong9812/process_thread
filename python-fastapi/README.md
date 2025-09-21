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
│   ├── main.py                                      # FastAPI 애플리케이션 진입점
│   ├── core/
│   │   ├── __init__.py
│   │   ├── config.py                                # 애플리케이션 설정 (Settings 클래스)
│   │   ├── dependencies.py                          # 의존성 주입 함수들
│   │   ├── exceptions.py                            # 커스텀 예외 클래스들
│   │   ├── logging.py                               # 로깅 설정
│   │   └── security.py                              # 보안 관련 설정
│   ├── api/
│   │   ├── __init__.py
│   │   └── v1/
│   │       ├── __init__.py
│   │       ├── api.py                               # API 라우터 집합
│   │       └── endpoints/
│   │           ├── __init__.py
│   │           ├── process.py                       # 프로세스 관련 엔드포인트
│   │           ├── thread.py                        # 스레드 관련 엔드포인트
│   │           ├── comparison.py                    # 비교 분석 엔드포인트
│   │           ├── health.py                        # 헬스체크 엔드포인트
│   │           └── metrics.py                       # 메트릭 엔드포인트
│   ├── process/
│   │   ├── __init__.py
│   │   ├── creation/
│   │   │   ├── __init__.py
│   │   │   ├── process_creator.py                   # subprocess를 사용한 프로세스 생성
│   │   │   ├── process_factory.py                   # 다양한 타입의 프로세스 생성 팩토리
│   │   │   ├── child_process.py                     # 자식 프로세스로 실행될 코드
│   │   │   ├── process_spawner.py                   # 병렬 프로세스 생성 및 관리
│   │   │   └── process_builder.py                   # 프로세스 빌더 패턴 구현
│   │   ├── communication/
│   │   │   ├── __init__.py
│   │   │   ├── socket_ipc.py                        # 소켓 기반 IPC 구현
│   │   │   ├── pipe_ipc.py                          # 파이프 기반 IPC 구현
│   │   │   ├── queue_ipc.py                         # Queue 기반 IPC 구현
│   │   │   ├── shared_memory_ipc.py                 # 공유 메모리 IPC 구현
│   │   │   ├── message_queue.py                     # 메시지 큐 시뮬레이션
│   │   │   └── ipc_benchmark.py                     # IPC 성능 측정
│   │   ├── monitoring/
│   │   │   ├── __init__.py
│   │   │   ├── process_monitor.py                   # 프로세스 상태 모니터링
│   │   │   ├── process_resource_tracker.py          # CPU, 메모리 사용량 추적
│   │   │   ├── process_lifecycle_tracker.py         # 프로세스 생명주기 추적
│   │   │   ├── process_metrics.py                   # 프로세스 메트릭 수집 및 분석
│   │   │   └── psutil_wrapper.py                    # psutil 라이브러리 래퍼
│   │   └── management/
│   │       ├── __init__.py
│   │       ├── process_manager.py                   # 프로세스 생성/종료 관리
│   │       ├── process_pool.py                      # multiprocessing.Pool 구현
│   │       ├── process_scheduler_simulator.py       # 프로세스 스케줄링 시뮬레이션
│   │       ├── process_orchestrator.py              # 복잡한 프로세스 워크플로우 관리
│   │       └── process_registry.py                  # 실행 중인 프로세스 레지스트리
│   ├── thread/
│   │   ├── __init__.py
│   │   ├── lifecycle/
│   │   │   ├── __init__.py
│   │   │   ├── thread_state_demo.py                 # 스레드 상태 전이 데모
│   │   │   ├── thread_lifecycle_observer.py         # 스레드 생명주기 관찰
│   │   │   ├── native_thread_demo.py                # threading 모듈 실습
│   │   │   ├── async_task_demo.py                   # asyncio 태스크 실습
│   │   │   ├── thread_creation_comparison.py        # 스레드 생성 방식 비교
│   │   │   └── gil_demo.py                          # GIL(Global Interpreter Lock) 데모
│   │   ├── synchronization/
│   │   │   ├── __init__.py
│   │   │   ├── mutex_demo.py                        # Lock(뮤텍스) 구현
│   │   │   ├── semaphore_demo.py                    # Semaphore 구현 및 실습
│   │   │   ├── condition_demo.py                    # Condition Variable 구현
│   │   │   ├── event_demo.py                        # Event 객체 사용
│   │   │   ├── barrier_demo.py                      # Barrier 동기화
│   │   │   ├── rlock_demo.py                        # RLock(재진입 가능 락) 실습
│   │   │   ├── producer_consumer.py                 # 생산자-소비자 문제
│   │   │   ├── dining_philosophers.py               # 철학자들의 만찬 문제
│   │   │   ├── readers_writers.py                   # 독자-작가 문제
│   │   │   ├── deadlock_simulator.py                # 데드락 시뮬레이션
│   │   │   └── deadlock_detector.py                 # 데드락 탐지 및 해결
│   │   ├── pool/
│   │   │   ├── __init__.py
│   │   │   ├── custom_thread_pool.py                # 커스텀 스레드 풀 구현
│   │   │   ├── thread_pool_executor_demo.py         # ThreadPoolExecutor 실습
│   │   │   ├── process_pool_executor_demo.py        # ProcessPoolExecutor 실습
│   │   │   ├── async_pool_demo.py                   # 비동기 작업 풀
│   │   │   ├── work_queue.py                        # 작업 큐 구현
│   │   │   └── thread_pool_monitor.py               # 스레드 풀 모니터링
│   │   ├── scheduling/
│   │   │   ├── __init__.py
│   │   │   ├── fcfs_scheduler.py                    # First-Come First-Served 구현
│   │   │   ├── sjf_scheduler.py                     # Shortest Job First 구현
│   │   │   ├── round_robin_scheduler.py             # Round Robin 구현
│   │   │   ├── priority_scheduler.py                # Priority 스케줄링 구현
│   │   │   ├── mlfq_scheduler.py                    # Multi-Level Feedback Queue 구현
│   │   │   └── scheduler_comparison.py              # 스케줄러 성능 비교
│   │   └── async_patterns/
│   │       ├── __init__.py
│   │       ├── async_context_manager.py             # 비동기 컨텍스트 매니저
│   │       ├── async_generator.py                   # 비동기 제너레이터
│   │       ├── async_queue.py                       # asyncio.Queue 패턴
│   │       ├── async_lock.py                        # 비동기 락 패턴
│   │       ├── gather_pattern.py                    # asyncio.gather 패턴
│   │       ├── task_group.py                        # TaskGroup 패턴 (Python 3.11+)
│   │       └── structured_concurrency.py            # 구조화된 동시성 패턴
│   ├── comparison/
│   │   ├── __init__.py
│   │   ├── performance/
│   │   │   ├── __init__.py
│   │   │   ├── creation_benchmark.py                # 생성 시간 벤치마크
│   │   │   ├── context_switch_benchmark.py          # 컨텍스트 스위칭 벤치마크
│   │   │   ├── io_benchmark.py                      # I/O 작업 벤치마크
│   │   │   ├── cpu_benchmark.py                     # CPU 집약 작업 벤치마크
│   │   │   ├── scalability_test.py                  # 확장성 테스트
│   │   │   └── throughput_analyzer.py               # 처리량 분석
│   │   ├── resource/
│   │   │   ├── __init__.py
│   │   │   ├── memory_usage_analyzer.py             # 메모리 사용량 분석
│   │   │   ├── cpu_usage_analyzer.py                # CPU 사용률 분석
│   │   │   ├── resource_profiler.py                 # 리소스 프로파일링
│   │   │   ├── resource_comparison.py               # 리소스 사용량 비교
│   │   │   └── gil_impact_analyzer.py               # GIL 영향 분석
│   │   └── concurrency/
│   │       ├── __init__.py
│   │       ├── concurrent_task_executor.py          # 동시 작업 실행 비교
│   │       ├── parallelism_analyzer.py              # 병렬성 분석
│   │       ├── race_condition_detector.py           # 경쟁 조건 탐지
│   │       ├── concurrency_patterns.py              # 동시성 패턴 비교
│   │       └── async_vs_thread_comparison.py        # asyncio vs threading 비교
│   ├── common/
│   │   ├── __init__.py
│   │   ├── utils/
│   │   │   ├── __init__.py
│   │   │   ├── system_info.py                       # 시스템 정보 조회
│   │   │   ├── time_utils.py                        # 시간 측정 유틸리티
│   │   │   ├── random_data_generator.py             # 테스트 데이터 생성
│   │   │   ├── file_utils.py                        # 파일 작업 유틸리티
│   │   │   ├── report_generator.py                  # 보고서 생성
│   │   │   └── decorators.py                        # 유용한 데코레이터들
│   │   ├── monitor/
│   │   │   ├── __init__.py
│   │   │   ├── base_monitor.py                      # 모니터링 기본 클래스
│   │   │   ├── metrics_collector.py                 # 메트릭 수집기
│   │   │   ├── performance_monitor.py               # 성능 모니터
│   │   │   ├── prometheus_metrics.py                # Prometheus 메트릭
│   │   │   └── dashboard_server.py                  # 모니터링 대시보드 서버
│   │   └── models/
│   │       ├── __init__.py
│   │       ├── process_model.py                     # 프로세스 데이터 모델
│   │       ├── thread_model.py                      # 스레드 데이터 모델
│   │       └── metrics_model.py                     # 메트릭 데이터 모델
│   └── demo/
│       ├── __init__.py
│       ├── demo_runner.py                           # 데모 실행 관리자
│       ├── process_demos.py                         # 프로세스 데모 모음
│       ├── thread_demos.py                          # 스레드 데모 모음
│       └── comparison_demos.py                      # 비교 데모 모음
├── tests/
│   ├── __init__.py
│   ├── conftest.py                                  # pytest 설정 및 fixtures
│   ├── unit/
│   │   ├── __init__.py
│   │   ├── process/
│   │   │   ├── __init__.py
│   │   │   ├── creation/
│   │   │   │   ├── __init__.py
│   │   │   │   ├── test_process_creator.py          # ProcessCreator 테스트
│   │   │   │   ├── test_process_factory.py          # ProcessFactory 테스트
│   │   │   │   └── test_process_spawner.py          # ProcessSpawner 테스트
│   │   │   ├── communication/
│   │   │   │   ├── __init__.py
│   │   │   │   ├── test_socket_ipc.py               # 소켓 IPC 테스트
│   │   │   │   ├── test_pipe_ipc.py                 # 파이프 IPC 테스트
│   │   │   │   ├── test_queue_ipc.py                # 큐 IPC 테스트
│   │   │   │   └── test_shared_memory_ipc.py        # 공유 메모리 테스트
│   │   │   ├── monitoring/
│   │   │   │   ├── __init__.py
│   │   │   │   ├── test_process_monitor.py          # 프로세스 모니터링 테스트
│   │   │   │   └── test_process_resource_tracker.py # 리소스 추적 테스트
│   │   │   └── management/
│   │   │       ├── __init__.py
│   │   │       ├── test_process_manager.py          # 프로세스 관리 테스트
│   │   │       └── test_process_pool.py             # 프로세스 풀 테스트
│   │   ├── thread/
│   │   │   ├── __init__.py
│   │   │   ├── lifecycle/
│   │   │   │   ├── __init__.py
│   │   │   │   ├── test_thread_state.py             # 스레드 상태 테스트
│   │   │   │   ├── test_native_thread.py            # threading 모듈 테스트
│   │   │   │   └── test_async_task.py               # asyncio 태스크 테스트
│   │   │   ├── synchronization/
│   │   │   │   ├── __init__.py
│   │   │   │   ├── test_mutex.py                    # 뮤텍스 테스트
│   │   │   │   ├── test_semaphore.py                # 세마포어 테스트
│   │   │   │   ├── test_producer_consumer.py        # 생산자-소비자 테스트
│   │   │   │   ├── test_deadlock_simulator.py       # 데드락 시뮬레이터 테스트
│   │   │   │   └── test_deadlock_detector.py        # 데드락 탐지 테스트
│   │   │   ├── pool/
│   │   │   │   ├── __init__.py
│   │   │   │   ├── test_custom_thread_pool.py       # 커스텀 풀 테스트
│   │   │   │   ├── test_thread_pool_executor.py     # ThreadPoolExecutor 테스트
│   │   │   │   └── test_async_pool.py               # 비동기 풀 테스트
│   │   │   └── scheduling/
│   │   │       ├── __init__.py
│   │   │       ├── test_fcfs_scheduler.py           # FCFS 알고리즘 테스트
│   │   │       ├── test_sjf_scheduler.py            # SJF 알고리즘 테스트
│   │   │       ├── test_round_robin_scheduler.py    # Round Robin 테스트
│   │   │       └── test_scheduler_comparison.py     # 스케줄러 비교 테스트
│   │   └── comparison/
│   │       ├── __init__.py
│   │       ├── performance/
│   │       │   ├── __init__.py
│   │       │   ├── test_creation_benchmark.py       # 생성 벤치마크 테스트
│   │       │   └── test_scalability.py              # 확장성 테스트
│   │       ├── resource/
│   │       │   ├── __init__.py
│   │       │   └── test_resource_analyzer.py        # 리소스 분석기 테스트
│   │       └── concurrency/
│   │           ├── __init__.py
│   │           └── test_race_condition_detector.py  # 경쟁 조건 탐지 테스트
│   ├── integration/
│   │   ├── __init__.py
│   │   ├── test_process_integration.py              # 프로세스 통합 테스트
│   │   ├── test_thread_integration.py               # 스레드 통합 테스트
│   │   ├── test_api_integration.py                  # API 통합 테스트
│   │   └── test_ipc_integration.py                  # IPC 통합 테스트
│   ├── benchmark/
│   │   ├── __init__.py
│   │   ├── bench_process_creation.py                # 프로세스 생성 벤치마크
│   │   ├── bench_thread_creation.py                 # 스레드 생성 벤치마크
│   │   ├── bench_context_switch.py                  # 컨텍스트 스위치 벤치마크
│   │   ├── bench_ipc_performance.py                 # IPC 성능 벤치마크
│   │   └── bench_gil_impact.py                      # GIL 영향 벤치마크
│   └── load/
│       ├── __init__.py
│       ├── locustfile.py                            # Locust 부하 테스트
│       └── stress_test.py                           # 스트레스 테스트
├── docs/
│   ├── api/
│   │   ├── openapi.json                             # OpenAPI 스펙
│   │   └── postman_collection.json                  # Postman 컬렉션
│   ├── architecture/
│   │   ├── system_design.md                         # 시스템 설계 문서
│   │   ├── component_diagram.md                     # 컴포넌트 다이어그램
│   │   └── sequence_diagrams.md                     # 시퀀스 다이어그램
│   ├── experiments/
│   │   ├── 01_process_isolation.md                  # 프로세스 격리 실험
│   │   ├── 02_gil_impact.md                         # GIL 영향 실험
│   │   ├── 03_async_vs_thread.md                    # async vs thread 실험
│   │   ├── 04_deadlock_scenarios.md                 # 데드락 시나리오 실험
│   │   └── 05_performance_comparison.md             # 성능 비교 실험
│   ├── tutorials/
│   │   ├── getting_started.md                       # 시작 가이드
│   │   ├── process_tutorial.md                      # 프로세스 튜토리얼
│   │   ├── thread_tutorial.md                       # 스레드 튜토리얼
│   │   └── async_tutorial.md                        # 비동기 프로그래밍 튜토리얼
│   └── results/
│       ├── benchmark_results.md                     # 벤치마크 결과
│       ├── performance_analysis.md                  # 성능 분석 보고서
│       └── charts/                                  # 결과 차트
│           ├── creation_time_comparison.png
│           ├── memory_usage_comparison.png
│           └── throughput_analysis.png
├── scripts/
│   ├── run_app.py                                   # 애플리케이션 실행 스크립트
│   ├── run_demo.py                                  # 데모 실행 스크립트
│   ├── run_benchmark.py                             # 벤치마크 실행 스크립트
│   ├── generate_report.py                           # 보고서 생성 스크립트
│   ├── setup_environment.py                         # 환경 설정 스크립트
│   ├── run-python.bat                               # Windows 실행 배치
│   ├── run-demo.bat                                 # Windows 데모 배치
│   ├── run-benchmark.bat                            # Windows 벤치마크 배치
│   └── run-tests.bat                                # Windows 테스트 배치
├── configs/
│   ├── logging.yaml                                 # 로깅 설정
│   ├── benchmark.yaml                               # 벤치마크 설정
│   └── demo.yaml                                    # 데모 설정
├── logs/                                             # 로그 디렉토리
│   ├── app.log
│   ├── process.log
│   └── thread.log
├── data/                                             # 데이터 디렉토리
│   ├── input/                                       # 입력 데이터
│   ├── output/                                      # 출력 데이터
│   └── temp/                                        # 임시 파일
├── .env                                              # 환경 변수
├── .env.example                                      # 환경 변수 예제
├── .gitignore                                        # Git ignore 파일
├── .dockerignore                                     # Docker ignore 파일
├── Dockerfile                                        # Docker 이미지 정의
├── docker-compose.yml                                # Docker Compose 설정
├── requirements.txt                                  # 프로덕션 의존성
├── requirements-dev.txt                              # 개발 의존성
├── pyproject.toml                                    # 프로젝트 메타데이터
├── setup.py                                          # 패키지 설정
├── setup.cfg                                         # 설정 파일
├── pytest.ini                                        # pytest 설정
├── .ruff.toml                                        # Ruff 린터 설정
├── .mypy.ini                                         # mypy 타입 체커 설정
├── Makefile                                          # Make 명령어
├── README.md                                         # 프로젝트 문서
└── LICENSE                                           # 라이센스 파일
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