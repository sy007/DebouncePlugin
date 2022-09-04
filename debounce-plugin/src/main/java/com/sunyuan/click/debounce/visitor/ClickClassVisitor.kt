package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.utils.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   :
 * version: 1.0
 */
class ClickClassVisitor(cv: ClassVisitor) :
    ClassVisitor(Opcodes.ASM7, cv) {

    private var mOwner: String? = null
    private var mFindInjectClassName = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        if (name == ConfigUtil.sOwner) {
            mFindInjectClassName = true
        }
        val collectMethods = MethodUtil.sModifyOfMethods[name]
        if (!collectMethods.isNullOrEmpty()) {
            mOwner = name
            if (isVisitInvokeSpecialField(collectMethods)) {
                //为当前类创建实例属性
                val visitField = cv.visitField(
                    Opcodes.ACC_PRIVATE,
                    ConfigUtil.sFieldName,
                    ConfigUtil.sFieldDesc,
                    null,
                    null
                )
                visitField.visitEnd()
            }
            if (isVisitInvokeStaticField(collectMethods)) {
                //为当前类创建静态属性
                val visitField = cv.visitField(
                    Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                    ConfigUtil.sStaticFieldName,
                    ConfigUtil.sFieldDesc, null, null
                )
                visitField.visitEnd()
            }
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }


    private fun isVisitInvokeStaticField(
        collectMethods: Map<String, MethodEntity>
    ): Boolean {
        return kotlin.run {
            collectMethods.forEach {
                if (it.value.access == Opcodes.H_INVOKESTATIC) {
                    return@run true
                }
            }
            return@run false
        }
    }

    private fun isVisitInvokeSpecialField(
        collectMethods: Map<String, MethodEntity>
    ): Boolean {
        return kotlin.run {
            collectMethods.forEach {
                if (it.value.access == -1 || it.value.access == Opcodes.H_INVOKESPECIAL) {
                    return@run true
                }
            }
            return@run false
        }
    }

    override fun visitMethod(
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (mFindInjectClassName && name == "<clinit>") {
            val methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
            return BounceCheckerClinitVisitor(
                ConfigUtil.sDebug,
                ConfigUtil.sCheckTime,
                methodVisitor
            )
        }
        val owner = mOwner ?: return super.visitMethod(access, name, desc, signature, exceptions)
        //处理需要修改的Lambda方法
        val collectMethods = MethodUtil.sModifyOfMethods[owner]
        collectMethods?.forEach {
            if (name == it.value.methodName && desc == it.value.methodDesc) {
                return when (it.value.access) {
                    Opcodes.H_INVOKESTATIC -> {
                        //non-instance-capturing lambdas
                        val methodVisitor =
                            cv.visitMethod(access, name, desc, signature, exceptions)
                        StaticClickMethodVisitor(
                            owner,
                            methodVisitor,
                            access,
                            name,
                            desc
                        )
                    }
                    else -> {
                        //instance-capturing lambda or method of impl Interface
                        val methodVisitor =
                            cv.visitMethod(access, name, desc, signature, exceptions)
                        NoStaticClickMethodVisitor(
                            owner,
                            methodVisitor,
                            access,
                            name,
                            desc
                        )
                    }
                }
            }
        }
        return super.visitMethod(access, name, desc, signature, exceptions)
    }
}