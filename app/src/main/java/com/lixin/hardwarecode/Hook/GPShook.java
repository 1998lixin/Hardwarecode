package com.lixin.hardwarecode.Hook;

import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class GPShook {
	public static void HookAndChange(ClassLoader classLoader, final double latitude, final double longtitude) {

		// 基站信息设置为Null
        XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", classLoader,
                 "getCellLocation", new XC_MethodHook() {
                     @Override
                     protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    	 param.setResult(null);
                     }	
                 });


         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BASE) {
        	 // 把基站信息设置为NULL
             XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", classLoader,
                     "getNeighboringCellInfo", new XC_MethodHook() {
                         @Override
                         protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        	 param.setResult(null);
                         }
                     });
         }
         if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
        	 XposedHelpers.findAndHookMethod(TelephonyManager.class, "getAllCellInfo", new XC_MethodHook() {
                 @Override
                 protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                     param.setResult(null);
                 }
             });
        	 
        	/*   // WIFI 集合
        	  XposedHelpers.findAndHookMethod("android.net.wifi.WifiManager", classLoader, "getScanResults", new XC_MethodHook() {
                  @Override
                  protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                      param.setResult(null);
                  }
          	});
         */




			 // 纬度
 			XposedHelpers.findAndHookMethod("android.location.Location", classLoader, "getLatitude", new XC_MethodHook() {

 				@Override
 				protected void beforeHookedMethod(MethodHookParam param)
 						throws Throwable {
 					// TODO Auto-generated method stub
 					super.beforeHookedMethod(param);
 					
 					param.setResult(latitude);
 				}
 				
 			});

			 // 经度
 			XposedHelpers.findAndHookMethod("android.location.Location", classLoader, "getLongitude", new XC_MethodHook() {

 				@Override
 				protected void beforeHookedMethod(MethodHookParam param)
 						throws Throwable {
 					// TODO Auto-generated method stub
 					super.beforeHookedMethod(param);
 					param.setResult(longtitude);
 				}
 				
 			});
        	  
        	  
        	  
        	  
        	  
        	  XposedHelpers.findAndHookMethod("android.net.wifi.WifiInfo", classLoader, "getBSSID", new XC_MethodHook() {
                  @Override
                  protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                      param.setResult("00-00-00-00-00-00-00-00");
                  }
              });
      	    XposedHelpers.findAndHookMethod("android.net.wifi.WifiInfo", classLoader, "getMacAddress", new XC_MethodHook() {
                  @Override
                  protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                      param.setResult("00-00-00-00-00-00-00-00");
                  }
              });



			 // WIFI 信息集合
    /*	
    	   
      	   XposedHelpers.findAndHookMethod("android.net.wifi.WifiInfo", classLoader, "getSSID", new XC_MethodHook() {
               @Override
               protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                   param.setResult("null");
               }
           });*/
