import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.extension.DebounceExtension
import com.sunyuan.click.debounce.utils.*
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
class ClickMethodVisitor(
    private val nextClassVisitor: ClassVisitor,
    private val hookMethodEntities: Map<String, MethodEntity>,
    private val includeMethodOfAnnotation: (annotationNode: AnnotationNode) -> Boolean,
    private val excludeMethodOfAnnotation: (annotationNode: AnnotationNode) -> Boolean,
    private val collectImplTargetInterfaces: (name: String) -> Set<String>
) : ClassNode(Opcodes.ASM7) {
    private lateinit var implTargetInterfaces: Set<String>
    override fun visitEnd() {
        super.visitEnd()
        if (name == ConfigUtil.sOwner) {
            BounceCheckerModifyUtil.modify(methods)
            accept(nextClassVisitor)
            return
        }
        implTargetInterfaces = collectImplTargetInterfaces(name)
        methods.filter {
            !excludeMethodOfAnnotation(it.visibleAnnotations)
        }.forEach { methodNode ->
            collectMethodOfAnnotation(methodNode)
            collectMethodOfImplInterface(methodNode)
            collectMethodOfLambda(methodNode)
        }
        ClickMethodModifyUtil.modify(this)
        accept(nextClassVisitor)
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
        record(name, methodNode.name, methodNode.desc)
    }

    private fun collectMethodOfImplInterface(methodNode: MethodNode) {
        if (implTargetInterfaces.isEmpty()) {
            return
        }
        val methodEntity = hookMethodEntities[methodNode.name + methodNode.desc]
        if (methodEntity != null && implTargetInterfaces.contains(methodEntity.interfaceName)
            && methodEntity.methodName == methodNode.name && methodEntity.methodDesc == methodNode.desc
        ) {
            record(name, methodNode.name, methodNode.desc)
        }
    }

    /**
     * 处理Lambda
     * 获取操作码列表  see https://juejin.cn/post/6844904118700474375#heading-19
     */
    private fun collectMethodOfLambda(methodNode: MethodNode) {
        val iterator: MutableListIterator<AbstractInsnNode> = methodNode.instructions.iterator()
        while (iterator.hasNext()) {
            val node: AbstractInsnNode = iterator.next()
            if (node !is InvokeDynamicInsnNode) {
                continue
            }
            /**
             * 1.JDK 9  字符串拼接使用inDy指令;此时bsm.owner=java/lang/invoke/StringConcatFactory
             * 2.JDK 11 动态常量使用inDy指令;此时bsm.owner=java/lang/invoke/ConstantBootstraps
             * 3.JDK 17 switch的模式匹配使用inDy指令;此时bsm.owner=java/lang/runtime/SwitchBootstraps
             */
            if (ConfigUtil.LambdaBSMOwner != node.bsm.owner) {
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
            //对于Lambda表达式bsmArgs是三个类型固定的值
            //方法描述符
            val samMethodType = bsmArgs[0] as Type
            //脱糖后的方法，从Handle中取出该方法的信息
            val handle: Handle = bsmArgs[1] as Handle
            val hookMethodIterator = hookMethodEntities.iterator()
            while (hookMethodIterator.hasNext()) {
                val methodEntity = hookMethodIterator.next().value
                if (methodEntity.interfaceName == samBase &&
                    methodEntity.methodName == samMethodName &&
                    methodEntity.methodDesc == samMethodType.descriptor
                ) {
                    record(handle.owner, handle.name, handle.desc, handle.tag)
                    break
                }
            }
        }
    }

    private fun includeMethodOfAnnotation(annotationNodes: List<AnnotationNode>?): Boolean {
        return kotlin.run {
            annotationNodes?.forEach {
                if (includeMethodOfAnnotation(it)) {
                    return@run true
                }
            }
            return@run false
        }
    }

    private fun excludeMethodOfAnnotation(annotationNodes: List<AnnotationNode>?): Boolean {
        return kotlin.run {
            annotationNodes?.forEach {
                if (excludeMethodOfAnnotation(it)) {
                    return@run true
                }
            }
            return@run false
        }
    }

    private fun record(name: String, methodName: String, methodDes: String, access: Int = -1) {
        var methodEntities = MethodUtil.sModifyOfMethods[name]
        if (methodEntities == null) {
            methodEntities = ConcurrentHashMap()
            MethodUtil.sModifyOfMethods[name] = methodEntities
        }
        methodEntities[methodName + methodDes] = MethodEntity().apply {
            this.access = access
            this.methodName = methodName
            this.methodDesc = methodDes
        }
    }
}