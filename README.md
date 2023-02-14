# DebouncePlugin

Android点击事件防抖动插件，主要为了解决项目以及第三方库中快速点击问题。

## 1.支持以下功能：

1. 支持application ,library 中使用

2. 支持Java,Kotlin点击事件防抖

3. 支持Java,Kotlin Lambda点击事件防抖

4. 支持排除或处理指定路径下的代码防抖处理**(文件级黑白名单)**，就跟写gitignore一样简单

5. 支持排除或处理指定方法防抖处理**(方法级黑白名单)**，两个注解解决你的问题

7. 支持多种类型的点击事件处理。例如：
   - ListView#onItemClick
   - ListView#onItemSelected
   - ExpandableListView#onGroupClick
   - ExpandableListView#onChildClick
   - ...只要你想处理，都支持。
   
7. 支持xml设置的点击事件防抖

8. 支持ButterKnife,XUtils等三方APT设置的点击事件防抖

9. 支持自定义防抖处理

   1. 一段时间内，只允许触发一次点击事件，时间多久你说了算

   2. 可以每个点击事件防抖状态唯一，也可以全局共享一个防抖状态
   3. 支持运行时二次拦截处理
   4. 甚至可以做到全局点击事件埋点

10. 代码修改透明(插件对代码的修改会生成一个html报告)

## 2.如何使用

1. 在你项目的build.gradle依赖如下：

```groovy
buildscript {
    ...
    dependencies {
        //依赖插件所需的环境
        classpath 'io.github.sy007:debounce-plugin:2.0.0'
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
//插件配置
debounce {
  proxyClassName = "$配置自定义代理类"
}
dependencies {
    //插件所需的依赖库
    implementation 'io.github.sy007:debounce-lib:2.0.0'
}
```



## 3.自定义配置

插件支持自定义配置，在你的app#build.gradle配置如下：

```groovy
debounce {
    proxyClassName = "$配置自定义代理类"
    generateReport = true
    includes = ["$填写需要事件防抖的目录或文件"]
    excludes = ["$填写不需要事件防抖的目录或文件"]
    excludeForMethodAnnotation = ["$填写不需要事件防抖的方法上注解信息"]
}
```

| 参数                       | 是否必须 |
| -------------------------- | -------- |
| proxyClassName             | 是       |
| generateReport             | 否       |
| includes                   | 否       |
| excludes                   | 否       |
| excludeForMethodAnnotation | 否       |

