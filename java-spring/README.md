~~# process_thread

프로세스와 스레드의 동작 방식을 실습하고 탐구하는 Java 기반 학습 프로젝트

## 프로젝트 개요

본 프로젝트는 운영체제의 핵심 개념인 프로세스와 스레드를 Java 환경에서 실제로 구현하고 실험해보는 것을 목표로 합니다. 이론적 개념을 코드로 구현하여 프로세스와 스레드의 생명주기, 상태 전이, 동기화, 스케줄링 등을 직접 관찰하고 분석할 수 있습니다.

## 학습 목표

### 프로세스 레벨
- JVM 프로세스의 생성과 종료
- 프로세스 간 통신 (IPC) 메커니즘 구현
- 프로세스 격리와 독립성 확인
- 프로세스 모니터링 및 리소스 사용량 측정

### 스레드 레벨
- 스레드 생명주기 및 상태 전이 관찰
- 스레드 풀과 스케줄링 메커니즘 구현
- 동기화 기법 (mutex, semaphore, monitor) 실습
- 데드락과 경쟁 조건 시뮬레이션

### 비교 분석
- 프로세스 vs 스레드 성능 비교
- 컨텍스트 스위칭 오버헤드 측정
- 메모리 사용량 및 리소스 효율성 분석
- 동시성과 병렬성 차이 실증

## 기술 스택

- **Language**: Java 21 (LTS)
- **Build Tool**: Gradle 8.5
- **Testing**: JUnit 5, AssertJ
- **Monitoring**: JMX, MXBean, JFR (Java Flight Recorder)
- **Visualization**: JFreeChart (선택사항)
- **Benchmarking**: JMH (Java Microbenchmark Harness)

## 프로젝트 구조

