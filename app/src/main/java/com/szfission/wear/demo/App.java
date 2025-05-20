package com.szfission.wear.demo;

import android.app.Application;
import android.os.Environment;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.common.BaiduMapSDKException;
import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.utils.BaiDuAiUtils;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.AnyWearConfig;
import com.szfission.wear.sdk.bean.HardWareInfo;
import com.tencent.bugly.crashreport.CrashReport;

import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.AutoSizeConfig;

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

        LogUtils.getConfig().setConsoleSwitch(true);
        LogUtils.getConfig().setLog2FileSwitch(true);
        LogUtils.getConfig().setSaveDays(7);
        LogUtils.getConfig().setFilePrefix("Device");
        LogUtils.getConfig().setDir(Environment.getExternalStorageDirectory());

        FissionSdkBleManage.getInstance().initSppConnect(this);

        AutoSizeConfig.getInstance()
                .setBaseOnWidth(true) // 以宽度为基准进行适配
                .setExcludeFontScale(true); // 不随系统字体缩放

        SDKInitializer.setAgreePrivacy(this, true);
        LocationClient.setAgreePrivacy(true);
        try {
            LocationClient.setAgreePrivacy(true);
            // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
            SDKInitializer.initialize(this);
        } catch (BaiduMapSDKException e) {

        }

        BaiDuAiUtils.initBaiDuAi("69253013", "BSmt4AxIJ5ttnlHauzD2LG9Q", "nCZCosHTYGVcWvzfRtbogSCBZelHQV1k", "oDKQ0Z6JvoCFd3c3O20DxEOOtDCaKCMN", "OtBdvt18HdJnSYGGGaEMn2Mg0mCTF77w");
    }

}
