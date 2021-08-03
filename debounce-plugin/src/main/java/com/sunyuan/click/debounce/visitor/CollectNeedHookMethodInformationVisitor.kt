import com.sunyuan.click.debounce.entity.LambdaMethodEntity
import com.sunyuan.click.debounce.entity.MethodEntity
import com.sunyuan.click.debounce.utils.ConfigUtil
import com.sunyuan.click.debounce.utils.MethodUtil
import com.sunyuan.click.debounce.utils.SpecifiedInterfaceImplChecked
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodNode
import java.util.function.Consumer

/**
 *    author : Six
 *    date   : 2020/11/29
 *    desc   : 收集需要hook的方法信息，包含普通方法和Lambda方法
 *    version: 1.0
 */
class CollectNeedHookMethodInformationVisitor(
    private val classVisitor: ClassVisitor,
    private val specifiedInterfaceImplChecked: SpecifiedInterfaceImplChecked
) : ClassNode(Opcodes.ASM7) {
    private val collectImplInterfaces = mutableSetOf<String>()
    override fun visitEnd() {
        super.visitEnd()
        specifiedInterfaceImplChecked.collectImplTargetInterfaces(
            name,
            ConfigUtil.sInterfaceSet,
            collectImplInterfaces
        )
//        LogUtil.warning(
//            "collected need hook method information",
//            "className:$name",
//            "collectImplInterfaces:$collectImplInterfaces"
//        )
        methods.forEach(Consumer { methodNode: MethodNode ->
            if (collectImplInterfaces.isNotEmpty()) {
                val methodEntity = ConfigUtil.sConfigHookMethods[methodNode.name + methodNode.desc]
                if (methodEntity != null && collectImplInterfaces.contains(methodEntity.interfaceName)) {
                    var methodEntities = MethodUtil.sCollectDefaultMethods[name]
                    if (methodEntities == null) {
                        methodEntities = mutableMapOf()
                        MethodUtil.sCollectDefaultMethods[name] = methodEntities
                    }
                    if (methodEntity.methodName == methodNode.name && methodEntity.methodDesc == methodNode.desc) {
                        methodEntities[methodNode.name + methodNode.desc] =
                            MethodEntity(methodNode.name, methodNode.desc, null)
                    }
                }
            }
            //处理Lambda
            //获取操作码列表  see https://juejin.cn/post/6844904118700474375#heading-19
            val iterator: MutableListIterator<AbstractInsnNode> = methodNode.instructions.iterator()
            while (iterator.hasNext()) {
                val node: AbstractInsnNode = iterator.next()
                if (node is InvokeDynamicInsnNode) {
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
                    //InvokeDynamic 原本的 handle
                    val handle: Handle = bsmArgs[1] as Handle
                    val configHookMethodIterator = ConfigUtil.sConfigHookMethods.iterator()
                    while (configHookMethodIterator.hasNext()) {
                        val methodEntity = configHookMethodIterator.next().value
                        if (methodEntity.interfaceName == samBase &&
                            methodEntity.methodName == samMethodName &&
                            methodEntity.methodDesc == samMethodType.descriptor
                        ) {
                            var lambdaMethods = MethodUtil.sCollectLambdaMethods[handle.owner]
                            if (lambdaMethods == null) {
                                lambdaMethods = mutableMapOf()
                                MethodUtil.sCollectLambdaMethods[handle.owner] = lambdaMethods
                            }
                            lambdaMethods[handle.name + handle.desc] = LambdaMethodEntity(
                                handle.name,
                                handle.desc, null, handle.tag
                            )
                            break
                        }
                    }
                }
            }
        })
        accept(classVisitor)
    }
}