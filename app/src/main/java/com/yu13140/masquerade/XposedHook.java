package com.yu13140.masquerade;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.content.Context;
import android.content.SharedPreferences;

public class XposedHook implements IXposedHookLoadPackage {
    private static String a;
    private static String b;
    private static String c;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {         
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Context ctx = (Context) XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.AppGlobals", null),
                    "getInitialApplication");
                
                SharedPreferences sp = ctx.getSharedPreferences(
                    "sys_conf", Context.MODE_PRIVATE);
                a = decrypt(sp.getString("a", ""));
                b = decrypt(sp.getString("b", "persist.sys.vold_app_data_isolation_enabled"));
                c = decrypt(sp.getString("c", "0"));

                if (lpparam.packageName.equals(a)) {
                    hookSystemProperties(lpparam.classLoader);
                }
            }, 3000);
        } catch (Throwable ignored) {}
    }

    private void hookSystemProperties(ClassLoader cl) {
        try {
            Class<?> sysProp = Class.forName("android.os.SystemProperties", false, cl); 
          
            XposedBridge.hookAllMethods(sysProp, "get", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (b.equals(param.args[0])) {
                        param.setResult(c);
                    }
                }
            });
           
            XposedBridge.hookAllMethods(sysProp, "getInt", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (b.equals(param.args[0])) {
                        param.setResult(Integer.parseInt(c));
                    }
                }
            });
        } catch (ClassNotFoundException ignored) {}
    }

    private static String decrypt(String input) {
        char[] key = {'X','P','O','S','E','D'};
        char[] output = new char[input.length()];
        for(int i=0; i<input.length(); i++) {
            output[i] = (char) (input.charAt(i) ^ key[i%key.length]);
        }
        return new String(output);
    }
       
    private static void antiDetection() {
        try {      
            XposedHelpers.findAndHookMethod(Process.class, "start", String.class, String.class, 
                int.class, int.class, int[].class, int.class, int.class, int.class, String.class, 
                String[].class, String[].class, String.class, String.class, String[].class, 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String niceName = (String) param.args[0];
                        if ("com.android.settings".equals(niceName)) {
                            param.args[1] = ((String) param.args[1]).replace("Xposed", "");
                        }
                    }
                });
        } catch (Throwable t) { /* 忽略异常 */ }
    }
}