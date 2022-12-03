package com.szfission.wear.demo;

import android.content.Context;
import android.text.TextUtils;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.StringUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.BleConnectListener;
import com.fission.wear.sdk.v2.config.BleComConfig;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.AnyWearConfig;
import com.szfission.wear.sdk.bean.MentalStressRecord;
import com.szfission.wear.sdk.bean.DaysReport;
import com.szfission.wear.sdk.bean.ExerGpsDetail;
import com.szfission.wear.sdk.bean.ExerciseReport;
import com.szfission.wear.sdk.bean.FissionAlarm;
import com.szfission.wear.sdk.bean.HandMeasureInfoBean;
import com.szfission.wear.sdk.bean.HardWareInfo;
import com.szfission.wear.sdk.bean.HeartRateRecord;
import com.szfission.wear.sdk.bean.HoursReport;
import com.szfission.wear.sdk.bean.MeasureInfo;
import com.szfission.wear.sdk.bean.SleepRecord;
import com.szfission.wear.sdk.bean.SleepReport;
import com.szfission.wear.sdk.bean.Spo2Record;
import com.szfission.wear.sdk.bean.StepsRecord;
import com.szfission.wear.sdk.bean.param.DndRemind;
import com.szfission.wear.sdk.bean.param.LiftWristPara;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.BleConnectCallback;
import com.szfission.wear.sdk.ifs.OnCheckOtaCallback;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;
import com.szfission.wear.sdk.ifs.ReceiveMsgListener;
import com.szfission.wear.sdk.util.BleUtil;
import com.szfission.wear.sdk.util.DateUtil;
import com.szfission.wear.sdk.util.FsLogUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * 中间处理件
 */
public class FissionSdk {
    public static FissionSdk mFissionSdk;
    private Context mContext;

    public static FissionSdk getInstance() {
        if (null == mFissionSdk) {
            synchronized (FissionSdk.class) {
                if (null == mFissionSdk) {
                    mFissionSdk = new FissionSdk();
                }
            }
        }
        return mFissionSdk;
    }

    /**
     * 初始化sdk
     */
    public void initSdk(Context context) {
        mContext = context;
        AnyWearConfig config = new AnyWearConfig.Builder().connectOutTime(30000).logTag("anyWear").isEnableDebug(false).builder();
        AnyWear.init(mContext, config);
        /**
         * @param bleConnectCallback 蓝牙设备连接状态监听
         * @param dataCallBack 通信数据监听
         * @param receiveMessageListener 接收设备主动发送数据监听
         */
        AnyWear.initListener(bleConnectCallback, bigDataCallBack, receiveMessageListener);
//        if (!SharedPreferencesUtil.getInstance().getBluetoothAddress().equals("")){
//            connectDevice(SharedPreferencesUtil.getInstance().getBluetoothAddress(),true,SharedPreferencesUtil.getInstance().getFissionKey());
//        }

    }

