package com.izukiken.adfreebrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private val prefs by lazy { getSharedPreferences("browser", MODE_PRIVATE) }
    private var privateMode = false
    private var adBlocking = true
    private val homeUrl = "https://duckduckgo.com/"

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 16): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adBlocking = prefs.getBoolean("block_ads", true)
        buildUi()
        configureWebView()
        if (savedInstanceState != null) browser.restoreState(savedInstanceState)
        else browser.loadUrl(homeUrl)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(15, 20, 27))
            setPadding(dp(12), dp(10), dp(12), 0)
        }
        val brand = TextView(this).apply {
            text = "ADFREE  •  BROWSER"
            setTextColor(Color.rgb(87, 214, 181))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(3), 0, dp(9))
        }
        root.addView(brand, LinearLayout.LayoutParams(-1, dp(36)))

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        address = EditText(this).apply {
            isSingleLine = true
            textSize = 14f
            hint = "Search or enter website"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            setPadding(dp(14), 0, dp(8), 0)
            background = rounded(Color.rgb(35, 43, 54), 14)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    navigate(address.text.toString()); true
                } else false
            }
        }
        addressRow.addView(address, LinearLayout.LayoutParams(0, dp(48), 1f))
        val go = button("GO")
        go.setOnClickListener { navigate(address.text.toString()) }
        addressRow.addView(go, LinearLayout.LayoutParams(dp(54), dp(48)).apply { leftMargin = dp(8) })
        root.addView(addressRow)

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(7))
        }
        listOf("‹", "›", "↻", "⌂", "⋮").forEachIndexed { index, label ->
            val b = button(label)
            b.textSize = if (index == 4) 22f else 23f
            b.setOnClickListener {
                when (index) {
                    0 -> if (browser.canGoBack()) browser.goBack()
                    1 -> if (browser.canGoForward()) browser.goForward()
                    2 -> browser.reload()
                    3 -> browser.loadUrl(homeUrl)
                    4 -> showMenu(b)
                }
            }
            tools.addView(b, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                if (index > 0) leftMargin = dp(5)
            })
        }
        root.addView(tools)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(87, 214, 181))
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(2)))
        status = TextView(this).apply {
            text = "Ad and tracker filter: ON"
            textSize = 11f
            setTextColor(Color.rgb(160, 175, 190))
            setPadding(dp(4), dp(5), 0, dp(5))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(25)))
        browser = WebView(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun button(label: String) = Button(this).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(35, 43, 54), 12)
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(0, 0, 0, 0)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        browser.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        CookieManager.getInstance().setAcceptCookie(true)
        browser.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
        }
        browser.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): android.webkit.WebResourceResponse? {
                if (adBlocking && request != null && AdBlocker.shouldBlock(request)) return AdBlocker.emptyResponse()
                return super.shouldInterceptRequest(view, request)
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                address.setText(url ?: "")
                status.text = (if (adBlocking) "Filter ON" else "Filter OFF") +
                    (if (privateMode) "  •  Private" else "")
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                address.setText(url ?: "")
            }
        }
    }

    private fun navigate(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        val url = if (text.contains(" ") || (!text.contains(".") && !text.startsWith("http"))) {
            "https://html.duckduckgo.com/html/?q=" + android.net.Uri.encode(text)
        } else if (text.startsWith("http://") || text.startsWith("https://")) text
        else "https://$text"
        browser.loadUrl(url)
    }

    private fun showMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(Menu.NONE, 1, 1, if (adBlocking) "Turn ad filter off" else "Turn ad filter on")
            menu.add(Menu.NONE, 2, 2, if (privateMode) "Exit private mode" else "New private session")
            menu.add(Menu.NONE, 3, 3, "Clear browsing data")
            menu.add(Menu.NONE, 4, 4, "About")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        adBlocking = !adBlocking
                        prefs.edit().putBoolean("block_ads", adBlocking).apply()
                        status.text = if (adBlocking) "Ad and tracker filter: ON" else "Ad and tracker filter: OFF"
                        browser.reload()
                    }
                    2 -> {
                        privateMode = !privateMode
                        CookieManager.getInstance().setAcceptCookie(!privateMode)
                        if (privateMode) {
                            browser.clearCache(true)
                            CookieManager.getInstance().removeAllCookies(null)
                            Toast.makeText(this@MainActivity, "Private session started; cookies disabled", Toast.LENGTH_SHORT).show()
                        } else {
                            CookieManager.getInstance().setAcceptCookie(true)
                            Toast.makeText(this@MainActivity, "Private session ended", Toast.LENGTH_SHORT).show()
                        }
                        status.text = (if (adBlocking) "Filter ON" else "Filter OFF") + if (privateMode) "  •  Private" else ""
                    }
                    3 -> AlertDialog.Builder(this@MainActivity)
                        .setTitle("Clear browsing data?")
                        .setMessage("This clears browser history, cookies, cache and saved site data.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Clear") { _, _ ->
                            browser.clearHistory()
                            browser.clearCache(true)
                            browser.clearFormData()
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                            Toast.makeText(this@MainActivity, "Browsing data cleared", Toast.LENGTH_SHORT).show()
                        }.show()
                    4 -> AlertDialog.Builder(this@MainActivity).setTitle("AdFree Browser")
                        .setMessage("A lightweight WebView browser. Its built-in host filter blocks some common ad and tracking domains, but cannot remove every ad.")
                        .setPositiveButton("OK", null).show()
                }
                true
            }
            show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        browser.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (browser.canGoBack()) browser.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        browser.destroy()
        super.onDestroy()
    }
}
