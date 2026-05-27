package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.ActionBar;

import com.bigkoo.pickerview.view.TimePickerView;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.BloodComponentPara;
import com.fission.wear.sdk.v2.bean.BloodSugarComponentPara;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.param.SportsTargetPara;

import java.util.Objects;

public class SetPrivateBloodSugarParaActivity extends BaseActivity {
    EditText et1;
    EditText et2;
    EditText et3;
    EditText et4;
    EditText et5;
    EditText et6;

    EditText et_time1;
    EditText et_time2;
    EditText et_time3;
    EditText et_time4;
    EditText et_time5;
    EditText et_time6;

    Switch switchOpen;
    private int timeType;

    private int     preBreakfastTime;
    private int     postBreakfastTime;

    private int     preLunchTime;
    private int     postLunchTime;

    private int     preDinnerTime;
    private int     postDinnerTime;


    Button btn_send, btn_get;
    TimePickerView pvTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_blood_sugar_para);
        setTitle(R.string.FUNC_SET_PRIVATE_BLOOD_SUGAR_SET);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        et1 = findViewById(R.id.et_value1);
        et2 = findViewById(R.id.et_value2);
        et3 = findViewById(R.id.et_value3);
        et4 = findViewById(R.id.et_value4);
        et5 = findViewById(R.id.et_value5);
        et6 = findViewById(R.id.et_value6);

         et_time1 = findViewById(R.id.et_time1);
         et_time2 = findViewById(R.id.et_time2);
         et_time3 = findViewById(R.id.et_time3);
         et_time4 = findViewById(R.id.et_time4);
         et_time5 = findViewById(R.id.et_time5);
         et_time6 = findViewById(R.id.et_time6);



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

            }


            @Override
            public void getPrivateBloodSugarMode(BloodSugarComponentPara bloodSugarComponentPara) {
                super.getPrivateBloodSugarMode(bloodSugarComponentPara);
                dismissProgress();
                showSuccessToast(R.string.FUNC_SET_PRIVATE_BLOOD_SUGAR_SET);
                addLog(R.string.FUNC_SET_PRIVATE_BLOOD_SUGAR_SET, bloodSugarComponentPara.toString());
                switchOpen.setChecked(bloodSugarComponentPara.isEnable());
                et1.setText(String.valueOf(bloodSugarComponentPara.getPreBreakfastGlucose()));
                et2.setText(String.valueOf(bloodSugarComponentPara.getPostBreakfastGlucose()));
                et3.setText(String.valueOf(bloodSugarComponentPara.getPreLunchGlucose()));
                et4.setText(String.valueOf(bloodSugarComponentPara.getPostLunchGlucose()));
                et5.setText(String.valueOf(bloodSugarComponentPara.getPreDinnerGlucose()));
                et6.setText(String.valueOf(bloodSugarComponentPara.getPostDinnerGlucose()));
                et_time1.setText(String.valueOf(bloodSugarComponentPara.getPreBreakfastTime()));
                et_time2.setText(String.valueOf(bloodSugarComponentPara.getPostBreakfastTime()));
                et_time3.setText(String.valueOf(bloodSugarComponentPara.getPreLunchTime()));
                et_time4.setText(String.valueOf(bloodSugarComponentPara.getPostLunchTime()));
                et_time5.setText(String.valueOf(bloodSugarComponentPara.getPreDinnerTime()));
                et_time6.setText(String.valueOf(bloodSugarComponentPara.getPostDinnerTime()));
            }

            @Override
            public void setPrivateBloodSugarMode() {
                super.setPrivateBloodSugarMode();
                dismissProgress();
                showSuccessToast();
            }

        });
    }

    private void getData() {
        FissionSdkBleManage.getInstance().getPrivateBloodSugarMode();
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
        BloodSugarComponentPara bloodSugarComponentPara = new BloodSugarComponentPara();
        bloodSugarComponentPara.setEnable(switchOpen.isChecked());
        bloodSugarComponentPara.setPreBreakfastTime(Integer.parseInt(et_time1.getText().toString()));
        bloodSugarComponentPara.setPreBreakfastGlucose(Float.parseFloat(et1.getText().toString()));
        bloodSugarComponentPara.setPostBreakfastTime(Integer.parseInt(et_time2.getText().toString()));
        bloodSugarComponentPara.setPostBreakfastGlucose(Float.parseFloat(et2.getText().toString()));

        bloodSugarComponentPara.setPreLunchTime(Integer.parseInt(et_time3.getText().toString()));
        bloodSugarComponentPara.setPreLunchGlucose(Float.parseFloat(et3.getText().toString()));
        bloodSugarComponentPara.setPostLunchTime(Integer.parseInt(et_time4.getText().toString()));
        bloodSugarComponentPara.setPostLunchGlucose(Float.parseFloat(et4.getText().toString()));

        bloodSugarComponentPara.setPreDinnerTime(Integer.parseInt(et_time5.getText().toString()));
        bloodSugarComponentPara.setPreDinnerGlucose(Float.parseFloat(et5.getText().toString()));
        bloodSugarComponentPara.setPostDinnerTime(Integer.parseInt(et_time6.getText().toString()));
        bloodSugarComponentPara.setPostDinnerGlucose(Float.parseFloat(et6.getText().toString()));

        FissionSdkBleManage.getInstance().setPrivateBloodSugarMode(bloodSugarComponentPara);

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
