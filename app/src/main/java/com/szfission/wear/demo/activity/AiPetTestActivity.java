package com.szfission.wear.demo.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.aipet.AiPetManage;
import com.fission.wear.sdk.v2.aipet.bean.AnimationConfig;
import com.fission.wear.sdk.v2.aipet.bean.FileProgress;
import com.fission.wear.sdk.v2.aipet.bean.HolidayAnimConfig;
import com.fission.wear.sdk.v2.aipet.bean.PoiItem;
import com.fission.wear.sdk.v2.aipet.bean.PoiReward;
import com.fission.wear.sdk.v2.aipet.bean.UploadFileConfig;
import com.fission.wear.sdk.v2.aipet.bean.WeatherDetails;
import com.fission.wear.sdk.v2.aipet.event.AgpsFileDownloadEvent;
import com.fission.wear.sdk.v2.aipet.event.AnimationAddEvent;
import com.fission.wear.sdk.v2.aipet.event.DeviceInfoEvent;
import com.fission.wear.sdk.v2.aipet.event.DeviceResErrorEvent;
import com.fission.wear.sdk.v2.aipet.event.FileTransferEvent;
import com.fission.wear.sdk.v2.aipet.event.GetCarModeEvent;
import com.fission.wear.sdk.v2.aipet.event.HolidayAnimationAddEvent;
import com.fission.wear.sdk.v2.aipet.event.PetInteractionEvent;
import com.fission.wear.sdk.v2.aipet.event.PetStatusEvent;
import com.fission.wear.sdk.v2.aipet.event.PoiCheckEvent;
import com.fission.wear.sdk.v2.aipet.event.PoiItemEvent;
import com.fission.wear.sdk.v2.aipet.event.ScreenBrightnessEvent;
import com.fission.wear.sdk.v2.aipet.event.SetAiChatMoodEvent;
import com.fission.wear.sdk.v2.aipet.event.SetAreaAnimEvent;
import com.fission.wear.sdk.v2.aipet.event.SetCarModeEvent;
import com.fission.wear.sdk.v2.aipet.event.SetDndModeEvent;
import com.fission.wear.sdk.v2.aipet.event.SetInteractionCodeEvent;
import com.fission.wear.sdk.v2.aipet.event.SetKwsSwitchEvent;
import com.fission.wear.sdk.v2.aipet.event.SetOffVoiceKeyEvent;
import com.fission.wear.sdk.v2.aipet.event.StartCheckInEvent;
import com.fission.wear.sdk.v2.aipet.event.UnBindUserEvent;
import com.fission.wear.sdk.v2.aipet.event.VolumeEvent;
import com.fission.wear.sdk.v2.bean.UploadResult;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.http.AgpsRepository;
import com.fission.wear.sdk.v2.utils.CRC32Checksum;
import com.fission.wear.sdk.v2.utils.FileByteReader;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.HiSiDirectoryUploadManager;
import com.fission.wear.sdk.v2.utils.HiSiliconFileTransferUtils;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.SharedPreferencesUtil;
import com.szfission.wear.sdk.util.RxTimerUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AiPetTestActivity extends BaseActivity {

    private Button btn_get_pet_state, btn_evolution, btn_set_weather, btn_binding, btn_unbind, btn_add_holiday_animation, btn_download_file, btn_ota_firmware, btn_set_car_mode_open;
    private Button btn_set_car_mode_close, btn_upload_agps_file, btn_get_car_mode, btn_set_off_voice_key, btn_spp_ota, btn_set_dnd_mode, btn_set_screen_brightness, btn_get_screen_brightness;

    private Button btn_set_anim_code, btn_set_background_code, btn_set_detail_code, btn_set_holiday_anim, btn_upload_all_file, btn_query_device_info, btn_kws_switch_set, btn_set_interaction_weather;

    private Button btn_set_device_volume, btn_get_device_volume, btn_set_language, btn_set_area_anim_code, btn_set_ai_mood_anim_code, btn_start_playing_ai_voice, btn_stop_play_ai_voice;

    private TextView tv_log;
    String filePath = "";
    private long crc32;
    private long fileSize;
    private int otaType;

    private List<File> mFileList;

    boolean isUploadAgpsFile = false;

    long startTime = 0;

    int tryNum =0;

    private int index = 0;

    private int animationCode;

    private int backgroundCode;

    private int detailCode;

    private RxTimerUtil mRxTimerUtil;

    private int mIndex, mTotal;
    boolean isOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_pet_test);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        String directory = getExternalFilesDir(null)+"/fission_haisi_gps";
        FileUtils.createOrExistsDir(directory);
        mFileList = FileUtils.listFilesInDir(directory);

        String path = getPath();

        btn_get_pet_state = findViewById(R.id.btn_get_pet_state);
        btn_evolution = findViewById(R.id.btn_evolution);
        btn_set_weather = findViewById(R.id.btn_set_weather);
        btn_binding = findViewById(R.id.btn_binding);
        btn_unbind = findViewById(R.id.btn_unbind);
        btn_add_holiday_animation = findViewById(R.id.btn_add_holiday_animation);
        tv_log = findViewById(R.id.tv_log);
        btn_download_file= findViewById(R.id.btn_download_file);
        btn_ota_firmware= findViewById(R.id.btn_ota_firmware);
        btn_set_car_mode_open= findViewById(R.id.btn_set_car_mode_open);
        btn_set_car_mode_close= findViewById(R.id.btn_set_car_mode_close);
        btn_upload_agps_file = findViewById(R.id.btn_upload_agps_file);
        btn_get_car_mode = findViewById(R.id.btn_get_car_mode);
        btn_set_off_voice_key = findViewById(R.id.btn_set_off_voice_key);
        btn_spp_ota = findViewById(R.id.btn_spp_ota);
        btn_set_anim_code = findViewById(R.id.btn_set_anim_code);
        btn_set_background_code = findViewById(R.id.btn_set_background_code);
        btn_set_detail_code = findViewById(R.id.btn_set_detail_code);
        btn_set_holiday_anim = findViewById(R.id.btn_set_holiday_anim);
        btn_set_device_volume = findViewById(R.id.btn_set_device_volume);
        btn_get_device_volume = findViewById(R.id.btn_get_device_volume);
        btn_set_language = findViewById(R.id.btn_set_language);
        btn_set_area_anim_code = findViewById(R.id.btn_set_area_anim_code);
        btn_set_ai_mood_anim_code = findViewById(R.id.btn_set_ai_mood_anim_code);
        btn_start_playing_ai_voice = findViewById(R.id.btn_start_playing_ai_voice);
        btn_stop_play_ai_voice = findViewById(R.id.btn_stop_play_ai_voice);
        btn_upload_all_file = findViewById(R.id.btn_upload_all_file);
        btn_query_device_info = findViewById(R.id.btn_query_device_info);
        btn_kws_switch_set = findViewById(R.id.btn_kws_switch_set);
        btn_set_dnd_mode = findViewById(R.id.btn_set_dnd_mode);
        btn_set_screen_brightness = findViewById(R.id.btn_set_screen_brightness);
        btn_get_screen_brightness = findViewById(R.id.btn_get_screen_brightness);
        btn_set_interaction_weather = findViewById(R.id.btn_set_interaction_weather);

        btn_get_pet_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FissionSdkBleManage.getInstance().getPetStatus();
            }
        });

        btn_evolution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().notifyPetEvolution();
            }
        });

        btn_set_weather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int min = 15101;
                int max = 15106;
                Random rand = new Random();
                int randomNum = rand.nextInt(max - min + 1) + min;
                int backgroundCode = 0;
                if(randomNum == 15102){
                    backgroundCode = 15108;
                }
                WeatherDetails weatherDetails =  new WeatherDetails(randomNum, backgroundCode);
                FissionSdkBleManage.getInstance().setWeatherDetails(weatherDetails);
            }
        });

        btn_binding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().bindingAiPetDevice("123456");
            }
        });

        btn_unbind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().unbindAiPetDevice(SharedPreferencesUtil.getInstance().getFissionKey());
            }
        });

        btn_add_holiday_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otaType = 1;
                if (Build.VERSION.SDK_INT >= 30 ){
                    // 先判断有没有权限
                    if (Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        Uri uri = Uri.parse(path);
                        intent.setDataAndType(uri, "*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 1);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                        startActivity(intent);
                    }
                }else{
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    Uri uri = Uri.parse(path);
                    intent.setDataAndType(uri, "*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 1);
                }
            }
        });

        btn_ota_firmware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otaType = 2;
                if (Build.VERSION.SDK_INT >= 30 ){
                    // 先判断有没有权限
                    if (Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        Uri uri = Uri.parse(path);
                        intent.setDataAndType(uri, "*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 2);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                        startActivity(intent);
                    }
                }else{
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    Uri uri = Uri.parse(path);
                    intent.setDataAndType(uri, "*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 2);
                }
            }
        });

        btn_download_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                long offset = 0;
