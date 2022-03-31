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
import com.szfission.wear.sdk.bean.SedentaryBean;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Objects;

@ContentView(R.layout.activity_set_sedentary_reminder)
public class SetSedentaryReminderActivity extends BaseActivity  {
    @ViewInject(R.id.etModerate)
    EditText etTargetStep;

    @ViewInject(R.id.etVigorous)
    EditText etStartTime;

    @ViewInject(R.id.etMaxHr2)
    EditText etEndTime;

    @ViewInject(R.id.etMaxHr)
    EditText etKeepTime;



    @ViewInject(R.id.switch_open)
    Switch switchOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_GET_SEDENTARY_PARA);
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
            public void getSedentaryPara(SedentaryBean sedentaryBean) {
                super.getSedentaryPara(sedentaryBean);
                dismissProgress();
                showSuccessToast(R.string.FUNC_GET_SEDENTARY_PARA);
                StringBuilder content = new StringBuilder();
                content.append("\n久坐提醒开关：").append(sedentaryBean.isEnable());
                content.append("\n检测起始时间：").append(sedentaryBean.getStartTime());
                content.append("\n检测结束时间：").append(sedentaryBean.getEndTime());
                content.append("\n久坐持续时间检测时间：").append(sedentaryBean.getDurTime());
                content.append("\n目标步数：").append(sedentaryBean.getTargetStep());
                addLog(R.string.FUNC_GET_SEDENTARY_PARA, content.toString());
                switchOpen.setChecked(sedentaryBean.isEnable());
                etStartTime.setText(String.valueOf(sedentaryBean.getStartTime()));
                etEndTime.setText(String.valueOf(sedentaryBean.getEndTime()));
                etKeepTime.setText(String.valueOf(sedentaryBean.getDurTime()));
                etTargetStep.setText(String.valueOf(sedentaryBean.getTargetStep()));
            }

            @Override
            public void setSedentaryPara() {
                super.setSedentaryPara();
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

    private void getData() {
        FissionSdkBleManage.getInstance().getSedentaryPara();

//        AnyWear.getSedentaryPara(new BigDataCallBack(){
//            @Override
//            public void OnSedentaryParaCallback(SedentaryBean sedentaryBean) {
//                dismissProgress();
//                showSuccessToast(R.string.FUNC_GET_SEDENTARY_PARA);
//                StringBuilder content = new StringBuilder();
//                content.append("\n久坐提醒开关：").append(sedentaryBean.isEnable());
//                content.append("\n检测起始时间：").append(sedentaryBean.getStartTime());
//                content.append("\n检测结束时间：").append(sedentaryBean.getEndTime());
//                content.append("\n久坐持续时间检测时间：").append(sedentaryBean.getDurTime());
//                content.append("\n目标步数：").append(sedentaryBean.getTargetStep());
//                addLog(R.string.FUNC_GET_SEDENTARY_PARA, content.toString());
//                switchOpen.setChecked(sedentaryBean.isEnable());
//                etStartTime.setText(String.valueOf(sedentaryBean.getStartTime()));
//                etEndTime.setText(String.valueOf(sedentaryBean.getEndTime()));
//                etKeepTime.setText(String.valueOf(sedentaryBean.getDurTime()));
//                etTargetStep.setText(String.valueOf(sedentaryBean.getTargetStep()));
//            }
//
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
//        });
    }

    @Event(R.id.btn_send)
    private void send(View v) {
        String startTime = etStartTime.getText().toString();
        String endTime = etEndTime.getText().toString();
        String keepTime = etKeepTime.getText().toString();
        String targetStep = etTargetStep.getText().toString();
        if (startTime.isEmpty()) {
            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endTime.isEmpty()) {
            Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (keepTime.isEmpty()) {
            Toast.makeText(this, "请输入持续检测时间", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress();
        SedentaryBean sedentaryBean = new SedentaryBean();
        sedentaryBean.setEnable(switchOpen.isChecked());
        sedentaryBean.setStartTime(Integer.parseInt(startTime));
        sedentaryBean.setEndTime(Integer.parseInt(endTime));
        sedentaryBean.setDurTime(Integer.parseInt(keepTime));
        sedentaryBean.setTargetStep(Integer.parseInt(targetStep));
        FissionSdkBleManage.getInstance().setSedentaryPara(sedentaryBean);
//        AnyWear.setSedentaryPara(sedentaryBean, new OnSmallDataCallback(){
//                    @Override
//                    public void OnError(String msg) {
//                        showToast(msg);
//                        dismissProgress();
//                    }
//
//
//            @Override
//            public void OnEmptyResult() {
//                        addLog(R.string.FUNC_GET_SEDENTARY_PARA, "设置成功");
//                        dismissProgress();
//                        showSuccessToast();
//                    }
//        });
    }


}
