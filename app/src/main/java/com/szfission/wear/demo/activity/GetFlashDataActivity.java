package com.szfission.wear.demo.activity;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.SPUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;


@ContentView(R.layout.activity_get_flash_data)
public class GetFlashDataActivity extends BaseActivity{
    @ViewInject(R.id.ed_data1)
    EditText ed_data1;
    @ViewInject(R.id.ed_data2)
    EditText ed_data2;
    @ViewInject(R.id.btn_get)
    Button btn_get;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        setTitle(R.string.FUNC_GET_FLASH_DATA);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);

        ed_data1.setText(SPUtils.getInstance().getString("flash_address"));
        ed_data2.setText(SPUtils.getInstance().getString("flash_length"));

        btn_get.setOnClickListener(v -> {
            String address = ed_data1.getText().toString();
            String length = ed_data2.getText().toString();
            if(TextUtils.isEmpty(address) || TextUtils.isEmpty(length)){
                Toast.makeText(this, "请输入完整信息！", Toast.LENGTH_SHORT).show();
                return;
            }
            FissionSdkBleManage.getInstance().getFlashData(address.substring(2), length.substring(2));
            SPUtils.getInstance().put("flash_address", address);
            SPUtils.getInstance().put("flash_length", length);
            Toast.makeText(this, "读取flash指定地址数据指令已发送！", Toast.LENGTH_SHORT).show();
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
