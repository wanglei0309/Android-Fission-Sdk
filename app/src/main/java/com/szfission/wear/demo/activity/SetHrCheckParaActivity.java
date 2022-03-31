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
import com.szfission.wear.sdk.bean.HrDetectPara;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Objects;

@ContentView(R.layout.activity_hr_check_para)
public class SetHrCheckParaActivity extends BaseActivity  {
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
        setTitle(R.string.FUNC_SET_HR_CHECK_PARA);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
//        AnyWear.getHrCheckPara(this);
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
                showSuccessToast(R.string.FUNC_SET_HR_CHECK_PARA);
                StringBuilder content = new StringBuilder();
                content.append("\n心率自动检测开关：").append(hrDetectPara.isOpen());
                content.append("\n心率检测起始时间：").append(hrDetectPara.getStartTime());
                content.append("\n心率检测结束时间：").append(hrDetectPara.getEndTime());
                content.append("\n心率检测周期：").append(hrDetectPara.getWeek());
                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
                switchOpen.setChecked(hrDetectPara.isOpen());
                etStartTime.setText(String.valueOf(hrDetectPara.getStartTime()));
                etEndTime.setText(String.valueOf(hrDetectPara.getEndTime()));
                etWeekTime.setText(String.valueOf(hrDetectPara.getWeek()));
            }

            @Override
            public void setHrDetectPara() {
                super.setHrDetectPara();
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
    private void get(View v){
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
        HrDetectPara hrDetectPara = new HrDetectPara();
        hrDetectPara.setOpen(switchOpen.isChecked());
        hrDetectPara.setStartTime(Integer.parseInt(startTime));
        hrDetectPara.setEndTime(Integer.parseInt(endTime));
        hrDetectPara.setWeek(Integer.parseInt(weekTime));
        FissionSdkBleManage.getInstance().setHrDetectPara(hrDetectPara);
//        AnyWear.setHrDetectPara( hrDetectPara, new OnSmallDataCallback(){
//                    @Override
//                    public void OnError(String msg) {
//                        showToast(msg);
//                        dismissProgress();
//                    }
//
//
//            @Override
//            public void OnEmptyResult() {
//                        addLog(R.string.FUNC_SET_HR_CHECK_PARA, "设置成功");
//                        dismissProgress();
//                        showSuccessToast();
//                    }
//                });
    }





    private void getData(){
        FissionSdkBleManage.getInstance().getHrDetectPara();
//        AnyWear.getHrDetectPara(new BigDataCallBack(){
//            @Override
//            public void OnEmpty(String cmdId) {
//                dismissProgress();
//            }
//
//            @Override
//            public void OnError(String msg) {
//                showToast(msg);
//                dismissProgress();
//            }
//
//            @Override
//            public void OnHrDetectPara(HrDetectPara hrDetectPara) {
//                dismissProgress();
//                showSuccessToast(R.string.FUNC_SET_HR_CHECK_PARA);
//                StringBuilder content = new StringBuilder();
//                content.append("\n心率自动检测开关：").append(hrDetectPara.isOpen());
//                content.append("\n心率检测起始时间：").append(hrDetectPara.getStartTime());
//                content.append("\n心率检测结束时间：").append(hrDetectPara.getEndTime());
//                content.append("\n心率检测周期：").append(hrDetectPara.getWeek());
//                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
//                switchOpen.setChecked(hrDetectPara.isOpen());
//                etStartTime.setText(String.valueOf(hrDetectPara.getStartTime()));
//                etEndTime.setText(String.valueOf(hrDetectPara.getEndTime()));
//                etWeekTime.setText(String.valueOf(hrDetectPara.getWeek()));
//            }
//        });
    }

}
