package com.szfission.wear.demo.activity;


import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.UriUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.FssStatus;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.FissionSdk;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.FissionDialUtil;
import com.szfission.wear.sdk.util.RxTimerUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.File;

@ContentView(R.layout.activity_push_sport)
public class PushSportModeActivity extends BaseActivity{
    private RxPermissions rxPermissions;
    @ViewInject(R.id.pushProgress)
    TextView pushProgress;
    @ViewInject(R.id.pushSport)
    Button pushSport;
    @ViewInject(R.id.pushSport2)
    Button pushSport2;
    @ViewInject(R.id.spinnerType)
    Spinner spinner;

    @ViewInject(R.id.llChooseFile)
    LinearLayout llChooseFile;
    @ViewInject(R.id.tvFile)
    TextView tvFile;

    String filePath = "";

    byte[] resultData = null;

    private boolean isRePush = false; //是否反复推送

    private RxTimerUtil mRxTimerUtil;

    private int num = 0;

    private long lastTime = 0; //上次升级成功的时候， 屏蔽固件重复返回进度100% 引起的逻辑问题。
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        setTitle(R.string.FUNC_PUSH_CUSTOM_SPORT);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);

        String path = getPath();

        File dir = new File(path);
        LogUtils.d("获取路径",dir.getAbsolutePath());

        String[] mItems = getResources().getStringArray(R.array.sport);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mItems);
        spinner.setAdapter(adapter);

        pushSport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRePush = false;
                int type = spinner.getSelectedItemPosition();
                if(TextUtils.isEmpty(filePath)){
                    if (type == 0){
                        setDiaModel("29.bin");
                    }else if (type == 1){
                        setDiaModel("35.bin");
                    }else if (type == 2){
                        setDiaModel("105.bin");
                    }else if (type == 3){
                        setDiaModel("106.bin");
                    }else if (type == 4){
                        setDiaModel("107.bin");
                    }else if (type == 5){
                        setDiaModel("125.bin");
                    }
                }else{
                    FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_SPORT_DATA);
                }
            }
        });

        pushSport2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRePush = true;
                if(!TextUtils.isEmpty(filePath)){
                    FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_SPORT_DATA);
                }else{
                    Toast.makeText(PushSportModeActivity.this, "请选择一个有效的运动文件", Toast.LENGTH_SHORT).show();
                }
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
            public void fssSuccess(FssStatus fssStatus) {
                super.fssSuccess(fssStatus);
                if(fssStatus.getFssType() == 23 && fssStatus.getFssStatus() == 100 && isRePush && System.currentTimeMillis() - lastTime > 2000){
                    lastTime = System.currentTimeMillis();
                    num++;
                    if(mRxTimerUtil!=null){
                        mRxTimerUtil.cancelTimer();
                        mRxTimerUtil = null;
                    }
                    mRxTimerUtil =  new RxTimerUtil();
                    mRxTimerUtil.timer(10000, number -> {
                        FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_SPORT_DATA);
                    });
                }
                pushProgress.setText("升级进度:"+fssStatus.getFssStatus()+"%"+", 升级成功次数："+num);
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
            }
        });

        llChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
    }

    @Override
    protected boolean useEventBus() {
        return true;
    }

    private void setDiaModel(String name)  {
        byte[] resultData =  FissionDialUtil.inputBin(this,name);
        FissionSdk.getInstance().startDial(resultData, FissionEnum.WRITE_SPORT_DATA);
        FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_SPORT_DATA);
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

    /**
     * 接收数据的事件总线
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DataMessageEvent event) {
        pushProgress.setText("升级进度:"+event.getMessageContent());
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
            tvFile.setText(split[split.length-1]);
            name = split[split.length-1];
        }else {
            tvFile.setText("后缀名不对,请重新选择");
        }
//        File file = new File();
//        if (file.exists()) {
        String treePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/"+ AppUtils.getAppPackageName()+"/files/Download"+"/";
//        filePath =getPath()+File.separator+name;
        filePath = UriUtils.uri2File(uri).getAbsolutePath();
        resultData = FileIOUtils.readFile2BytesByStream(filePath);
        LogUtils.d("获取文件路径getData",filePath);

//        }else {
//            LogUtils.d("文件不存在,请重新选择");
//        }
    }
}
