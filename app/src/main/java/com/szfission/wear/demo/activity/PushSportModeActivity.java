package com.szfission.wear.demo.activity;


import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.FissionSdk;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.FissionDialUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

@ContentView(R.layout.activity_push_sport)
public class PushSportModeActivity extends BaseActivity{
    private RxPermissions rxPermissions;
    @ViewInject(R.id.pushProgress)
    TextView pushProgress;
    @ViewInject(R.id.pushSport)
    Button pushSport;
    @ViewInject(R.id.spinnerType)
    Spinner spinner;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        setTitle(R.string.FUNC_PUSH_CUSTOM_SPORT);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);

        String[] mItems = getResources().getStringArray(R.array.sport);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mItems);
        spinner.setAdapter(adapter);

        pushSport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int type = spinner.getSelectedItemPosition();
                if (type == 0){
                    setDiaModel("61.bin");
                }else if (type == 1){
                    setDiaModel("82.bin");
                }else if (type == 2){
                    setDiaModel("112.bin");
                }else if (type == 3){
                    setDiaModel("27.bin");
                }
            }
        });

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

            }

            @Override
            public void onUpdateDialProgress(int state, int progress) {
                super.onUpdateDialProgress(state, progress);
                pushProgress.setText("升级进度:"+progress+"%");
            }
        });
    }

    @Override
    protected boolean useEventBus() {
        return true;
    }

    private void setDiaModel(String name)  {
        byte[] resultData =  FissionDialUtil.inputBin(this,name);
        FissionSdk.getInstance().startDial(resultData, FissionEnum.WRITE_SPORT_DATA);
        FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_SPORT_DATA);
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

    /**
     * 接收数据的事件总线
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DataMessageEvent event) {
        pushProgress.setText("升级进度:"+event.getMessageContent());
    }

}
