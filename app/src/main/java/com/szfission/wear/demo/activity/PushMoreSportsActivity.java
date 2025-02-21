package com.szfission.wear.demo.activity;

import static com.fission.wear.sdk.v2.constant.FissionConstant.COMPRESS_EXERCISE_MORE;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.ClickUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.FssStatus;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.utils.FissionSportsUtil;
import com.fission.wear.sdk.v2.utils.QuickLZUtils;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.RxTimerUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.util.List;

public class PushMoreSportsActivity extends BaseActivity {

    TextView tv_file_directory;

    TextView btn_pack;
    TextView btn_push;
    TextView btn_push2;
    TextView btn_push_internal;

    TextView tv_progress;

    private int type = FissionConstant.OTA_TYPE_MORE_SPORTS;


    private List<File> mFileList;
    private List<File> mFileList2;
    private byte[]outData;

    private boolean isRePush = false; //是否反复推送

    private RxTimerUtil mRxTimerUtil;

    private int num = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_more_sport);
        setTitle(R.string.FUNC_PUSH_MORE_CUSTOM_SPORT);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String directory = getPath()+"/fission_sports";
        String directory2 = getPath()+"/fission_sports_internal";
        String text = getString(R.string.push_more_sport_tip, directory, directory2);
        FileUtils.createOrExistsDir(directory);
        FileUtils.createOrExistsDir(directory2);

        mFileList = FileUtils.listFilesInDir(directory);
        mFileList2 = FileUtils.listFilesInDir(directory2);

        tv_file_directory = findViewById(R.id.tv_file_directory);
        btn_pack = findViewById(R.id.btn_pack);
        btn_push = findViewById(R.id.btn_push);
        btn_push2 = findViewById(R.id.btn_push2);
        btn_push_internal = findViewById(R.id.btn_push_internal);
        tv_progress = findViewById(R.id.tv_progress);
        btn_push.setText(getString(R.string.FUNC_PUSH_CUSTOM_SPORT)+"(03F6)");
        tv_file_directory.setText(text);

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
                if(fssStatus.getFssType() == 23 && fssStatus.getFssStatus() == 100 && isRePush){
                    num++;
                    if(mRxTimerUtil!=null){
                        mRxTimerUtil.cancelTimer();
                        mRxTimerUtil = null;
                    }
                    mRxTimerUtil =  new RxTimerUtil();
                    mRxTimerUtil.timer(10000, number -> {
                        FissionSdkBleManage.getInstance().pushMoreSport(outData, type);
                    });
                }
                tv_progress.setText("升级进度:"+fssStatus.getFssStatus()+"%"+", 升级成功次数："+num);
            }
        });

        btn_pack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtils.d("wl", "当前选择文件类型："+type);

            }
        });

        btn_push.setOnClickListener(new ClickUtils.OnDebouncingClickListener() {
            @Override
            public void onDebouncingClick(View v) {
                isRePush = false;
                type = FissionConstant.OTA_TYPE_MORE_SPORTS;
                packMultiSportFiles();
                if(outData!=null){
                    FissionSdkBleManage.getInstance().pushMoreSport(outData, type);
                    ToastUtils.showShort("开始推送多运动，文件长度："+outData.length);
                }else{
                    ToastUtils.showShort("多运动数据打包异常");
                }
            }
        });

        btn_push2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRePush = true;
                type = FissionConstant.OTA_TYPE_MORE_SPORTS;
                if(outData == null){
                    packMultiSportFiles();
                }
                if(outData!=null){
                    FissionSdkBleManage.getInstance().pushMoreSport(outData, type);
                    ToastUtils.showShort("开始推送多运动，文件长度："+outData.length);
                }else{
                    ToastUtils.showShort("多运动数据打包异常");
                }
            }
        });

        btn_push_internal.setOnClickListener(new ClickUtils.OnDebouncingClickListener() {
            @Override
            public void onDebouncingClick(View v) {
                type = FissionConstant.OTA_TYPE_MORE_SPORTS_INTERNAL;
                packMultiSportFiles();
                if(outData!=null){
                    FissionSdkBleManage.getInstance().pushMoreSport(outData, type);
                    ToastUtils.showShort("开始推送内置多运动， 文件长度："+outData.length);
                }else{
                    ToastUtils.showShort("多运动数据打包异常");
                }
            }
        });
    }

    private void packMultiSportFiles(){
        if(type == FissionConstant.OTA_TYPE_MORE_SPORTS){
            if (mFileList!=null && mFileList.size()>0){
                ToastUtils.showShort("开始打包运动文件");
                byte[] resultData = FissionSportsUtil.packMultiSportFiles(mFileList, type);
                outData =QuickLZUtils.compressFission(resultData, COMPRESS_EXERCISE_MORE);
//                String filePath = Environment.getExternalStorageDirectory()+"/sport.bin";
//                FileUtils.createFileByDeleteOldFile(filePath);
//                FileIOUtils.writeFileFromBytesByStream (filePath, resultData);
                ToastUtils.showShort("运动文件打包成功");
            }else {
                ToastUtils.showShort("待打包目录没有bin文件");
            }
        }else if(type == FissionConstant.OTA_TYPE_MORE_SPORTS_INTERNAL){
            if (mFileList2!=null && mFileList2.size()>0){
                ToastUtils.showShort("开始打包运动文件");
                byte[] resultData = FissionSportsUtil.packMultiSportFiles(mFileList2, type);
                outData =QuickLZUtils.compressFission(resultData, COMPRESS_EXERCISE_MORE);
//                String filePath = Environment.getExternalStorageDirectory()+"/sport.bin";
//                FileUtils.createFileByDeleteOldFile(filePath);
//                FileIOUtils.writeFileFromBytesByStream (filePath, resultData);
                ToastUtils.showShort("运动文件打包成功");
            }else {
                ToastUtils.showShort("待打包目录没有bin文件");
            }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


}
