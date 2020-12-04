package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.entity.LambdaMethodEntity
import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.utils.ConfigUtil
import com.sunyuan.click.debounce.utils.LogUtil
import com.sunyuan.click.debounce.utils.MethodUtil
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

    private var mInterfaces: Array<out String>? = null
    private var mOwner: String? = null
    private var mIsVisitField = false
    private var mFindInjectClassName = false
    private var mVisitInit = false

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
        val matchNormalClickMethod = isMatchNormalClickMethod(interfaces);
        val lambdaMethodEntityList = MethodUtil.sLambdaMethods[name]
        if (!lambdaMethodEntityList.isNullOrEmpty() || matchNormalClickMethod) {
            mOwner = name
        }
        if (isVisitField(lambdaMethodEntityList, matchNormalClickMethod)) {
            mIsVisitField = true
            if (matchNormalClickMethod) {
                mInterfaces = interfaces
            }
            //为当前类创建属性
            val visitField = cv.visitField(
                Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL,
                ConfigUtil.sFieldName,
                ConfigUtil.sFieldDesc,
                null,
                null
            )
            visitField.visitEnd()
        }
        if (ConfigUtil.sDebug && (mFindInjectClassName || mOwner != null)) {
            mCollectClassName = "className:${name.replace("/", ".")}"
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    private fun isVisitField(
        lambdaMethodEntityList: List<LambdaMethodEntity>?,
        matchNormalClickMethod: Boolean
    ): Boolean {
        return matchNormalClickMethod || kotlin.run {
            lambdaMethodEntityList?.forEach {
                if (it.tag == Opcodes.H_INVOKESPECIAL) {
                    return@run true
                }
            }
            return@run false
        }
    }

    private fun isMatchNormalClickMethod(interfaces: Array<out String>?): Boolean {
        if (!interfaces.isNullOrEmpty()) {
            val iterator = MethodUtil.sNormalMethods.iterator()
            while (iterator.hasNext()) {
                val methodEntity: MethodEntity? = iterator.next().value
                if (methodEntity != null && interfaces.contains(methodEntity.interfaceName)) {
                    return true
                }
            }
        }
        return false
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
        if (mIsVisitField && !mVisitInit && name == "<init>") {
            mVisitInit = true
            mCollectMethod?.add(name + desc)
            val methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
            return InitVisitor(
                owner,
                methodVisitor
            )
        }
        //处理需要Hook的Lambda方法
        val lambdaMethodEntityList = MethodUtil.sLambdaMethods[owner]
        lambdaMethodEntityList?.forEach {
            if (it != null && name == it.methodName && desc == it.methodDesc) {
                return when (it.tag) {
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
                                it.methodName + it.methodDesc,
                                owner
                            )
                        )
                        super.visitMethod(access, name, desc, signature, exceptions)
                    }
                }
            }
        }
        //处理其他需要Hook的方法
        val interfaces = mInterfaces
        if (interfaces.isNullOrEmpty()) {
            return super.visitMethod(access, name, desc, signature, exceptions)
        }
        val methodEntity: MethodEntity? = MethodUtil.sNormalMethods[name + desc]
        if (methodEntity != null && interfaces.contains(methodEntity.interfaceName)) {
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