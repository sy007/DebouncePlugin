package com.sunyuan.click.debounce.utils

import com.android.builder.model.Version
import com.android.repository.Revision

/**
 * @author sy007
 * @date 2022/11/13
 * @description
 */
object VersionUtil {
    private val version: Revision by lazy {
        Revision.parseRevision(Version.ANDROID_GRADLE_PLUGIN_VERSION)
    }
    val V7_4: Boolean by lazy {
        LogUtil.warn("current AGP version => ${version.toShortString()}")
        version.major >= 7 || version.major >= 7 && version.minor >= 4
    }
}