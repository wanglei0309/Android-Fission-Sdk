package com.szfission.wear.demo;

import android.app.Application;
import android.os.Environment;

import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.AnyWearConfig;
import com.szfission.wear.sdk.bean.HardWareInfo;
import com.tencent.bugly.crashreport.CrashReport;

import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class App extends Application {

    public static List<String> logData;
    public static List<String> logSingleData;

    public static HardWareInfo mHardWareInfo;
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferencesUtil.getInstance().init(this);
//        FissionSdk.getInstance().initSdk(this);
        x.Ext.init(this);
        x.Ext.setDebug(true);
        /**
         * @param connectOutTime 连接超时时间
         *  @param logTag 日志输出名字
         *  @param isEnableDebug 开启日志
         */
        AnyWearConfig config = new AnyWearConfig.Builder().connectOutTime(30000).logTag("anyWear").isEnableDebug(true).builder();
        AnyWear.init(this, config);
        logData = new ArrayList<>();
        logSingleData = new ArrayList<>();
        CrashReport.initCrashReport(getApplicationContext(), "683183f15b", true);

        FissionSdkBleManage.getInstance().initFissionSdk(this);
        FissionSdkBleManage.getInstance().setDebug(true);

        LogUtils.getConfig().setConsoleSwitch(BuildConfig.DEBUG);
        LogUtils.getConfig().setLog2FileSwitch(true);
        LogUtils.getConfig().setSaveDays(7);
        LogUtils.getConfig().setFilePrefix("Device");
        LogUtils.getConfig().setDir(Environment.getExternalStorageDirectory());

        FissionSdkBleManage.getInstance().initSppConnect(this);
    }
}
