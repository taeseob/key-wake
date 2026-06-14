# Key-Wake (키웨이크) - 아침을 깨우는 맞춤형 미션 알람 앱

**Key-Wake**는 단순히 기상하는 것뿐만 아니라, **사용자가 잠에서 깨어나 꼭 해야 할 특정 행동(자리 이동, 확언 타이핑, 음성 발화 등)을 즉각적으로 유도**하여 생산적인 하루의 시작을 돕는 Android 전용 알람 애플리케이션입니다.

사용자가 설정한 알람 해제 조건(미션)을 완벽하게 성공하기 전까지는 알람 소리와 진동이 멈추지 않으며, 홈 화면으로 도망치거나 백 버튼으로 강제 종료할 수 없도록 설계되었습니다.

---

## 🚀 주요 기능 (Key Features)

1. **다중 알람 관리 (Alarm Management)**:
   - 시/분 시간 설정, 요일별 반복 설정, 알람 라벨 지정 지원.
   - **진동 전용 모드** 지원 (소리 없이 강력한 진동 패턴으로만 작동 가능).
   - 무한 재생 구조 (완벽히 기상할 때까지 정지 불가).

2. **3대 알람 해제 미션 (Wake-up Missions)**:
   - ✍️ **글자 받아쓰기 (Text)**: 사용자가 미리 등록한 확언이나 각오 문구를 정확하게 받아쓰는 미션 (백스페이스를 활용한 오타 수정 지원).
   - 🚶‍♂️ **걸음 수 측정 (Step Counter)**: 자리를 일어나 물리적으로 지정된 걸음 수(예: 30~50걸음) 이상 이동해야 종료되는 실내 최적화 기상 미션 (물리 센서 스텝 감지 및 가속도계 보정 탑재).
   - 🎙️ **음성 인식 (Voice)**: 스마트폰 내장 STT API를 활용해 목표 구절을 따라 말하는 미션 (오프라인 상태 시 2분 후 자동 정지 및 즉시 끄기 버튼 제공의 폴백 시나리오 작동).

3. **고신뢰성 알람 엔진 (High-Reliability Engine)**:
   - 디바이스 슬립 모드에서도 정확하게 울리는 `AlarmManager` 연동.
   - 통화 중 수신이 들어올 경우 전화를 받을 수 있도록 일시 묵음 처리 및 통화 종료 시 자동 재개 기능.
   - 기기 재부팅 시 기존 알람 구동 스케줄을 자동으로 재등록하는 `BootReceiver` 탑재.

4. **프리미엄 테마 시스템 (Dynamic Themes)**:
   - 설정 화면에서 취향에 맞게 디자인 톤앤매너를 직접 선택 가능.
   - Midnight Dark(기본), Cyberpunk Neon, Sunset Glow, Minimal Light 총 4가지 프리미엄 스킨 제공.

---

## 🛠 기술 스택 (Tech Stack)

- **Platform**: Android Native
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Modern Declartive UI)
- **Persistency**: SharedPreferences + Kotlinx.Serialization JSON (가볍고 안전한 로컬 저장 구조)
- **Background Engine**: Foreground Service & BroadcastReceiver (Exact Alarm API)
- **Hardware Integration**: SensorManager (Step Counter, Step Detector, Accelerometer), AudioManager (Audio Focus 제어), SpeechRecognizer API

---

## 📂 개발 문서 (Development Documents)

아래 문서를 통해 프로젝트 기획 요구사항부터 구현, 빌드 결과까지의 자세한 개발 히스토리를 확인하실 수 있습니다.

- [📝 구현 계획서 (Implementation Plan)](./implementation_plan.md) - 플랫폼 제약 분석 및 세부 사양 정의서.
- [📋 개발 태스크 목록 (Task List)](./task.md) - 단계별 구현 일정 및 기능 개발 체크리스트.
- [🔍 개발 결과 보고서 (Walkthrough)](./walkthrough.md) - 전체 코드 아키텍처 의사결정 및 빌드 검증 보고서.

---

## 💻 빌드 및 시작하기 (How to Build & Run)

1. 이 프로젝트를 로컬에 복제합니다.
   ```bash
   git clone https://github.com/taeseob/key-wake.git
   ```
2. Android Studio (Koala 이상 권장)를 실행하고 프로젝트 루트 디렉토리를 오픈합니다.
3. 로컬 JVM 및 Gradle 동기화를 완료합니다. (JDK 17 사용)
4. 터미널에서 아래 명령어를 통해 프로젝트 코드를 즉시 컴파일하고 문법 오류를 검증할 수 있습니다.
   ```bash
   # Windows PowerShell
   .\gradlew.bat compileDebugKotlin
   
   # Mac/Linux Terminal
   ./gradlew compileDebugKotlin
   ```
5. 기기(에뮬레이터 또는 실기기)를 연결하고 실행 버튼(Run)을 눌러 설치합니다.
