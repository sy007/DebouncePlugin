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
        /**
         * if($$debounceCheck==null){
         *   $$debounceCheck = new DebounceCheck();
         * }
         */
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(
            Opcodes.GETFIELD,
            owner,
            ConfigUtil.sFieldName,
            ConfigUtil.sFieldDesc
        )
        val label1 = Label()
        mv.visitJumpInsn(Opcodes.IFNONNULL, label1)
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
        mv.visitLabel(label1)

        /**
         * if($$debounceCheck.isShake){
         *     return;
         * }
         */
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
        val label2 = Label()
        mv.visitJumpInsn(Opcodes.IFEQ, label2)
        mv.visitInsn(Opcodes.RETURN)
        mv.visitLabel(label2)
    }
}