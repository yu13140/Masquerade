package com.yu13140.masquerade;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class XposedHook implements IXposedHookLoadPackage {
    private static final String CONFIG_PATH = "/data/data/com.yu13140.masquerade/xposed_config.json";
    private long lastModifiedTime = 0;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        JSONObject config = loadConfig();
        if (config == null) return;

        String targetApp = config.optString("targetApp", "");
        if (!lpparam.packageName.equals(targetApp)) {
            return;
        }

        final String propName = config.optString("systemProperty", "ro.boot.flash.locked");
        final String fakeValue = config.optString("fakeValue", "0");

        XposedBridge.log("[Masquerade] Hooking " + lpparam.packageName + " - " + propName + "=" + fakeValue);

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
                    if (propName.equals(key)) {
                        param.setResult(fakeValue);
                        XposedBridge.log("[Masquerade] Hooked: " + key + "=" + fakeValue);
                    }
                }
            }
        );

        hideXposed(lpparam);
        NativeHook.init();
    }

    private JSONObject loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                XposedBridge.log("[Masquerade] 配置文件不存在: " + CONFIG_PATH);
                return null;
            }

            long newModifiedTime = file.lastModified();
            if (newModifiedTime == lastModifiedTime) {
                return null;
            }
            lastModifiedTime = newModifiedTime;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            return new JSONObject(sb.toString());
        } catch (Exception e) {
            XposedBridge.log("[Masquerade] 读取配置失败: " + e.getMessage());
            return null;
        }
    }

    private void hideXposed(XC_LoadPackage.LoadPackageParam lpparam) {
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