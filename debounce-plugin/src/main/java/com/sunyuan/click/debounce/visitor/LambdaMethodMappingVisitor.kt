import com.sunyuan.click.debounce.entity.LambdaMethodEntity
import com.sunyuan.click.debounce.utils.MethodUtil
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
 *    desc   : 映射需要处理的Lambda表达式
 *    version: 1.0
 */
class LambdaMethodMappingVisitor(private val classVisitor: ClassVisitor) : ClassNode(Opcodes.ASM6) {

    override fun visitEnd() {
        super.visitEnd()
        methods.forEach(Consumer { methodNode: MethodNode ->
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
                    val normalMethodsIterator = MethodUtil.sNormalMethods.iterator()
                    while (normalMethodsIterator.hasNext()) {
                        val methodEntity = normalMethodsIterator.next().value
                        if (methodEntity.interfaceName == samBase &&
                            methodEntity.methodName == samMethodName &&
                            methodEntity.methodDesc == samMethodType.descriptor
                        ) {
                            val value = MethodUtil.sLambdaMethods.getOrPut(
                                handle.owner,
                                { mutableListOf<LambdaMethodEntity>() })
                            value.add(
                                LambdaMethodEntity(
                                    handle.name,
                                    handle.desc, null, handle.tag
                                )
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