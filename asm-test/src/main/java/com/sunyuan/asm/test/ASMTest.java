package com.sunyuan.asm.test;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileInputStream;
import java.util.ListIterator;
import java.util.function.Consumer;


public class ASMTest {
    public static void main(String[] args) {
        lambdaTest();
    }

    private static void lambdaTest() {
        try (FileInputStream fis = new FileInputStream(getClassFilePath(LambdaSimple.class))) {
            ClassReader classReader = new ClassReader(fis);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            classNode.methods.forEach(new Consumer<MethodNode>() {
                @Override
                public void accept(MethodNode methodNode) {
                    ListIterator<AbstractInsnNode> it = methodNode.instructions.iterator();
                    while (it.hasNext()) {
                        AbstractInsnNode abstractInsnNode = it.next();
                        if (abstractInsnNode instanceof InvokeDynamicInsnNode) {
                            InvokeDynamicInsnNode node = (InvokeDynamicInsnNode) abstractInsnNode;
                            String desc = node.desc;
                            Type samBaseType = Type.getType(desc).getReturnType();
                            //接口名
                            String samBase = samBaseType.getDescriptor().replaceFirst("L", "")
                                    .replace(";", "");
                            //方法名
                            String samMethodName = node.name;
                            Object[] bsmArgs = node.bsmArgs;
                            //方法描述符
                            Type samMethodType = (Type) bsmArgs[0];
                            //脱糖后的方法，从Handle中取出该方法的信息
                            Handle handle = (Handle) bsmArgs[1];
                            System.out.println("desc=" + desc);
                            System.out.println("samBaseType.getDescriptor()=" + samBaseType.getDescriptor());
                            System.out.println("interfaceName=" + samBase);
                            System.out.println("methodName=" + samMethodName);
                            System.out.println("methodDes=" + samMethodType.getDescriptor());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getClassFilePath(Class clazz) {
        String buildDir = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
        String fileName = clazz.getSimpleName() + ".class";
        File file = new File(buildDir + clazz.getPackage().getName().replaceAll("[.]", "/") + "/", fileName);
        return file.getAbsolutePath();
    }

}