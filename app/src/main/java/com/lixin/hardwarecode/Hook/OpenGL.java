package com.lixin.hardwarecode.Hook;

import com.lixin.hardwarecode.Utis.SharedPref;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Administrator on 2017/4/17 0017.
 */

public class OpenGL {
            /*
                   显卡
             */

    public void OpenGLTest(XC_LoadPackage.LoadPackageParam loadPkgParam) {
        try {
            XposedHelpers.findAndHookMethod("com.google.android.gles_jni.GLImpl", loadPkgParam.classLoader, "glGetString", Integer.TYPE, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param)
                        throws Throwable {
                    // TODO Auto-generated method stub
                    //super.beforeHookedMethod(param);
                    if (param.args[0] != null) {
                        if (param.args[0].equals(Integer.valueOf(7936))) {
                            param.setResult(SharedPref.getXValue("GLVendor"));
                        }
                        if (param.args[0].equals(Integer.valueOf(7937))) {
                            param.setResult(SharedPref.getXValue("GLRenderer"));
                        }
                    }
                }

            });
        } catch (Exception e) {
            XposedBridge.log("HOOK GPU 失败" + e.getMessage());
        }
    }
}
