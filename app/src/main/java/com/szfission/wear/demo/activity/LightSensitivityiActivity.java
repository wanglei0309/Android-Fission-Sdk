package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.SnAndCmeiInfo;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LightSensitivityiActivity extends BaseActivity {
    EditText ed_ch0;

    EditText ed_ch1;

    EditText ed_standard_ch0;

    EditText ed_standard_ch1;

    Button btn_get;

    Button btn_set;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_light_sensitivity);
        setTitle(R.string.FUNC_GET_LIGHT_SENSITIVITY);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ed_ch0 = findViewById(R.id.ed_ch0);
        ed_ch1 = findViewById(R.id.ed_ch1);
        ed_standard_ch0 = findViewById(R.id.ed_standard_ch0);
        ed_standard_ch1 = findViewById(R.id.ed_standard_ch1);
        btn_get = findViewById(R.id.btn_get);
        btn_set = findViewById(R.id.btn_set);

        init();
    }

    private void init(){
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
            public void setLightSensitivityStandardValue() {
                super.setLightSensitivityStandardValue();
                ToastUtils.showShort("光感目标机校准值设置成功！！");
            }

            @Override
            public void getLightSensitivityStandardValue(int ch0, int ch1) {
                super.getLightSensitivityStandardValue(ch0, ch1);
                ed_ch0.setText(String.valueOf(ch0));
                ed_ch1.setText(String.valueOf(ch1));
            }
        });

        btn_get.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().getLightSensitivityStandardValue();
            }
        });

        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String standard_ch0 = ed_standard_ch0.getText().toString();
                String standard_ch1 = ed_standard_ch1.getText().toString();
                String ch0 = ed_ch0.getText().toString();
                String ch1 = ed_ch1.getText().toString();
                if(TextUtils.isEmpty(standard_ch0) || TextUtils.isEmpty(standard_ch1)){
                    ToastUtils.showShort("金机值不能为空！！");
                    return;
                }
                BigDecimal ch0Val = new BigDecimal(standard_ch0).divide(new BigDecimal(ch0), 3, RoundingMode.HALF_UP);
                BigDecimal ch1Val = new BigDecimal(standard_ch1).divide(new BigDecimal(ch1), 3, RoundingMode.HALF_UP);

// 转为 float（如果你后续还要继续参与计算）
                float diff_ch0 = ch0Val.floatValue();
                float diff_ch1 = ch1Val.floatValue();

                FissionSdkBleManage.getInstance().setLightSensitivityStandardValue(diff_ch0, diff_ch1);
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
