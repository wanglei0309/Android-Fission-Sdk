package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.HbModelPara;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.HrDetectPara;

import java.util.Objects;

public class SetHbParaActivity extends BaseActivity  {
    EditText etVibrationLevel;
    EditText etDayStartTime;
    EditText etNightStartTime;
    EditText etDayEndTime;
    EditText etNightEndTime;
    Switch switchOpen, switch_open2;

    Button btn_send, btn_get;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hb_para);
        setTitle(R.string.FUNC_HANBAO_MODEL);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        etVibrationLevel = findViewById(R.id.etVibrationLevel);
        etDayStartTime = findViewById(R.id.etDayStartTime);
        etNightStartTime = findViewById(R.id.etNightStartTime);
        etDayEndTime = findViewById(R.id.etDayEndTime);
        etNightEndTime = findViewById(R.id.etNightEndTime);
        switchOpen = findViewById(R.id.switch_open);
        switch_open2 = findViewById(R.id.switch_open2);
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
            public void getHrDetectPara(HrDetectPara hrDetectPara) {
                super.getHrDetectPara(hrDetectPara);
                dismissProgress();
                showSuccessToast(R.string.FUNC_HANBAO_MODEL);
                switchOpen.setChecked(hrDetectPara.isOpen());
            }

            @Override
            public void getHbModelPara(HbModelPara hbModelPara) {
                super.getHbModelPara(hbModelPara);
                dismissProgress();
                showSuccessToast(R.string.FUNC_HANBAO_MODEL);
                switchOpen.setChecked(hbModelPara.isDaySwitch());
                switch_open2.setChecked(hbModelPara.isNightSwitch());
                etVibrationLevel.setText(String.valueOf(hbModelPara.getVibrationLevel()));
                etDayStartTime.setText(String.valueOf(hbModelPara.getDayStartTime()));
                etNightStartTime.setText(String.valueOf(hbModelPara.getNightStartTime()));
                etDayEndTime.setText(String.valueOf(hbModelPara.getDayEndTime()));
                etNightEndTime.setText(String.valueOf(hbModelPara.getNightEndTime()));
            }

            @Override
            public void setHbModelPara() {
                super.setHbModelPara();
                dismissProgress();
                showSuccessToast();
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

    private void get(){
        getData();
        showProgress();
    }

    private void send() {
        String vibrationLevel = etVibrationLevel.getText().toString();
        String dayStartTime = etDayStartTime.getText().toString();
        String nightStartTime = etNightStartTime.getText().toString();
        String dayEndTime = etDayEndTime.getText().toString();
        String nightEndTime = etNightEndTime.getText().toString();
        if (vibrationLevel.isEmpty()) {
            Toast.makeText(this, "请输入震动等级", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dayStartTime.isEmpty()) {
            Toast.makeText(this, "请输入白天模式开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nightStartTime.isEmpty()) {
            Toast.makeText(this, "请输入夜晚模式开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dayEndTime.isEmpty()) {
            Toast.makeText(this, "请输入白天模式结束时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nightEndTime.isEmpty()) {
            Toast.makeText(this, "请输入夜晚模式结束时间", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress();
        HbModelPara hbModelPara = new HbModelPara();
        hbModelPara.setVibrationLevel(Integer.parseInt(vibrationLevel));
        hbModelPara.setDaySwitch(switchOpen.isChecked());
        hbModelPara.setNightSwitch(switch_open2.isChecked());
        hbModelPara.setDayStartTime(Integer.parseInt(dayStartTime));
        hbModelPara.setNightStartTime(Integer.parseInt(nightStartTime));
        hbModelPara.setDayEndTime(Integer.parseInt(dayEndTime));
        hbModelPara.setNightEndTime(Integer.parseInt(nightEndTime));
        FissionSdkBleManage.getInstance().setHbModelPara(hbModelPara);
    }





    private void getData(){
        FissionSdkBleManage.getInstance().getHbModelPara();
    }

}