    /**
     * 接收设备实时返回数据的回调
     */
    ReceiveMsgListener receiveMessageListener = new ReceiveMsgListener() {


        @Override
        public void fssSuccess(int fssType, int fssStatus) {
            if (fssType == FissionEnum.SC_CAMERA_MODE) {
                if (fssStatus == 1) {
                    //APP调用跳转进入拍照页面
                    EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_CAMERA, "设备打开照相"));

                } else if (fssStatus == 0) {
                    EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_CAMERA, "设备退出照相"));
                }
            } else if (fssType == FissionEnum.SC_MUSIC) {
                //手环通知设备进入音乐状态
            } else if (fssType == FissionEnum.SC_CUR_BATTERY_PERCENT) {
                LogUtils.d("获取电池当前百分比",fssStatus);
                EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_BATTERY, "电池当前百分比:"+fssStatus));
            } else if (fssType == FissionEnum.SC_BRIGHT_SCREEN_TIME) {
                //亮屏时长同步
                EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_SET_WRIST_BRIGHT_SCREEN, "亮屏时长:"+fssStatus));
            } else if (fssType == FissionEnum.SC_DND) {
                EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_SET_DONT_DISTURB_PARA, "勿扰模式:"+fssStatus));
            } else if (fssType == FissionEnum.SC_ALARM1 || fssType == FissionEnum.SC_ALARM2 || fssType == FissionEnum.SC_ALARM3 || fssType == FissionEnum.SC_ALARM4 || fssType == FissionEnum.SC_ALARM5) {
//                EventBus.getDefault().post(new AlarmEvent((long) 1, fssType - 4, C.ALARM_NOTIFY, fssStatus));
            } else if (fssType == FissionEnum.SC_WRIST_SCREEN_ENABLE) {
                //抬腕亮屏开关
                EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_SET_WRIST_BRIGHT_SCREEN, "亮屏开关:"+fssStatus));

            }
        }

        @Override
        public void OnMusicControl(int type) {

        }

        @Override
        public void OnVolumeControl(int volumeType, int max) {

        }

        @Override
        public void gpsSuccess(boolean open) {

        }

        @Override
        public void onFindPhoneCallBack() {
            LogUtils.d("查找手机成功");
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_FIND_PHONE, "来自设备端的查找"));
        }

        @Override
        public void onGiveUpFindPhoneCallBack() {
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GIVE_UP_FIND_DEVICE, "来自设备端的放弃查找，需要手机开启静音"));
            LogUtils.d("放弃查找手机成功");
        }

        @Override
        public void onTakePhotoCallback() {

        }

        @Override
        public void onTakePhoneCallback(int callStatus) {

        }

        @Override
        public void OnError(String msg) {

        }
    };

    /**
     * 连接的回调
     */
    private final BleConnectCallback bleConnectCallback = new BleConnectCallback() {
        @Override
        public void connectSuccess(String address, String name) {
            LogUtils.d("连接成功,连接地址:" + address + "连接名称:" + name);
            SharedPreferencesUtil.getInstance().setBluetoothName(name);
            EventBus.getDefault().post(new ConnectedStateEvent(C.CONNECTED, name));
            SharedPreferencesUtil.getInstance().setBluetoothAddress(address);
        }

        @Override
        public void connectLoading() {

        }

        @Override
        public void disconnected(int code) {
            EventBus.getDefault().post(new ConnectedStateEvent(C.DISCONNECT, "成功断开蓝牙设备的连接"));
        }

        @Override
        public void connectTimeOut() {
            EventBus.getDefault().post(new ConnectedStateEvent(C.DISCONNECT, ""));
        }

        @Override
        public void connectRefuse(String type) {
            SharedPreferencesUtil.getInstance().setFissionKey("");
            EventBus.getDefault().post(new ConnectedStateEvent(C.DISCONNECT, ""));
            String result = "";
            switch (type) {
                case FissionEnum.connectBind:
                    result = "设备已经被绑定了";
                    break;
                case FissionEnum.connectTimeOuted:
                    result = "请求绑定设备超时";
                    break;
                case FissionEnum.connectRefused:
                    result = "请求绑定设备被拒绝";
//            } else if (type == FissionEnum.atTimeOuted) {
//                result = "AT指令超时了";
                    break;
            }
            AnyWear.disConnect();
            EventBus.getDefault().post(new DataMessageEvent(R.string.disconnected, result));
        }

    };

    //主动断开设备
    public void disConnectDevice() {
        if (BleUtil.bleName != null) {
            if (BleUtil.bleName.contains("LW39") || BleUtil.bleName.contains("DIZO Watch 2 Sports")) {
                AnyWear.unBindDevice(SharedPreferencesUtil.getInstance().getFissionKey(), new OnSmallDataCallback() {
                    @Override
                    public void OnStringResult(String content) {
                        LogUtils.d("解绑密钥" + content);
                        SharedPreferencesUtil.getInstance().setFissionKey("");
                        AnyWear.disConnect();
                    }
                });
            } else {
                AnyWear.disConnect();
            }
        } else {
            AnyWear.disConnect();
        }

    }


    //获取硬件信息
    //获取硬件版本信息
    public void getHardVersion() {
        AnyWear.getVersion();
    }

    public void getHardWareInfo() {
        AnyWear.getHardwareInfo(new BigDataCallBack() {
            @Override
            public void OnHardwareInfo(HardWareInfo hardWareInfo) {
                LogUtils.d("接收hardwearinfo", "设备名字" + hardWareInfo.getDeviceName());
                LogUtils.d("接收hardwearinfo", "设备地址" + hardWareInfo.getDeviceMac());
                EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_HARDWARE_INFO, "设备名字:" + hardWareInfo.getDeviceName()
                        + "\n" + "设备MAC地址:" + hardWareInfo.getDeviceMac() + "\n" + "适配号:" + hardWareInfo.getAdapterNum()));
            }
        });
    }

    //获取每日活动统计
    public void getDaysReport(long startTime, long endTime) {
        AnyWear.getDaysReport(startTime, endTime);
    }

    //整点活动统计
    public void getHoursReport(long startTime, long endTime) {
        AnyWear.getHoursReport(startTime, endTime);
    }

    //睡眠统计报告
    public void getSleepReport(long startTime, long endTime) {
        AnyWear.getSleepReport(startTime, endTime);
//        AnyWear.getSleepReport(new OnSleepReportCallback() {
//            @Override
//            public void empty(String cmdId) {
//            }
//            @Override
//            public void success(List<SleepReport> sleepReports) {
//
//            }
//
//            @Override
//            public void failed(String msg) {
//
//            }
//        }, startTime, endTime);
//    }
//    //睡眠状态记录
//    public void getSleepRecord(long startTime, long endTime){
//        AnyWear.getSleepRecord(startTime,endTime);
    }
    //运动记录
    //心率记录
    //计步记录
    //血氧记录
    //血压记录
    //血氧记录
    //运动定位记录


    /**
     * 数据的回调
     */
    private final BigDataCallBack bigDataCallBack = new BigDataCallBack() {

        @Override
        public void OnError(String msg) {
            LogUtils.d("错误信息" + msg);
        }

        @Override
        public void OnEmpty(String cmdId) {
            LogUtils.d("数据为空" + cmdId);
            EventBus.getDefault().post(new DataMessageEvent(R.string.emptyData, cmdId));
        }

        @Override
        public void OnHeartRateRecord(List<HeartRateRecord> heartRateRecords) {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < heartRateRecords.size(); i++) {
                HeartRateRecord heartRateRecord = heartRateRecords.get(i);
                content.append("第").append(i + 1).append("条记录时间：").append(DateUtil.gmtToStrDate((int) heartRateRecord.getTime(), "yyyy-MM-dd HH:mm:ss"));
                content.append("\n结构体版本：").append(heartRateRecord.getBodyVersion());
                content.append("\n记录生成周期：").append(heartRateRecord.getWeek() / 60).append("分钟");
                content.append("\n有效记录条数：").append(heartRateRecord.getEffectiveNumber());
                content.append("\n单条记录长度：").append(heartRateRecord.getRecordLength());
                content.append("\n记录类型：").append(heartRateRecord.getType());
                content.append("\n心率值：");
                for (int h = 0; h < heartRateRecord.getHrList().size(); h++) {
                    content.append(DateUtil.gmtToStrDate((Long) (heartRateRecord.getHrListTime().get(h)), "yyyy-MM-dd HH:mm:ss")).append(":  ").append(heartRateRecord.getHrList().get(h)).append("\n");
                }
                content.append("\n");
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_HEARTRATE_RECORD, content.toString()));
        }


        @Override
        public void OnCssCallback(boolean isHighOpen) {
            //这里设置一个用于监听高速通道是否超时

        }

        //获取版本信息,长数据在datacallback中回调
        @Override
        public void OnVersionCallback(String softwareVersion, String hardwareVersion) {
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_VERSION, "软件版本：" + softwareVersion + " 硬件版本：" + hardwareVersion));
        }

        //获取电量
        @Override
        public void OnRequestBattery(int batteryValue, int batteryStatus) {
            String tvBatteryStatus = "";
            if (batteryStatus == 0) {
                tvBatteryStatus = "正常";
            } else if (batteryStatus == 1) {
                tvBatteryStatus = "低压";
            } else if (batteryStatus == 2) {
                tvBatteryStatus = "充电中";
            } else if (batteryStatus == 3) {
                tvBatteryStatus = "电池满电";
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_BATTERY, "电池电量:" + batteryValue + "\n" + "电池状态:" + tvBatteryStatus));
        }

        @Override
        public void OnMeasureInfoCallback(MeasureInfo measureInfo) {
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_MEARURE_INFO, "当天累计步数:" + measureInfo.getStep()
                    + "\n" + "当天累计消耗卡路里:" + measureInfo.getCalorie() + "\n" + "当天累计行程:" + measureInfo.getDistance()));

        }

        @Override
        public void OnStepsRecord(List<StepsRecord> stepsRecords) {
            StringBuilder content = new StringBuilder();
            int index = 0;
            for (StepsRecord stepsRecord : stepsRecords) {
                index += 1;
                content.append("\n第").append(index).append("条记录时间：").append(DateUtil.gmtToStrDate(stepsRecord.getTime()));
                content.append("\n结构体版本：").append(stepsRecord.getBodyVersion());
                content.append("\n记录生成周期：").append(stepsRecord.getWeek()).append("秒");
                content.append("\n此记录块包含有效记录条数：").append(stepsRecord.getNumber());
                content.append("\n单条记录长度：").append(stepsRecord.getLength());
                content.append("\n记录类型：").append(stepsRecord.getType());
                content.append("\n计步数累加值：\n");
                for (Integer step : stepsRecord.getSteps()) {
                    content.append(step).append(" ");
                }
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_STEPS_RECORD, content.toString()));
        }

        @Override
        //获取每日活动统计
        public void OnDaysReportCallback(List<DaysReport> daysReports) {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < daysReports.size(); i++) {
                DaysReport daysReport = daysReports.get(i);
                content.append("\n第").append(i + 1).append("条记录时间：").append(DateUtil.gmtToStrDate(daysReport.getTime()));
                content.append("\n当天累计步数：").append(daysReport.getStep());
                content.append("\n当天累计消耗卡洛里：").append(daysReport.getCalorie());
                content.append("\n当天累计行程：").append(daysReport.getDistance());
                content.append("\n");
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_DAYS_REPORT, content.toString()));

        }

        @Override
        public void OnHoursReport(List<HoursReport> hoursReports) {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < hoursReports.size(); i++) {
                HoursReport hoursReport = hoursReports.get(i);
                content.append("\n第").append(i + 1).append("条记录时间：").append(DateUtil.gmtToStrDate(hoursReport.getTime()));
                content.append("\n结构体版本：").append(hoursReport.getBodyVersion());
                content.append("\n到此刻为止的累计步数：").append(hoursReport.getStep());
                content.append("\n到此刻为止的累计行走距离：").append(hoursReport.getDistance());
                content.append("\n到此刻为止的累计消耗卡路里：").append(hoursReport.getCalorie());
                content.append("\n");
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_HOURS_REPORT, content.toString()));
        }

        @Override
        public void OnExerciseReport(List<ExerciseReport> exerciseReports) {
        }

        @Override
        public void OnExprGpsDetailCallback(List<ExerGpsDetail> exertGpsDetailList) {
            StringBuilder content = new StringBuilder();
            for (ExerGpsDetail exerGpsDetail : exertGpsDetailList) {
                content.append("\n第一条记录时间：").append(exerGpsDetail.getTime());
                content.append("\n结构体版本：").append(exerGpsDetail.getBodyVersion());
                content.append("\n记录生成周期：").append(exerGpsDetail.getWeek());
                content.append("\n单条记录长度：").append(exerGpsDetail.getLength());
                content.append("\n记录类型：").append(exerGpsDetail.getType());
                content.append("\n详情：").append(exerGpsDetail.getType());
                for (ExerGpsDetail.Detail detail : exerGpsDetail.getDetails()) {
                    content.append("\n\n纬度:").append(detail.getLatitude());
                    content.append("\n经度:").append(detail.getLongitude());
                    content.append("\n速度:").append(detail.getSpeed()).append("m/s");
                    content.append("\n状态:").append(detail.getState());
                }
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_EXER_GPS_DETAIL, content.toString()));
        }

        @Override
        public void OnCurSleepRecordCallback(SleepRecord curSleepRecord) {
            StringBuilder content = new StringBuilder();
        }

        @Override
        public void OnSleepRecord(List<SleepRecord> sleepRecordList) {
            FsLogUtil.d("获取睡眠历史记录成功");
            StringBuilder content = new StringBuilder();
            int i = 0;
            for (SleepRecord sleepRecord : sleepRecordList) {
                i+=1;
                content.append("\n第 ").append(i).append(" 条记录时间：").append(DateUtil.gmtToStrDate(sleepRecord.getUtcTime()));
                content.append("\n结构体版本：").append(sleepRecord.getBodyVersion());
                content.append("\n本次开始睡觉时间：").append(DateUtil.gmtToStrDate(sleepRecord.getStartTime())).append("秒");
                content.append("\n本次结束睡觉时间：").append(DateUtil.gmtToStrDate(sleepRecord.getEndTime())).append("秒");
//                content.append("\n本次睡眠清醒累计时间：").append(sleepReport.getTotalSoberTime()).append("分");
//                content.append("\n本次睡眠浅睡累计时间：").append(sleepReport.getTotalLightTime()).append("分");
//                content.append("\n本次睡眠深睡累计时间：").append(sleepReport.getTotalDeepTime()).append("分");

                for (SleepRecord.Detail detail : sleepRecord.getDetails()) {
                    long startTime = sleepRecord.getStartTime();
                    long endTime = sleepRecord.getEndTime() ;
                    startTime = startTime * 1000;
                    endTime = endTime * 1000;
                    long durationTime = detail.getTime();
                    durationTime = durationTime * 60 * 1000;
                    LogUtils.d("睡眠历史数据startTime：" +DateUtil.gmtToStrDate(startTime /1000) +
                            "\nendTime:" + DateUtil.gmtToStrDate(endTime /1000) +
                            "\ndurationTime：" + durationTime);
                }
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_SLEEP_RECORD, content.toString()));

        }

        @Override
        public void OnSleepReport(List<SleepReport> sleepReports) {
            StringBuilder content = new StringBuilder();
            int i = 0;
            for (SleepReport sleepReport : sleepReports) {
                i += 1;
                content.append("\n第 ").append(i).append(" 条记录时间：").append(DateUtil.gmtToStrDate(sleepReport.getTime()));
                content.append("\n结构体版本：").append(sleepReport.getBodyVersion());
                content.append("\n本次开始睡觉时间：").append(DateUtil.gmtToStrDate(sleepReport.getStartTime()));
                content.append("\n本次结束睡觉时间：").append(DateUtil.gmtToStrDate(sleepReport.getEndTime()));
                content.append("\n本次睡眠持续总时间：").append(sleepReport.getTotalTime()).append("分");
                content.append("\n本次睡眠清醒累计时间：").append(sleepReport.getTotalSoberTime()).append("分");
                content.append("\n本次睡眠浅睡累计时间：").append(sleepReport.getTotalLightTime()).append("分");
                content.append("\n本次睡眠深睡累计时间：").append(sleepReport.getTotalDeepTime()).append("分");
                content.append("\n最大血氧：").append(sleepReport.getMaxBloodOxygen());
                content.append("\n最小血氧：").append(sleepReport.getMinBloodOxygen());
                content.append("\n最大心率：").append(sleepReport.getMaxHR());
                content.append("\n最小心率：").append(sleepReport.getMinHR());
                content.append("\n本次报告结果：").append(sleepReport.isEffectivity());
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_SLEEP_REPORT, content.toString()));
        }

        @Override
        public void OnSpo2Record(List<Spo2Record> spo2RecordList) {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < spo2RecordList.size(); i++) {
                Spo2Record spo2Record = spo2RecordList.get(i);
                content.append("\n第").append(i + 1).append("条记录时间：").append(DateUtil.gmtToStrDate(spo2Record.getTime(), "yyyy-MM-dd HH:mm:ss"));
                content.append("\n结构体版本：").append(spo2Record.getBodyVersion());
                content.append("\n记录生成周期：").append(spo2Record.getWeek()).append("秒");
                content.append("\n此记录块包含有效记录条数：").append(spo2Record.getNumber());
                content.append("\n单条记录长度：").append(spo2Record.getLength());
                content.append("\n记录类型：").append(spo2Record.getType());
                content.append("\n血氧值：\n");
                for (Integer integer : spo2Record.getSpList()) {
                    content.append(integer).append(" ");
                }
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_SPO2_RECORD, content.toString()));

        }

        @Override
        public void OnBloodPressureRecord(List<MentalStressRecord> mentalStressRecordList) {
            FsLogUtil.d("获取血压记录成功");
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < mentalStressRecordList.size(); i++) {
                MentalStressRecord mentalStressRecord = mentalStressRecordList.get(i);
                content.append("\n第").append(i + 1).append("条记录时间：").append(DateUtil.gmtToStrDate(mentalStressRecord.getTime()));
                content.append("\n结构体版本：").append(mentalStressRecord.getBodyVersion());
                content.append("\n记录生成周期：").append(mentalStressRecord.getWeek()).append("秒");
                content.append("\n此记录块包含有效记录条数：").append(mentalStressRecord.getNumber());
                content.append("\n单条记录长度：").append(mentalStressRecord.getLength());
                content.append("\n记录类型 ：").append(mentalStressRecord.getType());
                content.append("\n血压详情:\n");
                for (MentalStressRecord.Detail detail : mentalStressRecord.getDetails()) {
                    content.append("高：").append(detail.getMentalStress()).append(" 低：").append(detail.getMentalStressLevel()).append(" | ");
                }
                content.append("\n");
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_MENTALSTRESS_RECORD, content.toString()));
        }


        @Override
        public void OnAlarmCallBack(List<FissionAlarm> timingList) {

        }

        @Override
        public void OnLiftWristPara(LiftWristPara liftWristPara) {

        }

        @Override
        public void OnDndPara(DndRemind dndRemind) {

        }

        @Override
        public void OnHandMeasureInfo(List<HandMeasureInfoBean> handMeasureInfoBeanList) {
            StringBuilder content = new StringBuilder() ;
            for (int i = 0;i<handMeasureInfoBeanList.size();i++) {
                content.append("\n生成记录时间:" + DateUtil.gmtToStrDate(handMeasureInfoBeanList.get(i).getUtcTime()));
                content.append("\n心率值:" + handMeasureInfoBeanList.get(i).getHr());
                content.append( "\n血氧值:" + handMeasureInfoBeanList.get(i).getBlood());
                content.append("\n实时收缩血压:" + handMeasureInfoBeanList.get(i).getRealSysBp());
                content.append( "\n实时舒张血压:" + handMeasureInfoBeanList.get(i).getRealDiaBp());
                content.append("\n\n");
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_HAND_MEASURE_INFO, content.toString()));
        }
    };


    /**
     * 监听电话
     *
     * @param state
     * @param incomingNumber
     */
    public void sendCallPhone(String state, String incomingNumber) {
        //先推送来电消息,再推送来电动作
        AnyWear.callData(System.currentTimeMillis() / 1000, incomingNumber, incomingNumber, null);
        if (StringUtils.equals("IDLE", state)) {
            //挂断了电话
            AnyWear.changeCallByPhone(null, 0);
        } else if (StringUtils.equals("OFFHOOK", state)) {
            //接听了电话
            AnyWear.changeCallByPhone(null, 1);
        }  //响铃中没有动作,已经推送了消息给手环

    }

    public void getBattery() {
        AnyWear.getBattery(new BigDataCallBack() {
            @Override
            public void OnRequestBattery(int batteryValue, int batteryStatus) {
                String content = "电池电量:"+batteryValue +"电池状态:"+batteryStatus;
                EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_BATTERY, content.toString()));
            }
        });
    }

    public void getSpo2Record(long startTime, long endTime) {
        AnyWear.getSpo2Record(startTime, endTime);
    }

    public void getBloodPressureRecord(long startTime, long endTime) {
        AnyWear.getBloodPressureRecord(startTime, endTime);
    }

    public void getHeartRateRecord(long startTime, long endTime) {
        AnyWear.getHeartRateRecord(startTime, endTime);
    }

    public void getExprGpsDetail(long startTime, long endTime) {
        AnyWear.getExprGpsDetail(startTime, endTime);
    }

    public void getMeasureInfo() {
        AnyWear.getMeasureInfo();
    }

    public void getSleepRecord(long startTime, long endTime) {
        AnyWear.getSleepRecord(startTime, endTime);
    }

    public void getStepsRecord(long startTime, long endTime) {
        //获取计步记录
        AnyWear.getStepsRecord(startTime, endTime);

    }

    //高速通道
    public void switchHighCh(boolean isHigh) {
        AnyWear.setHighSpeedConnect(isHigh, new OnSmallDataCallback() {
            @Override
            public void OnBooleanResult(boolean enable) {
                EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_SET_HIGH_SPEED_CONNECT, enable ? "高速模式开" : "高速模式关"));
                if (enable) {
                    //开启
                    LogUtils.d("开启高速模式成功");
                } else {
                    //关闭
                    LogUtils.d("开启低速模式成功");
                }
            }
        });
    }

    public void connectDevice(String deviceAddress, boolean isBind, String fissionKey) {
        LogUtils.d("求这个地址" + deviceAddress);
        EventBus.getDefault().post(new ConnectedStateEvent(C.CONNECT_LOADING, SharedPreferencesUtil.getInstance().getBluetoothName()));
        if (SharedPreferencesUtil.getInstance().getFissionKey().equals("")) {
            long time = System.currentTimeMillis();
            int lastTime = (int) (time % 10000);
            int bindKey = AnyWear.bindDevice((int) (lastTime), deviceAddress);
            SharedPreferencesUtil.getInstance().setFissionKey(lastTime + "," + bindKey);
        }
        AnyWear.connectDevice(SharedPreferencesUtil.getInstance().getBluetoothName(),deviceAddress, isBind, SharedPreferencesUtil.getInstance().getFissionKey());
    }

    public void connectDevice(String deviceAddress, boolean isBind, String fissionKey, BleConnectListener listener) {
        LogUtils.d("求这个地址" + deviceAddress);
//        EventBus.getDefault().post(new ConnectedStateEvent(C.CONNECT_LOADING, SharedPreferencesUtil.getInstance().getBluetoothName()));
        if(TextUtils.isEmpty(deviceAddress)){
            FissionSdkBleManage.getInstance().connectBleDevice(deviceAddress, null, false, listener);
            return;
        }
        if (SharedPreferencesUtil.getInstance().getFissionKey().equals("")) {
            long time = System.currentTimeMillis();
            int lastTime = (int) (time % 10000);
            int bindKey = AnyWear.bindDevice((int) (lastTime), deviceAddress);
            SharedPreferencesUtil.getInstance().setFissionKey(lastTime + "," + bindKey);
        }
        BleComConfig bleComConfig = new BleComConfig();
        bleComConfig.setBind(isBind);
        bleComConfig.setBindKeys(SharedPreferencesUtil.getInstance().getFissionKey());
        if(SPUtils.getInstance().getBoolean(SpKey.IS_IC_TYPE_8763E)){
            bleComConfig.setNeedSppConnect(true);
        }else{
            bleComConfig.setNeedSppConnect(false);
        }
        FissionSdkBleManage.getInstance().connectBleDevice(deviceAddress, bleComConfig, false, listener);
    }

    public void getCurSleepRecord(long startTime, long endTime) {
        AnyWear.getCurSleepRecord();
    }


    public void startDial(byte[] data ,int type){
        int otaMode  = 0;
        if (type == FissionEnum.WRITE_DIAL_DATA){
            otaMode = 20;
        }else {
            otaMode = 6;
        }
        int finalOtaMode = otaMode;
        AnyWear.setHighSpeedConnect(true,new OnSmallDataCallback(){
            @Override
            public void OnBooleanResult(boolean enable) {
                if (enable){
                    AnyWear.checkOTA(String.valueOf(finalOtaMode), new OnCheckOtaCallback() {
                        @Override
                        public void OnError(String msg) {

                        }

                        @Override
                        public void success(String number) {
                            LogUtils.d("请求OTA 成功"+number);
                            AnyWear.writeDialAndSportData(data,type,new BigDataCallBack(){
                                @Override
                                public void OnUpdateDialProgress(int state, int progress) {
                                    if (state == FissionEnum.CUS_DIAL_UPDATE_FAILED){
                                        AnyWear.writeDailCount = 0;
                                        AnyWear.writeDailCurCount = 0;
                                        AnyWear.writeDailLength = 0;
                                    }else {
                                        LogUtils.d("获取进度"+progress);
//                                        setData(data,FissionEnum.WRITE_DIAL_DATA);
                                        EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_PUSH_CUSTOM_SPORT, progress+""));


                                    }
                                }
                            });
                        }

                    });
                }
            }
        });

    }

    //手动测量记录
    public void getHandMeasureInfo(long startTime, long endTime) {
        AnyWear.getHandMeasureInfo(startTime,endTime);
    }
}
