package com.sunyuan.click.debounce.utils

import com.sunyuan.click.debounce.entity.ProxyClassEntity
import com.sunyuan.click.debounce.visitor.ProxyClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes

/**
 * @author sy007
 * @date 2023/07/12
 * @description
 */
object ProxyClassParserUtil {

    fun parse(
        urlClassLoader: ClassLoader,
        proxyClassName: String
    ): ProxyClassEntity? {
        val entity = ProxyClassEntity()
        urlClassLoader.getResourceAsStream(
            "${proxyClassName.replace(".", "/")}.class"
        )?.use { ins ->
            ins.readBytes()
        }?.run {
            ClassReader(this)
        }?.accept(ProxyClassVisitor(entity), 0)
        if (entity.owner.isEmpty()) {
            LogUtil.warn("failed to parse proxy class,${proxyClassName} create or not?")
            return null
        }
        entity.print()
        return entity
    }
}