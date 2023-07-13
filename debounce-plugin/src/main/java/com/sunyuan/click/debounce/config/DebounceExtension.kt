package com.sunyuan.click.debounce.config

import com.sunyuan.click.debounce.utils.LogUtil
import com.sunyuan.click.debounce.utils.PathMatcherUtil
import groovy.json.DefaultJsonGenerator
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject


/**
 * author : Sy007
 * date   : 2020/12/1
 * desc   : 提供给外部配置
 * version: 1.0
 */
open class DebounceExtension @Inject constructor(objects: ObjectFactory) {

    var generateReport: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    var includes: SetProperty<String> = objects.setProperty(String::class.java)
        .convention(emptySet())
    var excludes: SetProperty<String> = objects.setProperty(String::class.java)
        .convention(emptySet())
    var excludeForMethodAnnotation: SetProperty<String> = objects.setProperty(String::class.java)
        .convention(emptySet())

    var proxyClassName: Property<String> = objects.property(String::class.java).convention("")


    fun init() {
        if (proxyClassName.get().isEmpty()) {
            throw IllegalArgumentException("proxyClassName cannot be empty")
        }
        excludeForMethodAnnotation.add(Const.sIgnoreClickDeBounceDesc)
        PathMatcherUtil.init(includes.get(), excludes.get())
        print()
    }


    private fun print() {
        LogUtil.warn("------------------debounce plugin config info--------------------")
        val configMap: LinkedHashMap<String, Any?> = LinkedHashMap()
        configMap["generateReport"] = generateReport.get()
        configMap["proxyClassName"] = proxyClassName.get()
        configMap["includes"] = includes.get()
        configMap["excludes"] = excludes.get()
        configMap["excludeForMethodAnnotation"] = excludeForMethodAnnotation.get()
        val jsonGenerator = object : DefaultJsonGenerator(JsonGenerator.Options().apply {
            excludeNulls()
        }) {}
        val configJson = JsonBuilder(configMap, jsonGenerator).toPrettyString()
        LogUtil.warn(configJson)
        LogUtil.warn("-----------------------------------------------------------------")
    }

}

