package inu.appcenter.intip_android

import android.util.Log
import android.webkit.JavascriptInterface

class WebAppInterface(
    private val onUpdateRequested: () -> Unit,
    private val onAppSettingsRequested: () -> Unit,
    private val onWebDiagnosticsLogged: ((String) -> Unit)? = null,
    private val onLaunchWebCleanupFinished: ((String) -> Unit)? = null,
    private val onPushState: (() -> Unit)? = null,
    private val onNavigateToRequested: ((String, String) -> Unit)? = null,
    private val onGoBackRequested: (() -> Unit)? = null
) {
    @JavascriptInterface
    fun navigateTo(destination: String, url: String) {
        Log.d("WebAppInterface", "navigateTo: $destination, $url")
        onNavigateToRequested?.invoke(destination, url)
    }

    @JavascriptInterface
    fun goBack() {
        Log.d("WebAppInterface", "goBack")
        onGoBackRequested?.invoke()
    }

    @JavascriptInterface
    fun onRouteChange(path: String) {
        Log.d("RouteChange", "경로 변경 $path")
    }

    @JavascriptInterface
    fun onPushState() {
        onPushState?.invoke()
    }

    @JavascriptInterface
    fun requestAppUpdate() {
        onUpdateRequested()
    }

    @JavascriptInterface
    fun openAppSettings() {
        onAppSettingsRequested()
    }

    @JavascriptInterface
    fun requestPermissionSettings() {
        onAppSettingsRequested()
    }

    @JavascriptInterface
    fun logWebDiagnostics(payload: String) {
        onWebDiagnosticsLogged?.invoke(payload)
    }

    @JavascriptInterface
    fun onLaunchWebCleanupFinished(payload: String) {
        onLaunchWebCleanupFinished?.invoke(payload)
    }
}

