package com.sunyuan.click.debounce.visitor

import com.sunyuan.click.debounce.config.Const
import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.entity.ProxyClassEntity
import com.sunyuan.click.debounce.entity.ProxyMethodEntity
import org.objectweb.asm.*

/**
 * @author sy007
 * @date 2023/01/30
 * @description
 */
class ProxyClassVisitor(api: Int, private val proxyClassEntity: ProxyClassEntity) :
    ClassVisitor(api) {

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        proxyClassEntity.owner = name
        super.visit(version, access, name, signature, superName, interfaces)
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
        if (argumentTypes.isEmpty() || argumentTypes.first().descriptor != Const.sMethodHookParamDesc) {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        val proxyMethodEntity = ProxyMethodEntity().apply {
            methodName = name
            methodDesc = descriptor
        }
        return ProxyMethodVisitor(api, proxyClassEntity, proxyMethodEntity)
    }
}

class ProxyMethodVisitor(
    api: Int,
    private val proxyClassEntity: ProxyClassEntity,
    private val proxyMethodEntity: ProxyMethodEntity
) : MethodVisitor(api) {

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        return when (descriptor) {
            Const.sInterfaceMethodProxyDesc -> {
                InterfaceMethodProxyVisitor(api, proxyClassEntity, proxyMethodEntity)
            }
            Const.sAnnotationMethodProxyDesc -> {
                AnnotationMethodProxyVisitor(
                    api,
                    proxyClassEntity,
                    proxyMethodEntity
                )
            }
            else -> super.visitAnnotation(descriptor, visible)
        }
    }
}

class InterfaceMethodProxyVisitor(
    api: Int,
    private val proxyClassEntity: ProxyClassEntity,
    private val proxyMethodEntity: ProxyMethodEntity
) :
    AnnotationVisitor(api) {
    private val values: MutableMap<String, Any> = mutableMapOf()

    override fun visit(name: String, value: Any) {
        values[name] = value
        super.visit(name, value)
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
        super.visitEnd()
    }
}

class AnnotationMethodProxyVisitor(
    api: Int,
    private val proxyClassEntity: ProxyClassEntity,
    private val proxyMethodEntity: ProxyMethodEntity
) : AnnotationVisitor(api) {
    private val values: MutableMap<String, Any> = mutableMapOf()
    override fun visit(name: String, value: Any) {
        values[name] = value
        super.visit(name, value)
    }

    override fun visitEnd() {
        val typeDescriptor = (values["type"] as Type).descriptor
        proxyClassEntity.annotationIndex[typeDescriptor] = proxyMethodEntity
        super.visitEnd()
    }
}