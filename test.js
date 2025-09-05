const mainPageUrl = 'YOUR_MAIN_PAGE_URL'; // 4단계의 mainPageUrl과 동일하게 맞춰주세요
let fcmToken = null;

document.addEventListener('DOMContentLoaded', function() {
    // AndroidBridge가 있는지 확인 (앱의 WebView에서 실행되었는지 감지)
    if (window.AndroidBridge) {
        console.log("AndroidBridge is available.");
        // 네이티브 앱으로부터 FCM 토큰을 가져옴
        fcmToken = window.AndroidBridge.getFcmToken();
        if (fcmToken) {
            console.log("FCM Token received from native: " + fcmToken);
        } else {
            console.log("FCM Token is not available from native yet.");
        }
    } else {
        console.log("AndroidBridge is not available.");
    }
});

// 로그인 API 호출 후, 성공 콜백 함수에서 아래 로직을 실행
function onLoginSuccess() {
    console.log("Login successful.");

    // AndroidBridge와 fcmToken이 모두 유효한 경우에만 실행
   if (window.AndroidBridge && fcmToken) {
    console.log("Calling native saveFcmToken with token: " + fcmToken);

    // 네이티브 앱에 토큰 저장을 요청
    window.AndroidBridge.saveFcmToken(fcmToken);
   } else {
    console.log("Cannot save FCM token. Bridge or token is missing.");
   }
    // 토큰 저장 요청 후 메인 페이지로 이동
   console.log("Redirecting to main page: " + mainPageUrl);
   window.location.href = mainPageUrl;
}


// --- 아래는 예시입니다. 실제 로그인 로직에 맞게 수정하세요. ---
// 예: 로그인 버튼 클릭 이벤트
document.getElementById('login-button').addEventListener('click', function() {
    // 가짜 로그인 성공 시뮬레이션
    setTimeout(function() {
        onLoginSuccess();
    }, 1000); // 1초 후 성공했다고 가정
});
