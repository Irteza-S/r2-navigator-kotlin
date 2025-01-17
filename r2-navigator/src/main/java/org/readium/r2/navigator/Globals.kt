/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import org.readium.r2.shared.Publication

/**
 * Created by aferditamuriqi on 10/3/17.
 */


var keywordGlobal = ""
var focusPosition = 0
var resourceGlobal = ""
var currentActivity : R2EpubActivity? = null
var globalPublication : Publication? = null

/**
 * Global Parameters
 */
//val PORT_NUMBER = 3333
const val BASE_URL = "http://127.0.0.1"
//val SERVER_URL = "$BASE_URL:$PORT_NUMBER"
//val MANIFEST = "/manifest"


/**
 * Extensions
 */

@ColorInt
fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}