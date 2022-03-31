package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.param.LiftWristPara;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Objects;

@ContentView(R.layout.activity_liftwrist_para)
public class SetLiftWristParaActivity extends BaseActivity {
    @ViewInject(R.id.etStartTime)
    EditText etStartTime;
    @ViewInject(R.id.etEndTime)
    EditText etEndTime;
    @ViewInject(R.id.switch_open)
    Switch switchOpen;
    @ViewInject(R.id.etWristTime)
    EditText etWristTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_SET_LIFTWRIST_PARA);
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
            public void getLiftWristPara(LiftWristPara liftWristPara) {
                super.getLiftWristPara(liftWristPara);
                dismissProgress();
                showSuccessToast(R.string.FUNC_SET_LIFTWRIST_PARA);
                StringBuilder content = new StringBuilder();
                content.append("\n抬腕亮屏开关：").append(liftWristPara.isEnable());
                content.append("\n抬腕有效起始时间：").append(liftWristPara.getStartTime());
                content.append("\n抬腕有效结束时间：").append(liftWristPara.getEndTime());
                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
                switchOpen.setChecked(liftWristPara.isEnable());
                etStartTime.setText(String.valueOf(liftWristPara.getStartTime()));
                etEndTime.setText(String.valueOf(liftWristPara.getEndTime()));
            }

            @Override
            public void setLiftWristPara() {
                super.setLiftWristPara();
                dismissProgress();
                showSuccessToast();
            }
        });

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionAtCmdResultListener(){

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
                showToast("设置亮屏时间失败："+errorMsg);
            }

            @Override
            public void getScreenKeep(String result) {
                super.getScreenKeep(result);
                etWristTime.setText(String.format("%s秒", result));
            }

            @Override
            public void setScreenKeep() {
                super.setScreenKeep();
                showToast("设置亮屏时间成功");
            }
        });
    }

    private void getData() {
        FissionSdkBleManage.getInstance().getScreenKeep();
        FissionSdkBleManage.getInstance().getLiftWristPara();

//        AnyWear.getWristTime(new OnSmallDataCallback(){
//            @Override
//            public void OnStringResult(String content) {
//               etWristTime.setText(String.format("%s秒", content));
//            }
//        });
//
//        AnyWear.getLiftWristPara(new BigDataCallBack() {
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
//
//            @Override
//            public void OnLiftWristPara(LiftWristPara liftWristPara) {
//                dismissProgress();
//                showSuccessToast(R.string.FUNC_SET_LIFTWRIST_PARA);
//                StringBuilder content = new StringBuilder();
//                content.append("\n抬腕亮屏开关：").append(liftWristPara.isEnable());
//                content.append("\n抬腕有效起始时间：").append(liftWristPara.getStartTime());
//                content.append("\n抬腕有效结束时间：").append(liftWristPara.getEndTime());
//                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
//                switchOpen.setChecked(liftWristPara.isEnable());
//                etStartTime.setText(String.valueOf(liftWristPara.getStartTime()));
//                etEndTime.setText(String.valueOf(liftWristPara.getEndTime()));
//            }
//        });

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
        if (startTime.isEmpty()) {
            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endTime.isEmpty()) {
            Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress();
        LiftWristPara liftWristPara = new LiftWristPara();
        liftWristPara.setStartTime(Integer.parseInt(startTime));
        liftWristPara.setEnable(switchOpen.isChecked());
        liftWristPara.setEndTime(Integer.parseInt(endTime));

        FissionSdkBleManage.getInstance().setLiftWristPara(liftWristPara);

//        AnyWear.setLiftWristPara(liftWristPara
//                , new OnSmallDataCallback() {
//                    @Override
//                    public void OnError(String msg) {
//                        showToast(msg);
//                        dismissProgress();
//                    }
//
//
//                    @Override
//                    public void OnEmptyResult() {
//                        addLog(R.string.FUNC_SET_LIFTWRIST_PARA, "设置成功");
//                        dismissProgress();
//                        showSuccessToast();
//                    }
//                });
        if (!etWristTime.getText().toString().equals("")) {
            FissionSdkBleManage.getInstance().setScreenKeep(Integer.parseInt(etWristTime.getText().toString().trim().replace("秒", "")));
//            AnyWear.setWristTime(Integer.parseInt(etWristTime.getText().toString().trim()), new OnSmallDataCallback() {
//                @Override
//                public void OnEmptyResult() {
//                  showToast("设置亮屏时间成功");
//                }
//            });
        }
    }


}
