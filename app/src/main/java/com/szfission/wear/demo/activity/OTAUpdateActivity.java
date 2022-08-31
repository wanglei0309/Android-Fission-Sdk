package com.szfission.wear.demo.activity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.realsil.sdk.dfu.model.DfuProgressInfo;
import com.realsil.sdk.dfu.model.OtaDeviceInfo;
import com.realsil.sdk.dfu.model.Throughput;
import com.realsil.sdk.dfu.utils.DfuAdapter;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.util.OtaUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.File;

/**
 * 推送当前歌曲信息
 */
@ContentView(R.layout.activity_ota_update)
public class OTAUpdateActivity extends BaseActivity {

    @ViewInject(R.id.llChooseFile)
    LinearLayout llChooseFile;
    @ViewInject(R.id.tvFile)
    TextView tvFile;
    @ViewInject(R.id.tvProgress)
    ProgressBar tvProgress;
    //    @ViewInject(R.id.btnUpload)
//    Button btnUpload;
//    @ViewInject(R.id.btnStop)
//    Button btnStop;
//    @ViewInject(R.id.tvProgress)
//    TextView tvProgress;
//    @ViewInject(R.id.tvProgressBar)
//    ProgressBar tvProgressBar;
//    @ViewInject(R.id.radioBtn1)
//    RadioButton radioBtn1;
//    @ViewInject(R.id.radioBtn2)
//    RadioButton radioBtn2;
//    @ViewInject(R.id.radioGroup)
//    RadioGroup radioGroup;
    @ViewInject(R.id.btn_send)
    Button btnUpload;
//    @ViewInject(R.id.btnStop)
//    Button btnStop;
    String filePath = "";
    int modelType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.updateota);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
//        showProgress();
        String path = getPath();

        File dir = new File(path);
        LogUtils.d("获取路径",dir.getAbsolutePath());
//        File[] files = dir.listFiles();
//        if (files == null) {
//            return;
//        }
//        FsLogUtil.d("filseisnukk" + files.length);
//        for (File file : files) {
//            String fileName = file.getPath();
//            String fileEnd = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
//            if (fileEnd.equals("bin")) {
//                tvFile.setText(file.getPath());
//                filePath = file.getPath();
//                FsLogUtil.d("获取路径2" + filePath);
//            }
//        }
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

//        btnStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                AnyWear.stopOta();
//                tvProgress.setVisibility(View.INVISIBLE);
//                tvProgressBar.setVisibility(View.INVISIBLE);
//                showToast("取消升级成功");
//            }
//        });
//        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                if (checkedId == radioBtn1.getId()) {
//                    modelType = 0;
//                } else {
//                    modelType = 16;
//                }
//            }
//        });
    }

    private void startOta() {
//        OtaUtil.startOtaProcess(this, BleUtil.bleAddress, 16, filePath, new OnOtaCallBack() {
//            @Override
//            public void setProgress(int progress) {
//                FsLogUtil.d("OTA升级进度"+progress);
//            }
//
//            @Override
//            public void error(String message) {
//                FsLogUtil.d("OTA升级失败"+message);
//            }
//
//            @Override
//            public void prepared(int State, String message) {
//                FsLogUtil.d("OTA升级准备"+State);
//            }
//
//        });

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

    @Event(R.id.btn_send)
    private void send(View v) {
//        filePath = "/storage/emulated/0/DIZO-V0_39-393-20220228.bin";
      if (filePath.equals("")){
          ToastUtils.showShort("还未选择bin文件");
      }else {
//          OtaUtils.startDfu(this,filePath,true);
//          OtaUtils.startDfu(this, filePath, FissionConstant.OTA_TYPE_FIRMWARE);
          FissionSdkBleManage.getInstance().startDfu(this, filePath, FissionConstant.OTA_TYPE_FIRMWARE, new DfuAdapter.DfuHelperCallback() {
              @Override
              public void onStateChanged(int i) {
                  super.onStateChanged(i);
              }

              @Override
              public void onTargetInfoChanged(OtaDeviceInfo otaDeviceInfo) {
                  super.onTargetInfoChanged(otaDeviceInfo);
              }

              @Override
              public void onError(int i, int i1) {
                  super.onError(i, i1);
              }

              @Override
              public void onProcessStateChanged(int i, Throughput throughput) {
                  super.onProcessStateChanged(i, throughput);
              }

              @Override
              public void onProgressChanged(DfuProgressInfo dfuProgressInfo) {
                  super.onProgressChanged(dfuProgressInfo);
              }
          });
      }
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
        LogUtils.d("获取文件路径getData",filePath);

//        }else {
//            LogUtils.d("文件不存在,请重新选择");
//        }
    }


    //获取文件真实路径
    public static String getRealPath(Context context,Uri uri){
        if ( null == uri ) return null;

        final String scheme = uri.getScheme();
        String data = null;

        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }


    /**
     * 接收数据的事件总线
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DataMessageEvent event) {
        LogUtils.d("获取event",event.getMessageType(),"获取event conente"+event.getMessageContent());
        if (event.getMessageType()== ModelConstant.FUNC_OTA){
            LogUtils.d("");
            tvProgress.setProgress(Integer.parseInt(event.getMessageContent()));
        }
    }

}
