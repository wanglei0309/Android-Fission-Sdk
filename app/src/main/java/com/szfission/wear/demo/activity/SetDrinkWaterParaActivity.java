package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.param.DkWaterRemind;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Objects;

@ContentView(R.layout.activity_set_drink_water_para)
public class SetDrinkWaterParaActivity extends BaseActivity {
    @ViewInject(R.id.etWeekTime)
    EditText etWeekTime;

    @ViewInject(R.id.etStartTime)
    EditText etStartTime;

    @ViewInject(R.id.etEndTime)
    EditText etEndTime;
    @ViewInject(R.id.switch_open)
     Switch switchOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_SET_DRINK_WATER_PARA);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
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
            public void getDrinkWaterPara(DkWaterRemind dkWaterRemind) {
                super.getDrinkWaterPara(dkWaterRemind);
                dismissProgress();
                showSuccessToast(R.string.FUNC_SET_DRINK_WATER_PARA);
                StringBuilder content = new StringBuilder();
                content.append("\n喝水提醒开关：").append(dkWaterRemind.isEnable());
                content.append("\n提醒起始时间：").append(dkWaterRemind.getStartTime());
                content.append("\n提醒结束时间：").append(dkWaterRemind.getEndTime());
                content.append("\n提醒周期：").append(dkWaterRemind.getRemindWeek());
                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
                switchOpen.setChecked(dkWaterRemind.isEnable());
                etStartTime.setText(String.valueOf(dkWaterRemind.getStartTime()));
                etEndTime.setText(String.valueOf(dkWaterRemind.getEndTime()));
                etWeekTime.setText(String.valueOf(dkWaterRemind.getRemindWeek()));
            }

            @Override
            public void setDrinkWaterPara() {
                super.setDrinkWaterPara();
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

    @Event(R.id.btn_get)
    private void get(View v) {
        getData();
        showProgress();
    }


    @Event(R.id.btn_send)
    private void send(View v) {
        String startTime = etStartTime.getText().toString();
        String endTime = etEndTime.getText().toString();
        String weekTime = etWeekTime.getText().toString();
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
        showProgress();
        DkWaterRemind dkWaterRemind = new DkWaterRemind();
        dkWaterRemind.setStartTime(Integer.parseInt(startTime));
        dkWaterRemind.setEndTime(Integer.parseInt(endTime));
        dkWaterRemind.setEnable(switchOpen.isChecked());
        dkWaterRemind.setRemindWeek(  Integer.parseInt(weekTime));

        dkWaterRemind.setStartTime(Integer.parseInt(startTime));
        FissionSdkBleManage.getInstance().setDrinkWaterPara(dkWaterRemind);

//        AnyWear.setDrinkWaterPara(dkWaterRemind, new OnSmallDataCallback() {
//                    @Override
//                    public void OnError(String msg) {
//                        showToast(msg);
//                        dismissProgress();
//                    }
//
//
//            @Override
//            public void OnEmptyResult() {
//                        addLog(R.string.FUNC_SET_DRINK_WATER_PARA, "设置成功");
//                        dismissProgress();
//                        showSuccessToast();
//                    }
//                });
    }


    private void getData() {
        FissionSdkBleManage.getInstance().getDrinkWaterPara();
//        AnyWear.getDrinkWaterPara(new BigDataCallBack() {
//            @Override
//            public void OnEmpty(String cmdId) {
//                dismissProgress();
//            }
//
//            @Override
//            public void OnDrinkWaterPara(DkWaterRemind dkWaterRemind) {
//                dismissProgress();
//                showSuccessToast(R.string.FUNC_SET_DRINK_WATER_PARA);
//                StringBuilder content = new StringBuilder();
//                content.append("\n喝水提醒开关：").append(dkWaterRemind.isEnable());
//                content.append("\n提醒起始时间：").append(dkWaterRemind.getStartTime());
//                content.append("\n提醒结束时间：").append(dkWaterRemind.getEndTime());
//                content.append("\n提醒周期：").append(dkWaterRemind.getRemindWeek());
//                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
//                switchOpen.setChecked(dkWaterRemind.isEnable());
//                etStartTime.setText(String.valueOf(dkWaterRemind.getStartTime()));
//                etEndTime.setText(String.valueOf(dkWaterRemind.getEndTime()));
//                etWeekTime.setText(String.valueOf(dkWaterRemind.getRemindWeek()));
//            }
//
//
//            @Override
//            public void OnError(String msg) {
//                showToast(msg);
//                dismissProgress();
//            }
//        });

    }


}
