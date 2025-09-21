# process_thread 프로젝트 구현 순서 가이드

## 개요
이 문서는 process_thread 프로젝트의 체계적인 구현을 위한 단계별 가이드입니다.
Java(Spring Boot)와 Python(FastAPI)을 동시에 구현하여 두 언어/프레임워크에서의 프로세스와 스레드 동작을 비교 학습합니다.

## 프로젝트 구조 개요

```
process_thread/
├── java-spring/                    # Java Spring Boot 프로젝트
│   ├── src/
│   ├── build.gradle
│   └── README.md
├── python-fastapi/                 # Python FastAPI 프로젝트
│   ├── app/
│   ├── requirements.txt
│   ├── pyproject.toml
│   └── README.md
├── shared/                         # 공통 리소스
│   ├── docs/                       # 통합 문서
│   ├── benchmarks/                 # 벤치마크 결과
│   └── configs/                    # 공통 설정
├── docker/                         # 컨테이너 설정
│   ├── java.Dockerfile
│   ├── python.Dockerfile
│   └── docker-compose.yml
├── scripts/                        # 실행 스크립트
│   ├── run-java.sh
│   ├── run-python.sh
│   └── run-comparison.sh
└── README.md                       # 전체 프로젝트 README
```

## 구현 단계 개요

| 단계 | 모듈 | Java/Python | 예상 소요 시간 | 난이도 | 의존성 |
|------|------|------------|--------------|--------|--------|
| Phase 1 | 프로젝트 설정 | 공통 | 1일 | ★☆☆ | 없음 |
| Phase 2 | Process 기초 | 병렬 구현 | 3일 | ★★☆ | Phase 1 |
| Phase 3 | Thread 기초 | 병렬 구현 | 3일 | ★★☆ | Phase 1 |
| Phase 4 | 동기화 | 병렬 구현 | 4일 | ★★★ | Phase 3 |
| Phase 5 | 웹 프레임워크 통합 | 병렬 구현 | 3일 | ★★★ | Phase 3, 4 |
| Phase 6 | 고급 기능 | 병렬 구현 | 4일 | ★★★ | Phase 2-5 |
| Phase 7 | 성능 비교 | 통합 | 3일 | ★★☆ | Phase 1-6 |
| Phase 8 | 통합 및 최적화 | 통합 | 2일 | ★★★ | 전체 |

## Phase 1: 프로젝트 초기 설정

### 목표
Java Spring Boot와 Python FastAPI 프로젝트 기본 구조 설정

### 1.1 Java Spring Boot 프로젝트 설정

#### 디렉토리 구조
```
java-spring/
├── src/main/java/com/study/processthread/
│   ├── process/
│   ├── thread/
│   ├── comparison/
│   ├── web/                       # Spring Web 컨트롤러
│   └── config/                     # Spring 설정
├── src/main/resources/
└── build.gradle
```

#### 구현 순서
```
1. build.gradle 설정
   - Spring Boot 3.2.x
   - Java 21
   - Virtual Thread 지원

2. application.yml 설정
   - 서버 포트: 8080
   - Virtual Thread 활성화

3. SpringBootApplication 메인 클래스

4. 기본 RestController
```

### 1.2 Python FastAPI 프로젝트 설정

#### 디렉토리 구조
```
python-fastapi/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── process/
│   ├── thread/
│   ├── comparison/
│   ├── api/                       # FastAPI 라우터
│   └── core/                      # 핵심 설정
├── tests/
├── requirements.txt
└── pyproject.toml
```

#### 구현 순서
```
1. pyproject.toml 설정
   - Python 3.11+
   - FastAPI
   - Uvicorn

2. requirements.txt
   - fastapi
   - uvicorn[standard]
   - asyncio
   - multiprocessing
   - psutil

3. main.py FastAPI 앱 생성

4. 기본 API 엔드포인트
```

---

## Phase 2: Process 모듈 구현 (병렬)

### 목표
Java와 Python에서 프로세스 생성과 관리 구현 및 비교

### 2.1 Java Process 구현

#### 구현 파일
```
java-spring/src/main/java/com/study/processthread/process/
├── creation/
│   ├── ProcessCreator.java
│   ├── ProcessFactory.java
│   └── ChildProcess.java
├── communication/
│   ├── SocketIPC.java
│   └── PipeIPC.java
├── monitoring/
│   └── ProcessMonitor.java
└── management/
    └── ProcessManager.java
```

