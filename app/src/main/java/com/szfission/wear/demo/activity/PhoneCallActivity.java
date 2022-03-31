package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

/**
 * 手机来电
 */
@ContentView(R.layout.activity_phone_call)
public class PhoneCallActivity extends BaseActivity {

    @ViewInject(R.id.etName)
    EditText etName;

    @ViewInject(R.id.etNumber)
    EditText etNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_STRU_CALL_DATA);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
//        showProgress();

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
            public void incomingCall() {
                super.incomingCall();
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

    @Event(R.id.btn_send)
    private void send(View v) {
        String name = etName.getText().toString();
        String number = etNumber.getText().toString();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.getBytes().length > 48) {
            Toast.makeText(this, "姓名过长", Toast.LENGTH_SHORT).show();
            return;
        }
        if (number.isEmpty()) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        if(number.getBytes().length > 20){
            Toast.makeText(this, "手机号过长", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress();
        FissionSdkBleManage.getInstance().incomingCall((System.currentTimeMillis() / 1000 ),name,number);
//        AnyWear.callData( (int)(System.currentTimeMillis() / 1000 ),name,number,new OnSmallDataCallback() {
//            @Override
//            public void OnEmptyResult() {
//                dismissProgress();
//                showSuccessToast();
//            }
//
//            @Override
//            public void OnError(String msg) {
//                showToast(msg);
//                dismissProgress();
//
//            }
//        });
    }
}
