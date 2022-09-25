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

    companion object {
        private const val GLOB_SYNTAX = "glob:"
        private const val methodEntityEx = "In %s,the %s of %s cannot be empty."
    }

    private lateinit var includeGlobPathMatcher: MutableSet<PathMatcher>
    private lateinit var excludeGlobPathMatcher: MutableSet<PathMatcher>

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
            ConfigUtil.sHookMethods[methodEntity.methodName + methodEntity.methodDesc] =
                methodEntity
        }
        ConfigUtil.sHookMethods.forEach { (_: String, methodEntity: MethodEntity) ->
            val interfaceName = methodEntity.interfaceName
            ConfigUtil.sInterfaceSet.add(interfaceName)
        }
        includeGlobPathMatcher = includes.toPathMatchers()
        excludeGlobPathMatcher = excludes.toPathMatchers()

        includeForMethodAnnotation.add("Lcom/sunyuan/debounce/lib/ClickDeBounce;")
        excludeForMethodAnnotation.add("Lcom/sunyuan/debounce/lib/IgnoreClickDeBounce;")
    }

    fun excludeMethodOfAnnotation(annotation: String): Boolean =
        excludeForMethodAnnotation.contains(annotation)

    fun includeMethodOfAnnotation(annotation: String): Boolean =
        includeForMethodAnnotation.contains(annotation)

    fun matchClassPath(classPath: String): Boolean {
        val path = FileSystems.getDefault().getPath(classPath)
        if (excludeGlobPathMatcher.isNotEmpty() && matchClassPath(path, excludeGlobPathMatcher)) {
            return false
        }
        return includeGlobPathMatcher.isEmpty() || matchClassPath(path, includeGlobPathMatcher)
    }

    private fun matchClassPath(path: Path, matchers: Set<PathMatcher>): Boolean {
        for (matcher in matchers) {
            if (matcher.matches(path)) {
                return true
            }
        }
        return false
    }


    fun printlnConfigInfo() {
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
        ConfigUtil.sHookMethods.forEach {
            val key = it.value.name
            printMethodEntities[key] = it.value
        }
        return printMethodEntities
    }

    private fun Set<String>.toPathMatchers(): MutableSet<PathMatcher> {
        val paths = this
        val matchers = mutableSetOf<PathMatcher>()
        if (paths.isEmpty()) {
            return matchers
        }
        for (path in paths) {
            try {
                val fs = FileSystems.getDefault()
                val matcher = fs.getPathMatcher(GLOB_SYNTAX + path)
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
}

