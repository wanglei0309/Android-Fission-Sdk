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
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;

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
public class WriteFlashDataActivity extends BaseActivity {

    @ViewInject(R.id.llChooseFile)
    LinearLayout llChooseFile;
    @ViewInject(R.id.tvFile)
    TextView tvFile;
    @ViewInject(R.id.tvProgress)
    ProgressBar tvProgress;
    @ViewInject(R.id.btn_send)
    String filePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.updateota);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String path = getPath();

        File dir = new File(path);
        LogUtils.d("获取路径",dir.getAbsolutePath());
        llChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
               Uri uri = Uri.parse(path);
                intent.setDataAndType(uri, "*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
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

    @Event(R.id.btn_send)
    private void send(View v) {
      if (filePath.equals("")){
          ToastUtils.showShort("还未选择bin文件");
      }else {
          String address = filePath.substring(filePath.length()-12, filePath.length()-4);
          LogUtils.d("wl", "flash data 文件地址截取:"+address);
          FissionSdkBleManage.getInstance().startOtaUi(FileIOUtils.readFile2BytesByStream(filePath),address);
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
                tvFile.setText(filePath);
            }else {
                tvFile.setText("后缀名不对,请重新选择");
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
