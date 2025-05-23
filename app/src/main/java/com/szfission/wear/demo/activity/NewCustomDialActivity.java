package com.szfission.wear.demo.activity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.GridSpanSizeLookup;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.CustomDialBinInfo;
import com.fission.wear.sdk.v2.bean.CustomWatchFaceInfo;
import com.fission.wear.sdk.v2.bean.FssStatus;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.utils.FissionCustomDialUtil;
import com.fission.wear.sdk.v2.utils.QuickLZUtils;
import com.lcw.library.stickerview.BaseSticker;
import com.lcw.library.stickerview.Sticker;
import com.lcw.library.stickerview.StickerLayout;
import com.szfission.wear.demo.App;
import com.szfission.wear.demo.C;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.adapter.MultiCustomDialAdapter;
import com.szfission.wear.demo.bean.MultiCustomDial;
import com.szfission.wear.demo.dialog.NormalDialog;
import com.szfission.wear.demo.util.CameraPhotoHelper;
import com.szfission.wear.demo.util.PhotoUtils;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.ImageScalingUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yalantis.ucrop.UCrop;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewCustomDialActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {
    ColorMatrix colorMatrix;

    private RxPermissions rxPermissions;

    TextView tv_file_directory;

    StickerLayout iv_watch_face;

    ImageView iv_watch_face2;
    Button btn_get_pic1;
    Button btn_get_pic2;
    SeekBar seekBarR2;
    TextView tv_color2;
    TextView tv_progress;

    RecyclerView recyclerView;

    FissionCustomDialUtil.CustomDialModel customDialModel;

    int dialWidth = 320;
    int dialHeight = 390;
    int thumbnailWidth = 320;
    int thumbnailHigh = 390;
    int dialShape = 0;

    private int colorValue ;

    private CustomWatchFaceInfo mCustomWatchFaceInfo;
    private List<MultiCustomDial> multiCustomDialList;
    private MultiCustomDialAdapter mAdapter;

    private Map<String, Sticker> stickerMap = new HashMap<>(); //缓存已经添加的功能贴纸。
    private Sticker lastScale =null; //上次设置的刻度
    private Sticker lastPointer =null;  //上次设置的指针
    private Sticker lastDateTime =null;  //上次设置的时间样式
    private Sticker lastBattery =null; //上次设置的电池样式
    private Sticker lastBle =null; //上次设置的BLE连接样式
    private Sticker lastBt =null; //上次设置的BT连接样式

    private String directory;

    private CustomDialBinInfo mCustomDialBinInfo;
    private int pointerIndex;
    private int numberDialIndex;
    private int batteryIndex;
    private int bleIndex;
    private int btIndex;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_dial_new);
        setTitle(R.string.FUNC_PUSH_CUSTOM_DIAL_NEW);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        colorMatrix = new ColorMatrix();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        tv_file_directory = findViewById(R.id.tv_file_directory);
        iv_watch_face = findViewById(R.id.iv_watch_face);
        recyclerView = findViewById(R.id.recyclerView);

        if(App.mHardWareInfo!=null){
            dialWidth = App.mHardWareInfo.getDeviceWidth();
            dialHeight = App.mHardWareInfo.getDeviceHigh();
            thumbnailWidth = App.mHardWareInfo.getThumbnailWidth();
            thumbnailHigh = App.mHardWareInfo.getThumbnailHigh();
            dialShape = App.mHardWareInfo.getDialShape();
        }

        directory = getPath()+"/fission_custom_dial_"+dialWidth+"x"+dialHeight+"/";
        String text = getString(R.string.new_custom_dial_directory, directory);
        tv_file_directory.setText(text);
        FileUtils.createOrExistsDir(directory);

        if(FileUtils.listFilesInDir(directory).size()>0){
            initCustomDialData();
        }

        colorValue = getResources().getColor(R.color.public_custom_dial_1);

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionAtCmdResultListener(){

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
            public void fssSuccess(FssStatus fssStatus) {
                super.fssSuccess(fssStatus);
                tv_progress.setText("推送进度:"+fssStatus.getFssStatus()+"%");
            }
        });

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionBigDataCmdResultListener() {
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
            public void onUpdateDialProgress(int state, int progress) {
                super.onUpdateDialProgress(state, progress);
                LogUtils.d("wl", "自定义表盘推送："+progress);
            }
        });
    }


    private void initCustomDialData(){
        String dataJson = FileIOUtils.readFile2String(directory+"info_png.json");
        mCustomWatchFaceInfo =  GsonUtils.fromJson(dataJson, CustomWatchFaceInfo.class) ;
        mCustomDialBinInfo = FissionCustomDialUtil.analyzeCustomDialBinInfo(FileIOUtils.readFile2BytesByStream(directory+"packet.bin"));
//        LogUtils.d("wl", "----analyzeCustomDialBinInfo---:"+mCustomDialBinInfo.getSmallNumber().imageData);
//        String filePath = Environment.getExternalStorageDirectory()+"/custom_dial_small_number.bin";
//        FileUtils.createFileByDeleteOldFile(filePath);
//        FileIOUtils.writeFileFromString (filePath, mCustomDialBinInfo.getBigNumber().imageData);

        multiCustomDialList = new ArrayList<>();
        multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_TITLE, "背景图"));
        List<CustomWatchFaceInfo.BackgroundImage> defaultBgImageList = mCustomWatchFaceInfo.getDefaultBgImageList();
        for(int i=0; i<defaultBgImageList.size(); i++){
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_BG, defaultBgImageList.get(i).Background_Name));
        }
        multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_TITLE, "刻度"));
        List<CustomWatchFaceInfo.ScaleBgImage> scaleBgList = mCustomWatchFaceInfo.getScaleBgList();
        for(int i=0; i<scaleBgList.size(); i++){
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_SCALE, scaleBgList.get(i).Graduation_Name));
        }
        multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_TITLE, "指针"));
        List<CustomWatchFaceInfo.PointerBgImage> pointerBgList = mCustomWatchFaceInfo.getPointerBgList();
        for(int i=0; i<pointerBgList.size(); i++){
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_POINTER, pointerBgList.get(i).PointerSet_Name));
        }
        multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_TITLE, "功能图标"));
        List<CustomWatchFaceInfo.Function> functionIconList = new ArrayList<>();

        List<CustomWatchFaceInfo.HeartRateIcon> heartRateIcons = mCustomWatchFaceInfo.getHeartrateIconList();
        for(CustomWatchFaceInfo.HeartRateIcon heartRateIcon : heartRateIcons){
            CustomWatchFaceInfo.Function function = new CustomWatchFaceInfo().new Function(6,heartRateIcon.Heartrate_Name);
            functionIconList.add(function);
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_FUNCTION, function));
        }

        List<CustomWatchFaceInfo.BloodOxygenIcon> bloodOxygenIcons = mCustomWatchFaceInfo.getBloodoxygenIconList();
        for(CustomWatchFaceInfo.BloodOxygenIcon bloodOxygenIcon : bloodOxygenIcons){
            CustomWatchFaceInfo.Function function = new CustomWatchFaceInfo().new Function(3,bloodOxygenIcon.bloodoxygen_Name);
            functionIconList.add(function);
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_FUNCTION, function));
        }

        List<CustomWatchFaceInfo.StepCountIcon> stepCountIcons = mCustomWatchFaceInfo.getStepcountIconList();
        for(CustomWatchFaceInfo.StepCountIcon stepCountIcon : stepCountIcons){
            CustomWatchFaceInfo.Function function = new CustomWatchFaceInfo().new Function(2,stepCountIcon.stepcount_Name);
            functionIconList.add(function);
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_FUNCTION, function));
        }

        List<CustomWatchFaceInfo.DistanceIcon> distanceIcons = mCustomWatchFaceInfo.getDistanceIconList();
        for(CustomWatchFaceInfo.DistanceIcon distanceIcon : distanceIcons){
            CustomWatchFaceInfo.Function function = new CustomWatchFaceInfo().new Function(5,distanceIcon.distance_Name);
            functionIconList.add(function);
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_FUNCTION, function));
        }

        List<CustomWatchFaceInfo.CaloriesIcon> caloriesIcons = mCustomWatchFaceInfo.getCaroliIconList();
        for(CustomWatchFaceInfo.CaloriesIcon caloriesIcon : caloriesIcons){
            CustomWatchFaceInfo.Function function = new CustomWatchFaceInfo().new Function(0,caloriesIcon.caroli_Name);
            functionIconList.add(function);
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_FUNCTION, function));
        }

        List<CustomWatchFaceInfo.BloodPressureIcon> bloodPressureIcons = mCustomWatchFaceInfo.getBloodpressureIconList();
        for(CustomWatchFaceInfo.BloodPressureIcon bloodPressureIcon : bloodPressureIcons){
            CustomWatchFaceInfo.Function function = new CustomWatchFaceInfo().new Function(7,bloodPressureIcon.bloodpressure_Name);
            functionIconList.add(function);
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_FUNCTION, function));
        }

        List<CustomWatchFaceInfo.StressIcon> stressIcons = mCustomWatchFaceInfo.getStressIconList();
        for(CustomWatchFaceInfo.StressIcon stressIcon : stressIcons){
            CustomWatchFaceInfo.Function function = new CustomWatchFaceInfo().new Function(1,stressIcon.stress_Name);
            functionIconList.add(function);
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_FUNCTION, function));
        }

