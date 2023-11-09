package com.szfission.wear.demo.activity;


import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.UriUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.szfission.wear.demo.App;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.File;


@ContentView(R.layout.activity_get_flash_data)
public class GetFlashDataActivity extends BaseActivity{
    @ViewInject(R.id.ed_data1)
    EditText ed_data1;
    @ViewInject(R.id.ed_data2)
    EditText ed_data2;
    @ViewInject(R.id.btn_get)
    Button btn_get;
    @ViewInject(R.id.tv_progress)
    TextView tv_progress;
    @ViewInject(R.id.radio1)
    RadioButton radio1;
    @ViewInject(R.id.radio2)
    RadioButton radio2;


    private int currentNum =0;
    private int count =0;

    private int hfOffsetAddress; //Hardfault 偏移地址
    private int hfLength;
    private int spOffsetAddress; //系统参数 偏移地址
    private int spLength;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        setTitle(R.string.FUNC_GET_FLASH_DATA);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);

        if(App.mHardWareInfo!=null){
            hfOffsetAddress = App.mHardWareInfo.getHfOffsetAddress();
            hfLength = App.mHardWareInfo.getHfLength();
            spOffsetAddress = App.mHardWareInfo.getSpOffsetAddress();
            spLength = App.mHardWareInfo.getSpLength();
        }

        ed_data1.setText(SPUtils.getInstance().getString("flash_address"));
        ed_data2.setText(SPUtils.getInstance().getString("flash_length"));

        btn_get.setOnClickListener(v -> {
            currentNum = 0;
            String address = ed_data1.getText().toString();
            String length = ed_data2.getText().toString();
            if(TextUtils.isEmpty(address) || TextUtils.isEmpty(length)){
                Toast.makeText(this, "请输入完整信息！", Toast.LENGTH_SHORT).show();
                return;
            }
            String filePath = Environment.getExternalStorageDirectory()+"/fission-flash-data.txt";
            if(FileUtils.isFileExists(filePath)){
                FileUtils.delete(filePath);
            }
            if(Integer.parseInt(length.substring(2), 16) % 2048 != 0){
                count = Integer.parseInt(length.substring(2), 16) / 2048 + 1;
            }else{
                count = Integer.parseInt(length.substring(2),16) / 2048;
            }
            tv_progress.setText(getString(R.string.get_progress)+currentNum+"/"+count);
            FissionSdkBleManage.getInstance().getFlashData(address.substring(2), length.substring(2));
            SPUtils.getInstance().put("flash_address", address);
            SPUtils.getInstance().put("flash_length", length);
            Toast.makeText(this, "读取flash指定地址数据指令已发送！", Toast.LENGTH_SHORT).show();
        });

        radio1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    SPUtils.getInstance().put("flash_address", "0x"+Integer.toHexString(hfOffsetAddress));
                    SPUtils.getInstance().put("flash_length", "0x"+Integer.toHexString(hfLength));
                    ed_data1.setText(SPUtils.getInstance().getString("flash_address"));
                    ed_data2.setText(SPUtils.getInstance().getString("flash_length"));
                }
            }
        });

        radio2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    SPUtils.getInstance().put("flash_address", "0x"+Integer.toHexString(spOffsetAddress));
                    SPUtils.getInstance().put("flash_length", "0x"+Integer.toHexString(spLength));
                    ed_data1.setText(SPUtils.getInstance().getString("flash_address"));
                    ed_data2.setText(SPUtils.getInstance().getString("flash_length"));
                }
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
            public void getFlashData(String filepath) {
                super.getFlashData(filepath);
                currentNum++;
                tv_progress.setText(getString(R.string.get_progress)+currentNum+"/"+count);
//                if(currentNum == count){
//                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                    intent.setData(UriUtils.file2Uri(new File(filepath)));
//                    intent.addCategory(Intent.CATEGORY_OPENABLE);
//                    startActivityForResult(intent,200);
//                }
                Toast.makeText(GetFlashDataActivity.this, "数据已保存："+filepath, Toast.LENGTH_LONG).show();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DataMessageEvent event) {
    }


}
