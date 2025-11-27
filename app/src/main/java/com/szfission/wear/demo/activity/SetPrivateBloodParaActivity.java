package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.BloodComponentPara;
import com.fission.wear.sdk.v2.bean.BloodPressurePara;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.param.SportsTargetPara;

import java.util.Objects;

public class SetPrivateBloodParaActivity extends BaseActivity {
    EditText et1;
    EditText et2;
    EditText et3;
    EditText et4;
    EditText et5;
    Switch switchOpen;

    Button btn_send, btn_get;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_blood_para);
        setTitle(R.string.FUNC_SET_PRIVATE_BLOOD_SET);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        et1 = findViewById(R.id.et_value1);
        et2 = findViewById(R.id.et_value2);
        et3 = findViewById(R.id.et_value3);
        et4 = findViewById(R.id.et_value4);
        et5 = findViewById(R.id.et_value5);


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
            public void getPrivateBloodComponentMode(BloodComponentPara bloodComponentData) {
                super.getPrivateBloodComponentMode(bloodComponentData);
                dismissProgress();
                showSuccessToast(R.string.FUNC_SET_PRIVATE_BLOOD_SET);
                addLog(R.string.FUNC_SET_PRIVATE_BLOOD_SET, bloodComponentData.toString());
                switchOpen.setChecked(bloodComponentData.isEnable());
                et1.setText(String.valueOf(bloodComponentData.getUricAcid()));
                et2.setText(String.valueOf(bloodComponentData.getTotalCholesterol()));
                et3.setText(String.valueOf(bloodComponentData.getTriglyceride()));
                et4.setText(String.valueOf(bloodComponentData.getHighDensityLipoprotein()));
                et5.setText(String.valueOf(bloodComponentData.getLowDensityLipoprotein()));
            }


            @Override
            public void setPrivateBloodComponentMode() {
                super.setPrivateBloodComponentMode();
                dismissProgress();
                showSuccessToast();
            }

        });
    }

    private void getData() {
        FissionSdkBleManage.getInstance().getPrivateBloodComponentMode();
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
        BloodComponentPara bloodComponentPara = new BloodComponentPara();
        bloodComponentPara.setEnable(switchOpen.isChecked());
        bloodComponentPara.setUricAcid(Integer.parseInt(et1.getText().toString()));
        bloodComponentPara.setTotalCholesterol(Float.parseFloat(et2.getText().toString()));
        bloodComponentPara.setTriglyceride(Float.parseFloat(et3.getText().toString()));
        bloodComponentPara.setHighDensityLipoprotein(Float.parseFloat(et4.getText().toString()));
        bloodComponentPara.setLowDensityLipoprotein(Float.parseFloat(et5.getText().toString()));
        FissionSdkBleManage.getInstance().setPrivateBloodComponentMode(bloodComponentPara);

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
