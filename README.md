# process_thread

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
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── study/
│   │   │           ├── process/
│   │   │           │   ├── creation/          # 프로세스 생성 실습
│   │   │           │   ├── communication/     # IPC 구현
│   │   │           │   ├── monitoring/        # 프로세스 모니터링
│   │   │           │   └── management/        # 프로세스 관리
│   │   │           ├── thread/
│   │   │           │   ├── lifecycle/         # 스레드 생명주기
│   │   │           │   ├── synchronization/   # 동기화 메커니즘
│   │   │           │   ├── pool/              # 스레드 풀 구현
│   │   │           │   └── scheduling/        # 스케줄링 시뮬레이션
│   │   │           ├── comparison/
│   │   │           │   ├── performance/       # 성능 비교
│   │   │           │   ├── resource/          # 리소스 사용량 비교
│   │   │           │   └── concurrency/       # 동시성 분석
│   │   │           ├── common/
│   │   │           │   ├── util/              # 유틸리티
│   │   │           │   └── monitor/           # 공통 모니터링
│   │   │           └── Main.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback.xml
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── study/
│       │           ├── process/
│       │           ├── thread/
│       │           └── comparison/
│       └── resources/
├── docs/
│   ├── architecture/
│   ├── experiments/
│   └── results/
├── scripts/
│   ├── run-process-demo.sh
│   └── run-thread-demo.sh
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── README.md
```

## 주요 모듈 설명

### 1. Process 모듈
프로세스의 생성, 관리, 통신을 담당하는 모듈

#### 핵심 클래스
- `ProcessCreator`: ProcessBuilder를 사용한 프로세스 생성
- `ProcessCommunicator`: Socket, Pipe, SharedMemory를 통한 IPC
- `ProcessMonitor`: 프로세스 상태 및 리소스 모니터링
- `ProcessScheduler`: 프로세스 스케줄링 시뮬레이션

### 2. Thread 모듈
스레드의 생명주기와 동기화를 다루는 모듈 (Platform Thread + Virtual Thread)

#### 핵심 클래스
- `ThreadLifecycle`: 스레드 상태 전이 실습
- `VirtualThreadDemo`: Java 21 Virtual Thread 실습
- `ThreadSynchronizer`: synchronized, Lock, Semaphore 구현
- `ThreadPoolManager`: ExecutorService 기반 스레드 풀
- `StructuredConcurrency`: Java 21 Structured Concurrency 실습
- `DeadlockSimulator`: 교착상태 재현 및 해결

### 3. Comparison 모듈
프로세스, Platform Thread, Virtual Thread의 성능 및 특성을 비교하는 모듈

#### 핵심 클래스
- `PerformanceBenchmark`: JMH를 활용한 성능 측정
- `VirtualThreadBenchmark`: Platform Thread vs Virtual Thread 비교
- `ResourceAnalyzer`: 메모리, CPU 사용량 분석
- `ContextSwitchMeasurer`: 컨텍스트 스위칭 오버헤드 측정

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

프로젝트 관련 문의사항이 있으시면 Issues를 통해 남겨주세요.