#### Spring 통합
```java
// ProcessController.java
@RestController
@RequestMapping("/api/java/process")
public class ProcessController {
    // POST /api/java/process/create
    // GET /api/java/process/{pid}/status
    // DELETE /api/java/process/{pid}
}
```

### 2.2 Python Process 구현

#### 구현 파일
```
python-fastapi/app/process/
├── creation/
│   ├── process_creator.py
│   ├── process_factory.py
│   └── child_process.py
├── communication/
│   ├── socket_ipc.py
│   ├── pipe_ipc.py
│   └── queue_ipc.py
├── monitoring/
│   └── process_monitor.py
└── management/
    └── process_manager.py
```

#### FastAPI 통합
```python
# process_router.py
from fastapi import APIRouter

router = APIRouter(prefix="/api/python/process")

@router.post("/create")
async def create_process():
    pass

@router.get("/{pid}/status")
async def get_process_status(pid: int):
    pass
```

### 2.3 비교 테스트
```
- 프로세스 생성 시간 비교
- 메모리 격리 확인
- IPC 성능 비교
```

---

## Phase 3: Thread 모듈 구현 (병렬)

### 목표
Java Virtual Thread와 Python asyncio/threading 비교

### 3.1 Java Thread 구현

#### Platform Thread vs Virtual Thread
```
java-spring/src/main/java/com/study/processthread/thread/
├── lifecycle/
│   ├── PlatformThreadDemo.java
│   ├── VirtualThreadDemo.java
│   └── ThreadStateObserver.java
├── synchronization/
│   ├── SynchronizedDemo.java
│   └── LockDemo.java
└── pool/
    ├── ThreadPoolExecutorDemo.java
    └── VirtualThreadExecutor.java
```

#### Spring WebFlux 통합
```java
@RestController
@RequestMapping("/api/java/thread")
public class ThreadController {
    
    @GetMapping(value = "/virtual/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> virtualThreadStream() {
        // Virtual Thread 기반 스트리밍
    }
}
```

### 3.2 Python Thread 구현

#### asyncio vs threading vs multiprocessing.Process
```
python-fastapi/app/thread/
├── lifecycle/
│   ├── thread_demo.py
│   ├── async_demo.py
│   └── thread_state_observer.py
├── synchronization/
│   ├── lock_demo.py
│   ├── async_lock_demo.py
│   └── semaphore_demo.py
└── pool/
    ├── thread_pool_demo.py
    └── async_pool_demo.py
```

#### FastAPI 비동기 통합
```python
@router.get("/async/stream")
async def async_stream():
    async def generate():
        for i in range(10):
            await asyncio.sleep(1)
            yield f"data: {i}\n\n"
    return StreamingResponse(generate())
```

### 3.3 비교 포인트
```
- Java Virtual Thread vs Python asyncio
- Java Platform Thread vs Python threading
- 동시성 모델 차이
- GIL의 영향
```

---

## Phase 4: 동기화 메커니즘 (병렬)

### 목표
양 언어의 동기화 메커니즘 구현 및 비교

### 4.1 Java 동기화

```java
// Java synchronized, ReentrantLock, Semaphore, etc.
java-spring/src/main/java/com/study/processthread/thread/synchronization/
├── ProducerConsumer.java
├── DiningPhilosophers.java
├── DeadlockSimulator.java
└── StampedLockDemo.java
```

### 4.2 Python 동기화

```python
# Python Lock, asyncio.Lock, Semaphore, etc.
python-fastapi/app/thread/synchronization/
├── producer_consumer.py
├── dining_philosophers.py
├── deadlock_simulator.py
└── async_synchronization.py
```

### 4.3 비교 실험
```
- 동기화 오버헤드
- 데드락 처리 방식
- 성능 차이
```

---

## Phase 5: 웹 프레임워크 통합

### 목표
Spring Boot와 FastAPI에서 프로세스/스레드 활용 비교

### 5.1 Spring Boot 통합

#### 구현 내용
```java
java-spring/src/main/java/com/study/processthread/web/
├── controller/
│   ├── ProcessController.java
│   ├── ThreadController.java
│   └── ComparisonController.java
├── service/
│   ├── AsyncService.java           // @Async 사용
│   ├── ReactiveService.java        // WebFlux
│   └── VirtualThreadService.java   // Virtual Thread
└── config/
    ├── AsyncConfig.java            // ThreadPoolTaskExecutor
    └── WebFluxConfig.java
```