```
process_thread/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── study/
│   │   │           ├── process/
│   │   │           │   ├── creation/
│   │   │           │   │   ├── ProcessCreator.java              # ProcessBuilder를 사용한 프로세스 생성
│   │   │           │   │   ├── ProcessFactory.java              # 다양한 타입의 프로세스 생성 팩토리
│   │   │           │   │   ├── ChildProcess.java                # 자식 프로세스로 실행될 메인 클래스
│   │   │           │   │   └── ProcessSpawner.java              # 병렬 프로세스 생성 및 관리
│   │   │           │   ├── communication/
│   │   │           │   │   ├── SocketIPC.java                   # 소켓 기반 IPC 구현
│   │   │           │   │   ├── PipeIPC.java                     # 파이프 기반 IPC 구현
│   │   │           │   │   ├── SharedMemoryIPC.java             # 메모리 맵 파일을 통한 IPC
│   │   │           │   │   ├── MessageQueue.java                # 메시지 큐 시뮬레이션
│   │   │           │   │   └── IPCBenchmark.java                # IPC 성능 측정
│   │   │           │   ├── monitoring/
│   │   │           │   │   ├── ProcessMonitor.java              # 프로세스 상태 모니터링
│   │   │           │   │   ├── ProcessResourceTracker.java      # CPU, 메모리 사용량 추적
│   │   │           │   │   ├── ProcessLifecycleTracker.java    # 프로세스 생명주기 추적
│   │   │           │   │   └── ProcessMetrics.java              # 프로세스 메트릭 수집 및 분석
│   │   │           │   └── management/
│   │   │           │       ├── ProcessManager.java              # 프로세스 생성/종료 관리
│   │   │           │       ├── ProcessPool.java                 # 프로세스 풀 구현
│   │   │           │       ├── ProcessSchedulerSimulator.java   # 프로세스 스케줄링 시뮬레이션
│   │   │           │       └── ProcessOrchestrator.java         # 복잡한 프로세스 워크플로우 관리
│   │   │           ├── thread/
│   │   │           │   ├── lifecycle/
│   │   │           │   │   ├── ThreadStateDemo.java             # 스레드 상태 전이 데모
│   │   │           │   │   ├── ThreadLifecycleObserver.java     # 스레드 생명주기 관찰
│   │   │           │   │   ├── PlatformThreadDemo.java          # Platform Thread 실습
│   │   │           │   │   ├── VirtualThreadDemo.java           # Virtual Thread 실습
│   │   │           │   │   └── ThreadCreationComparison.java    # 스레드 생성 방식 비교
│   │   │           │   ├── synchronization/
│   │   │           │   │   ├── MutexDemo.java                   # Mutex(상호배제) 구현
│   │   │           │   │   ├── SemaphoreDemo.java               # Semaphore 구현 및 실습
│   │   │           │   │   ├── MonitorDemo.java                 # Monitor 패턴 구현
│   │   │           │   │   ├── ReentrantLockDemo.java           # ReentrantLock 실습
│   │   │           │   │   ├── ReadWriteLockDemo.java           # ReadWriteLock 실습
│   │   │           │   │   ├── StampedLockDemo.java             # StampedLock 실습
│   │   │           │   │   ├── ProducerConsumer.java            # 생산자-소비자 문제
│   │   │           │   │   ├── DiningPhilosophers.java          # 철학자들의 만찬 문제
│   │   │           │   │   ├── DeadlockSimulator.java           # 데드락 시뮬레이션
│   │   │           │   │   └── DeadlockDetector.java            # 데드락 탐지 및 해결
│   │   │           │   ├── pool/
│   │   │           │   │   ├── CustomThreadPool.java            # 커스텀 스레드 풀 구현
│   │   │           │   │   ├── FixedThreadPoolDemo.java         # FixedThreadPool 실습
│   │   │           │   │   ├── CachedThreadPoolDemo.java        # CachedThreadPool 실습
│   │   │           │   │   ├── ScheduledThreadPoolDemo.java     # ScheduledThreadPool 실습
│   │   │           │   │   ├── WorkStealingPoolDemo.java        # Work-Stealing Pool 실습
│   │   │           │   │   ├── VirtualThreadExecutor.java       # Virtual Thread 전용 Executor
│   │   │           │   │   └── ThreadPoolMonitor.java           # 스레드 풀 모니터링
│   │   │           │   ├── scheduling/
│   │   │           │   │   ├── FCFSScheduler.java               # First-Come First-Served 구현
│   │   │           │   │   ├── SJFScheduler.java                # Shortest Job First 구현
│   │   │           │   │   ├── RoundRobinScheduler.java         # Round Robin 구현
│   │   │           │   │   ├── PriorityScheduler.java           # Priority 스케줄링 구현
│   │   │           │   │   ├── MLFQScheduler.java               # Multi-Level Feedback Queue 구현
│   │   │           │   │   └── SchedulerComparison.java         # 스케줄러 성능 비교
│   │   │           │   └── structured/
│   │   │           │       ├── StructuredTaskScope.java         # Java 21 Structured Concurrency
│   │   │           │       ├── ScopedValueDemo.java             # Scoped Values 실습
│   │   │           │       └── StructuredPatterns.java          # 구조화된 동시성 패턴
│   │   │           ├── comparison/
│   │   │           │   ├── performance/
│   │   │           │   │   ├── CreationBenchmark.java           # 생성 시간 벤치마크
│   │   │           │   │   ├── ContextSwitchBenchmark.java      # 컨텍스트 스위칭 벤치마크
│   │   │           │   │   ├── IOBenchmark.java                 # I/O 작업 벤치마크
│   │   │           │   │   ├── CPUBenchmark.java                # CPU 집약 작업 벤치마크
│   │   │           │   │   ├── ScalabilityTest.java             # 확장성 테스트
│   │   │           │   │   └── ThroughputAnalyzer.java          # 처리량 분석
│   │   │           │   ├── resource/
│   │   │           │   │   ├── MemoryUsageAnalyzer.java         # 메모리 사용량 분석
│   │   │           │   │   ├── CPUUsageAnalyzer.java            # CPU 사용률 분석
│   │   │           │   │   ├── ResourceProfiler.java            # 리소스 프로파일링
│   │   │           │   │   └── ResourceComparison.java          # 리소스 사용량 비교
│   │   │           │   └── concurrency/
│   │   │           │       ├── ConcurrentTaskExecutor.java      # 동시 작업 실행 비교
│   │   │           │       ├── ParallelismAnalyzer.java         # 병렬성 분석
│   │   │           │       ├── RaceConditionDetector.java       # 경쟁 조건 탐지
│   │   │           │       └── ConcurrencyPatterns.java         # 동시성 패턴 비교
│   │   │           ├── common/
│   │   │           │   ├── util/
│   │   │           │   │   ├── SystemInfo.java                  # 시스템 정보 조회
│   │   │           │   │   ├── TimeUtils.java                   # 시간 측정 유틸리티
│   │   │           │   │   ├── RandomDataGenerator.java         # 테스트 데이터 생성
│   │   │           │   │   ├── FileUtils.java                   # 파일 작업 유틸리티
│   │   │           │   │   └── ReportGenerator.java             # 보고서 생성
│   │   │           │   └── monitor/
│   │   │           │       ├── BaseMonitor.java                 # 모니터링 기본 클래스
│   │   │           │       ├── MetricsCollector.java            # 메트릭 수집기
│   │   │           │       ├── PerformanceMonitor.java          # 성능 모니터
│   │   │           │       ├── JFRRecorder.java                 # Java Flight Recorder 래퍼
│   │   │           │       └── DashboardServer.java             # 모니터링 대시보드 서버
│   │   │           ├── Main.java                                # 메인 애플리케이션 진입점
│   │   │           └── DemoRunner.java                          # 데모 실행 관리자
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback.xml
│   │       ├── benchmark.properties                             # 벤치마크 설정
│   │       └── demo-scripts/                                    # 데모 실행 스크립트
│   │           ├── process-demo.txt
│   │           └── thread-demo.txt
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── study/
│       │           ├── process/
│       │           │   ├── creation/
│       │           │   │   ├── ProcessCreatorTest.java          # 프로세스 생성 테스트
│       │           │   │   ├── ProcessFactoryTest.java          # 팩토리 패턴 테스트
│       │           │   │   └── ProcessSpawnerTest.java          # 병렬 프로세스 생성 테스트
│       │           │   ├── communication/
│       │           │   │   ├── SocketIPCTest.java               # 소켓 IPC 통신 테스트
│       │           │   │   ├── PipeIPCTest.java                 # 파이프 IPC 테스트
│       │           │   │   ├── SharedMemoryIPCTest.java         # 공유 메모리 테스트
│       │           │   │   └── MessageQueueTest.java            # 메시지 큐 테스트
│       │           │   ├── monitoring/
│       │           │   │   ├── ProcessMonitorTest.java          # 프로세스 모니터링 테스트
│       │           │   │   └── ProcessResourceTrackerTest.java  # 리소스 추적 테스트
│       │           │   └── management/
│       │           │       ├── ProcessManagerTest.java          # 프로세스 관리 테스트
│       │           │       └── ProcessPoolTest.java             # 프로세스 풀 테스트
│       │           ├── thread/
│       │           │   ├── lifecycle/
│       │           │   │   ├── ThreadStateDemoTest.java         # 스레드 상태 테스트
│       │           │   │   ├── PlatformThreadTest.java          # Platform Thread 테스트
│       │           │   │   └── VirtualThreadTest.java           # Virtual Thread 테스트
│       │           │   ├── synchronization/
│       │           │   │   ├── MutexDemoTest.java               # Mutex 동작 테스트
│       │           │   │   ├── SemaphoreDemoTest.java           # Semaphore 테스트
│       │           │   │   ├── ProducerConsumerTest.java        # 생산자-소비자 테스트
│       │           │   │   ├── DeadlockSimulatorTest.java       # 데드락 발생 테스트
│       │           │   │   └── DeadlockDetectorTest.java        # 데드락 탐지 테스트
│       │           │   ├── pool/
│       │           │   │   ├── CustomThreadPoolTest.java        # 커스텀 풀 테스트
│       │           │   │   ├── ThreadPoolPerformanceTest.java   # 스레드 풀 성능 테스트
│       │           │   │   └── VirtualThreadExecutorTest.java   # Virtual Thread Executor 테스트
│       │           │   └── scheduling/
│       │           │       ├── FCFSSchedulerTest.java           # FCFS 알고리즘 테스트
│       │           │       ├── SJFSchedulerTest.java            # SJF 알고리즘 테스트
│       │           │       ├── RoundRobinSchedulerTest.java     # Round Robin 테스트
│       │           │       └── SchedulerComparisonTest.java     # 스케줄러 비교 테스트
│       │           └── comparison/
│       │               ├── performance/
│       │               │   ├── CreationBenchmarkTest.java       # 생성 벤치마크 검증
│       │               │   └── ScalabilityTestTest.java         # 확장성 테스트 검증
│       │               ├── resource/
│       │               │   └── ResourceAnalyzerTest.java        # 리소스 분석기 테스트
│       │               └── concurrency/
│       │                   └── RaceConditionDetectorTest.java   # 경쟁 조건 탐지 테스트
│       └── resources/
│           ├── test-application.properties                      # 테스트 설정
│           └── test-data/                                       # 테스트 데이터
│               ├── process-test-data.json
│               └── thread-test-data.json
├── docs/
│   ├── architecture/
│   │   ├── system-design.md                                     # 시스템 설계 문서
│   │   ├── class-diagrams.puml                                  # UML 클래스 다이어그램
│   │   └── sequence-diagrams.puml                               # UML 시퀀스 다이어그램
│   ├── experiments/
│   │   ├── experiment-01-process-isolation.md                   # 프로세스 격리 실험
│   │   ├── experiment-02-thread-sharing.md                      # 스레드 메모리 공유 실험
│   │   ├── experiment-03-virtual-thread-scale.md                # Virtual Thread 확장성 실험
│   │   ├── experiment-04-deadlock-scenarios.md                  # 데드락 시나리오 실험
│   │   └── experiment-05-performance-comparison.md              # 성능 비교 실험
│   └── results/
│       ├── benchmark-results.md                                 # 벤치마크 결과
│       ├── performance-analysis.md                              # 성능 분석 보고서
│       └── charts/                                              # 결과 차트
│           ├── creation-time-comparison.png
│           ├── memory-usage-comparison.png
│           └── throughput-analysis.png
├── scripts/
│   ├── run-process-demo.sh                                      # 프로세스 데모 실행
│   ├── run-thread-demo.sh                                       # 스레드 데모 실행
│   ├── run-virtual-thread-demo.sh                               # Virtual Thread 데모 실행
│   ├── run-all-benchmarks.sh                                    # 모든 벤치마크 실행
│   ├── generate-report.sh                                       # 보고서 생성
│   └── setup-environment.sh                                     # 환경 설정
├── logs/                                                         # 로그 디렉토리
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── LICENSEprocess_thread/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── study/
│   │   │           ├── process/
│   │   │           │   ├── creation/
│   │   │           │   │   ├── ProcessCreator.java              # ProcessBuilder를 사용한 프로세스 생성
│   │   │           │   │   ├── ProcessFactory.java              # 다양한 타입의 프로세스 생성 팩토리
│   │   │           │   │   ├── ChildProcess.java                # 자식 프로세스로 실행될 메인 클래스
│   │   │           │   │   └── ProcessSpawner.java              # 병렬 프로세스 생성 및 관리
│   │   │           │   ├── communication/
│   │   │           │   │   ├── SocketIPC.java                   # 소켓 기반 IPC 구현
│   │   │           │   │   ├── PipeIPC.java                     # 파이프 기반 IPC 구현
│   │   │           │   │   ├── SharedMemoryIPC.java             # 메모리 맵 파일을 통한 IPC
│   │   │           │   │   ├── MessageQueue.java                # 메시지 큐 시뮬레이션
│   │   │           │   │   └── IPCBenchmark.java                # IPC 성능 측정
│   │   │           │   ├── monitoring/
│   │   │           │   │   ├── ProcessMonitor.java              # 프로세스 상태 모니터링
│   │   │           │   │   ├── ProcessResourceTracker.java      # CPU, 메모리 사용량 추적
│   │   │           │   │   ├── ProcessLifecycleTracker.java    # 프로세스 생명주기 추적
│   │   │           │   │   └── ProcessMetrics.java              # 프로세스 메트릭 수집 및 분석
│   │   │           │   └── management/
│   │   │           │       ├── ProcessManager.java              # 프로세스 생성/종료 관리
│   │   │           │       ├── ProcessPool.java                 # 프로세스 풀 구현
│   │   │           │       ├── ProcessSchedulerSimulator.java   # 프로세스 스케줄링 시뮬레이션
│   │   │           │       └── ProcessOrchestrator.java         # 복잡한 프로세스 워크플로우 관리
│   │   │           ├── thread/
│   │   │           │   ├── lifecycle/
│   │   │           │   │   ├── ThreadStateDemo.java             # 스레드 상태 전이 데모
│   │   │           │   │   ├── ThreadLifecycleObserver.java     # 스레드 생명주기 관찰
│   │   │           │   │   ├── PlatformThreadDemo.java          # Platform Thread 실습
│   │   │           │   │   ├── VirtualThreadDemo.java           # Virtual Thread 실습
│   │   │           │   │   └── ThreadCreationComparison.java    # 스레드 생성 방식 비교
│   │   │           │   ├── synchronization/
│   │   │           │   │   ├── MutexDemo.java                   # Mutex(상호배제) 구현
│   │   │           │   │   ├── SemaphoreDemo.java               # Semaphore 구현 및 실습
│   │   │           │   │   ├── MonitorDemo.java                 # Monitor 패턴 구현
│   │   │           │   │   ├── ReentrantLockDemo.java           # ReentrantLock 실습
│   │   │           │   │   ├── ReadWriteLockDemo.java           # ReadWriteLock 실습
│   │   │           │   │   ├── StampedLockDemo.java             # StampedLock 실습
│   │   │           │   │   ├── ProducerConsumer.java            # 생산자-소비자 문제
│   │   │           │   │   ├── DiningPhilosophers.java          # 철학자들의 만찬 문제
│   │   │           │   │   ├── DeadlockSimulator.java           # 데드락 시뮬레이션
│   │   │           │   │   └── DeadlockDetector.java            # 데드락 탐지 및 해결
│   │   │           │   ├── pool/
│   │   │           │   │   ├── CustomThreadPool.java            # 커스텀 스레드 풀 구현
│   │   │           │   │   ├── FixedThreadPoolDemo.java         # FixedThreadPool 실습
│   │   │           │   │   ├── CachedThreadPoolDemo.java        # CachedThreadPool 실습
│   │   │           │   │   ├── ScheduledThreadPoolDemo.java     # ScheduledThreadPool 실습
│   │   │           │   │   ├── WorkStealingPoolDemo.java        # Work-Stealing Pool 실습
│   │   │           │   │   ├── VirtualThreadExecutor.java       # Virtual Thread 전용 Executor
│   │   │           │   │   └── ThreadPoolMonitor.java           # 스레드 풀 모니터링
│   │   │           │   ├── scheduling/
│   │   │           │   │   ├── FCFSScheduler.java               # First-Come First-Served 구현
│   │   │           │   │   ├── SJFScheduler.java                # Shortest Job First 구현
│   │   │           │   │   ├── RoundRobinScheduler.java         # Round Robin 구현
│   │   │           │   │   ├── PriorityScheduler.java           # Priority 스케줄링 구현
│   │   │           │   │   ├── MLFQScheduler.java               # Multi-Level Feedback Queue 구현
│   │   │           │   │   └── SchedulerComparison.java         # 스케줄러 성능 비교
│   │   │           │   └── structured/
│   │   │           │       ├── StructuredTaskScope.java         # Java 21 Structured Concurrency
│   │   │           │       ├── ScopedValueDemo.java             # Scoped Values 실습
│   │   │           │       └── StructuredPatterns.java          # 구조화된 동시성 패턴
│   │   │           ├── comparison/
│   │   │           │   ├── performance/
│   │   │           │   │   ├── CreationBenchmark.java           # 생성 시간 벤치마크
│   │   │           │   │   ├── ContextSwitchBenchmark.java      # 컨텍스트 스위칭 벤치마크
│   │   │           │   │   ├── IOBenchmark.java                 # I/O 작업 벤치마크
│   │   │           │   │   ├── CPUBenchmark.java                # CPU 집약 작업 벤치마크
│   │   │           │   │   ├── ScalabilityTest.java             # 확장성 테스트
│   │   │           │   │   └── ThroughputAnalyzer.java          # 처리량 분석
│   │   │           │   ├── resource/
│   │   │           │   │   ├── MemoryUsageAnalyzer.java         # 메모리 사용량 분석
│   │   │           │   │   ├── CPUUsageAnalyzer.java            # CPU 사용률 분석
│   │   │           │   │   ├── ResourceProfiler.java            # 리소스 프로파일링
│   │   │           │   │   └── ResourceComparison.java          # 리소스 사용량 비교
│   │   │           │   └── concurrency/
│   │   │           │       ├── ConcurrentTaskExecutor.java      # 동시 작업 실행 비교
│   │   │           │       ├── ParallelismAnalyzer.java         # 병렬성 분석
│   │   │           │       ├── RaceConditionDetector.java       # 경쟁 조건 탐지
│   │   │           │       └── ConcurrencyPatterns.java         # 동시성 패턴 비교
│   │   │           ├── common/
│   │   │           │   ├── util/
│   │   │           │   │   ├── SystemInfo.java                  # 시스템 정보 조회
│   │   │           │   │   ├── TimeUtils.java                   # 시간 측정 유틸리티
│   │   │           │   │   ├── RandomDataGenerator.java         # 테스트 데이터 생성
│   │   │           │   │   ├── FileUtils.java                   # 파일 작업 유틸리티
│   │   │           │   │   └── ReportGenerator.java             # 보고서 생성
│   │   │           │   └── monitor/
│   │   │           │       ├── BaseMonitor.java                 # 모니터링 기본 클래스
│   │   │           │       ├── MetricsCollector.java            # 메트릭 수집기
│   │   │           │       ├── PerformanceMonitor.java          # 성능 모니터
│   │   │           │       ├── JFRRecorder.java                 # Java Flight Recorder 래퍼
│   │   │           │       └── DashboardServer.java             # 모니터링 대시보드 서버
│   │   │           ├── Main.java                                # 메인 애플리케이션 진입점
│   │   │           └── DemoRunner.java                          # 데모 실행 관리자
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback.xml
│   │       ├── benchmark.properties                             # 벤치마크 설정
│   │       └── demo-scripts/                                    # 데모 실행 스크립트
│   │           ├── process-demo.txt
│   │           └── thread-demo.txt
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── study/
│       │           ├── process/
│       │           │   ├── creation/
│       │           │   │   ├── ProcessCreatorTest.java          # 프로세스 생성 테스트
│       │           │   │   ├── ProcessFactoryTest.java          # 팩토리 패턴 테스트
│       │           │   │   └── ProcessSpawnerTest.java          # 병렬 프로세스 생성 테스트
│       │           │   ├── communication/
│       │           │   │   ├── SocketIPCTest.java               # 소켓 IPC 통신 테스트
│       │           │   │   ├── PipeIPCTest.java                 # 파이프 IPC 테스트
│       │           │   │   ├── SharedMemoryIPCTest.java         # 공유 메모리 테스트
│       │           │   │   └── MessageQueueTest.java            # 메시지 큐 테스트
│       │           │   ├── monitoring/
│       │           │   │   ├── ProcessMonitorTest.java          # 프로세스 모니터링 테스트
│       │           │   │   └── ProcessResourceTrackerTest.java  # 리소스 추적 테스트
│       │           │   └── management/
│       │           │       ├── ProcessManagerTest.java          # 프로세스 관리 테스트
│       │           │       └── ProcessPoolTest.java             # 프로세스 풀 테스트
│       │           ├── thread/
│       │           │   ├── lifecycle/
│       │           │   │   ├── ThreadStateDemoTest.java         # 스레드 상태 테스트
│       │           │   │   ├── PlatformThreadTest.java          # Platform Thread 테스트
│       │           │   │   └── VirtualThreadTest.java           # Virtual Thread 테스트
│       │           │   ├── synchronization/
│       │           │   │   ├── MutexDemoTest.java               # Mutex 동작 테스트
│       │           │   │   ├── SemaphoreDemoTest.java           # Semaphore 테스트
│       │           │   │   ├── ProducerConsumerTest.java        # 생산자-소비자 테스트
│       │           │   │   ├── DeadlockSimulatorTest.java       # 데드락 발생 테스트
│       │           │   │   └── DeadlockDetectorTest.java        # 데드락 탐지 테스트
│       │           │   ├── pool/
│       │           │   │   ├── CustomThreadPoolTest.java        # 커스텀 풀 테스트
│       │           │   │   ├── ThreadPoolPerformanceTest.java   # 스레드 풀 성능 테스트
│       │           │   │   └── VirtualThreadExecutorTest.java   # Virtual Thread Executor 테스트
│       │           │   └── scheduling/
│       │           │       ├── FCFSSchedulerTest.java           # FCFS 알고리즘 테스트
│       │           │       ├── SJFSchedulerTest.java            # SJF 알고리즘 테스트
│       │           │       ├── RoundRobinSchedulerTest.java     # Round Robin 테스트
│       │           │       └── SchedulerComparisonTest.java     # 스케줄러 비교 테스트
│       │           └── comparison/
│       │               ├── performance/
│       │               │   ├── CreationBenchmarkTest.java       # 생성 벤치마크 검증
│       │               │   └── ScalabilityTestTest.java         # 확장성 테스트 검증
│       │               ├── resource/
│       │               │   └── ResourceAnalyzerTest.java        # 리소스 분석기 테스트
│       │               └── concurrency/
│       │                   └── RaceConditionDetectorTest.java   # 경쟁 조건 탐지 테스트
│       └── resources/
│           ├── test-application.properties                      # 테스트 설정
│           └── test-data/                                       # 테스트 데이터
│               ├── process-test-data.json
│               └── thread-test-data.json
├── docs/
│   ├── architecture/
│   │   ├── system-design.md                                     # 시스템 설계 문서
│   │   ├── class-diagrams.puml                                  # UML 클래스 다이어그램
│   │   └── sequence-diagrams.puml                               # UML 시퀀스 다이어그램
│   ├── experiments/
│   │   ├── experiment-01-process-isolation.md                   # 프로세스 격리 실험
│   │   ├── experiment-02-thread-sharing.md                      # 스레드 메모리 공유 실험
│   │   ├── experiment-03-virtual-thread-scale.md                # Virtual Thread 확장성 실험
│   │   ├── experiment-04-deadlock-scenarios.md                  # 데드락 시나리오 실험
│   │   └── experiment-05-performance-comparison.md              # 성능 비교 실험
│   └── results/
│       ├── benchmark-results.md                                 # 벤치마크 결과
│       ├── performance-analysis.md                              # 성능 분석 보고서
│       └── charts/                                              # 결과 차트
│           ├── creation-time-comparison.png
│           ├── memory-usage-comparison.png
│           └── throughput-analysis.png
├── scripts/
│   ├── run-process-demo.sh                                      # 프로세스 데모 실행
│   ├── run-thread-demo.sh                                       # 스레드 데모 실행
│   ├── run-virtual-thread-demo.sh                               # Virtual Thread 데모 실행
│   ├── run-all-benchmarks.sh                                    # 모든 벤치마크 실행
│   ├── generate-report.sh                                       # 보고서 생성
│   └── setup-environment.sh                                     # 환경 설정
├── logs/                                                         # 로그 디렉토리
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── LICENSE
```

