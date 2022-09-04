import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.ConfigUtil
import com.sunyuan.click.debounce.utils.MethodUtil
import com.sunyuan.click.debounce.utils.FindInterfaceImplHelper
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.concurrent.ConcurrentHashMap

/**
 *    author : Six
 *    date   : 2020/11/29
 *    desc   : 收集需要hook的方法信息，包含普通方法和Lambda方法
 *    version: 1.0
 */
class CollectNeedModifyOfMethodVisitor(
    private val classVisitor: ClassVisitor,
    private val debounceExtension: DebounceExtension,
    private val findInterfaceImplHelper: FindInterfaceImplHelper
) : ClassNode(Opcodes.ASM7) {
    private val collectImplInterfaces = mutableSetOf<String>()
    override fun visitEnd() {
        super.visitEnd()
        findInterfaceImplHelper.findTargetInterfaceImpl(
            name,
            ConfigUtil.sInterfaceSet,
            collectImplInterfaces
        )
        methods.filter {
            !excludeMethodOfAnnotation(it.visibleAnnotations)
        }.forEach { methodNode ->
            collectMethodOfAnnotation(methodNode)
            collectMethodOfImplInterface(methodNode)
            collectMethodOfLambda(methodNode)
        }
        accept(classVisitor)
    }

    private fun collectMethodOfAnnotation(methodNode: MethodNode) {
        if (!includeMethodOfAnnotation(methodNode.visibleAnnotations)) {
            return
        }
        if (methodNode.access and Opcodes.ACC_STATIC != 0) {
            val annotations = methodNode.visibleAnnotations?.map {
                it.desc
            }.toString()
            throw  IllegalStateException(
                "$annotations decorated method cannot be static. (${
                    name.replace(
                        "/",
                        "."
                    )
                }.${methodNode.name + methodNode.desc})"
            )
        }
        var methodEntities = MethodUtil.sModifyOfMethods[name]
        if (methodEntities == null) {
            methodEntities = ConcurrentHashMap()
            MethodUtil.sModifyOfMethods[name] = methodEntities
        }
        methodEntities[methodNode.name + methodNode.desc] = MethodEntity().apply {
            methodName = methodNode.name
            methodDesc = methodNode.desc
        }
    }

    private fun collectMethodOfImplInterface(methodNode: MethodNode) {
        if (collectImplInterfaces.isEmpty()) {
            return
        }
        val methodEntity = ConfigUtil.sHookMethods[methodNode.name + methodNode.desc]
        if (methodEntity != null && collectImplInterfaces.contains(methodEntity.interfaceName)) {
            var methodEntities = MethodUtil.sModifyOfMethods[name]
            if (methodEntities == null) {
                methodEntities = ConcurrentHashMap()
                MethodUtil.sModifyOfMethods[name] = methodEntities
            }
            if (methodEntity.methodName == methodNode.name && methodEntity.methodDesc == methodNode.desc) {
                methodEntities[methodNode.name + methodNode.desc] = MethodEntity().apply {
                    methodName = methodNode.name
                    methodDesc = methodNode.desc
                }
            }
        }
    }

    /**
     * 处理Lambda
     * 获取操作码列表  see https://juejin.cn/post/6844904118700474375#heading-19
     */
    private fun collectMethodOfLambda(methodNode: MethodNode) {
        methodNode.access
        val iterator: MutableListIterator<AbstractInsnNode> = methodNode.instructions.iterator()
        while (iterator.hasNext()) {
            val node: AbstractInsnNode = iterator.next()
            if (node !is InvokeDynamicInsnNode) {
                continue
            }
            val desc: String = node.desc
            val samBaseType = Type.getType(desc).returnType
            //接口名
            val samBase = samBaseType.descriptor.replaceFirst("L", "")
                .replace(";", "")
            //方法名
            val samMethodName: String = node.name
            val bsmArgs: Array<Any> = node.bsmArgs
            //方法描述符
            val samMethodType = bsmArgs[0] as Type
            //脱糖后的方法，从Handle中取出该方法的信息
            val handle: Handle = bsmArgs[1] as Handle
            val hookMethodIterator = ConfigUtil.sHookMethods.iterator()
            while (hookMethodIterator.hasNext()) {
                val methodEntity = hookMethodIterator.next().value
                if (methodEntity.interfaceName == samBase &&
                    methodEntity.methodName == samMethodName &&
                    methodEntity.methodDesc == samMethodType.descriptor
                ) {
                    var methodEntities = MethodUtil.sModifyOfMethods[handle.owner]
                    if (methodEntities == null) {
                        methodEntities = ConcurrentHashMap()
                        MethodUtil.sModifyOfMethods[handle.owner] = methodEntities
                    }
                    methodEntities[handle.name + handle.desc] = MethodEntity().apply {
                        access = handle.tag
                        methodName = handle.name
                        methodDesc = handle.desc
                    }
                    break
                }
            }
        }
    }

    private fun includeMethodOfAnnotation(annotationNodes: List<AnnotationNode>?): Boolean {
        return kotlin.run {
            annotationNodes?.forEach {
                if (debounceExtension.includeMethodOfAnnotation(it.desc)) {
                    return@run true
                }
            }
            return@run false
        }
    }

    private fun excludeMethodOfAnnotation(annotationNodes: List<AnnotationNode>?): Boolean {
        return kotlin.run {
            annotationNodes?.forEach {
                if (debounceExtension.excludeMethodOfAnnotation(it.desc)) {
                    return@run true
                }
            }
            return@run false
        }
    }
}