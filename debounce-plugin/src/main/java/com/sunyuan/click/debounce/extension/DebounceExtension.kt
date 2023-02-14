package com.sunyuan.click.debounce.extension

import com.sunyuan.click.debounce.utils.HookManager
import com.sunyuan.click.debounce.utils.LogUtil
import groovy.json.DefaultJsonGenerator
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher


/**
 * author : Sy007
 * date   : 2020/12/1
 * desc   : 提供给外部配置
 * version: 1.0
 */
open class DebounceExtension {
    var generateReport: Boolean = false
    var includes: MutableSet<String> = mutableSetOf()
    var excludes: MutableSet<String> = mutableSetOf()
    var excludeForMethodAnnotation: MutableSet<String> = mutableSetOf()
    var proxyClassName: String = ""

    private lateinit var excludeGlobPathMatcher: MutableSet<PathMatcher>
    private lateinit var includeGlobPathMatcher: MutableSet<PathMatcher>

    fun init() {
        if (proxyClassName.isEmpty()) {
            throw IllegalArgumentException("proxyClassName cannot be empty")
        }
        excludeForMethodAnnotation.add(HookManager.sIgnoreClickDeBounceDesc)
        includeGlobPathMatcher = includes.toPathMatchers()
        excludeGlobPathMatcher = excludes.toPathMatchers()
        print()
    }


    fun matchClassPath(
        canonicalName: String,
    ): Boolean {
        val path = FileSystems.getDefault().getPath(canonicalName)
        if (excludeGlobPathMatcher.isNotEmpty() && matchClassPath(path, excludeGlobPathMatcher)) {
            return false
        }
        return includeGlobPathMatcher.isEmpty() || matchClassPath(path, includeGlobPathMatcher)
    }

    private fun print() {
        LogUtil.warn("------------------debounce plugin config info--------------------")
        val configMap: LinkedHashMap<String, Any?> = LinkedHashMap()
        configMap["generateReport"] = generateReport
        configMap["proxyClassName"] = proxyClassName
        configMap["includes"] = includes
        configMap["excludes"] = excludes
        configMap["excludeForMethodAnnotation"] = excludeForMethodAnnotation
        val jsonGenerator = object : DefaultJsonGenerator(JsonGenerator.Options().apply {
            excludeNulls()
        }) {}
        val configJson = JsonBuilder(configMap, jsonGenerator).toPrettyString()
        LogUtil.warn(configJson)
        LogUtil.warn("-----------------------------------------------------------------")
    }


    companion object {
        private const val GLOB_SYNTAX = "glob:"

        private fun Set<String>.toPathMatchers(): MutableSet<PathMatcher> {
            val paths = this
            val matchers = mutableSetOf<PathMatcher>()
            if (paths.isEmpty()) {
                return matchers
            }
            for (path in paths) {
                try {
                    val fs = FileSystems.getDefault()
                    val matcher = fs.getPathMatcher("$GLOB_SYNTAX$path")
                    matchers.add(matcher)
                } catch (e: IllegalArgumentException) {
                    LogUtil.error(
                        String.format(
                            "Ignoring relativePath '{%s}' glob pattern.Because something unusual happened here '{%s}'",
                            path,
                            e
                        )
                    )
                }
            }
            return matchers
        }


        private fun matchClassPath(path: Path, matchers: Set<PathMatcher>): Boolean {
            for (matcher in matchers) {
                if (matcher.matches(path)) {
                    return true
                }
            }
            return false
        }
    }
}