//        List<CustomWatchFaceInfo.BatteryIcon> batteryIcons = mCustomWatchFaceInfo.getBatteryList();
//        for(CustomWatchFaceInfo.BatteryIcon batteryIcon : batteryIcons){
//            CustomWatchFaceInfo.Function function = new CustomWatchFaceInfo().new Function(4,batteryIcon.battery_Name);
//            functionIconList.add(function);
//            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_FUNCTION, function));
//        }

        List<CustomWatchFaceInfo.BatteryIconStyle> batteryStyleList = mCustomWatchFaceInfo.getBatteryStyleList();
        multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_TITLE, "电量"));
        for(CustomWatchFaceInfo.BatteryIconStyle batteryIconStyle : batteryStyleList){
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_BATTERY, batteryIconStyle.BatteryStyle_Name));
        }

        List<CustomWatchFaceInfo.BleIcon> bleIconList = mCustomWatchFaceInfo.getBLEIconList();
        multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_TITLE, "BLE蓝牙连接"));
        for(CustomWatchFaceInfo.BleIcon bleIcon : bleIconList){
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_BLE, bleIcon.ble_Name));
        }

        List<CustomWatchFaceInfo.BtIcon> btIconList = mCustomWatchFaceInfo.getBTIconList();
        multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_TITLE, "BT蓝牙连接"));
        for(CustomWatchFaceInfo.BtIcon btIcon : btIconList){
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_BT, btIcon.bt_Name));
        }

        List<CustomWatchFaceInfo.TimeStyleImage> datetimeStyle = mCustomWatchFaceInfo.getTimeStyleList();
        multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_TITLE, "数字时间样式"));
        for(CustomWatchFaceInfo.TimeStyleImage timeStyleImage: datetimeStyle){
            multiCustomDialList.add(new MultiCustomDial(MultiCustomDial.TYPE_DATA_DATETIME, timeStyleImage.TimeStyle_Name));
        }

        mAdapter = new MultiCustomDialAdapter(this, multiCustomDialList, directory);
        View headerView = LayoutInflater.from(this).inflate(R.layout.list_item_header, null);
        mAdapter.addHeaderView(headerView);

        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                MultiCustomDial multiCustomDial = multiCustomDialList.get(position);
                Bitmap bitmap;
                switch (multiCustomDial.getItemType()){
                    case MultiCustomDial.TYPE_DATA_BG:
                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+multiCustomDial.getData().toString()));
                        iv_watch_face.setImageBitmap(bitmap);
                        break;

                    case MultiCustomDial.TYPE_DATA_SCALE:
                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+multiCustomDial.getData().toString()));
                        if(lastScale!=null){
                            iv_watch_face.removeSticker(lastScale);
                            lastScale = null;
                        }else{
                            Sticker sticker = new Sticker(NewCustomDialActivity.this, bitmap);
                            sticker.setMove(false);
                            iv_watch_face.addSticker(sticker);
                            lastScale = sticker;
                        }
                        break;

                    case MultiCustomDial.TYPE_DATA_POINTER:
                        String  pointerName = multiCustomDial.getData().toString();
                        pointerIndex = Integer.parseInt(pointerName.substring(pointerName.lastIndexOf(".")-1, pointerName.lastIndexOf(".")));
                        LogUtils.d("wl", "选中指针套图下标:"+pointerIndex);
                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+multiCustomDial.getData().toString()));
                        if(lastPointer!=null){
                            iv_watch_face.removeSticker(lastPointer);
                            lastPointer = null;
                        }else{
                            if(lastDateTime!=null){
//                                iv_watch_face.removeSticker(lastDateTime);
//                                lastDateTime = null;
                            }
                            Sticker sticker = new Sticker(NewCustomDialActivity.this, bitmap);
                            sticker.setMove(false);
                            iv_watch_face.addSticker(sticker);
                            lastPointer = sticker;
                        }
                        break;

                    case MultiCustomDial.TYPE_DATA_FUNCTION:
                        CustomWatchFaceInfo.Function function = (CustomWatchFaceInfo.Function) multiCustomDial.getData();
                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+function.functionName));
                        Sticker sticker = stickerMap.get(function.functionName);
                        if(sticker!=null){
                            iv_watch_face.removeSticker(sticker);
                            stickerMap.remove(function.functionName);
                        }else{
                            Sticker stickerFunction = null;
                            switch (function.functionType){
                                case 0:
                                    stickerFunction = new Sticker(NewCustomDialActivity.this, bitmap, "50", (function.functionType+1), iv_watch_face.getWidth(), iv_watch_face.getHeight());
                                    stickerFunction.setType(FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_CALORIES);
                                    break;

                                case 1:
                                    stickerFunction = new Sticker(NewCustomDialActivity.this, bitmap, "100", (function.functionType+1), iv_watch_face.getWidth(), iv_watch_face.getHeight());
                                    stickerFunction.setType(FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_STRESS);
                                    break;

                                case 2:
                                    stickerFunction = new Sticker(NewCustomDialActivity.this, bitmap, "12000", (function.functionType+1), iv_watch_face.getWidth(), iv_watch_face.getHeight());
                                    stickerFunction.setType(FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_STEPS);
                                    break;

                                case 3:
                                    stickerFunction = new Sticker(NewCustomDialActivity.this, bitmap, "99%", (function.functionType+1), iv_watch_face.getWidth(), iv_watch_face.getHeight());
                                    stickerFunction.setType(FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_SPO2);
                                    break;

                                case 4:
                                    stickerFunction = new Sticker(NewCustomDialActivity.this, bitmap, "10%", (function.functionType+1), iv_watch_face.getWidth(), iv_watch_face.getHeight());
                                    stickerFunction.setType(FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_BATTERY);
                                    break;

                                case 5:
                                    stickerFunction = new Sticker(NewCustomDialActivity.this, bitmap, "5", (function.functionType+1), iv_watch_face.getWidth(), iv_watch_face.getHeight());
                                    stickerFunction.setType(FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_DISTANCE);
                                    break;

                                case 6:
                                    stickerFunction = new Sticker(NewCustomDialActivity.this, bitmap, "88", (function.functionType+1), iv_watch_face.getWidth(), iv_watch_face.getHeight());
                                    stickerFunction.setType(FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_HEARTBEAT);
                                    break;

                                case 7:
                                    stickerFunction = new Sticker(NewCustomDialActivity.this, bitmap, "95/130", (function.functionType+1), iv_watch_face.getWidth(), iv_watch_face.getHeight());
                                    stickerFunction.setType(FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_BLOOD_PRESSURE);
                                    break;
                            }
                            iv_watch_face.addSticker(stickerFunction);
                            stickerMap.put(function.functionName, stickerFunction);

                        }
                        break;

                    case MultiCustomDial.TYPE_DATA_DATETIME:
                        String  numberStyleName = multiCustomDial.getData().toString();
                        numberDialIndex = Integer.parseInt(numberStyleName.substring(numberStyleName.lastIndexOf(".")-1, numberStyleName.lastIndexOf(".")));
                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+multiCustomDial.getData().toString()));
                        if(lastDateTime!=null){
                            iv_watch_face.removeSticker(lastDateTime);
                            lastDateTime = null;
                        }else{
                            if(lastPointer!=null){
//                                iv_watch_face.removeSticker(lastPointer);
//                                lastPointer = null;
                            }
                            Sticker stickerDateTime = new Sticker(NewCustomDialActivity.this, bitmap);
                            iv_watch_face.addSticker(stickerDateTime);
                            lastDateTime = stickerDateTime;
                        }
                        break;

                    case MultiCustomDial.TYPE_DATA_BATTERY:
                        String  batteryStyleName = multiCustomDial.getData().toString();
                        batteryIndex = Integer.parseInt(batteryStyleName.substring(batteryStyleName.lastIndexOf(".")-1, batteryStyleName.lastIndexOf(".")))-1;
                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+multiCustomDial.getData().toString())); // 带数字图片
