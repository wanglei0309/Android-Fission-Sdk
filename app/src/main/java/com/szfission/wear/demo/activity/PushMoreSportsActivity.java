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

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.utils.FissionSportsUtil;
import com.fission.wear.sdk.v2.utils.QuickLZUtils;
import com.szfission.wear.demo.R;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.util.List;

@ContentView(R.layout.activity_push_more_sport)
public class PushMoreSportsActivity extends BaseActivity {

    @ViewInject(R.id.tv_file_directory)
    TextView tv_file_directory;

    @ViewInject(R.id.btn_pack)
    TextView btn_pack;
    @ViewInject(R.id.btn_push)
    TextView btn_push;

    private int type = FissionConstant.OTA_TYPE_MORE_SPORTS;


    private List<File> mFileList;
    private byte[]outData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        btn_push.setText(getString(R.string.FUNC_PUSH_CUSTOM_SPORT)+"(03F6)");
        setTitle(R.string.FUNC_PUSH_MORE_CUSTOM_SPORT);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String directory = getPath()+"/fission_sports";
        String text = getString(R.string.push_more_sport_tip, directory);
        tv_file_directory.setText(text);
        FileUtils.createOrExistsDir(directory);

        mFileList = FileUtils.listFilesInDir(directory);

        btn_pack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtils.d("wl", "当前选择文件类型："+type);
                if (mFileList!=null && mFileList.size()>0){
                    ToastUtils.showShort("开始打包运动文件");
                    byte[] resultData = FissionSportsUtil.packMultiSportFiles(mFileList);
                    outData =QuickLZUtils.compressFission(resultData, COMPRESS_EXERCISE_MORE);
                    ToastUtils.showShort("运动文件打包成功");
                }else {
                    ToastUtils.showShort("待打包目录没有bin文件");
                }
            }
        });

        btn_push.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(outData!=null){
                    FissionSdkBleManage.getInstance().pushMoreSport(outData);
                    ToastUtils.showShort("开始推送多运动");
                }else{
                    ToastUtils.showShort("多运动数据打包异常");
                }
            }
        });

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
