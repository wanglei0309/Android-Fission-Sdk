package com.szfission.wear.demo.activity;

import static com.fission.wear.sdk.v2.constant.FissionConstant.COMPRESS_EXERCISE_MORE;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.ClickUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.FssStatus;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.FissionSportsUtil;
import com.fission.wear.sdk.v2.utils.HiSiliconFileTransferUtils;
import com.fission.wear.sdk.v2.utils.QuickLZUtils;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.util.RxTimerUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.util.List;

public class PushMoreGpsDataActivity extends BaseActivity {

    TextView tv_file_directory;

    TextView btn_pack;
    TextView btn_push;

    TextView tv_progress;

    private int type = FissionConstant.OTA_TYPE_AGPS_DATA;

    private List<File> mFileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_more_gps_data);
        String[] testArray = getResources().getStringArray(R.array.haisi_test_array);
        setTitle(testArray[3]);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String directory = getPath()+"/fission_haisi_gps";
        String text = getString(R.string.push_more_gps_tip, directory);
        FileUtils.createOrExistsDir(directory);

        tv_file_directory = findViewById(R.id.tv_file_directory);
        btn_pack = findViewById(R.id.btn_pack);
        btn_push = findViewById(R.id.btn_push);
        tv_progress = findViewById(R.id.tv_progress);

        tv_file_directory.setText(text);

        mFileList = FileUtils.listFilesInDir(directory);

        btn_pack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtils.d("wl", "当前选择文件类型："+type);

            }
        });

        btn_push.setOnClickListener(new ClickUtils.OnDebouncingClickListener() {
            @Override
            public void onDebouncingClick(View v) {
                if(mFileList!= null && mFileList.size()>0){
                    FissionSdkBleManage.getInstance().pushMoreHsAgpsData(mFileList);
                }
            }
        });

        FissionSdkBleManage.getInstance().setHiSiliconFileTransferListener(new HiSiliconFileTransferUtils.HiSiliconFileTransferListener() {
            @Override
            public void onProgressChanged(long curFrames, long framesCount, int fileListIndex, int fileSize) {
                FissionLogUtils.d("hs_ft", "---onProgressChanged---当前传输帧数："+curFrames+", 单文件总帧数："+framesCount+", 当前第"+fileListIndex+"个文件， 总文件数："+fileSize);
            }

            @Override
            public void onComplete() {
                FissionLogUtils.d("hs_ft", "-------onComplete-----");
            }

            @Override
            public void onTimeOut() {
                FissionLogUtils.d("hs_ft", "-------onTimeOut-----");
            }

            @Override
            public void onError(Exception e) {
                FissionLogUtils.d("hs_ft", "-------onError-----"+e.toString());
            }

            @Override
            public void onTransmitting() {
                FissionLogUtils.d("hs_ft", "-------onTransmitting-----文件正在传输，请稍后再试");
            }
        });

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
