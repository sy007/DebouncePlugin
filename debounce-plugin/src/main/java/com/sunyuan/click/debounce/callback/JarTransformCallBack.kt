package com.sunyuan.click.debounce.callback

interface JarTransformCallBack {
    fun process(relativePath: String, sourceBytes: ByteArray): ByteArray?
}