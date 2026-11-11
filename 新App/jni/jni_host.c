#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "lua.h"
#include "lauxlib.h"
#include "lualib.h"

static JavaVM *g_vm = NULL;
static jclass g_cls = NULL;
static jmethodID g_call = NULL;
static lua_State *L = NULL;

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;
    g_vm = vm;
    return JNI_VERSION_1_6;
}

static JNIEnv *get_env(int *attached) {
    JNIEnv *env = NULL;
    *attached = 0;
    if ((*g_vm)->GetEnv(g_vm, (void **) &env, JNI_VERSION_1_6) == JNI_EDETACHED) {
        if ((*g_vm)->AttachCurrentThread(g_vm, &env, NULL) != JNI_OK) return NULL;
        *attached = 1;
    }
    return env;
}

static int l_call(lua_State *lua) {
    const char *m = luaL_checkstring(lua, 1);
    const char *a = luaL_optstring(lua, 2, "[]");
    int attached;
    JNIEnv *env = get_env(&attached);
    if (env == NULL) {
        lua_pushnil(lua);
        return 1;
    }
    jstring jm = (*env)->NewStringUTF(env, m);
    jstring ja = (*env)->NewStringUTF(env, a);
    jstring jr = (jstring) (*env)->CallStaticObjectMethod(env, g_cls, g_call, jm, ja);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        jr = NULL;
    }
    if (jr != NULL) {
        const char *r = (*env)->GetStringUTFChars(env, jr, 0);
        lua_pushstring(lua, r ? r : "null");
        if (r) (*env)->ReleaseStringUTFChars(env, jr, r);
        (*env)->DeleteLocalRef(env, jr);
    } else {
        lua_pushnil(lua);
    }
    (*env)->DeleteLocalRef(env, jm);
    (*env)->DeleteLocalRef(env, ja);
    if (attached) (*g_vm)->DetachCurrentThread(g_vm);
    return 1;
}

JNIEXPORT void JNICALL
Java_com_sixzs_local_LuaHost_nativeInit(JNIEnv *env, jclass cls, jstring prelude) {
    if (g_cls == NULL) g_cls = (jclass) (*env)->NewGlobalRef(env, cls);
    if (g_call == NULL)
        g_call = (*env)->GetStaticMethodID(env, cls, "call",
                                           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (L != NULL) {
        lua_close(L);
        L = NULL;
    }
    L = luaL_newstate();
    if (L == NULL) return;
    luaL_openlibs(L);
    lua_newtable(L);
    lua_pushcfunction(L, l_call);
    lua_setfield(L, -2, "call");
    lua_setglobal(L, "__host");
    const char *p = (*env)->GetStringUTFChars(env, prelude, 0);
    if (p != NULL) {
        if (luaL_dostring(L, p) != LUA_OK) {
            const char *e = lua_tostring(L, -1);
            if (e) __android_log_print(3, "DMSX", "prelude error: %s", e);
            lua_pop(L, 1);
        }
        (*env)->ReleaseStringUTFChars(env, prelude, p);
    }
}

JNIEXPORT jstring JNICALL
Java_com_sixzs_local_LuaHost_nativeRun(JNIEnv *env, jclass cls, jstring code) {
    (void) cls;
    if (L == NULL) return (*env)->NewStringUTF(env, "lua not init");
    const char *c = (*env)->GetStringUTFChars(env, code, 0);
    if (c == NULL) return (*env)->NewStringUTF(env, "code null");
    int rc = luaL_dostring(L, c);
    (*env)->ReleaseStringUTFChars(env, code, c);
    if (rc != LUA_OK) {
        const char *e = lua_tostring(L, -1);
        jstring r = (*env)->NewStringUTF(env, e ? e : "error");
        lua_pop(L, 1);
        return r;
    }
    return (*env)->NewStringUTF(env, "");
}
