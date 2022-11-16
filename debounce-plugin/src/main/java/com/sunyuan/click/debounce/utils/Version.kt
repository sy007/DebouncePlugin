package com.sunyuan.click.debounce.utils

import com.android.builder.model.Version
import com.android.repository.Revision

/**
 * @author sy007
 * @date 2022/11/13
 * @description
 */
object Version {
    private val version: Revision by lazy {
        Revision.parseRevision(Version.ANDROID_GRADLE_PLUGIN_VERSION)
    }
    val V7_X: Boolean by lazy { version.major >= 7 }
}