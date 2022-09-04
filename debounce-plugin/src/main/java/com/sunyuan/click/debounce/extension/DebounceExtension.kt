package com.sunyuan.click.debounce.extension

import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.toPathMatchers
import com.sunyuan.click.debounce.utils.ConfigUtil
import com.sunyuan.click.debounce.utils.LogUtil
import groovy.json.JsonBuilder
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
        configMap["methodEntities"] = methodEntities.asMap
        val configJson = JsonBuilder(configMap).toPrettyString()
        LogUtil.warn(configJson)
        LogUtil.warn("-----------------------------------------------------------------")
    }
}