#### 주요 기능
```java
@RestController
public class WorkloadController {
    
    // CPU 집약적 작업
    @PostMapping("/api/java/workload/cpu")
    public CompletableFuture<Result> cpuIntensive() {
        return CompletableFuture.supplyAsync(() -> {
            // Virtual Thread에서 실행
        }, virtualThreadExecutor);
    }
    
    // I/O 집약적 작업
    @PostMapping("/api/java/workload/io")
    public Mono<Result> ioIntensive() {
        // WebFlux 비동기 처리
    }
}
```

### 5.2 FastAPI 통합

#### 구현 내용
```python
python-fastapi/app/api/
├── routers/
│   ├── process_router.py
│   ├── thread_router.py
│   └── comparison_router.py
├── services/
│   ├── async_service.py
│   ├── background_service.py
│   └── concurrent_service.py
└── dependencies/
    └── thread_pool.py
```

#### 주요 기능
```python
@router.post("/api/python/workload/cpu")
async def cpu_intensive():
    loop = asyncio.get_event_loop()
    # ProcessPoolExecutor 사용
    result = await loop.run_in_executor(
        process_pool, cpu_bound_task
    )
    return result

@router.post("/api/python/workload/io")
async def io_intensive():
    # asyncio 비동기 처리
    async with httpx.AsyncClient() as client:
        results = await asyncio.gather(*tasks)
    return results
```

### 5.3 프레임워크 비교
```
- 요청 처리 모델 (Thread-per-request vs Event Loop)
- 동시 요청 처리 능력
- 리소스 사용량
- 응답 시간
```

---

## Phase 6: 고급 기능 구현

### 목표
각 언어의 고급 동시성 기능 구현

### 6.1 Java 고급 기능

#### 구현 내용
```
java-spring/src/main/java/com/study/processthread/advanced/
├── structured/
│   ├── StructuredConcurrency.java  // Java 21 Preview
│   └── ScopedValueDemo.java
├── reactive/
│   ├── ReactorDemo.java            // Project Reactor
│   └── BackpressureDemo.java
└── distributed/
    └── HazelcastDemo.java           // 분산 처리
```

### 6.2 Python 고급 기능

#### 구현 내용
```
python-fastapi/app/advanced/
├── async_patterns/
│   ├── async_context_manager.py
│   └── async_generator.py
├── concurrent_futures/
│   ├── process_pool_executor.py
│   └── thread_pool_executor.py
└── distributed/
    └── celery_demo.py               # 분산 태스크 큐
```

---

## Phase 7: 성능 비교 및 벤치마킹

### 목표
Java와 Python의 종합적인 성능 비교

### 7.1 벤치마크 시나리오

#### 공통 테스트 케이스
```
shared/benchmarks/scenarios/
├── process_creation.yaml
├── thread_creation.yaml
├── context_switching.yaml
├── io_intensive.yaml
├── cpu_intensive.yaml
└── mixed_workload.yaml
```

### 7.2 Java 벤치마크

```java
java-spring/src/test/java/com/study/processthread/benchmark/
├── ProcessBenchmark.java    // JMH
├── ThreadBenchmark.java
└── WebBenchmark.java         // Spring Boot 엔드포인트
```

### 7.3 Python 벤치마크

```python
python-fastapi/tests/benchmark/
├── process_benchmark.py     # pytest-benchmark
├── thread_benchmark.py
└── api_benchmark.py          # locust
```

### 7.4 비교 메트릭
```
1. 생성 시간
   - Process: Java ProcessBuilder vs Python multiprocessing
   - Thread: Java Virtual Thread vs Python asyncio

2. 컨텍스트 스위칭
   - Java: Platform Thread vs Virtual Thread
   - Python: threading vs asyncio

3. 메모리 사용량
   - 프로세스별 메모리
   - 스레드별 메모리

4. 처리량 (Throughput)
   - 동시 요청 처리
   - 초당 작업 수

5. 응답 시간
   - P50, P95, P99
```

---

## Phase 8: 통합 및 문서화

### 목표
전체 시스템 통합과 비교 분석 문서 작성

### 8.1 통합 대시보드

