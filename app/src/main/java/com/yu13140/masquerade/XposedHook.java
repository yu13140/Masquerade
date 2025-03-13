package com.yu13140.masquerade;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedHook implements IXposedHookLoadPackage {
    private static final String TARGET_PROP = "ro.boot.flash.locked";
    private static final String FAKE_VALUE = "0";
    private XC_LoadPackage.LoadPackageParam lpparam;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        this.lpparam = lpparam;

        if (isSystemProcess(lpparam.packageName)) return;

        XposedHelpers.findAndHookMethod(
            "android.os.SystemProperties",
            lpparam.classLoader,
            "get",
            String.class,
            String.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    if (TARGET_PROP.equals(key)) {
                        param.setResult(FAKE_VALUE);
                        XposedBridge.log("[Masquerade] Java层Hook成功: " + key + "=" + FAKE_VALUE);
                    }
                }
            }
        );

        NativeHook.init();
        hideXposed();
    }
    
    private boolean isSystemProcess(String packageName) {
        return packageName.startsWith("android.") 
               || packageName.startsWith("com.android.")
               || packageName.equals("android");
    }

    private void hideXposed() {
        if (lpparam == null) return;
        XposedHelpers.findAndHookMethod(
            "de.robv.android.xposed.XposedBridge",
            lpparam.classLoader,
            "isXposedEnabled",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(false);
                }
            }
        );
    }
}