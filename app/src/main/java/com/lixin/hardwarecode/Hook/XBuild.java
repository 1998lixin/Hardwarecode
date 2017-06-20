package com.lixin.hardwarecode.Hook;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;

import com.lixin.hardwarecode.Utis.SharedPref;

import java.lang.reflect.Member;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Administrator on 2017/4/17 0017.
 */

public class XBuild {

    public XBuild(XC_LoadPackage.LoadPackageParam sharePkgParam){
        AndroidSerial(sharePkgParam);
        BaseBand(sharePkgParam);
        BuildProp(sharePkgParam);
    }


    public void AndroidSerial(XC_LoadPackage.LoadPackageParam loadPkgParam){
        try {
            Class<?> classBuild = XposedHelpers.findClass("android.os.Build",
                    loadPkgParam.classLoader);
            XposedHelpers.setStaticObjectField(classBuild, "SERIAL",
                    SharedPref.getXValue("serial")); // 串口序列号




            Class<?> classSysProp = Class
                    .forName("android.os.SystemProperties");
            XposedHelpers.findAndHookMethod(classSysProp, "get", String.class,
                    new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param)
                                throws Throwable {
                            // TODO Auto-generated method stub
                            super.afterHookedMethod(param);
                            String serialno = (String) param.args[0];

                            if (serialno.equals("gsm.version.baseband")
                                    || serialno.equals("no message")
                                    ) {
                                param.setResult(SharedPref.getXValue("getBaseband"));
                            }
                        }

                    });

            XposedHelpers.findAndHookMethod(classSysProp, "get", String.class,
                    String.class, new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param)
                                throws Throwable {
                            // TODO Auto-generated method stub
                            super.afterHookedMethod(param);

                            String serialno = (String) param.args[0];
                            if (serialno.equals("gsm.version.baseband")
                                    || serialno.equals("no message")
                                    ) {
                                param.setResult(SharedPref.getXValue("getBaseband"));
                            }
                        }

                    });
            return;
        } catch (Exception ex) {
            XposedBridge.log(" AndroidSerial 错误: " + ex.getMessage());
        }
    }
    public void BaseBand(XC_LoadPackage.LoadPackageParam loadPkgParam) {
        try {

            XposedHelpers.findAndHookMethod("android.os.Build",
                    loadPkgParam.classLoader, "getRadioVersion", new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param)
                                throws Throwable {
                            //固件版本
                            param.setResult(SharedPref.getXValue("BaseBand"));
                        }

                    });


        } catch (Exception e) {
            XposedBridge.log(" BaseBand 错误: " + e.getMessage());
        }


    }





    public void BuildProp(XC_LoadPackage.LoadPackageParam loadPkgParam){
        try {

            XposedHelpers.findField(Build.class, "BOARD").set(null, SharedPref.getXValue("board"));
            XposedHelpers.findField(Build.class, "BRAND").set(null, SharedPref.getXValue("brand"));
            XposedHelpers.findField(Build.class, "CPU_ABI").set(null, SharedPref.getXValue("ABI"));
            XposedHelpers.findField(Build.class, "CPU_ABI2").set(null, SharedPref.getXValue("ABI2"));
            XposedHelpers.findField(Build.class, "DEVICE").set(null, SharedPref.getXValue("device"));
            XposedHelpers.findField(Build.class, "DISPLAY").set(null, SharedPref.getXValue("display"));
            XposedHelpers.findField(Build.class, "FINGERPRINT").set(null, SharedPref.getXValue("fingerprint"));
            XposedHelpers.findField(Build.class, "HARDWARE").set(null, SharedPref.getXValue("NAME"));
            XposedHelpers.findField(Build.class, "ID").set(null, SharedPref.getXValue("ID"));
            XposedHelpers.findField(Build.class, "MANUFACTURER").set(null, SharedPref.getXValue("Manufacture"));
            XposedHelpers.findField(Build.class, "MODEL").set(null, SharedPref.getXValue("model"));
            XposedHelpers.findField(Build.class, "PRODUCT").set(null, SharedPref.getXValue("product"));
            XposedHelpers.findField(Build.class, "BOOTLOADER").set(null, SharedPref.getXValue("booltloader")); //主板引导程序
            XposedHelpers.findField(Build.class, "HOST").set(null, SharedPref.getXValue("host"));  // 设备主机地址
            XposedHelpers.findField(Build.class, "TAGS").set(null, SharedPref.getXValue("build_tags"));  //描述build的标签
            XposedHelpers.findField(Build.class, "TYPE").set(null, SharedPref.getXValue("shenbei_type")); //设备版本类型
           	XposedHelpers.findField(Build.VERSION.class, "INCREMENTAL").set(null, SharedPref.getXValue("incrementalincremental")); //源码控制版本号
            XposedHelpers.findField(android.os.Build.VERSION.class, "RELEASE").set(null, SharedPref.getXValue("AndroidVer"));
            XposedHelpers.findField(android.os.Build.VERSION.class, "SDK").set(null, SharedPref.getXValue("API"));
            XposedHelpers.findField(android.os.Build.VERSION.class, "CODENAME").set(null, "REL"); //写死就行 这个值为固定



            XposedHelpers.findField(Build.class, "TIME").set(null,SharedPref.getintXValue("time"));  // 固件时间build
            //	XposedHelpers.findField(Build.VERSION.class, "SDK_INT").set(null, pre.getInt("sdkint", 6));


        } catch (Exception e) {
            // TODO Auto-generated catch block
            XposedBridge.log(" BuilProp 错误: " + e.getMessage());
        }

       try {
            XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPkgParam.classLoader, "getString",ContentResolver.class, String.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param)
                        throws Throwable {

                    if (param.args[1].equals(Settings.Secure.ANDROID_ID)) {
                        param.setResult(SharedPref.getXValue("AndroidID"));
                    }
                }
            });

        } catch (Exception ex) {
            XposedBridge.log(" Android ID 错误: " + ex.getMessage());
        }

        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            if(cls != null){
                for (Member mem : cls.getDeclaredMethods()) {
                    XposedBridge.hookMethod(mem, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param)
                                throws Throwable {
                            // TODO Auto-generated method stub
                            super.beforeHookedMethod(param);
                             // 用户的KEY
                            if (param.args.length > 0 && param.args[0] != null && param.args[0].equals("ro.build.description")) {
                                param.setResult(SharedPref.getXValue("DESCRIPTION"));
                            }
                        }
                    });
                }
            }

        } catch (ClassNotFoundException e) {
            XposedBridge.log(" DESCRIPTION 错误: " + e.getMessage());
        }
    }
}