/*
	  XposedHelpers.findAndHookMethod("android.net.wifi.WifiManager", classLoader, "getWifiState", new XC_MethodHook() {
              @Override
              protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                  param.setResult(1);
              }
          });
      	    


        XposedHelpers.findAndHookMethod("android.net.NetworkInfo", classLoader,
                 "getTypeName", new XC_MethodHook() {
                     @Override
                     protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                         param.setResult("WIFI");
                     }
                 });*/
		XposedHelpers.findAndHookMethod("android.net.wifi.WifiManager", classLoader, "isWifiEnabled", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
         
   /*     XposedHelpers.findAndHookMethod("android.net.NetworkInfo", classLoader,
                 "isConnectedOrConnecting", new XC_MethodHook() {
                     @Override
                     protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                         param.setResult(false);
                     }
                 });
         
         XposedHelpers.findAndHookMethod("android.net.NetworkInfo", classLoader,
                 "isConnected", new XC_MethodHook() {
                     @Override
                     protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                         param.setResult(false);
                     }
                 });

        XposedHelpers.findAndHookMethod("android.net.NetworkInfo", classLoader,
                 "isAvailable", new XC_MethodHook() {
                     @Override
                     protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                         param.setResult(false);
                     }
                 });

         XposedHelpers.findAndHookMethod("android.telephony.CellInfo", classLoader,
                 "isRegistered", new XC_MethodHook() {
                     @Override
                     protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                         param.setResult(false);
                     }
                 });
*/
		
		//�жϸ�LocationProvider�Ƿ���Ҫ���������վ
		XposedHelpers.findAndHookMethod(LocationManager.class, "requiresCell",  new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});

		//�жϸ�LocationProvider�Ƿ���Ҫ��������
		XposedHelpers.findAndHookMethod(LocationManager.class, "requiresNetwork",  new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});
		
		
		XposedHelpers.findAndHookMethod(LocationManager.class, "getLastLocation", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Location l = new Location(LocationManager.GPS_PROVIDER);
				l.setLatitude(latitude);
				l.setLongitude(longtitude);
				l.setAccuracy(100f);
				l.setTime(0);
				/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
					l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
				}*/
				param.setResult(l);
			}
		});

		XposedHelpers.findAndHookMethod(LocationManager.class, "getLastKnownLocation", String.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Location l = new Location(LocationManager.GPS_PROVIDER);
				l.setLatitude(latitude);
				l.setLongitude(longtitude);
				l.setAccuracy(100f);
				l.setTime(0);
				/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
					l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
				}*/
				param.setResult(l);
			}
		});


		XposedBridge.hookAllMethods(LocationManager.class, "getProviders", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				ArrayList<String> arrayList = new ArrayList<String>();
				arrayList.add("gps");
				param.setResult(arrayList);
			}
		});

		XposedHelpers.findAndHookMethod(LocationManager.class, "getBestProvider", Criteria.class, Boolean.TYPE, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult("gps");
			}
		});

		XposedHelpers.findAndHookMethod(LocationManager.class, "addGpsStatusListener", GpsStatus.Listener.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (param.args[0] != null) {
					XposedHelpers.callMethod(param.args[0], "onGpsStatusChanged", 1);
					XposedHelpers.callMethod(param.args[0], "onGpsStatusChanged", 3);
				}
			}
		});

		XposedHelpers.findAndHookMethod(LocationManager.class, "addNmeaListener", GpsStatus.NmeaListener.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});

		XposedHelpers.findAndHookMethod("android.location.LocationManager", classLoader,
				"getGpsStatus", GpsStatus.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						GpsStatus gss = (GpsStatus) param.getResult();
						if (gss == null)
							return;

						Class<?> clazz = GpsStatus.class;
						Method m = null;
						for (Method method : clazz.getDeclaredMethods()) {
							if (method.getName().equals("setStatus")) {
								if (method.getParameterTypes().length > 1) {
									m = method;
									break;
								}
							}
						}
						if (m == null)
							return;

						//access the private setStatus function of GpsStatus
						m.setAccessible(true);

						//make the apps belive GPS works fine now
						int svCount = 5;
						int[] prns = {1, 2, 3, 4, 5};
						float[] snrs = {0, 0, 0, 0, 0};
						float[] elevations = {0, 0, 0, 0, 0};
						float[] azimuths = {0, 0, 0, 0, 0};
						int ephemerisMask = 0x1f;
						int almanacMask = 0x1f;

						//5 satellites are fixed
						int usedInFixMask = 0x1f;

						XposedHelpers.callMethod(gss, "setStatus", svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
						param.args[0] = gss;
						param.setResult(gss);
						try {
							m.invoke(gss, svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
							param.setResult(gss);
						} catch (Exception e) {
							XposedBridge.log(e);
						}
					}
				});

		for (Method method : LocationManager.class.getDeclaredMethods()) {
			if (method.getName().equals("requestLocationUpdates")
					&& !Modifier.isAbstract(method.getModifiers())
					&& Modifier.isPublic(method.getModifiers())) {
				XposedBridge.hookMethod(method, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (param.args.length >= 4 && (param.args[3] instanceof LocationListener)) {

							LocationListener ll = (LocationListener) param.args[3];

							Class<?> clazz = LocationListener.class;
							Method m = null;
							for (Method method : clazz.getDeclaredMethods()) {
								if (method.getName().equals("onLocationChanged") && !Modifier.isAbstract(method.getModifiers())) {
									m = method;
									break;
								}
							}
							Location l = new Location(LocationManager.GPS_PROVIDER);
							l.setLatitude(latitude);
							l.setLongitude(longtitude);
							l.setAccuracy(10.00f);
							l.setTime(0);
						/*	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
								l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
							}*/
							XposedHelpers.callMethod(ll, "onLocationChanged", l);
							try {
								if (m != null) {
									m.invoke(ll, l);
								}
							} catch (Exception e) {
								XposedBridge.log(e);
							}
						}
					}
				});
			}

			if (method.getName().equals("requestSingleUpdate ")
					&& !Modifier.isAbstract(method.getModifiers())
					&& Modifier.isPublic(method.getModifiers())) {
				XposedBridge.hookMethod(method, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (param.args.length >= 3 && (param.args[1] instanceof LocationListener)) {

							LocationListener ll = (LocationListener) param.args[3];

							Class<?> clazz = LocationListener.class;
							Method m = null;
							for (Method method : clazz.getDeclaredMethods()) {
								if (method.getName().equals("onLocationChanged") && !Modifier.isAbstract(method.getModifiers())) {
									m = method;
									break;
								}
							}

							try {
								if (m != null) {
									Location l = new Location(LocationManager.GPS_PROVIDER);
									l.setLatitude(latitude);
									l.setLongitude(longtitude);								
									l.setAccuracy(100f);
									l.setTime(0);
								/*	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
										l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
									}*/
									m.invoke(ll, l);
								}
							} catch (Exception e) {
								XposedBridge.log(e);
							}
						}
					}
				});
			}
		}
	
         }
	}
}