//                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+mCustomWatchFaceInfo.getBatteryList().get(batteryIndex).battery_Name));
                        if(lastBattery!=null){
                            iv_watch_face.removeSticker(lastBattery);
                            lastBattery = null;
                        }else{
                            float x =  iv_watch_face.getWidth() -  bitmap.getWidth() -10;
                            float y = iv_watch_face.getHeight()*0.1f;
                            if(dialShape == FissionConstant.ROUND){
                                x = iv_watch_face.getWidth()/2f - bitmap.getWidth()/2f;
                                y = iv_watch_face.getHeight()*0.65f;
                            }
                            Sticker stickerBattery = new Sticker(NewCustomDialActivity.this, bitmap, "", x, y);  //不带标签
//                            Sticker stickerBattery = new Sticker(NewCustomDialActivity.this, bitmap, "80%", x, y);
//                            switch (batteryIndex){
//                                case 0:
//                                case 2:
//                                    stickerBattery.setLabelLocation(BaseSticker.LOCATION_BOT);
//                                    break;
//
//                                case 1:
//                                    stickerBattery.setLabelLocation(BaseSticker.LOCATION_LEFT);
//                                    break;
//
//                                case 3:
//                                    stickerBattery.setLabelLocation(BaseSticker.LOCATION_RIGHT);
//                                    break;
//                            }
                            stickerBattery.setMove(true);
                            iv_watch_face.addSticker(stickerBattery);
                            lastBattery = stickerBattery;
                        }
                        break;

                    case MultiCustomDial.TYPE_DATA_BLE:
                        String  bleStyleName = multiCustomDial.getData().toString();
                        bleIndex = Integer.parseInt(bleStyleName.substring(bleStyleName.lastIndexOf(".")-1, bleStyleName.lastIndexOf(".")))-1;
                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+multiCustomDial.getData().toString()));
                        if(lastBle!=null){
                            iv_watch_face.removeSticker(lastBle);
                            lastBle = null;
                        }else{
                            float x =  iv_watch_face.getWidth()*0.1f;
                            float y = iv_watch_face.getHeight()*0.1f;
                            if(dialShape == FissionConstant.ROUND){
                                x = iv_watch_face.getWidth()/2f - bitmap.getWidth() - bitmap.getWidth()/4f;
                                y = iv_watch_face.getHeight()*0.15f;
                            }
                            Sticker stickerBle = new Sticker(NewCustomDialActivity.this, bitmap, "", x, y);
                            stickerBle.setMove(true);
                            iv_watch_face.addSticker(stickerBle);
                            lastBle = stickerBle;
                        }
                        break;

                    case MultiCustomDial.TYPE_DATA_BT:
                        String  btStyleName = multiCustomDial.getData().toString();
                        btIndex = Integer.parseInt(btStyleName.substring(btStyleName.lastIndexOf(".")-1, btStyleName.lastIndexOf(".")))-1;
                        bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+multiCustomDial.getData().toString()));
                        if(lastBt!=null){
                            iv_watch_face.removeSticker(lastBt);
                            lastBt = null;
                        }else{
                            float x =  iv_watch_face.getWidth()*0.1f+bitmap.getWidth() +10f;
                            float y = iv_watch_face.getHeight()*0.1f;
                            if(dialShape == FissionConstant.ROUND){
                                x = iv_watch_face.getWidth()/2f + bitmap.getWidth()/4f;
                                y = iv_watch_face.getHeight()*0.15f;
                            }
                            Sticker stickerBt = new Sticker(NewCustomDialActivity.this, bitmap, "", x, y);
                            stickerBt.setMove(true);
                            iv_watch_face.addSticker(stickerBt);
                            lastBt = stickerBt;
                        }
                        break;

                }

            }
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        mAdapter.setGridSpanSizeLookup(new GridSpanSizeLookup() {
            @Override
            public int getSpanSize(@NonNull GridLayoutManager gridLayoutManager, int viewType, int position) {
                MultiCustomDial multiCustomDial = multiCustomDialList.get(position);
                int spanSize = 0;
                switch (multiCustomDial.getType()){
                    case MultiCustomDial.TYPE_TITLE:
                        spanSize = 4;
                        break;

                    case MultiCustomDial.TYPE_DATA_BG:
                    case MultiCustomDial.TYPE_DATA_SCALE:
                    case MultiCustomDial.TYPE_DATA_POINTER:
                        spanSize = 2;
                        break;

                    case MultiCustomDial.TYPE_DATA_FUNCTION:
                    case MultiCustomDial.TYPE_DATA_DATETIME:
                    case MultiCustomDial.TYPE_DATA_BLE:
                    case MultiCustomDial.TYPE_DATA_BT:
                    case MultiCustomDial.TYPE_DATA_BATTERY:
                        spanSize = 1;
                        break;

                }
                return spanSize;
            }
        });
        recyclerView.setAdapter(mAdapter);

        btn_get_pic1 = headerView.findViewById(R.id.btn_get_pic1);
        btn_get_pic2 = headerView.findViewById(R.id.btn_get_pic2);
        seekBarR2 = headerView.findViewById(R.id.bar_R2);
        tv_color2 = headerView.findViewById(R.id.tv_color2);
        tv_progress = headerView.findViewById(R.id.tv_progress);
        iv_watch_face2 = headerView.findViewById(R.id.iv_watch_thumb);

        seekBarR2.setOnSeekBarChangeListener(this);
        rxPermissions = new RxPermissions(this);
        btn_get_pic1.setOnClickListener(v -> {
            NormalDialog normalDialog = new NormalDialog(NewCustomDialActivity.this,2, ModelConstant.FUNC_PUSH_CUSTOM_DIAL);
            normalDialog.setOnConfirmClickListener(content -> {
                if (content.equals("0")){
                    //打开相册
                    PhotoUtils.getInstance().openAlbum(rxPermissions,this);
                }else {
                    //打开相机
                    PhotoUtils.getInstance().openCamera(rxPermissions,this);
                }
            });
        });
        btn_get_pic2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float zoomOut = (float) iv_watch_face.getWidth() / dialWidth;
                customDialModel = new FissionCustomDialUtil().new CustomDialModel();
                if(lastDateTime!=null){
                    int datetimeNumberX = (int)(lastDateTime.getDstPoints()[0]/zoomOut);
                    int datetimeNumberY = (int)(lastDateTime.getDstPoints()[1]/zoomOut);
                    if(dialShape == FissionConstant.ROUND){
                        datetimeNumberX = datetimeNumberX+20;
                    }
                    customDialModel.setDatetimeNumberX(datetimeNumberX);
                    customDialModel.setDatetimeNumberY(datetimeNumberY);
                    customDialModel.setDatetimeType(FissionCustomDialUtil.CustomDialModel.DATETIME_TYPE_NUMBER);
                    Bitmap numberStyleImage =  ImageUtils.scale(lastDateTime.getBitmap(),
                            (int)(lastDateTime.getBitmap().getWidth()/zoomOut), (int)(lastDateTime.getBitmap().getHeight()/zoomOut));
                    customDialModel.setNumberStyleImage(numberStyleImage);
                    iv_watch_face.removeSticker(lastDateTime);
                }
                List<FissionCustomDialUtil.CustomDialFunctionModel> functionModels =new ArrayList<>();
                if(stickerMap.size()>0){
                    for(Sticker sticker: stickerMap.values()){
                        FissionCustomDialUtil.CustomDialFunctionModel functionModel = new FissionCustomDialUtil().new CustomDialFunctionModel();
                        int functionNumberX = (int)(sticker.getDstPoints()[8]/zoomOut);
                        int functionNumberY = (int)((sticker.getDstPoints()[7]+20)/zoomOut);
                        functionModel.setType(sticker.getType());
                        functionModel.setFunctionNumberX(functionNumberX);
                        functionModel.setFunctionNumberY(functionNumberY);
                        functionModels.add(functionModel);
                    }
                    iv_watch_face.hideAllLabel();
                }
                Bitmap pointerBitmap = ImageUtils.view2Bitmap(iv_watch_face);
                if(lastPointer!=null){
                    customDialModel.setDatetimeType(FissionCustomDialUtil.CustomDialModel.DATETIME_TYPE_POINTER);
                    customDialModel.setPointerIndex(pointerIndex-1);
                    customDialModel.setPointerStyleImage(lastPointer.getBitmap());
                    iv_watch_face.removeSticker(lastPointer);
                }
                if(lastBattery!=null){
                    int batteryX = (int)(lastBattery.getDstPoints()[0]/zoomOut);
                    int batteryY = (int)(lastBattery.getDstPoints()[1]/zoomOut);
                    if(dialShape == FissionConstant.ROUND){
                        if(batteryIndex == 0 || batteryIndex == 2){
                            batteryX =  (int)(lastBattery.getDstPoints()[8]/zoomOut-mCustomDialBinInfo.getBatterySymbol().width/2);
                        }else if(batteryIndex == 1){
                            batteryX =  (int)(lastBattery.getDstPoints()[8]/zoomOut+15);
                            batteryY =  (int)(lastBattery.getDstPoints()[5]/zoomOut*0.82f);
                        }else{
                            batteryX =  (int)(lastBattery.getDstPoints()[8]/zoomOut-mCustomDialBinInfo.getBatterySymbol().width/2);
                        }
                    }
                    customDialModel.setBatteryIndex(batteryIndex);
                    customDialModel.setShowBattery(true);
                    customDialModel.setBatteryX(batteryX);
                    customDialModel.setBatteryY(batteryY);
                    iv_watch_face.removeSticker(lastBattery);
                }
                if(lastBle!=null){
                    int bleX = (int)(lastBle.getDstPoints()[0]/zoomOut);
                    int bleY = (int)(lastBle.getDstPoints()[1]/zoomOut);
                    customDialModel.setBleIndex(bleIndex);
                    customDialModel.setShowBle(true);
                    customDialModel.setBleX(bleX);
                    customDialModel.setBleY(bleY);
                    customDialModel.setBleColor(mCustomWatchFaceInfo.getBLEIconList().get(bleIndex).icon_color);
                    iv_watch_face.removeSticker(lastBle);
                }
                if(lastBt!=null){
                    int btX = (int)(lastBt.getDstPoints()[0]/zoomOut);
                    int btY = (int)(lastBt.getDstPoints()[1]/zoomOut);
                    customDialModel.setBtIndex(btIndex);
                    customDialModel.setShowBt(true);
                    customDialModel.setBtX(btX);
                    customDialModel.setBtY(btY);
                    customDialModel.setBtColor(mCustomWatchFaceInfo.getBTIconList().get(btIndex).icon_color);
                    iv_watch_face.removeSticker(lastBt);

                }
                Bitmap bitmap = ImageUtils.view2Bitmap(iv_watch_face);

                if(lastDateTime!=null){
                    iv_watch_face.addSticker(lastDateTime);
                    customDialModel.setPreviewImage(bitmap);
                    customDialModel.setNumberDialIndex(numberDialIndex-1);
                }
                if(lastPointer!=null){
                    iv_watch_face.addSticker(lastPointer);
//                    customDialModel.setPreviewImage(pointerBitmap);
                }

                if(stickerMap.size()>0){
                    for(Sticker sticker: stickerMap.values()){
                        if(sticker.getType() == FissionCustomDialUtil.CustomDialFunctionModel.FUNCTION_TYPE_BLOOD_PRESSURE){
                            if(dialShape == FissionConstant.ROUND){
                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "/", (int)sticker.getDstPoints()[8], (int)(sticker.getDstPoints()[7]+sticker.getBitmap().getHeight()/1.8)+15, colorValue);
                            }else{
                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "/", (int)sticker.getDstPoints()[8], (int)(sticker.getDstPoints()[7]+sticker.getBitmap().getHeight()/1.8)+6, colorValue);
                            }
                        }
                    }
                }

                if(lastBattery!=null){
                    iv_watch_face.addSticker(lastBattery);
                    customDialModel.setPreviewImage(pointerBitmap);
                    Bitmap coverBitmap;
                    switch (batteryIndex){
//                        case 0:
//                            if(dialShape == FissionConstant.ROUND){
//                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "%", (int)lastBattery.getDstPoints()[8]+20, (int)(lastBattery.getDstPoints()[7]-dialHeight*0.17), colorValue);
//                            }else{
//                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "%", (int)lastBattery.getDstPoints()[8]-20, (int)lastBattery.getDstPoints()[7], colorValue);
//                            }
//                            break;

//                        case 1:
//                            if(dialShape == FissionConstant.ROUND){
//                                customDialModel.setBatteryX((int)(customDialModel.getBatteryX()+mCustomDialBinInfo.getBatterySymbol().width*0.75));
//                                customDialModel.setBatteryY(customDialModel.getBatteryY()+30);
//                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "%", (int)(customDialModel.getBatteryX()*zoomOut-mCustomDialBinInfo.getBatterySymbol().width-10), (int)(customDialModel.getBatteryY()*zoomOut+mCustomDialBinInfo.getBatterySymbol().height+20), colorValue);
//                            }else{
//                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "%", (int)lastBattery.getDstPoints()[6]-(int)(lastBattery.getBitmap().getWidth()/3.5), (int)lastBattery.getDstPoints()[5]-(int)(lastBattery.getBitmap().getHeight()/1.8), colorValue);
//                            }
//                            break;

                        case 2:
                            if(dialShape == FissionConstant.ROUND){
                                coverBitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+mCustomWatchFaceInfo.getBatteryList().get(2).battery_Name));
                                bitmap = ImageUtils.addImageWatermark(bitmap, coverBitmap, (int)(customDialModel.getBatteryX()*zoomOut), (int)lastBattery.getDstPoints()[1], 255);
//                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "%", (int)lastBattery.getDstPoints()[8]+20, (int)(lastBattery.getDstPoints()[7]-10), colorValue);
                            }else{
                                coverBitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+mCustomWatchFaceInfo.getBatteryList().get(2).battery_Name));
                                bitmap = ImageUtils.addImageWatermark(bitmap, coverBitmap, (int)lastBattery.getDstPoints()[0], (int)lastBattery.getDstPoints()[1], 255);
