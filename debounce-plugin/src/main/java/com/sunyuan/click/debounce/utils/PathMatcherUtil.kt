package com.sunyuan.click.debounce.utils

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher

/**
 * @author sy007
 * @date 2023/07/09
 * @description
 */
object PathMatcherUtil {
    private const val GLOB_SYNTAX = "glob:"
    private val includeGlobPathMatcher: MutableSet<PathMatcher> = mutableSetOf()
    private val excludeGlobPathMatcher: MutableSet<PathMatcher> = mutableSetOf()

    private fun Set<String>.toPathMatchers(): MutableSet<PathMatcher> {
        val paths = this
        val matchers = mutableSetOf<PathMatcher>()
        if (paths.isEmpty()) {
            return matchers
        }
        for (path in paths) {
            try {
                val fs = FileSystems.getDefault()
                val matcher = fs.getPathMatcher("${GLOB_SYNTAX}$path")
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

    fun matchClassPath(
        canonicalName: String,
    ): Boolean {
        val path = FileSystems.getDefault().getPath(canonicalName)
        if (excludeGlobPathMatcher.isNotEmpty() && matchClassPath(
                path,
                excludeGlobPathMatcher
            )
        ) {
            return false
        }
        return includeGlobPathMatcher.isEmpty() || matchClassPath(
            path,
            includeGlobPathMatcher
        )
    }

    private fun matchClassPath(path: Path, matchers: Set<PathMatcher>): Boolean {
        for (matcher in matchers) {
            if (matcher.matches(path)) {
                return true
            }
        }
        return false
    }

    fun init(includes: Set<String>, excludes: Set<String>) {
        includeGlobPathMatcher.clear()
        excludeGlobPathMatcher.clear()
        includeGlobPathMatcher.addAll(includes.toPathMatchers())
        excludeGlobPathMatcher.addAll(excludes.toPathMatchers())
    }
}