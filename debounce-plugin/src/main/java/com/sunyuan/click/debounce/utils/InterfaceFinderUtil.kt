package com.sunyuan.click.debounce.utils

import org.objectweb.asm.ClassReader

/**
 * author : Six
 * date   : 2021/1/13 001319:47
 * desc   :
 * version: 1.0
 */
object InterfaceFinderUtil {

    private const val OBJECT = "java/lang/Object"

    /**
     * 检查当前类是 Object 类型
     *
     * @param className class name
     * @return checked result
     */
    private fun isObject(className: String): Boolean {
        return OBJECT == className
    }

    private lateinit var classLoader: ClassLoader

    fun setUrlClassLoader(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    /**
     * 查找直接或间接实现目标接口的接口
     *
     * @param className             当前类
     * @param targetInterfaces      目标接口
     * @param implTargetInterfaces  记录直接或间接实现目标接口的接口
     */
    fun find(
        className: String?,
        targetInterfaces: Set<String>,
        implTargetInterfaces: MutableSet<String>
    ) {
        if (className.isNullOrEmpty() || isObject(className) || targetInterfaces.size == implTargetInterfaces.size) {
            return
        }
        val reader = getClassReader(className) ?: return
        matchTargetInterface(reader.interfaces, targetInterfaces, implTargetInterfaces)
        find(reader.superName, targetInterfaces, implTargetInterfaces)
    }

    /**
     * 匹配目标接口，将匹配结果放入recordImplInterfaceSet中
     * @param interfaces             待检查接口
     * @param targetInterfaceSet     目标接口
     * @param implTargetInterfaces   记录直接或间接实现目标接口的接口
     */
    private fun matchTargetInterface(
        interfaces: Array<String>,
        targetInterfaceSet: Set<String>,
        implTargetInterfaces: MutableSet<String>
    ) {
        if (interfaces.isEmpty() || targetInterfaceSet.size == implTargetInterfaces.size) {
            return
        }
        for (inter in interfaces) {
            if (targetInterfaceSet.contains(inter)) {
                implTargetInterfaces.add(inter)
            } else {
                val reader = getClassReader(inter) ?: return
                matchTargetInterface(reader.interfaces, targetInterfaceSet, implTargetInterfaces)
            }
        }
    }

    private fun getClassReader(className: String): ClassReader? {
        return classLoader.getResourceAsStream("$className.class")?.use {
            it.readBytes()
        }?.run {
            ClassReader(this)
        }
    }
}