package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.AppMessageBean;
import com.szfission.wear.sdk.util.FsLogUtil;
import com.szfission.wear.sdk.util.NumberUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

public class PushAgpsLocationActivity extends BaseActivity {
    EditText etLng;

    EditText etLat;

    Button btn_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_agps_location);
        setTitle(R.string.FUNC_SET_AGPS_LOCATION);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        etLng = findViewById(R.id.etLng);
        etLat = findViewById(R.id.etLat);
        btn_send  = findViewById(R.id.btn_send);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        addCmdResultListener(new FissionBigDataCmdResultListener() {
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
                dismissProgress();
            }

            @Override
            public void setAgpsLocation() {
                super.setAgpsLocation();
                LogUtils.d("wl", "AGPS 辅助定位位置发送成功");
                dismissProgress();
            }
        });

        etLng.setText("114.03015");
        etLat.setText("22.618055");
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
        String lng = etLng.getText().toString();
        String lat = etLat.getText().toString();
        if (lng.isEmpty()) {
            Toast.makeText(this, "经度（longitude）不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lat.isEmpty()) {
            Toast.makeText(this, "纬度（latitude）不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        byte [] data = NumberUtil.double2Bytes(Double.parseDouble(lng));
        float lng1 =  NumberUtil.bytes2Float(data, 0, 4);
        LogUtils.d("wl", "---经度转换--"+lng1);
        showProgress();
        FissionSdkBleManage.getInstance().setAgpsLocation(Double.parseDouble(lng), Double.parseDouble(lat));
    }
}