```
shared/dashboard/
├── docker-compose.yml
├── prometheus.yml          # 메트릭 수집
├── grafana/
│   └── dashboards/        # 비교 대시보드
└── scripts/
    └── run-comparison.sh   # 동시 실행 스크립트
```

### 8.2 Docker 통합

```dockerfile
# docker/java.Dockerfile
FROM eclipse-temurin:21-jdk
# Spring Boot 앱 실행

# docker/python.Dockerfile  
FROM python:3.11-slim
# FastAPI 앱 실행
```

### 8.3 비교 문서

```
shared/docs/
├── architecture/
│   ├── java-architecture.md
│   ├── python-architecture.md
│   └── comparison.md
├── experiments/
│   ├── 01-process-isolation.md
│   ├── 02-thread-models.md
│   ├── 03-gil-vs-virtual-thread.md
│   └── 04-framework-comparison.md
└── results/
    ├── performance-report.md
    └── recommendations.md
```

---

## 구현 순서 체크리스트

### Week 1: 기초 설정
- [ ] Java Spring Boot 프로젝트 생성
- [ ] Python FastAPI 프로젝트 생성
- [ ] Docker 환경 구성
- [ ] 기본 API 엔드포인트

### Week 2: Process & Thread 기초
- [ ] Java Process 구현
- [ ] Python Process 구현
- [ ] Java Thread (Platform/Virtual) 구현
- [ ] Python Thread (threading/asyncio) 구현

### Week 3: 동기화 및 웹 통합
- [ ] Java 동기화 메커니즘
- [ ] Python 동기화 메커니즘
- [ ] Spring Boot 통합
- [ ] FastAPI 통합

### Week 4: 고급 기능 및 비교
- [ ] 고급 동시성 패턴
- [ ] 벤치마크 구현
- [ ] 성능 측정
- [ ] 결과 분석

### Week 5: 문서화 및 마무리
- [ ] 통합 테스트
- [ ] 문서 작성
- [ ] 대시보드 구성
- [ ] 최종 검토

---

## 주요 비교 포인트

### 프로세스 레벨
```
Java:
- ProcessBuilder API
- JVM 프로세스 격리
- 메모리 오버헤드

Python:
- multiprocessing 모듈
- GIL 우회
- 프로세스 간 통신
```

### 스레드 레벨
```
Java:
- Platform Thread (OS 스레드)
- Virtual Thread (경량 스레드)
- Spring @Async, WebFlux

Python:
- threading (GIL 제약)
- asyncio (이벤트 루프)
- FastAPI 비동기 처리
```

### 웹 프레임워크
```
Spring Boot:
- Thread-per-request (전통적)
- Reactive (WebFlux)
- Virtual Thread 지원

FastAPI:
- ASGI 서버 (Uvicorn)
- 이벤트 루프 기반
- async/await 네이티브 지원
```

---

## 개발 환경 설정

### Java 환경
```bash
# JDK 21 설치
sdk install java 21-tem

# Gradle 설정
./gradlew build

# Spring Boot 실행
./gradlew bootRun
```

### Python 환경
```bash
# Python 3.11+ 설치
pyenv install 3.11.7
pyenv local 3.11.7

# 가상환경 생성
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate  # Windows

# 의존성 설치
pip install -r requirements.txt

# FastAPI 실행
uvicorn app.main:app --reload --port 8000
```

### Docker 환경
```bash
# 전체 시스템 실행
docker-compose up

# 개별 실행
docker-compose up java-app
docker-compose up python-app
```

---

## 테스트 전략

### Java 테스트
```
- JUnit 5: 단위 테스트
- MockMvc: 컨트롤러 테스트
- JMH: 마이크로 벤치마크
- Testcontainers: 통합 테스트
```

### Python 테스트
```
- pytest: 단위 테스트
- pytest-asyncio: 비동기 테스트
- httpx: API 테스트
- pytest-benchmark: 성능 테스트
```

### 부하 테스트
```
- JMeter: HTTP 부하 테스트
- Locust: Python 기반 부하 테스트
- wrk: 간단한 HTTP 벤치마킹
```

---

이제 Java Spring Boot와 Python FastAPI를 모두 포함한 통합 프로젝트 구조가 완성되었습니다.
각 언어의 특성을 살려 구현하고 직접 비교할 수 있도록 설계했습니다.

