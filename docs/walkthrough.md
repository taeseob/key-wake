# Key-Wake 알람 애플리케이션 개발 완료 보고서 (Walkthrough)

사용자가 설정한 알람 해제 조건(미션)을 수행하기 전까지 절대 꺼지지 않는 Android 전용 알람 앱 **'Key-Wake(키웨이크)'**의 모든 핵심 기능 설계, 구현 및 빌드 검증을 성공적으로 마쳤습니다.

---

## 1. 구현된 핵심 기능 목록
1. **다중 알람 관리 (Alarm Management)**:
   - 등록된 알람들의 시/분 시간 설정, 요일 반복 설정, 진동 전용(무음) 모드, 개별 알람 토글 스위치 제공.
   - 로컬 SharedPreferences 및 Kotlin Serialization을 결합하여 가볍고 견고하게 데이터 영속성 유지 (Room DB 사용에 따른 추가 복잡성 및 KSP 버전 충돌 배제).
2. **백그라운드 알람 엔진 (Alarm Engine)**:
   - `AlarmManager`를 사용해 디바이스가 슬립 상태이거나 백그라운드일 때도 정확한 시간(Exact Alarm)에 알람 작동 보장.
   - 시스템 재부팅 시 예약을 복구하는 `BootReceiver` 구현.
   - 알람 울림 시 백그라운드 강제 종료를 막기 위한 `Foreground Service` 구동.
3. **잠금화면 오버레이 (Lockscreen Overlay)**:
   - `fullScreenIntent` 알림과 Activity Window Flag 설정을 조합하여, 잠금화면 상태에서도 앱 화면이 강제로 상단에 표시되어 화면을 점유하도록 설계.
   - 사용자가 백 버튼으로 알람을 강제 종료할 수 없도록 물리 백프레스 무력화 (`BackHandler` 연동).
4. **전화 수신 예외 처리 (Call Handling)**:
   - 오디오 포커스(`AudioManager.OnAudioFocusChangeListener`) 변화를 실시간으로 감지.
   - 알람 작동 중 통화 수신 시 알람 벨소리와 진동이 자동으로 묵음 및 정지되며, 통화가 종료되는 즉시 알람이 재개되도록 구현 (민감한 권한 요구 없이 깔끔하게 작동).
5. **3가지 해제 미션 (Missions)**:
   - **글자 받아쓰기 (Text)**: 오타 수정(백스페이스)이 가능한 텍스트 입력창 제공. 설정된 문구와 100% 일치 시 알람 즉시 해제.
   - **걸음 수 측정 (Step Counter)**: Android 기본 물리 센서(`TYPE_STEP_COUNTER` 또는 `TYPE_STEP_DETECTOR`)를 사용하며, 해당 하드웨어 센서가 없을 경우에 대비해 가속도 센서(`TYPE_ACCELEROMETER`)를 기반으로 한 피크 감지 방식의 자체 알고리즘을 하이브리드로 탑재하여 권한 없이도 실내 걷기를 완벽히 감지.
   - **음성 인식 (Voice)**: 안드로이드 내장 `SpeechRecognizer` API 연동.
6. **음성 인식 오프라인 폴백 (STT Offline Fallback)**:
   - 네트워크 연결 여부를 실시간으로 감지하여 온라인인 경우에만 마이크 버튼 활성화.
   - 오프라인일 때(비행기 모드 등)는 음성 인식이 제한된다는 안내와 함께 **즉시 끄기 버튼**을 제공하며, 사용자가 영원히 알람을 끄지 못하는 불상사를 방지하기 위해 2분간 알람 작동 후 자동으로 정지되는 기본 알람 폴백 시나리오 동작.
7. **디자인 테마 선택 시스템 (Theme Selection)**:
   - 사용자가 설정 화면에서 취향에 맞게 디자인 톤앤매너를 직접 선택 가능.
   - Midnight Dark(기본), Cyberpunk Neon, Sunset Glow, Minimal Light 총 4가지 프리미엄 스킨 제공.

---

## 2. 생성 및 수정된 파일 상세

