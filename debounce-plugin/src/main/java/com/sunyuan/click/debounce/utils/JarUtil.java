package com.sunyuan.click.debounce.utils;

import com.sunyuan.click.debounce.callback.JarTransformCallBack;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarUtil {
    public static File modifyJarFile(File jarFile, File tempDir, JarTransformCallBack callBack) throws IOException {
        //临时输出文件
        String hexName = DigestUtils.md5Hex(jarFile.getAbsolutePath()).substring(0, 8);
        File outPutJar = new File(tempDir, hexName + jarFile.getName());
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outPutJar));
        //原文件
        JarFile originJar = new JarFile(jarFile);
        Enumeration<JarEntry> enumeration = originJar.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement();
            InputStream inputStream = originJar.getInputStream(jarEntry);
            String entryName = jarEntry.getName();
            JarEntry destEntry = new JarEntry(entryName);
            jarOutputStream.putNextEntry(destEntry);
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream);
            byte[] modifiedClassBytes = null;
            if (ClassUtil.checkClassName(entryName)) {
                modifiedClassBytes = callBack.process(entryName, sourceClassBytes);
            }
            if (modifiedClassBytes == null) {
                jarOutputStream.write(sourceClassBytes);
            } else {
                jarOutputStream.write(modifiedClassBytes);
            }
            jarOutputStream.closeEntry();
        }
        jarOutputStream.close();
        originJar.close();
        return outPutJar;
    }
}