1. proxyClassName: 自定义代理类，其目的是为了配置hook信息。比如在你工程下创建`ClickMethodProxy.java`类。

   ```java
   package com.example.gradleplugin;
   import android.view.View;
   import android.widget.AdapterView;
   import com.sunyuan.debounce.lib.BounceChecker;
   import com.sunyuan.debounce.lib.ClickDeBounce;
   import com.sunyuan.debounce.lib.AnnotationMethodProxy;
   import com.sunyuan.debounce.lib.InterfaceMethodProxy;
   import com.sunyuan.debounce.lib.MethodHookParam;
   import butterknife.OnClick;
   import butterknife.OnItemClick;
   /**
    * @author sy007
    * @date 2023/01/17
    * @description
    */
   public class ClickMethodProxy {
   
       /**
        * 多长事件内只触发一次点击事件
        */
       private static final long CHECK_TIME = 1000;
   
       /**
        * 防抖判断工具类
        */
       private final BounceChecker checker = new BounceChecker();
   
       /**
        * 处理{@link View.OnClickListener#onClick(View)}点击事件防抖
        * <p>
        * 根据{@link InterfaceMethodProxy}注解上的配置，插件扫描到{@link View.OnClickListener#onClick(View)}时
        * 会调用该方法，你可以从{@link MethodHookParam}中取出点击事件所属的类和方法名以及参数来做防抖判断
        *
        * @param param 事件方法描述
        * @return 返回true表示拦截，false则不拦截
        */
       @InterfaceMethodProxy(
               ownerType = View.OnClickListener.class,
               methodName = "onClick",
               parameterTypes = {View.class},
               returnType = void.class)
       public boolean onClickProxy(MethodHookParam param) {
           /**
            * {@link View.OnClickListener#onClick(View)}只有一个参数View，所以直接取
            */
           if (param.args[0] instanceof CheckBox) {
               return false;
           }
           View view = (View) param.args[0];
           boolean isBounce = checker.checkView(param.owner, param.methodName, view, CHECK_TIME);
           LogUtil.d("onClickProxy=>" + "[isBounce:" + isBounce + ",checkTime:" + CHECK_TIME + "]");
           return isBounce;
       }
     
     
    	 /**
        * 处理xml中设置的点击事件防抖
        * <p>
        * 根据{@link AnnotationMethodProxy}注解上的配置，插件扫描到声明{@link ClickDeBounce}注解的方法时
        * 会调用该方法,你可以从{@link MethodHookParam}中取出点击事件所属的类和方法名以及参数来做防抖判断
        * <p>
        * 注意:{@link ClickDeBounce}注解必须声明在有切仅有一个View参数的方法上，这个注解是为了解决xml中设置的点击事件防抖
        *
        * @param param 事件方法描述
        * @return 返回true表示拦截，false则不拦截
        */
       @AnnotationMethodProxy(type = ClickDeBounce.class)
       public boolean onClickDeBounceAnnotationProxy(MethodHookParam param) {
           /**
            * {@link ClickDeBounce}声明在有切仅有一个View参数的方法上，所以直接取
            */
           View view = (View) param.args[0];
           boolean isBounce = checker.checkView(param.owner, param.methodName, view, CHECK_TIME);
           LogUtil.d("onClickDeBounceAnnotationProxy=>" + "[isBounce:" + isBounce + ",checkTime:" + CHECK_TIME + "]");
           return isBounce;
       }
   }
   
   ```

   那么`proxyClassName`填写就是`com.example.gradleplugin.ClickMethodProxy`,插件会根据`ClickMethodProxy`中的注解配置来决定hook哪些点击事件并调用这些方法。

   根据`ClickMethodProxy`代码中定义，插件提供两个注解:

   1. InterfaceMethodProxy: 配置需要hook的事件信息

   ```java
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface InterfaceMethodProxy {
       //事件所属的接口类型
       Class<?> ownerType();
   
       //事件所属的方法名
       String methodName();
   
       //事件所属的方法参数列表类型
       Class<?>[] parameterTypes();
   
       //事件方法的返回类型
       Class<?> returnType();
   }
   ```

   2. AnnotationMethodProxy: 配置需要hook的方法(**方法上声明AnnotationMethodProxy配置的注解，都会被hook**)

   ```java
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface AnnotationMethodProxy {
       //注解类型
       Class<? extends Annotation> type();
   }
   ```

   **注意:自定义`ClickMethodProxy`中被InterfaceMethodProxy和AnnotationMethodProxy修饰的方法必须满足下面三个条件:**

   - **非静态**
   - **方法参数必须是MethodHookParam且只有一个参数**
   - **返回值必须是boolean**

2. generateReport：是否生成方法修改报告，即插件修改的方法会生成一份html报告，报告路径:app/build/reports/debounce-plugin/${buildType}/modified-method-list.html

3. includes： 处理指定路径下的代码事件防抖(文件级白名单),类似于.gitignore 编写规则

4. excludes：排除指定路径下的代码事件防抖(文件级黑名单),类似于.gitignore 编写规则

5. excludeForMethodAnnotation：方法级别黑名单,方法上声明了这些注解，那么该方法不会插入防抖代码。插件内部默认添加了` IgnoreClickDeBounce`注解，即声明在方法上的`IgnoreClickDeBounce`注解，都不会插入防抖代码。**注意:这里配置的是注解的字节码**

   ```kotlin
   //插件内部自动添加了IgnoreClickDeBounce注解
   excludeForMethodAnnotation.add("Lcom/sunyuan/debounce/lib/IgnoreClickDeBounce;")
   ```

## 4.运行说明

集成完毕后需要同步下，插件会输出如下日志：

  ```json
------------------debounce plugin config info--------------------
{
    "generateReport": true,
    "proxyClassName": "com.example.gradleplugin.ClickMethodProxy",
    "includes": [
        
    ],
    "excludes": [
        "com/example/gradleplugin/excludes/*",
        "androidx/**/*",
        "android/**/*",
        "com/google/android/**/*",
        "butterknife/internal/DebouncingOnClickListener.class",
        "**/*_ViewBinding*.class"
    ],
    "excludeForMethodAnnotation": [
        "Lcom/sunyuan/debounce/lib/IgnoreClickDeBounce;"
    ]
}
-----------------------------------------------------------------
  ```