### 목표
프로젝트의 기본 구조와 공통 유틸리티 구현

### 구현 순서

#### 1.1 프로젝트 초기 설정
```
1. build.gradle 설정
2. 디렉토리 구조 생성
3. 로깅 설정 (logback.xml)
4. application.properties 설정
```

#### 1.2 Common 유틸리티 구현
```
순서:
1. com.study.common.util.SystemInfo
   - OS 정보 조회
   - JVM 정보 조회
   - 하드웨어 정보 조회

2. com.study.common.util.TimeUtils
   - 나노초 단위 시간 측정
   - 실행 시간 측정 메서드
   - 포맷팅 유틸리티

3. com.study.common.util.FileUtils
   - 파일 읽기/쓰기
   - 임시 파일 생성
   - 디렉토리 관리

4. com.study.common.monitor.BaseMonitor
   - 모니터링 인터페이스 정의
   - 기본 메트릭 수집 로직

5. com.study.common.monitor.MetricsCollector
   - 메트릭 저장 구조
   - 집계 로직
```

#### 1.3 테스트 환경 구축
```
1. JUnit 5 설정 확인
2. 유틸리티 클래스 단위 테스트 작성
3. 테스트 데이터 준비
```

---

## Phase 2: Process 모듈 기초 구현

### 목표
프로세스 생성과 기본 관리 기능 구현

### 구현 순서

#### 2.1 프로세스 생성 기초
```
순서:
1. com.study.process.creation.ChildProcess
   - main 메서드 구현
   - 간단한 작업 수행 로직
   - 종료 코드 처리

2. com.study.process.creation.ProcessCreator
   - ProcessBuilder 래핑
   - 프로세스 시작 메서드
   - 출력 스트림 처리

3. com.study.process.creation.ProcessFactory
   - 다양한 타입의 프로세스 생성
   - 팩토리 패턴 구현

테스트:
- ProcessCreatorTest
- ProcessFactoryTest
```

#### 2.2 프로세스 모니터링
```
순서:
1. com.study.process.monitoring.ProcessMonitor
   - PID 조회
   - isAlive() 체크
   - 기본 상태 모니터링

2. com.study.process.monitoring.ProcessLifecycleTracker
   - 생명주기 이벤트 추적
   - 상태 변화 로깅

테스트:
- ProcessMonitorTest
- ProcessLifecycleTrackerTest
```

#### 2.3 프로세스 관리
```
순서:
1. com.study.process.management.ProcessManager
   - 프로세스 목록 관리
   - 시작/종료 메서드
   - 타임아웃 처리

테스트:
- ProcessManagerTest
```

---

## Phase 3: Thread 모듈 기초 구현

### 목표
스레드 생명주기와 기본 동작 구현

### 구현 순서

#### 3.1 스레드 생명주기
```
순서:
1. com.study.thread.lifecycle.ThreadStateDemo
   - 스레드 상태 전이 데모
   - 각 상태별 예제 코드
   - 상태 출력 메서드

2. com.study.thread.lifecycle.ThreadLifecycleObserver
   - 상태 변화 감지
   - 이벤트 리스너 패턴
   - 로깅 및 기록

3. com.study.thread.lifecycle.PlatformThreadDemo
   - 전통적인 스레드 생성
   - Thread 클래스 사용
   - Runnable 인터페이스 구현

테스트:
- ThreadStateDemoTest
- PlatformThreadTest
```

#### 3.2 Virtual Thread 구현 (Java 21)
```
순서:
1. com.study.thread.lifecycle.VirtualThreadDemo
   - Virtual Thread 생성
   - 대량 스레드 생성 테스트
   - 성능 특성 확인

2. com.study.thread.lifecycle.ThreadCreationComparison
   - Platform vs Virtual 비교
   - 생성 시간 측정
   - 메모리 사용량 비교

테스트:
- VirtualThreadTest
- ThreadCreationComparisonTest
```

---

## Phase 4: 동기화 메커니즘 구현

### 목표
스레드 동기화와 동시성 제어 구현

### 구현 순서

#### 4.1 기본 동기화
```
순서:
1. com.study.thread.synchronization.MutexDemo
   - synchronized 키워드
   - 임계 영역 보호
   - 상호 배제 구현

2. com.study.thread.synchronization.MonitorDemo
   - wait/notify 메커니즘
   - 조건 변수 사용
   - 모니터 패턴

3. com.study.thread.synchronization.SemaphoreDemo
   - Semaphore 클래스 사용
   - 카운팅 세마포어
   - 리소스 풀 관리

테스트:
- MutexDemoTest
- MonitorDemoTest
- SemaphoreDemoTest
```