## 주요 모듈 설명

### 1. Process 모듈
프로세스의 생성, 관리, 통신을 담당하는 모듈

#### 핵심 클래스
- `ProcessCreator`: ProcessBuilder를 사용한 프로세스 생성
- `ProcessCommunicator`: Socket, Pipe, SharedMemory를 통한 IPC
- `ProcessMonitor`: 프로세스 상태 및 리소스 모니터링
- `ProcessScheduler`: 프로세스 스케줄링 시뮬레이션

Process 모듈 파일 설명
creation/ 디렉토리

ProcessCreator.java: ProcessBuilder API를 활용한 기본 프로세스 생성 클래스
ProcessFactory.java: 다양한 유형의 프로세스를 생성하는 팩토리 패턴 구현
ChildProcess.java: 자식 프로세스로 실행될 독립적인 Java 애플리케이션
ProcessSpawner.java: 여러 프로세스를 동시에 생성하고 관리하는 클래스

communication/ 디렉토리

SocketIPC.java: TCP/UDP 소켓을 사용한 프로세스 간 통신 구현
PipeIPC.java: Named Pipe를 사용한 단방향/양방향 통신 구현
SharedMemoryIPC.java: MappedByteBuffer를 사용한 공유 메모리 통신
MessageQueue.java: 메시지 큐 패턴을 시뮬레이션한 IPC
IPCBenchmark.java: 각 IPC 방식의 성능을 측정하고 비교

