# DebouncePlugin

Android点击事件防抖动插件，主要为了解决项目中以及第三方库中快速点击问题。

## 1.支持以下功能：

1. 支持Lambda点击事件
2. 支持排除指定目录或指定文件，就跟写gitignore一样简单
3. 支持配置点击事件间隔，即规定时间内，只允许触发一次点击事件
4. 支持多种类型的点击事件处理。例如：
   - ListView#onItemClick
   - ListView#onItemSelected
   - ExpandableListView#onGroupClick
   - ExpandableListView#onChildClick
   - ...只要你想处理，都支持。
5. 更详细的日志输出，插件对哪些方法进行Hook，都会输出到控制台

## 2.如何使用

1. 在你项目的build.gradle依赖如下：

```groovy
buildscript {
    ext.kotlin_version = "1.3.72"
    repositories {
        google()
        jcenter()
    }
    dependencies {
        //依赖插件所需的环境
        classpath 'com.sunyuan:debounce-plugin:0.1.2'
    }
}
```

2. 在app module中依赖如下：

```groovy
dependencies {
    //插件所需的依赖库
    implementation 'com.sunyuan:debounce-lib:0.1.0'
}
```

这样插件就能正常工作了。无任何配置情况下，插件只会对全局所有onClick事件处理(项目，library,第三方jar和arr)。

## 3.自定义配置

插件支持自定义配置，在你的app#build.gradle配置如下：

```
debounce {
    isDebug = true
    debounceCheckTime = 1000
    excludes = ["com/example/gradleplugin/excludes/*"]
    methodEntities {
        onItemClick {//随便填写，在methodEntities只要唯一,就像你在写productFlavors
            methodName 'onItemClick'//方法名称
            methodDesc '(Landroid/widget/AdapterView;Landroid/view/View;IJ)V'//方法描述
            interfaceName 'android/widget/AdapterView\$OnItemClickListener' //事件方法所在的接口名
        }
    }
}
```

1. isDebug : 为true时会有日志输出

2. debounceCheckTime：两次点击间隔超过多长时间就认定为非抖动，单位毫秒。

3. excludes : 排除哪些目录或文件的插桩,类似于.gitignore 编写规则

4. methodEntities : 需要插桩的方法信息，只有声明在methodEntities中的方法信息才会被插桩。

   注意:插件中默认注册了onClick方法信息，所以如果只是处理View的OnClickListener点击抖动，不需要声明methodEntities并添加方法信息，插件内部会自动处理。

   这里以ListView#onItemClick为例子;当然你可以添加其他类型点击事件，比如ExpandableListView子条目点击事件。

   ```groovy
    methodEntities {
        onItemClick {//随便填写，在methodEntities只要唯一,就像你在写productFlavors
            methodName 'onItemClick'//方法名称
            methodDesc '(Landroid/widget/AdapterView;Landroid/view/View;IJ)V'//方法描述
            interfaceName 'android/widget/AdapterView\$OnItemClickListener' //事件方法所在的接口名
        }
    }
   ```

## 4.日志说明

集成完毕后需要同步下，插件会输出如下日志：

  ```json
╔═══════════════════════════════════════════════════════════════════════════════════════════════════
║                                      Debounce  configInfo
╟───────────────────────────────────────────────────────────────────────────────────────────────────
║{
    "isDebug": true,
    "excludes": [
        "com/example/gradleplugin/excludes/*"
    ],
    "methodEntities": {
        "onItemClick": {
            "methodDesc": "(Landroid/widget/AdapterView;Landroid/view/View;IJ)V",
            "interfaceName": "android/widget/AdapterView$OnItemClickListener",
            "methodName": "onItemClick"
        }
    },
    "debounceCheckTime": 1000
}
╚═══════════════════════════════════════════════════════════════════════════════════════════════════
  ```

上面日志的输出说明配置没问题，插件在被应用时会打印外部配置的信息。

在运行apk时，插件会详细输出哪些方法被Hook了，以便开发者明确知道。这里以Demo中的MainActivity为例:

```java
╔═══════════════════════════════════════════════════════════════════════════════════════════════════
║className:com.example.gradleplugin.MainActivity
╟───────────────────────────────────────────────────────────────────────────────────────────────────
║methods:
║	<init>()V
║	onClick(Landroid/view/View;)V
║	lambda$onCreate$1(Landroid/view/View;)V
║	lambda$onCreate$0(Landroid/view/View;)V
╚═══════════════════════════════════════════════════════════════════════════════════════════════════
```

Demo运行起来后，点击页面上的按钮:

![](http://m.qpic.cn/psc?/V51CSwpO1slVFI402aSY2YlJCy2S2DcR/TmEUgtj9EK6.7V8ajmQrENwO0OadvJR9vCme0YQ.NpGJ*VJFmcwR7laCrXuNggZJqV95TQeUdlcx5wvLW6cF7afdHKxmsPfgUGReM6n8Lzw!/b&bo=0QRFAtEERQIDGTw!&rf=viewer_4)

## 5.注意事项

凡是修改以下结点的任何信息，都需要Build->clean Project，然后在运行项目。否则新修改的配置不会生效。

```groovy
debounce {
    isDebug = true
    debounceCheckTime = 500
    excludes = ["com/example/gradleplugin/excludes/*"]
    methodEntities {
        onItemClick {
            methodName 'onItemClick'
            methodDesc '(Landroid/widget/AdapterView;Landroid/view/View;IJ)V'
            interfaceName 'android/widget/AdapterView\$OnItemClickListener'
        }
    }
}
```

如何查看插件修改过后的代码：

查看路径：app\build\intermediates\transforms\DebounceTransform\xxx

