# DebouncePlugin

Android点击事件防抖动插件，主要为了解决项目以及第三方库中快速点击问题。

## 1.支持以下功能：

1. 支持application ,library project 中使用
2. 支持Java,Kotlin点击事件防抖
3. 支持Java,Kotlin Lambda点击事件防抖
4. 支持排除或处理指定路径下的代码防抖处理**(文件级黑白名单)**，就跟写gitignore一样简单
5. 支持排除或处理指定方法防抖处理**(方法级黑白名单)**，两个注解解决你的问题
6. 支持配置点击事件间隔，即指定时间内，只允许触发一次点击事件
7. 支持多种类型的点击事件处理。例如：
   - ListView#onItemClick
   - ListView#onItemSelected
   - ExpandableListView#onGroupClick
   - ExpandableListView#onChildClick
   - ...只要你想处理，都支持。
8. 支持xml设置的点击事件防抖
9. 支持ButterKnife,XUtils等三方APT设置的点击事件防抖
10. 代码修改透明(插件对代码的修改会生成一个html报告)

## 2.如何使用

1. 在你项目的build.gradle依赖如下：

```groovy
buildscript {
    ...
    dependencies {
        //依赖插件所需的环境
        classpath 'io.github.sy007:debounce-plugin:1.2.0'
    }
}

allprojects {
    repositories {
        //添加mavenCentral仓库
        mavenCentral()
    }
}
```

2. 在app module中依赖如下：

```groovy
apply plugin: 'com.android.application'
//应用插件
apply plugin: 'debounce-plugin'
android{
   ...
}
dependencies {
    //插件所需的依赖库
    implementation 'io.github.sy007:debounce-lib:1.2.0'
}
```

这样插件就能正常工作了。无任何配置情况下，插件只会对全局所有onClick事件处理(项目，library,第三方jar和arr)。

## 3.自定义配置

插件支持自定义配置，在你的app#build.gradle配置如下：

```groovy
debounce {
    isDebug = true
    checkTime = 500
    generateReport = true
    includes = ["$填写需要事件防抖的目录或文件"]
    excludes = ["$填写不需要事件防抖的目录或文件"]
    includeForMethodAnnotation = ["$填写需要事件防抖的方法上注解信息"]
    excludeForMethodAnnotation = ["$填写不需要事件防抖的方法上注解信息"]
 		//需要防抖的事件信息
    methodEntities {
        xxxx {//随便填写，在methodEntities只要唯一,就像你在写productFlavors
            methodName 'xxx'//方法名称
            methodDesc 'xxxx'//方法描述
            interfaceName 'xxxx' //事件方法所在的接口名
        }
    }
}
```

1. isDebug ： 为true时会有日志输出

2. checkTime：两次点击相隔超过多长时间就认定为非抖动，单位毫秒

3. generateReport：是否生成方法修改报告，即插件修改的方法会生成一份html报告，报告路径:app/build/reports/debounce-plugin/${buildType}/modified-method-list.html

4. includes： 处理指定路径下的代码事件防抖(文件级白名单),类似于.gitignore 编写规则

5. excludes：排除指定路径下的代码事件防抖(文件级黑名单),类似于.gitignore 编写规则

6. includeForMethodAnnotation: 方法级白名单, 方法上声明了这些注解，那么该方法会插入防抖代码。**注意:这里给includeForMethodAnnotation配置的是注解的字节码**

   - 比如处理ButterKnife的OnClick和OnItemClick事件，方法上凡是声明了OnClick或OnItemClick注解都会插入防抖代码

   ```groovy
   includeForMethodAnnotation = ["Lbutterknife/OnClick;",
                                     "Lbutterknife/OnItemClick;"]
   ```

   - 插件内部默认添加了`ClickDeBounce`注解,即方法上声明了`ClickDeBounce`注解，都会插入防抖代码

     ```kotlin
      includeForMethodAnnotation.add("Lcom/sunyuan/debounce/lib/ClickDeBounce;")
     ```