monitoring/ 디렉토리

ProcessMonitor.java: 프로세스의 실행 상태, PID, 생존 여부 모니터링
ProcessResourceTracker.java: CPU, 메모리, I/O 사용량 실시간 추적
ProcessLifecycleTracker.java: 프로세스의 생성부터 종료까지 생명주기 추적
ProcessMetrics.java: 수집된 메트릭을 분석하고 리포트 생성

management/ 디렉토리

ProcessManager.java: 프로세스의 생성, 종료, 재시작 등 전체 생명주기 관리
ProcessPool.java: 재사용 가능한 프로세스 풀 구현
ProcessSchedulerSimulator.java: FCFS, SJF 등 프로세스 스케줄링 알고리즘 시뮬레이션
ProcessOrchestrator.java: 복잡한 프로세스 의존성 및 워크플로우 관리

### 2. Thread 모듈
스레드의 생명주기와 동기화를 다루는 모듈 (Platform Thread + Virtual Thread)

#### 핵심 클래스
- `ThreadLifecycle`: 스레드 상태 전이 실습
- `VirtualThreadDemo`: Java 21 Virtual Thread 실습
- `ThreadSynchronizer`: synchronized, Lock, Semaphore 구현
- `ThreadPoolManager`: ExecutorService 기반 스레드 풀
- `StructuredConcurrency`: Java 21 Structured Concurrency 실습
- `DeadlockSimulator`: 교착상태 재현 및 해결

