package com.corn.hook;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class StartActivityHookHelper {

    private StartActivityHookHelper() {

    }

    public static void hookStartActivity(Activity activity) {
        try {
            Field mInstrumentationField = Activity.class.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation instrumentationObject = (Instrumentation) mInstrumentationField.get(activity);


            // hook 方式1, 非全局Activity，与每个Activity对象相关
            //ProxyInstrumentation proxyInstrumentation = new ProxyInstrumentation(instrumentationObject);
            //mInstrumentationField.set(activity, proxyInstrumentation);

            // hooks 方式2, 全局hook
            Class<?> ActivityManagerClz = Class.forName("android.app.ActivityManager");
            Method getServiceMethod = ActivityManagerClz.getDeclaredMethod("getService");
            // 取得AMS实例
            final Object IActivityManagerObj = getServiceMethod.invoke(null);

            Class<?> IActivityManagerClz = Class.forName("android.app.IActivityManager");

            Object proxyIActivityManager = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[]{IActivityManagerClz}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            // proxy是创建出来的代理类，method是接口中的方法，args是接口执行时的实参
                            if (method.getName().equals("startActivity")) {
                                Log.d("ttt", "全局hook 到了 startActivity, excited!");
                            }
                            return method.invoke(IActivityManagerObj, args);
                        }
                    });

            // 替换对应持有者
            Field IActivityManagerSingletonField = ActivityManagerClz.getDeclaredField("IActivityManagerSingleton");
            IActivityManagerSingletonField.setAccessible(true);
            Object IActivityManagerSingletonObj = IActivityManagerSingletonField.get(null);

            // 反射创建一个Singleton的class
            Class<?> SingletonClz = Class.forName("android.util.Singleton");
            Field mInstanceField = SingletonClz.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            // 将实例对象替换成地代理对象
            mInstanceField.set(IActivityManagerSingletonObj, proxyIActivityManager);

        } catch (Exception e) {
            Log.e("ttt", "error", e);
        }

    }


    private static class ProxyInstrumentation extends Instrumentation {
        private Instrumentation sourceInstrumentation;

        public ProxyInstrumentation(Instrumentation sourceInstrumentation) {
            this.sourceInstrumentation = sourceInstrumentation;
        }

        public ActivityResult execStartActivity(
                Context who, IBinder contextThread, IBinder token, Activity target,
                Intent intent, int requestCode, Bundle options) {

            Log.d("ttt", "startActivity hook 到了.. excited!");

            try {
                // 非SDK API，但是目前仍在非受灰名单中，仍然可以使用
                Class<?> InstrumentationClz = Class.forName("android.app.Instrumentation");
                Method execStartActivity = InstrumentationClz.getDeclaredMethod("execStartActivity",
                        Context.class, IBinder.class, IBinder.class, Activity.class,
                        Intent.class, int.class, Bundle.class);

                return (ActivityResult) execStartActivity.invoke(sourceInstrumentation,
                        who, contextThread, token, target, intent, requestCode, options);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

}
