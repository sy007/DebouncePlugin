package com.sunyuan.click.debounce.utils

import org.objectweb.asm.ClassReader
import java.io.IOException

/**
 * author : Six
 * date   : 2021/1/13 001319:47
 * desc   :
 * version: 1.0
 */
class FindInterfaceImplHelper {
    private lateinit var urlClassLoader: ClassLoader

    fun setUrlClassLoader(urlClassLoader: ClassLoader) {
        this.urlClassLoader = urlClassLoader
    }

    /**
     * 查找直接或间接实现目标接口的接口
     *
     * @param className             当前类
     * @param targetInterfaces      目标接口
     * @param collectImplInterfaces 记录直接或间接实现目标接口的接口
     * 记录结果放入collectImplInterfaces中
     */
    fun findTargetInterfaceImpl(
        className: String,
        targetInterfaces: Set<String>,
        collectImplInterfaces: MutableSet<String>
    ) {
        if (isObject(className) || targetInterfaces.size == collectImplInterfaces.size) {
            return
        }
        val reader = getClassReader(className) ?: return
        matchTargetInterface(reader.interfaces, targetInterfaces, collectImplInterfaces)
        findTargetInterfaceImpl(reader.superName, targetInterfaces, collectImplInterfaces)
    }

    /**
     * 匹配目标接口，将匹配结果放入recordImplInterfaceSet中
     * @param interfaces             待检查接口
     * @param targetInterfaceSet     目标接口
     * @param recordImplInterfaceSet 匹配结果
     */
    private fun matchTargetInterface(
        interfaces: Array<String>,
        targetInterfaceSet: Set<String>,
        recordImplInterfaceSet: MutableSet<String>
    ) {
        if (interfaces.isEmpty() || targetInterfaceSet.size == recordImplInterfaceSet.size) {
            return
        }
        for (inter in interfaces) {
            if (targetInterfaceSet.contains(inter)) {
                recordImplInterfaceSet.add(inter)
            } else {
                val reader = getClassReader(inter) ?: return
                matchTargetInterface(reader.interfaces, targetInterfaceSet, recordImplInterfaceSet)
            }
        }
    }

    private fun getClassReader(className: String): ClassReader? {
        val inputStream = urlClassLoader.getResourceAsStream("$className.class")
        try {
            if (inputStream != null) {
                return ClassReader(inputStream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    companion object {
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
    }
}