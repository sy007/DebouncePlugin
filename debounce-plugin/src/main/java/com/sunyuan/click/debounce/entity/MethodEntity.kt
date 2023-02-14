package com.sunyuan.click.debounce.entity

import java.io.Serializable

/**
 * author : Sy007
 * date   : 2020/11/29
 * desc   :
 * version: 1.0
 */

open class MethodEntity : Serializable {
    var methodName: String = ""

    var methodDesc: String = ""

    var owner: String = ""

    /**
     * lambda method access flags.
     */
    var access: Int = -1

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodEntity) return false
        if (owner != other.owner) return false
        if (methodName != other.methodName) return false
        if (methodDesc != other.methodDesc) return false
        return true
    }

    override fun hashCode(): Int {
        var result = methodName.hashCode()
        result = 31 * result + methodDesc.hashCode()
        result = 31 * result + owner.hashCode()
        return result
    }

    fun nameWithDesc() = methodName + methodDesc

}
