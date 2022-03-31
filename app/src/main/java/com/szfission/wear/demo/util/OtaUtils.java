package com.szfission.wear.demo.util;

import android.content.Context;

import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.BaseCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.realsil.sdk.core.RtkConfigure;
import com.realsil.sdk.core.RtkCore;
import com.realsil.sdk.core.bluetooth.GlobalGatt;
import com.realsil.sdk.core.utility.DataConverter;
import com.realsil.sdk.dfu.DfuConstants;
import com.realsil.sdk.dfu.RtkDfu;
import com.realsil.sdk.dfu.image.BinIndicator;
import com.realsil.sdk.dfu.model.ConnectionParameters;
import com.realsil.sdk.dfu.model.DfuConfig;
import com.realsil.sdk.dfu.model.DfuProgressInfo;
import com.realsil.sdk.dfu.model.OtaDeviceInfo;
import com.realsil.sdk.dfu.model.Throughput;
import com.realsil.sdk.dfu.utils.DfuAdapter;
import com.realsil.sdk.dfu.utils.GattDfuAdapter;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.SharedPreferencesUtil;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.ifs.OnCheckOtaCallback;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.greenrobot.eventbus.EventBus;

public class OtaUtils {
    private static DfuConfig mDfuConfig;
    private static RtkConfigure mRtkConfigure;
    private static GattDfuAdapter mGattDfuAdapter;
    private static String path;

   static String otaType = "0";

   private static BaseCmdResultListener listener;

   private static Context mContext;

    public static void startDfu(Context context, String filePath, boolean isFirmwareUpgrade){
        AnyWear.setHighSpeedConnect(true, new OnSmallDataCallback() {
            @Override
            public void OnBooleanResult(boolean enable) {
//                if (isFirmwareUpgrade) {
//                    otaType = "0";
//                } else {
//                    otaType = "1";
//                }
                if (enable){
                    AnyWear.checkOTA("1", new OnCheckOtaCallback() {
                        @Override
                        public void success(String number) {
//                            if (number.equals(otaType)) {
                                if (mRtkConfigure == null) {
                                    mRtkConfigure = new RtkConfigure.Builder().logTag("clx").build();
                                    RtkCore.initialize(context, mRtkConfigure);
                                    RtkDfu.initialize(context, true);
                                }
                                mGattDfuAdapter = GattDfuAdapter.getInstance(context);
                                mGattDfuAdapter.removeDfuHelperCallback(mDfuAdapterCallback);
                                mGattDfuAdapter.initialize(mDfuAdapterCallback);
                                path = filePath;
                                LogUtils.d("OTA传输路径"+path);
//                            }
                        }
                        @Override
                        public void OnError(String msg) {
                        }
                    });
                }
                super.OnBooleanResult(enable);
            }
        });


    }
    public static void startDfu(Context context, String filePath, int otaType){
        mContext = context;
        listener = new FissionAtCmdResultListener() {
            @Override
            public void sendSuccess(String cmdId) {

            }

            @Override
            public void sendFail(String cmdId) {

            }

            @Override
            public void onResultTimeout(String cmdId) {

            }

            @Override
            public void onResultError(String errorMsg) {

            }

            @Override
            public void checkOTA(String otaType) {
                super.checkOTA(otaType);
                if(Integer.parseInt(otaType) == FissionConstant.OTA_TYPE_FIRMWARE || Integer.parseInt(otaType) == FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL){
                    if (mRtkConfigure == null) {
                        mRtkConfigure = new RtkConfigure.Builder().logTag("clx").build();
                        RtkCore.initialize(context, mRtkConfigure);
                        RtkDfu.initialize(context, true);
                    }
                    mGattDfuAdapter = GattDfuAdapter.getInstance(context);
                    mGattDfuAdapter.removeDfuHelperCallback(mDfuAdapterCallback);
                    mGattDfuAdapter.initialize(mDfuAdapterCallback);
                    path = filePath;
                    LogUtils.d("OTA传输路径"+path);
                    FissionSdkBleManage.getInstance().removeCmdResultListener(listener);
                    listener = null;
                }
            }

            @Override
            public void setSwitchHighCh(boolean enable) {
                super.setSwitchHighCh(enable);

            }
        };
        FissionSdkBleManage.getInstance().addCmdResultListener(listener);
        FissionSdkBleManage.getInstance().setSwitchHighCh(true);
        FissionSdkBleManage.getInstance().checkOTA(String.valueOf(otaType));
    }


