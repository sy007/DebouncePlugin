package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.utils.ConfigUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class DoubleCheckClinitVisitor(
    private val isDebug: Boolean, private val checkTime: Long, mv: MethodVisitor
) : MethodVisitor(Opcodes.ASM6, mv) {
    override fun visitInsn(opcode: Int) {
        if (opcode == Opcodes.RETURN) {
            val debugInsn = if (isDebug) {
                Opcodes.ICONST_1
            } else {
                Opcodes.ICONST_0
            }
            mv.visitInsn(debugInsn)
            mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                ConfigUtil.sOwner,
                ConfigUtil.sIsDebugFieldName,
                "Z"
            )
            mv.visitLdcInsn(checkTime)
            mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                ConfigUtil.sOwner,
                ConfigUtil.sDebounceCheckTimeFieldName,
                "J"
            )
        }
        super.visitInsn(opcode)
    }
}