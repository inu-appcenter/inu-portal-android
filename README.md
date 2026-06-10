# INU 포탈넷 안드로이드

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black"/>
  <img src="https://img.shields.io/badge/minSdk-25-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/version-1.0.3-brightgreen?style=flat-square"/>
  <br/><br/>
  <a href="https://play.google.com/store/apps/details?id=inu.appcenter.intip_android&hl=ko">
    <img src="https://img.shields.io/badge/Google_Play-414141?style=flat-square&logo=google-play&logoColor=white"/>
  </a>
  <a href="https://apps.apple.com/us/app/intip-%EC%9D%B8%ED%8C%81-%EC%9D%B8%EC%B2%9C%EB%8C%80-%EA%B3%B5%EC%A7%80%EC%95%8C%EB%A6%AC%EB%AF%B8-%EC%9D%B8%EC%9E%85%EB%9F%B0-%EC%A0%84%ED%99%94%EB%B2%88%ED%98%B8%EB%B6%80/id6740070975">
    <img src="https://img.shields.io/badge/App_Store-0D96F6?style=flat-square&logo=app-store&logoColor=white"/>
  </a>
</p>

인천대학교 학생들을 위한 포탈넷 안드로이드 애플리케이션입니다.  
**MAU 2,702 · DAU 655**

웹앱을 WebView로 감싸는 하이브리드 앱 구조로, 네이티브 로그인·푸시알림·파일 처리 등을 안드로이드 레이어에서 담당합니다.  
현재는 완전한 네이티브 앱으로 전환되어 이 레포지토리는 레거시입니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Kotlin |
| UI | Jetpack Compose (로그인, 스플래시, 바텀바), WebView |
| Architecture | MVVM, Repository Pattern |
| Network | Retrofit2, OkHttp3 |
| DI | Koin |
| Local Storage | DataStore |
| Push | Firebase Cloud Messaging (FCM) |
| Monitoring | Firebase Crashlytics, Analytics |

---

## 아키텍처

네이티브 UI는 인증·네비게이션 레이어에만 존재하고, 실제 콘텐츠는 WebView가 담당합니다.

```
┌─────────────────────────────────────┐
│           Android Native            │
│  SplashScreen → LoginScreen         │
│  BottomBar (네이티브 라우팅 동기화)   │
│  WebViewScreen                      │
│    └─ AndroidBridge (JS ↔ Native)   │
│  AuthInterceptor (토큰 자동 주입)    │
└─────────────────────────────────────┘
          ↕ WebView
┌─────────────────────────────────────┐
│         INU 포탈넷 Web App          │
│  공지사항 / 학식 / 도서관 / 동아리   │
│  캠퍼스맵 / 헬로버스 / 청원 등       │
└─────────────────────────────────────┘
```

---

## 구현 내용

> Implemented by [@junhee8649](https://github.com/junhee8649)

### 로그인 및 JWT 토큰 관리

학번/비밀번호 로그인 후 Access Token과 Refresh Token을 DataStore에 저장합니다.  
앱 시작 시 토큰 만료 여부를 먼저 확인하고, 만료된 경우에만 Refresh API를 호출해 서버 부담을 줄였습니다.  
Refresh Token까지 만료된 경우 토큰을 전부 삭제하고 로그인 화면으로 이동합니다.

- OkHttp `Interceptor`로 모든 요청에 Access Token 자동 주입
- Refresh Token 만료 시 로그아웃 처리 및 토큰 전체 초기화
- 일부 사용자에서 발생하던 `SocketTimeoutException` 예외 처리 추가

### Compose-Legacy View 레이아웃 측정 차이로 인한 WebView 무한 스크롤 불가

Jetpack Compose의 `AndroidView`로 WebView를 감쌀 때, Compose의 레이아웃 측정 방식과 Legacy View의 측정 방식이 달라 WebView 높이가 제대로 계산되지 않아 무한 스크롤이 동작하지 않는 문제가 있었습니다.  
`layoutParams`에 `MATCH_PARENT`를 명시적으로 할당해 해결했습니다.

### WebView ↔ 네이티브 라우팅 동기화

웹앱에서 페이지를 이동할 때 네이티브 바텀바 상태와 백스택이 함께 동기화되어야 합니다.  
`WebViewClient`에서 URL을 인터셉트해 쿼리 파라미터를 파싱하고, 해당하는 네이티브 Destination으로 라우팅합니다.  
웹앱의 뒤로가기 버튼도 `AndroidBridge`를 통해 네이티브 `popBackStack`과 연동했습니다.

### NavGraph 타이밍 문제 → 스플래시 화면으로 해결

앱 최초 실행 시 WebView 화면을 첫 Destination으로 바로 띄우면, NavGraph가 초기화되기 전에 라우팅이 시도되어 부모 라우트를 인식하지 못하는 문제가 있었습니다.  
중간에 Splash 화면을 추가해 NavGraph 준비 시간을 확보하는 방식으로 해결했습니다.

### WebView UX 버그 수정

- **키보드 검은 패딩**: WebView에서 키보드가 올라올 때 하단에 검은 영역이 생기는 문제를 `setDecorFitsSystemWindows(window, true)` 설정으로 해결
- **로그인 만료 alert 중복 노출**: 토큰 만료 시 alert가 반복적으로 뜨는 문제를 플래그 관리로 한 번만 노출되도록 수정
- **바텀바 중복 스택**: 바텀바 탭 이동 시 스택이 계속 쌓이는 문제를 `popBackStack` 수동 관리로 해결

### 보안 및 코드 품질

- `google-services.json` `.gitignore` 추가 및 히스토리에서 제거
- ProGuard 규칙 설정 (JSON 데이터 난독화 방지 포함)
- 서버 URL, 앱 설정 상수를 `K` 오브젝트로 일괄 관리
