package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.utils.ConfigUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

/**
 *    author : Six
 *    date   : 2020/11/29 002918:49
 *    version: 1.0
 */
class StaticClickMethodVisitor(
    private val owner: String, mv: MethodVisitor, access: Int, name: String, desc: String
) :
    AdviceAdapter(Opcodes.ASM7, mv, access, name, desc) {
    override fun onMethodEnter() {
        super.onMethodEnter()
        /**
         * if($$sBounceChecker==null){
         *   $$sBounceChecker = new BounceChecker();
         * }
         */
        mv.visitFieldInsn(
            GETSTATIC,
            owner,
            ConfigUtil.sStaticFieldName,
            ConfigUtil.sFieldDesc
        )
        val label1 = Label()
        mv.visitJumpInsn(IFNONNULL, label1)
        mv.visitTypeInsn(NEW, ConfigUtil.sOwner)
        mv.visitInsn(DUP)
        mv.visitMethodInsn(
            INVOKESPECIAL,
            ConfigUtil.sOwner,
            ConfigUtil.sInitName,
            ConfigUtil.sInitDesc,
            false
        )
        mv.visitFieldInsn(
            PUTSTATIC,
            owner,
            ConfigUtil.sStaticFieldName,
            ConfigUtil.sFieldDesc
        )
        mv.visitLabel(label1)

        /**
         * if($$sBounceChecker.check()){
         *    return;
         * }
         */
        mv.visitFieldInsn(
            GETSTATIC,
            owner,
            ConfigUtil.sStaticFieldName,
            ConfigUtil.sFieldDesc
        )
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            ConfigUtil.sOwner,
            ConfigUtil.sMethodName,
            ConfigUtil.sMethodDesc,
            false
        )
        val label2 = Label()
        mv.visitJumpInsn(IFEQ, label2)
        mv.visitInsn(RETURN)
        mv.visitLabel(label2)
    }
}