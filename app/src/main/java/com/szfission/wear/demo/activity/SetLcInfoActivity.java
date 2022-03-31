package com.szfission.wear.demo.activity;


import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;
import com.szfission.wear.sdk.util.FsLogUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;

@ContentView(R.layout.activity_set_location_information)
public class SetLcInfoActivity extends BaseActivity{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_LOCATION_INFORMATION);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
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
                showToast("设置运动定位数据"+errorMsg);
            }

            @Override
            public void setLocationInfo() {
                super.setLocationInfo();
                showToast("设置运动定位数据成功");
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
        //数据异常，需要和确认是不是协议问题
        FissionSdkBleManage.getInstance().setLocationInfo(113.88308f,22.55329f);
//        AnyWear.setLocationInfo(113.46f,22.27f
//                , new OnSmallDataCallback() {
//                    @Override
//                    public void OnEmptyResult() {
//                        showToast("设置运动定位数据成功");
//                        FsLogUtil.d("设置运动定位数据成功");
//                    }
//
//                    @Override
//                    public void OnError(String msg) {
//                        showToast("设置运动定位数据"+msg);
//                        FsLogUtil.d("设置运动定位数据失败"+msg);
//                    }
//                });
    }
}