#### 4.2 고급 동기화
```
순서:
1. com.study.thread.synchronization.ReentrantLockDemo
   - Lock 인터페이스
   - tryLock 메커니즘
   - Condition 사용

2. com.study.thread.synchronization.ReadWriteLockDemo
   - 읽기/쓰기 락 분리
   - 성능 최적화
   - 공정성 설정

3. com.study.thread.synchronization.StampedLockDemo
   - 낙관적 읽기
   - 스탬프 기반 락
   - 성능 비교

테스트:
- ReentrantLockDemoTest
- ReadWriteLockDemoTest
- StampedLockDemoTest
```

#### 4.3 동기화 문제 시뮬레이션
```
순서:
1. com.study.thread.synchronization.ProducerConsumer
   - BlockingQueue 사용
   - wait/notify 구현
   - 성능 측정

2. com.study.thread.synchronization.DiningPhilosophers
   - 데드락 시나리오
   - 해결책 구현
   - 시각화

3. com.study.thread.synchronization.DeadlockSimulator
   - 의도적 데드락 생성
   - 순환 대기 구현

4. com.study.thread.synchronization.DeadlockDetector
   - ThreadMXBean 사용
   - 데드락 감지 알고리즘
   - 복구 메커니즘

테스트:
- ProducerConsumerTest
- DiningPhilosophersTest
- DeadlockSimulatorTest
- DeadlockDetectorTest
```

---

## Phase 5: 고급 기능 구현

### 목표
프로세스 간 통신, 스레드 풀, 스케줄링 구현

### 구현 순서

#### 5.1 프로세스 간 통신 (IPC)
```
순서:
1. com.study.process.communication.PipeIPC
   - 파이프 생성
   - 단방향 통신
   - 데이터 전송

2. com.study.process.communication.SocketIPC
   - TCP 소켓 구현
   - 서버/클라이언트
   - 메시지 프로토콜

3. com.study.process.communication.SharedMemoryIPC
   - MappedByteBuffer 사용
   - 메모리 동기화
   - 성능 최적화

4. com.study.process.communication.MessageQueue
   - 큐 기반 통신
   - 비동기 메시징
   - 메시지 라우팅

테스트:
- PipeIPCTest
- SocketIPCTest
- SharedMemoryIPCTest
- MessageQueueTest
```

#### 5.2 스레드 풀 구현
```
순서:
1. com.study.thread.pool.CustomThreadPool
   - 기본 풀 구조
   - 워커 스레드 관리
   - 태스크 큐

2. com.study.thread.pool.FixedThreadPoolDemo
   - Executors 사용
   - 고정 크기 풀
   - 성능 특성

3. com.study.thread.pool.CachedThreadPoolDemo
   - 동적 크기 조정
   - 스레드 재사용
   - 타임아웃 처리

4. com.study.thread.pool.VirtualThreadExecutor
   - Virtual Thread 전용
   - 대량 동시성
   - 성능 최적화

5. com.study.thread.pool.ThreadPoolMonitor
   - 풀 상태 모니터링
   - 메트릭 수집
   - 성능 분석

테스트:
- CustomThreadPoolTest
- ThreadPoolPerformanceTest
- VirtualThreadExecutorTest
```

#### 5.3 스케줄링 알고리즘
```
순서:
1. com.study.thread.scheduling.FCFSScheduler
   - FIFO 큐 구현
   - 순차 처리

2. com.study.thread.scheduling.SJFScheduler
   - 작업 시간 예측
   - 우선순위 큐

3. com.study.thread.scheduling.RoundRobinScheduler
   - 타임 슬라이스
   - 순환 스케줄링

4. com.study.thread.scheduling.PriorityScheduler
   - 우선순위 관리
   - 동적 우선순위

5. com.study.thread.scheduling.MLFQScheduler
   - 다단계 큐
   - 피드백 메커니즘

테스트:
- 각 스케줄러별 테스트
- SchedulerComparisonTest
```

---

## Phase 6: 성능 측정 및 비교

### 목표
종합적인 성능 측정과 분석

### 구현 순서