Thread 모듈 파일 설명
lifecycle/ 디렉토리

ThreadStateDemo.java: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED 상태 전이 실습
ThreadLifecycleObserver.java: 스레드 상태 변화를 실시간으로 관찰하고 기록
PlatformThreadDemo.java: 전통적인 OS 스레드 생성 및 관리
VirtualThreadDemo.java: Java 21 Virtual Thread 생성 및 특성 실습
ThreadCreationComparison.java: 다양한 스레드 생성 방식의 성능 비교

synchronization/ 디렉토리

MutexDemo.java: synchronized 키워드와 Lock 인터페이스를 사용한 상호배제
SemaphoreDemo.java: 카운팅 세마포어를 사용한 동시 접근 제어
MonitorDemo.java: wait/notify를 사용한 모니터 패턴 구현
ReentrantLockDemo.java: 재진입 가능 락과 조건 변수 사용
ReadWriteLockDemo.java: 읽기/쓰기 락을 사용한 동시성 향상
StampedLockDemo.java: 낙관적 읽기를 지원하는 StampedLock 사용
ProducerConsumer.java: BlockingQueue를 사용한 생산자-소비자 패턴
DiningPhilosophers.java: 철학자들의 만찬 문제로 데드락 이해
DeadlockSimulator.java: 의도적인 데드락 생성 및 분석
DeadlockDetector.java: ThreadMXBean을 사용한 데드락 탐지 및 해결

