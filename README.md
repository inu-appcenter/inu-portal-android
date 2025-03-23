# INU 포탈넷 안드로이드

인천대학교 앱센터에서 개발한, 인천대학교 학생들을 위한 포탈넷 안드로이드 애플리케이션입니다.

## 프로젝트 아키텍처 원칙

1. **불필요하고 중복된 코드는 방지할 것**
2. **코드 구조의 일관성을 유지할 것**
3. **2번 원칙을 고수하되 클린 아키텍처 구조를 실현할 것**

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose
- **아키텍처**: MVVM 패턴
- **네트워크**: Retrofit2, OkHttp3
- **종속성 주입**: Koin
- **이미지 로딩**: Coil
- **로컬 데이터 저장**: DataStore
- **분석 및 모니터링**: Firebase Analytics, Crashlytics

## 프로젝트 구조

```
inu.appcenter.intip_android
├── di        - Koin 종속성 주입 설정
├── local     - 로컬 데이터 관리
├── model     - 데이터 모델 클래스
├── network   - API 통신 (Retrofit, 인터셉터)
├── repository - 데이터 리포지토리
├── ui
│   ├── component - 재사용 가능한 UI 컴포넌트
│   ├── login     - 로그인 관련 UI
│   ├── navigate  - 네비게이션 설정
│   ├── screen    - 화면 UI
│   └── theme     - 앱 테마 및 스타일
└── utils     - 유틸리티 함수 및 상수
```

## 주요 기능

- 학교 공지사항 확인
- 캠퍼스 맵 및 헬로버스 정보
- 학식 메뉴
- 학사 일정
- 도서관 서비스
- 분실물 정보
- 동아리 활동
- 총학생회 소식 및 청원 시스템
- 사용자 프로필 관리

## 시작하기

### 요구사항
- Android Studio Arctic Fox 이상
- Kotlin 1.9.0 이상
- Android SDK 25 이상

### 설치 방법
```bash
git clone https://github.com/inu-appcenter/inu-portal-android.git
```

## 버전 정보
현재 버전: 0.1.2 (versionCode: 12)