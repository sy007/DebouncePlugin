package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.utils.ConfigUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

/**
 *    author : Six
 *    date   : 2020/11/29 002918:49
 *    desc   : non-instance-capturing lambdas hook
 *    version: 1.0
 */
class LambdaStaticClickMethodVisitor(mv: MethodVisitor, access: Int, name: String, desc: String) :
    AdviceAdapter(Opcodes.ASM7, mv, access, name, desc) {
    override fun onMethodEnter() {
        super.onMethodEnter()
        mv.visitFieldInsn(
            GETSTATIC,
            ConfigUtil.sStaticLambdaOwner,
            ConfigUtil.sStaticLambdaName,
            ConfigUtil.sStaticLambdaDesc
        );
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            ConfigUtil.sOwner,
            ConfigUtil.sMethodName,
            ConfigUtil.sMethodDesc,
            false
        );
        val label = Label()
        mv.visitJumpInsn(IFEQ, label);
        mv.visitInsn(Opcodes.RETURN)
        mv.visitLabel(label)
    }
}