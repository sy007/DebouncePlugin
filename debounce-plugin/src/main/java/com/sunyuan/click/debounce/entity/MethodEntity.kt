package com.sunyuan.click.debounce.entity

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */

open class MethodEntity {
    @set:JvmName("methodName")
    var methodName: String = ""

    @set:JvmName("methodDesc")
    var methodDesc: String = ""

    @set:JvmName("interfaceName")
    var interfaceName: String = ""

    @set:JvmName("name")
    var name: String = ""

    constructor()

    /**
     * lambda method access flags.
     */
    var access: Int = -1

    constructor(name: String) {
        this.name = name
    }
}
