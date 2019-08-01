/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.content.Context
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.TextView
import kotlinx.coroutines.*
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.readium.r2.navigator.BuildConfig
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.shared.Locations
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.getAbsolute
import java.util.concurrent.CountDownLatch


/**
 * Created by Aferdita Muriqi on 12/2/17.
 */

open class R2BasicWebView(context: Context, attrs: AttributeSet) : WebView(context, attrs) {

    lateinit var activity: R2EpubActivity
    var progression: Double = 0.0
    var overrideUrlLoading = true
    var resourceUrl: String? = null

    var callback: OnOverScrolledCallback? = null

    private val uiScope = CoroutineScope(Dispatchers.Main)

    init {
      setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }

    interface OnOverScrolledCallback {
        fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean)
    }

    fun setOnOverScrolledCallback(callback: OnOverScrolledCallback) {
        this.callback = callback
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        if (callback != null) {
            callback?.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        }
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }


    @android.webkit.JavascriptInterface
    open fun scrollRight() {
        uiScope.launch {

            println("                       DEBUG_INFORMATIONS: "+ "Next Page Asked")
            if (activity.supportActionBar!!.isShowing && activity.allowToggleActionBar) {
                activity.resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            }
            val scrollMode = activity.preferences.getBoolean(SCROLL_REF, false)
            if (scrollMode) {
                if (activity.publication.metadata.direction == "rtl") {
                    this@R2BasicWebView.evaluateJavascript("scrollRightRTL();") { result ->
                        if (result.contains("edge")) {
                            activity.previousResource(false)
                        }
                    }
                } else {
                    activity.nextResource(false)
                }
            } else {
                if (!this@R2BasicWebView.canScrollHorizontally(1)) {
                    activity.nextResource(false)
                }
                this@R2BasicWebView.evaluateJavascript("scrollRight();", null)
            }
        }

    }

    @android.webkit.JavascriptInterface
    open fun scrollLeft() {

        println("                       DEBUG_INFORMATIONS: "+ "Previous Page Asked")
        uiScope.launch {
            if (activity.supportActionBar!!.isShowing && activity.allowToggleActionBar) {
                activity.resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            }
            val scrollMode = activity.preferences.getBoolean(SCROLL_REF, false)
            if (scrollMode) {
                if (activity.publication.metadata.direction == "rtl") {
                    this@R2BasicWebView.evaluateJavascript("scrollLeftRTL();") { result ->
                        if (result.contains("edge")) {
                            activity.nextResource(false)
                        }
                    }
                } else {
                    activity.previousResource(false)
                }
            } else {
                // fix this for when vertical scrolling is enabled
                if (!this@R2BasicWebView.canScrollHorizontally(-1)) {
                    activity.previousResource(false)
                }
                this@R2BasicWebView.evaluateJavascript("scrollLeft();", null)
            }
        }
    }

    @android.webkit.JavascriptInterface
    fun progressionDidChange(positionString: String) {
        progression = positionString.toDouble()
        activity.storeProgression(Locations(progression = progression))
    }

    @android.webkit.JavascriptInterface
    fun centerTapped() {
        activity.toggleActionBar()
    }

    @android.webkit.JavascriptInterface
    fun handleClick(html: String) {
        val doc = Jsoup.parse(html)
        val link = doc.select("a[epub:type=noteref]")?.first()
        link?.let { noteref ->
            val href = noteref.attr("href")
            if (href.indexOf("#") > 0) {
                val id = href.substring(href.indexOf('#')+1)
                var absolute = getAbsolute(href, resourceUrl!!)
                absolute = absolute.substring(0, absolute.indexOf("#"))
                val document = Jsoup.connect(absolute).get()
                val aside = document.select("aside#$id").first()?.html()
                aside?.let {
                    val safe = Jsoup.clean(aside, Whitelist.relaxed())

                    // Initialize a new instance of LayoutInflater service
                    val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                    // Inflate the custom layout/view
                    val customView = inflater.inflate(R.layout.popup_footnote, null)

                    // Initialize a new instance of popup window
                    val mPopupWindow = PopupWindow(
                            customView,
                            ListPopupWindow.WRAP_CONTENT,
                            ListPopupWindow.WRAP_CONTENT
                    )
                    mPopupWindow.isOutsideTouchable = true
                    mPopupWindow.isFocusable = true

                    // Set an elevation value for popup window
                    // Call requires API level 21
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mPopupWindow.elevation = 5.0f
                    }

                    val textView = customView.findViewById(R.id.footnote) as TextView
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textView.text = Html.fromHtml(safe, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        textView.text = Html.fromHtml(safe)
                    }

                    // Get a reference for the custom view close button
                    val closeButton = customView.findViewById(R.id.ib_close) as ImageButton

                    // Set a click listener for the popup window close button
                    closeButton.setOnClickListener {
                        // Dismiss the popup window
                        mPopupWindow.dismiss()
                    }

                    // Finally, show the popup window at the center location of root relative layout
                    mPopupWindow.showAtLocation(this, Gravity.CENTER, 0, 0)

                    overrideUrlLoading = false
                }
            }
        }
    }

    fun Boolean.toInt() = if (this) 1 else 0

    fun scrollToStart() {
        this.evaluateJavascript("scrollToStart();", null)
    }

    fun scrollToEnd() {
        this.evaluateJavascript("scrollToEnd();", null)
    }

    fun scrollToPosition(progression: Double) {
        this.evaluateJavascript("scrollToPosition(\"$progression\", \"${activity.publication.metadata.direction}\");", null)
    }

    fun setProperty(key: String, value: String) {
        this.evaluateJavascript("setProperty(\"$key\", \"$value\");", null)
    }

    fun removeProperty(key: String) {
        this.evaluateJavascript("removeProperty(\"$key\");", null)
    }

    fun getHTML() : String {
        //val latch = CountDownLatch(1)
        var result = ""
        this.evaluateJavascript("getHTML();") { value ->
                result = value
            Log.d("TEST : 1", result)
                //latch.countDown()
        }

        //latch.await()
        return result
    }


    fun tmp(): String {
        var sum: String = ""
        runBlocking {
            val jobA = async { getHTML() }
            runBlocking{
                sum = jobA.await()
            }
        }
        return sum
    }

    fun searchKeyword(keyword: String, document: String, resourceId: String) {
        //var jsonObject = JSONObject(document)

        //Replaceing double quotes
        var tmp = document.replace("\"", "\\%")
        //Replacing single quotes
        tmp = tmp.replace("\'", "\\$")

        this.evaluateJavascript("performMark('$keyword', '$tmp', '$resourceId')", null)
        Log.d("Search : ", "Search function performed")
    }

    fun searchKeywordInCurrentResource(resource: String, keyword: String, focusPosition: Int) {
        this.evaluateJavascript("performMarkInCurrentResource('$resource','$keyword', $focusPosition)", null)
    }

    fun previousWord() {
        this.evaluateJavascript("moveToPreviousWord()", null)
    }
    fun nextWord() {
        this.evaluateJavascript("moveToNextWord()", null)
    }

    fun cancel() {
        this.evaluateJavascript("cancel()", null)
    }




}