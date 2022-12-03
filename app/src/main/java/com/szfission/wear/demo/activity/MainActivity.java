package com.szfission.wear.demo.activity;

import android.Manifest;
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
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;

import com.android.internal.telephony.ITelephony;
import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.MusicConfig;
import com.fission.wear.sdk.v2.bean.StreamData;
import com.fission.wear.sdk.v2.callback.BaseCmdResultListener;
import com.fission.wear.sdk.v2.callback.BleConnectListener;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionFmDataResultListener;
import com.fission.wear.sdk.v2.callback.FissionRawDataResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.constant.SpKey;
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
import com.szfission.wear.demo.dialog.MusicProgressDialog;
import com.szfission.wear.demo.dialog.MusicVolumeDialog;
import com.szfission.wear.demo.dialog.NormalDialog;
import com.szfission.wear.demo.viewmodel.HomeViewModel;
import com.szfission.wear.sdk.AnyWear;
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
import com.szfission.wear.sdk.bean.param.DkWaterRemind;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnBleResultCallback;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;
import com.szfission.wear.sdk.ifs.OnStreamListener;
import com.szfission.wear.sdk.ifs.ReceiveMsgListener;
import com.szfission.wear.sdk.util.DateUtil;
import com.szfission.wear.sdk.util.FsLogUtil;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import static com.szfission.wear.demo.ModelConstant.FUNC_CAMERA_MODEL;
import static com.szfission.wear.demo.ModelConstant.FUNC_CLEAR_SPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_CLEAR_USER_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_COMPRESS_CMD;
import static com.szfission.wear.demo.ModelConstant.FUNC_FIND_DEVICE;
import static com.szfission.wear.demo.ModelConstant.FUNC_FLASH_WRITE_CMD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_APPS_MESS;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_BATTERY;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_BLOODPRESSURE_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_BURIED_DATA;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_CUR_SLEEP_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_DAYS_REPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXERCISE_DETAIL;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXERCISE_GPS;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXERCISE_LIST;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXERCISE_REPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXER_GPS_DETAIL;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_FLASH_DATA;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_GPV;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_HAND_MEASURE_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_HARDWARE_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_HEARTED_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_HOURS_REPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_MEASURE_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_PERSONAL_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_RESTING_HR;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SEDENTARY_DRINK_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SEDENTARY_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SLEEP_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SLEEP_REPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SPO2_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_STEPS_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_TIME;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_TIMEZONE;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_UI_VERSION;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_VERSION;
import static com.szfission.wear.demo.ModelConstant.FUNC_GIVE_UP_FIND_DEVICE;
import static com.szfission.wear.demo.ModelConstant.FUNC_GPS_SPORT_CMD;
import static com.szfission.wear.demo.ModelConstant.FUNC_LOCATION_INFORMATION;
import static com.szfission.wear.demo.ModelConstant.FUNC_MUSIC_CONTROL;
import static com.szfission.wear.demo.ModelConstant.FUNC_MUSIC_PROGRESS;
import static com.szfission.wear.demo.ModelConstant.FUNC_MUSIC_VOLUME;
import static com.szfission.wear.demo.ModelConstant.FUNC_ONLINE_DIAL_PUSH;
import static com.szfission.wear.demo.ModelConstant.FUNC_OTA;
import static com.szfission.wear.demo.ModelConstant.FUNC_PAGE_SKIP;
import static com.szfission.wear.demo.ModelConstant.FUNC_PUSH_CUSTOM_DIAL;
import static com.szfission.wear.demo.ModelConstant.FUNC_PUSH_CUSTOM_SPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_PUSH_QLZ_DATA;
import static com.szfission.wear.demo.ModelConstant.FUNC_QUICK_REPLY_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_REBOOT_DEVICE;
import static com.szfission.wear.demo.ModelConstant.FUNC_RESET;
import static com.szfission.wear.demo.ModelConstant.FUNC_SAFETY_CONFIRM;
import static com.szfission.wear.demo.ModelConstant.FUNC_SELF_INSPECTION_MODE;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_DATA_STREAM;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_DATA_STREAM2;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_DONT_DISTURB_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_DRINK_WATER_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_FEMALE_PHYSIOLOGY;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HIGH_SPEED_CONNECT;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HRLEV_ALGO_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HR_CHECK_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HR_WARN_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_LANG;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_LIFTWRIST_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_MESSAGE_TYPE_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_PROMPT;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TARGET_SET;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIME;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIMEZONE;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIME_MODE;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIMING_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_UNIT;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_WRIST_BRIGHT_SCREEN;
import static com.szfission.wear.demo.ModelConstant.FUNC_SHUTDOWN;
import static com.szfission.wear.demo.ModelConstant.FUNC_SHUTDOWN_STATE;
import static com.szfission.wear.demo.ModelConstant.FUNC_STRU_CALL_DATA;
import static com.szfission.wear.demo.ModelConstant.FUNC_STRU_MUSIC_CONT;
import static com.szfission.wear.demo.ModelConstant.FUNC_SWITCH_HR_RATE;
import static com.szfission.wear.demo.ModelConstant.FUNC_SYN_PHONE_BOOK;
import static com.szfission.wear.demo.ModelConstant.FUNC_VIBRATION;
import static com.szfission.wear.demo.ModelConstant.FUNC_WEATHER;
import static com.szfission.wear.demo.ModelConstant.FUNC_WEATHER_DETAIL;