运行apk或执行  `./gradlew clean`  `./gradlew  assembleDebug` 

在执行过程中控制台会输出自定义代理类解析后的hook信息

```json
------------------proxy class config info--------------------
{
    "owner": "com/example/gradleplugin/ClickMethodProxy",
    "annotationIndex": {
        "Lcom/sunyuan/debounce/lib/ClickDeBounce;": {
            "methodDesc": "(Lcom/sunyuan/debounce/lib/MethodHookParam;)Z",
            "methodName": "onClickDeBounceAnnotationProxy"
        },
        "Lbutterknife/OnItemClick;": {
            "methodDesc": "(Lcom/sunyuan/debounce/lib/MethodHookParam;)Z",
            "methodName": "onItemClickWithButterKnifeProxy"
        },
        "Lbutterknife/OnClick;": {
            "methodDesc": "(Lcom/sunyuan/debounce/lib/MethodHookParam;)Z",
            "methodName": "onClickWithButterKnifeProxy"
        }
    },
    "methodIndex": [
        {
            "samMethodEntity": {
                "owner": "android/view/View$OnClickListener",
                "methodDesc": "(Landroid/view/View;)V",
                "methodName": "onClick"
            },
            "proxyMethodEntity": {
                "methodDesc": "(Lcom/sunyuan/debounce/lib/MethodHookParam;)Z",
                "methodName": "onClickProxy"
            }
        },
        {
            "samMethodEntity": {
                "owner": "android/widget/AdapterView$OnItemClickListener",
                "methodDesc": "(Landroid/widget/AdapterView;Landroid/view/View;IJ)V",
                "methodName": "onItemClick"
            },
            "proxyMethodEntity": {
                "methodDesc": "(Lcom/sunyuan/debounce/lib/MethodHookParam;)Z",
                "methodName": "onItemClickProxy"
            }
        }
    ]
}
```

插件执行完毕后控制台输出插件执行耗时以及修改报告地址:

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

![image](http://m.qpic.cn/psc?/V11vVsP84HfNn2/bqQfVz5yrrGYSXMvKr.cqYpqJxqZga9c8eRhMoRWXwHxrrSsyw*fZlgaKBa76ZLChc7DBNiVUQG1NL3wYexkfna5GwRPuhxhkk*cEm4Ena4!/b&bo=6AooBugKKAYDByI!&rf=viewer_4)

## 5.FAQ

### 5.1 插件提供了include, exclude和excludeForMethodAnnotation，他们在事件防抖中起到什么作用，以及他们之间的优先级是怎样的？

#### 5.1.1 背景

插件提供`include`，`exclude`，和`excludeForMethodAnnotation` 主要解决事件防抖个性化的场景，不是每个应用都需要处理全局事件防抖。

于是有了`include`和`exclude`用于处理或排除文件级别的事件防抖。那还有一种场景是某个方法不需要防抖，于是插件提供了`excludeForMethodAnnotation`排除方法级别的防抖。

#### 5.1.2 优先级

`exclude`优先级高于`include`

`include`优先级高于`excludeForMethodAnnotation`

分为两个步骤:

1. 插件执行时会遍历所有class文件，根据`exclude`的配置排除某些class文件处理，剩余的class文件再根据`include`配置判断是否需要处理
2. 第一步结束后会得到需要处理的class文件，然后遍历每一个class的method列表，通过`excludeForMethodAnnotation`的配置排除某个方法

### 5.2 为什么修改了debounce配置没有生效?

修改debounce任何配置都需要Build->clean Project，然后在运行项目，否则新修改的配置不会生效

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

APT会生成模版类，在模版类中依然使用的是原生的点击事件。既然是原生的点击事件，那插件根据自定义`ClickMethodProxy.java`中配置信息就能处理，为什么还要说明下**如何对ButterKnife等三方APT设置的点击事件防抖处理呢？**

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
    proxyClassName = "com.example.gradleplugin.ClickMethodProxy"
    includes = [$主工程代码路径]
    /**
     * 排除ButterKnife生成的模版类
     * ButterKnife事件防抖由自定义`ClickMethodProxy.java`中的配置保证
     */
    excludes = ["**/*_ViewBinding*.class"]
}
```

```java
package com.example.gradleplugin;
import android.view.View;
import com.sunyuan.debounce.lib.BounceChecker;
import com.sunyuan.debounce.lib.AnnotationMethodProxy;
import com.sunyuan.debounce.lib.MethodHookParam;
import butterknife.OnClick;
import butterknife.OnItemClick;
/**
 * @author sy007
 * @date 2023/01/17
 * @description
 */
