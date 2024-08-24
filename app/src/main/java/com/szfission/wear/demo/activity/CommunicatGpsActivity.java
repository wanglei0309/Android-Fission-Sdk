package com.szfission.wear.demo.activity;


import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.ControlGpsSportInfo;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.FissionSdk;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.param.CommunicatGps;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;
import com.szfission.wear.sdk.util.FissionDialUtil;
import com.szfission.wear.sdk.util.RxTimerUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

public class CommunicatGpsActivity extends BaseActivity{
    Button startSport;
    Button pauseSport;
    Button continueSport;
    Button stopSport;
    Button pushSport;
    Button getSportState;
    TextView tv_result;
    Button autoPushSport;
    Spinner spinner;
    long curGpsTime = System.currentTimeMillis()/1000;

    long startTime = 0;
    int duration = 0;

    private CommunicatGps mCommunicatGps;

    private RxTimerUtil mRxTimerUtil;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        setContentView(R.layout.activity_push_gps);
        setTitle(R.string.FUNC_GPS_SPORT_CMD);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);

        startSport = findViewById(R.id.startSport);
        pauseSport = findViewById(R.id.pauseSport);
        continueSport = findViewById(R.id.continueSport);
        stopSport = findViewById(R.id.stopSport);
        pushSport = findViewById(R.id.pushSport);
        getSportState = findViewById(R.id.getSportState);
        tv_result = findViewById(R.id.tv_result);
        autoPushSport = findViewById(R.id.autoPushSport);
        spinner = findViewById(R.id.spinnerType);

        String[] mItems = getResources().getStringArray(R.array.sport);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mItems);
        spinner.setAdapter(adapter);

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
            public void sendGpsCommand(CommunicatGps communicatGps) {
                super.sendGpsCommand(communicatGps);
                mCommunicatGps.setWatchMaxHr(communicatGps.getWatchMaxHr());
                mCommunicatGps.setWatchMinHr(communicatGps.getWatchMinHr());
                mCommunicatGps.setWatchRealAvgHr(communicatGps.getWatchRealAvgHr());
                mCommunicatGps.setWatchRealHr(communicatGps.getWatchRealHr());
                LogUtils.d("wl", "GPS互联运动交互数据："+mCommunicatGps.toString());
                tv_result.setText(mCommunicatGps.toString());
            }
        });

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionAtCmdResultListener() {
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
            public void controlGpsSportStatus(ControlGpsSportInfo controlGpsSportInfo) {
                super.controlGpsSportStatus(controlGpsSportInfo);
                LogUtils.d("wl", "当前运动状态："+controlGpsSportInfo.getSportState());
            }

            @Override
            public void replyControlGpsSportResult(ControlGpsSportInfo controlGpsSportInfo) {
                super.replyControlGpsSportResult(controlGpsSportInfo);
                LogUtils.d("wl", "设备端控制运动状态："+controlGpsSportInfo.getSportState());
                FissionSdkBleManage.getInstance().replyControlGpsSportResult(controlGpsSportInfo.getSportType(), controlGpsSportInfo.getSportState(), FissionConstant.GPS_SPORT_RESULT_NORMAL_EXECUTION, controlGpsSportInfo.getDuration());
            }

            @Override
            public void getSportState(int state) {
                super.getSportState(state);
                LogUtils.d("wl", "获取当前运动状态："+state);
                tv_result.setText("获取当前运动状态："+state);
            }
        });

        startSport.setOnClickListener(v -> {
//            AnyWear.controlGpsStatus(8,1,new OnSmallDataCallback(){
//                @Override
//                public void OnStringResult(String s) {
//                    LogUtils.d("开启运动状态"+s);
//                }
//            });
            FissionSdkBleManage.getInstance().controlGpsSportStatus(15, FissionConstant.GPS_SPORT_START, 0);
            startTime = System.currentTimeMillis();
        });

        pauseSport.setOnClickListener(v -> {
//            AnyWear.controlGpsStatus(0,0,new OnSmallDataCallback(){
//                @Override
//                public void OnStringResult(String s) {
//                    LogUtils.d("开启运动状态"+s);
//                }
//            });
            duration = (int)(System.currentTimeMillis()-startTime)/1000;
            FissionSdkBleManage.getInstance().controlGpsSportStatus(15, FissionConstant.GPS_SPORT_PAUSE, duration);
        });

        continueSport.setOnClickListener(v -> {
            FissionSdkBleManage.getInstance().controlGpsSportStatus(15, FissionConstant.GPS_SPORT_CONTINUE, duration);
            startTime = System.currentTimeMillis();
        });

        stopSport.setOnClickListener(v -> {
            if(mRxTimerUtil!=null){
                mRxTimerUtil.cancelTimer();
                mRxTimerUtil =null;
            }
            duration = duration+(int)(System.currentTimeMillis()-startTime)/1000;
            FissionSdkBleManage.getInstance().controlGpsSportStatus(15, FissionConstant.GPS_SPORT_STOP, duration);
        });

        pushSport.setOnClickListener(v -> {
            mCommunicatGps = new CommunicatGps();
            mCommunicatGps.setUtcTime(System.currentTimeMillis() / 1000);
            mCommunicatGps.setSportId(curGpsTime);
            mCommunicatGps.setStartUtc(curGpsTime);
            mCommunicatGps.setTotalCalorie(15);
            mCommunicatGps.setTotalStep(200);
            mCommunicatGps.setTotalTime(10);
            mCommunicatGps.setCurDistance(300);
            mCommunicatGps.setSportType(15);
            mCommunicatGps.setSportStatus(1);
            mCommunicatGps.setMaxCadence(120);
            mCommunicatGps.setAvgCadence(90);
            mCommunicatGps.setResetCount(0);
            mCommunicatGps.setCurPace(300);

//            AnyWear.sendGpsCommand(communicatGps,new BigDataCallBack(){
//                @Override
//                public void OnCommunicatGpsData(CommunicatGps communicatGps) {
//                }
//            });
            FissionSdkBleManage.getInstance().sendGpsCommand(mCommunicatGps);
        });
        getSportState.setOnClickListener(v -> {
            FissionSdkBleManage.getInstance().getSportState();
        });

        autoPushSport.setOnClickListener(v -> {
            if(mRxTimerUtil == null){
                mRxTimerUtil =  new RxTimerUtil();
                mRxTimerUtil.interval(1000, new RxTimerUtil.RxAction() {
                    @Override
                    public void action(long number) {
                        mCommunicatGps = new CommunicatGps();
                        mCommunicatGps.setUtcTime(System.currentTimeMillis() / 1000);
                        mCommunicatGps.setSportId(curGpsTime);
                        mCommunicatGps.setStartUtc(curGpsTime);
                        mCommunicatGps.setTotalCalorie(15);
                        mCommunicatGps.setTotalStep(200);
                        mCommunicatGps.setTotalTime(0);
                        mCommunicatGps.setCurDistance(300);
                        mCommunicatGps.setSportType(15);
                        mCommunicatGps.setSportStatus(1);
                        mCommunicatGps.setMaxCadence(120);
                        mCommunicatGps.setAvgCadence(90);
                        mCommunicatGps.setResetCount(0);
                        mCommunicatGps.setCurPace(300);
                        FissionSdkBleManage.getInstance().sendGpsCommand(mCommunicatGps);
                    }
                });
            }
        });
    }

    @Override
    protected boolean useEventBus() {
        return true;
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mRxTimerUtil!=null){
            mRxTimerUtil.cancelTimer();
            mRxTimerUtil = null;
        }
    }
}