@ContentView(R.layout.activity_home)
public class MainActivity extends BaseActivity implements OnStreamListener {
    @ViewInject(R.id.tvDeviceStatus)
    TextView tvDeviceStatus;
    @ViewInject(R.id.tvActionConnect)
    TextView tvActionConnect;
    @ViewInject(R.id.recycleMain)
    ListView recycleMain;
    @ViewInject(R.id.tvClear)
    TextView tvClear;
    LogAdapter logAdapter;
    private List<String> logList;
    @ViewInject(R.id.sub4)
    TextView sub4;
    @ViewInject(R.id.sub6)
    TextView sub6;
    @ViewInject(R.id.sub2)
    TextView sub2;
    @ViewInject(R.id.tvQrcode)
    TextView tvQrcode;
    @ViewInject(R.id.expandView)
    ExpandableListView expandView;
    @ViewInject(R.id.btnStartTime)
    Button btnStartTime;
    @ViewInject(R.id.btnEndTime)
    Button btnEndTime;
    @ViewInject(R.id.tvAppVersion)
    TextView tvAppVersion;
    @ViewInject(R.id.tv_menstrual_period)
    TextView tv_menstrual_period;
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
            App.logData.add(result);
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
        public void setTimes(String times) {
            super.setTimes(times);
            LogUtils.d("wl", "上层回调setTimes："+times);
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
            logList.add(heartRateRecords!=null ?heartRateRecords.toString() :"null");
            logAdapter.notifyDataSetChanged();
        }

