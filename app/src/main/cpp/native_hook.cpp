#include <jni.h>
#include <string>
#include <dlfcn.h>
#include <android/log.h>

#define LOG_TAG "Masquerade"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

typedef int (*T_system_property_get)(const char*, char*, const char*);
static T_system_property_get orig_system_property_get = nullptr;

int system_property_get(const char* name, char* value, const char* default_value) {
    if (!orig_system_property_get) {
        void* handle = dlopen("libc.so", RTLD_NOW);
        orig_system_property_get = reinterpret_cast<T_system_property_get>(dlsym(handle, "__system_property_get"));
    }

    int ret = orig_system_property_get(name, value, default_value);

    if (strcmp(name, "ro.boot.flash.locked") == 0) {
        strncpy(value, "0", 2);
        LOGD("Native层Hook成功: %s=0", name);
    }
    return ret;
}

extern "C" JNIEXPORT void JNICALL
Java_com_yu13140_masquerade_NativeHook_init(JNIEnv* env, jobject thiz) {   
    void* libc = dlopen("libc.so", RTLD_NOW);
    void* original = dlsym(libc, "__system_property_get");
    if (original) {
        orig_system_property_get = reinterpret_cast<T_system_property_get>(original);
    }
    dlclose(libc);
}