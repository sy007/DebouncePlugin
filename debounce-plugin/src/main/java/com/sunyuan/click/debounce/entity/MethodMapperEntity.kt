package com.sunyuan.click.debounce.entity

/**
 * @author sy007
 * @date 2023/02/11
 * @description
 */
class MethodMapperEntity(
    //被插桩的方法信息
    val methodEntity: MethodEntity,
    //插桩的方法信息
    val proxyMethodEntity: ProxyMethodEntity
) {
    //函数式接口信息
    var samMethodEntity: MethodEntity? = null
}