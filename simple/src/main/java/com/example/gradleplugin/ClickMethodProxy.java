package com.example.gradleplugin;

import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;

import com.sunyuan.debounce.lib.BounceChecker;
import com.sunyuan.debounce.lib.AnnotationMethodProxy;
import com.sunyuan.debounce.lib.ClickDeBounce;
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
     * 处理{@link AdapterView.OnItemClickListener#onItemClick(AdapterView, View, int, long)} 点击事件防抖
     * <p>
     * 根据{@link InterfaceMethodProxy}注解上的配置，插件扫描到{@link AdapterView.OnItemClickListener#onItemClick(AdapterView, View, int, long)}时
     * 会调用该方法,你可以从{@link MethodHookParam}中取出点击事件所属的类和方法名以及参数来做防抖判断
     *
     * @param param 事件方法描述
     * @return 返回true表示拦截，false则不拦截
     */
    @InterfaceMethodProxy(ownerType =
            AdapterView.OnItemClickListener.class,
            methodName = "onItemClick",
            parameterTypes = {AdapterView.class,
                    View.class,
                    int.class,
                    long.class
            }, returnType = void.class)
    public boolean onItemClickProxy(MethodHookParam param) {
        /**
         * {@link AdapterView.OnItemClickListener#onItemClick(AdapterView, View, int, long)}
         * 第三个参数position可以当作唯一标识，所以可以直接取出position
         */
        int position = (int) param.args[2];
        /**
         * 根据事件所属的类和以及事件方法和点击的position位置可以生成
         * 相同点击位置上的唯一标识
         */
        String uniqueId = param.owner + "|" + param.methodName + "|" + position;
        boolean isBounce = checker.checkAny(uniqueId, CHECK_TIME);
        LogUtil.d("onItemClickProxy=>" + "[isBounce:" + isBounce + ",checkTime:" + CHECK_TIME + "]");
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