        @Override
        public void getStepsRecord(List<StepsRecord> stepsRecords) {
            super.getStepsRecord(stepsRecords);
//            logList.clear();
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
//                AudioUtils.disabledMainAudio(MainActivity.this);

                Intent intent = new Intent(MainActivity.this, MenstrualPeriodActivity.class);
                startActivity(intent);

//                List<byte[]> list = new ArrayList<>();
//
//                list.add(StringUtil.hexToByteArray("ff ff 42 47 01 00 81 84 05 c0 62 73 76 df 62 73 87 94 f7 93".replace(" ", "")));
//                list.add(StringUtil.hexToByteArray("00 f2 1b 77 73 62 df 76 73 62 3c 00 3c 00 0c 00 04 00 73 05 00 00 03 00 3d 00 2b 92 00 00 00 00 00 00 04 00 00 00 00 66 00 00 00 00 00 00 02 00 00 00 00 54 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 02 00 00 00 00 46 00 00 00 00 00 00 02 00 00 00 00 55 00 00 00 00 00 00 02 00 00 00 00 59 00 00 00 00 00 00 03 00 00 00 00 5c 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 02 00 00 00 00 4f 00 00 00 00 00 00 02 00 00 00 00 4d 00 00 00 00 00 00 02 00 00 00 00 51 00 00 00 00 00 00 02 00 00 00 00 56 00 00 00 00 00 00 02 00 00 00 00 51 00 00 00 00 00 00 02 00 00 00 00 4e 00 00 00 00 00 00 02 00 00 00 00 49 00 00 00 00 00 00 02 00 00 00 00 48 00 00 00 00 00 00 02 00 00 00 00 47 00 00 00 00 00 00 02 00 00 00 00 4b".replace(" ", "")));
//                list.add(StringUtil.hexToByteArray("01 e4 00 00 00 00 00 00 02 00 00 00 00 49 00 00 00 00 00 00 02 00 00 00 00 48 00 00 00 00 00 00 02 00 00 00 00 56 00 00 00 00 00 00 02 00 00 00 00 4b 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 02 00 00 00 00 4b 00 00 00 00 00 00 03 00 00 00 00 5b 00 00 00 00 00 00 02 00 00 00 00 57 00 00 00 00 00 00 02 00 00 00 00 51 00 00 00 00 00 00 02 00 00 00 00 53 00 00 00 00 00 00 02 00 00 00 00 4d 00 00 00 00 00 00 02 00 00 00 00 48 00 00 00 00 00 00 02 00 00 00 00 47 00 00 00 00 00 00 02 00 00 00 00 4c 00 00 00 00 00 00 02 00 00 00 00 47 00 00 00 00 00 00 02 00 00 00 00 56 00 00 00 00 00 00 02 00 00 00 00 57 00 00 00 00 00 00 02 00 00 00 00 47 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 02 00 00 00 00 49 00 00".replace(" ", "")));
//                list.add(StringUtil.hexToByteArray("02 d6 00 00 00 00 02 00 00 00 00 48 00 00 00 00 00 00 02 00 00 00 00 4d 00 00 00 00 00 00 02 00 00 00 00 4e 00 00 00 00 00 00 02 00 00 00 00 4c 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 03 00 00 00 00 56 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 02 00 00 00 00 4c 00 00 00 00 00 00 02 00 00 00 00 4c 00 00 00 00 00 00 02 00 00 00 00 4e 00 00 00 00 00 00 02 00 00 00 00 49 00 00 00 00 00 00 02 00 00 00 00 4a 00 00 00 00 00 00 02 00 00 00 00 4c 00 00 00 00 00 00 02 00 00 00 00 47 00 00 00 00 00 00 02 00 00 00 00 4c 00 00 b7 05 00 00 02 00 38 00 29 59 00 00 18 05 00 00 04 00 40 00 2e 69 00 00 00 00 00 00 02 00 00 00 00 6b 00 00 00 00".replace(" ", "")));
//                list.add(StringUtil.hexToByteArray("03 c8 00 00 02 00 00 00 00 45 00 00 2b 85 73 62 df 76 73 62 3c 00 0b 00 0c 00 04 00 00 00 00 00 02 00 00 00 00 4b 00 00 00 00 00 00 03 00 00 00 00 5f 00 00 00 00 00 00 05 00 00 00 00 73 00 00 00 00 00 00 03 00 00 00 00 7b 00 00 00 00 00 00 02 00 00 00 00 45 00 00 00 00 00 00 02 00 00 00 00 44 00 00 00 00 00 00 02 00 00 00 00 47 00 00 00 00 00 00 02 00 00 00 00 4f 00 00 00 00 00 00 02 00 00 00 00 47 00 00 00 00 00 00 02 00 00 00 00 5c 00 00 1c 04 00 00 01 00 50 00 39 5b 00 00 ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff".replace(" ", "")));
//                list.add(StringUtil.hexToByteArray("04 ba ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff".replace(" ", "")));
//                list.add(StringUtil.hexToByteArray("05 ac ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff".replace(" ", "")));
//                list.add(StringUtil.hexToByteArray("05 c0 ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff".replace(" ", "")));
//                BigDataParse bigDataParse = new BigDataParse();
//                for(int i=0; i<list.size(); i++){
////                    ArrayList<Integer> dataArray = new ArrayList<>(StringUtil.bytesToArrayList(list.get(i)));
////                    bigDataParse.parseResultData(list.get(i), dataArray);
//                    BigDataParseManage.getInstance().parseResultData(list.get(i), new ParseDataListener() {
//                        @Override
//                        public <T> void parseAtResult(String cmdId, T data) {
//
//                        }
//
//                        @Override
//                        public <T> void parseBigDataResult(String cmdId, T data) {
//
//                        }
//
//                        @Override
//                        public <T> void parseFmResult(T data) {
//
//                        }
//
//                        @Override
//                        public void parseError(Exception e) {
//                            LogUtils.d("wl", e.toString());
//                        }
//
//                        @Override
//                        public void receivingBigData() {
//
//                        }
//                    });
//                }
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
        groupList.add(new FuncGroup("AT指令"));
        groupList.add(new FuncGroup(getString(R.string.stream_data)));
        groupList.add(new FuncGroup("大数据"));
        groupList.add(new FuncGroup("设置久坐喝水提醒"));
        funcBeanList = homeViewModel.getFuncBeans();
        MainAdapter mainAdapter = new MainAdapter(groupList, funcBeanList);
        expandView.setAdapter(mainAdapter);
        handler = new Handler();
//        lvContent.setOnItemClickListener(this);
        AnyWear.setOnStreamListener(this);
        Date date = new Date();
        String times = DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
        btnEndTime.setText(times);

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
                    case FUNC_SET_TIMEZONE:
                        showEditDialog(FUNC_SET_TIMEZONE);
                        break;
                    case FUNC_SET_TIME_MODE:
                        showCheckModelDialog(FUNC_SET_TIME_MODE);
                        break;
                    case FUNC_SET_UNIT:
                        showCheckModelDialog(FUNC_SET_UNIT);
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
                    case FUNC_GET_MEASURE_INFO:
//                        FissionSdk.getInstance().getMeasureInfo();
                        FissionSdkBleManage.getInstance().getMeasureInfo();
                        break;
                    case FUNC_GET_DAYS_REPORT:
                        //获取每日活动统计
//                        FissionSdk.getInstance().getDaysReport(startTime,endTime);
                        FissionSdkBleManage.getInstance().getDaysReport(startTime,endTime);
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
                    case FUNC_GET_BLOODPRESSURE_RECORD:
                        //获取血压记录
//                        FissionSdk.getInstance().getBloodPressureRecord(startTime,endTime);
                        FissionSdkBleManage.getInstance().getMentalStressRecord(startTime,endTime);
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
                        AnyWear.setFemalePhysiology(value, new OnSmallDataCallback() {
                            @Override
                            public void OnStringResult(String content) {
                                showLog(R.string.FUNC_SET_FEMALE_PHYSIOLOGY,content);
                            }
                        });
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

                }
            }
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
                    if(!TextUtils.isEmpty(deviceName) && (deviceName.contains("LW71") || deviceName.contains("LW76") || deviceName.contains("LW82") || deviceName.contains("LW83") || deviceName.contains("LW77"))){
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
                if (newState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                    connectSuccessfully = true;
                    tvDeviceStatus.setText(deviceName);
                    tvActionConnect.setText(R.string.disconnect);
                    showLog(R.string.connected,"连接成功:连接设备------"+deviceName);
                } else if (newState == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
                    FsLogUtil.d("成功断开了设备");
                    connectSuccessfully = false;
                    tvDeviceStatus.setText(R.string.disconnected);
                    tvActionConnect.setText(R.string.connect);
                    showLog(R.string.disconnected,"断开连接成功:断开连接设备------"+deviceName);
                }else if (newState == RxBleConnection.RxBleConnectionState.CONNECTING){
                    tvDeviceStatus.setText("正在连接" + deviceName);
                    tvActionConnect.setText(R.string.disconnect);
                }
            }

            @Override
            public void onBinding() {
                LogUtils.d("wl", "---onBinding--");
            }

            @Override
            public void onBindSucceeded(String address, String name) {
                LogUtils.d("wl", "---onBindSucceeded--");
                SharedPreferencesUtil.getInstance().setBluetoothAddress(address);
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
                String qrResult = result.getContents();
//                String downUrl = qrResult.substring(0,qrResult.indexOf("MAC"));
                String MAC = (qrResult.substring(0,qrResult.indexOf("CODE"))).replace("CODE","").substring(qrResult.indexOf("MAC:")).replace("MAC:","").trim();
//
//                String mac1 = MAC.substring(0, 2).toUpperCase() + ":";
//                String mac2 = MAC.substring(2, 4).toUpperCase() + ":";
//                String mac3 = MAC.substring(4, 6).toUpperCase() + ":";
//                String mac4 = MAC.substring(6, 8).toUpperCase() + ":";
//                String mac5 = MAC.substring(8, 10).toUpperCase() + ":";
//                String mac6 = MAC.substring(10, 12).toUpperCase();
//                MAC = mac1+mac2+mac3+mac4+mac5+mac6;
//                String SN =(qrResult.substring(qrResult.indexOf("SN:")));
//                SN = SN.substring(0,SN.indexOf("NAME")).replace("SN:","");
                String name = (qrResult.substring(qrResult.indexOf("NAME:")).replace("NAME:",""));
//                final String content = "下载链接地址:" + downUrl.trim() + "\nMAC地址:" + MAC.trim() + "\nSN:" + SN.trim() + "\n设备名称:" + name.trim();
                final String content =  "\nMAC地址:" + MAC.trim()  + "\n设备名称:" + name.trim();
                String mac = StringUtil.addSymbol(MAC);
//                FsLogUtil.d(content);
//                Toast.makeText(this, "扫描内容:\n" + result.getContents(), Toast.LENGTH_LONG).show();
                LogUtils.d("二维码信息"+ qrResult,"名字"+content);
                showLog(R.string.funcQrcode, qrResult);
                SharedPreferencesUtil.getInstance().setBluetoothAddress(mac);
                SharedPreferencesUtil.getInstance().setBluetoothName(name);
                tvDeviceStatus.setText(String.format("%d%s", R.string.device_connecting, name));
                connectSuccessfully = true;
                tvActionConnect.setText(R.string.disconnect);
                FissionSdk.getInstance().connectDevice(mac,false,"");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

    @Event({R.id.tvActionConnect, R.id.tvLog, R.id.btnStartTime, R.id.btnEndTime, R.id.tvClear})
    private void click(View v) {
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
            startActivity(new Intent(this, LogActivity.class));
//            Intent intent = new Intent(BleComService.START_CHECK_BLE_CONNECT_ACTION);
//            intent.setPackage(getPackageName());
//            startService(intent);

        } else if (id == R.id.btnStartTime) {
            timeType = 1;
            getTimeSelect(context, 1);
        } else if (id == R.id.btnEndTime) {
            timeType = 2;
            getTimeSelect(context, 2);
        } else if (id == R.id.tvClear) {
            logList.clear();
            logAdapter.notifyDataSetChanged();
//            Intent intent = new Intent(BleComService.STOP_CHECK_BLE_CONNECT_ACTION);
//            intent.setPackage(getPackageName());
//            startService(intent);
        }
    }

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
        AnyWear.disConnect();
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

}
