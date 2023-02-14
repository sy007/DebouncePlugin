@file:Suppress("DEPRECATION")

package com.sunyuan.click.debounce.utils

import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.entity.MethodMapperEntity
import com.sunyuan.click.debounce.entity.ProxyClassEntity
import com.sunyuan.click.debounce.entity.ProxyMethodEntity
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*


/**
 * @author sy007
 * @date 2022/09/24
 * @description
 */
object ClickMethodModifyUtil {
    var fieldName = "$\$clickProxy"
    var staticFieldName = "$\$sClickProxy"
    var fieldDesc = ""

    fun modify(classNode: ClassNode, proxyClassEntity: ProxyClassEntity) {
        val collectMethods = HookManager.sModifyOfMethods[classNode.name]
        if (collectMethods.isNullOrEmpty()) {
            return
        }
        fieldDesc = "L${proxyClassEntity.owner};"

        if (classNode.fields.find { (it.name == fieldName || it.name == staticFieldName) } != null) {
            LogUtil.warn("${classNode.name} has been modified by debounce-plugin")
            return
        }

        if (isVisitInvokeSpecialField(collectMethods)) {
            //为当前类创建实例属性
            classNode.fields.add(
                FieldNode(
                    Opcodes.ACC_PRIVATE,
                    fieldName, fieldDesc, null, null
                )
            )
        }
        if (isVisitInvokeStaticField(collectMethods)) {
            //为当前类创建静态属性
            classNode.fields.add(
                FieldNode(
                    Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                    staticFieldName, fieldDesc, null, null
                )
            )
        }
        classNode.methods.filter {
            collectMethods.contains(it.name + it.desc)
        }.forEach {
            val mapper = collectMethods[it.name + it.desc]!!
            val insnList = when (mapper.methodEntity.access) {
                Opcodes.H_INVOKESTATIC -> {
                    modifyStaticClickMethod(
                        classNode.name,
                        proxyClassEntity.owner,
                        mapper
                    )
                }
                else -> {
                    modifyClickMethod(
                        classNode.name,
                        proxyClassEntity.owner,
                        mapper
                    )
                }
            }
            it.instructions.insert(insnList)
        }
    }

    /**
     * 在字节码跳转位置后插入FrameNode解决D8 warning [Expected stack map table for method with non-linear control flow]
     */
    private fun modifyStaticClickMethod(
        owner: String,
        proxyOwner: String,
        mapper: MethodMapperEntity
    ): InsnList {
        val insnList = InsnList()
        insnList.add(
            FieldInsnNode(
                Opcodes.GETSTATIC,
                owner,
                staticFieldName,
                fieldDesc
            )
        )
        val label = Label()
        insnList.add(JumpInsnNode(Opcodes.IFNONNULL, LabelNode(label)))
        newProxyClass(insnList, proxyOwner)
        insnList.add(
            FieldInsnNode(
                Opcodes.PUTSTATIC,
                owner,
                staticFieldName,
                fieldDesc
            )
        )
        insnList.add(LabelNode(label))
        insnList.add(FrameNode(Opcodes.F_SAME, 0, null, 0, null))
        val localIndex =
            insnList.createMethodHookParam(owner, mapper.methodEntity, mapper.samMethodEntity, true)
        insnList.add(FieldInsnNode(Opcodes.GETSTATIC, owner, staticFieldName, fieldDesc))
        insnList.invokeProxyMethod(localIndex, proxyOwner, mapper.proxyMethodEntity)
        return insnList
    }

