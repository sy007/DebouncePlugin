package com.sunyuan.asm.test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * @author sy007
 * @date 2022/10/29
 * @description
 */
public class LambdaSimple {

    public LambdaSimple(){

    }
    public LambdaSimple(String s) {
        System.out.println(s);
    }
    public void instanceReference(String str) {
        System.out.println(str);
    }
    public static void staticReference(String str) {
        System.out.println(str);
    }
    public void classReference(String str) {
        System.out.println(str);
    }

    public void test1() {
        Runnable runnable = () -> {

        };
        runnable.run();
    }


    public void test2() {
        //实例方法引用
        Consumer<String> consumer = new LambdaSimple()::instanceReference;
        consumer.accept("hello instance reference!");
        //静态方法引用
        consumer = LambdaSimple::staticReference;
        consumer.accept("hello static reference!");
        //构造方法引用
        consumer = LambdaSimple::new;
        consumer.accept("hello static reference!");
        //类方法引用
        BiConsumer<LambdaSimple, String> classReference = LambdaSimple::classReference;
        classReference.accept(new LambdaSimple(), "hello class reference!");
    }

}
