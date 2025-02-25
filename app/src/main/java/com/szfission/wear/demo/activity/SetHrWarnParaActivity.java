package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.HrWarnPara;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Objects;

public class SetHrWarnParaActivity extends BaseActivity {
    EditText etWeekTime;
    EditText etStartTime;
    EditText etEndTime;
    EditText etMaxHr;
    EditText etMinHr;
    Switch switchOpen;

    Button btn_send, btn_get;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hr_check_para);
        setTitle(R.string.FUNC_SET_HR_WARN_PARA);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        etWeekTime = findViewById(R.id.etWeekTime);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etMaxHr = findViewById(R.id.etMaxHr);
        etMinHr = findViewById(R.id.etMinHr);
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
        String startTime = etStartTime.getText().toString();
        String endTime = etEndTime.getText().toString();
        String weekTime = etWeekTime.getText().toString();
        String maxHr = etMaxHr.getText().toString();
        String minHr = etMinHr.getText().toString();
        if (startTime.isEmpty()) {
            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endTime.isEmpty()) {
            Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (weekTime.isEmpty()) {
            Toast.makeText(this, "请输入提醒周期时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (maxHr.isEmpty()) {
            Toast.makeText(this, "请输入最大心率预警", Toast.LENGTH_SHORT).show();
            return;
        }
        if (minHr.isEmpty()) {
            Toast.makeText(this, "请输入最小心率预警", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress();
        HrWarnPara hrWarnPara = new HrWarnPara();
        hrWarnPara.setMainSwitch(switchOpen.isChecked());
        hrWarnPara.setMaxHrWarn(Integer.parseInt(maxHr));
        hrWarnPara.setMinHrWarn(Integer.parseInt(minHr));
        hrWarnPara.setLimitTime(Integer.parseInt(weekTime));
        hrWarnPara.setStartTime(Integer.parseInt(startTime));
        hrWarnPara.setEndTime(Integer.parseInt(endTime));
        FissionSdkBleManage.getInstance().setHrWarnPara(hrWarnPara);
    }


    private void getData() {
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
            public void getHrWarnPara(HrWarnPara hrWarnPara) {
                super.getHrWarnPara(hrWarnPara);
                dismissProgress();
                showSuccessToast(R.string.FUNC_SET_HR_WARN_PARA);
                StringBuilder content = new StringBuilder();
                switchOpen.setChecked(hrWarnPara.isMainSwitch());
                etStartTime.setText(String.valueOf(hrWarnPara.getStartTime()));
                etEndTime.setText(String.valueOf(hrWarnPara.getEndTime()));
                etMaxHr.setText(String.valueOf(hrWarnPara.getMaxHrWarn()));
                etMinHr.setText(String.valueOf(hrWarnPara.getMinHrWarn()));
                etWeekTime.setText(String.valueOf(hrWarnPara.getLimitTime()));
            }

            @Override
            public void setHrWarnPara() {
                super.setHrWarnPara();
                dismissProgress();
                showSuccessToast();
            }
        });
        FissionSdkBleManage.getInstance().getHrWarnPara();

//        AnyWear.getHrWarnPara(new BigDataCallBack() {
//            @Override
//            public void OnEmpty(String cmdId) {
//                dismissProgress();
//            }
//
//            @Override
//            public void OnError(String msg) {
//                LogUtils.d("错误提醒"+msg);
//                showToast(msg);
//                dismissProgress();
//            }
//
//            @Override
//            public void OnHrWarnPara(HrWarnPara hrWarnPara) {
//                dismissProgress();
//                showSuccessToast(R.string.FUNC_SET_HR_WARN_PARA);
//                StringBuilder content = new StringBuilder();
////                content.append("\n心率异常提醒总开关：").append(hrWarnPara.isMainSwitch());
////                content.append("\n心率系：").append(hrDetectPara.getStartTime());
////                content.append("\n心率检测结束时间：").append(hrDetectPara.getEndTime());
////                content.append("\n心率检测周期：").append(hrDetectPara.getWeek());
////                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
//                switchOpen.setChecked(hrWarnPara.isMainSwitch());
//                etStartTime.setText(String.valueOf(hrWarnPara.getStartTime()));
//                etEndTime.setText(String.valueOf(hrWarnPara.getEndTime()));
////                etWeekTime.setText(String.valueOf(hrWarnPara.getWeek()));
//            }
//        });
    }

}
