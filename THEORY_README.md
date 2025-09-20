# 프로세스와 스레드 종합 가이드

## 목차
1. [프로세스와 스레드의 정의 및 어원](#1-프로세스와-스레드의-정의-및-어원)
2. [프로세스와 스레드의 동작 방식](#2-프로세스와-스레드의-동작-방식)
3. [다양한 관점에서 바라본 프로세스와 스레드](#3-다양한-관점에서-바라본-프로세스와-스레드)
4. [컴파일 언어와 인터프리터 언어에서의 프로세스와 스레드](#4-컴파일-언어와-인터프리터-언어에서의-프로세스와-스레드)
5. [Java(Spring)과 Python(FastAPI)에서의 프로세스와 스레드](#5-javaspring과-pythonfastapi에서의-프로세스와-스레드)

---

## 1. 프로세스의 이론적 기초

### 1.1 프로세스의 개념과 정의

#### 고전적 프로세스 개념
프로세스는 컴퓨터상에서 실행 중인 프로그램의 개념을 나타내며, 운영체제에서 작업(Task)이라고도 불립니다. 다중 프로그래밍(Multiprogramming), 동기화(Synchronization), 교착상태(Deadlock) 등의 운영체제 개념들은 모두 고전적 프로세스 모델 하에서 개발되었습니다.

#### 프로세스의 다양한 정의
1. **PCB 관점**: 현재 실행중이거나 곧 실행할 수 있는 PCB(Process Control Block)을 가진 프로그램
2. **실행 관점**: 실행중인 프로시저(procedure) 또는 프로그램
3. **비동기성 관점**: 비동기적인 행위로, 어떤 명령의 동작이 끝났음을 가리키는 신호로 다음 명령의 수행을 시작
4. **디스패치 관점**: 프로세서에 할당되어 실행되는 개체(entity)로 디스패치 가능한 대상
5. **시스템 관점**: CPU가 현재 수행하고 있는 사용자 프로그램과 시스템 프로그램으로, 시스템의 작업 단위

#### 프로세스의 구성 요소
프로세스는 단순한 프로그램 코드 이상의 것으로, 다음을 포함합니다:
- **프로세스 스택**: 서브루틴 매개변수, 복귀 주소, 임시 변수 등의 임시적 자료
- **데이터 섹션**: 전역 변수들
- **힙(Heap)**: 동적으로 할당되는 메모리
- **프로그램 카운터(PC)**: 다음 실행할 명령어 위치
- **레지스터 집합**: CPU 레지스터들의 현재 값

### 1.2 프로세스의 문맥(Context)

프로세스의 문맥은 프로그램 실행으로 얻어진 모든 내용을 포함합니다:

#### 1. 프로세서 문맥 (Processor Context)
- 상태 워드 (PSW: Program Status Word)
- 범용 레지스터
- 프로그램 카운터
- 스택 포인터

#### 2. 기억장치 문맥 (Memory Context)
- **코드 세그먼트**: 실행 가능한 명령어들
- **데이터 세그먼트**: 전역 변수와 정적 변수
- **실행 스택**: 지역 변수와 함수 호출 정보

#### 3. 프로세스 속성들
- **프로세스 이름(PID)**: 실행 시 할당된 내부적 번호
- **프로세스 우선순위**: CPU 할당 스케줄링에 사용
- **프로세스 권한**: 정보 보호 및 수행 가능한 연산 지정

### 1.3 어원 (Etymology)

#### 프로세스 (Process)
- **어원**: 라틴어 "processus"에서 유래 (전진, 진행을 의미)
- **컴퓨터 과학 도입**: 1960년대 초반 시분할 시스템과 함께 도입
- **의미 확장**: 단순한 프로그램 실행에서 자원 할당과 관리 단위로 발전

#### 스레드 (Thread)
- **어원**: "실타래" 또는 "일련의 연결된 것"을 의미
- **컴퓨터 과학 도입**: 1967년 IBM OS/360에서 "tasks"라는 이름으로 첫 등장
- **용어 확립**: Victor A. Vyssotsky가 1966년 "thread"라는 용어 최초 사용
- **비유적 의미**: 대화나 사고의 연속된 흐름처럼, 실행의 연속된 흐름을 나타냄

**출처**:
- [Thread (computing) - Wikipedia](https://en.wikipedia.org/wiki/Thread_(computing))
- [Why are threads called threads? - Software Engineering Stack Exchange](https://softwareengineering.stackexchange.com/questions/225371/why-are-threads-called-threads)
- Saltzer (1966) - "Traffic Control in a Multiplexed Computer System"

### 1.4 프로세스의 종류

#### 1. 순차 프로세스 (Sequential Process)
- 프로세스는 CPU 시간, 기억장치, 파일, 입출력장치 등 여러 자원을 필요로 함
- 자원들은 생성 시 또는 실행 중에 할당
- 프로그램 자체는 수동적 단위, 프로세스는 능동적 단위
- 한 시점에 하나의 명령어만 실행되는 순차적 방식

#### 2. 독립 프로세스와 협동 프로세스
**독립 프로세스 (Independent Process)**:
- 다른 프로세스에 영향을 주지도 받지도 않음
- 데이터를 공유하지 않음
- 결정적(deterministic) 실행

**협동 프로세스 (Cooperating Process)**:
- 다른 프로세스와 영향을 주고받음
- 데이터와 자원 공유
- 프로세스 간 통신(IPC)과 동기화 필요

#### 3. 시스템 프로세스와 사용자 프로세스
**시스템 프로세스 (System Process)**:
- 프로세스 실행 순서 제어
- 메모리 영역 침범 감시
- 시스템 운영에 필요한 작업 수행
- 커널 프로세스 또는 운영체제 프로세스라고도 함

**사용자 프로세스 (User Process)**:
- 사용자 프로그램을 수행하는 프로세스
- 시분할 프로그램, 일괄처리 작업 등

### 1.5 컴퓨터 시스템에서의 정의

#### 프로세스 (Process)
**정의**: 실행 중인 프로그램의 인스턴스로, 운영체제로부터 자원을 할당받는 작업의 단위

**주요 특징**:
- 독립적인 메모리 공간 보유 (Code, Data, Stack, Heap)
- 프로세스 제어 블록(PCB)으로 관리
- 프로세스 간 통신(IPC)을 통해 데이터 교환
- 운영체제의 보호를 받는 독립적인 실행 단위

#### 스레드 (Thread)
**정의**: 프로세스 내에서 실제로 작업을 수행하는 주체이자, 운영체제 스케줄러가 독립적으로 관리할 수 있는 가장 작은 프로그램 실행 단위

**주요 특징**:
- 프로세스의 자원을 공유 (Code, Data, Heap)
- 독립적인 스택과 레지스터 세트 보유
- 경량 프로세스(Lightweight Process)라고도 불림
- 같은 프로세스 내 스레드들은 메모리 공유로 빠른 통신 가능

**출처**:
- [Tanenbaum - Modern Operating Systems (PDF)](https://csc-knu.github.io/sys-prog/books/Andrew%20S.%20Tanenbaum%20-%20Modern%20Operating%20Systems.pdf)
- [Silberschatz - Operating System Concepts (PDF)](https://drive.uqu.edu.sa/_/mskhayat/files/MySubjects/2017SS%20Operating%20Systems/Abraham%20Silberschatz-Operating%20System%20Concepts%20(9th,2012_12).pdf)
- [Difference between Process and Thread - GeeksforGeeks](https://www.geeksforgeeks.org/operating-systems/difference-between-process-and-thread/)

### 1.6 하드웨어 레벨에서의 프로세스와 스레드

#### CPU와 RAM 관점

**CPU 처리**:
- **컨텍스트 스위칭**: CPU는 여러 프로세스/스레드를 번갈아가며 실행
- **레지스터 상태**: 각 스레드는 고유한 레지스터 값(PC, SP, 범용 레지스터) 보유
- **캐시 영향**: 프로세스 전환 시 캐시 무효화로 성능 저하 발생

**RAM 관점**:
- **프로세스**: 독립적인 가상 주소 공간 (보통 4GB 이상)
- **스레드**: 프로세스의 주소 공간 공유, 독립적인 스택 영역만 할당 (보통 1-8MB)
- **페이지 테이블**: 프로세스별로 독립적, 스레드는 공유

#### 트랜지스터 레벨

**실행 메커니즘**:
1. **명령어 페치**: 트랜지스터가 메모리에서 명령어를 읽음
2. **디코딩**: 명령어를 트랜지스터가 실행할 수 있는 신호로 변환
3. **실행**: ALU의 트랜지스터들이 실제 연산 수행
4. **상태 저장**: 레지스터(플립플롭)에 중간 결과 저장

**컨텍스트 스위칭 시**:
- 수천~수만 개의 트랜지스터가 현재 상태를 메모리에 저장
- 새로운 프로세스/스레드의 상태를 레지스터로 로드
- 일반적으로 1-100 마이크로초 소요

**출처**:
- [Linux Context Switching Internals: Part 1 - Process State and Memory](https://blog.codingconfessions.com/p/linux-context-switching-internals)
- [Context switch - Wikipedia](https://en.wikipedia.org/wiki/Context_switch)
- [Measuring context switching and memory overheads for Linux threads - Eli Bendersky](https://eli.thegreenplace.net/2018/measuring-context-switching-and-memory-overheads-for-linux-threads/)

---

## 2. 프로세스 상태와 전이

### 2.1 프로세스 상태 (Process States)

프로세스는 시스템 내에 존재하는 동안 여러 사건들로 인해 일련의 구분되는 상태 변화를 거칩니다.

#### 기본 5-상태 모델
1. **New (생성) 상태**
    - 프로세스가 생성 중인 상태
    - PCB가 만들어지고 메모리 할당 대기
    - 아직 메인 메모리에 적재되지 않음

2. **Ready (준비) 상태**
    - CPU를 할당받을 준비가 완료된 상태
    - 준비 큐(Ready Queue)에서 대기
    - CPU 스케줄링 정책에 따라 선택 대기

3. **Running (실행) 상태**
    - CPU를 차지하고 명령어를 실행 중인 상태
    - 단일 프로세서 시스템에서는 오직 하나의 프로세스만 이 상태
    - 타임 슬라이스 만료 또는 인터럽트로 선점 가능

4. **Waiting/Blocked (대기) 상태**
    - I/O 작업이나 이벤트 대기 중인 상태
    - CPU를 양도하고 대기 큐에서 대기
    - I/O 완료 또는 이벤트 발생 시 Ready 상태로 전이

5. **Terminated (종료) 상태**
    - 프로세스 실행이 완료된 상태
    - PCB와 할당된 자원들이 해제됨

#### 확장된 7-상태 모델
6. **Suspend Ready (보류 준비) 상태**
    - Ready 상태의 프로세스가 메모리 부족으로 디스크로 스왑된 상태
    - 메모리 확보 시 다시 Ready 상태로 전이

7. **Suspend Wait/Blocked (보류 대기) 상태**
    - Waiting 상태의 프로세스가 디스크로 스왑된 상태
    - I/O 완료 시 Suspend Ready 상태로 전이

### 2.2 프로세스 상태 전이 (State Transitions)

```
New → Ready: 프로세스 생성 완료 (Admit)
Ready → Running: CPU 할당 (Dispatch)
Running → Ready: 타임 슬라이스 만료 (Timeout/Preempt)
Running → Waiting: I/O 요청 또는 이벤트 대기 (I/O or Event Wait)
Waiting → Ready: I/O 완료 또는 이벤트 발생 (I/O or Event Completion)
Running → Terminated: 실행 완료 (Exit)
Ready → Suspend Ready: 메모리 부족 (Suspend)
Waiting → Suspend Wait: 메모리 부족 (Suspend)
Suspend Wait → Suspend Ready: I/O 완료 (I/O Completion)
Suspend Ready → Ready: 메모리 확보 (Resume)
```

### 2.3 문맥 교환 (Context Switching)

#### 문맥 교환의 정의
문맥 교환은 CPU가 한 프로세스에서 다른 프로세스로 전환할 때 발생하는 작업입니다.

#### 문맥 교환 과정
1. **현재 프로세스 상태 저장**
    - CPU 레지스터 값을 PCB에 저장
    - 프로그램 카운터, 스택 포인터 저장
    - 메모리 관리 정보 저장

2. **다음 프로세스 상태 복원**
    - 선택된 프로세스의 PCB에서 정보 로드
    - 레지스터 값 복원
    - 메모리 맵 설정

3. **실행 재개**
    - 새 프로세스의 프로그램 카운터부터 실행

#### 문맥 교환 오버헤드
- **시간 비용**: 일반적으로 1-100 마이크로초
- **캐시 무효화**: TLB(Translation Lookaside Buffer) 플러시
- **메모리 접근 증가**: 새로운 작업 집합 로드

### 2.4 디스패치와 스케줄링 개념

#### 디스패치 (Dispatch)
- Ready 상태의 프로세스를 선택하여 Running 상태로 전환
- 디스패처(Dispatcher)가 수행
- 문맥 교환 포함

#### 타임 슬라이스 (Time Slice/Quantum)
- 프로세스가 CPU를 사용할 수 있는 최대 시간
- 일반적으로 10-100ms
- Round Robin 스케줄링의 핵심 개념

#### 할당 시간량 (Time Quantum)
- 타임 슬라이스와 동일한 개념
- 너무 크면: FCFS와 유사해짐
- 너무 작으면: 문맥 교환 오버헤드 증가

**출처**:
- [States of a Process in Operating Systems - GeeksforGeeks](https://www.geeksforgeeks.org/operating-systems/states-of-a-process-in-operating-systems/)
- [Process State in Operating System - Byjus](https://byjus.com/gate/process-state-in-operating-system-notes/)
- [Process State in OS - Scaler Topics](https://www.scaler.com/topics/operating-system/process-state-in-os/)
- [OS Process States - Javatpoint](https://www.javatpoint.com/os-process-states)

---

## 3. 프로세스 제어 블록 (PCB)

### 3.1 PCB의 정의와 역할

**Process Control Block (PCB)**는 특정 프로세스에 대한 모든 정보를 담고 있는 운영체제 커널의 자료구조입니다. 프로세스 디스크립터(Process Descriptor)라고도 불리며, 프로세스의 "신분증" 역할을 합니다.

### 3.2 PCB의 구성 요소

#### 1. 프로세스 식별 정보
- **프로세스 ID (PID)**: 고유 식별 번호
- **부모 프로세스 ID (PPID)**
- **사용자 ID (UID)**
- **그룹 ID (GID)**

#### 2. 프로세스 상태 정보
- **현재 상태**: New, Ready, Running, Waiting, Terminated
- **프로세스 우선순위**
- **스케줄링 정보**: 큐 포인터, 스케줄링 매개변수

#### 3. CPU 레지스터 정보
- **프로그램 카운터 (PC)**
- **스택 포인터 (SP)**
- **범용 레지스터들**
- **상태 레지스터 (PSW)**

#### 4. 메모리 관리 정보
- **베이스 레지스터, 한계 레지스터**
- **페이지 테이블 또는 세그먼트 테이블**
- **메모리 할당 정보**

#### 5. I/O 상태 정보
- **열린 파일 목록**
- **할당된 I/O 장치**
- **대기 중인 I/O 작업**

#### 6. 계정 정보
- **CPU 사용 시간**
- **실행 시간 제한**
- **프로세스 번호**

### 3.3 PCB 관리

#### PCB 테이블 (Process Table)
- 모든 활성 프로세스의 PCB를 포함하는 배열 또는 연결 리스트
- 운영체제가 프로세스를 효율적으로 관리
- 프로세스 생성 시 PCB 할당, 종료 시 해제

#### PCB의 동적 관리
```
프로세스 생성:
1. PCB 메모리 할당
2. PID 할당
3. 초기 상태 설정 (New)
4. 프로세스 테이블에 추가

프로세스 종료:
1. 자원 해제
2. 부모 프로세스에 종료 통지
3. PCB 제거
4. 프로세스 테이블에서 삭제
```

### 3.4 PCB와 문맥 교환

문맥 교환 시 PCB의 역할:
1. **저장 단계**: 현재 실행 중인 프로세스의 상태를 PCB에 저장
2. **선택 단계**: 스케줄러가 다음 실행할 프로세스의 PCB 선택
3. **복원 단계**: 선택된 PCB에서 프로세스 상태 복원
4. **실행 단계**: 복원된 상태에서 프로세스 실행 재개

**출처**:
- [Process Table and Process Control Block (PCB) - GeeksforGeeks](https://www.geeksforgeeks.org/operating-systems/process-table-and-process-control-block-pcb/)
- [Process Control Block in Operating Systems - SlideShare](https://www.slideshare.net/slideshow/process-process-states-process-control-block-in-operating-systems/264660384)

---

## 4. 프로세스 스케줄링

### 4.1 스케줄링의 목적과 기준

#### 스케줄링의 목적
1. **공정한 스케줄링**: 모든 프로세스에 공평한 CPU 시간 할당
2. **처리량 극대화**: 단위 시간당 완료되는 프로세스 수 최대화
3. **응답 시간 최소화**: 사용자 요청에 대한 빠른 응답
4. **반환 시간 예측 가능**: 일관된 성능 제공
5. **균형있는 자원 사용**: CPU, 메모리, I/O 균형 활용
6. **무한 연기 배제**: 기아 상태(Starvation) 방지
7. **우선순위 실시**: 중요한 작업 우선 처리
8. **오버헤드 최소화**: 스케줄링 자체 비용 감소

#### 스케줄링 평가 기준
- **CPU 이용률 (CPU Utilization)**: 40-90% 유지 목표
- **처리량 (Throughput)**: 단위 시간당 완료 프로세스 수
- **반환 시간 (Turnaround Time)**: 제출부터 완료까지 시간
- **대기 시간 (Waiting Time)**: 준비 큐에서 대기한 시간
- **응답 시간 (Response Time)**: 첫 응답까지의 시간

### 4.2 스케줄링 분류

#### 1. 기능별 분류
**작업 스케줄링 (Job Scheduling)**:
- 장기 스케줄링 (Long-term Scheduling)
- 어떤 작업을 메모리에 적재할지 결정
- 다중 프로그래밍 정도 제어

**프로세스 스케줄링 (Process Scheduling)**:
- 단기 스케줄링 (Short-term Scheduling)
- Ready 큐의 프로세스 중 CPU 할당 대상 선택
- 매우 자주 발생 (밀리초 단위)

#### 2. 방법별 분류
**선점 스케줄링 (Preemptive)**:
- 실행 중인 프로세스를 중단시킬 수 있음
- 응답성 향상, 공정성 보장
- 문맥 교환 오버헤드 증가

**비선점 스케줄링 (Non-preemptive)**:
- 프로세스가 자발적으로 CPU 반납할 때까지 대기
- 구현 간단, 오버헤드 적음
- 응답성 저하 가능

### 4.3 스케줄링 알고리즘

#### 1. FCFS (First-Come First-Served)
```
특징:
- 비선점 스케줄링
- FIFO 큐 구조
- 구현 간단, 공정함

장점:
- 이해와 구현 용이
- 기아 상태 없음

단점:
- 평균 대기 시간 길어짐
- Convoy Effect 발생 가능
```

#### 2. SJF (Shortest Job First)
```
특징:
- 최단 작업 우선
- 비선점/선점 가능

장점:
- 평균 대기 시간 최소
- 처리량 최대화

단점:
- 긴 작업 기아 가능
- 실행 시간 예측 어려움
```

#### 3. SRTF (Shortest Remaining Time First)
```
특징:
- SJF의 선점형 버전
- 남은 시간이 가장 짧은 프로세스 선택

장점:
- 평균 대기 시간 최소화

단점:
- 잦은 문맥 교환
- 긴 프로세스 기아 상태
```

#### 4. Priority Scheduling (우선순위 스케줄링)
```
특징:
- 우선순위 기반 선택
- 선점/비선점 가능

장점:
- 중요 작업 우선 처리
- 유연한 스케줄링

단점:
- 기아 상태 가능
- 우선순위 역전 문제

해결책:
- Aging 기법: 대기 시간에 따라 우선순위 증가
```

#### 5. Round Robin (RR)
```
특징:
- 시분할 시스템용
- 타임 슬라이스(Quantum) 사용
- 선점형 스케줄링

장점:
- 응답 시간 보장
- 공정한 CPU 할당

단점:
- 문맥 교환 오버헤드
- 타임 슬라이스 선택 중요

최적 타임 슬라이스:
- 80% 작업이 타임 슬라이스 내 완료
- 일반적으로 10-100ms
```

#### 6. HRN (Highest Response Ratio Next)
```
응답률 = (대기 시간 + 서비스 시간) / 서비스 시간

특징:
- SJF와 FCFS 절충
- Aging 효과 내재

장점:
- 기아 상태 방지
- 균형잡힌 스케줄링
```

#### 7. MLQ (Multi-Level Queue)
```
특징:
- 프로세스를 여러 큐로 분류
- 각 큐별 다른 알고리즘 적용

구조:
- System Processes (최고 우선순위)
- Interactive Processes
- Batch Processes (최저 우선순위)

큐 간 스케줄링:
- 고정 우선순위
- 시간 할당 (80% 전경, 20% 배경)
```

#### 8. MLFQ (Multi-Level Feedback Queue)
```
특징:
- 프로세스가 큐 간 이동 가능
- 동적 우선순위 조정

규칙:
1. 새 프로세스는 최상위 큐
2. 타임 슬라이스 내 완료 시 유지
3. 타임 슬라이스 소진 시 강등
4. I/O 작업 후 승격

장점:
- I/O 바운드와 CPU 바운드 자동 구분
- 적응적 스케줄링
```

### 4.4 스케줄링 알고리즘 비교

| 알고리즘 | 선점 | 평균 대기 시간 | 처리량 | 응답 시간 | 오버헤드 | 기아 |
|---------|------|---------------|--------|-----------|----------|------|
| FCFS | X | 높음 | 낮음 | 높음 | 낮음 | 없음 |
| SJF | X | 최소 | 높음 | 중간 | 낮음 | 가능 |
| SRTF | O | 최소 | 높음 | 낮음 | 높음 | 가능 |
| Priority | O/X | 중간 | 중간 | 중간 | 중간 | 가능 |
| RR | O | 중간 | 중간 | 낮음 | 높음 | 없음 |
| MLFQ | O | 낮음 | 높음 | 낮음 | 중간 | 최소 |

**출처**:
- [Operating System Scheduling Algorithms - TutorialsPoint](https://www.tutorialspoint.com/operating_system/os_process_scheduling_algorithms.htm)
- [CPU Scheduling in Operating Systems - GeeksforGeeks](https://www.geeksforgeeks.org/operating-systems/cpu-scheduling-in-operating-systems/)
- [CPU Scheduling Algorithms - CS UIC](https://www.cs.uic.edu/~jbell/CourseNotes/OperatingSystems/5_CPU_Scheduling.html)
- [OS Scheduling Algorithms - Unstop](https://unstop.com/blog/scheduling-algorithms-in-operating-system)
- [Scheduling Algorithm PDF - VBS Purvanchal University](https://www.vbspu.ac.in/e-content/Scheduling-Algorithm.pdf)

---

## 5. 프로세스와 스레드의 동작 방식

#### 프로세스 생성과 실행
```
1. 프로그램 로드
   ├─> 실행 파일을 메모리에 적재
   ├─> 코드, 데이터, 힙, 스택 영역 할당
   └─> PCB(Process Control Block) 생성

2. 프로세스 초기화
   ├─> PID(Process ID) 할당
   ├─> 우선순위 설정
   └─> 자원 할당 (파일 디스크립터, 메모리 등)

3. 실행
   ├─> Ready Queue에 추가
   ├─> 스케줄러에 의해 CPU 할당
   └─> Running 상태로 전환

4. 종료
   ├─> Exit 시스템 콜 호출
   ├─> 자원 반환
   └─> PCB 제거
```

#### 프로세스 상태 전이
- **New**: 프로세스 생성 중
- **Ready**: CPU 할당 대기
- **Running**: 명령어 실행 중
- **Waiting/Blocked**: I/O 또는 이벤트 대기
- **Terminated**: 실행 완료

#### 프로세스 간 통신 (IPC)
- **파이프**: 단방향 통신
- **메시지 큐**: 비동기 메시지 전달
- **공유 메모리**: 빠른 데이터 공유
- **소켓**: 네트워크 통신

### 2.2 스레드의 동작 방식

#### 스레드 생성과 실행
```
1. 스레드 생성
   ├─> 부모 프로세스의 자원 공유
   ├─> 독립적인 스택 할당
   └─> 스레드 ID(TID) 할당

2. 스레드 스케줄링
   ├─> 사용자 수준 스레드: 라이브러리가 스케줄링
   └─> 커널 수준 스레드: OS가 스케줄링

3. 실행
   ├─> CPU 코어에 할당
   ├─> 타임 슬라이스 동안 실행
   └─> 선점 또는 양보로 전환

4. 종료
   ├─> 스택 메모리 해제
   └─> TID 반환
```

#### 스레드 동기화 메커니즘
- **뮤텍스(Mutex)**: 상호 배제
- **세마포어(Semaphore)**: 카운팅 기반 동기화
- **조건 변수**: 특정 조건 대기
- **배리어**: 모든 스레드 동기화 지점

**출처**:
- [Operating System Concepts (Silberschatz) - PDF Chapter 3, 4](https://drive.uqu.edu.sa/_/mskhayat/files/MySubjects/2017SS%20Operating%20Systems/Abraham%20Silberschatz-Operating%20System%20Concepts%20(9th,2012_12).pdf)
- [Context Switching in Operating System - GeeksforGeeks](https://www.geeksforgeeks.org/operating-systems/context-switch-in-operating-system/)
- [Thread in Operating System - GeeksforGeeks](https://www.geeksforgeeks.org/operating-systems/thread-in-operating-system/)
- [Processes and Threads - Computer Science Wiki](https://computersciencewiki.org/index.php/Processes_and_Threads)

---

## 3. 다양한 관점에서 바라본 프로세스와 스레드

### 3.1 운영체제 관점

**리소스 관리**:
- 프로세스: 독립적인 자원 할당 단위
- 스레드: 프로세스 내 실행 단위

**스케줄링**:
- 프로세스 스케줄링: 공정성과 처리량 중심
- 스레드 스케줄링: 응답성과 동시성 중심

**보호와 격리**:
- 프로세스: 완전한 메모리 보호
- 스레드: 프로세스 내에서만 보호

### 3.2 커널 관점

**커널 수준 스레드 (KLT)**:
- 커널이 직접 관리
- 시스템 콜로 생성/관리
- 멀티프로세서 활용 가능
- 컨텍스트 스위칭 오버헤드 큼

**사용자 수준 스레드 (ULT)**:
- 사용자 라이브러리가 관리
- 빠른 컨텍스트 스위칭
- 커널은 단일 프로세스로 인식
- 블로킹 시스템 콜 시 전체 프로세스 블록

**하이브리드 모델 (M:N)**:
- M개의 사용자 스레드를 N개의 커널 스레드에 매핑
- 유연성과 성능의 균형

**출처**:
- [Context switch internals - Stack Overflow](https://stackoverflow.com/questions/12630214/context-switch-internals)
- [Thread context switch Vs. process context switch - Stack Overflow](https://stackoverflow.com/questions/5440128/thread-context-switch-vs-process-context-switch)
- [Kernel Context Switching Between Processes - TutorialsPoint](https://www.tutorialspoint.com/how-can-kernels-context-switch-between-processes)

### 3.3 JVM 관점

#### JVM의 스레드 모델

**플랫폼 스레드 (전통적 모델)**:
- OS 스레드와 1:1 매핑
- 생성 비용이 높음 (약 1MB 스택)
- 수천 개가 실용적 한계

**가상 스레드 (Java 19+)**:
```java
// 플랫폼 스레드
Thread platformThread = new Thread(() -> {
    // 작업 수행
});

// 가상 스레드
Thread virtualThread = Thread.ofVirtual().start(() -> {
    // 작업 수행
});
```

**특징**:
- 경량 (수 KB 메모리)
- 수백만 개 생성 가능
- 캐리어 스레드에 마운트/언마운트
- I/O 블로킹 시 자동 양보

#### JVM 내부 스레드
- **Main Thread**: 애플리케이션 진입점
- **VM Thread**: 안전점(safepoint) 작업 수행
- **GC Threads**: 가비지 컬렉션
- **Compiler Threads**: JIT 컴파일
- **Signal Dispatcher**: OS 신호 처리

**메모리 공유**:
- **Heap**: 모든 스레드가 공유
- **Method Area**: 클래스 메타데이터 공유
- **Thread Stack**: 각 스레드별 독립적
- **PC Register**: 스레드별 현재 실행 위치

**출처**:
- [The Ultimate Guide to Java Virtual Threads - Rock the JVM](https://rockthejvm.com/articles/the-ultimate-guide-to-java-virtual-threads)
- [Virtual Threads - Oracle Documentation](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [Java Virtual Threads — Easy introduction - Medium](https://medium.com/@RamLakshmanan/java-virtual-threads-easy-introduction-44d96b8270f8)
- [A Deep Dive into JVM Memory Architecture and Thread Interactions - Medium](https://ondrej-kvasnovsky.medium.com/a-deep-dive-into-jvm-memory-architecture-and-thread-interactions-bbab7fa87051)
- [Overview of JVM Threads: Understanding Multithreading in Java](https://blog.ycrash.io/overview-jvm-threads-multithreading-java/)
- [How Many Threads Can a Java VM Support? - Baeldung](https://www.baeldung.com/jvm-max-threads)

---

## 4. 컴파일 언어와 인터프리터 언어에서의 프로세스와 스레드

### 4.1 컴파일 언어 (C/C++, Go, Rust)

**특징**:
- OS 스레드 직접 제어
- 하드웨어에 가까운 추상화
- 세밀한 메모리 관리

**스레드 모델**:
```c
// C - POSIX Threads
pthread_t thread;
pthread_create(&thread, NULL, worker_function, NULL);

// C++ - std::thread
std::thread t([]() {
    // 작업 수행
});

// Go - Goroutines (M:N 모델)
go func() {
    // 경량 스레드
}()

// Rust - 안전한 동시성
let handle = std::thread::spawn(|| {
    // 소유권 시스템으로 데이터 레이스 방지
});
```

**장점**:
- 예측 가능한 성능
- 실시간 시스템 가능
- 효율적인 자원 사용

**단점**:
- 복잡한 메모리 관리
- 데이터 레이스 위험
- 플랫폼 의존적

### 4.2 인터프리터 언어 (Python, Ruby, JavaScript)

**Python의 GIL (Global Interpreter Lock)**:
```python
# CPU 바운드 작업 - GIL로 인해 진정한 병렬 처리 불가
import threading

def cpu_bound():
    total = 0
    for i in range(100_000_000):
        total += i

# 스레드 생성해도 순차 실행과 비슷한 성능
threads = [threading.Thread(target=cpu_bound) for _ in range(4)]
```

**해결 방법**:
```python
# 1. 멀티프로세싱 사용
from multiprocessing import Process

processes = [Process(target=cpu_bound) for _ in range(4)]

# 2. 비동기 I/O (I/O 바운드 작업에 적합)
import asyncio

async def io_bound():
    await asyncio.sleep(1)
    # I/O 작업

# 3. 네이티브 확장 (NumPy, Pandas)
# C 확장은 GIL을 해제할 수 있음
```

**JavaScript의 이벤트 루프**:
```javascript
// 단일 스레드, 비동기 모델
setTimeout(() => console.log("1"), 0);
Promise.resolve().then(() => console.log("2"));
console.log("3");
// 출력: 3, 2, 1

// Web Workers로 진정한 병렬 처리
const worker = new Worker('worker.js');
worker.postMessage({cmd: 'start'});
```

**특징 비교**:

| 특성 | 컴파일 언어 | 인터프리터 언어 |
|------|------------|----------------|
| 스레드 생성 비용 | 높음 | 상대적으로 낮음 |
| 진정한 병렬성 | 가능 | 제한적 (GIL 등) |
| 메모리 효율성 | 높음 | 낮음 |
| 개발 편의성 | 낮음 | 높음 |
| 동시성 모델 | 명시적 | 추상화됨 |

**출처**:
- [Compiler vs. Interpreter in Programming - Built In](https://builtin.com/software-engineering-perspectives/compiler-vs-interpreter)
- [Interpreted vs Compiled Programming Languages - freeCodeCamp](https://www.freecodecamp.org/news/compiled-versus-interpreted-languages/)
- [Can interpreted language be natively multithreaded? - Software Engineering Stack Exchange](https://softwareengineering.stackexchange.com/questions/447894/can-interpreted-language-be-natively-multithreaded)
- [Difference between Compiled and Interpreted Language - GeeksforGeeks](https://www.geeksforgeeks.org/compiler-design/difference-between-compiled-and-interpreted-language/)
- [Threads vs. Processes: How They Work Within Your Program - Backblaze](https://www.backblaze.com/blog/whats-the-diff-programs-processes-and-threads/)

---

## 5. Java(Spring)과 Python(FastAPI)에서의 프로세스와 스레드

### 5.1 Java Spring Framework

#### 스레드 풀 관리

**ThreadPoolTaskExecutor 설정**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);      // 기본 스레드 수
        executor.setMaxPoolSize(8);        // 최대 스레드 수
        executor.setQueueCapacity(100);    // 큐 크기
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }
}
```

**비동기 처리**:
```java
@Service
public class AsyncService {
    
    @Async("taskExecutor")
    public CompletableFuture<String> processAsync() {
        // 별도 스레드에서 실행
        return CompletableFuture.completedFuture("완료");
    }
    
    // 병렬 처리
    public void parallelProcessing() {
        List<CompletableFuture<String>> futures = IntStream.range(0, 10)
            .mapToObj(i -> processAsync())
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();
    }
}
```

#### Spring WebFlux (리액티브)
```java
@RestController
public class ReactiveController {
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamData() {
        return Flux.interval(Duration.ofSeconds(1))
            .map(seq -> "Data " + seq)
            .take(10);
    }
}
```

#### 모니터링과 튜닝
```java
@Component
public class ThreadPoolMonitor {
    
    @Autowired
    private ThreadPoolTaskExecutor executor;
    
    @Scheduled(fixedDelay = 5000)
    public void monitorThreadPool() {
        int poolSize = executor.getPoolSize();
        int activeCount = executor.getActiveCount();
        int queueSize = executor.getThreadPoolExecutor().getQueue().size();
        
        log.info("Pool Size: {}, Active: {}, Queue: {}", 
            poolSize, activeCount, queueSize);
    }
}
```

**Spring의 스레드 모델 특징**:
- 서블릿 컨테이너 기반 (Tomcat 기본 200 스레드)
- 요청당 스레드 모델
- 스레드 풀로 자원 관리
- @Async로 비동기 처리
- Virtual Thread 지원 (Spring 6.1+)

**출처**:
- [Task Execution and Scheduling - Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- [Introduction to Thread Pools in Java - Baeldung](https://www.baeldung.com/thread-pool-java-and-guava)
- [ThreadPoolTaskExecutor - Spring Framework API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor.html)
- [Threading in Spring: A Comprehensive Guide](https://www.mymiller.name/wordpress/springboot/threading-in-spring-a-comprehensive-guide/)
- [How to Develop and Monitor Thread Pool Services Using Spring - DZone](https://dzone.com/articles/how-develop-and-monitor-thread)

### 5.2 Python FastAPI

#### 비동기 I/O 모델

**기본 비동기 엔드포인트**:
```python
from fastapi import FastAPI
import asyncio
import httpx

app = FastAPI()

@app.get("/")
async def root():
    # 비동기 I/O - 이벤트 루프를 블록하지 않음
    async with httpx.AsyncClient() as client:
        response = await client.get("https://api.example.com")
    return {"data": response.json()}

# 동기 함수는 스레드 풀에서 실행
@app.get("/sync")
def sync_endpoint():
    # 자동으로 스레드 풀에서 실행됨
    time.sleep(1)
    return {"status": "완료"}
```

#### CPU 집약적 작업 처리
```python
import asyncio
from concurrent.futures import ProcessPoolExecutor
from fastapi import BackgroundTasks

# 프로세스 풀 생성
process_pool = ProcessPoolExecutor(max_workers=4)

def cpu_intensive_task(n):
    """CPU 집약적 작업"""
    total = sum(i * i for i in range(n))
    return total

@app.get("/compute")
async def compute(n: int):
    loop = asyncio.get_event_loop()
    # CPU 작업을 별도 프로세스에서 실행
    result = await loop.run_in_executor(
        process_pool, 
        cpu_intensive_task, 
        n
    )
    return {"result": result}
```

#### 백그라운드 태스크
```python
from fastapi import BackgroundTasks

def write_log(message: str):
    """백그라운드에서 실행될 작업"""
    with open("log.txt", "a") as f:
        f.write(f"{message}\n")

@app.post("/send-notification/")
async def send_notification(
    email: str, 
    background_tasks: BackgroundTasks
):
    # 즉시 응답 반환
    background_tasks.add_task(write_log, f"Notification sent to {email}")
    return {"message": "Notification sent"}
```

#### 스레드 풀 활용
```python
from concurrent.futures import ThreadPoolExecutor
import asyncio

# I/O 집약적 작업을 위한 스레드 풀
thread_pool = ThreadPoolExecutor(max_workers=10)

def blocking_io_operation():
    """블로킹 I/O 작업 (예: 레거시 DB 드라이버)"""
    time.sleep(1)
    return "데이터"

@app.get("/blocking-io")
async def handle_blocking_io():
    loop = asyncio.get_event_loop()
    # 블로킹 작업을 스레드에서 실행
    result = await loop.run_in_executor(
        thread_pool,
        blocking_io_operation
    )
    return {"data": result}
```

#### 생명주기 관리
```python
from contextlib import asynccontextmanager

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 시작 시
    print("애플리케이션 시작")
    # 백그라운드 태스크 시작
    task = asyncio.create_task(background_worker())
    
    yield
    
    # 종료 시
    task.cancel()
    await process_pool.shutdown()
    print("애플리케이션 종료")

app = FastAPI(lifespan=lifespan)

async def background_worker():
    """주기적으로 실행되는 백그라운드 작업"""
    while True:
        await asyncio.sleep(60)
        # 작업 수행
```

**출처**:
- [Concurrency and async / await - FastAPI Documentation](https://fastapi.tiangolo.com/async/)
- [FastAPI python: How to run a thread in the background? - Stack Overflow](https://stackoverflow.com/questions/70872276/fastapi-python-how-to-run-a-thread-in-the-background)
- [FastAPI runs API calls in serial instead of parallel fashion - Stack Overflow](https://stackoverflow.com/questions/71516140/fastapi-runs-api-calls-in-serial-instead-of-parallel-fashion)
- [Mastering Python Async IO with FastAPI - DEV Community](https://dev.to/leapcell/mastering-python-async-io-with-fastapi-13e8)
- [Dead Simple: When to Use Async in FastAPI - Medium](https://hughesadam87.medium.com/dead-simple-when-to-use-async-in-fastapi-0e3259acea6f)
- [Concurrency with FastAPI - Medium](https://medium.com/cuddle-ai/concurrency-with-fastapi-1bd809916130)

### 5.3 성능 비교 및 최적화 전략

#### Java Spring
**장점**:
- 진정한 멀티스레딩
- 성숙한 스레드 풀 관리
- JVM 최적화 (JIT 컴파일)
- Virtual Thread로 확장성 향상

**최적화 전략**:
```properties
# application.properties
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
server.tomcat.accept-count=100
server.tomcat.connection-timeout=20000

# Virtual Threads 활성화 (Spring Boot 3.2+)
spring.threads.virtual.enabled=true
```

#### Python FastAPI
**장점**:
- 비동기 I/O로 높은 동시성
- 간단한 비동기 프로그래밍
- 낮은 메모리 사용량
- 빠른 개발 속도

**최적화 전략**:
```python
# Uvicorn 워커 설정
# uvicorn main:app --workers 4 --loop uvloop

# Gunicorn + Uvicorn
# gunicorn main:app -w 4 -k uvicorn.workers.UvicornWorker

# 비동기 라이브러리 사용
# - httpx (비동기 HTTP)
# - asyncpg (비동기 PostgreSQL)
# - motor (비동기 MongoDB)
# - aioredis (비동기 Redis)
```

#### 선택 기준

| 시나리오 | 추천 | 이유 |
|---------|------|------|
| CPU 집약적 | Java Spring | 진정한 병렬 처리, JIT 최적화 |
| I/O 집약적 | FastAPI | 효율적인 비동기 I/O |
| 실시간 처리 | Java Spring | 예측 가능한 레이턴시 |
| 마이크로서비스 | FastAPI | 가벼운 메모리 풋프린트 |
| 엔터프라이즈 | Java Spring | 성숙한 생태계, 풍부한 라이브러리 |
| 빠른 프로토타이핑 | FastAPI | 간단한 구문, 자동 문서화 |

---

## 추가 참고 자료

### 운영체제 교과서
- [Operating Systems: Design and Implementation - Tanenbaum & Woodhull (PDF)](https://csc-knu.github.io/sys-prog/books/Andrew%20S.%20Tanenbaum%20-%20Operating%20Systems.%20Design%20and%20Implementation.pdf)
- [Operating System Concepts - Silberschatz (Course Materials)](http://www.cs.man.ac.uk/~rizos/CS2051/2003-04/)

### 온라인 리소스
- [Processes and Threads - Operating Systems Core CS](https://workat.tech/core-cs/tutorial/processes-and-threads-os-6iboki1s2y3t)
- [Understanding Context Switching and Its Impact on System Performance - Netdata](https://www.netdata.cloud/blog/understanding-context-switching-and-its-impact-on-system-performance/)
- [Thread Pools in Java - GeeksforGeeks](https://www.geeksforgeeks.org/java/thread-pools-java/)
- [What is a "thread" (really)? - Stack Overflow](https://stackoverflow.com/questions/5201852/what-is-a-thread-really)

### 깃헙 이슈 및 토론
- [Fast api is blocking long running requests when using asyncio calls - GitHub Discussion](https://github.com/fastapi/fastapi/discussions/8842)
- [Run async operations on separate threads - FastAPI GitHub Discussion](https://github.com/fastapi/fastapi/discussions/6305)

---

## 결론

프로세스와 스레드는 현대 컴퓨팅의 핵심 개념으로, 각각의 특성과 용도를 정확히 이해하는 것이 중요합니다.

**핵심 요약**:
1. **프로세스**는 독립적인 실행 단위로 안정성이 높지만 자원 소모가 큼
2. **스레드**는 가벼운 실행 단위로 효율적이지만 동기화 문제에 주의 필요
3. **언어와 프레임워크**마다 고유한 동시성 모델을 제공
4. **애플리케이션 특성**에 맞는 적절한 모델 선택이 성능의 핵심
5. **하드웨어 발전**(멀티코어, Virtual Thread)으로 동시성 프로그래밍의 중요성 증가

올바른 동시성 모델 선택과 최적화를 통해 확장 가능하고 효율적인 시스템을 구축할 수 있습니다.