public class ClickMethodProxy {
    /**
     * 多长事件内只触发一次点击事件
     */
    private static final long CHECK_TIME = 1000;

    /**
     * 防抖判断工具类
     */
    private final BounceChecker checker = new BounceChecker();

    /**
     * 处理ButterKnife OnItemClick点击事件防抖
     * <p>
     * 根据{@link AnnotationMethodProxy}注解上的配置，插件扫描到声明{@link OnItemClick}注解的方法时
     * 会调用该方法,你可以从{@link MethodHookParam}中取出点击事件所属的类和方法名以及参数来做防抖判断
     * <p>
     * 注意:{@link OnItemClick}属于ButterKnife中的注解，在使用时方法参数可以写，也可以不写，甚至不写全都行
     * 所以这里生成点击事件唯一标识只能拼接方法所属的类+方法名+方法参数{@link MethodHookParam#generateUniqueId()}
     *
     * @param param 事件方法描述
     * @return 返回true表示拦截，false则不拦截
     */
    @AnnotationMethodProxy(type = OnItemClick.class)
    public boolean onItemClickWithButterKnifeProxy(MethodHookParam param) {
        boolean isBounce = checker.checkAny(param.generateUniqueId(), CHECK_TIME);
        LogUtil.d("onItemClickWithButterKnifeProxy=>" + "[isBounce:" + isBounce + ",checkTime:" + CHECK_TIME + "]");
        return isBounce;
    }

    /**
     * 处理ButterKnife OnClick点击事件防抖
     * <p>
     * 根据{@link AnnotationMethodProxy}注解上的配置，插件扫描到声明{@link OnClick}注解的方法时
     * 会调用该方法,你可以从{@link MethodHookParam}中取出点击事件所属的类和方法名以及参数来做防抖判断
     * <p>
     * 注意:{@link OnClick}属于ButterKnife中的注解，在使用时方法参数可以写，也可以不写
     * <p>
     * 所以这里生成点击事件唯一标识的逻辑是事件方法的参数判断:
     * <p>
     * 有一个参数这个参数就是{@link View}调用{@link BounceChecker#checkView(String, String, View, long)}就可以了，
     * 没有参数调用{@link BounceChecker#checkAny(String, long)}
     *
     * @param param 事件方法描述
     * @return 返回true表示拦截，false则不拦截
     */
    @AnnotationMethodProxy(type = OnClick.class)
    public boolean onClickWithButterKnifeProxy(MethodHookParam param) {
        boolean isBounce;
        if (param.args.length != 0) {
            isBounce = checker.checkView(param.owner, param.methodName, (View) param.args[0], CHECK_TIME);
        } else {
            isBounce = checker.checkAny(param.generateUniqueId(), CHECK_TIME);
        }
        LogUtil.d("onClickWithButterKnifeProxy=>" + "[isBounce:" + isBounce + ",checkTime:" + CHECK_TIME + "]");
        return isBounce;
    }
}


```



### 5.7 如何对xml中设置的点击事件防抖处理？

#### 5.7.1 原理

在View源码中会解析xml属性，如设置了onClick属性，会创建一个`DeclaredOnClickListener`设置给当前View,在收到点击时间时，反射调用xml中onClick属性指定的方法。**由于View是android.jar包下的类，只参与编译，所以无法利用插桩对android.jar下的类插入自己的代码**。

僵硬，难道走不通了吗？其实不然，我们现在的Activity都是继承AppCompatActivity,在AppCompatActivity对LayoutInflater设置自定义解析，如果xml中设置了onClick，则会创建一个`DeclaredOnClickListener`设置给当前View,在收到点击时间时，反射调用xml中onClick指定的方法。看起来好像跟android.jar包下View的处理一样，但是**AppCompatActivity是androidx.appcompat:appcompat:x.y.z 包下的，这样我们就可以愉快的插入自己的代码了。**

#### 5.7.2 处理

5.6.1中描述了xml中设置的点击事件防抖处理原理，一般情况下无需配置，插件已帮你处理了。但是如果事件防抖处理有严格的规则。如果只想处理主工程下的事件的防抖，那么这种情况下就需要特殊配置了,和ButterKnife处理类似。

```java
/**
 * xml中设置的点击事件
 */
