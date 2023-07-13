package com.sunyuan.click.debounce.entity

import com.sunyuan.click.debounce.utils.LogUtil
import groovy.json.DefaultJsonGenerator
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.tasks.Input
import java.io.Serializable


/**
 * @author sy007
 * @date 2023/01/31
 * @description
 */
class ProxyClassEntity : Serializable {

    fun print() {
        LogUtil.warn("------------------proxy class config info--------------------")
        val jsonGenerator = object : DefaultJsonGenerator(JsonGenerator.Options().apply {
            excludeNulls()
            excludeFieldsByName("access")
        }) {}
        val params = linkedMapOf<String, Any>()
        params["owner"] = owner
        params["annotationIndex"] = annotationIndex
        params["methodIndex"] = methodIndex.map {
            val map = mutableMapOf<String, Any>()
            map["samMethodEntity"] = it.key
            map["proxyMethodEntity"] = it.value
            map
        }
        LogUtil.warn(JsonBuilder(params, jsonGenerator).toPrettyString())
        LogUtil.warn("-----------------------------------------------------------------")
    }

    var owner: String = ""

    val methodIndex: MutableMap<MethodEntity, ProxyMethodEntity> = mutableMapOf()

    val annotationIndex: MutableMap<String, ProxyMethodEntity> = mutableMapOf()

    fun findProxyMethodEntity(samMethodEntity: MethodEntity): ProxyMethodEntity? {
        val key = methodIndex.keys.find {
            it == samMethodEntity
        } ?: return null
        return methodIndex[key]
    }
}

class ProxyMethodEntity : Serializable {
    var methodName: String = ""

    var methodDesc: String = ""
}
