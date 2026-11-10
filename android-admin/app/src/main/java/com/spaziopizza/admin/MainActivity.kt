package com.spaziopizza.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.app.Activity
import com.google.firebase.messaging.FirebaseMessaging
import android.os.Bundle
import android.graphics.Color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.widget.Toast
import android.widget.FrameLayout
import android.view.ViewGroup
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fcmToken = ""
    private val adminUrl = "https://spaziopizzadelivery.github.io/spazio-pizzaria/admin.html"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        // Mantém o conteúdo do app dentro da área segura do Android.
        // Um contêiner externo recebe os insets e desloca o WebView inteiro,
        // evitando que o cabeçalho e o menu inferior fiquem sob as barras do sistema.
        val systemBarColor = Color.rgb(26, 21, 17)
        window.statusBarColor = systemBarColor
        window.navigationBarColor = systemBarColor
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val safeArea = FrameLayout(this).apply {
            // A cor do contêiner também preenche as áreas das barras do sistema.
            setBackgroundColor(systemBarColor)
        }
        webView = WebView(this)
        safeArea.addView(webView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        ViewCompat.setOnApplyWindowInsetsListener(safeArea) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            windowInsets
        }
        setContentView(safeArea)
        ViewCompat.requestApplyInsets(safeArea)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.webChromeClient = WebChromeClient()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            fcmToken = token
            if (webView.url?.startsWith(adminUrl) == true) {
                webView.evaluateJavascript("window.spazioFcmToken = " + org.json.JSONObject.quote(token) + "; window.spazioRegisterAndroidPushToken?.()", null)
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (url.startsWith(adminUrl) && fcmToken.isNotEmpty()) {
                    view.evaluateJavascript("window.spazioFcmToken = " + org.json.JSONObject.quote(fcmToken) + "; window.spazioRegisterAndroidPushToken?.()", null)
                }
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return !request.url.toString().startsWith("https://spaziopizzadelivery.github.io/spazio-pizzaria/")
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) Toast.makeText(this@MainActivity, "Sem conexão. Verifique sua internet e tente novamente.", Toast.LENGTH_LONG).show()
            }
        }
        if (savedInstanceState == null) webView.loadUrl(adminUrl) else webView.restoreState(savedInstanceState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
