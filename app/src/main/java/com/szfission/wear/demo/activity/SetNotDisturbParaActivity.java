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
import com.szfission.wear.sdk.bean.param.DndRemind;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Objects;

@ContentView(R.layout.activity_not_disturb_para)
public class SetNotDisturbParaActivity extends BaseActivity  {
    @ViewInject(R.id.etStartTime)
    EditText etStartTime;
    @ViewInject(R.id.etEndTime)
    EditText etEndTime;
    @ViewInject(R.id.switch_open)
    Switch switchOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_SET_DONT_DISTURB_PARA);
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
            public void getDndPara(DndRemind dndRemind) {
                super.getDndPara(dndRemind);
                dismissProgress();
                showSuccessToast(R.string.FUNC_SET_DONT_DISTURB_PARA);
                StringBuilder content = new StringBuilder();
                content.append("\n抬腕亮屏开关：").append(dndRemind.isEnable());
                content.append("\n抬腕有效起始时间：").append(dndRemind.getStartTime());
                content.append("\n抬腕有效结束时间：").append(dndRemind.getEndTime());
                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
                switchOpen.setChecked(dndRemind.isEnable());
                etStartTime.setText(String.valueOf(dndRemind.getStartTime()));
                etEndTime.setText(String.valueOf(dndRemind.getEndTime()));
            }

            @Override
            public void setDndPara() {
                super.setDndPara();
                dismissProgress();
                showSuccessToast();
            }
        });
    }

    private void getData() {
        FissionSdkBleManage.getInstance().getDndPara();
//        AnyWear.getDndPara(new BigDataCallBack(){
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
//            public void OnDndPara(DndRemind dndRemind) {
//                dismissProgress();
//                showSuccessToast(R.string.FUNC_SET_DONT_DISTURB_PARA);
//                StringBuilder content = new StringBuilder();
//                content.append("\n抬腕亮屏开关：").append(dndRemind.isEnable());
//                content.append("\n抬腕有效起始时间：").append(dndRemind.getStartTime());
//                content.append("\n抬腕有效结束时间：").append(dndRemind.getEndTime());
//                addLog(R.string.FUNC_SET_DRINK_WATER_PARA, content.toString());
//                switchOpen.setChecked(dndRemind.isEnable());
//                etStartTime.setText(String.valueOf(dndRemind.getStartTime()));
//                etEndTime.setText(String.valueOf(dndRemind.getEndTime()));
//            }
//
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
    private void get(View v){
        getData();
        showProgress();
    }
    @Event(R.id.btn_send)
    private void send(View v) {
        String startTime = etStartTime.getText().toString();
        String endTime = etEndTime.getText().toString();
        DndRemind dndRemind = new DndRemind();
        if (startTime.isEmpty()) {
            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endTime.isEmpty()) {
            Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress();
        dndRemind.setStartTime(Integer.parseInt(startTime));
        dndRemind.setEndTime(Integer.parseInt(endTime));
        dndRemind.setEnable(switchOpen.isChecked());
        FissionSdkBleManage.getInstance().setDndPara(dndRemind);

//        AnyWear.setDndPara(dndRemind
//                , new OnSmallDataCallback(){
//                    @Override
//                    public void OnError(String msg) {
//                        showToast(msg);
//                        dismissProgress();
//                    }
//
//
//                    @Override
//                    public void OnEmptyResult() {
//                        addLog(R.string.FUNC_SET_DONT_DISTURB_PARA, "设置成功");
//                        dismissProgress();
//                        showSuccessToast();
//                    }
//                });
    }




}
