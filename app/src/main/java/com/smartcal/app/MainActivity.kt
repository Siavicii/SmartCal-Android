package com.smartcal.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val GITHUB_REPO = "Siavicii/SmartCal-Android"
    private val CHANNEL_ID = "smartcal_status_date_channel"
    private val NOTIF_ID = 6201

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IconUpdater.updateTodayIcon(this)
        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
        checkForGitHubUpdate()

        WindowCompat.setDecorFitsSystemWindows(window, true)
        webView = WebView(this)
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setGeolocationEnabled(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
    }

    private fun checkForGitHubUpdate() {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "SmartCal-Android")
                }
                if (conn.responseCode != 200) return@Thread

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestTag = json.optString("tag_name", "").removePrefix("v").trim()
                val assets = json.optJSONArray("assets")

                var apkUrl = json.optString("html_url", "")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name").endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url", apkUrl)
                            break
                        }
                    }
                }

                val currentVer = packageManager.getPackageInfo(packageName, 0).versionName ?: "0"
                if (latestTag.isNotEmpty() && latestTag != currentVer && apkUrl.isNotEmpty()) {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            AlertDialog.Builder(this)
                                .setTitle("بروزرسانی جدید (نسخه $latestTag)")
                                .setMessage("نسخه جدید تقویم هوشمند منتشر شده است. آیا مایل به دانلود و نصب آن هستید؟")
                                .setPositiveButton("دانلود و نصب") { _, _ ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                                    startActivity(intent)
                                }
                                .setNegativeButton("بعداً", null)
                                .show()
                        }
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تاریخ روز در نوار وضعیت",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش دائمی تاریخ روز در نوار وضعیت بالای گوشی"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001
                )
            }
        }
    }

    private fun createDayNumberBitmap(dayText: String): Bitmap {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isFakeBoldText = true
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 3.5f
            textSize = 100f
        }
        val bounds = Rect()
        paint.getTextBounds(dayText, 0, dayText.length, bounds)
        val target = size * 0.90f
        val scale = minOf(
            target / bounds.width().coerceAtLeast(1),
            target / bounds.height().coerceAtLeast(1)
        )
        paint.textSize = paint.textSize * scale
        paint.getTextBounds(dayText, 0, dayText.length, bounds)
        val x = size / 2f - bounds.exactCenterX()
        val y = size / 2f - bounds.exactCenterY()
        canvas.drawText(dayText, x, y, paint)
        return bmp
    }

    override fun onResume() {
        super.onResume()
        IconUpdater.updateTodayIcon(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun syncStatusBarColor(hexColor: String, isLightIcons: Boolean) {
            runOnUiThread {
                try {
                    window.statusBarColor = Color.parseColor(hexColor)
                    window.navigationBarColor = Color.parseColor(hexColor)
                    val ctrl = WindowInsetsControllerCompat(window, window.decorView)
                    ctrl.isAppearanceLightStatusBars = isLightIcons
                    ctrl.isAppearanceLightNavigationBars = isLightIcons
                } catch (_: Exception) {}
            }
        }

        @JavascriptInterface
        fun showStatusDateNotification(dayNum: String, title: String, body: String): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    runOnUiThread { requestNotificationPermissionIfNeeded() }
                    return false
                }
                val launchIntent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pi = PendingIntent.getActivity(
                    this@MainActivity,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val dayIcon = IconCompat.createWithBitmap(createDayNumberBitmap(dayNum))
                val notif = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                    .setSmallIcon(dayIcon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setOngoing(true)
                    .setSilent(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pi)
                    .build()
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, notif)
                true
            } catch (_: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun cancelStatusDateNotification() {
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIF_ID)
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun fetchUrlNative(urlString: String): String {
            return try {
                val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) SmartCal/62.1")
                }
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else ""
            } catch (_: Exception) {
                ""
            }
        }
    }
}
