package com.szfission.wear.demo.activity;

import static com.szfission.wear.demo.ModelConstant.*;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;

import com.android.internal.telephony.ITelephony;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ColorUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.BdWatchMapNaviInitReqInfo;
import com.fission.wear.sdk.v2.bean.BdWatchMapPoiReq;
import com.fission.wear.sdk.v2.bean.DeviceBattery;
import com.fission.wear.sdk.v2.bean.DeviceVersion;
import com.fission.wear.sdk.v2.bean.DiskSpaceInfo;
import com.fission.wear.sdk.v2.bean.FssStatus;
import com.fission.wear.sdk.v2.bean.HsDialInfo;
import com.fission.wear.sdk.v2.bean.HsJsFileInfo;
import com.fission.wear.sdk.v2.bean.MusicConfig;
import com.fission.wear.sdk.v2.bean.SportListInfo;
import com.fission.wear.sdk.v2.bean.StreamData;
import com.fission.wear.sdk.v2.bean.SystemFunctionSwitch;
import com.fission.wear.sdk.v2.bean.TransferNotify;
import com.fission.wear.sdk.v2.bean.TransferParameterRequest;
import com.fission.wear.sdk.v2.bean.TransferRequest;
import com.fission.wear.sdk.v2.bean.TransferResponse;
import com.fission.wear.sdk.v2.callback.BaseCmdResultListener;
import com.fission.wear.sdk.v2.callback.BleConnectListener;
import com.fission.wear.sdk.v2.callback.BtConnectListener;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionFmDataResultListener;
import com.fission.wear.sdk.v2.callback.FissionRawDataResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.parse.BigDataParseManage;
import com.fission.wear.sdk.v2.parse.HiSiliconSppCmdHelper;
import com.fission.wear.sdk.v2.parse.HiSiliconSppCmdID;
import com.fission.wear.sdk.v2.parse.ParseDataListener;
import com.fission.wear.sdk.v2.utils.BaiDuAiUtils;
import com.fission.wear.sdk.v2.utils.FissionCustomDialUtil;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.HiSiTaskManage;
import com.fission.wear.sdk.v2.utils.MacUtil;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.polidea.rxandroidble2.RxBleConnection;
import com.szfission.wear.demo.App;
import com.szfission.wear.demo.C;
import com.szfission.wear.demo.ConnectedStateEvent;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.FissionSdk;
import com.szfission.wear.demo.LogAdapter;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.SharedPreferencesUtil;
import com.szfission.wear.demo.adapter.MainAdapter;
import com.szfission.wear.demo.bean.FuncBean;
import com.szfission.wear.demo.bean.FuncGroup;
import com.szfission.wear.demo.chat.DemoMessagesActivity;
import com.szfission.wear.demo.chat.StyledMessagesActivity;
import com.szfission.wear.demo.dialog.MusicProgressDialog;
import com.szfission.wear.demo.dialog.MusicVolumeDialog;
import com.szfission.wear.demo.dialog.NormalDialog;
import com.szfission.wear.demo.viewmodel.HomeViewModel;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.BloodPressureRecord;
import com.szfission.wear.sdk.bean.HrDetectPara;
import com.szfission.wear.sdk.bean.MentalStressRecord;
import com.szfission.wear.sdk.bean.DaysReport;
import com.szfission.wear.sdk.bean.ExerGpsDetail;
import com.szfission.wear.sdk.bean.ExerciseDetail;
import com.szfission.wear.sdk.bean.ExerciseList;
import com.szfission.wear.sdk.bean.ExerciseReport;
import com.szfission.wear.sdk.bean.ExerciseReportDetail;
import com.szfission.wear.sdk.bean.HandMeasureInfoBean;
import com.szfission.wear.sdk.bean.HardWareInfo;
import com.szfission.wear.sdk.bean.HeartRateRecord;
import com.szfission.wear.sdk.bean.HoursReport;
import com.szfission.wear.sdk.bean.HrWarnPara;
import com.szfission.wear.sdk.bean.MeasureInfo;
import com.szfission.wear.sdk.bean.SedentaryBean;
import com.szfission.wear.sdk.bean.SleepRecord;
import com.szfission.wear.sdk.bean.SleepReport;
import com.szfission.wear.sdk.bean.Spo2Record;
import com.szfission.wear.sdk.bean.StepsRecord;
import com.szfission.wear.sdk.bean.UserInfo;
import com.szfission.wear.sdk.bean.param.DkWaterRemind;
import com.szfission.wear.sdk.bean.param.DndRemind;
import com.szfission.wear.sdk.bean.param.LiftWristPara;
import com.szfission.wear.sdk.bean.param.SportsTargetPara;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnBleResultCallback;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;
import com.szfission.wear.sdk.ifs.OnStreamListener;
import com.szfission.wear.sdk.ifs.ReceiveMsgListener;
import com.szfission.wear.sdk.service.BigDataTaskUtil;
import com.szfission.wear.sdk.service.BleConfig;
import com.szfission.wear.sdk.util.DateUtil;
import com.szfission.wear.sdk.util.FsLogUtil;
import com.szfission.wear.sdk.util.NumberUtil;
import com.szfission.wear.sdk.util.StringUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;


public class MainActivity extends BaseActivity implements OnStreamListener, View.OnClickListener {
    TextView tvLog;
    TextView tvDeviceStatus;
    TextView tvActionConnect;
    ListView recycleMain;
    TextView tvClear;
    LogAdapter logAdapter;
    private List<String> logList;
    TextView tvQrcode;
    ExpandableListView expandView;
    Button btnStartTime;
    Button btnEndTime;
    TextView tvAppVersion;
    TextView tv_menstrual_period;
    TextView tv_synchronous_data;

    TextView tv_haisi_test;

    TextView tv_chatgpt;

    private HomeViewModel homeViewModel;
    ArrayList<ArrayList<FuncBean>> funcBeanList = new ArrayList<>();

    private Context context;

    private String deviceAddress;
    private String deviceName;
    private String deviceVersion;
    private Integer deviceBattery;

    Handler handler;
    public static boolean connectSuccessfully = false;



    private OnBleResultCallback onBleResultCallback;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    long startTime = 0;
    long endTime = (int) (System.currentTimeMillis() / 1000);

    private BaseCmdResultListener mRawDataListener = new FissionRawDataResultListener() {
        @Override
        public void onRawDataResult(String result) {
            addLog(result);
        }

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
    };

    private BaseCmdResultListener mAtCmdListener = new FissionAtCmdResultListener() {
        @Override
        public void sendSuccess(String cmdId) {
            LogUtils.d("wl", cmdId+",AT指令发送成功");
        }

        @Override
        public void sendFail(String cmdId) {
            LogUtils.d("wl", cmdId+",AT指令发送失败");
        }

        @Override
        public void onResultTimeout(String cmdId) {
            LogUtils.d("wl", cmdId+",AT指令返回结果超时");
        }

        @Override
        public void onResultError(String errorMsg) {

        }

        @Override
        public void getUiVersion(String uiVersion) {
            super.getUiVersion(uiVersion);
            showLog(R.string.FUNC_GET_UI_VERSION,uiVersion);
        }

        @Override
        public void getDeviceVersion(DeviceVersion deviceVersion) {
            super.getDeviceVersion(deviceVersion);
            showLog(R.string.FUNC_GET_VERSION, deviceVersion.toString());
        }

        @Override
        public void getDeviceBattery(DeviceBattery deviceBattery) {
            super.getDeviceBattery(deviceBattery);
            showLog(R.string.FUNC_GET_BATTERY, deviceBattery.toString());
        }

        @Override
        public void getProtocolVersion(String version) {
            super.getProtocolVersion(version);
            showLog(R.string.FUNC_GET_GPV, version);
        }

        @Override
        public void getTimes(String times) {
            super.getTimes(times);
            showLog(R.string.FUNC_GET_TIME, times);
        }

        @Override
        public void setTimes(String times) {
            super.setTimes(times);
            showLog(R.string.FUNC_SET_TIME,times);
        }

        @Override
        public void getRestingHeartRate(String heartRate) {
            super.getRestingHeartRate(heartRate);
            showLog(R.string.FUNC_GET_RESTING_HR,heartRate);
        }

        @Override
        public void getTimezone(String timezone) {
            super.getTimezone(timezone);
            showLog(R.string.FUNC_GET_TIMEZONE,timezone);
        }

        @Override
        public void setTimezone(String timezone) {
            super.setTimezone(timezone);
            showLog(R.string.FUNC_SET_TIMEZONE,timezone);
        }

        @Override
        public void setTimeFormat(boolean is24Format) {
            super.setTimeFormat(is24Format);
            showLog(R.string.FUNC_SET_TIME_MODE,is24Format? "24" : "12");
        }

        @Override
        public void setLanguage(int language) {
            super.setLanguage(language);
            showLog(R.string.FUNC_SET_LANG,"FissionEnum LG:"+language);
        }

        @Override
        public void setUnit(int unit) {
            super.setUnit(unit);
            showLog(R.string.FUNC_SET_UNIT,unit == 0? getString(R.string.imperial) : getString(R.string.metric) );
        }

        @Override
        public void setTemType() {
            super.setTemType();
            showLog(R.string.FUNC_SET_TEMPERATURE_UNIT,  "成功");
        }

        @Override
        public void isBindNewDevice(boolean isNewDevice) {
            super.isBindNewDevice(isNewDevice);
            if(isNewDevice){
                showTipDialog();
            }
        }

        @Override
        public void setSTO(String sto) {
            super.setSTO(sto);
            showLog(R.string.FUNC_SET_STO,  sto+"/成功");
        }

        @Override
        public void fssSuccess(FssStatus fssStatus) {
            super.fssSuccess(fssStatus);
            if(fssStatus.getFssType() == 35){
                FissionSdkBleManage.getInstance().setAgpsLocation(114.027901, 22.619909);
            }else if(fssStatus.getFssType() == 38){
                FissionSdkBleManage.getInstance().sendNetworkStatus("1");
            }
        }
    };