7. excludeForMethodAnnotation：方法级别黑名单,方法上声明了这些注解，那么该方法不会插入防抖代码。插件内部默认添加了` IgnoreClickDeBounce`注解，即声明在方法上的`IgnoreClickDeBounce`注解，都不会插入防抖代码。**注意:这里配置的是注解的字节码**

   ```kotlin
   excludeForMethodAnnotation.add("Lcom/sunyuan/debounce/lib/IgnoreClickDeBounce;")
   ```

8. methodEntities :  需要防抖的事件信息，即想要处理哪些事件防抖。除了**includeForMethodAnnotation**方法级别白名单外，代码中只有匹配methodEntities声明的事件信息才会防抖。

   **注意:插件中默认添加了`View.OnClickListener#onClick`事件信息，所以如果只是处理View的OnClickListener事件防抖，不需要声明methodEntities和添加事件信息。**

   假如我们想处理`ListView#onItemClick`事件防抖，那么只在methodEntities声明`ListView#onItemClick`事件信息即可。当然你还可以添加其他类型事件信息。

   ```groovy
    methodEntities {
        onItemClick {//随便填写，在methodEntities只要唯一,就像你在写productFlavors
            methodName 'onItemClick'//方法名称
            methodDesc '(Landroid/widget/AdapterView;Landroid/view/View;IJ)V'//方法描述
            interfaceName 'android/widget/AdapterView\$OnItemClickListener' //事件方法所在的接口名
        }
    }
   ```

   **注意:`methodEntities`中声明的事件信息都是事件的字节码信息**

## 4.运行说明

集成完毕后需要同步下，插件会输出如下日志：

  ```json
------------------debounce plugin config info--------------------
{
    "isDebug": true,
    "generateReport": true,
    "checkTime": 500,
    "includes": [
        
    ],
    "excludes": [
        "com/example/gradleplugin/excludes/*",
        "androidx/**/*",
        "android/**/*",
        "com/google/android/**/*"
    ],
    "includeForMethodAnnotation": [
        "Lbutterknife/OnClick;",
        "Lbutterknife/OnItemClick;",
        "Lcom/sunyuan/debounce/lib/ClickDeBounce;"
    ],
    "excludeForMethodAnnotation": [
        "Lcom/sunyuan/debounce/lib/IgnoreClickDeBounce;"
    ],
    "methodEntities": {
        "onItemClick": {
            "access": -1,
            "methodDesc": "(Landroid/widget/AdapterView;Landroid/view/View;IJ)V",
            "interfaceName": "android/widget/AdapterView$OnItemClickListener",
            "name": "onItemClick",
            "methodName": "onItemClick"
        }
    }
}
-----------------------------------------------------------------
  ```

运行apk或执行  `./gradlew clean`  `./gradlew  assembleDebug` 控制台输出插件执行耗时以及报告地址:

```java
> Task :app:transformClassesWithDebounceTransformForDebug
--------------------------------------------------------
DebounceTransform cost 2918ms
--------------------------------------------------------
--------------------------------------------------------
debounce-transform-report:xxx/app/build/reports/debounce-plugin/debug/modified-method-list.html
--------------------------------------------------------

```

打开报告,报告中详细列出了插件修改的方法:

![](http://m.qpic.cn/psc?/V51CSwpO1slVFI402aSY2YlJCy2S2DcR/bqQfVz5yrrGYSXMvKr.cqU69DlyLgGD4TtKgaP3Y5.H70w.bIE5CkzyPFY7A83Pch6RuSrN92z5lAX5Q17UScXQ38vcNaDSeVV6EaBNjf3g!/b&bo=lgoGB5YKBgcDByI!&rf=viewer_4)

Demo运行起来后，点击页面上的按钮如图所示:

![image](http://m.qpic.cn/psc?/V51CSwpO1slVFI402aSY2YlJCy2S2DcR/bqQfVz5yrrGYSXMvKr.cqePcXuEw9lvqRGaaW*a*RiJ*aeD0x.m8m5uh2VEoSmXfM2XcpnwnYULiALIHhrRryZaHBtm.1*NLBaknbtXsofQ!/b&bo=3grwBd4K8AUDByI!&rf=viewer_4)

## 5.FAQ

### 5.1 插件提供了include, exclude,includeForMethodAnnotation和excludeForMethodAnnotation，他们在事件防抖功能中起到什么作用，以及他们之间的优先级是怎样的？

#### 5.1.1 背景

插件提供`include`，`exclude`，`includeForMethodAnnotation`和`excludeForMethodAnnotation` 主要解决事件防抖个性化的场景，不是每个app都需要全局处理事件防抖。

于是有了`include`和`exclude`用于处理或排除文件级别的事件防抖。那还有一种场景是某个方法不需要防抖，于是插件提供了,`includeForMethodAnnotation`和`excludeForMethodAnnotation`用于处理或排除方法级别的防抖。

#### 5.1.2 优先级

`exclude`优先级高于`include`；`excludeForMethodAnnotation`优先级高于`includeForMethodAnnotation`。

分为两个步骤:

1. 插件执行时会遍历所有class文件，根据`exclude`的配置排除某些class文件处理，剩余的class文件再根据`include`配置判断是否需要处理
2. 第一步结束后会得到需要处理的class文件，然后遍历每一个class的method列表，通过`excludeForMethodAnnotation`的配置排除某个方法处理，剩余的method再根据`includeForMethodAnnotation`配置判断是否需要处理

### 5.2 为什么修改了debounce配置没有生效?

修改debounce任何配置都需要Build->clean Project，然后在运行项目。否则新修改的配置不会生效

### 5.3 为什么生成的报告不全？

这是因为插件支持增量更新，哪个文件改动了，只会处理该文件的事件防抖，所以输出的报告只有该文件中的防抖(修改)的方法信息，如果要产生全量的方法防抖(修改)报告，需要Build->clean Project，然后运行项目。

### 5.4 如何查看插件修改后的代码?

查看路径：app\build\intermediates\transforms\DebounceTransform\xxx

### 5.5 如何关闭插件功能？

在工程的`gradle.properties`中配置

```properties
#关闭防抖动插件
debounceEnable=false
```

同步gradle,日志输出:

```
debounce-plugin is off.
```

说明插件功能已关闭

### 5.6 如何对ButterKnife等三方APT设置的点击事件防抖处理？

APT会生成模版类，在模版类中依然使用的是原生的点击事件。既然是原生的点击事件，那插件根据`methodEntities`配置信息就能处理，为什么还要说明下**如何对ButterKnife等三方APT设置的点击事件防抖处理**呢？

假设项目中使用了`ButterKnife`,只想对主工程下的代码事件防抖，其他模块下的不处理。按照插件配置规则需要配置`include[$主工程代码路径]`。插件根据include配置的路径筛选出处理的class时

由于`ButterKinfe`通过APT设置的`onClick`使用`DebouncingOnClickListener`包装了一层。如下所示:

```java
  @UiThread
  public MainActivity_ViewBinding(final MainActivity target, View source) {
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onClick();
      }
    });
  }
```

```java
package butterknife.internal;

public abstract class DebouncingOnClickListener implements View.OnClickListener {
  private static final Runnable ENABLE_AGAIN = () -> enabled = true;
  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  static boolean enabled = true;
  @Override public final void onClick(View v) {
    if (enabled) {
      enabled = false;
      MAIN.post(ENABLE_AGAIN);
      doClick(v);
    }
  }
  public abstract void doClick(View v);
}
```

根据`include[$主工程代码路径]`配置，所以`DebouncingOnClickListener#onClick`不会插入防抖代码，即ButterKnife设置的点击事件不会有防抖功能

需要配置以下策略,以正确处理ButterKnife设置的事件防抖

```groovy
debounce {
    includes = [$主工程代码路径]
    /**
     * 排除ButterKnife生成的模版类
     * ButterKnife事件防抖由includeForMethodAnnotation保证
     */
    excludes = ["**/*_ViewBinding*.class"]

    /**
     * 声明在方法上的的这些注解都需要插桩
     * 比如处理ButterKnife OnClick和OnItemClick点击事件
     */
    includeForMethodAnnotation = ["Lbutterknife/OnClick;",
                                  "Lbutterknife/OnItemClick;"]
}
```

### 5.7 如何对xml中设置的点击事件防抖处理？

#### 5.7.1 原理

在View源码中会解析xml属性，如设置了onClick属性，会创建一个`DeclaredOnClickListener`设置给当前View,在收到点击时间时，反射调用xml中onClick属性指定的方法。**由于View是android.jar包下的类，只参与编译，所以无法利用插桩对android.jar下的类插入自己的代码**。

僵硬，难道走不通了吗？其实不然，我们现在的Activity都是继承AppCompatActivity,在AppCompatActivity对LayoutInflater设置自定义解析，如果xml中设置了onClick，则会创建一个`DeclaredOnClickListener`设置给当前View,在收到点击时间时，反射调用xml中onClick指定的方法。看起来好像跟android.jar包下View的处理一样，但是**AppCompatActivity是androidx.appcompat:appcompat:x.y.z 包下的，这样我们就可以愉快的插入自己的代码了。**

#### 5.7.2 处理

5.6.1中描述了xml中设置的点击事件防抖处理原理，一般情况下无需配置，插件已帮你处理了。但是如果事件防抖处理有严格的规则，即如果只想处理主工程下的事件的防抖，那么这种情况下就需要特殊配置了,和ButterKnife处理类似。

```groovy
debounce {
    includes = [$主工程代码路径]
}
```

**注意:和`ButterKnife`处理不同的是无需声明`includeForMethodAnnotation`注解，插件内部默认添加了`ClickDeBounce`注解。所以在xml中声明的点击事件方法上添加`ClickDeBounce`注解就可以了**

```java
/**
 * xml中设置的点击事件
 */
@ClickDeBounce
public void reflectOnClick(View view) {
    LogUtil.d("xml设置onClick事件");
}
```

### 5.8 如何排除某个事件方法防抖处理？

在不需要防抖的事件方法上声明`@IgnoreClickDeBounce`注解

```java
findViewById(R.id.btn_ignore_click_debounce).setOnClickListener(new View.OnClickListener() {
    @IgnoreClickDeBounce
    @Override
    public void onClick(View v) {
        LogUtil.d("忽略点击防抖");
    }
});
```



## 6.更新日志

## 1.2.0

1. 支持Library中使用debounce-plugin 

## 1.1.3

1. 适配JDK9,11,17 inDy指令解析 [#6](https://github.com/sy007/DebouncePlugin/issues/6)

## 1.1.2

1. 修复checkTime配置无效问题 [#6](https://github.com/sy007/DebouncePlugin/issues/7#issuecomment-1296265701)

## 1.1.1

1. 修复Lambda表达式字节码解析异常 [#6](https://github.com/sy007/DebouncePlugin/issues/6)
2. 修复kotlin方法引用异常 [#7](https://github.com/sy007/DebouncePlugin/issues/7)

### 1.1.0

1. 解决D8 warning:Expected stack map table for method with non-linear control flow
2. transform性能提升50%（感谢booster）

### 1.0.2

1. 完善lambda表达式插桩处理
2. 解决kotlin plugin 1.7.10版本，lambda实例方法引用插桩失败问题

### 1.0.1

修复ClassReader.superName为空问题

### 1.0.0

1. 新增ClickDeBounce和IgnoreClickDeBounce注解,用于声明方法是否需要插桩
2. 将debounceCheckTime命名修改为checkTime
3. 新增includeForMethodAnnotation和excludeForMethodAnnotation配置, 用于控制方法是否插桩
4. 新增方法修改报告
5. 多线程优化插桩速度

### 0.4.1

新增`gradle.properties`配置属性`debounceEnable`是否关闭插件功能

### 0.3.0

升级`debounce-plugin`依赖的ASM版本到7.3.1版本

### 0.2.0

1. 修复自定义View中运行时点击按钮出现闪退问题

- 针对子类重写父类的方法时,子类和父类中都插入判断代码

 - 构造函数中创建检测工具类延迟到点击方法被调用时

2. 调整日志打印

### 0.1.0

项目初始化