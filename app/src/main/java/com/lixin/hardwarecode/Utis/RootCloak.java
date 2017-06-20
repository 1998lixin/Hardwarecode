package com.lixin.hardwarecode.Utis;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;

public class RootCloak implements IXposedHookLoadPackage {
    private static final String FAKE_COMMAND = "FAKEJUNKCOMMAND";
    private static final String FAKE_FILE = "FAKEJUNKFILE";
    private static final String FAKE_PACKAGE = "FAKE.JUNK.PACKAGE";
    private static final String FAKE_APPLICATION = "FAKE.JUNK.APPLICATION";

    private Set<String> appSet;
    private Set<String> keywordSet;
    private Set<String> commandSet;
    private Set<String> libnameSet;
    private boolean debugPref;
    private boolean isRootCloakLoadingPref = false;
    private String listApp;

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        loadPrefs(); // 加载首选项为任何应用程序。 这样我们可以确定它是否匹配应用程序列表以隐藏根。
	/*	if (debugPref) {
			XposedBridge.log("Found app: " + lpparam.packageName);
		}*/

        if (!(appSet.contains(lpparam.packageName))) { // 如果应用程序不匹配，请勿钩住任何东西，只需返回。
            return;
        }

        if (debugPref) {
            XposedBridge.log("加载应用程式：" + lpparam.packageName);
        }

