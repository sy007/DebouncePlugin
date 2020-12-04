package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.utils.ConfigUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 *    author : Six
 *    date   : 2020/11/28
 *    desc   : 在<init>函数中创建DebounceCheck实例
 *    version: 1.0
 */
class InitVisitor(
    private val owner: String, mv: MethodVisitor
) : MethodVisitor(Opcodes.ASM6, mv) {
    override fun visitInsn(opcode: Int) {
        //在init结束之前初始化DebounceCheck
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitTypeInsn(Opcodes.NEW, ConfigUtil.sOwner)
            mv.visitInsn(Opcodes.DUP)
            mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                ConfigUtil.sOwner,
                ConfigUtil.sInitName,
                ConfigUtil.sInitDesc,
                false
            )
            mv.visitFieldInsn(
                Opcodes.PUTFIELD,
                owner,
                ConfigUtil.sFieldName,
                ConfigUtil.sFieldDesc
            )
        }
        super.visitInsn(opcode)
    }
}