package com.sunyuan.click.debounce.extension

import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.utils.ConfigUtil
import com.sunyuan.click.debounce.utils.LogUtil
import groovy.json.DefaultJsonGenerator
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher


/**
 * author : Sy007
 * date   : 2020/12/1
 * desc   : 提供给外部配置
 * version: 1.0
 */
open class DebounceExtension(project: Project) {
    val methodEntities: NamedDomainObjectContainer<MethodEntity> =
        project.container(MethodEntity::class.java)

    @JvmField
    var isDebug: Boolean = false
    var generateReport: Boolean = false
    var checkTime: Long = 1000L
    var includes: MutableSet<String> = mutableSetOf()
    var excludes: MutableSet<String> = mutableSetOf()
    var includeForMethodAnnotation: MutableSet<String> = mutableSetOf()
    var excludeForMethodAnnotation: MutableSet<String> = mutableSetOf()


    internal val hookMethodEntities = mutableMapOf<String, MethodEntity>().apply {
        put("onClick(Landroid/view/View;)V", MethodEntity("onClick").apply {
            methodName = "onClick"
            methodDesc = "(Landroid/view/View;)V"
            interfaceName = "android/view/View\$OnClickListener"
        })
    }
    internal val hookInterfaces = mutableSetOf<String>()


    fun init() {
        val methodEntitiesAsMap = methodEntities.asMap
        methodEntitiesAsMap.forEach { (identification: String, methodEntity: MethodEntity) ->
            require(methodEntity.methodName.isNotEmpty()) {
                String.format(
                    methodEntityEx,
                    "methodEntities",
                    "methodName",
                    identification
                )
            }
            require(methodEntity.methodDesc.isNotEmpty()) {
                String.format(
                    methodEntityEx,
                    "methodEntities",
                    "methodDesc",
                    identification
                )
            }
            require(methodEntity.interfaceName.isNotEmpty()) {
                String.format(
                    methodEntityEx,
                    "methodEntities",
                    "interfaceName",
                    identification
                )
            }
            hookMethodEntities[methodEntity.methodName + methodEntity.methodDesc] = methodEntity
        }
        hookMethodEntities.forEach { (_: String, methodEntity: MethodEntity) ->
            val interfaceName = methodEntity.interfaceName
            hookInterfaces.add(interfaceName)
        }
        includeForMethodAnnotation.add("Lcom/sunyuan/debounce/lib/ClickDeBounce;")
        excludeForMethodAnnotation.add("Lcom/sunyuan/debounce/lib/IgnoreClickDeBounce;")
        printlnConfigInfo()
    }

    internal fun clear() {
        includeGlobPathMatcher?.clear()
        includeGlobPathMatcher = null
        excludeGlobPathMatcher?.clear()
        excludeGlobPathMatcher = null
    }

    private fun printlnConfigInfo() {
        LogUtil.warn("------------------debounce plugin config info--------------------")
        val configMap: LinkedHashMap<String, Any?> = LinkedHashMap()
        configMap["isDebug"] = isDebug
        configMap["generateReport"] = generateReport
        configMap["checkTime"] = checkTime
        configMap["includes"] = includes
        configMap["excludes"] = excludes
        configMap["includeForMethodAnnotation"] = includeForMethodAnnotation
        configMap["excludeForMethodAnnotation"] = excludeForMethodAnnotation
        configMap["methodEntities"] = getPrintMethodEntities()
        val jsonGenerator = object : DefaultJsonGenerator(JsonGenerator.Options().apply {
            excludeNulls()
            excludeFieldsByName("access", "name")
        }) {}
        val configJson = JsonBuilder(configMap, jsonGenerator).toPrettyString()
        LogUtil.warn(configJson)
        LogUtil.warn("-----------------------------------------------------------------")
    }


    private fun getPrintMethodEntities(): LinkedHashMap<String, MethodEntity> {
        val printMethodEntities = linkedMapOf<String, MethodEntity>()
        hookMethodEntities.forEach {
            val key = it.value.name
            printMethodEntities[key] = it.value
        }
        return printMethodEntities
    }


    companion object {
        private const val GLOB_SYNTAX = "glob:"
        private const val methodEntityEx = "In %s,the %s of %s cannot be empty."
        internal var excludeGlobPathMatcher: MutableSet<PathMatcher>? = null
        internal var includeGlobPathMatcher: MutableSet<PathMatcher>? = null

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


        fun matchClassPath(
            canonicalName: String,
            includes: Set<String>,
            excludes: Set<String>
        ): Boolean {
            if (ConfigUtil.sOwnerClassPath == canonicalName) {
                return true
            }
            if (includeGlobPathMatcher == null) {
                includeGlobPathMatcher = includes.toPathMatchers()
            }
            if (excludeGlobPathMatcher == null) {
                excludeGlobPathMatcher = excludes.toPathMatchers()
            }
            val path = FileSystems.getDefault().getPath(canonicalName)
            if (excludeGlobPathMatcher!!.isNotEmpty() && matchClassPath(
                    path,
                    excludeGlobPathMatcher!!
                )
            ) {
                return false
            }
            return includeGlobPathMatcher!!.isEmpty() || matchClassPath(
                path,
                includeGlobPathMatcher!!
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
    }
}