@ClickDeBounce
public void reflectOnClick(View view) {
    LogUtil.d("xml设置onClick事件");
}
```

```groovy
//插件配置
debounce {
    proxyClassName = "com.example.gradleplugin.ClickMethodProxy"
    includes = [$主工程代码路径]
}
```

```java
package com.example.gradleplugin;
import android.view.View;
import com.sunyuan.debounce.lib.BounceChecker;
import com.sunyuan.debounce.lib.AnnotationMethodProxy;
import com.sunyuan.debounce.lib.ClickDeBounce;
import com.sunyuan.debounce.lib.MethodHookParam;
/**
 * @author sy007
 * @date 2023/01/17
 * @description
 */
public class ClickMethodProxy {

    /**
     * 多长事件内只触发一次点击事件
     */
    private static final long CHECK_TIME = 1000;

    /**
     * 防抖判断工具类
     */
    private final BounceChecker checker = new BounceChecker();

    /**
     * 处理xml中设置的点击事件防抖
     * <p>
     * 根据{@link AnnotationMethodProxy}注解上的配置，插件扫描到声明{@link ClickDeBounce}注解的方法时
     * 会调用该方法,你可以从{@link MethodHookParam}中取出点击事件所属的类和方法名以及参数来做防抖判断
     * <p>
     * 注意:{@link ClickDeBounce}注解必须声明在有切仅有一个View参数的方法上，这个注解是为了解决xml中设置的点击事件防抖
     *
     * @param param 事件方法描述
     * @return 返回true表示拦截，false则不拦截
     */
    @AnnotationMethodProxy(type = ClickDeBounce.class)
    public boolean onClickDeBounceAnnotationProxy(MethodHookParam param) {
        /**
         * {@link ClickDeBounce}声明在有且仅有一个View参数的方法上，所以直接取
         */
        View view = (View) param.args[0];
        boolean isBounce = checker.checkView(param.owner, param.methodName, view, CHECK_TIME);
        LogUtil.d("onClickDeBounceAnnotationProxy=>" + "[isBounce:" + isBounce + ",checkTime:" + CHECK_TIME + "]");
        return isBounce;
    }
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

### 5.9 运行时二次拦截呢？

这个功能源自于一位老哥的反馈

![](http://photogz.photo.store.qq.com/psc?/V11vVsP84HfNn2/bqQfVz5yrrGYSXMvKr.cqQbTJqTscVPZ7nnrNG8dvTUxjuqR0GHINQfY**t3p13gOqQkpdOLyzsJcimqj2.J9C5xoB*9jRrgYdW.9xfMlek!/b&bo=ZgiAAmYIgAIDByI!&rf=viewer_4)

CheckBox显示状态和点击事件处理时获取的状态不一致。用户快速点击两次，页面上CheckBox从未选中状态->选中状态->未选中状态。而点击事件只执行了一次。此时点击事件中只执行了选中状态的事件。

这种情况如何处理呢？

很简单，只需在自定义代理类中代理onClick的方法中过滤掉CheckBox的防抖处理即可

```java
@InterfaceMethodProxy(
  ownerType = View.OnClickListener.class,
  methodName = "onClick",
  parameterTypes = {View.class},
  returnType = void.class)
public boolean onClickProxy(MethodHookParam param) {
  /**
  * {@link View.OnClickListener#onClick(View)}只有一个参数View，所以直接取
  */
  if (param.args[0] instanceof CheckBox) {
    //解决给CheckBox设置点击事件时页面显示状态和事件处理状态不一致问题，这里对CheckBox就不防抖处理了。
    return false;
  }
  View view = (View) param.args[0];
  boolean isBounce = checker.checkView(param.owner, param.methodName, view, CHECK_TIME);
  LogUtil.d("onClickProxy=>" + "[isBounce:" + isBounce + ",checkTime:" + CHECK_TIME + "]");
  return isBounce;
}
```

## 6.更新日志

## 2.0.0

1. 插件底层逻辑重构

2. 支持自定义防抖处理

3. 移除插件配置:

   1. isDebug 

   2. checkTime

   3. includeForMethodAnnotation 

   4. methodEntities 

      **以上被移除的配置，所表示的功能由自定义代理类代替**

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