//                File file = new File(getExternalFilesDir(null)+"audio_cap.mp3");
//                FissionLogUtils.d("wl", "文件下载存储路径："+file.getAbsolutePath());
//                if(file.exists()){
//                    offset = file.length();
//                    if(!SPUtils.getInstance().getBoolean("isDownload")){
//                        DownloadFileConfig downloadFileConfig = new DownloadFileConfig("/user/", "audio_cap.mp3", offset);
//                        FissionSdkBleManage.getInstance().downloadFileInit(downloadFileConfig);
//                        FissionLogUtils.d("wl", "文件已存在断点下载， 文件信息："+downloadFileConfig);
//                    }else{
//                        FissionLogUtils.d("wl", "该文件已经下载成功！");
//                    }
//                }else{
//                    SPUtils.getInstance().put("isDownload", false);
//                    DownloadFileConfig downloadFileConfig = new DownloadFileConfig("/user/", "audio_cap.mp3", offset);
//                    FissionSdkBleManage.getInstance().downloadFileInit(downloadFileConfig);
//                }

                FissionSdkBleManage.getInstance().notifyOtaFirmware();
            }
        });

        btn_set_car_mode_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().setCarMode(true);
            }
        });

        btn_set_car_mode_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().setCarMode(false);
            }
        });

        btn_upload_agps_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                otaType = 3;
                startTime  = System.currentTimeMillis();