#### 6.1 벤치마크 구현
```
순서:
1. com.study.comparison.performance.CreationBenchmark
   - JMH 설정
   - 생성 시간 측정
   - 결과 분석

2. com.study.comparison.performance.ContextSwitchBenchmark
   - 컨텍스트 스위칭 측정
   - 오버헤드 분석

3. com.study.comparison.performance.IOBenchmark
   - I/O 성능 측정
   - 블로킹/논블로킹 비교

4. com.study.comparison.performance.CPUBenchmark
   - CPU 집약 작업
   - 병렬 처리 성능

테스트:
- 벤치마크 검증 테스트
```

#### 6.2 리소스 분석
```
순서:
1. com.study.comparison.resource.MemoryUsageAnalyzer
   - 힙 메모리 분석
   - 네이티브 메모리
   - GC 영향 측정

2. com.study.comparison.resource.CPUUsageAnalyzer
   - CPU 사용률
   - 코어별 분석
   - 스레드별 사용량

3. com.study.comparison.resource.ResourceProfiler
   - JFR 통합
   - 상세 프로파일링
   - 보고서 생성

테스트:
- ResourceAnalyzerTest
```

#### 6.3 동시성 분석
```
순서:
1. com.study.comparison.concurrency.ConcurrentTaskExecutor
   - 동시 실행 테스트
   - 처리량 측정

2. com.study.comparison.concurrency.ParallelismAnalyzer
   - 실제 병렬성 측정
   - Amdahl's Law 검증

3. com.study.comparison.concurrency.RaceConditionDetector
   - 경쟁 조건 감지
   - Thread Sanitizer 통합

테스트:
- ConcurrencyAnalysisTest
```

---

## Phase 7: 통합 및 최적화

### 목표
전체 시스템 통합과 최적화

### 구현 순서

#### 7.1 메인 애플리케이션
```
순서:
1. com.study.Main
   - 명령행 인터페이스
   - 메뉴 시스템
   - 데모 선택

2. com.study.DemoRunner
   - 데모 오케스트레이션
   - 시나리오 실행
   - 결과 수집
```

#### 7.2 모니터링 시스템
```
순서:
1. com.study.common.monitor.PerformanceMonitor
   - 실시간 모니터링
   - 메트릭 집계

2. com.study.common.monitor.JFRRecorder
   - JFR 이벤트 정의
   - 레코딩 관리

3. com.study.common.monitor.DashboardServer
   - 웹 서버 구현
   - 실시간 차트
   - REST API
```

#### 7.3 보고서 생성
```
순서:
1. com.study.common.util.ReportGenerator
   - HTML 보고서
   - Markdown 문서
   - 차트 생성

2. com.study.process.communication.IPCBenchmark
   - 종합 벤치마크
   - 비교 분석

3. com.study.thread.scheduling.SchedulerComparison
   - 알고리즘 비교
   - 성능 그래프
```

#### 7.4 통합 테스트
```
순서:
1. 엔드투엔드 테스트 작성
2. 성능 회귀 테스트
3. 스트레스 테스트
4. 메모리 누수 테스트
```

---

## 구현 팁과 주의사항

### 각 Phase별 체크리스트

#### Phase 1 체크리스트
- [ ] Gradle 빌드 성공
- [ ] 로깅 출력 확인
- [ ] 유틸리티 테스트 통과
- [ ] 시스템 정보 정확히 출력

#### Phase 2 체크리스트
- [ ] 프로세스 생성/종료 정상 동작
- [ ] 자식 프로세스 독립 실행
- [ ] 프로세스 모니터링 데이터 수집
- [ ] 예외 처리 구현

#### Phase 3 체크리스트
- [ ] 스레드 상태 전이 확인
- [ ] Platform Thread 생성
- [ ] Virtual Thread 생성 (Java 21)
- [ ] 생명주기 추적 동작

#### Phase 4 체크리스트
- [ ] 동기화 메커니즘 정상 동작
- [ ] 데드락 시나리오 재현
- [ ] 데드락 감지 및 로깅
- [ ] 경쟁 조건 방지

#### Phase 5 체크리스트
- [ ] IPC 통신 성공
- [ ] 스레드 풀 동작 확인
- [ ] 스케줄링 알고리즘 구현
- [ ] 성능 기준 충족