pool/ 디렉토리

CustomThreadPool.java: BlockingQueue를 사용한 커스텀 스레드 풀 구현
FixedThreadPoolDemo.java: 고정 크기 스레드 풀의 특성 실습
CachedThreadPoolDemo.java: 동적으로 크기가 변하는 스레드 풀 실습
ScheduledThreadPoolDemo.java: 주기적 작업 실행을 위한 스케줄드 풀
WorkStealingPoolDemo.java: ForkJoinPool 기반 work-stealing 실습
VirtualThreadExecutor.java: Virtual Thread를 위한 커스텀 Executor
ThreadPoolMonitor.java: 스레드 풀의 상태와 성능 실시간 모니터링

### 3. Comparison 모듈
프로세스, Platform Thread, Virtual Thread의 성능 및 특성을 비교하는 모듈

#### 핵심 클래스
- `PerformanceBenchmark`: JMH를 활용한 성능 측정
- `VirtualThreadBenchmark`: Platform Thread vs Virtual Thread 비교
- `ResourceAnalyzer`: 메모리, CPU 사용량 분석
- `ContextSwitchMeasurer`: 컨텍스트 스위칭 오버헤드 측정

scheduling/ 디렉토리

FCFSScheduler.java: First-Come First-Served 스케줄링 구현
SJFScheduler.java: Shortest Job First 스케줄링 구현
RoundRobinScheduler.java: 타임 슬라이스 기반 라운드 로빈 구현
PriorityScheduler.java: 우선순위 기반 스케줄링 구현
MLFQScheduler.java: Multi-Level Feedback Queue 구현
SchedulerComparison.java: 각 스케줄링 알고리즘의 성능 비교

