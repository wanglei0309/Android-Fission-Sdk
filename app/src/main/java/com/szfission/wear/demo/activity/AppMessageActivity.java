package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.AppMessageBean;
import com.szfission.wear.sdk.util.FsLogUtil;
import com.szfission.wear.sdk.util.RxTimerUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Random;

public class AppMessageActivity extends BaseActivity {
    EditText etName;

    EditText etContent, etTime;

    Spinner spinnerType;

    Button btn_send, btn_send_timer;

    private RxTimerUtil mRxTimerUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_message);

        etName = findViewById(R.id.etName);
        etTime = findViewById(R.id.etTime);
        etContent = findViewById(R.id.etContent);
        spinnerType = findViewById(R.id.spinnerType);
        btn_send = findViewById(R.id.btn_send);
        btn_send_timer = findViewById(R.id.btn_send_timer);

        setTitle(R.string.FUNC_GET_APPS_MESS);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mRxTimerUtil = new RxTimerUtil();

        String[] mItems = getResources().getStringArray(R.array.messageType);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mItems);
        spinnerType.setAdapter(adapter);

        addCmdResultListener(new FissionBigDataCmdResultListener() {
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
                dismissProgress();
            }

            @Override
            public void pushAppNotification() {
                super.pushAppNotification();
                LogUtils.d("wl", "通知发送成功回调");
                dismissProgress();
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        btn_send_timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendTimer();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mRxTimerUtil!=null){
            mRxTimerUtil.cancelTimer();
        }
    }

    private void send() {
        if(mRxTimerUtil!=null){
            mRxTimerUtil.cancelTimer();
        }
        String name = etName.getText().toString();
        String content = etContent.getText().toString();
        if (name.isEmpty()) {
            Toast.makeText(this, "联系人不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.getBytes().length > 20) {
            Toast.makeText(this, "联系人过长", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.getBytes().length > 200) {
            Toast.makeText(this, "内容过长", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress();
        int id = 1;
        int type = spinnerType.getSelectedItemPosition();
        AppMessageBean  appMessageBean = new AppMessageBean();
        appMessageBean.setMsgId(id);
        appMessageBean.setMsgType(type);
        appMessageBean.setContactName(etName.getText().toString());
        appMessageBean.setMsgContent(etContent.getText().toString());
        appMessageBean.setMsgTime(System.currentTimeMillis()/1000);
        FsLogUtil.d("推送类型"+appMessageBean.getMsgType());
//        AnyWear.setAppMsg(appMessageBean, new OnSmallDataCallback() {
//            @Override
//            public void OnEmptyResult() {
//                FsLogUtil.d("推送消息成功");
//                dismissProgress();
//            }
//
//            @Override
//            public void OnError(String msg) {
//                FsLogUtil.d("推送消息失败"+msg);
//            }
//        });
        FissionSdkBleManage.getInstance().pushAppNotification(appMessageBean);
    }

    private void sendTimer() {
        if(TextUtils.isEmpty(etTime.getText().toString())){
            ToastUtils.showShort("时间间隔参数不能为null");
            return;
        }
        int time = Integer.parseInt(etTime.getText().toString().trim())*1000;
        ToastUtils.showLong("定时间隔"+(time/1000)+"s推送消息");
        mRxTimerUtil.interval(time, new RxTimerUtil.RxAction() {
            @Override
            public void action(long number) {
                String name = "随机推送消息";
                String content = "hello world";
                showProgress();
                int id = 1;
                Random random = new Random();

                int type = random.nextInt(56);
                AppMessageBean  appMessageBean = new AppMessageBean();
                appMessageBean.setMsgId(id);
                appMessageBean.setMsgType(type);
                appMessageBean.setContactName(name);
                appMessageBean.setMsgContent(content);
                appMessageBean.setMsgTime(System.currentTimeMillis()/1000);
                FsLogUtil.d("推送类型"+appMessageBean.getMsgType());
                FissionSdkBleManage.getInstance().pushAppNotification(appMessageBean);
            }
        });
    }
}
