package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.entity.LambdaMethodEntity
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
    ClassVisitor(Opcodes.ASM6, cv) {

    private var mOwner: String? = null
    private var mFindInjectClassName = false

    //hook info collect
    private var mCollectClassName: String? = null
    private val mCollectField: MutableList<String>? = if (ConfigUtil.sDebug) {
        mutableListOf()
    } else {
        null
    }
    private val mCollectMethod: MutableList<String>? = if (ConfigUtil.sDebug) {
        mutableListOf()
    } else {
        null
    }


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
        val collectDefaultMethods = MethodUtil.sCollectDefaultMethods[name]
        val lambdaMethodEntityList = MethodUtil.sCollectLambdaMethods[name]
        if (!CollectionUtil.isEmpty(collectDefaultMethods) || !CollectionUtil.isEmpty(
                lambdaMethodEntityList
            )
        ) {
            mOwner = name
            if (isVisitField(collectDefaultMethods, lambdaMethodEntityList)) {
                //为当前类创建属性
                val visitField = cv.visitField(
                    Opcodes.ACC_PRIVATE,
                    ConfigUtil.sFieldName,
                    ConfigUtil.sFieldDesc,
                    null,
                    null
                )
                visitField.visitEnd()
            }
        }
        if (ConfigUtil.sDebug && (mFindInjectClassName || mOwner != null)) {
            mCollectClassName = "className:${name.replace("/", ".")}"
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    private fun isVisitField(
        collectDefaultMethods: Map<String, MethodEntity>?,
        lambdaMethods: Map<String, LambdaMethodEntity>?
    ): Boolean {
        return !CollectionUtil.isEmpty(collectDefaultMethods) || kotlin.run {
            lambdaMethods?.forEach {
                if (it.value.tag == Opcodes.H_INVOKESPECIAL) {
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
            mCollectField?.add("${ConfigUtil.sIsDebugFieldName} modified to ${ConfigUtil.sDebug}")
            mCollectField?.add("${ConfigUtil.sDebounceCheckTimeFieldName} modified to ${ConfigUtil.sDebounceCheckTime}")
            val methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
            return DoubleCheckClinitVisitor(
                ConfigUtil.sDebug,
                ConfigUtil.sDebounceCheckTime,
                methodVisitor
            )
        }
        val owner = mOwner ?: return super.visitMethod(access, name, desc, signature, exceptions)
        //处理需要Hook的Lambda方法
        val lambdaMethodEntityList = MethodUtil.sCollectLambdaMethods[owner]
        lambdaMethodEntityList?.forEach {
            if (name == it.value.methodName && desc == it.value.methodDesc) {
                return when (it.value.tag) {
                    Opcodes.H_INVOKESTATIC -> {
                        //non-instance-capturing lambdas
                        mCollectMethod?.add(name + desc)
                        val methodVisitor =
                            cv.visitMethod(access, name, desc, signature, exceptions)
                        LambdaStaticClickMethodVisitor(
                            methodVisitor,
                            access,
                            name,
                            desc
                        )
                    }
                    Opcodes.H_INVOKESPECIAL -> {
                        //instance-capturing lambda
                        mCollectMethod?.add(name + desc)
                        val methodVisitor =
                            cv.visitMethod(access, name, desc, signature, exceptions)
                        ClickMethodVisitor(
                            owner,
                            methodVisitor,
                            access,
                            name,
                            desc
                        )
                    }
                    else -> {
                        LogUtil.warning(
                            String.format(
                                "An unknown '%s' lambda was captured in '%s'.",
                                it.value.methodName + it.value.methodDesc,
                                owner
                            )
                        )
                        super.visitMethod(access, name, desc, signature, exceptions)
                    }
                }
            }
        }
        //处理其他需要Hook的方法
        val collectDefaultMethods = MethodUtil.sCollectDefaultMethods[mOwner]
        if (CollectionUtil.isEmpty(collectDefaultMethods)) {
            return super.visitMethod(access, name, desc, signature, exceptions)
        }
        val methodEntity = collectDefaultMethods!![name + desc]
        if (methodEntity != null) {
            mCollectMethod?.add(name + desc)
            val methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
            return ClickMethodVisitor(
                owner,
                methodVisitor,
                access,
                name,
                desc
            )
        }
        return super.visitMethod(access, name, desc, signature, exceptions)
    }


    override fun visitEnd() {
        super.visitEnd()
        LogUtil.printlnHookInfo(mCollectClassName, mCollectField, mCollectMethod)
    }
}