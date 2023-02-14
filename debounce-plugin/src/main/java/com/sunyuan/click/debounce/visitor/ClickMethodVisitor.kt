import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.entity.MethodMapperEntity
import com.sunyuan.click.debounce.entity.ProxyClassEntity
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
    private val proxyClassEntity: ProxyClassEntity,
    private val nextClassVisitor: ClassVisitor,
    private val hookMethodEntities: MutableSet<MethodEntity>,
    private val excludeMethodOfAnnotation: (annotationNode: AnnotationNode) -> Boolean,
    private val collectImplTargetInterfaces: (name: String) -> Set<String>
) : ClassNode(Opcodes.ASM7) {
    private lateinit var implTargetInterfaces: Set<String>

    override fun visitEnd() {
        super.visitEnd()
        implTargetInterfaces = collectImplTargetInterfaces(name)
        methods.filter {
            !excludeMethodOfAnnotation(it.visibleAnnotations)
        }.forEach { methodNode ->
            collectMethodOfAnnotation(methodNode)
            collectMethodOfImplInterface(methodNode)
            collectMethodOfLambda(methodNode)
        }
        ClickMethodModifyUtil.modify(this, proxyClassEntity)
        accept(nextClassVisitor)
    }


    private fun collectMethodOfAnnotation(methodNode: MethodNode) {
        val annotationDesc = includeMethodOfAnnotation(methodNode.visibleAnnotations)
        if (annotationDesc.isNullOrEmpty()) {
            return
        }
        if (methodNode.access and Opcodes.ACC_STATIC != 0) {
            throw IllegalStateException(
                "$annotationDesc decorated method cannot be static. (${
                    name.replace(
                        "/",
                        "."
                    )
                }.${methodNode.name + methodNode.desc})"
            )
        }
        if (HookManager.sClickDeBounceDesc == annotationDesc && (methodNode.localVariables.size != 2 || methodNode.localVariables.find { HookManager.sViewDesc == it.desc } == null)) {
            throw  IllegalStateException(
                "$annotationDesc decorated method , method parameter must have only one [Landroid/view/View] parameter. (${
                    name.replace(
                        "/",
                        "."
                    )
                }.${methodNode.name + methodNode.desc})"
            )
        }
        val proxyMethodEntity = proxyClassEntity.annotationIndex[annotationDesc]!!
        val methodEntity = MethodEntity().apply {
            this.methodName = methodNode.name
            this.methodDesc = methodNode.desc
        }
        val mapperEntity = MethodMapperEntity(methodEntity, proxyMethodEntity)
        record(name, mapperEntity)
    }

    private fun collectMethodOfImplInterface(methodNode: MethodNode) {
        if (implTargetInterfaces.isEmpty()) {
            return
        }
        val samMethodEntity = hookMethodEntities.find {
            it.nameWithDesc() == methodNode.name + methodNode.desc
        }
        if (samMethodEntity != null && implTargetInterfaces.contains(samMethodEntity.owner)) {
            proxyClassEntity.findProxyMethodEntity(samMethodEntity)?.apply {
                val mapper = MethodMapperEntity(MethodEntity().apply {
                    this.methodName = methodNode.name
                    this.methodDesc = methodNode.desc
                }, this).apply {
                    this.samMethodEntity = samMethodEntity
                }
                record(name, mapper)
            }
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
            if ("java/lang/invoke/LambdaMetafactory" != node.bsm.owner) {
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
            hookMethodEntities.find {
                it.owner == samBase && it.methodName == samMethodName && it.methodDesc == samMethodType.descriptor
            }?.let { samMethodEntity ->
                proxyClassEntity.findProxyMethodEntity(samMethodEntity)?.apply {
                    val mapper = MethodMapperEntity(MethodEntity().apply {
                        this.methodName = handle.name
                        this.methodDesc = handle.desc
                        this.access = handle.tag
                    }, this).apply {
                        this.samMethodEntity = samMethodEntity
                    }
                    record(name, mapper)
                }
            }
        }
    }

    private fun includeMethodOfAnnotation(annotationNodes: List<AnnotationNode>?): String? {
        return kotlin.run {
            annotationNodes?.forEach {
                if (HookManager.sProxyClassEntity?.annotationIndex?.containsKey(it.desc) == true) {
                    return@run it.desc
                }
            }
            return@run null
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

    private fun record(
        name: String,
        mapperEntity: MethodMapperEntity,
    ) {
        var methodEntities = HookManager.sModifyOfMethods[name]
        if (methodEntities == null) {
            methodEntities = ConcurrentHashMap()
            HookManager.sModifyOfMethods[name] = methodEntities
        }
        methodEntities[mapperEntity.methodEntity.nameWithDesc()] = mapperEntity
    }
}