//                otaType = 3;
//                FissionLogUtils.d("wl", "星历文件数量："+mFileList.size());
//                if(mFileList!= null && !mFileList.isEmpty() && !isUploadAgpsFile){
//                    isUploadAgpsFile = true;
//                    uploadAgpsFiles(mFileList.get(index));
//                }else{
//                    ToastUtils.showShort("星历文件不存在，请检查");
//                }
                new AgpsRepository(AiPetTestActivity.this).fetchAgps();
            }
        });

        btn_get_car_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().getCarMode();
            }
        });

        btn_set_off_voice_key.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AiPetManage.getInstance().downloadAuthCode();
            }
        });

        btn_spp_ota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                otaType = 4;
                if (Build.VERSION.SDK_INT >= 30 ){
                    // 先判断有没有权限
                    if (Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        Uri uri = Uri.parse(path);
                        intent.setDataAndType(uri, "*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 2);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                        startActivity(intent);
                    }
                }else{
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    Uri uri = Uri.parse(path);
                    intent.setDataAndType(uri, "*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 2);
                }
            }
        });

        btn_set_anim_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animationCode = 95201;
                otaType = 5;
                if (Build.VERSION.SDK_INT >= 30 ){
                    // 先判断有没有权限
                    if (Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        Uri uri = Uri.parse(path);
                        intent.setDataAndType(uri, "*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 2);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                        startActivity(intent);
                    }
                }else{
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    Uri uri = Uri.parse(path);
                    intent.setDataAndType(uri, "*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 2);
                }
            }
        });

        btn_set_background_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backgroundCode = 95206;
                otaType = 6;
                if (Build.VERSION.SDK_INT >= 30 ){
                    // 先判断有没有权限
                    if (Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        Uri uri = Uri.parse(path);
                        intent.setDataAndType(uri, "*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 2);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                        startActivity(intent);
                    }
                }else{
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    Uri uri = Uri.parse(path);
                    intent.setDataAndType(uri, "*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 2);
                }
            }
        });

        btn_set_detail_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detailCode = 95306;
                otaType = 7;
                if (Build.VERSION.SDK_INT >= 30 ){
                    // 先判断有没有权限
                    if (Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        Uri uri = Uri.parse(path);
                        intent.setDataAndType(uri, "*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 2);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                        startActivity(intent);
                    }
                }else{
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    Uri uri = Uri.parse(path);
                    intent.setDataAndType(uri, "*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 2);
                }
            }
        });

        btn_set_holiday_anim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HolidayAnimConfig holidayAnimConfig = new HolidayAnimConfig(2, 16, animationCode, backgroundCode, detailCode);
                FissionSdkBleManage.getInstance().holidayAnimAdd(holidayAnimConfig);
                FissionLogUtils.d("wl", "设置春节动画");
            }
        });

        btn_set_device_volume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().setDeviceVolume(10);
            }
        });

        btn_get_device_volume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().getDeviceVolume();
            }
        });

        btn_set_language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().setDeviceLanguage(1);  // 0 ==英文； 1 == 中文
            }
        });

        btn_set_area_anim_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FissionSdkBleManage.getInstance().setAreaAnimCode(81301);
            }
        });

        btn_set_ai_mood_anim_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FissionSdkBleManage.getInstance().setAiChatMoodAnimCode(42101);
            }
        });

        btn_start_playing_ai_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().notifyStartPlayingAiVoice();
            }
        });

        btn_stop_play_ai_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().notifyStopPlayAiVoice();
            }
        });

        btn_upload_all_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().diffUploadFileInit(1024*1024*1024);
            }
        });

        btn_query_device_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().queryDeviceInfo();
            }
        });


        btn_kws_switch_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int min = 71101;
                int max = 71104;
                Random rand = new Random();
                int randomNum = rand.nextInt(max - min + 1) + min;
                isOpen = !isOpen;
                FissionSdkBleManage.getInstance().setKwsSwitch(randomNum, isOpen) ;
            }
        });

        btn_set_dnd_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isOpen = !isOpen;
                FissionSdkBleManage.getInstance().setDndMode(isOpen);
            }
        });

        btn_set_screen_brightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random rand = new Random();
                int randomNum = rand.nextInt(90)+10; //10-100随机值， 亮度范围0-100
                FissionSdkBleManage.getInstance().setDeviceScreenBrightness(randomNum);
            }
        });

        btn_get_screen_brightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().getDeviceScreenBrightness();
            }
        });

        btn_set_interaction_weather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int min = 22201;
                int max = 22206;
                Random rand = new Random();
                int randomNum = rand.nextInt(max - min + 1) + min;
                FissionSdkBleManage.getInstance().setInteractionWeather(randomNum);
            }
        });
    }

    @Override
    protected boolean useEventBus() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPetStatusEvent(PetStatusEvent event) {
        FissionLogUtils.d("wl", "app接收到宠物状态数据："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPoiCheckEvent(PoiCheckEvent event) {
        FissionLogUtils.d("wl", "app接收到Poi打卡数据："+event);
        ToastUtils.showLong("宠物POI打卡成功！！！");
        int rewardNum = (int)(Math.random() * 255) + 1;
        int currentProgress = (int)(Math.random() * 120) + 1;
        PoiReward poiReward = new PoiReward(1001, 0, currentProgress, 120);
        FissionSdkBleManage.getInstance().responsePoiCheckReward(poiReward);
        if(mRxTimerUtil!=null){
            mRxTimerUtil.cancelTimer();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPetInteractionEvent(PetInteractionEvent event) {
        FissionLogUtils.d("wl", "接收到撸宠事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnBindUserEvent(UnBindUserEvent event) {
        FissionLogUtils.d("wl", "用户解除绑定事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetCarModeEvent(SetCarModeEvent event) {
        FissionLogUtils.d("wl", "设置车载模式事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetCarModeEvent(GetCarModeEvent event) {
        FissionLogUtils.d("wl", "获取车载模式事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartCheckInEvent(StartCheckInEvent event) {
        FissionLogUtils.d("wl", "设备开始打卡事件："+event);
        int time = event.getTimeoutDuration()*1000;
        // 获取设备侧的打卡超时时间， app需要获取定位，在超时时间内没有收到onPoiCheckEvent事件时， 使用app获取的定位去执行打卡抽奖逻辑
        if(mRxTimerUtil == null){
            mRxTimerUtil = new RxTimerUtil();
            mRxTimerUtil.timer(time, new RxTimerUtil.RxAction() {
                @Override
                public void action(long number) {
//                    int min = 1001;
//                    int max = 1004;
//                    Random rand = new Random();
//                    int randomNum = rand.nextInt(max - min + 1) + min;
//                    int currentProgress = (int)(Math.random() * 120) + 1;
//                    PoiReward poiReward = new PoiReward(randomNum, 0, currentProgress, 120);
//                    FissionSdkBleManage.getInstance().responsePoiCheckReward(poiReward);

                    List<PoiItem> list = new ArrayList<>();
                    PoiItem poiItem = new PoiItem(81401, "咖啡店");
                    PoiItem poiItem2 = new PoiItem(81402, "轻食简餐店");
                    PoiItem poiItem3 = new PoiItem(81403, "甜品店");
                    PoiItem poiItem4 = new PoiItem(81404, "汉堡快餐店");
                    PoiItem poiItem5 = new PoiItem(81405, "美式早餐");
                    PoiItem poiItem6 = new PoiItem(81406, "披萨店");
                    PoiItem poiItem7 = new PoiItem(81407, "中餐厅");
                    PoiItem poiItem8 = new PoiItem(81408, "亚洲面馆");
                    PoiItem poiItem9 = new PoiItem(81409, "西餐厅");
                    PoiItem poiItem10 = new PoiItem(81410, "日料店");
                    list.add(poiItem);
                    list.add(poiItem2);
                    list.add(poiItem3);
                    list.add(poiItem4);
                    list.add(poiItem5);
                    list.add(poiItem6);
                    list.add(poiItem7);
                    list.add(poiItem8);
                    list.add(poiItem9);
                    list.add(poiItem10);
                    FissionSdkBleManage.getInstance().setPoiList(list);
                    mRxTimerUtil = null;
                }
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPoiItemEvent(PoiItemEvent event) {
        FissionLogUtils.d("wl", "设备选中打卡地点事件："+event);
        //打桩随机数据， app应该根据选中的类型，去处理对应的打卡奖励数据
        int min = 1001;
        int max = 1004;
        Random rand = new Random();
        int randomNum = rand.nextInt(max - min + 1) + min;
        int currentProgress = (int)(Math.random() * 120) + 1;
        int rewardCode = 31101;
        switch (event.poiItem.getType()){
            case 81401:
                rewardCode = 31101;
                break;

            case 81402:
                rewardCode = 31102;
                break;

            case 81403:
                rewardCode = 31103;
                break;

            case 81404:
                rewardCode = 31104;
                break;

            case 81405:
                rewardCode = 31105;
                break;

            case 81406:
                rewardCode = 31106;
                break;

            case 81407:
                rewardCode = 31107;
                break;

            case 81408:
                rewardCode = 31108;
                break;

            case 81409:
                rewardCode = 31109;
                break;

            case 81410:
                rewardCode = 31110;
                break;
        }
        PoiReward poiReward = new PoiReward(randomNum, rewardCode, currentProgress, 120);
        FissionSdkBleManage.getInstance().responsePoiCheckReward(poiReward);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetOffVoiceKeyEvent(SetOffVoiceKeyEvent event) {
        FissionLogUtils.d("wl", "设置离线语音授权码："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVolumeEvent(VolumeEvent event) {
        FissionLogUtils.d("wl", "获取设备音量事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScreenBrightnessEvent(ScreenBrightnessEvent event) {
        FissionLogUtils.d("wl", "获取设备屏幕亮度事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetAreaAnimEvent(SetAreaAnimEvent event) {
        FissionLogUtils.d("wl", "设置定位区域动画编码事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetAiChatMoodEvent(SetAiChatMoodEvent event) {
        FissionLogUtils.d("wl", "设置AI聊天情绪动画编码事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnimationAddEvent(AnimationAddEvent event) {
        FissionLogUtils.d("wl", "添加动画事件："+event);
        if(event.getErrorCode() == 0){
            if(otaType == 5){
                FissionLogUtils.d("wl", "前景动画添加成功");
            }else if(otaType == 6){
                FissionLogUtils.d("wl", "背景动画添加成功");
            }else if(otaType == 7){
                FissionLogUtils.d("wl", "背景详情动画添加成功");
            }
        }else{
            if(otaType == 5){
                FissionLogUtils.d("wl", "前景动画添加失败");
            }else if(otaType == 6){
                FissionLogUtils.d("wl", "背景动画添加失败");
            }else if(otaType == 7){
                FissionLogUtils.d("wl", "背景详情动画添加失败");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHolidayAnimationAddEvent(HolidayAnimationAddEvent event) {
        FissionLogUtils.d("wl", "添加节日动画事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFileTransferEvent(FileTransferEvent event) {
        FissionLogUtils.d("wl", "文件传输事件："+event+", otaType:"+otaType);
        if(event.operate == 0){
            if(event.errorCode == 0){
                new RxTimerUtil().timer(1000, new RxTimerUtil.RxAction() {
                    @Override
                    public void action(long number) {
//                        if(otaType == 3){
//                            FissionSdkBleManage.getInstance().uploadFileStart(mFileList.get(index).getAbsolutePath(), event.offset);
//                        }else{
//                            FissionSdkBleManage.getInstance().uploadFileStart(filePath, event.offset);
//                        }
                        FissionSdkBleManage.getInstance().uploadFileStart(filePath, event.offset);
                    }
                });
            }else if(event.errorCode == 2 && otaType == 1){
                AnimationConfig animationConfig = new AnimationConfig();
                animationConfig.setNumber(20306);
                animationConfig.setName("test_V0.1.bin");
                animationConfig.setCount(98);
                animationConfig.setPointY(0);
                animationConfig.setPointX(0);
                animationConfig.setSizeWidth(466);
                animationConfig.setSizeHeight(466);
                animationConfig.setScale(false);
                animationConfig.setMultiForm(false);
                animationConfig.setPlaySound(false);
                FissionSdkBleManage.getInstance().addAnimation(animationConfig);
            }else if(event.errorCode == 2 && otaType == 2){
                FissionSdkBleManage.getInstance().notifyOtaFirmware();
            }else if(event.errorCode == 2 && otaType == 3){ //星历文件已存在
                index++;
                if(index < mFileList.size()){
                    uploadAgpsFiles(mFileList.get(index));
                }else{
                    index = 0;
                    isUploadAgpsFile = false;
                    FissionLogUtils.d("wl", "星历文件上传完毕, 耗时："+(System.currentTimeMillis()-startTime)/1000+"s");
                }
            }else if(event.errorCode == 2 && otaType == 5){
                animAdd(animationCode, 149);
            }else if(event.errorCode == 2 &&otaType == 6){
                animAdd(backgroundCode, 1);
            }else if(event.errorCode == 2 && otaType == 7){
                animAdd(detailCode, 149);
            }
        }else if(event.operate == 1){
            if(event.errorCode == 0){
                FissionLogUtils.d("wl", "file upload ok!!  otaType："+otaType);
                if(otaType == 1){
                    AnimationConfig animationConfig = new AnimationConfig();
                    animationConfig.setNumber(20306);
                    animationConfig.setName("test_V0.1.bin");
                    animationConfig.setCount(98);
                    animationConfig.setPointY(0);
                    animationConfig.setPointX(0);
                    animationConfig.setSizeWidth(466);
                    animationConfig.setSizeHeight(466);
                    animationConfig.setScale(false);
                    animationConfig.setMultiForm(false);
                    animationConfig.setPlaySound(false);
                    FissionSdkBleManage.getInstance().addAnimation(animationConfig);
                }else if(otaType == 2){
                    FissionSdkBleManage.getInstance().notifyOtaFirmware();
                }else if(otaType == 3){
//                    index++;
//                    if(index < mFileList.size()){
//                        uploadAgpsFiles(mFileList.get(index));
//                    }else{
//                        index = 0;
//                        isUploadAgpsFile = false;
//                        FissionLogUtils.d("wl", "星历文件上传完毕, 耗时："+(System.currentTimeMillis()-startTime)/1000+"s");
//                    }
                    FissionLogUtils.d("wl", "星历文件上传完毕, 耗时："+(System.currentTimeMillis()-startTime)/1000+"s");
                }else if(otaType == 5){
                    animAdd(animationCode, 149);
                }else if(otaType == 6){
                    animAdd(backgroundCode, 1);
                }else if(otaType == 7){
                    animAdd(detailCode, 149);
                }
            }else{
                FissionLogUtils.d("wl", "file upload fail!!  otaType："+otaType);
                if(otaType == 3){
                    if(tryNum == 3){
                        isUploadAgpsFile = false;
                        return;
                    }
                    uploadAgpsFiles(mFileList.get(index));
                    tryNum++;
                }else{
//                    if(tryNum == 10){
//                        FissionLogUtils.d("wl", "tryNum maxed");
//                        return;
//                    }
//                    tryNum++;
//                    uploadResFile();
                }
            }
        }else if(event.operate == 2){
            FissionLogUtils.d("wl", "file upload progress:"+event.progress);
        }else if(event.operate == 3){
            crc32 = event.crc32;
            fileSize = event.fileSize;
        }else if(event.operate == 4){
            String filePath = getExternalFilesDir(null)+"watch001.bin";
            FileByteReader.writeDataByOffset(filePath, event.offset, event.data);
            if(event.isEnd){
                if(fileSize == new File(filePath).length() && crc32 == CRC32Checksum.crc32(0, FileIOUtils.readFile2BytesByStream(filePath))){
                    FissionLogUtils.d("wl", "数据长度和crc校验一致，文件下载成功");
                    SPUtils.getInstance().put("isDownload", true);
                }else{
                    FissionLogUtils.d("wl", "文件完整校验失败，下载失败");
                }
            }
        }else if(event.operate == 5){
            //diff 升级资源
            if(event.errorCode == 0){
                scanDiffUploadFile();
            }else{
                FissionLogUtils.d("wl", "存储空间不足，不能升级资源");
            }
        }else if(event.operate == 6){
            // hisi spp ota上传文件协商结果
            FissionLogUtils.d("wl", "hisi spp ota协商："+event.errorCode);
            if(event.errorCode == 0){
                HiSiliconFileTransferUtils.getInstance().setHiSiliconFileTransferListener(new HiSiliconFileTransferUtils.HiSiliconFileTransferListener() {
                    @Override
                    public void onProgressChanged(long curFrames, long framesCount, int fileListIndex, int fileSize) {
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int progress = (int)(curFrames*100/framesCount);
                                tv_log.setText("OTA文件传输进度："+progress+"%");
                                FileProgress fileProgress = new FileProgress(0, 1, progress, curFrames, framesCount);
                                FissionSdkBleManage.getInstance().diffUploadFileProgress(fileProgress);
                            }
                        });
                    }

                    @Override
                    public void onComplete() {
                        FileProgress fileProgress = new FileProgress(0, 1, 100, 0, 0);
                        FissionSdkBleManage.getInstance().diffUploadFileProgress(fileProgress);
                        FissionSdkBleManage.getInstance().notifyOtaFirmware();
                    }

                    @Override
                    public void onTimeOut() {

                    }

                    @Override
                    public void onError(Exception e) {

                    }

                    @Override
                    public void onTransmitting() {

                    }
                });
                HiSiliconFileTransferUtils.getInstance().init();
                HiSiliconFileTransferUtils.getInstance().sendFile(filePath, FissionConstant.OTA_TYPE_FIRMWARE);
            }else{
                FissionLogUtils.d("wl", "存储空间不足，不能升级资源");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAgpsFileDownloadEvent(AgpsFileDownloadEvent event) {
        FissionLogUtils.d("wl", "下载AGPS星历文件事件："+event);
        if(event.errorCode == 0){
            filePath = event.filePath;
            uploadAgpsFiles(new File(event.filePath));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceInfoEvent(DeviceInfoEvent event) {
        FissionLogUtils.d("wl", "获取设备信息事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceResErrorEvent(DeviceResErrorEvent event) {
        FissionLogUtils.d("wl", "设备主动上报资源异常："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetKwsSwitchEvent(SetKwsSwitchEvent event) {
        FissionLogUtils.d("wl", "设置指令开关事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetDndModeEvent(SetDndModeEvent event) {
        FissionLogUtils.d("wl", "设置勿扰模式开关事件："+event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetInteractionCodeEvent(SetInteractionCodeEvent event) {
        FissionLogUtils.d("wl", "设置主动天气交互事件："+event);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getPath() {
        File dir = null;
        boolean state = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (state) {
            if (Build.VERSION.SDK_INT >= 28) {
                //Android10之后
                dir = this.getExternalFilesDir(null);
            } else {
                dir = Environment.getExternalStorageDirectory();
            }
        } else {
            dir = Environment.getRootDirectory();
        }
        return dir.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        if (data.getData() == null){
            return;
        }

        Uri uri = data.getData();
        String dd = uri.toString();
        String prefix = dd.substring(dd.lastIndexOf(".")+1);
        LogUtils.d("prefix",prefix);
        String name = null;
        if ("bin".equals(prefix)){
            String path = uri.getPath();
            String []split = path.split(":");
            tv_log.setText(split[split.length-1]);
        }else {
            tv_log.setText("后缀名不对,请重新选择");
        }
        filePath = UriUtils.uri2File(uri).getAbsolutePath();
        LogUtils.d("获取文件路径getData",filePath);
        FissionLogUtils.d("wl", "otaType:"+otaType);
        if(otaType == 4){
            FissionSdkBleManage.getInstance().hisiSppOtaUploadFileInit(new File(filePath).length());
            //spp模式 OTA
//            HiSiliconFileTransferUtils.getInstance().setHiSiliconFileTransferListener(new HiSiliconFileTransferUtils.HiSiliconFileTransferListener() {
//                @Override
//                public void onProgressChanged(long curFrames, long framesCount, int fileListIndex, int fileSize) {
//                    ThreadUtils.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            int progress = (int)(curFrames*100/framesCount);
//                            tv_log.setText("OTA文件传输进度："+progress+"%");
//                        }
//                    });
//                }
//
//                @Override
//                public void onComplete() {
//                    FissionSdkBleManage.getInstance().notifyOtaFirmware();
//                }
//
//                @Override
//                public void onTimeOut() {
//
//                }
//
//                @Override
//                public void onError(Exception e) {
//
//                }
//
//                @Override
//                public void onTransmitting() {
//
//                }
//            });
//            HiSiliconFileTransferUtils.getInstance().init();
//            HiSiliconFileTransferUtils.getInstance().sendFile(filePath, FissionConstant.OTA_TYPE_FIRMWARE);
        }else{
            try {
                UploadFileConfig uploadFileConfig = new UploadFileConfig();
                uploadFileConfig.setSize((int)new File(filePath).length());
                uploadFileConfig.setFileName(FileUtils.getFileName(filePath));
                if(otaType == 1){
                    uploadFileConfig.setFilePath("/user/res/");
                }else if(otaType == 2){
                    uploadFileConfig.setFilePath("/user/");
                    uploadFileConfig.setFileName("update.fwpkg");
                }else if(otaType == 5 || otaType == 6 || otaType == 7){
                    uploadFileConfig.setFilePath("user/res/pet/");
                }
                uploadFileConfig.setType(0);
                uploadFileConfig.setOperate(0);
                uploadFileConfig.setCrcCode(CRC32Checksum.crc32(0, FileIOUtils.readFile2BytesByStream(filePath)));
                uploadFileConfig.setResuming(true);
                FissionSdkBleManage.getInstance().uploadFileInit(uploadFileConfig);
                FissionLogUtils.d("wl", "文件上传参数协商："+uploadFileConfig);
            } catch (Exception e) {
                FissionLogUtils.d("wl", "文件读取异常，CRC32计算失败");
            }
        }
    }

    private void uploadResFile(){
        try {
            UploadFileConfig uploadFileConfig = new UploadFileConfig();
            if(otaType == 1){
                uploadFileConfig.setFilePath("/user/res/");
            }else if(otaType == 2){
                uploadFileConfig.setFilePath("/user/");
            }
            uploadFileConfig.setSize((int)new File(filePath).length());
            uploadFileConfig.setFileName(FileUtils.getFileName(filePath));
            uploadFileConfig.setType(0);
            uploadFileConfig.setOperate(0);
            uploadFileConfig.setCrcCode(CRC32Checksum.crc32(0, FileIOUtils.readFile2BytesByStream(filePath)));
            uploadFileConfig.setResuming(true);
            FissionSdkBleManage.getInstance().uploadFileInit(uploadFileConfig);
            FissionLogUtils.d("wl", "文件上传参数协商："+uploadFileConfig);
        } catch (Exception e) {
            FissionLogUtils.d("wl", "文件读取异常，CRC32计算失败");
        }
    }

    private void uploadAgpsFiles(File file){
        UploadFileConfig uploadFileConfig = new UploadFileConfig();
        uploadFileConfig.setFilePath("/user/xgnss/");
        uploadFileConfig.setSize((int)file.length());
        uploadFileConfig.setFileName(FileUtils.getFileName(file.getAbsolutePath()));
        uploadFileConfig.setType(1);
        uploadFileConfig.setOperate(0);
        uploadFileConfig.setCrcCode(CRC32Checksum.crc32(0, FileIOUtils.readFile2BytesByStream(file.getAbsolutePath())));
        uploadFileConfig.setResuming(true);
        FissionSdkBleManage.getInstance().uploadFileInit(uploadFileConfig);
        FissionLogUtils.d("wl", "星历文件上传参数协商："+uploadFileConfig);
    }

    private void animAdd(int code, int count){
        AnimationConfig animationConfig = new AnimationConfig();
        animationConfig.setNumber(code);
        animationConfig.setName(FileUtils.getFileName(filePath));
        animationConfig.setCount(count);
        animationConfig.setPointY(0);
        animationConfig.setPointX(0);
        animationConfig.setSizeWidth(466);
        animationConfig.setSizeHeight(466);
        animationConfig.setScale(false);
        animationConfig.setMultiForm(false);
        animationConfig.setPlaySound(false);
        FissionSdkBleManage.getInstance().addAnimation(animationConfig);
    }

    private void scanDiffUploadFile(){
        String localPath = getExternalFilesDir(null)+"/aipet/user/";
        HiSiDirectoryUploadManager manager = new HiSiDirectoryUploadManager();

        manager.uploadDirectory(
                new File(localPath),
                "/user",
                FissionConstant.OTA_TYPE_AI_PET_RES,
                true,   // ← 是否启用断点恢复
                new HiSiDirectoryUploadManager.Callback() {

                    @Override
                    public void onProgress(int cur, int total) {
                        LogUtils.d("onProgress", "进度：" + cur + "/" + total);
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_log.setText("UPLOAD进度：" + cur + "/" + total);
                                mIndex = cur;
                                mTotal = total;
                            }
                        });
                    }

                    @Override
                    public void onTotalProgress(int percent, long sentBytes, long totalBytes) {
                        FissionLogUtils.d("wl", "onProgress: percent=="+percent+", sentBytes=="+sentBytes+", totalBytes=="+totalBytes);
                        FileProgress fileProgress = new FileProgress(mIndex, mTotal, percent, sentBytes, totalBytes);
                        FissionSdkBleManage.getInstance().diffUploadFileProgress(fileProgress);
                    }

                    @Override
                    public void onComplete(List<UploadResult> results) {

                        boolean hasFail = false;

                        for (UploadResult r : results) {
                            if (!r.success) {
                                hasFail = true;
                                LogUtils.e("UPLOAD",
                                        "失败文件：" + r.devicePath
                                                + " reason=" + r.reason);
                            }
                        }

                        if(results.isEmpty()){
                            FileProgress fileProgress = new FileProgress(mIndex, mTotal, 100, 0, 0);
                            FissionSdkBleManage.getInstance().diffUploadFileProgress(fileProgress);
                        }

//                                if (hasFail) {
//                                    LogUtils.w("UPLOAD", "开始失败文件重传");
//                                    manager.retryFailed(
//                                            FissionConstant.OTA_TYPE_AI_PET_RES,
//                                            new HiSiDirectoryUploadManager.Callback() {
//
//                                                @Override
//                                                public void onProgress(int cur, int total) {
//                                                    LogUtils.d("UPLOAD-RETRY",
//                                                            cur + "/" + total);
//                                                }
//
//                                                @Override
//                                                public void onComplete(List<UploadResult> retryResults) {
//                                                    LogUtils.d("UPLOAD-RETRY",
//                                                            "失败文件重传完成");
//                                                }
//                                            }
//                                    );
//                                }
                    }
                }
        );
    }
}
