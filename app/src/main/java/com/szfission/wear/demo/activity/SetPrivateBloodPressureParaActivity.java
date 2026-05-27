package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.BloodPressurePara;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.param.SportsTargetPara;

import java.util.Objects;

public class SetPrivateBloodPressureParaActivity extends BaseActivity {
    EditText etMaxBp;
    EditText etMinBp;
    Switch switchOpen;

    Button btn_send, btn_get;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_blood_pressure_para);
        setTitle(R.string.FUNC_SET_PRIVATE_BLOOD_PRESSURE_SET);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        etMaxBp = findViewById(R.id.etMaxBp);
        etMinBp = findViewById(R.id.etMinBp);
        switchOpen = findViewById(R.id.switch_open);
        btn_send = findViewById(R.id.btn_send);
        btn_get = findViewById(R.id.btn_get);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        btn_get.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                get();
            }
        });

        getData();
        showProgress();

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
                showToast(errorMsg);
                dismissProgress();
            }

            @Override
            public void getTargetSet(SportsTargetPara sportsTargetPara) {
                super.getTargetSet(sportsTargetPara);
            }

            @Override
            public void getPrivateBloodPressureMode(BloodPressurePara bloodPressurePara) {
                super.getPrivateBloodPressureMode(bloodPressurePara);
                dismissProgress();
                showSuccessToast(R.string.FUNC_SET_PRIVATE_BLOOD_PRESSURE_SET);
                addLog(R.string.FUNC_SET_PRIVATE_BLOOD_PRESSURE_SET, bloodPressurePara.toString());
                switchOpen.setChecked(bloodPressurePara.isEnable());
                etMaxBp.setText(String.valueOf(bloodPressurePara.getSystolicPressure()));
                etMinBp.setText(String.valueOf(bloodPressurePara.getDiastolicPressure()));
            }

            @Override
            public void setPrivateBloodPressureMode() {
                super.setPrivateBloodPressureMode();
                dismissProgress();
                showSuccessToast();
            }

        });
    }

    private void getData() {
        FissionSdkBleManage.getInstance().getPrivateBloodPressureMode();
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

    private void get() {
        getData();
        showProgress();
    }

    private void send() {
//        if (startTime.isEmpty()) {
//            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (endTime.isEmpty()) {
//            Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
//            return;
//        }
        showProgress();
        BloodPressurePara bloodPressurePara = new BloodPressurePara();
        bloodPressurePara.setEnable(switchOpen.isChecked());
        bloodPressurePara.setSystolicPressure(Integer.parseInt(etMaxBp.getText().toString()));
        bloodPressurePara.setDiastolicPressure(Integer.parseInt(etMinBp.getText().toString()));
        FissionSdkBleManage.getInstance().setPrivateBloodPressureMode(bloodPressurePara);

//        AnyWear.setTargetSet(sportsTargetPara, new OnSmallDataCallback() {
//                    @Override
//                    public void OnError(String msg) {
//                        showToast(msg);
//                        dismissProgress();
//                    }
//
//
//                    @Override
//                    public void OnEmptyResult() {
//                        addLog(R.string.FUNC_SET_TARGET_SET, "设置成功");
//                        dismissProgress();
//                        showSuccessToast();
//                    }
//                });
    }


}