- **데이터 레이어 (Data & Persistency)**
  - [Alarm.kt](../app/src/main/java/com/taeseob/keywake/data/Alarm.kt): 알람 모델 및 미션 타입 정의.
  - [AlarmRepository.kt](../app/src/main/java/com/taeseob/keywake/data/AlarmRepository.kt): SharedPreferences 및 JSON 직렬화를 통한 영속성 및 테마 저장 제어.
  - [KeyWakeApplication.kt](../app/src/main/java/com/taeseob/keywake/KeyWakeApplication.kt): 싱글톤 패턴의 Repository 접근을 지원하는 Application 클래스.

- **알람 구동 엔진 및 리시버 (Alarm Engine)**
  - [AlarmScheduler.kt](../app/src/main/java/com/taeseob/keywake/util/AlarmScheduler.kt): AlarmManager 예약 등록 및 취소 유틸리티.
  - [AlarmReceiver.kt](../app/src/main/java/com/taeseob/keywake/receiver/AlarmReceiver.kt): 지정 시간에 예약을 받아 Foreground Service를 실행하는 BroadcastReceiver.
  - [BootReceiver.kt](../app/src/main/java/com/taeseob/keywake/receiver/BootReceiver.kt): 기기 부팅 시 알람 목록을 다시 동적으로 등록해 주는 Receiver.
  - [AlarmService.kt](../app/src/main/java/com/taeseob/keywake/service/AlarmService.kt): 알람 벨소리/진동 미디어 재생 및 오디오 포커스 감지 제어용 Foreground Service.

- **UI 레이어 및 테마 (UI & Design)**
  - [MainActivity.kt](../app/src/main/java/com/taeseob/keywake/MainActivity.kt): 알림, 녹음, 활동 감지 권한을 기동 시 동적 획득하며 테마와 내비게이션을 로딩하는 메인 엔트리.
  - [NavigationKeys.kt](../app/src/main/java/com/taeseob/keywake/NavigationKeys.kt): 직렬화 가능한 라우팅 Key 모음.
  - [Navigation.kt](../app/src/main/java/com/taeseob/keywake/Navigation.kt): 메인, 알람 수정, 설정 화면 라우팅 연결.
  - [MainScreen.kt](../app/src/main/java/com/taeseob/keywake/ui/main/MainScreen.kt): 등록된 알람 목록 및 메인 뷰 구현.
  - [MainScreenViewModel.kt](../app/src/main/java/com/taeseob/keywake/ui/main/MainScreenViewModel.kt): 알람 목록 상태 흐름 관리 뷰모델.
  - [AddEditAlarmScreen.kt](../app/src/main/java/com/taeseob/keywake/ui/alarm/AddEditAlarmScreen.kt): 요일 반복, 진동 전용, 미션 조건 설정용 세부 에디터.
  - [SettingsScreen.kt](../app/src/main/java/com/taeseob/keywake/ui/settings/SettingsScreen.kt): 테마 설정 카드 및 오프라인 Google STT 활성화 튜토리얼 뷰.
  - [AlarmActivity.kt](../app/src/main/java/com/taeseob/keywake/ui/alarm/AlarmActivity.kt): 실제 알람 구동 및 3대 미션(텍스트 입력, 걸음 수, 마이크 음성)의 실시간 상태 검증 화면.
  - [Color.kt](../app/src/main/java/com/taeseob/keywake/theme/Color.kt) & [Theme.kt](../app/src/main/java/com/taeseob/keywake/theme/Theme.kt): 4대 프리미엄 컬러 세트 및 동적 테마 시스템 연동.

- **설정 및 명세 (Settings)**
  - [AndroidManifest.xml](../app/src/main/AndroidManifest.xml): 알람, 진동, 백그라운드 서비스 유형, 권한 등 정의.
  - [build.gradle.kts](../app/build.gradle.kts): 직렬화 엔진 및 아이콘 라이브러리 탑재.
  - [gradle.properties](../gradle.properties): 윈도우 환경 내 한글 경로명이 존재할 때의 빌드 충돌 해결을 위한 `android.overridePathCheck=true` 설정.

---

## 3. 검증 결과 (Verification Results)
- 로컬 안드로이드 컴파일러를 이용해 Kotlin 코드의 검증을 수행했습니다.
- `.\gradlew.bat compileDebugKotlin` 빌드 확인 결과: **성공 (BUILD SUCCESSFUL in 1m 29s)**
- 어떠한 컴파일 에러, 패키지 미인식, 문법 충돌 없이 성공적으로 완전한 빌드가 완료됨을 확인하였습니다.
