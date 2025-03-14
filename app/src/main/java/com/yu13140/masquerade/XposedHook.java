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
import java.io.InputStream;
import java.util.List;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;

public class XposedHook implements IXposedHookLoadPackage {
    private static final String CONFIG_PATH = "/data/local/masquerade/xposed_config.json";

    private JSONObject loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                XposedBridge.log("[Masquerade] 配置文件不存在: " + CONFIG_PATH);
                return null;
            }

            XposedBridge.log("[Masquerade] 读取中，配置文件存在: " + CONFIG_PATH);

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

    private JSONObject loadConfig() {
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {
                XposedBridge.log("[Masquerade] 配置文件不存在: " + CONFIG_PATH);
                return null;
            }
            XposedBridge.log("[Masquerade] 读取中，配置文件存在: " + CONFIG_PATH);

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