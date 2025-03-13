package com.yu13140.masquerade;

public class NativeHook {
    static {
        System.loadLibrary("nativehook");
    }

    public static native void init();
}