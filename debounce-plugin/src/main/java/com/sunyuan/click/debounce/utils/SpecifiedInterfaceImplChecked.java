package com.sunyuan.click.debounce.utils;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * author : Six
 * date   : 2021/1/13 001319:47
 * desc   :
 * version: 1.0
 */
public class SpecifiedInterfaceImplChecked {
    private ClassLoader urlClassLoader;
    private static final String OBJECT = "java/lang/Object";

    public void setUrlClassLoader(ClassLoader urlClassLoader) {
        this.urlClassLoader = urlClassLoader;
    }

    /**
     * 记录直接或间接实现目标接口的接口
     *
     * @param className             当前类
     * @param targetInterfaces      目标接口
     * @param collectImplInterfaces 记录直接或间接实现目标接口的接口
     *                              <p>
     *                              记录结果放入collectImplInterfaces中
     */
    public void collectImplTargetInterfaces(String className, Set<String> targetInterfaces, Set<String> collectImplInterfaces) {
        if (isObject(className) || targetInterfaces.size() == collectImplInterfaces.size()) {
            return;
        }
        ClassReader reader = getClassReader(className);
        if (reader == null) {
            return;
        }
        matchTargetInterfaces(reader.getInterfaces(), targetInterfaces, collectImplInterfaces);
        collectImplTargetInterfaces(reader.getSuperName(), targetInterfaces, collectImplInterfaces);
    }


    /**
     * 检查当前类是 Object 类型
     *
     * @param className class name
     * @return checked result
     */
    private static boolean isObject(String className) {
        return OBJECT.equals(className);
    }

    /**
     * 匹配目标接口，将匹配结果放入recordImplInterfaceSet中
     *
     * @param interfaces             待检查接口
     * @param targetInterfaceSet     目标接口
     * @param recordImplInterfaceSet 匹配结果
     */
    private void matchTargetInterfaces(String[] interfaces, Set<String> targetInterfaceSet, Set<String> recordImplInterfaceSet) {
        if (interfaces.length == 0 || targetInterfaceSet.size() == recordImplInterfaceSet.size()) {
            return;
        }
        for (String inter : interfaces) {
            if (targetInterfaceSet.contains(inter)) {
                recordImplInterfaceSet.add(inter);
            } else {
                ClassReader reader = getClassReader(inter);
                if (reader == null) {
                    return;
                }
                matchTargetInterfaces(reader.getInterfaces(), targetInterfaceSet, recordImplInterfaceSet);
            }
        }
    }


    private ClassReader getClassReader(final String className) {
        InputStream inputStream = urlClassLoader.getResourceAsStream(className + ".class");
        try {
            if (inputStream != null) {
                return new ClassReader(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
