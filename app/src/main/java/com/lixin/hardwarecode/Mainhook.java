package com.lixin.hardwarecode;

import com.lixin.hardwarecode.Hook.Cpuinfo;
import com.lixin.hardwarecode.Hook.GPShook;
import com.lixin.hardwarecode.Hook.Hook;
import com.lixin.hardwarecode.Hook.OpenGL;
import com.lixin.hardwarecode.Hook.Phone;
import com.lixin.hardwarecode.Hook.Resolution;
import com.lixin.hardwarecode.Hook.XBuild;
import com.lixin.hardwarecode.Utis.RootCloak;
import com.lixin.hardwarecode.Utis.SharedPref;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Administrator on 2017/4/17 0017.
 */

public class Mainhook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
    //     XposedBridge.log("HOOK  作用于全局" );

            new Hook().HookTest(loadPackageParam); // 动态生效 不用重启
            new RootCloak().handleLoadPackage(loadPackageParam);
            new XBuild(loadPackageParam);  //build
            new Phone(loadPackageParam);  // TelephonyManager
            new Resolution().Display(loadPackageParam);  // 屏幕
            new OpenGL().OpenGLTest(loadPackageParam);   // 显卡
            new Cpuinfo(loadPackageParam);         // CPU*/




        /*
            GPS位置 只对百度高德生效 有需要的朋友可以添加
            要更改位置应用的包名  不要作用于全局  某些机型可能不太好使 请自行适配 不难的
         */
        if (loadPackageParam.packageName.equals("com.baidu.BaiduMap")||
                loadPackageParam.packageName.equals("com.autonavi.minimap")) {

            GPShook.HookAndChange(loadPackageParam.classLoader,
                    SharedPref.getfloatXValue("lat"),SharedPref.getfloatXValue("log"));



        }
    }
}
