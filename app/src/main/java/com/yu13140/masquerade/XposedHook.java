package com.yu13140.masquerade;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.util.List;

public class XposedHook implements IXposedHookLoadPackage {
   
    private JSONObject loadConfig() {
        try {            
            XSharedPreferences prefs = new XSharedPreferences("com.yu13140.masquerade", "xposed_config");
            prefs.makeWorldReadable();
            prefs.reload();

            String targetApp = prefs.getString("targetApp", "");
            String systemProperty = prefs.getString("systemProperty", "ro.boot.flash.locked");
            String fakeValue = prefs.getString("fakeValue", "0");

            JSONObject config = new JSONObject();
            config.put("targetApp", targetApp);
            config.put("systemProperty", systemProperty);
            config.put("fakeValue", fakeValue);
            return config;
        } catch (Exception e) {
            XposedBridge.log("[Masquerade] 读取配置失败: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        JSONObject config = loadConfig();
        if (config == null) return;

        String targetApp = config.optString("targetApp", "");
        String propName = config.optString("systemProperty", "ro.boot.flash.locked");
        String fakeValue = config.optString("fakeValue", "0");
        
        if (!lpparam.packageName.equals(targetApp)) {
            return;
        }

        XposedBridge.log("[Masquerade] Hooking " + lpparam.packageName + " - " + propName + "=" + fakeValue);
        hookSystemProperties(lpparam, propName, fakeValue);
        hookRuntimeExec(lpparam, propName, fakeValue);
        hideXposed(lpparam);
    }

    private void hookSystemProperties(XC_LoadPackage.LoadPackageParam lpparam, String propName, String fakeValue) {
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
                        XposedBridge.log("[Masquerade] Hooked SystemProperties: " + key + " -> " + fakeValue);
                        param.setResult(fakeValue);
                    }
                }
            }
        );
    }

    private void hookRuntimeExec(XC_LoadPackage.LoadPackageParam lpparam, String propName, String fakeValue) {
        XC_MethodHook execHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String cmd = param.args[0].toString();
                if (cmd.equals("getprop " + propName)) {
                    XposedBridge.log("[Masquerade] Hooked Runtime.exec(): " + cmd);
                    param.setResult(fakeProcess(fakeValue));
                }
            }
        };

        XposedHelpers.findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "exec", String.class, execHook);
        XposedHelpers.findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "exec", String[].class, execHook);

        XposedHelpers.findAndHookMethod("java.lang.ProcessBuilder", lpparam.classLoader, "start", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                ProcessBuilder builder = (ProcessBuilder) param.thisObject;
                List<String> cmd = builder.command();
                if (cmd.contains("getprop") && cmd.contains(propName)) {
                    XposedBridge.log("[Masquerade] Hooked ProcessBuilder.start(): " + cmd);
                    param.setResult(fakeProcess(fakeValue));
                }
            }
        });
    }

    private Process fakeProcess(String fakeValue) {
        return new Process() {
            @Override public OutputStream getOutputStream() { return null; }
            @Override public InputStream getInputStream() {
                return new ByteArrayInputStream((fakeValue + "\n").getBytes());
            }
            @Override public InputStream getErrorStream() { return null; }
            @Override public int waitFor() { return 0; }
            @Override public int exitValue() { return 0; }
            @Override public void destroy() {}
        };
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