#### Phase 6 체크리스트
- [ ] JMH 벤치마크 실행
- [ ] 성능 데이터 수집
- [ ] 리소스 사용량 측정
- [ ] 비교 분석 완료

#### Phase 7 체크리스트
- [ ] 전체 시스템 통합 완료
- [ ] 모니터링 대시보드 동작
- [ ] 보고서 생성 성공
- [ ] 모든 테스트 통과

### 개발 환경 설정

#### 필수 도구
```
1. JDK 21 설치 및 JAVA_HOME 설정
2. Gradle 8.5+ 설치
3. IDE 설정 (IntelliJ IDEA 권장)
   - Enable preview features
   - Gradle 통합 확인
4. Git 설정
```

#### IDE 설정 (IntelliJ IDEA)
```
1. File → Project Structure
   - Project SDK: JDK 21
   - Language Level: 21 (Preview)

2. Settings → Build → Compiler → Java Compiler
   - Additional command line parameters:
     --enable-preview

3. Run Configuration
   - VM options: --enable-preview
   - Environment variables 설정
```

### 코딩 규칙

#### 네이밍 컨벤션
```java
- 클래스명: PascalCase (예: ProcessCreator)
- 메서드명: camelCase (예: createProcess)
- 상수: UPPER_SNAKE_CASE (예: MAX_THREADS)
- 패키지명: 소문자 (예: com.study.process)
```

#### 문서화
```java
- 모든 public 메서드에 JavaDoc 작성
- 복잡한 로직에 인라인 주석
- README 파일 업데이트
- 실험 결과 문서화
```

### 디버깅 팁

#### 프로세스 디버깅
```
1. 프로세스 출력 스트림 리다이렉션
2. 종료 코드 확인
3. jps 명령어로 JVM 프로세스 확인
4. Process Explorer/Activity Monitor 사용
```

#### 스레드 디버깅
```
1. jstack으로 스레드 덤프
2. IDE 스레드 뷰 활용
3. ThreadMXBean으로 상태 확인
4. 동기화 포인트에 브레이크포인트
```

#### 성능 분석
```
1. JFR (Java Flight Recorder) 활용
2. async-profiler 사용
3. JMH 결과 분석
4. GC 로그 분석
```

### 자주 발생하는 문제와 해결

#### Virtual Thread 관련
```
문제: Virtual Thread가 생성되지 않음
해결: 
- JDK 21 확인
- --enable-preview 플래그 확인
- 캐리어 스레드 풀 설정 확인
```

#### IPC 관련
```
문제: 프로세스 간 통신 실패
해결:
- 포트 사용 가능 여부 확인
- 파이프 이름 충돌 확인
- 권한 문제 확인
```

#### 메모리 관련
```
문제: OutOfMemoryError
해결:
- JVM 힙 크기 증가 (-Xmx)
- 스레드 스택 크기 조정 (-Xss)
- 메모리 누수 확인
```

---

## 완성도 체크리스트

### 코드 품질
- [ ] 코드 리뷰 완료
- [ ] 정적 분석 통과 (SonarQube)
- [ ] 테스트 커버리지 80% 이상
- [ ] 문서화 완료

### 기능 완성도
- [ ] 모든 Phase 구현 완료
- [ ] 통합 테스트 통과
- [ ] 성능 목표 달성
- [ ] 예외 처리 구현

### 문서화
- [ ] JavaDoc 작성
- [ ] README 업데이트
- [ ] 실험 결과 문서화
- [ ] 아키텍처 문서 작성

### 배포 준비
- [ ] 빌드 스크립트 작성
- [ ] 실행 스크립트 작성
- [ ] 의존성 정리
- [ ] 라이센스 확인

---

## 다음 단계

프로젝트 완료 후 추가 학습 및 확장 가능한 영역:

1. **분산 시스템**: 여러 머신에서의 프로세스 관리
2. **컨테이너화**: Docker를 사용한 프로세스 격리
3. **클라우드 네이티브**: Kubernetes에서의 스케줄링
4. **반응형 프로그래밍**: Project Reactor, RxJava
5. **Actor 모델**: Akka를 사용한 동시성
6. **코루틴**: Kotlin 코루틴과의 비교

---

이 구현 순서를 따라 진행하면 체계적으로 프로젝트를 완성할 수 있습니다.
각 Phase는 독립적으로 테스트 가능하며, 점진적으로 복잡도가 증가합니다.