structured/ 디렉토리

StructuredTaskScope.java: Java 21 Structured Concurrency API 실습
ScopedValueDemo.java: 스레드 로컬의 대안인 Scoped Values 사용
StructuredPatterns.java: 구조화된 동시성 디자인 패턴

Comparison 모듈 파일 설명
performance/ 디렉토리

CreationBenchmark.java: 프로세스, Platform Thread, Virtual Thread 생성 시간 측정
ContextSwitchBenchmark.java: 컨텍스트 스위칭 오버헤드 측정
IOBenchmark.java: I/O 집약적 작업에서의 성능 비교
CPUBenchmark.java: CPU 집약적 작업에서의 성능 비교
ScalabilityTest.java: 부하 증가에 따른 확장성 테스트
ThroughputAnalyzer.java: 단위 시간당 처리량 분석

resource/ 디렉토리

MemoryUsageAnalyzer.java: 힙, 비힙, 네이티브 메모리 사용량 분석
CPUUsageAnalyzer.java: CPU 사용률 및 코어별 활용도 분석
ResourceProfiler.java: JFR을 사용한 상세 리소스 프로파일링
ResourceComparison.java: 리소스 사용량 종합 비교 분석

concurrency/ 디렉토리

ConcurrentTaskExecutor.java: 동시 작업 실행 성능 비교
ParallelismAnalyzer.java: 실제 병렬 실행 정도 분석
RaceConditionDetector.java: 경쟁 조건 발생 감지 및 분석
ConcurrencyPatterns.java: 다양한 동시성 패턴 성능 비교

Common 모듈 파일 설명
util/ 디렉토리

