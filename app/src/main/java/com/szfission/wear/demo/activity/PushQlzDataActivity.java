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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.utils.QuickLZUtils;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.util.List;

@ContentView(R.layout.activity_push_qlz_data)
public class PushQlzDataActivity extends BaseActivity {

    @ViewInject(R.id.llChooseFile)
    LinearLayout llChooseFile;
    @ViewInject(R.id.tvFile)
    TextView tvFile;
    @ViewInject(R.id.tvProgress)
    ProgressBar tvProgress;
    @ViewInject(R.id.btn_send)
    Button btn_send;
    String filePath = "";

    @ViewInject(R.id.radio_ui)
    RadioButton radio_ui;
    @ViewInject(R.id.radio_font)
    RadioButton radio_font;
    @ViewInject(R.id.radio_dial)
    RadioButton radio_dial;
    @ViewInject(R.id.radio_small_font)
    RadioButton radio_small_font;
    @ViewInject(R.id.radio_more_dial)
    RadioButton radio_more_dial;
    @ViewInject(R.id.radio_more_sport)
    RadioButton radio_more_sport;

    private int type = FissionConstant.OTA_TYPE_UI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_COMPRESS_CMD);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String path = getPath();

        File dir = new File(path);
        LogUtils.d("获取路径",dir.getAbsolutePath());
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

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtils.d("wl", "当前选择文件类型："+type);
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择bin文件");
                }else {
                    byte[] resultData = FileIOUtils.readFile2BytesByStream(filePath);
                    List<byte[]> outData =QuickLZUtils.splitQlzData(resultData);
                    FissionSdkBleManage.getInstance().startWriteQlzData(outData, type);
                }
            }
        });

        initRadioButton();

    }

    private void initRadioButton(){
        radio_ui.setChecked(true);
        type = FissionConstant.OTA_TYPE_UI;
        radio_ui.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    type = FissionConstant.OTA_TYPE_UI;
                }
            }
        });

        radio_font.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    type = FissionConstant.OTA_TYPE_LARGE_FONT;
                }
            }
        });

        radio_dial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    type = FissionConstant.OTA_TYPE_MORE_DIAL;
                }
            }
        });

        radio_small_font.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    type = FissionConstant.OTA_TYPE_SMALL_FONT;
                }
            }
        });

        radio_more_dial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    type = FissionConstant.OTA_TYPE_MORE_DIAL;
                }
            }
        });

        radio_more_sport.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    type = FissionConstant.OTA_TYPE_MORE_SPORTS;
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