//                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "%", (int)lastBattery.getDstPoints()[8]-20, (int)(lastBattery.getDstPoints()[5]+coverBitmap.getHeight()/2.2f), colorValue);
                            }
                            break;

                        case 3:
                            if(dialShape == FissionConstant.ROUND){
                                customDialModel.setBatteryY(customDialModel.getBatteryY()+30);
                                coverBitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+mCustomWatchFaceInfo.getBatteryList().get(2).battery_Name));
                                bitmap = ImageUtils.addImageWatermark(bitmap, coverBitmap, (int)lastBattery.getDstPoints()[0]-10, (int)lastBattery.getDstPoints()[1]+70, 255);
//                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "%", (int)lastBattery.getDstPoints()[8]+30, (int)(lastBattery.getDstPoints()[7]-90), colorValue);
                            }else{
                                coverBitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(directory+mCustomWatchFaceInfo.getBatteryList().get(2).battery_Name));
                                bitmap = ImageUtils.addImageWatermark(bitmap, coverBitmap, (int)lastBattery.getDstPoints()[0]-coverBitmap.getWidth(), (int)lastBattery.getDstPoints()[1], 255);
//                                bitmap = FissionCustomDialUtil.addCoverText(bitmap, "%", (int)lastBattery.getDstPoints()[8]-20, (int)(lastBattery.getDstPoints()[5])-38, colorValue);
                            }
                            break;
                    }
                }
                if(lastBle!=null){
                    iv_watch_face.addSticker(lastBle);
                    customDialModel.setPreviewImage(pointerBitmap);
                }
                if(lastBt!=null){
                    iv_watch_face.addSticker(lastBt);
                    customDialModel.setPreviewImage(pointerBitmap);
                }
                if(stickerMap.size()>0){
                    iv_watch_face.showAllLabel();
                }

                if(lastPointer!=null && lastDateTime!=null){
                    customDialModel.setDatetimeType(FissionCustomDialUtil.CustomDialModel.DATETIME_TYPE_POINTER_AND_NUMBER);
                }

                customDialModel.setDialShape(dialShape);
                customDialModel.setDialWidth(dialWidth);
                customDialModel.setDialHeight(dialHeight);
                customDialModel.setBackgroundImage(bitmap);
                customDialModel.setDialPosition(1);
                customDialModel.setPreImageWidth(thumbnailWidth);
                customDialModel.setPreImageHeight(thumbnailHigh);
                customDialModel.setDialPosition(3);
                customDialModel.setDialStyleColor(colorValue);
                customDialModel.setFunctionModels(functionModels);
                customDialModel.setCustomDialBinInfo(mCustomDialBinInfo);
                setDiaModelCompress(customDialModel);
            }
        });
    }

    public static Bitmap drawBg4Bitmap(int color, Bitmap originBitmap) {
        Bitmap updateBitmap = Bitmap.createBitmap(originBitmap.getWidth(),
                originBitmap.getHeight(), originBitmap.getConfig());
        Paint paint = new Paint();
        Canvas canvas = new Canvas(updateBitmap);
         ColorMatrix colorMatrix = new ColorMatrix();
         int R = Color.red(color);
        int G = Color.green(color);
        int B = Color.blue(color);
        int A = Color.alpha(color);
//        colorMatrix.setScale(intToHex(R), intToHex(G),intToHex(B),0xff);
        paint.setAntiAlias(true);
        final Matrix matrix = new Matrix();
        canvas.drawBitmap(originBitmap, matrix, paint);
        return updateBitmap;
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


    private void setDiaModelCompress(FissionCustomDialUtil.CustomDialModel dialModel)  {
        try {
            Bitmap bitmap1 = FissionCustomDialUtil.getPreviewImageBitmap(this,dialModel);
            iv_watch_face2.setImageBitmap(bitmap1);
            byte[] resultData = FissionCustomDialUtil.getDiaInfoBinData(this, dialModel);
            byte[] outData = QuickLZUtils.compressFission(resultData);
//        LogUtils.d("wl", "相册自定义表盘字节大小(压缩前)："+resultData.length);
//        String filePath = Environment.getExternalStorageDirectory()+"/custom_dial_1.bin";
//        FileUtils.createFileByDeleteOldFile(filePath);
//        FileIOUtils.writeFileFromBytesByStream (filePath, resultData);
            FissionSdkBleManage.getInstance().startDial(outData, FissionEnum.WRITE_DIAL_DATA_V2);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            ToastUtils.showShort("自定义表盘控件数量超出最大限制");
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    private Bitmap toGray(Bitmap bmpOriginal) {
        int width = bmpOriginal.getWidth();
        int height = bmpOriginal.getHeight();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }




    private File saveFile(Context context, Bitmap body) {
        //把bitmap 转换为byte
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        body.compress(Bitmap.CompressFormat.PNG, 100, arrayOutputStream);
        byte[] bitmapByte = arrayOutputStream.toByteArray();
        File futureStudioIconFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "fission_ota.png");

        InputStream inputStream  = null;
        OutputStream outputStream = null;

        byte[] fileReader         = new byte[4096];
        int   fileSize           = bitmapByte.length;
        long   fileSizeDownloaded = 0;
        try {
//            inputStream = body.getRowBytes();
            int read;
            outputStream = new FileOutputStream(futureStudioIconFile);
//            while ((read = inputStream.read(fileReader)) != -1) {
                outputStream.write(bitmapByte, 0, fileSize);
                //统计这个下载的数量
//            }
            outputStream.flush();
            return futureStudioIconFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    String filePath = "";
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == C.RC_CAMERA) {//【拍照
            Uri file = CameraPhotoHelper.getOutputMediaFileUri(this);
            if (file != null) {
//                filePath = UriUtils.uri2File(file).getAbsolutePath();
//                LogUtils.d("获取路径",filePath);
//                Glide.with(this).load(filePath).into(iv_watch_face);
                CameraPhotoHelper.cropImage(this, file, dialWidth, dialHeight, false);
            }


//            CameraPhotoHelper.cropImage(UserInfoActivity.this, UriUtils.file2Uri(file), 480, 480, true);
        } else if (resultCode == RESULT_OK && requestCode == C.RC_CHOOSE) {//选择照片
            LogUtils.d("获取getData"+data.getData());
            if (data.getData() != null) {
//               File ff =  UriUtils.uri2File(data.getData());
//                LogUtils.d("获取路径",ff.getAbsolutePath());
//                Glide.with(this).load(data.getData()).into(iv_watch_face);
//                saveFile(this,((BitmapDrawable)iv_watch_face.getDrawable()).getBitmap());
                CameraPhotoHelper.cropImage(this, data.getData(), dialWidth, dialHeight, false);
            }
        }else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            Uri bgUri = UCrop.getOutput(data);
            Glide.with(this).load(bgUri).into(iv_watch_face);
            LogUtils.d("clx", "_-------" + bgUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            LogUtils.e("clx", "------裁剪错误" + data);
        }
    }

    public String getPath() {
        File dir = null;
        boolean state = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (state) {
//            if (Build.VERSION.SDK_INT >= 28) {
//                //Android10之后
//                dir = this.getExternalFilesDir(null);
//            } else {
                dir = Environment.getExternalStorageDirectory();
//            }
        } else {
            dir = Environment.getRootDirectory();
        }
        return dir.toString();
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switch (progress){
                case 0:
                    tv_color2.setText("当前选中大字体颜色：白色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_white_big);
                    break;

                case 1:
                    tv_color2.setText("当前选中大字体颜色：绿色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_green_big);
                    break;

                case 2:
                    tv_color2.setText("当前选中大字体颜色：红色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_red_big);
                    break;

                case 3:
                    tv_color2.setText("当前选中大字体颜色：黄色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_yellow_big);
                    break;

                case 4:
                    tv_color2.setText("当前选中大字体颜色：橘红色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_orange_big);
                    break;

                case 5:
                    tv_color2.setText("当前选中大字体颜色：紫色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_purple_big);
                    break;

                case 6:
                    tv_color2.setText("当前选中大字体颜色：天蓝色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_sky_blue_big);
                    break;

                case 7:
                    tv_color2.setText("当前选中大字体颜色：灰色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_gray_big);
                    break;

                case 8:
                    tv_color2.setText("当前选中大字体颜色：浅蓝色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_light_blue_big);
                    break;

                case 9:
                    tv_color2.setText("当前选中大字体颜色：深蓝色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_dark_blue_big);
                    break;
            }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    protected float caculate(int progress) {
        float scale = progress / 128f;
        return scale;
    }

    @Override
    protected void onDestroy() {
        iv_watch_face.removeAllSticker();
        super.onDestroy();
    }

}
