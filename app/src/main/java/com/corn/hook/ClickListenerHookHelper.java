package com.corn.hook;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ClickListenerHookHelper {

    private ClickListenerHookHelper() {

    }

    public static void hookViewClickListener(final Context context, View view, final TextView tips) {
        try {
            Method getListenerInfoMethod = View.class.getDeclaredMethod("getListenerInfo");
            getListenerInfoMethod.setAccessible(true);
            Object listenerInfoObject = getListenerInfoMethod.invoke(view);

            Field onClickListenerFiled = listenerInfoObject.getClass().getField("mOnClickListener");
            final View.OnClickListener onClickListenerObject = (View.OnClickListener)onClickListenerFiled.get(listenerInfoObject);

            // 静态代理模式
            //ProxyOnClickListener proxyOnClickListener = new ProxyOnClickListener(onClickListenerObject, context, tips);

            // 动态代理模式
            Object proxyOnClickListener = Proxy.newProxyInstance(context.getClass().getClassLoader(), new Class[]{View.OnClickListener.class}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    // 调用 hook 内容
                    hookContent(context, tips);

                    // 执行被代理对象的原有逻辑
                    return method.invoke(onClickListenerObject, args);
                }
            });

            onClickListenerFiled.set(listenerInfoObject, proxyOnClickListener);
        } catch (Exception e) {
            Log.e("ttt", "error", e);
        }
    }


    /**
     * 静态代理模式，静态代理类
     *
     */
    static class ProxyOnClickListener implements View.OnClickListener {
        View.OnClickListener sourceOnClickListener;
        Context context;
        TextView tips;

        public ProxyOnClickListener(View.OnClickListener sourceOnClickListener, Context context, TextView tips) {
            this.sourceOnClickListener = sourceOnClickListener;
            this.context = context;
            this.tips = tips;
        }

        @Override
        public void onClick(View v) {
            // 调用 hook 内容
            hookContent(context, tips);

            // 执行被代理对象的原有逻辑
            if (sourceOnClickListener != null) {
                sourceOnClickListener.onClick(v);
            }
        }
    }

    public static void hookContent(Context context, TextView tips) {
        Log.d("ttt", "hook到了.. excited!");
        tips.setText("hook到了.. excited!");
    }
}