        // 做所有的钩子
        initOther(lpparam);
        initFile(lpparam);
        initPackageManager(lpparam);
        initActivityManager(lpparam);
        initRuntime(lpparam);
        initProcessBuilder(lpparam);
        initSettingsGlobal(lpparam);
    }

    /**
     * 处理一堆杂项钩。
     */
    private void initOther(final LoadPackageParam lpparam) {
        //  检查调试是否开启时始终返回false
        XposedHelpers.findAndHookMethod("android.os.Debug", lpparam.classLoader, "isDebuggerConnected", XC_MethodReplacement.returnConstant(false));

        //如果测试键，更改为释放键
        if (!Build.TAGS.equals("release-keys")) {
            if (debugPref) {
                XposedBridge.log("原始构建标签： " + Build.TAGS);
            }
            XposedHelpers.setStaticObjectField(Build.class, "TAGS", "release-keys");
            if (debugPref) {
                XposedBridge.log("新建标签： " + Build.TAGS);
            }
        } else {
            if (debugPref) {
                XposedBridge.log("无需更改构建标签: " + Build.TAGS);
            }
        }

        // 告诉SELinux正在执行的应用程序，即使不是。
        findAndHookMethod("android.os.SystemProperties", lpparam.classLoader, "get", String.class , new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (((String) param.args[0]).equals("ro.build.selinux")) {
                    param.setResult("1");
                    if (debugPref) {
                        XposedBridge.log("SELinux is enforced.");
                    }
                }
            }
        });

        // 从应用程序隐藏Xposed类
        findAndHookMethod("java.lang.Class", lpparam.classLoader, "forName", String.class, boolean.class, ClassLoader.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String classname = (String) param.args[0];

                if (classname != null && (classname.equals("de.robv.android.xposed.XposedBridge") || classname.equals("de.robv.android.xposed.XC_MethodReplacement"))) {
                    param.setThrowable(new ClassNotFoundException());
                    if (debugPref) {
                        XposedBridge.log("找到并隐藏Xposed类名称：" + classname);
                    }
                }
            }
        });
    }

    /**
     * 处理与java.io.File相关的所有钩子。
     */
    private void initFile(final LoadPackageParam lpparam) {
        /**
         挂钩File文件的一个版本。
                   一个应用程序可能会使用文件检查存在的文件，如su，busybox或其他文件。
         */
        Constructor<?> constructLayoutParams = findConstructorExact(File.class, String.class);
        XposedBridge.hookMethod(constructLayoutParams, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] != null) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor: " + ((String) param.args[0]));
                    }
                }

                if (isRootCloakLoadingPref) {
                    // RootCloak试图加载它的首选项，我们不应该阻止这个。
                    return;
                }

                if (((String) param.args[0]).endsWith("su")) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor ending with su");
                    }
                    param.args[0] = "/system/xbin/" + FAKE_FILE;
                } else if (((String) param.args[0]).endsWith("busybox")) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor ending with busybox");
                    }
                    param.args[0] = "/system/xbin/" + FAKE_FILE;
                } else if (stringContainsFromSet(((String) param.args[0]), keywordSet)) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor with word super, noshufou, or chainfire");
                    }
                    param.args[0] = "/system/app/" + FAKE_FILE + ".apk";
                }
            }
        });

        /**
         钩住File构造函数的一个版本。
                   一个应用程序可能会使用文件检查存在的文件，如su，busybox或其他文件。
         */
        Constructor<?> extendedFileConstructor = findConstructorExact(File.class, String.class, String.class);
        XposedBridge.hookMethod(extendedFileConstructor, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] != null && param.args[1] != null) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor: " + ((String) param.args[0]) + ", with: " + ((String) param.args[1]));
                    }
                }

                if (isRootCloakLoadingPref) {
                    // RootCloak试图加载它的首选项，我们不应该阻止这个。
                    return;
                }

                if (((String) param.args[1]).equalsIgnoreCase("su")) {
                    if (debugPref) {
                        XposedBridge.log("文件：找到一个文件名为su的File构造函数");
                    }
                    param.args[1] = FAKE_FILE;
                } else if (((String) param.args[1]).contains("busybox")) {
                    if (debugPref) {
                        XposedBridge.log("文件：找到一个以busybox结尾的文件构造函数");
                    }
                    param.args[1] = FAKE_FILE;
                } else if (stringContainsFromSet(((String) param.args[1]), keywordSet)) {
                    if (debugPref) {
                        XposedBridge.log("文件：找到一个文件构造函数，带有super，noshufou或chainfire");
                    }
                    param.args[1] = FAKE_FILE + ".apk";
                }
            }
        });

        /**
         钩住使用URI的File构造函数的一个版本。
                   一个应用程序可能会使用文件检查存在的文件，如su，busybox或其他文件。
                   注意：目前仅用于调试目的，通常不会使用。
         */
        Constructor<?> uriFileConstructor = findConstructorExact(File.class, URI.class);
        XposedBridge.hookMethod(uriFileConstructor, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] != null) {
                    if (debugPref) {
                        XposedBridge.log("文件：找到一个URI文件构造函数： " + ((URI) param.args[0]).toString());
                    }
                }
            }
        });
    }

    /**
     * 处理与PackageManager相关的所有挂钩。
     */
    private void initPackageManager(final LoadPackageParam lpparam) {
        /**
         钩子在PackageManager中获取安装应用程序。
                   *应用程式可以通过这种方式检查其他应用程式。 在有根设备的上下文中，应用程序可能会查找SuperSU，Xposed，Superuser或其他。
                   *匹配关键字集中的条目的结果将被隐藏。
         */
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledApplications", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getIntalledApplications is called
                if (debugPref) {
                    XposedBridge.log("Hooked getInstalledApplications");
                }

                List<ApplicationInfo> packages = (List<ApplicationInfo>) param.getResult(); // Get the results from the method call
                Iterator<ApplicationInfo> iter = packages.iterator(); 
                ApplicationInfo tempAppInfo;
                String tempPackageName;

                // 通过ApplicationInfo列表迭代，并删除与keywordSet中的关键字匹配的任何提及
                while (iter.hasNext()) {
                    tempAppInfo = iter.next();
                    tempPackageName = tempAppInfo.packageName;
                    if (tempPackageName != null && stringContainsFromSet(tempPackageName, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("找到并隐藏包：" + tempPackageName);
                        }
                    }
                }

                param.setResult(packages); // 将返回值设置为干净列表
            }
        });

        /**
         Hooks在PackageManager中getInstalledPackages。
           应用程式可以通过这种方式检查其他应用程式。 在有根设备的上下文中，应用程序可能会查找SuperSU，Xposed，Superuser或其他。
               匹配关键字集中的条目的结果将被隐藏。
         */
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getInstalledPackages is called
                if (debugPref) {
                    XposedBridge.log("Hooked getInstalledPackages");
                }

                List<PackageInfo> packages = (List<PackageInfo>) param.getResult(); // Get the results from the method call
                Iterator<PackageInfo> iter = packages.iterator();
                PackageInfo tempPackageInfo;
                String tempPackageName;

                // 通过PackageInfo列表迭代，并删除与keywordSet中的关键字匹配的任何提及
                while (iter.hasNext()) {
                    tempPackageInfo = iter.next();
                    tempPackageName = tempPackageInfo.packageName;
                    if (tempPackageName != null && stringContainsFromSet(tempPackageName, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("找到并隐藏包：" + tempPackageName);
                        }
                    }
                }

                param.setResult(packages); // 将返回值设置为干净列表
            }
        });

        /**
         在PackageManager中挂钩getPackageInfo。
                   *应用程序可以以这种方式检查其他包。 我们在getPackageInfo被调用之前钩。
                   *如果正在查看的包与keywordSet中的条目匹配，则替换假包名称。
                   *这将最终抛出一个PackageManager.NameNotFoundException。
         */
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getPackageInfo", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked getPackageInfo");
                }
                String name = (String) param.args[0];

                if (name != null && stringContainsFromSet(name, keywordSet)) {
                    param.args[0] = FAKE_PACKAGE; // 设置假包名称
                    if (debugPref) {
                        XposedBridge.log("设置假包名称: " + name);
                    }
                }
            }
        });

        /**
         Hooks在PackageManager中获取getApplicationInfo。
                   *应用程序可以通过这种方式检查其他应用程序。 我们在getApplicationInfo被调用之前钩。
                   *如果正在查看的应用程序与keywordSet中的条目匹配，则替换假冒的应用程序名称。
                   *这将最终抛出一个PackageManager.NameNotFoundException。
         */
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getApplicationInfo", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                String name = (String) param.args[0];
                if (debugPref) {
                    XposedBridge.log("Hooked getApplicationInfo : " + name);
                }

                if (name != null && stringContainsFromSet(name, keywordSet)) {
                    param.args[0] = FAKE_APPLICATION; // 设置一个假的应用程序名称
                    if (debugPref) {
                        XposedBridge.log("发现和隐藏应用程序： " + name);
                    }
                }
            }
        });
    }

    /**
     * 处理与ActivityManager相关的所有挂钩。
     */
    private void initActivityManager(final LoadPackageParam lpparam) {
        /**
         Hooks在ActivityManager中getRunningServices。
                   *应用程式可以通过这种方式检查其他应用程式。 在有根设备的上下文中，应用程序可能会查找SuperSU，Xposed，Superuser或其他。
                   *匹配关键字集中的条目的结果将被隐藏。
         */
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningServices", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getRunningServices is called
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningServices");
                }

                List<RunningServiceInfo> services = (List<RunningServiceInfo>) param.getResult(); // 从方法调用获取结果
                Iterator<RunningServiceInfo> iter = services.iterator();
                RunningServiceInfo tempService;
                String tempProcessName;

                // 通过RunningServiceInfo列表迭代，并删除与keywordSet中的关键字匹配的任何提及
                while (iter.hasNext()) {
                    tempService = iter.next();
                    tempProcessName = tempService.process;
                    if (tempProcessName != null && stringContainsFromSet(tempProcessName, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("发现和隐藏服务： " + tempProcessName);
                        }
                    }
                }

                param.setResult(services); //将返回值设置为干净列表
            }
        });

        /**
         Hooks在ActivityManager中getRunningTasks。
                   *应用程式可以通过这种方式检查其他应用程式。 在有根设备的上下文中，应用程序可能会查找SuperSU，Xposed，Superuser或其他。
                   *匹配关键字集中的条目的结果将被隐藏。
         */
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningTasks", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getRunningTasks is called
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningTasks");
                }

                List<RunningTaskInfo> services = (List<RunningTaskInfo>) param.getResult(); // 从方法调用获取结果
                Iterator<RunningTaskInfo> iter = services.iterator();
                RunningTaskInfo tempTask;
                String tempBaseActivity;

                // 通过RunningTaskInfo列表迭代，并删除与keywordSet中的关键字匹配的任何提及
                while (iter.hasNext()) {
                    tempTask = iter.next();
                    tempBaseActivity = tempTask.baseActivity.flattenToString(); // Need to make it a string for comparison
                    if (tempBaseActivity != null && stringContainsFromSet(tempBaseActivity, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("找到并隐藏BaseActivity： " + tempBaseActivity);
                        }
                    }
                }

                param.setResult(services); // 将返回值设置为干净列表
            }
        });

        /**
         Hooks在ActivityManager中getRunningAppProcesses。
                应用程式可以通过这种方式检查其他应用程式。 在有根设备的上下文中，
         应用程序可能会查找SuperSU，Xposed，Superuser或其他。 匹配关键字集中的条目的结果将被隐藏。
                
         */
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningAppProcesses", new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getRunningAppProcesses is called
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningAppProcesses");
                }

                List<RunningAppProcessInfo> processes = (List<RunningAppProcessInfo>) param.getResult(); // Get the results from the method call
                Iterator<RunningAppProcessInfo> iter = processes.iterator();
                RunningAppProcessInfo tempProcess;
                String tempProcessName;

                // 通过RunningAppProcessInfo列表迭代，并删除与keywordSet中的关键字匹配的任何提及
                while (iter.hasNext()) {
                    tempProcess = iter.next();
                    tempProcessName = tempProcess.processName;
                    if (tempProcessName != null && stringContainsFromSet(tempProcessName, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("发现和隐藏过程： " + tempProcessName);
                        }
                    }
                }

                param.setResult(processes); // 将返回值设置为干净列表
            }
        });
    }

    /**
     * 处理与java.lang.Runtime相关的所有钩子，它用于执行其他程序或shell命令。
     */
    private void initRuntime(final LoadPackageParam lpparam) {
        /**
         钩住java.lang.Runtime中的exec（）。
                   *这是唯一需要挂钩的版本，因为所有其他版本都是“方便”的变体。
                   *形式为：exec（String [] cmdarray，String [] envp，File dir）。
                   *有很多不同的方式可以使用exec来检查根设备。
         */
        findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "exec", String[].class, String[].class, File.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked Runtime.exec");
                }

                String[] execArray = (String[]) param.args[0]; //抓住令牌化的命令数组
                if ((execArray != null) && (execArray.length >= 1)) { // Do some checking so we don't break anything
                    String firstParam = execArray[0]; // firstParam将是正在运行的主要命令/程序
                    if (debugPref) { // 如果调试已打开，请打印出所调用的内容
                        String tempString = "Exec Command:";
                        for (String temp : execArray) {
                            tempString = tempString + " " + temp;
                        }
                        XposedBridge.log(tempString);
                    }

                    if (stringEndsWithFromSet(firstParam, commandSet)) { // 检查firstParam是否是我们要过滤的关键字之一
                        if (debugPref) {
                            XposedBridge.log("Found blacklisted command at the end of the string: " + firstParam);
                        }

                        // 解决方案依赖于哪个命令被调用了一堆逻辑
                        // TODO: ***Clean up this logic***
                        if (firstParam.equals("su") || firstParam.endsWith("/su")) { // 如果它的su或以su结尾（/ bin / su，/ xbin / su等）
                            param.setThrowable(new IOException()); //抛出异常，暗示没有找到命令
                        } else if (commandSet.contains("pm") && (firstParam.equals("pm") || firstParam.endsWith("/pm"))) {
                            // 尝试使用exec运行pm（包管理器）。 现在我们来处理这些子句
                            if (execArray.length >= 3 && execArray[1].equalsIgnoreCase("list") && execArray[2].equalsIgnoreCase("packages")) {
                                // 试图列出所有的软件包，所以我们将筛选出与关键字匹配的任何内容
                                //param.args[0] = new String[] {"pm", "list", "packages", "-v", "grep", "-v", "\"su\""};
                                param.args[0] = buildGrepArraySingle(execArray, true);
                            } else if (execArray.length >= 3 && (execArray[1].equalsIgnoreCase("dump") || execArray[1].equalsIgnoreCase("path"))) {

                                if (stringContainsFromSet(execArray[2], keywordSet)) {
                                    param.args[0] = new String[]{execArray[0], execArray[1], FAKE_PACKAGE};
                                }
                            }
                        } else if (commandSet.contains("ps") && (firstParam.equals("ps") || firstParam.endsWith("/ps"))) { // This is a process list command
                            // 尝试运行ps命令以查看正在运行的进程（例如，查找以su或daemonsu运行的方式）。 过滤出来
                            param.args[0] = buildGrepArraySingle(execArray, true);
                        } else if (commandSet.contains("which") && (firstParam.equals("which") || firstParam.endsWith("/which"))) {
                            // Busybox“哪个”命令。 抛出异常
                            param.setThrowable(new IOException());
                        } else if (commandSet.contains("busybox") && anyWordEndingWithKeyword("busybox", execArray)) {
                            param.setThrowable(new IOException());
                        } else if (commandSet.contains("sh") && (firstParam.equals("sh") || firstParam.endsWith("/sh"))) {
                            param.setThrowable(new IOException());
                        } else {
                            param.setThrowable(new IOException());
                        }

                        if (debugPref && param.getThrowable() == null) { // Print out the new command if debugging is on
                            String tempString = "New Exec Command:";
                            for (String temp : (String[]) param.args[0]) {
                                tempString = tempString + " " + temp;
                            }
                            XposedBridge.log(tempString);
                        }
                    }


                } else {
                    if (debugPref) {
                        XposedBridge.log("执行时空或空数组");
                    }
                }
            }
        });
        
        /**
         * 挂钩在java.lang.Runtime中loadLibrary（）。
                   有专门为检查根目录的库。 这有助于我们阻止
         */
        findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "loadLibrary", String.class, ClassLoader.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked loadLibrary");
                }
                String libname = (String) param.args[0];

                if (libname != null && stringContainsFromSet(libname, libnameSet)) {
                    // 如果我们找到我们阻止的一个库，那么我们来防止它被加载
                    param.setResult(null);
                    if (debugPref) {
                        XposedBridge.log("Loading of library " + libname + " disabled.");
                    }
                }
            }
        });
    }

    private void initProcessBuilder(final LoadPackageParam lpparam) {
        // Hook ProcessBuilder并防止运行某些命令
        Constructor<?> processBuilderConstructor2 = findConstructorExact(ProcessBuilder.class, String[].class);
        XposedBridge.hookMethod(processBuilderConstructor2, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("Hooked ProcessBuilder");
                if (param.args[0] != null) {
                    String[] cmdArray = (String[]) param.args[0];
                    if (debugPref) {
                        String tempString = "ProcessBuilder Command:";
                        for (String temp : cmdArray) {
                            tempString = tempString + " " + temp;
                        }
                        XposedBridge.log(tempString);
                    }
                    if (stringEndsWithFromSet(cmdArray[0], commandSet)) {
                        cmdArray[0] = FAKE_COMMAND;
                        param.args[0] = cmdArray;
                    }

                    if (debugPref) {
                        String tempString = "New ProcessBuilder Command:";
                        for (String temp : (String[]) param.args[0]) {
                            tempString = tempString + " " + temp;
                        }
                        XposedBridge.log(tempString);
                    }
                }
            }
        });
    }

    /**
     * 挂钩设备设置。
     */
    private void initSettingsGlobal(final LoadPackageParam lpparam) {
        // Hooks Settings.Global.getInt。 对于这种方法，我们将阻止列表中任何应用程序获取包信息
        findAndHookMethod(Settings.Global.class, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                String setting = (String) param.args[1];
                if (setting != null && Settings.Global.ADB_ENABLED.equals(setting)) { // Hide ADB being on from an app
                    param.setResult(0);
                    if (debugPref) {
                        XposedBridge.log("挂钩ADB调试信息，adb状态关闭");
                    }
                }
            }
        });
    }

    /**
     * 加载所有首选项，如关键字，命令等。
     */
    public void loadPrefs() {
        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                .permitDiskReads()
                .permitDiskWrites()
                .build());

        isRootCloakLoadingPref = true;

        try {

        	keywordSet = new HashSet<String>();
        	commandSet = new HashSet<String>();
        	libnameSet = new HashSet<String>();
        	
        	keywordSet.isEmpty();
            commandSet.isEmpty();
            libnameSet.isEmpty();

        	listApp = SharedPref.getXValue("HideRootPackge");
        	if(listApp != null){
        		appSet = new HashSet<String>(getAppset(listApp));
        	}else {
        		appSet = Common.DEFAULT_APPS_SET;
			}
            if (keywordSet.isEmpty()) {
                keywordSet = Common.DEFAULT_KEYWORD_SET;
            }
            if (commandSet.isEmpty()) {
                commandSet = Common.DEFAULT_COMMAND_SET;
            }
            if (libnameSet.isEmpty()) {
                libnameSet = Common.DEFAULT_LIBNAME_SET;
            }
        } finally {
            StrictMode.setThreadPolicy(old);

            isRootCloakLoadingPref = false;
        }

    }
    
    public static List<String> getAppset(String parramString) {
        if (TextUtils.isEmpty(parramString)) {
            return null;
        }
        return Arrays.asList(TextUtils.split(parramString.replace(" ", ""), ","));
    }




    /**
     * 获取一个关键字字符串和一个字符串数组，并检查该数组中的任何值是否以关键字结尾
     */
    private Boolean anyWordEndingWithKeyword(String keyword, String[] wordArray) {
        for (String tempString : wordArray) {
            if (tempString.endsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取字符串和一组字符串，并检查基本字符串是否包含集合中的任何值。
     */
    public boolean stringContainsFromSet(String base, Set<String> values) {
        if (base != null && values != null) {
            for (String tempString : values) {
                if (base.matches(".*(\\W|^)" + tempString + "(\\W|$).*")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 使用字符串和一组字符串，并检查基本字符串是否以该集合中的任何值结尾。
     */
    public boolean stringEndsWithFromSet(String base, Set<String> values) {
        if (base != null && values != null) {
            for (String tempString : values) {
                if (base.endsWith(tempString)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 这个帮手需要一个命令，并附加了很多的grep。 这个想法是过滤出与keywordSet匹配的任何内容。
     */
    private String[] buildGrepArraySingle(String[] original, boolean addSH) {
        StringBuilder builder = new StringBuilder();
        ArrayList<String> originalList = new ArrayList<String>();
        if (addSH) {
            originalList.add("sh");
            originalList.add("-c");
        }
        for (String temp : original) {
            builder.append(" ");
            builder.append(temp);
        }


        for (String temp : keywordSet) {
            builder.append(" | grep -v ");
            builder.append(temp);
        }


        originalList.add(builder.toString());
        return originalList.toArray(new String[0]);
    }

}