    private static void starOta(){
        OtaDeviceInfo otaDeviceInfo = mGattDfuAdapter.getOtaDeviceInfo();
        if (mDfuConfig == null) {
            mDfuConfig = new DfuConfig();
        }
        mDfuConfig.setChannelType(DfuConfig.CHANNEL_TYPE_GATT);
        mDfuConfig.setAddress(SharedPreferencesUtil.getInstance().getBluetoothAddress());
        mDfuConfig.setLogLevel(3);
        LogUtils.d("传输的路径" + path);
        mDfuConfig.setFilePath(path);
        mDfuConfig.setOtaWorkMode(16);
        if (otaDeviceInfo != null) {
            mDfuConfig.setProtocolType(otaDeviceInfo.getProtocolType());
        } else {
            mDfuConfig.setProtocolType(0);
        }
        ConnectionParameters connectionParameters = new ConnectionParameters.Builder().minInterval(0x0006).maxInterval(0x0011).latency(0).timeout(500).build();
        mDfuConfig.setConnectionParameters(connectionParameters);
        mDfuConfig.setAutomaticActiveEnabled(true);//自动激活
        mDfuConfig.setBreakpointResumeEnabled(false);//断点续传
        mDfuConfig.setBatteryCheckEnabled(false);
        mDfuConfig.setLowBatteryThreshold(30);
        mDfuConfig.setBatteryLevelFormat(0);
        mDfuConfig.setVersionCheckEnabled(false);
        mDfuConfig.setVersionCheckMode(0);
        mDfuConfig.setSecretKey(DataConverter.hex2Bytes("4E46F8C5092B29E29A971A0CD1F610FB1F6763DF807A7E70960D4CD3118E601A"));
        mDfuConfig.setIcCheckEnabled(false);
        mDfuConfig.setSectionSizeCheckEnabled(true);
        mDfuConfig.setThroughputEnabled(false);
        mDfuConfig.setMtuUpdateEnabled(false);
        mDfuConfig.setWaitActiveCmdAckEnabled(false);
        mDfuConfig.setConParamUpdateLatencyEnabled(true);
        mDfuConfig.setLatencyTimeout(10);
        mDfuConfig.setHandoverTimeout(6);
        mDfuConfig.addErrorAction(DfuConfig.ERROR_ACTION_DISCONNECT);
        mDfuConfig.addErrorAction(DfuConfig.ERROR_ACTION_REFRESH_DEVICE);
        mDfuConfig.addErrorAction(DfuConfig.EA_CLOSE_GATT);
        GlobalGatt.CLOSE_GATT_ENABLED = true;
        mDfuConfig.removeCompleteAction(DfuConfig.COMPLETE_ACTION_REMOVE_BOND);
        mDfuConfig.setPhy(0);
        if (mDfuConfig.getOtaWorkMode() == DfuConstants.OTA_MODE_NORMAL_FUNCTION) {
            mDfuConfig.setWaitDisconnectWhenEnterOtaMode(true);
        }
        mDfuConfig.setFlowControlEnabled(true);
        mDfuConfig.setFlowControlInterval(0);
        mDfuConfig.setFileSuffix("bin");
        mDfuConfig.setFileIndicator(BinIndicator.INDICATOR_FULL);
        mGattDfuAdapter.startOtaProcedure(mDfuConfig, GattDfuAdapter.getInstance(mContext).getOtaDeviceInfo(), true);
    }



    private static final DfuAdapter.DfuHelperCallback mDfuAdapterCallback = new DfuAdapter.DfuHelperCallback() {
        @Override
        public void onStateChanged(int state) {
            super.onStateChanged(state);
            if (state == DfuAdapter.STATE_INIT_OK) {  //可以dfu
                LogUtils.d("clx", "----------STATE_INIT_OK");
                starOta();
            } else if (state == DfuAdapter.STATE_PREPARED) {//连接上，准备dfu
                LogUtils.d("clx", "----------STATE_PREPARED");
            } else if (state == DfuAdapter.STATE_DISCONNECTED || state == DfuAdapter.STATE_CONNECT_FAILED) {//连接错误，或者断开。
                LogUtils.d("clx", "---------连接错误：" + state);
            } else {
                LogUtils.d("clx", "---------state：" + state);
            }
        }

        @Override
        public void onTargetInfoChanged(OtaDeviceInfo otaDeviceInfo) {
            super.onTargetInfoChanged(otaDeviceInfo);
            LogUtils.d("clx", "---------onTargetInfoChanged：" + otaDeviceInfo.toString());
        }

        @Override
        public void onError(int i, int i1) {
            super.onError(i, i1);
//            AnyWear.checkOTA("255", null);
            LogUtils.e("clx", "---------onError：" + i);
        }

        @Override
        public void onProcessStateChanged(int i, Throughput throughput) {
            super.onProcessStateChanged(i, throughput);
            LogUtils.e("clx", "---------onProcessStateChanged：" + i);
        }

        @Override
        public void onProgressChanged(DfuProgressInfo dfuProgressInfo) {
            super.onProgressChanged(dfuProgressInfo);//进度条
            LogUtils.e("clx", "---------DfuProgressInfo：" + dfuProgressInfo.getProgress());
//            EventBus.getDefault().post(new DataMessageEvent(ModelConstant.FUNC_OTA,  dfuProgressInfo.getProgress()+""));
        }
    };







}
