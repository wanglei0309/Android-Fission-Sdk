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
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.SnAndCmeiInfo;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.AppMessageBean;
import com.szfission.wear.sdk.util.FsLogUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

public class SetSnAndCmeiActivity extends BaseActivity {
    EditText ed_sn;

    EditText ed_cmei;

    Button btn_get_code;

    Button btn_set_code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_sn_cmei);
        setTitle(R.string.FUNC_SET_SN_CMEI);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ed_sn = findViewById(R.id.ed_sn);
        ed_cmei = findViewById(R.id.ed_cmei);
        btn_get_code = findViewById(R.id.btn_get_code);
        btn_set_code = findViewById(R.id.btn_set_code);

        init();
    }

    private void init(){
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
            public void getSnAndCmeiCode(SnAndCmeiInfo snAndCmeiInfo) {
                super.getSnAndCmeiCode(snAndCmeiInfo);
                ed_sn.setText(snAndCmeiInfo.getSnCode());
                ed_cmei.setText(snAndCmeiInfo.getCmeiCode());
            }

            @Override
            public void setSnAndCmeiCode() {
                super.setSnAndCmeiCode();
                ToastUtils.showLong("SN/CMEI码设置成功");
            }
        });

        btn_get_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().getSnAndCmeiCode();
            }
        });

        btn_set_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sn = ed_sn.getText().toString().trim();
                String cmei= ed_cmei.getText().toString().trim();
                try {
                    FissionSdkBleManage.getInstance().setSnAndCmeiCode(sn, cmei);
                } catch (Exception e) {
                    ToastUtils.showLong("输入数据格式有误！");
                }
            }
        });
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

}