SystemInfo.java: OS, JVM, 하드웨어 정보 조회
TimeUtils.java: 나노초 단위 정밀 시간 측정
RandomDataGenerator.java: 테스트용 랜덤 데이터 생성
FileUtils.java: 파일 읽기/쓰기 유틸리티
ReportGenerator.java: HTML/Markdown 형식 보고서 생성

monitor/ 디렉토리

BaseMonitor.java: 모든 모니터의 기본 추상 클래스
MetricsCollector.java: 다양한 메트릭 수집 및 집계
PerformanceMonitor.java: 성능 지표 실시간 모니터링
JFRRecorder.java: Java Flight Recorder 래퍼 클래스
DashboardServer.java: 웹 기반 모니터링 대시보드 서버

## 실행 방법

### 사전 요구사항
- JDK 21 이상 (LTS)
- Gradle 8.5 이상 (또는 gradlew 사용)

### JDK 21 설정 확인
```bash
java --version
# openjdk 21 2023-09-19 LTS 출력 확인
```

### 빌드
```bash
./gradlew clean build
```

### 테스트 실행
```bash
./gradlew test
```

### 메인 애플리케이션 실행
```bash
./gradlew run
```

### 개별 데모 실행
```bash
# 프로세스 데모
./gradlew run --args="process"

# Platform Thread 데모
./gradlew run --args="platform-thread"

# Virtual Thread 데모
./gradlew run --args="virtual-thread"

# 비교 분석
./gradlew run --args="compare"
```

## 실습 시나리오

### 시나리오 1: 프로세스 격리 확인
여러 JVM 프로세스를 생성하고 각각의 메모리 공간이 격리되어 있음을 확인

### 시나리오 2: 스레드 공유 메모리
같은 프로세스 내 Platform Thread들이 메모리를 공유하는 것을 실습

### 시나리오 3: Virtual Thread 대규모 동시성
100만 개의 Virtual Thread 생성 및 실행 (Platform Thread로는 불가능한 규모)

### 시나리오 4: Structured Concurrency
Java 21의 구조화된 동시성을 활용한 작업 관리

### 시나리오 5: Producer-Consumer 문제
Platform Thread와 Virtual Thread 각각으로 구현 및 비교

### 시나리오 6: 스케줄링 알고리즘 시뮬레이션
FCFS, SJF, Round Robin 등 스케줄링 알고리즘 구현 및 비교

### 시나리오 7: 성능 벤치마크
- 프로세스 생성 vs Platform Thread 생성 vs Virtual Thread 생성
- I/O 집약적 작업에서의 성능 비교
- CPU 집약적 작업에서의 성능 비교

## 학습 산출물

### 측정 지표
- 프로세스/스레드 생성 시간
- 컨텍스트 스위칭 시간
- 메모리 사용량 (Heap, Non-Heap, Native)
- CPU 사용률
- 처리량 (Throughput)
- 응답 시간 (Response Time)

### 분석 보고서
각 실습 후 다음 내용을 포함한 보고서 작성:
- 실험 목적 및 가설
- 실험 방법 및 환경
- 측정 결과 및 그래프
- 결과 분석 및 인사이트
- 이론과 실제의 차이점

## Java 21 주요 기능 활용

### Virtual Threads (JEP 444)
- 경량 스레드로 수백만 개 생성 가능
- I/O 집약적 작업에서 뛰어난 성능
- Platform Thread와의 성능 비교 실습

### Structured Concurrency (Preview - JEP 453)
- 구조화된 방식으로 동시 작업 관리
- 작업 그룹의 생명주기 관리
- 에러 전파 및 취소 메커니즘

### Pattern Matching 활용
- switch 표현식과 패턴 매칭
- Record 패턴을 활용한 데이터 처리
- 더 간결한 코드 작성

### Sequenced Collections (JEP 431)
- 순서가 있는 컬렉션 인터페이스
- 스레드 안전 컬렉션 활용

## 확장 가능성

### 추가 구현 가능 기능
1. Virtual Thread와 Platform Thread 혼합 사용 패턴
2. Foreign Function & Memory API (Preview) 활용
3. Vector API를 활용한 병렬 처리
4. 분산 시스템 시뮬레이션
5. 실시간 모니터링 대시보드 (JFR 활용)
6. 다양한 IPC 메커니즘 구현
7. 커스텀 스케줄러 구현

### 연계 학습
- 운영체제 이론
- JVM 내부 구조
- 동시성 프로그래밍
- 시스템 프로그래밍

## 참고 자료

### 도서
- "Operating System Concepts" - Silberschatz, Galvin, Gagne
- "Java Concurrency in Practice" - Brian Goetz
- "Modern Operating Systems" - Andrew S. Tanenbaum

### 온라인 리소스
- [OpenJDK Documentation](https://openjdk.org/docs/)
- [Java Concurrency Tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/)
- [JVM Specification](https://docs.oracle.com/javase/specs/jvms/se17/html/)

## 라이센스

MIT License

## 기여 가이드

프로젝트 개선을 위한 기여를 환영합니다. 다음 절차를 따라주세요:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 문의사항

프로젝트 관련 문의사항이 있으시면 Issues를 통해 남겨주세요.~~