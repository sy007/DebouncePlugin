package com.sunyuan.click.debounce.callback;

public interface JarTransformCallBack {
    byte[] process(String relativePath, byte[] sourceBytes);
}
