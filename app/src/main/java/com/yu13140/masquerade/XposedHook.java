package com.yu13140.masquerade;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.content.Context;
import android.content.SharedPreferences;

public class XposedHook implements IXposedHookLoadPackage {
    private static String targetPackage;
    private static String propName;
    private static String propValue;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // 修复步骤：添加类型转换和空检查
        Object application = XposedHelpers.callStaticMethod(
            XposedHelpers.findClass("android.app.AppGlobals", null),
            "getInitialApplication"
        );
        
        if (application instanceof Context) {
            Context context = (Context) application;
            SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);

            targetPackage = prefs.getString("target_pkg", "");
            propName = prefs.getString("prop_name", "ro.boot.flash.locked");
            propValue = prefs.getString("prop_value", "1");

            if (!lpparam.packageName.equals(targetPackage))
                return;

            // Hook SystemProperties.get()
            XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                lpparam.classLoader,
                "get",
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (propName.equals(param.args[0])) {
                            param.setResult(propValue);
                        }
                    }
                }
            );

            // 可选：Hook getInt方法
            XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                lpparam.classLoader,
                "getInt",
                String.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (propName.equals(param.args[0])) {
                            param.setResult(Integer.parseInt(propValue));
                        }
                    }
                }
            );
        }
    }
}