    private fun newProxyClass(insnList: InsnList, proxyOwner: String) {
        insnList.add(TypeInsnNode(Opcodes.NEW, proxyOwner))
        insnList.add(InsnNode(Opcodes.DUP))
        insnList.add(
            MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                proxyOwner,
                "<init>",
                "()V", false
            )
        )
    }

    /**
     * 在字节码跳转位置后插入FrameNode解决D8 warning [Expected stack map table for method with non-linear control flow]
     */
    private fun modifyClickMethod(
        owner: String,
        proxyOwner: String,
        mapper: MethodMapperEntity
    ): InsnList {
        val insnList = InsnList()
        insnList.add(VarInsnNode(Opcodes.ALOAD, 0))
        insnList.add(
            FieldInsnNode(
                Opcodes.GETFIELD,
                owner,
                fieldName,
                fieldDesc
            )
        )
        val label = Label()
        insnList.add(JumpInsnNode(Opcodes.IFNONNULL, LabelNode(label)))
        insnList.add(VarInsnNode(Opcodes.ALOAD, 0))
        newProxyClass(insnList, proxyOwner)
        insnList.add(
            FieldInsnNode(
                Opcodes.PUTFIELD,
                owner,
                fieldName,
                fieldDesc
            )
        )
        insnList.add(LabelNode(label))
        insnList.add(FrameNode(Opcodes.F_SAME, 0, null, 0, null))
        val localIndex = insnList.createMethodHookParam(
            owner,
            mapper.methodEntity,
            mapper.samMethodEntity,
            false
        )
        insnList.add(VarInsnNode(Opcodes.ALOAD, 0))
        insnList.add(FieldInsnNode(Opcodes.GETFIELD, owner, fieldName, fieldDesc))
        insnList.invokeProxyMethod(localIndex, proxyOwner, mapper.proxyMethodEntity)
        return insnList
    }

    private fun InsnList.invokeProxyMethod(
        localIndex: Int,
        proxyOwner: String,
        proxyMethodEntity: ProxyMethodEntity,
    ) {
        add(VarInsnNode(Opcodes.ALOAD, localIndex))
        add(
            MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                proxyOwner,
                proxyMethodEntity.methodName,
                proxyMethodEntity.methodDesc,
                false
            )
        )
        val label = Label()
        add(JumpInsnNode(Opcodes.IFEQ, LabelNode(label)))
        add(InsnNode(Opcodes.RETURN))
        add(LabelNode(label))
        add(
            FrameNode(
                Opcodes.F_APPEND,
                1,
                arrayOf(HookManager.sMethodHookParamDesc.substringBefore(";").substringAfter("L")),
                0,
                null
            )
        )
    }

    private fun InsnList.createMethodHookParam(
        owner: String,
        methodEntity: MethodEntity,
        samMethodEntity: MethodEntity?,
        isStatic: Boolean
    ): Int {
        var localIndex = if (isStatic) 0 else 1
        var argumentTypes = Type.getArgumentTypes(methodEntity.methodDesc)
        if (samMethodEntity != null && argumentTypes.isNotEmpty()) {
            val samArgumentTypes = Type.getArgumentTypes(samMethodEntity.methodDesc)
            val samMethodArgumentIndex = argumentTypes.size - samArgumentTypes.size
            argumentTypes.run {
                forEachIndexed { index, type ->
                    if (index == samMethodArgumentIndex) {
                        return@run
                    }
                    localIndex += type.size
                }
            }
            argumentTypes = argumentTypes.toMutableList()
                .slice(samMethodArgumentIndex until argumentTypes.size)
                .toTypedArray()
        }
        val argsLength = argumentTypes.size
        add(LdcInsnNode(owner))
        add(LdcInsnNode(methodEntity.methodName))
        add(argsLength.insnNode())
        add(TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"))
        argumentTypes.forEachIndexed { index, type ->
            add(InsnNode(Opcodes.DUP))
            add(index.insnNode())
            val opcode = when (type.sort) {
                Type.BYTE, Type.SHORT, Type.CHAR, Type.INT -> Opcodes.ILOAD
                Type.FLOAT -> Opcodes.FLOAD
                Type.LONG -> Opcodes.LLOAD
                Type.DOUBLE -> Opcodes.DLOAD
                Type.OBJECT, Type.ARRAY -> Opcodes.ALOAD
                else -> throw IllegalArgumentException("localVariables parameter exception (owner:${owner},method:(${methodEntity.nameWithDesc()})")
            }
            add(VarInsnNode(opcode, localIndex))
            when (type.sort) {
                Type.BYTE -> MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Byte",
                    "valueOf",
                    "(B)Ljava/lang/Byte;",
                    false
                )
                Type.SHORT -> MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Short",
                    "valueOf",
                    "(S)Ljava/lang/Short;",
                    false
                )
                Type.CHAR -> MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Character",
                    "valueOf",
                    "(C)Ljava/lang/Character;",
                    false
                )
                Type.INT -> MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false
                )
                Type.LONG -> MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false
                )
                Type.FLOAT -> MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false
                )
                Type.DOUBLE -> MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                )
                else -> null
            }?.apply {
                add(this)
            }
            add(InsnNode(Opcodes.AASTORE))
            localIndex += type.size
        }
        add(
            MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HookManager.sMethodHookParamDesc.substringBefore(";").substringAfter("L"),
                "newInstance",
                "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)${HookManager.sMethodHookParamDesc}"
            )
        )
        add(VarInsnNode(Opcodes.ASTORE, localIndex))
        return localIndex
    }


    private fun isVisitInvokeStaticField(
        collectMethods: Map<String, MethodMapperEntity>
    ): Boolean {
        return kotlin.run {
            collectMethods.forEach {
                val access = it.value.methodEntity.access
                if (access == Opcodes.H_INVOKESTATIC) { //lambda表达式未捕获外部实例->私有静态方法或静态方法引用
                    return@run true
                }
            }
            return@run false
        }
    }

    /**
     *  将常量池中的常量压入操作数栈中
     *  iconst [-1,5]
     *  bipush [-128,127]
     *  sipush [-32768,32767]
     *  其他 ldc
     */
    private fun Int.insnNode(): AbstractInsnNode {
        return when {
            this <= 5 -> InsnNode(this + Opcodes.ICONST_0)
            this <= 127 -> IntInsnNode(Opcodes.BIPUSH, this)
            this <= 32767 -> IntInsnNode(Opcodes.SIPUSH, this)
            else -> LdcInsnNode(this)
        }
    }

    private fun isVisitInvokeSpecialField(
        collectMethods: Map<String, MethodMapperEntity>
    ): Boolean {
        return kotlin.run {
            collectMethods.forEach {
                val access = it.value.methodEntity.access
                if (access == -1 ||
                    access == Opcodes.H_INVOKESPECIAL || //lambda表达式捕获外部事例->私有实例方法
                    access == Opcodes.H_NEWINVOKESPECIAL || //lambda方法引用->构造方法调用
                    access == Opcodes.H_INVOKEVIRTUAL ||//lambda方法引用->实例方法调用｜类方法引用
                    access == Opcodes.H_INVOKEINTERFACE //lambda方法引用->实例方法调用(多态)
                ) {
                    return@run true
                }
            }
            return@run false
        }
    }
}