    private BaseCmdResultListener mBigDataCmdListener = new FissionBigDataCmdResultListener() {
        @Override
        public void sendSuccess(String cmdId) {
            LogUtils.d("wl", cmdId+",BigData指令发送成功");
        }

        @Override
        public void sendFail(String cmdId) {
            LogUtils.d("wl", cmdId+",BigData指令发送失败");
        }

        @Override
        public void onResultTimeout(String cmdId) {
            LogUtils.d("wl", cmdId+",BigData指令返回结果超时");
            ToastUtils.showLong(cmdId+",BigData指令返回结果超时, 请尽快检查！！");
            logList.add(cmdId+",BigData指令返回结果超时");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void onResultError(String errorMsg) {

        }

        @Override
        public void getHardwareInfo(HardWareInfo hardWareInfo) {
            super.getHardwareInfo(hardWareInfo);
            App.mHardWareInfo = hardWareInfo;
            logList.add(hardWareInfo!=null ?hardWareInfo.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getMeasureInfo(MeasureInfo measureInfo) {
            super.getMeasureInfo(measureInfo);
//            logList.clear();
            logList.add(measureInfo!=null ?measureInfo.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getDaysReport(List<DaysReport> daysReports) {
            super.getDaysReport(daysReports);
//            logList.clear();
            logList.add(daysReports!=null ?daysReports.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getHoursReport(List<HoursReport> hoursReports) {
            super.getHoursReport(hoursReports);
//            logList.clear();
            logList.add(hoursReports.toString());
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getSleepRecord(List<SleepRecord> sleepRecords) {
            super.getSleepRecord(sleepRecords);
//            logList.clear();
            logList.add(sleepRecords!=null ?sleepRecords.toString():"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getSleepReport(List<SleepReport> sleepReports) {
            super.getSleepReport(sleepReports);
//            logList.clear();
            logList.add(sleepReports!=null ?sleepReports.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getCurSleepRecord(SleepRecord sleepRecord) {
            super.getCurSleepRecord(sleepRecord);
//            logList.clear();
            logList.add(sleepRecord!=null ?sleepRecord.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getHeartRateRecord(List<HeartRateRecord> heartRateRecords) {
            super.getHeartRateRecord(heartRateRecords);
//            logList.clear();
            LogUtils.d("wl", "getHeartRateRecord:"+heartRateRecords);
//            if(heartRateRecords!=null){
//                // 获取当前日期
//                Date currentDate = new Date();
//
//                // 创建 Calendar 实例并设置为当前日期
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(currentDate);
//
//                // 设置时间为0点0分0秒
//                calendar.set(Calendar.HOUR_OF_DAY, 0);
//                calendar.set(Calendar.MINUTE, 0);
//                calendar.set(Calendar.SECOND, 0);
//                calendar.set(Calendar.MILLISECOND, 0);
//
//                // 获取当前日期0点的时间戳（毫秒）
//                long times = calendar.getTime().getTime()/1000;
//
//                int[] data = new int[144];
//                // 遍历时间戳集合，将数据填充到数组
//                for(HeartRateRecord heartRateRecord: heartRateRecords){
//                    for (int i = 0; i < heartRateRecord.getHrListTime().size(); i++) {
//                        long timestamp = heartRateRecord.getHrListTime().get(i);
//                        if(timestamp < times){
//                            continue;
//                        }
//                        int value = heartRateRecord.getHrList().get(i);
//
//                        // 计算时间戳对应的索引（每10分钟一个点）
//                        int index = (int) ((timestamp - times) / 600);
//
//                        // 将数据填充到数组中的对应索引位置
//                        data[index] = value;
//                    }
//                }
//                logList.add(Arrays.toString(data));
//            }

            logList.add(heartRateRecords!=null ?heartRateRecords.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getStepsRecord(List<StepsRecord> stepsRecords) {
            super.getStepsRecord(stepsRecords);
//            logList.clear();
            LogUtils.d("wl", "getStepsRecord:"+stepsRecords);
            logList.add(stepsRecords!=null ?stepsRecords.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getSpo2Record(List<Spo2Record> spo2Records) {
            super.getSpo2Record(spo2Records);
//            logList.clear();
            logList.add(spo2Records!=null ?spo2Records.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getMentalStressRecord(List<MentalStressRecord> mentalStressRecords) {
            super.getMentalStressRecord(mentalStressRecords);
//            logList.clear();
            logList.add(mentalStressRecords !=null ? mentalStressRecords.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getBloodPressureRecord(List<BloodPressureRecord> bloodPressureRecords) {
            super.getBloodPressureRecord(bloodPressureRecords);
//            logList.clear();
            logList.add(bloodPressureRecords!=null ?bloodPressureRecords.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getExerciseDetail(List<ExerciseDetail> exerciseDetails) {
            super.getExerciseDetail(exerciseDetails);
//            logList.clear();
            logList.add(exerciseDetails!=null ?exerciseDetails.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getExprGpsDetail(List<ExerGpsDetail> exerGpsDetails) {
            super.getExprGpsDetail(exerGpsDetails);
//            logList.clear();
            logList.add(exerGpsDetails!=null ?exerGpsDetails.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getHandMeasureInfo(List<HandMeasureInfoBean> handMeasureInfoBeans) {
            super.getHandMeasureInfo(handMeasureInfoBeans);
//            logList.clear();
            logList.add(handMeasureInfoBeans!=null ?handMeasureInfoBeans.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getBuriedData(String filepath) {
            super.getBuriedData(filepath);
            logList.add(filepath);
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getFlashData(String filepath) {
            super.getFlashData(filepath);
            logList.add(filepath);
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getSportListInfo(SportListInfo sportListInfo) {
            super.getSportListInfo(sportListInfo);
            logList.add(sportListInfo.toString());
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getSystemFunctionSwitch(SystemFunctionSwitch systemFunctionSwitch) {
            super.getSystemFunctionSwitch(systemFunctionSwitch);
            logList.add(systemFunctionSwitch.toString());
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getSystemInfo(byte[] data) {
            super.getSystemInfo(data);
            logList.add(StringUtil.bytesToHexStr(data));
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getDiskSpaceInfo(DiskSpaceInfo diskSpaceInfo) {
            super.getDiskSpaceInfo(diskSpaceInfo);
            logList.add(diskSpaceInfo.toString());
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getHsDialFileList(List<HsDialInfo> list) {
            super.getHsDialFileList(list);
            logList.add(list.toString());
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getHsJsAppFileList(List<HsJsFileInfo> list) {
            super.getHsJsAppFileList(list);
            logList.add(list.toString());
            logAdapter.notifyDataSetChanged();
        }
    };

    private FissionFmDataResultListener fmDataResultListener =new FissionFmDataResultListener() {
        @Override
        public void readStreamDataSuccess(StreamData streamData) {
//            logList.clear();
            logList.add(streamData!=null ?streamData.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void readStreamDataFail(String msg) {

        }

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
    };

    public MainActivity() {
    }

    @Override
    protected boolean useEventBus() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvLog = findViewById(R.id.tvLog);
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus);
        tvActionConnect = findViewById(R.id.tvActionConnect);
        recycleMain = findViewById(R.id.recycleMain);
        tvClear = findViewById(R.id.tvClear);
        tvQrcode = findViewById(R.id.tvQrcode);
        expandView = findViewById(R.id.expandView);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        tv_menstrual_period = findViewById(R.id.tv_menstrual_period);
        tv_synchronous_data = findViewById(R.id.tv_synchronous_data);
        tv_haisi_test = findViewById(R.id.tv_haisi_test);
        tv_chatgpt = findViewById(R.id.tv_chatgpt);

        tvLog.setOnClickListener(this);
        tvActionConnect.setOnClickListener(this);
        btnStartTime.setOnClickListener(this);
        btnEndTime.setOnClickListener(this);
        tvClear.setOnClickListener(this);

        context = this;

        registerActivityResult();
//        connectDevice();

        tvAppVersion.setText(MessageFormat.format("{0}({1})", AppUtils.getAppVersionName(), AppUtils.getAppVersionCode()));

        tvQrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// 创建IntentIntegrator对象
                IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                // 开始扫描
                intentIntegrator.initiateScan();
//                AudioUtils.enableVoiceAssistant();
//                AudioUtils.enableMainAudio(MainActivity.this);
            }
        });

        tv_menstrual_period.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MenstrualPeriodActivity.class);
                startActivity(intent);
            }
        });

        tv_synchronous_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataSynchronization();
            }
        });

        tv_haisi_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HiSiliconTestActivity.class);
                startActivity(intent);
            }
        });

        tv_chatgpt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, StyledMessagesActivity.class);
                startActivity(intent);
            }
        });

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        FsLogUtil.d("获取当天时间" + cal.getTimeInMillis() / 1000);

        Objects.requireNonNull(getSupportActionBar()).hide();
        validPermission();
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);

        logList = new ArrayList<>();
        logAdapter = new LogAdapter(context, logList);
        recycleMain.setAdapter(logAdapter);

        ArrayList<FuncGroup> groupList = new ArrayList<>();
        groupList.add(new FuncGroup(getString(R.string.at_cmd)));
        groupList.add(new FuncGroup(getString(R.string.stream_data)));
        groupList.add(new FuncGroup(getString(R.string.big_data_cmd)));
//        groupList.add(new FuncGroup("设置久坐喝水提醒"));
        funcBeanList = homeViewModel.getFuncBeans();
        MainAdapter mainAdapter = new MainAdapter(groupList, funcBeanList);
        expandView.setAdapter(mainAdapter);
        handler = new Handler();
//        lvContent.setOnItemClickListener(this);
        AnyWear.setOnStreamListener(this);
//        Date date = new Date();
//        String times = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
//        btnEndTime.setText(times);

        initDate();

        addCmdResultListener(mRawDataListener);
        addCmdResultListener(mAtCmdListener);
        addCmdResultListener(mBigDataCmdListener);
        addCmdResultListener(fmDataResultListener);


        expandView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                int type = funcBeanList.get(groupPosition).get(childPosition).getFunc();
                switch (type) {
                    case FUNC_GET_VERSION:
//                        AnyWear.getVersion();
                        FissionSdkBleManage.getInstance().getDeviceVersion();
                        break;

                    case FUNC_GET_UI_VERSION:
//                        AnyWear.getVersion();
                        FissionSdkBleManage.getInstance().getUiVersion();
                        break;

                    case FUNC_GET_BATTERY:
//                        FissionSdk.getInstance().getBattery();
                        FissionSdkBleManage.getInstance().getDeviceBattery();
                        break;
                    case FUNC_GET_TIME:
                        //时间戳
                        AnyWear.getUtcTime(new OnSmallDataCallback(){
                            @Override
                            public void OnLongResult(Long result) {
                                showLog(R.string.FUNC_GET_TIME, DateUtil.gmtToStrDate(result));
                            }
                        });
                        FissionSdkBleManage.getInstance().getTimes();
                        break;
                    case FUNC_GET_TIMEZONE:
                        //获取时区
//                        AnyWear.getTimezone(new OnSmallDataCallback() {
//                            @Override
//                            public void OnIntegerResult(int content) {
//                                showLog(R.string.FUNC_GET_TIMEZONE, "时区:"+content);
//                            }
//
//                            @Override
//                            public void OnError(String msg) {
//                                super.OnError(msg);
//                            }
//                        });
                        FissionSdkBleManage.getInstance().getTimezone();
                        break;
                    case FUNC_GET_GPV:
                        //获取协议版本
//                        AnyWear.getProtocolVersion(new OnSmallDataCallback() {
//                            @Override
//                            public void OnStringResult(String version) {
//                                showLog(R.string.FUNC_GET_GPV, version);
//                            }
//                        });
                        FissionSdkBleManage.getInstance().getProtocolVersion();
                        break;
                    case FUNC_GET_RESTING_HR:
//                        //获取协议版本
//                        AnyWear.getRestingHr(new OnSmallDataCallback() {
//                            @Override
//                            public void OnStringResult(String content) {
//                                showLog(R.string.FUNC_GET_RESTING_HR, content);
//                            }
//                        });
                        FissionSdkBleManage.getInstance().getRestingHeartRate();
                        break;
                    case FUNC_SET_TIME:
                        //同步时间
//                        AnyWear.setUtcTime(System.currentTimeMillis() / 1000,new OnSmallDataCallback() {
//                            @Override
//                            public void OnLongResult(Long result) {
//                                showLog(R.string.FUNC_SET_TIME, DateUtil.gmtToStrDate(result));
//                                //传入的参数为 Long 型,则回调 Long 的接口方法。
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setTimes();
                        break;

                    case FUNC_SET_ANY_TIME:
                        startActivity(new Intent(context, SetAnyTimesActivity.class));
                        break;

                    case FUNC_SET_TIMEZONE:
                        showEditDialog(FUNC_SET_TIMEZONE);
                        break;

                    case FUNC_SET_FUNC_SET_STO:
                        showEditDialogSTO();
                        break;

                    case FUNC_SET_TIME_MODE:
                        showCheckModelDialog(FUNC_SET_TIME_MODE);
                        break;
                    case FUNC_SET_UNIT:
                        showCheckModelDialog(FUNC_SET_UNIT);
                        break;
                    case FUNC_SET_TEMPERATURE_UNIT:
                        showCheckModelDialog(FUNC_SET_TEMPERATURE_UNIT);
                        break;
                    case FUNC_SET_LANG:
                        showCheckModelDialog(FUNC_SET_LANG);
                        break;
                    case FUNC_SET_FEMALE_PHYSIOLOGY:
                        showCheckModelDialog(FUNC_SET_FEMALE_PHYSIOLOGY);
                        break;
                    case FUNC_VIBRATION:
                        showCheckModelDialog(FUNC_VIBRATION);
                        break;
                    case FUNC_SET_WRIST_BRIGHT_SCREEN:
                        showCheckModelDialog(FUNC_SET_WRIST_BRIGHT_SCREEN);
                        break;
                    case FUNC_CAMERA_MODEL:
                        showCheckModelDialog(FUNC_CAMERA_MODEL);
                        break;
                    case FUNC_SET_DATA_STREAM:
                        showEditDialog(FUNC_SET_DATA_STREAM);
                        break;

                    case FUNC_SET_DATA_STREAM2:
                        showEditDialog(FUNC_SET_DATA_STREAM2);
                        break;

                    case FUNC_SET_HIGH_SPEED_CONNECT:
                        showCheckModelDialog(FUNC_SET_HIGH_SPEED_CONNECT);
                        break;
                    case FUNC_SWITCH_HR_RATE:
                        showCheckModelDialog(FUNC_SWITCH_HR_RATE);
                        break;
                    case FUNC_SET_BLOOD_OXYGEN_SWITCH:
                        showCheckModelDialog(FUNC_SET_BLOOD_OXYGEN_SWITCH);
                        break;
                    case FUNC_SET_MENTAL_STRESS_SWITCH:
                        showCheckModelDialog(FUNC_SET_MENTAL_STRESS_SWITCH);
                        break;

                    case FUNC_SET_HEART_RATE_SWITCH:
                        showCheckModelDialog(FUNC_SET_HEART_RATE_SWITCH);
                        break;

                    case FUNC_SET_CALL_AUDIO_SWITCH:
                        showCheckModelDialog(FUNC_SET_CALL_AUDIO_SWITCH);
                        break;

                    case FUNC_SET_MEDIA_AUDIO_SWITCH:
                        showCheckModelDialog(FUNC_SET_MEDIA_AUDIO_SWITCH);
                        break;

                    case ModelConstant.FUNC_CAMERA:
                        AnyWear.camera(new OnSmallDataCallback() {
                            @Override
                            public void OnEmptyResult() {
                                showLog(R.string.FUNC_CAMERA, "成功");
                            }

                            @Override
                            public void OnError(String msg) {
                                showToast(msg);
                            }
                        });
                        break;
                    case FUNC_FIND_DEVICE:
//                        AnyWear.findDevice(new OnSmallDataCallback() {
//                            @Override
//                            public void OnEmptyResult() {
//                                showLog(R.string.FUNC_FIND_DEVICE, "成功");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().findDevice();
                        break;
                    case FUNC_GIVE_UP_FIND_DEVICE:
//                        AnyWear.giveUpFindDevice(new OnSmallDataCallback() {
//                            @Override
//                            public void OnEmptyResult() {
//                                showLog(R.string.FUNC_GIVE_UP_FIND_DEVICE, "成功");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().cancelFindPhone();
                        break;
                    case FUNC_SET_PROMPT:
                        showCheckModelDialog(FUNC_SET_PROMPT);

                        break;
                    case ModelConstant.FUNC_BREAK_DEVICE:
//                        AnyWear.breakDevice(new OnSmallDataCallback() {
//                            @Override
//                            public void OnEmptyResult() {
//                                showLog(R.string.FUNC_BREAK_DEVICE, "成功");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().disconnectBleDevice();
                        break;
                    case FUNC_REBOOT_DEVICE:
//                        AnyWear.reboot(new OnSmallDataCallback() {
//                            @Override
//                            public void OnEmptyResult() {
//                                showLog(R.string.FUNC_REBOOT_DEVICE, "成功");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().rebootDevice();
                        break;

                    case FUNC_SHUTDOWN_STATE:
                        FissionSdkBleManage.getInstance().setShutdownState();
                        break;

                    case FUNC_RESET:
//                        AnyWear.restoreFactory(new OnSmallDataCallback() {
//                            @Override
//                            public void OnEmptyResult() {
//                                showLog(R.string.FUNC_RESET, "成功");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().resetDevice();
                        break;

                    case FUNC_SHUTDOWN:
//                        AnyWear.shutdown(new OnSmallDataCallback() {
//                            @Override
//                            public void OnEmptyResult() {
//                                showLog(R.string.FUNC_SHUTDOWN, "成功");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().shutdown();
                        break;

                    case FUNC_OTA:
                        startActivity(new Intent(context, OTAUpdateActivity.class));
                        break;
                    case FUNC_PUSH_CUSTOM_DIAL:
                        startActivity(new Intent(context, CustomDialActivity.class));
                        break;
                    case FUNC_PUSH_CUSTOM_DIAL_NEW:
                        startActivity(new Intent(context, NewCustomDialActivity.class));
                        break;
                    case FUNC_PUSH_CUSTOM_SPORT:
                        startActivity(new Intent(context, PushSportModeActivity.class));
                        break;
                    case FUNC_GPS_SPORT_CMD:
                        startActivity(new Intent(context, CommunicatGpsActivity.class));
                        break;
                    case FUNC_FLASH_WRITE_CMD:
                        startActivity(new Intent(context, WriteFlashDataActivity.class));
                        break;
                    case FUNC_COMPRESS_CMD:
                        startActivity(new Intent(context, CompressDataActivity.class));
                        break;
                    case FUNC_ONLINE_DIAL_PUSH:
                        startActivity(new Intent(context, OnlineDialPushActivity.class));
                        break;
                    case FUNC_PUSH_QLZ_DATA:
                        startActivity(new Intent(context, PushQlzDataActivity.class));
                        break;
                    case FUNC_PUSH_MORE_SPORT:
                        startActivity(new Intent(context, PushMoreSportsActivity.class));
                        break;
                    case FUNC_SAFETY_CONFIRM:
                        showEditDialog(FUNC_SAFETY_CONFIRM);
                        break;
                    case FUNC_SELF_INSPECTION_MODE:
                        showCheckModelDialog(FUNC_SELF_INSPECTION_MODE);
                        break;
                    case FUNC_CLEAR_USER_INFO:
                        AnyWear.clearUserInfo(new OnSmallDataCallback() {
                            @Override
                            public void OnEmptyResult() {
                                showLog(R.string.FUNC_CLEAR_USER_INFO, "成功");
                            }

                            @Override
                            public void OnError(String msg) {
                                showToast(msg);
                            }
                        });
                        break;
                    case FUNC_CLEAR_SPORT:
                        AnyWear.clearSportData(new OnSmallDataCallback() {
                            @Override
                            public void OnEmptyResult() {
                                showLog(R.string.FUNC_CLEAR_SPORT, "成功");
                            }

                            @Override
                            public void OnError(String msg) {
                                showToast(msg);
                            }
                        });
                        break;
                    case FUNC_PAGE_SKIP:
                        showEditDialog(FUNC_PAGE_SKIP);
                        break;
                    case FUNC_MUSIC_VOLUME:
                        MusicVolumeDialog musicVolumeDialog = new MusicVolumeDialog(context);
                        musicVolumeDialog.setOnConfirmClickListener(new MusicVolumeDialog.OnConfirmClickListener() {
                            @Override
                            public void confirm(int max, int progress) {
//                                AnyWear.setMusicVolume(new OnMusicVolumeCallback() {
//                                    @Override
//                                    public void volumeSuccess(int progress, int max) {
//                                        showLog(R.string.FUNC_MUSIC_VOLUME, "最大音量：" + max + " 当前音量：" + progress);
//                                    }
//
//                                    @Override
//                                    public void OnError(String msg) {
//                                        showToast(msg);
//                                    }
//                                }, progress, max);
                                MusicConfig musicConfig = new MusicConfig();
                                musicConfig.setOperationType(MusicConfig.OPERATION_TYPE_VOLUME);
                                musicConfig.setCurrentVolume(progress);
                                musicConfig.setMaxVolume(max);
                                FissionSdkBleManage.getInstance().setMusicVolume(musicConfig);
                            }
                        });
                        musicVolumeDialog.create();
                        musicVolumeDialog.show();
                        break;
                    case FUNC_MUSIC_CONTROL:
                        showMusicControlDialog();
                        break;
                    case FUNC_MUSIC_PROGRESS:
                        MusicProgressDialog musicProgressDialog = new MusicProgressDialog(context);
                        musicProgressDialog.setOnConfirmClickListener(new MusicProgressDialog.OnConfirmClickListener() {
                            @Override
                            public void confirm(int max, int progress) {
//                                AnyWear.setMusicProgress(new OnMusicProgressCallback() {
//                                    @Override
//                                    public void success(int progress, int max) {
//                                        showLog(R.string.FUNC_MUSIC_PROGRESS, "总时间：" + max + "秒 进度" + progress + "秒");
//                                    }
//
//                                    @Override
//                                    public void OnError(String msg) {
//                                        showToast(msg);
//                                    }
//                                }, progress, max);

                                MusicConfig musicConfig = new MusicConfig();
                                musicConfig.setOperationType(MusicConfig.OPERATION_TYPE_PLAYBACK_PROGRESS);
                                musicConfig.setProgress(progress);
                                musicConfig.setDuration(max);
                                FissionSdkBleManage.getInstance().setMusicVolume(musicConfig);
                            }
                        });
                        musicProgressDialog.create();
                        musicProgressDialog.show();
                        break;
                    case FUNC_GET_HARDWARE_INFO:
                        //获取硬件信息
//                        FissionSdk.getInstance().getHardWareInfo();
                        FissionSdkBleManage.getInstance().getHardwareInfo();
                        break;

                    case FUNC_GET_SYSTEM_INFO:
                        //获取当前系统动态信息
                        FissionSdkBleManage.getInstance().getSystemInfo();
                        break;

                    case FUNC_GET_MEASURE_INFO:
//                        FissionSdk.getInstance().getMeasureInfo();
                        FissionSdkBleManage.getInstance().getMeasureInfo();
                        break;
                    case FUNC_GET_DAYS_REPORT:
                        //获取每日活动统计
//                        FissionSdk.getInstance().getDaysReport(startTime,endTime);
                        FissionSdkBleManage.getInstance().getDaysReport(startTime,endTime);
                        break;
                    case FUNC_GET_HOURS_REPORT:
                        //获取整点活动统计方法
//                        FissionSdk.getInstance().getHoursReport(startTime, endTime);
                        FissionSdkBleManage.getInstance().getHoursReport(startTime, endTime);
                        break;
                    case FUNC_GET_SLEEP_RECORD:
//                        FissionSdk.getInstance().getSleepRecord(startTime,endTime);
                        FissionSdkBleManage.getInstance().getSleepRecord(startTime, endTime);
                        break;
                    case FUNC_GET_SLEEP_REPORT:
//                        FissionSdk.getInstance().getSleepReport(startTime,endTime);
                        FissionSdkBleManage.getInstance().getSleepReport(startTime, endTime);
                        break;
                    case FUNC_GET_CUR_SLEEP_RECORD:
//                        FissionSdk.getInstance().getCurSleepRecord(startTime,endTime);
                        FissionSdkBleManage.getInstance().getCurSleepRecord();
                        break;
                    case FUNC_GET_EXERCISE_LIST:
                        Intent i = new Intent(MainActivity.this,ExercisesListActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putLong("startTime",startTime);
                        bundle.putLong("endTime",endTime);
                        i.putExtras(bundle);
                        startActivity(i);
                        break;
                    case FUNC_GET_EXERCISE_REPORT:
                        AnyWear.getExerciseReport(startTime, endTime, new BigDataCallBack() {
                            @Override
                            public void OnExerciseReport(List<ExerciseReport> exerciseReports) {
                                int distance = 0;
                                int calorie = 0;
                                showSuccessToast(R.string.FUNC_GET_EXERCISE_REPORT);
                                StringBuilder content = new StringBuilder();
                                for (int i = 0; i < exerciseReports.size(); i++) {
                                    ExerciseReport exerciseReport = exerciseReports.get(i);
                                    FsLogUtil.d("抓取utc" + exerciseReport.getUtcTime());
                                    content.append("\n第").append(i + 1).append("个时间：").append(DateUtil.gmtToStrDate(exerciseReport.getUtcTime()));
                                    content.append("\n结构体版本：").append(DateUtil.gmtToStrDate(exerciseReport.getBodyVersion()));
                                    content.append("\n开始时间：").append(DateUtil.gmtToStrDate(exerciseReport.getBeginTime()));
                                    content.append("\n结束时间：").append(DateUtil.gmtToStrDate(exerciseReport.getEndTime()));
                                    content.append("\n总时间：").append(exerciseReport.getTotalTime()).append("秒");
                                    content.append("\n总步数：").append(exerciseReport.getTotalStep());
                                    content.append("\n总卡路里：").append(exerciseReport.getTotalCalorie());
                                    content.append("\n总距离：").append(exerciseReport.getTotalDistance() + "米");
                                    content.append("\n轨迹运动距离：").append(exerciseReport.getTotalTrackDistance());
                                    content.append("\n运动模式：").append(exerciseReport.getModel());
                                    distance = distance + exerciseReport.getTotalDistance();
                                    FsLogUtil.d("运动报告总距离" + distance);
                                    content.append("\n本次运动最大心率：").append(exerciseReport.getHighHR() + "次/分钟");
                                    content.append("\n本次运动最小心率：").append(exerciseReport.getLowHR() + "次/分钟");
                                    content.append("\n本次运动平均心率：").append(exerciseReport.getAvgHR() + "次/分钟");
                                    content.append("\n本次运动最大步频：").append(exerciseReport.getMaxStride() + "步/分钟");
                                    content.append("\n本次运动平均步频：").append(exerciseReport.getAvgStride() + "步/分钟");
                                    content.append("\n运动次数：").append(exerciseReport.getSportCount());
                                    content.append("\n最大速度：").append(exerciseReport.getMaxSpeed() + "米/秒");
                                    content.append("\n平均速度：").append(exerciseReport.getAvgSpeed() + "米/秒");
                                    content.append("\n无轨迹运动平均配速：").append(exerciseReport.getNotTrackAvgSpeed() + "秒/公里");
                                    content.append("\n有轨迹运动配速：").append(exerciseReport.getHasTrackAvgSpeed() + "秒/公里");
                                    content.append("\n重复运动的周期数：").append(exerciseReport.getRepeatSportWeek() + "圈");
                                    content.append("\n摆臂次数：").append(exerciseReport.getSwingNumber() + "次");
                                    content.append("\n中断UTC记录:");
                                    if (exerciseReport.getDetails().size() > 0) {
                                        for (ExerciseReportDetail detail : exerciseReport.getDetails()) {
                                            content.append("\n    暂停时间：").append(DateUtil.gmtToStrDate(detail.getPauseTime())).append(" 重新开始时间：").append(DateUtil.gmtToStrDate(detail.getStartTime()));
                                        }
                                    }
//                                    content.append("\n第").append(i + 1).append("条运动详情记录块的起始地址:").append(exerciseReport.getFirstSportAddress());
//                                    content.append("\n第").append(i + 1).append("条运动GPS信息记录块的起始地址:").append(exerciseReport.getFirstGpsAddress());
//                                    content.append("\n本次运动产生的详情记录块数:").append(exerciseReport.getTotalBodyNumber());
//                                    content.append("\n记录详情GPS记录块生成周期:").append(exerciseReport.getTotalGpsNumber() + "秒");
                                    content.append("\n");
                                }
                                showLog(R.string.FUNC_GET_EXERCISE_REPORT,context.toString());
                            }
                        });
                        break;
                    case FUNC_GET_EXERCISE_GPS:
                        startActivity(new Intent(MainActivity.this, ExercisesGpsChangeActivity.class));
                        break;
                    case FUNC_GET_HEARTED_RECORD:
//                        FissionSdk.getInstance().getHeartRateRecord(startTime,endTime);
                        FissionSdkBleManage.getInstance().getHeartRateRecord(startTime,endTime);
                        break;
                    case FUNC_SET_HR_WARN_PARA:
                        startActivity(new Intent(MainActivity.this,SetHrWarnParaActivity.class));
                        break;
                    case FUNC_GET_EXERCISE_DETAIL:
                        FissionSdkBleManage.getInstance().getExerciseDetail(startTime,endTime);
                        break;
                    case FUNC_GET_EXER_GPS_DETAIL:
//                        FissionSdk.getInstance().getExprGpsDetail(startTime,endTime);
                        FissionSdkBleManage.getInstance().getExprGpsDetail(startTime,endTime);
                        break;

                    case FUNC_GET_PERSONAL_INFO:
                        startActivity(new Intent(context, SetUserInfoActivity.class));
                        break;
                    case FUNC_GET_SEDENTARY_PARA:
                        startActivity(new Intent(context, SetSedentaryReminderActivity.class));
                        break;
                    case FUNC_GET_APPS_MESS:
                        startActivity(new Intent(context, AppMessageActivity.class));
                        break;
                    case FUNC_SET_HRLEV_ALGO_PARA:
                        startActivity(new Intent(context, SetHrlevAlgoParaActivity.class));
                        break;
                    case FUNC_SET_DRINK_WATER_PARA:
                        //喝水判定参数
                        startActivity(new Intent(context, SetDrinkWaterParaActivity.class));
                        break;
                    case FUNC_SET_DONT_DISTURB_PARA:
                        //勿扰参数
                        startActivity(new Intent(context, SetNotDisturbParaActivity.class));
                        break;
                    case FUNC_SET_HR_CHECK_PARA:
                        //心率检测参数
                        startActivity(new Intent(context, SetHrCheckParaActivity.class));
                        break;
                    case FUNC_SET_LIFTWRIST_PARA:
                        //抬腕亮屏参数
                        startActivity(new Intent(context, SetLiftWristParaActivity.class));
                        break;
                    case FUNC_SET_TARGET_SET:
                        startActivity(new Intent(context, SetTargetParaActivity.class));
                        //运动目标参数
                        break;
                    case FUNC_SET_TIMING_INFO:
                        startActivity(new Intent(context, SetTimingInfoActivity.class));
                        //闹铃信息
                        break;
                    case FUNC_SET_MESSAGE_TYPE_PARA:
                        startActivity(new Intent(context, SetMessageTypeParaActivity.class));
                        //推送消息开关参数
                        break;
                    case FUNC_GET_STEPS_RECORD:
//                        FissionSdk.getInstance().getStepsRecord(startTime,endTime);
                        FissionSdkBleManage.getInstance().getStepsRecord(startTime,endTime);
                        break;
                    case FUNC_GET_SPO2_RECORD:
//                        FissionSdk.getInstance().getSpo2Record(startTime,endTime);
                        FissionSdkBleManage.getInstance().getSpo2Record(startTime,endTime);
                        break;
                    case FUNC_GET_MENTALSTRESS_RECORD:
                        //获取精神压力记录
                        FissionSdkBleManage.getInstance().getMentalStressRecord(startTime,endTime);
                        break;

                    case FUNC_GET_BLOODPRESSURE_RECORD:
                        //获取血压记录
//                        FissionSdk.getInstance().getBloodPressureRecord(startTime,endTime);
                        FissionSdkBleManage.getInstance().getBloodPressureRecord(startTime,endTime);
                        break;

                    case FUNC_WEATHER:
                        //推送天气消息
                        startActivity(new Intent(context, SetWeatherActivity.class));
                        break;
                    case FUNC_WEATHER_DETAIL:
                        startActivity(new Intent(context, SetWeatherDetailActivity.class));
                        break;
                    case FUNC_STRU_CALL_DATA:
                        startActivity(new Intent(context, PhoneCallActivity.class));
                        break;
                    case FUNC_STRU_MUSIC_CONT:
                        startActivity(new Intent(context, MusicActivity.class));
                        break;
                    case FUNC_LOCATION_INFORMATION:
                        startActivity(new Intent(context, SetLcInfoActivity.class));
                        break;

                    case FUNC_QUICK_REPLY_INFO:
                        startActivity(new Intent(context, QuickReplyActivity.class));
                        break;

                    case FUNC_GET_BURIED_DATA:
                        if (Build.VERSION.SDK_INT >= 30 ){
                            // 先判断有没有权限
                            if (Environment.isExternalStorageManager()) {
                                FissionSdkBleManage.getInstance().getBuriedData();
                                ToastUtils.showLong("获取设备埋点数据");
                            } else {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                                startActivity(intent);
                            }
                        }
                        break;

                    case FUNC_SYN_PHONE_BOOK:
                        startActivity(new Intent(context, PhoneBookActivity.class));
                        break;

                    case FUNC_GET_FLASH_DATA:
                        startActivity(new Intent(context, GetFlashDataActivity.class));
                        break;

                    case FUNC_GET_HAND_MEASURE_INFO:
                        //手动测量记录
//                        FissionSdk.getInstance().getHandMeasureInfo(startTime,endTime);
                        FissionSdkBleManage.getInstance().getHandMeasureInfo(startTime,endTime);
                        FissionSdkBleManage.getInstance().getNewHandMeasureInfo(startTime,endTime);
                        break;

                    case FUNC_GET_HRPS_DETAIL:
                        FissionSdkBleManage.getInstance().getHrpsDetailRecord(startTime,endTime);
                        break;

                    case FUNC_GET_CALL_AUDIO_SWITCH:
                        FissionSdkBleManage.getInstance().getCallAudioSwitch();
                        break;

                    case FUNC_GET_MEDIA_AUDIO_SWITCH:
                        FissionSdkBleManage.getInstance().getMediaAudioSwitch();
                        break;

                    case FUNC_GET_SEDENTARY_DRINK_PARA:
                        StringBuilder stringBuilder = new StringBuilder();
                        DkWaterRemind dkWaterRemind = new DkWaterRemind();
                        dkWaterRemind.setStartTime(0);
                        dkWaterRemind.setEndTime(1320);
                        dkWaterRemind.setEnable(true);
                        dkWaterRemind.setRemindWeek(3);

                        AnyWear.setDrinkWaterPara(dkWaterRemind, new OnSmallDataCallback(){
                            @Override
                            public void OnEmptyResult() {
                                stringBuilder.append(getString(R.string.FUNC_SET_DRINK_WATER_PARA)).append("设置成功");
                            }
                        });
                        SedentaryBean sedentaryBean = new SedentaryBean();
                        sedentaryBean.setEnable(true);
                        sedentaryBean.setStartTime(0);
                        sedentaryBean.setEndTime(1340);
                        sedentaryBean.setDurTime(5);
                        sedentaryBean.setTargetStep(100);

                        AnyWear.setSedentaryPara(sedentaryBean, new OnSmallDataCallback(){
                            @Override
                            public void OnError(String msg) {
                                showToast(msg);
                                dismissProgress();
                            }


                            @Override
                            public void OnEmptyResult() {
                                stringBuilder.append("\n").append(getString(R.string.FUNC_GET_SEDENTARY_PARA)).append("设置成功");
                                showLog(R.string.FUNC_GET_SEDENTARY_DRINK_PARA,stringBuilder.toString());
                            }
                        });
                        break;

                    case FUNC_GET_SPORT_LIST_INFO:
                        FissionSdkBleManage.getInstance().getSportListInfo();
                        break;

                    case FUNC_GET_SYSTEM_FUNCTION_SWITCH:
                        FissionSdkBleManage.getInstance().getSystemFunctionSwitch();
                        break;

                    case FUNC_SET_AGPS_LOCATION:
                        startActivity(new Intent(context, PushAgpsLocationActivity.class));
                        break;

                    case FUNC_SET_AGPS_DATA:
                        startActivity(new Intent(context, PushAgpsDataActivity.class));
                        break;

                    case FUNC_SET_SN_CMEI:
                        startActivity(new Intent(context, SetSnAndCmeiActivity.class));
                        break;

                    case FUNC_NOTES_REMINDERS:
                        startActivity(new Intent(context, NotesRemindersActivity.class));
                        break;

                    case FUNC_GET_DISK_SPACE_INFO:
                        FissionSdkBleManage.getInstance().getDiskSpaceInfo();
                        break;

                    case FUNC_GET_HS_FILE_LIST:
                        startActivity(new Intent(context, FileListActivity.class));
                        break;

                    case FUNC_SET_MAC:
                        String mac = MacUtil.generateBluetoothMacAddress().replace(":", "").trim();
                        logList.add("设置随机mac地址：" + mac);
                        logAdapter.notifyDataSetChanged();
                        FissionLogUtils.d("wl", "设置随机mac地址："+mac);
                        FissionSdkBleManage.getInstance().setMAC("000000000000");
                        break;
                }
                return true;
            }


        });


//        AnyWear.findPhoneCallback = findPhoneCallbak;
        final boolean[] isTv = {false};
        onBleResultCallback = new OnBleResultCallback() {
            @Override
            public void success(String content) {
            }

            @Override
            public void addSendLength(int length) {
                showLog(R.string.sendBytes, "Send Bytes: :" + length);
                logList.add("Send Bytes :" + length);
                logAdapter.notifyDataSetChanged();
            }

            @Override
            public void addReceiverLength(int length) {
                logList.add("Received Bytes :" + length);
                logAdapter.notifyDataSetChanged();
            }
        };
        AnyWear.onBleResultCallback = onBleResultCallback;
    }

    private void showLog(int funcType, String content) {
//        showSuccessToast(funcType);
        addLog(funcType, content);
        addCurLog(funcType, content);
    }

    private void showCheckModelDialog(int modelType) {
        NormalDialog normalDialog = new NormalDialog(MainActivity.this,2,modelType);
        normalDialog.setOnConfirmClickListener(new NormalDialog.OnConfirmClickListener() {
            @Override
            public void confirm(String content) {
                int value = Integer.parseInt(content);
                switch (modelType) {
                    case FUNC_SET_TIME_MODE:
//                        AnyWear.setTimeModel(value==1,new OnSmallDataCallback(){
//                            @Override
//                            public void OnBooleanResult(boolean enable) {
//                                showLog(R.string.FUNC_SET_TIME_MODE,enable?"24小时制":"12小时制");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setTimeFormat(value==1);
                        break;
                    case FUNC_SET_LANG:
//                        AnyWear.setLanguage(value, new OnSmallDataCallback(){
//                            @Override
//                            public void OnIntegerResult(int lgType) {
//                                showLog(R.string.FUNC_SET_LANG,"语言类型:"+lgType);
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setLanguage(value);
                        break;
                    case FUNC_SET_UNIT:
//                        AnyWear.setUnit(value, new OnSmallDataCallback(){
//                            @Override
//                            public void OnIntegerResult(int content) {
//                                showLog(R.string.FUNC_SET_UNIT,content==1?"公制":"英制");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setUnit(value);
                        break;

                    case FUNC_SET_TEMPERATURE_UNIT:
                        FissionSdkBleManage.getInstance().setTemType(value == 0);
                        break;

                    case FUNC_VIBRATION:
//                        AnyWear.switchVibration(value == 1,new OnSmallDataCallback(){
//                            @Override
//                            public void OnBooleanResult(boolean enable) {
//                                showLog(R.string.FUNC_VIBRATION,enable?"震动开":"震动关");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setSwitchVibration(value == 1);
                        break;
                    case FUNC_SET_WRIST_BRIGHT_SCREEN:
//                        AnyWear.switchWBScreen(value == 1, new OnSmallDataCallback() {
//                            @Override
//                            public void OnBooleanResult(boolean enable) {
//                                showLog(R.string.FUNC_SET_WRIST_BRIGHT_SCREEN,enable?"抬腕亮屏开":"抬腕亮屏关");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setSwitchWBScreen(value == 1);
                        break;
                    case FUNC_CAMERA_MODEL:
//                        AnyWear.switchPhotoModel(value == 1, new OnSmallDataCallback() {
//                            @Override
//                            public void OnBooleanResult(boolean enable) {
//                                showLog(R.string.FUNC_CAMERA_MODEL,enable?"相机开":"相机关");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setSwitchPhotoMode(value == 1);
                        break;
                    case FUNC_SET_HIGH_SPEED_CONNECT:
//                        FissionSdk.getInstance().switchHighCh(value==1);
                        FissionSdkBleManage.getInstance().setSwitchHighCh(value == 1);
                        break;
                    case FUNC_SET_FEMALE_PHYSIOLOGY:
//                        AnyWear.setFemalePhysiology(value, new OnSmallDataCallback() {
//                            @Override
//                            public void OnStringResult(String content) {
//                                showLog(R.string.FUNC_SET_FEMALE_PHYSIOLOGY,content);
//                            }
//                        });
                        break;
                    case FUNC_SELF_INSPECTION_MODE:
//                        AnyWear.switchSelfInspectionMode(value==1, new OnSmallDataCallback() {
//                            @Override
//                            public void OnBooleanResult(boolean enable)  {
//                                showLog(R.string.FUNC_SELF_INSPECTION_MODE,enable?"自检模式开":"自检模式关");
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setSwitchSelfInspectionMode(value == 1);
                        break;
                    case FUNC_SWITCH_HR_RATE:
                        FissionSdkBleManage.getInstance().switchHrRate(value==1);
//                        AnyWear.switchHrRate(value==1,new OnSmallDataCallback(){
//                            @Override
//                            public void OnBooleanResult(boolean enable) {
//                                showLog(R.string.FUNC_SWITCH_HR_RATE,enable?"心率检测开启":"心率检测关闭");
//                            }
//                        });
                        break;
                    case FUNC_SET_PROMPT:
//                        AnyWear.setPromptFuc(1,value, new OnSmallDataCallback(){
//                            @Override
//                            public void OnStringResult(String content) {
//                                showLog(R.string.FUNC_SET_PROMPT,"运动心率超高提示:"+content);
//                            }
//                        });
                        FissionSdkBleManage.getInstance().setHeartRateHighTips(1,value);
                        break;

                    case FUNC_SET_BLOOD_OXYGEN_SWITCH:
                        FissionSdkBleManage.getInstance().setBloodOxygenSwitch(value);
                        break;

                    case FUNC_SET_MENTAL_STRESS_SWITCH:
                        FissionSdkBleManage.getInstance().setMentalStressSwitch(value);
                        break;

                    case FUNC_SET_HEART_RATE_SWITCH:
                        FissionSdkBleManage.getInstance().setHeartRateSwitch(value);
                        break;

                    case FUNC_SET_CALL_AUDIO_SWITCH:
                        FissionSdkBleManage.getInstance().setCallAudioSwitch(value);
                        break;

                    case FUNC_SET_MEDIA_AUDIO_SWITCH:
                        FissionSdkBleManage.getInstance().setMediaAudioSwitch(value);
                        break;

                }
            }
        });
    }

    private void showEditDialogSTO() {
        NormalDialog normalDialog = new NormalDialog(MainActivity.this,3,FUNC_SET_FUNC_SET_STO);
        normalDialog.setOnConfirmClickListener(content -> {
            FissionSdkBleManage.getInstance().setSTO(Integer.parseInt(content));
        });
    }

    private void showEditDialog(int funcType) {
        NormalDialog normalDialog = new NormalDialog(MainActivity.this,1,funcType);
        normalDialog.setOnConfirmClickListener(content -> {
            switch (funcType) {
                case FUNC_SET_TIMEZONE:
                    //TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (3600 * 1000)
//                    AnyWear.setTimezone(Integer.parseInt(content), new OnSmallDataCallback() {
//                        @Override
//                        public void OnIntegerResult(int result) {
//                            showLog(R.string.FUNC_SET_TIMEZONE, "设置时区:"+result);
//                        }
//                    });
                    FissionSdkBleManage.getInstance().setTimezone(Integer.parseInt(content));
                    break;
                case FUNC_SET_DATA_STREAM:
                    FissionSdkBleManage.getInstance().setDataStream(Integer.parseInt(content));
//                    AnyWear.setDataStream(Integer.parseInt(content), new OnSmallDataCallback() {
//                        @Override
//                        public void OnStringResult(String content) {
//                            showLog(R.string.FUNC_SET_DATA_STREAM, content);
//                        }
//                    });
                    break;
                case FUNC_SAFETY_CONFIRM:
//                    AnyWear.setSafetyConfirm(content, new OnSmallDataCallback() {
//                        @Override
//                        public void OnStringResult(String content) {
//                            showLog(R.string.FUNC_SAFETY_CONFIRM, content);
//                        }
//                    });
                    FissionSdkBleManage.getInstance().safetyConfirmation(content);
                    break;
                case FUNC_PAGE_SKIP:
//                    AnyWear.setPageSkip(content, new OnSmallDataCallback() {
//                        @Override
//                        public void OnStringResult(String content) {
//                            showLog(R.string.FUNC_PAGE_SKIP, content);
//                        }
//                    });
                    FissionSdkBleManage.getInstance().setPageSkip(content);
                    break;

                case FUNC_SET_DATA_STREAM2:
                    FissionSdkBleManage.getInstance().setDataStream2(Integer.parseInt(content));
                    break;
            }
        });
    }


    private void registerActivityResult() {
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (data != null) {
                int resultCode = result.getResultCode();
                if (resultCode == -1) {
                    deviceName = data.getStringExtra("name");
                    deviceAddress = data.getStringExtra("macAddress");
                    tvDeviceStatus.setText(R.string.device_connecting);
                    showLog(R.string.device_connecting,deviceName);
                    connectSuccessfully = true;
                    tvActionConnect.setText(R.string.disconnect);
                    if(!TextUtils.isEmpty(deviceName) && (deviceName.contains("LW71") || deviceName.contains("LW76") || deviceName.contains("LW82") || deviceName.contains("LW83") || deviceName.contains("LW77")  || deviceName.contains("FT")  || deviceName.contains("RONIN") || deviceName.contains("Amazfit Pop") || deviceName.contains("Titan_90188")  || deviceName.contains("LA")  || deviceName.contains("TSW1")  || deviceName.contains("BUFF") || deviceName.contains("FireBoltt") || deviceName.contains("XINJI") || deviceName.contains("AGPTEK") || deviceName.contains("PARSONVER") || deviceName.contains("LW101") || deviceName.contains("LW102") || deviceName.contains("LG") || deviceName.contains("LA99") || deviceName.contains("LW11") || deviceName.contains("COBEE") || deviceName.contains("hs") || deviceName.contains("LQ") || deviceName.contains("Watch") || deviceName.contains("NX") || deviceName.contains("Brooke") || deviceName.contains("CAVIAR") || deviceName.contains("NOHON") || deviceName.contains("Prowatch"))){
                        SPUtils.getInstance().put(SpKey.IS_IC_TYPE_8763E, true);
                    }else{
                        SPUtils.getInstance().put(SpKey.IS_IC_TYPE_8763E, false);
                    }
                    connectDevice();
                }
            }
            // 获取解析结果
        });
    }

    private void connectDevice(){
        FissionSdk.getInstance().connectDevice(deviceAddress, true, "", new BleConnectListener() {
            @Override
            public void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
                LogUtils.d("wl", "FissionSdk_v2----onConnectionStateChange: "+newState.toString());
                ThreadUtils.runOnUiThread(() -> {
                    if (newState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                        connectSuccessfully = true;
                        tvDeviceStatus.setText(deviceName);
                        tvActionConnect.setText(R.string.disconnect);
                        showLog(R.string.connected,deviceName);
                    } else if (newState == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
                        FsLogUtil.d("成功断开了设备");
                        connectSuccessfully = false;
                        tvDeviceStatus.setText(R.string.disconnected);
                        tvActionConnect.setText(R.string.connect);
                        showLog(R.string.disconnected,deviceName);
                    }else if (newState == RxBleConnection.RxBleConnectionState.CONNECTING){
                        tvDeviceStatus.setText(getString(R.string.device_connecting)+ deviceName);
                        tvActionConnect.setText(R.string.disconnect);
                    }
                });
            }

            @Override
            public void isBindNewDevice() {

            }

            @Override
            public void onBinding() {
                LogUtils.d("wl", "---onBinding--");
            }

            @Override
            public void onBindSucceeded(String address, String name) {
                LogUtils.d("wl", "---onBindSucceeded--");
                SharedPreferencesUtil.getInstance().setBluetoothAddress(address);

                BaiDuAiUtils.initDeviceId("oDKQ0Z6JvoCFd3c3O20DxEOOtDCaKCMN", "OtBdvt18HdJnSYGGGaEMn2Mg0mCTF77w");
//                dataSynchronization();
                FissionSdkBleManage.getInstance().setBtConnectListener(new BtConnectListener() {
                    @Override
                    public void onConnectionStateChanged(@NonNull BluetoothDevice device, int state) {

                    }

                    @Override
                    public void onRemoveBondFail() {
                        ToastUtils.showShort("BT配对信息移除失败， 请前往系统蓝牙手动解除配对。");
                        FissionLogUtils.d("wl", "BT配对信息移除失败， 请前往系统蓝牙手动解除配对");
                    }
                });
            }

            @Override
            public void onBindFailed(int code) {
                LogUtils.d("wl", "---onBindFailed--");
                if(code == FissionConstant.BIND_FAIL_KEY_ERROR){ //绑定秘钥出错，重置秘钥
                    long time = System.currentTimeMillis();
                    int lastTime = (int) (time % 10000);
                    int bindKey = AnyWear.bindDevice((int) (lastTime), deviceAddress);
                    SharedPreferencesUtil.getInstance().setFissionKey(lastTime + "," + bindKey);
                }
            }

            @Override
            public void onConnectionFailure(Throwable throwable) {

            }

            @Override
            public void onServiceDisconnected() {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 获取解析结果
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "取消扫描", Toast.LENGTH_LONG).show();
            } else {
                try {
                    String qrResult = result.getContents();
                    String mac = qrResult.split("&")[3].replace("MAC=", "");
                    String name = qrResult.split("&")[4].replace("BtName=","");
                    showLog(R.string.funcQrcode, qrResult);
                    deviceName = name;
                    deviceAddress = mac;
                    tvDeviceStatus.setText(String.format("%d%s", R.string.device_connecting, name));
                    connectSuccessfully = true;
                    tvActionConnect.setText(R.string.disconnect);
                    connectDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void dataSynchronization(){
        // 获取硬件设备信息
        FissionSdkBleManage.getInstance().getHardwareInfo();
        // 设置心率过高提醒 110 = 报警心率值
        FissionSdkBleManage.getInstance().setHeartRateHighTips(1,110);

        // 定时检测心率参数
        HrDetectPara hrDetectPara = new HrDetectPara();
        hrDetectPara.setOpen(true);
        hrDetectPara.setStartTime(0);
        hrDetectPara.setEndTime(1440);
        hrDetectPara.setWeek(0);
        FissionSdkBleManage.getInstance().setHrDetectPara(hrDetectPara);

        // 设置久坐提醒参数
        SedentaryBean sedentaryBean = new SedentaryBean();
        sedentaryBean.setEnable(false);
        sedentaryBean.setStartTime(0);
        sedentaryBean.setEndTime(1440);
        sedentaryBean.setDurTime(30);
        sedentaryBean.setTargetStep(50);
        FissionSdkBleManage.getInstance().setSedentaryPara(sedentaryBean);

        //设置喝水提醒参数
        DkWaterRemind dkWaterRemind = new DkWaterRemind();
        dkWaterRemind.setStartTime(0);
        dkWaterRemind.setEndTime(1440);
        dkWaterRemind.setEnable(false);
        dkWaterRemind.setRemindWeek(0);
        FissionSdkBleManage.getInstance().setDrinkWaterPara(dkWaterRemind);

        // 设置勿扰模式参数
        DndRemind dndRemind = new DndRemind();
        dndRemind.setStartTime(0);
        dndRemind.setEndTime(1440);
        dndRemind.setEnable(false);
        FissionSdkBleManage.getInstance().setDndPara(dndRemind);

        //设置抬腕亮屏参数
        LiftWristPara liftWristPara = new LiftWristPara();
        liftWristPara.setStartTime(0);
        liftWristPara.setEnable(true);
        liftWristPara.setEndTime(1440);
        FissionSdkBleManage.getInstance().setLiftWristPara(liftWristPara);

        //设置用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(101);
        userInfo.setNickname("wanglei");
        userInfo.setHeight(170);
        userInfo.setWeight(65);
        userInfo.setTimeZone(480);
        userInfo.setSex(1);
        userInfo.setAge(18);
        userInfo.setStride(50);
        FissionSdkBleManage.getInstance().setUserInfo(userInfo);

        // 设置温度单位 true=摄氏度 false=华氏度
        FissionSdkBleManage.getInstance().setTemType(true);
        // 设置时间格式 true = 24小时  false = 12小时
        FissionSdkBleManage.getInstance().setTimeFormat(true);
        // 设置单位 1 = 公制，  0 = 英制
        FissionSdkBleManage.getInstance().setUnit(1);

        //设置运动目标
        SportsTargetPara sportsTargetPara = new SportsTargetPara();
        sportsTargetPara.setStep(true);
        sportsTargetPara.setCalorie(true);
        sportsTargetPara.setDistance(true);
        sportsTargetPara.setExercise(true);
        sportsTargetPara.setTargetStep(8000);
        sportsTargetPara.setTargetCalorie(100);
        sportsTargetPara.setTargetDistance(5000);
        sportsTargetPara.setTargetExTime(60);
        FissionSdkBleManage.getInstance().setTargetSet(sportsTargetPara);

        // 获取当天的活动测量数据
        FissionSdkBleManage.getInstance().getMeasureInfo();
        // 获取历史步数记录
        FissionSdkBleManage.getInstance().getStepsRecord(startTime,endTime);
        // 获取运动报告
        FissionSdkBleManage.getInstance().getExerciseReport(startTime, endTime);
        // 获取运动详情
        FissionSdkBleManage.getInstance().getExerciseDetail(startTime, endTime);
        // 获取自动测量心率历史记录
        FissionSdkBleManage.getInstance().getHeartRateRecord(startTime,endTime);
        // 获取历史睡眠记录
        FissionSdkBleManage.getInstance().getSleepRecord(startTime, endTime);
        // 获取当前睡眠数据， 用户绘制当天睡眠的图表
        FissionSdkBleManage.getInstance().getCurSleepRecord();
        // 获取血氧历史记录
        FissionSdkBleManage.getInstance().getSpo2Record(startTime,endTime);
        // 获取手动测量数据， 当固件版本支持压力时， 需要使用getNewHandMeasureInfo。
        FissionSdkBleManage.getInstance().getHandMeasureInfo(startTime,endTime);
    }

    private void initDate() {
        pvTime = new TimePickerBuilder(context, new OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {
                String times = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
                if (timeType == 1) {
                    btnStartTime.setText(times);
                    startTime = (int) (date.getTime() / 1000);
                } else {
                    btnEndTime.setText(times);
                    endTime = (int) (date.getTime() / 1000);
                }
            }
        }).setType(new boolean[]{true, true, true, true, true, true}).build();

        IntentFilter mediafilter = new IntentFilter();
//拦截按键KeyEvent.KEYCODE_MEDIA_NEXT、KeyEvent.KEYCODE_MEDIA_PREVIOUS、KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        mediafilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        mediafilter.setPriority(100);//设置优先级，优先级太低可能被拦截，收不到信息。一般默认优先级为0，通话优先级为1，该优先级的值域是-1000到1000。
        registerReceiver(mediaButtonReceiver, mediafilter);
    }

    private BroadcastReceiver mediaButtonReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isActionMediaButton = Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction());  //判断是不是耳机按键事件
            if(!isActionMediaButton) return;
            KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);  //判断有没有耳机按键事件
            if(event==null) return;
//过滤按下事件
            boolean isActionUp = (event.getAction()==KeyEvent.ACTION_UP);
            if(!isActionUp) return;
//避免在Receiver里做长时间的处理，使得程序在CPU使用率过高的情况下出错，把信息发给handlera处理。
            int keyCode = event.getKeyCode();
            long eventTime = event.getEventTime()-event.getDownTime();//按键按下到松开的时长
//终止广播(不让别的程序收到此广播，免受干扰)
            App.logData.add("---onReceive---"+keyCode+",按键时长："+eventTime);
            abortBroadcast();
        }
    };

    private void addCurLog(int type, String result) {
        logList.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        logList.add(sdf.format(new Date()) + "\n" +getString(type)+"\n"+result + "\n");
        logAdapter.notifyDataSetChanged();
    }

    TimePickerView pvTime;

    public void getTimeSelect(Context context, final int type) {
        pvTime.show();
    }



    int timeType = 1;

    private void showMusicControlDialog() {
        String[] array = {"停止", "暂停", "播放", "上一首", "下一首", "缓冲中", "退出"};
//
    }
    private final int synTime = 1;
    private final int setSystem = 2;
    private final int isMtuSuccessFul = 4;
    //获取硬件信息
    private final int getHardWearInfo = 3;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mAtCmdListener!=null){
            FissionSdkBleManage.getInstance().removeCmdResultListener(mAtCmdListener);
        }
        if(mBigDataCmdListener!=null){
            FissionSdkBleManage.getInstance().removeCmdResultListener(mBigDataCmdListener);
        }
        if(mRawDataListener!=null){
            FissionSdkBleManage.getInstance().removeCmdResultListener(mRawDataListener);
        }
    }

    public void addLog(int type, String result) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        addLog(getString(type) + " " + sdf.format(new Date()) + "\n" + result + "\n");
    }


    @Override
    public void readStreamSuccess(int number, int hr, int level, int step, int distance, int calorie) {
        String content = "\n流帧计数：" + number + "\n心率：" + hr + "\n心率等级：" + level + "\n计步数：" + step + "\n距离：" + distance + "\n卡路里：" + calorie;
        showLog(R.string.stream_data, content);
    }


    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private void validPermission() {
        PermissionUtils.permission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).callback(new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                Toast.makeText(MainActivity.this,"没有权限,请检查权限",Toast.LENGTH_SHORT).show();
            }
        }).request();

        if (Build.VERSION.SDK_INT >= 30 ){
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {

            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                startActivity(intent);
            }
        }
    }

    public static boolean isSameDay(long millis1, long millis2) {
        String day1 = DateUtil.gmtToStrDate((int) millis1, "yyyy-MM-dd");
        String day2 = DateUtil.gmtToStrDate((int) millis2, "yyyy-MM-dd");
        FsLogUtil.d("days一" + day1);
        FsLogUtil.d("days二" + day2);
        if (day1.equals(day2)) {
            return true;
        } else {
            return false;
        }
    }

    private static long millis2Days(long millis, TimeZone timeZone) {
        return (((long) timeZone.getOffset(millis)) + millis) / 86400000;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static final String KEY_TEST_MSG_SET_BEFORE_ON_CREATE = "key_test_msg_set_before_on_create";

    private void testMessageSetBeforeOnCreate() {
        //先发出一个消息
    }


//    //电话监听
//    private void initPhoneListener() {
//        AndPermission.with(this)
//                .runtime()
//                .permission(Permission.READ_CALL_LOG, Permission.CALL_PHONE)
//                .onGranted(permissions -> {
//
//                })
//                .onDenied(permissions -> {
//                    FsLogUtil.d("没有权限,请检查权限");
//                    Toast.makeText(this, "没有通话权限,请检查权限", Toast.LENGTH_SHORT).show();
//                })
//                .start();
//
//
//    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ConnectedStateEvent event) {
        FsLogUtil.d("收到了消息" + event.getState());
        if (event.getState() == C.CONNECTED) {
            connectSuccessfully = true;
            tvDeviceStatus.setText(event.getName());
            tvActionConnect.setText(R.string.disconnect);
            showLog(R.string.connected,"连接成功:连接设备------"+event.getName());
            FissionSdkBleManage.getInstance().getDeviceVersion();
        } else if (event.getState() == C.DISCONNECT) {
            FsLogUtil.d("成功断开了设备");
            connectSuccessfully = false;
            tvDeviceStatus.setText(R.string.disconnected);
            tvActionConnect.setText(R.string.connect);
            SharedPreferencesUtil.getInstance().setFissionKey("");
            showLog(R.string.disconnected,"断开连接成功:断开连接设备------"+event.getName());
        }else if (event.getState() == C.CONNECT_LOADING){
            tvDeviceStatus.setText("正在连接" + event.getName());
            tvActionConnect.setText(R.string.disconnect);
        }
    }

    /**
     * 接收数据的事件总线
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DataMessageEvent event) {
//        LogUtils.d("获取event",event.getMessageType(),"获取event conente"+event.getMessageContent());
        showLog(event.getMessageType(),event.getMessageContent());
    }



    //从手表接收到的带数据的返回
    private final BigDataCallBack bigDataCallBack = new BigDataCallBack() {
        @Override
        public void OnExprGpsDetailCallback(List<ExerGpsDetail> exertGpsDetailList) {
            super.OnExprGpsDetailCallback(exertGpsDetailList);
        }

        //电量信息
        @Override
        public void OnRequestBattery(int batteryValue, int batteryStatus) {
            super.OnRequestBattery(batteryValue, batteryStatus);
        }



        //获取每日活动统计
        @Override
        public void OnDaysReportCallback(List<DaysReport> daysReports) {
            StringBuilder content = new StringBuilder();
            for (DaysReport daysReport : daysReports) {
                content.append("时间:").append(DateUtil.gmtToStrDate(daysReport.getTime()));
                content.append("\n结构体版本:").append(daysReport.getBodyVersion());
                content.append("\n当天累计计步数:").append(daysReport.getStep());
                content.append("\n当天累计消耗卡洛里:").append(daysReport.getCalorie());
                content.append("\n当天累计行程:").append(daysReport.getDistance());
                content.append("\n当天平均心率:").append(daysReport.getAvgHR());
                content.append("\n当天最高心率:").append(daysReport.getHighHR());
                content.append("\n当天最低心率：").append(daysReport.getLowHR());
                content.append("\n当天平均血氧:").append(daysReport.getAvgBloodOxygen());
                content.append("\n当天累计运动时间:").append(daysReport.getSportTime());
                content.append("\n当天累计激烈运动时间:").append(daysReport.getIntenseTime());
                content.append("\n当天深度睡眠时间:").append(daysReport.getDeepSleepTime());
                content.append("\n当天浅睡时间:").append(daysReport.getLightSleepTime());
                content.append("\n当天最高血压:").append(daysReport.getHighBloodPressure());
                content.append("\n当天最低血压:").append(daysReport.getLowBloodPressure());
                content.append("\n\n");
            }
            FsLogUtil.d(content.toString());
        }

        //获取整点活动统计
        @Override
        public void OnHoursReport(List<HoursReport> hoursReports) {
            super.OnHoursReport(hoursReports);
        }

        //获取睡眠记录
        @Override
        public void OnSleepRecord(List<SleepRecord> sleepRecordList) {
            super.OnSleepRecord(sleepRecordList);
        }

        //当前睡眠实时状态记录
        @Override
        public void OnCurSleepRecordCallback(SleepRecord curSleepRecord) {
            super.OnCurSleepRecordCallback(curSleepRecord);
        }

        //获取睡眠统计报告
        @Override
        public void OnSleepReport(List<SleepReport> sleepReports) {
            super.OnSleepReport(sleepReports);
        }

        //运动记录列表
        @Override
        public void OnExerciseListCallback(List<ExerciseList> exerciseLists) {
            super.OnExerciseListCallback(exerciseLists);
        }
        //心率警告提醒参数
        @Override
        public void OnHrWarnPara(HrWarnPara hrWarnPara) {
            super.OnHrWarnPara(hrWarnPara);
        }
    };

    /**
     * 从手表返回的实时消息监听
     */
    private final ReceiveMsgListener messageListener = new ReceiveMsgListener() {
        @Override
        public void fssSuccess(int fssType,int fssStatus) {
           //example:勿扰开关状态
            if (fssType == C.FSS_DND){
             FsLogUtil.d("接收到了来自手环勿扰开关的变更"+fssStatus);
             if (fssStatus == 1) FsLogUtil.d("开启勿扰");
             else  FsLogUtil.d("关闭勿扰");
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

        }

        @Override
        public void onGiveUpFindPhoneCallBack() {

        }

        @Override
        public void onTakePhotoCallback() {

        }

        @Override
        public void onTakePhoneCallback(int callStatus) {
            //手环接听电话
            switch (callStatus) {
                case 0:
                    //拒接电话
                    endCall(0);
                    break;
                case 1:
                    //接听电话
                    endCall(1);
                    break;
                default:
                    //静音
                    silentSwitchOn(false);
                    break;
            }
        }

        @Override
        public void OnError(String msg) {

        }
    };

    int currentVoice = -1;

    private void silentSwitchOn(boolean isOn) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            if (!isOn) {
                currentVoice = audioManager.getStreamVolume(AudioManager.STREAM_RING);
//                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_PLAY_SOUND);
            } else {
                if (currentVoice != -1) {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, currentVoice, AudioManager.FLAG_PLAY_SOUND);
                }
            }


//            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
//            audioManager.getStreamVolume(AudioManager.STREAM_RING);

        }
    }



    private void endCall(int state) {
        FsLogUtil.d("clx"+ "-----挂断电话");
        try {
            //获取到ServiceManager
            Class<?> clazz = Class.forName("android.os.ServiceManager");
            //获取到ServiceManager里面的方法
            Method method = clazz.getDeclaredMethod("getService", String.class);
            //通过反射的方法调用方法
            IBinder iBinder = (IBinder) method.invoke(null, TELEPHONY_SERVICE);
            //注意：ITelephony的包名必须是com.android.internal.telephony，不能随便改的
            ITelephony iTelephony = ITelephony.Stub.asInterface(iBinder);
            if (iTelephony != null) {
                if (state == 0) {
                    iTelephony.endCall();
                } else {
                    iTelephony.answerRingingCall();
                }
            }
            silentSwitchOn(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        LogUtils.d("---onKeyLongPress---"+keyCode);
        App.logData.add("---onKeyLongPress---"+keyCode);
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        App.logData.add("---onKeyDown---"+keyCode);
        return super.onKeyDown(keyCode, event);
    }

    private void test(){
//        BigDataTaskUtil.endTime = System.currentTimeMillis()/1000;
//        byte [] data = StringUtil.hexToByteArray("00 00 00 00 00 00 00 00 20 16 00 00 00 00 00 00 00 00 00 00 04 01 1a 1e f8 3f 31 00 90 77 10 20 00 00 00 00 06 01 1a 20 00 00 00 00 07 01 1a 20 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 30 77 00 00 00 00 00 00 00 00 ff ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 10 77 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 b8 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 98 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ff ff 42 47 01 00 01 81 00 00 00 00 00 00 64 6a d0 40 fb 17 03 01 1d 00 00 00 00 00 08 02 00 00 00 00 00 00 00 00 00 00 20 16 00 00 00 00 00 00 00 00 00 00 04 01 1a 1e f8 3f 31 00 90 77 10 20 00 00 00 00 06 01 1a 20 00 00 00 00 07 01 1a 20 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 30 77 00 00 00 00 00 00 00 00 ff ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 10 77 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 b8 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 98 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ff ff 42 47 01 00 01 81 00 00 00 00 00 00 64 6a d0 40 fb 17 03 01 1d 00 00 00 00 00 08 02 00 00 00 00 00 00 00 00 00 00 20 16 00 00 00 00 00 00 00 00 00 00 04 01 1a 1e f8 3f 31 00 90 77 10 20 00 00 00 00 06 01 1a 20 00 00 00 00 07 01 1a 20 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 30 77 00 00 00 00 00 00 00 00 ff ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 10 77 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 b8 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 98 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 b8 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 98 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ff ff 42 47 01 00 01 81 00 00 00 00 00 00 64 6a d0 40 fb 17 03 01 1d 00 00 00 00 00 08 02 00 00 00 00 00 00 00 00 00 00 20 16 00 00 00 00 00 00 00 00 00 00 04 01 1a 1e f8 3f 31 00 90 77 10 20 00 00 00 00 06 01 1a 20 00 00 00 00 07 01 1a 20 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 30 77 00 00 00 00 00 00 00 00 ff ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 10 77 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 b8 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 98 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ff ff 42 47 01 00 01 81 00 00 00 00 00 00 64 6a d0 40 fb 17 03 01 1d 00 00 00 00 00 08 02 00 00 00 00 00 00 00 00 00 00 20 16 00 00 00 00 00 00 00 00 00 00 04 01 1a 1e f8 3f 31 00 90 77 10 20 00 00 00 00 06 01 1a 20 00 00 00 00 07 01 1a 20 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 30 77 00 00 00 00 00 00 00 00 ff ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 10 77 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 b8 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f8 3f 31 00 98 76 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00".replace(" ", ""));
//        BigDataParseManage.getInstance().parseStepsRecord(data);

//        FissionSdkBleManage.getInstance().setMtu(244);
//        BleConfig.setMTU(244);
//        BleConfig.setIsMTU(true);
//        BaiDuAiUtils.onChat("今天是几月几号", BaiDuAiUtils.AI_VOICE_TYPE_CHAT);

//        BaiDuAiUtils.onChat("导航到深圳北站", "02");

    }


    private void showTipDialog(){
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_simple_input, null);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        EditText etContent = view.findViewById(R.id.etContent);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvTitle.setText("连接新设备");
        btnConfirm.setText("重新绑定");
        btnCancel.setText("取消绑定");
        final AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                FissionSdkBleManage.getInstance().disconnectBleDevice();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                long time = System.currentTimeMillis();
                int lastTime = (int) (time % 10000);
                int bindKey = AnyWear.bindDevice((int) (lastTime), deviceAddress);
                SharedPreferencesUtil.getInstance().setFissionKey(lastTime + "," + bindKey);
                FissionSdkBleManage.getInstance().bindNewDevice(SharedPreferencesUtil.getInstance().getFissionKey());
            }
        });
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tvActionConnect) {
            if (connectSuccessfully) {
                connectSuccessfully = false;
                tvDeviceStatus.setText(R.string.disconnected);
                tvActionConnect.setText(R.string.connect);
//                FissionSdk.getInstance().disConnectDevice();
                SharedPreferencesUtil.getInstance().setFissionKey("");
                FissionSdkBleManage.getInstance().disconnectBleDevice();
            } else {
                activityResultLauncher.launch(new Intent(this, DeviceScanActivity.class));
            }
        } else if (id == R.id.tvLog) {
//            startActivity(new Intent(this, LogActivity.class));
            test();
        } else if (id == R.id.btnStartTime) {
            timeType = 1;
            getTimeSelect(context, 1);
        } else if (id == R.id.btnEndTime) {
            timeType = 2;
            getTimeSelect(context, 2);
        } else if (id == R.id.tvClear) {
            logList.clear();
            logAdapter.notifyDataSetChanged();
        }
    }
}
