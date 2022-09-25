package com.sunyuan.click.debounce.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

/**
 * @author sy007
 * @date 2022/09/24
 * @description
 */
object BounceCheckerModifyUtil {

    fun modify(methods: List<MethodNode>) {
        val instructions = methods.find {
            it.name.equals("<clinit>") && it.desc.equals("()V")
        }?.instructions ?: return
        instructions.forEach {
            if (isDebugField(it)) {
                instructions[it] =
                    InsnNode(if (ConfigUtil.sDebug) Opcodes.ICONST_1 else Opcodes.ICONST_0)
            } else if (isCheckTimeField(it)) {
                (it as LdcInsnNode).cst = ConfigUtil.sCheckTime
            }
        }
    }

    private fun isDebugField(node: AbstractInsnNode): Boolean {
        if (node !is InsnNode) {
            return false
        }
        if (node.next !is FieldInsnNode) {
            return false
        }
        val fieldInsnNode = node.next as FieldInsnNode
        return (fieldInsnNode.name + fieldInsnNode.desc) == ConfigUtil.sDebugNameWithDes
    }

    private fun isCheckTimeField(node: AbstractInsnNode): Boolean {
        if (node !is LdcInsnNode) {
            return false
        }
        if (node.next !is FieldInsnNode) {
            return false
        }
        val fieldInsnNode = node.next as FieldInsnNode
        return (fieldInsnNode.name + fieldInsnNode.desc) == ConfigUtil.sCheckTimeNameWithDes
    }
}