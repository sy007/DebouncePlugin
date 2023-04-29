package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.entity.ProxyClassEntity
import com.sunyuan.click.debounce.entity.ProxyMethodEntity
import com.sunyuan.click.debounce.utils.HookManager
import org.objectweb.asm.*

/**
 * @author sy007
 * @date 2023/01/30
 * @description
 */
class ProxyClassVisitor : ClassVisitor(Opcodes.ASM7) {
    private lateinit var proxyClassEntity: ProxyClassEntity
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        proxyClassEntity = ProxyClassEntity()
        proxyClassEntity.owner = name
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        if (access and Opcodes.ACC_STATIC != 0 ||
            Type.BOOLEAN_TYPE != Type.getType(descriptor).returnType
        ) {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        val argumentTypes: Array<Type> = Type.getArgumentTypes(descriptor)
        if (argumentTypes.isEmpty() || argumentTypes.first().descriptor != HookManager.sMethodHookParamDesc) {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        val proxyMethodEntity = ProxyMethodEntity().apply {
            methodName = name
            methodDesc = descriptor
        }
        return ProxyMethodVisitor(proxyClassEntity, proxyMethodEntity)
    }

    override fun visitEnd() {
        super.visitEnd()
        proxyClassEntity.print()
        HookManager.sProxyClassEntity = proxyClassEntity
    }
}

class ProxyMethodVisitor(
    private val proxyClassEntity: ProxyClassEntity,
    private val proxyMethodEntity: ProxyMethodEntity
) :
    MethodVisitor(Opcodes.ASM7) {

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        return when (descriptor) {
            HookManager.sInterfaceMethodProxyDesc -> {
                InterfaceMethodProxyVisitor(proxyClassEntity, proxyMethodEntity)
            }
            HookManager.sAnnotationMethodProxyDesc -> {
                AnnotationMethodProxyVisitor(
                    proxyClassEntity,
                    proxyMethodEntity
                )
            }
            else -> super.visitAnnotation(descriptor, visible)
        }
    }
}

class InterfaceMethodProxyVisitor(
    private val proxyClassEntity: ProxyClassEntity,
    private val proxyMethodEntity: ProxyMethodEntity
) :
    AnnotationVisitor(Opcodes.ASM7) {
    private val values: MutableMap<String, Any> = mutableMapOf()

    override fun visit(name: String, value: Any) {
        super.visit(name, value)
        values[name] = value
    }

    override fun visitArray(name: String): AnnotationVisitor {
        val temp = name
        val elements = mutableListOf<Type>()
        return object : AnnotationVisitor(Opcodes.ASM7) {
            override fun visit(name: String?, value: Any) {
                super.visit(name, value)
                elements.add((value as Type))
                values[temp] = elements
            }
        }
    }

    override fun visitEnd() {
        super.visitEnd()
        val ownerType = values["ownerType"] as Type
        val methodName = values["methodName"].toString()
        var returnType = values["returnType"] as Type
        //兼容kotlin写法的代理类
        if ("Lkotlin/Unit;" == returnType.descriptor) {
            returnType = Type.VOID_TYPE
        }
        val parametersType = values["parameterTypes"] as List<Type>
        val methodDesc = Type.getMethodDescriptor(
            returnType,
            *parametersType.toTypedArray()
        )
        val owner = ownerType.descriptor.substringBefore(";").substringAfter("L")
        proxyClassEntity.methodIndex[MethodEntity().apply {
            this.owner = owner
            this.methodName = methodName
            this.methodDesc = methodDesc
        }] = proxyMethodEntity
    }
}

class AnnotationMethodProxyVisitor(
    private val proxyClassEntity: ProxyClassEntity,
    private val proxyMethodEntity: ProxyMethodEntity
) : AnnotationVisitor(Opcodes.ASM7) {
    private val values: MutableMap<String, Any> = mutableMapOf()
    override fun visit(name: String, value: Any) {
        super.visit(name, value)
        values[name] = value
    }

    override fun visitEnd() {
        super.visitEnd()
        val typeDescriptor = (values["type"] as Type).descriptor
        proxyClassEntity.annotationIndex[typeDescriptor] = proxyMethodEntity
    }
}