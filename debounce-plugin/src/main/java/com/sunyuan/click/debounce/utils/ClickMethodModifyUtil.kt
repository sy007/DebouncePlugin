package com.sunyuan.click.debounce.utils

import com.sunyuan.click.debounce.entity.MethodEntity
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

/**
 * @author sy007
 * @date 2022/09/24
 * @description
 */
object ClickMethodModifyUtil {

    fun modify(classNode: ClassNode) {
        val collectMethods = MethodUtil.sModifyOfMethods[classNode.name]
        if (collectMethods.isNullOrEmpty()) {
            return
        }
        if (isVisitInvokeSpecialField(collectMethods)) {
            //为当前类创建实例属性
            classNode.fields.add(
                FieldNode(
                    Opcodes.ACC_PRIVATE,
                    ConfigUtil.sFieldName, ConfigUtil.sFieldDesc, null, null
                )
            )
        }
        if (isVisitInvokeStaticField(collectMethods)) {
            //为当前类创建静态属性
            classNode.fields.add(
                FieldNode(
                    Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                    ConfigUtil.sStaticFieldName, ConfigUtil.sFieldDesc, null, null
                )
            )
        }
        classNode.methods.filter {
            collectMethods.contains(it.name + it.desc)
        }.forEach {
            val methodEntity = collectMethods[it.name + it.desc]!!
            when (methodEntity.access) {
                Opcodes.H_INVOKESTATIC -> {
                    modifyStaticClickMethod(classNode.name, it)
                }
                else -> {
                    modifyClickMethod(classNode.name, it)
                }
            }
        }
    }
    /**
     * 在字节码跳转位置后插入FrameNode解决D8 warning [Expected stack map table for method with non-linear control flow]
     */
    private fun modifyStaticClickMethod(owner: String, methodNode: MethodNode) {
        val insnList = InsnList()
        /**
         * if($$sBounceChecker==null){
         *   $$sBounceChecker = new BounceChecker();
         * }
         */
        insnList.add(LabelNode())
        insnList.add(
            FieldInsnNode(
                Opcodes.GETSTATIC,
                owner,
                ConfigUtil.sStaticFieldName,
                ConfigUtil.sFieldDesc
            )
        )
        val label1 = Label()
        insnList.add(JumpInsnNode(Opcodes.IFNONNULL, LabelNode(label1)))
        insnList.add(TypeInsnNode(Opcodes.NEW, ConfigUtil.sOwner))
        insnList.add(InsnNode(Opcodes.DUP))
        insnList.add(
            MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                ConfigUtil.sOwner,
                ConfigUtil.sInitName,
                ConfigUtil.sInitDesc
            )
        )
        insnList.add(
            FieldInsnNode(
                Opcodes.PUTSTATIC,
                owner,
                ConfigUtil.sStaticFieldName,
                ConfigUtil.sFieldDesc
            )
        )
        insnList.add(LabelNode(label1))
        insnList.add(FrameNode(Opcodes.F_SAME, 0, null, 0, null))
        /**
         * if($$sBounceChecker.check()){
         * return;
         * }
         */
        insnList.add(
            FieldInsnNode(
                Opcodes.GETSTATIC,
                owner,
                ConfigUtil.sStaticFieldName,
                ConfigUtil.sFieldDesc
            )
        )
        insnList.add(
            MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                ConfigUtil.sOwner,
                ConfigUtil.sMethodName,
                ConfigUtil.sMethodDesc
            )
        )
        val label2 = Label()
        insnList.add(JumpInsnNode(Opcodes.IFEQ, LabelNode(label2)))
        insnList.add(InsnNode(Opcodes.RETURN))
        insnList.add(LabelNode(label2))
        insnList.add(FrameNode(Opcodes.F_SAME, 0, null, 0, null))
        methodNode.instructions.insert(insnList)
    }

    /**
     * 在字节码跳转位置后插入FrameNode解决D8 warning [Expected stack map table for method with non-linear control flow]
     */
    private fun modifyClickMethod(owner: String, methodNode: MethodNode) {
        val insnList = InsnList()
        /**
         * if($$bounceChecker==null){
         * $$bounceChecker = new BounceChecker();
         * }
         */
        insnList.add(VarInsnNode(Opcodes.ALOAD, 0))
        insnList.add(
            FieldInsnNode(
                Opcodes.GETFIELD,
                owner,
                ConfigUtil.sFieldName,
                ConfigUtil.sFieldDesc
            )
        )
        val label1 = Label()
        insnList.add(JumpInsnNode(Opcodes.IFNONNULL, LabelNode(label1)))
        insnList.add(VarInsnNode(Opcodes.ALOAD, 0))
        insnList.add(TypeInsnNode(Opcodes.NEW, ConfigUtil.sOwner))
        insnList.add(InsnNode(Opcodes.DUP))
        insnList.add(
            MethodInsnNode(
                Opcodes.INVOKESPECIAL, ConfigUtil.sOwner,
                ConfigUtil.sInitName,
                ConfigUtil.sInitDesc
            )
        )
        insnList.add(
            FieldInsnNode(
                Opcodes.PUTFIELD,
                owner,
                ConfigUtil.sFieldName,
                ConfigUtil.sFieldDesc
            )
        )
        insnList.add(LabelNode(label1))
        insnList.add(FrameNode(Opcodes.F_SAME, 0, null, 0, null))
        /**
         * if($$bounceChecker.check()){
         * return;
         * }
         */
        insnList.add(VarInsnNode(Opcodes.ALOAD, 0))
        insnList.add(
            FieldInsnNode(
                Opcodes.GETFIELD,
                owner,
                ConfigUtil.sFieldName,
                ConfigUtil.sFieldDesc
            )
        )
        insnList.add(
            MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                ConfigUtil.sOwner,
                ConfigUtil.sMethodName,
                ConfigUtil.sMethodDesc
            )
        )
        val label2 = Label()
        insnList.add(JumpInsnNode(Opcodes.IFEQ, LabelNode(label2)))
        insnList.add(InsnNode(Opcodes.RETURN))
        insnList.add(LabelNode(label2))
        insnList.add(FrameNode(Opcodes.F_SAME, 0, null, 0, null))
        methodNode.instructions.insert(insnList)
    }


    private fun isVisitInvokeStaticField(
        collectMethods: Map<String, MethodEntity>
    ): Boolean {
        return kotlin.run {
            collectMethods.forEach {
                if (it.value.access == Opcodes.H_INVOKESTATIC) { //lambda表达式未捕获外部实例->私有静态方法或静态方法引用
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
                if (it.value.access == -1 ||
                    it.value.access == Opcodes.H_INVOKESPECIAL || //lambda表达式捕获外部事例->私有实例方法
                    it.value.access == Opcodes.H_NEWINVOKESPECIAL || //lambda方法引用->构造方法调用
                    it.value.access == Opcodes.H_INVOKEVIRTUAL ||//lambda方法引用->实例方法调用｜类方法引用
                    it.value.access == Opcodes.H_INVOKEINTERFACE //lambda方法引用->实例方法调用(多态)
                ) {
                    return@run true
                }
            }
            return@run false
        }
    }
}