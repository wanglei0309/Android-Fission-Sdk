package com.szfission.wear.demo.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.DeviceBattery;
import com.fission.wear.sdk.v2.bean.DeviceVersion;
import com.fission.wear.sdk.v2.bean.MusicConfig;
import com.fission.wear.sdk.v2.callback.BaseCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.szfission.wear.demo.App;

import org.greenrobot.eventbus.EventBus;
import org.xutils.common.util.LogUtil;
import org.xutils.x;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class BaseActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;
    private Timer refreshLogTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        x.view().inject(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("加载中...");


        // 初始化事件总线
        if (useEventBus() && !EventBus.getDefault().isRegistered(this)) {
            LogUtil.d("---------------注册EventBus" + this);
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void showProgress(){
        if(progressDialog != null){
            progressDialog.show();
        }
    }

    protected void dismissProgress(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    protected void showSuccessToast() {
        Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
    }

    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showSuccessToast(int type) {
        ToastUtils.showShort("成功:"+getString(type) );
    }

    protected void showFailToast(final int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.showShort("失败，errorCode:" + errorCode);
            }
        });
    }

    public void addLog(int type, String result) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        addLog(getString(type) + " " + sdf.format(new Date()) + "\n" + result + "\n");
    }

    protected  synchronized void addLog(String content) {
        App.logData.add(content);
        App.logSingleData.add(content);
        if(refreshLogTimer == null){
            refreshLogTimer = new Timer();
            refreshLogTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendBroadcast(new Intent(LogActivity.ACTION_REFRESH));
                    refreshLogTimer = null;
                }
            },100);
        }
    }

    /**
     * 事件总线
     */
    protected boolean useEventBus() {
        return false;
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(progressDialog != null){
            progressDialog.dismiss();
            progressDialog = null;
        }
        if (useEventBus()) {
            LogUtil.d("---------------注销EventBus");
            EventBus.getDefault().unregister(this);
        }
    }

    public void addCmdResultListener(BaseCmdResultListener listener){
        FissionSdkBleManage.getInstance().addCmdResultListener(listener);
    }

    public void removeCmdResultListener(BaseCmdResultListener listener){
        FissionSdkBleManage.getInstance().removeCmdResultListener(listener);
    }
}
