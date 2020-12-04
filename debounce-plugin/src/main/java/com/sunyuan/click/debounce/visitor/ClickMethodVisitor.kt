package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.utils.ConfigUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

/**
 * author : Sy007
 * date   : 2020/11/28
 * desc   :
 * version: 1.0
 */
class ClickMethodVisitor(
    private val owner: String,
    mv: MethodVisitor, access: Int, name: String, desc: String
) : AdviceAdapter(Opcodes.ASM6, mv, access, name, desc) {

    override fun onMethodEnter() {
        super.onMethodEnter()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(
            Opcodes.GETFIELD,
            owner,
            ConfigUtil.sFieldName,
            ConfigUtil.sFieldDesc
        )
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            ConfigUtil.sOwner,
            ConfigUtil.sMethodName,
            ConfigUtil.sMethodDesc,
            false
        )
        val label = Label()
        mv.visitJumpInsn(Opcodes.IFEQ, label)
        mv.visitInsn(Opcodes.RETURN)
        mv.visitLabel(label)
    }

}