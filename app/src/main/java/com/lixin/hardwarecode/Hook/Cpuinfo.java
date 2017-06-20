package com.lixin.hardwarecode.Hook;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Cpuinfo {
	public Cpuinfo(LoadPackageParam sharePkgParam) {

		/*
		   通过重定向cpuinfo文件 改变其内容
		 */

		FakeCPUFile(sharePkgParam);

	}

	public static boolean CreatDataCpu(Context context) {
		String str = "/data/data/" + context.getPackageName() + "/cpuinfo";
		// String str2 = "/data/data/" + context.getPackageName() + "/version";
		try {
			AssetManager assets = context.getAssets();
			InputStream open = assets.open("cpuinfo");
			OutputStream fileOutputStream = new FileOutputStream(str);
			writeValue(open, fileOutputStream);
			open.close();
			fileOutputStream.flush();
			fileOutputStream.close();
			// InputStream open2 = assets.open("version");
			// OutputStream fileOutputStream2 = new FileOutputStream(str2);
			// writeValue(open2, fileOutputStream2);
			// open2.close();
			// fileOutputStream2.flush();
			// fileOutputStream2.close();
			Sendfile(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static void writeValue(InputStream inputStream,
			OutputStream outputStream) {
		try {
			byte[] bArr = new byte[AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT];
			while (true) {
				int read = inputStream.read(bArr);
				if (read != -1) {
					outputStream.write(bArr, 0, read);
				} else {
					return;
				}
			}
		} catch (Exception e) {
		}
	}

	private static void Sendfile(String str) {
		IOException e;
		try {
			DataOutputStream dataOutputStream = new DataOutputStream(Runtime
					.getRuntime().exec("su").getOutputStream());
			try {
				dataOutputStream.writeBytes("mkdir /sdcard/Test/\n");
				dataOutputStream.flush();
				dataOutputStream.writeBytes("chmod 777 /sdcard/Test/\n");
				dataOutputStream.flush();
				dataOutputStream.writeBytes("cp " + str + " /sdcard/Test/\n");
				dataOutputStream.flush();
				dataOutputStream
						.writeBytes("chmod 444 /sdcard/Test/cpuinfo\n");
				dataOutputStream.flush();
				dataOutputStream.writeBytes("rm " + str + "\n");
				dataOutputStream.flush();
				// dataOutputStream.writeBytes("cp " + str2 +
				// " /data/misc/sys\n");
				// dataOutputStream.flush();
				// dataOutputStream.writeBytes("chmod 444 /data/misc/sys/version\n");
				// dataOutputStream.flush();
				// dataOutputStream.writeBytes("rm " + str2 + "\n");
				// dataOutputStream.flush();
				dataOutputStream.close();            
				if (new File("/sdcard/Test/cpuinfo").exists()) {
					return;
				}
				throw new IOException();
			} catch (IOException e2) {
				e = e2;
				DataOutputStream dataOutputStream2 = dataOutputStream;
				e.printStackTrace();
			}
		} catch (IOException e3) {
			e = e3;
			e.printStackTrace();
		}
	}

	public void FakeCPUFile(LoadPackageParam loadPkgParam) {
		
		try {

			XposedBridge.hookAllConstructors(File.class, new XC_MethodHook() {

				@Override
				protected void beforeHookedMethod(MethodHookParam param)
						throws Throwable {
					// TODO Auto-generated method stub
					super.beforeHookedMethod(param);
					if (param.args.length == 1) {
						if (param.args[0].equals("/proc/cpuinfo")) {
							param.args[0] = "/sdcard/Test/cpuinfo";
						}

					} else if (param.args.length == 2
							&& !File.class.isInstance(param.args[0])) {
						int i = 0;
						String str = "";
						while (i < 2) {
							String stringBuilder;
							if (param.args[i] != null) {
								if (param.args[i].equals("/proc/cpuinfo")) {
									param.args[i] = "/sdcard/Test/cpuinfo";
								}

								stringBuilder = new StringBuilder(String
										.valueOf(str)).append(param.args[i])
										.append(":").toString();
						
							} else {
								stringBuilder = str;
							}
							i++;
							str = stringBuilder;
						}
					}
				}

			});

			XposedHelpers.findAndHookMethod("java.lang.Runtime",
					loadPkgParam.classLoader, "exec", String[].class,
					String[].class, File.class, new XC_MethodHook() {

						@Override
						protected void beforeHookedMethod(MethodHookParam param)
								throws Throwable {
							// TODO Auto-generated method stub
							super.beforeHookedMethod(param);
							if (param.args.length == 1) {
								if (param.args[0].equals("/proc/cpuinfo")) {
									param.args[0] = "/sdcard/Test/cpuinfo";
								}

							} else if (param.args.length == 2
									&& !File.class.isInstance(param.args[0])) {
								int i = 0;
								String str = "";
								while (i < 2) {
									String stringBuilder;
									if (param.args[i] != null) {
										if (param.args[i]
												.equals("/proc/cpuinfo")) {
											param.args[i] = "/sdcard/Test/cpuinfo";
										}

										stringBuilder = new StringBuilder(
												String.valueOf(str))
												.append(param.args[i])
												.append(":").toString();
									} else {
										stringBuilder = str;
									}
									i++;
									str = stringBuilder;
								}
							}
						}

					});
		} catch (Exception e) {

		}

		try {
			XposedBridge.hookMethod(XposedHelpers.findConstructorExact(
					ProcessBuilder.class, new Class[] { String[].class }),
					new XC_MethodHook() {

						protected void beforeHookedMethod(MethodHookParam param)
								throws Throwable {
							// TODO Auto-generated method stub
							super.beforeHookedMethod(param);
							if (param.args[0] != null) {
								String[] strArr = (String[]) param.args[0];
								String str = "";
								for (String str2 : strArr) {
									str = new StringBuilder(String.valueOf(str))
											.append(str2).append(":")
											.toString();
									if (str2 == "/proc/cpuinfo") {
										strArr[1] = "/sdcard/Test/cpuinfo";
									}

								}
								param.args[0] = strArr;
							}
						}

					});

		} catch (Exception e) {

		}


		try {
			XposedHelpers.findAndHookMethod("java.util.regex.Pattern",
					loadPkgParam.classLoader, "matcher", CharSequence.class,
					new XC_MethodHook() {

						@Override
						protected void beforeHookedMethod(MethodHookParam param)
								throws Throwable {
							// TODO Auto-generated method stub
							super.beforeHookedMethod(param);
							if (param.args.length == 1) {
								if (param.args[0].equals("/proc/cpuinfo")) {
									param.args[0] = "/sdcard/Test/cpuinfo";
								}

							}
						}

					});

		} catch (Exception e) {
			// TODO: handle exception
		}

	
	}
	
}
