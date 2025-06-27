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
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.FssStatus;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.utils.CRC32Checksum;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.HiSiliconFileTransferUtils;
import com.realsil.sdk.dfu.DfuConstants;
import com.realsil.sdk.dfu.model.DfuProgressInfo;
import com.realsil.sdk.dfu.model.OtaDeviceInfo;
import com.realsil.sdk.dfu.model.Throughput;
import com.realsil.sdk.dfu.utils.DfuAdapter;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.HardWareInfo;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.RxTimerUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.File;

/**
 * 推送当前歌曲信息
 */
public class OnlineDialPushActivity extends BaseActivity {

    LinearLayout llChooseFile;
    LinearLayout llChooseFile2;
    TextView tvFile;
    TextView tvFile2;
    ProgressBar tvProgress;
    String filePath = "";
    Button btn_re_update, btn_send;
    TextView tv_progress_value;
    TextView tv_tip;

    private int index;
    private int fileIndex;
    private int count;
    String filePath1 = "";
    String filePath2 = "";
    private RxTimerUtil mRxTimerUtil;
    private byte[] dialData1;
    private byte[] dialData2;

    private long lastTime = 0; //上次升级成功的时候， 屏蔽固件重复返回进度100% 引起的逻辑问题。

    private boolean isReUpdate =false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_update_dial);
        setTitle(R.string.FUNC_ONLINE_DIAL_PUSH);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String path = getPath();

        llChooseFile = findViewById(R.id.llChooseFile);
        llChooseFile2 = findViewById(R.id.llChooseFile2);
        tvFile = findViewById(R.id.tvFile);
        tvFile2 = findViewById(R.id.tvFile2);
        tvProgress = findViewById(R.id.tvProgress);
        tv_progress_value = findViewById(R.id.tv_progress_value);
        btn_re_update = findViewById(R.id.btn_re_update);
        btn_send = findViewById(R.id.btn_send);
        tv_tip = findViewById(R.id.tv_tip);

        File dir = new File(path);
        LogUtils.d("获取路径",dir.getAbsolutePath());
        llChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                index = 0;
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

        llChooseFile2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                index = 1;
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

        btn_re_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isReUpdate = true;
                if (filePath1.equals("")){
                    ToastUtils.showShort("表盘1，还未选择bin文件");
                }else if(filePath2.equals("")){
                    ToastUtils.showShort("表盘2，还未选择bin文件");
                }else{
                    fileIndex = 1;
                    if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK || SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
                        FissionSdkBleManage.getInstance().startDial(FileIOUtils.readFile2BytesByStream(filePath1), FissionEnum.WRITE_REMOTE_DIAL_DATA);
                    }else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_HS){
//                        FissionSdkBleManage.getInstance().startDfu(OnlineDialPushActivity.this, filePath1, FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL, null);
                        FissionSdkBleManage.getInstance().pushHiSiOnlineDial(filePath1, FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL, "10001");
                    }
                }
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
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
                try {
                    if(fssStatus.getFssType() == 23){
                        tvProgress.setProgress(fssStatus.getFssStatus());
                        tv_progress_value.setText("当前进度："+fssStatus.getFssStatus()+"%");
                        tv_tip.setText("当前升级的是第"+fileIndex+"个文件, 总升级成功次数："+count);

                        if(fssStatus.getFssStatus() == 100  && System.currentTimeMillis() - lastTime > 2000 && isReUpdate){
                            lastTime = System.currentTimeMillis();
                            count++;
                            if(mRxTimerUtil!=null){
                                mRxTimerUtil.cancelTimer();
                                mRxTimerUtil = null;
                            }
                            mRxTimerUtil =  new RxTimerUtil();
                            mRxTimerUtil.timer(10000, number -> {
                                if(fileIndex == 1){
                                    fileIndex = 2;
                                    FissionLogUtils.d("wl", "11111111111111111");
                                    if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK || SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
                                        FissionSdkBleManage.getInstance().startDial(dialData1, FissionEnum.WRITE_REMOTE_DIAL_DATA);
                                    }else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_HS){
//                                        FissionSdkBleManage.getInstance().startDfu(OnlineDialPushActivity.this, filePath1, FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL, null);
                                        FissionSdkBleManage.getInstance().pushHiSiOnlineDial(filePath1, FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL, "10001");
                                    }
                                }else{
                                    fileIndex = 1;
                                    FissionLogUtils.d("wl", "2222222222222222 ");
                                    if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK || SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
                                        FissionSdkBleManage.getInstance().startDial(dialData2, FissionEnum.WRITE_REMOTE_DIAL_DATA);
                                    }else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_HS){
//                                        FissionSdkBleManage.getInstance().startDfu(OnlineDialPushActivity.this, filePath2, FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL, null);
                                        FissionSdkBleManage.getInstance().pushHiSiOnlineDial(filePath2, FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL, "10001");
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    private void send() {
        isReUpdate = false;
      if (filePath.equals("")){
          ToastUtils.showShort("还未选择bin文件");
      }else {
          byte [] dialData =  FileIOUtils.readFile2BytesByStream(filePath1);
          boolean isValid = false;
          if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
              isValid = CRC32Checksum.checksumOnlineDialDataBy8773(dialData);
              if(!isValid){
                  ToastUtils.showShort("8773表盘文件格式有误，校验失败");
                  return;
              }
          }else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK){
              isValid = CRC32Checksum.checksumOnlineDialData(dialData);
              if(!isValid){
                  ToastUtils.showShort("表盘文件格式有误，校验失败");
                  return;
              }
          }
          FissionLogUtils.d("wl", "在线表盘有效性校验结果："+isValid);
          if(index == 0){
              FissionSdkBleManage.getInstance().startDial(FileIOUtils.readFile2BytesByStream(filePath1), FissionEnum.WRITE_REMOTE_DIAL_DATA);
          }else{
              FissionSdkBleManage.getInstance().startDial(FileIOUtils.readFile2BytesByStream(filePath2), FissionEnum.WRITE_REMOTE_DIAL_DATA);
          }
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
        filePath = UriUtils.uri2File(uri).getAbsolutePath();
        if(!TextUtils.isEmpty(filePath)){
            String prefix = filePath.substring(filePath.lastIndexOf(".")+1);
            LogUtils.d("prefix",prefix);
            LogUtils.d("获取文件路径getData",filePath);
            if ("bin".equals(prefix)){
                if(index == 0){
                    filePath1 = filePath;
                    tvFile.setText(filePath);
                    dialData1 =FileIOUtils.readFile2BytesByStream(filePath1);
                }else{
                    filePath2 = filePath;
                    tvFile2.setText(filePath);
                    dialData2 =FileIOUtils.readFile2BytesByStream(filePath2);
                }
            }else {
                if(index == 0){
                    tvFile.setText("后缀名不对,请重新选择");
                }else{
                    tvFile2.setText("后缀名不对,请重新选择");
                }
            }
        }
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


}
