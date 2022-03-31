package com.szfission.wear.demo.activity;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.QuickReply;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

@ContentView(R.layout.activity_quick_reply)
public class QuickReplyActivity extends BaseActivity{
    @ViewInject(R.id.ed_data1)
    EditText ed_data1;
    @ViewInject(R.id.ed_data2)
    EditText ed_data2;
    @ViewInject(R.id.ed_data3)
    EditText ed_data3;
    @ViewInject(R.id.ed_data4)
    EditText ed_data4;
    @ViewInject(R.id.ed_data5)
    EditText ed_data5;
    @ViewInject(R.id.ed_data6)
    EditText ed_data6;
    @ViewInject(R.id.ed_data7)
    EditText ed_data7;
    @ViewInject(R.id.ed_data8)
    EditText ed_data8;
    @ViewInject(R.id.ed_data9)
    EditText ed_data9;
    @ViewInject(R.id.ed_data10)
    EditText ed_data10;
    @ViewInject(R.id.btn_set)
    Button btn_set;
    @ViewInject(R.id.btn_get)
    Button btn_get;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        setTitle(R.string.FUNC_QUICK_REPLY_CMD);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);

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
            public void setQuickReplyData() {
                super.setQuickReplyData();
                Toast.makeText(QuickReplyActivity.this, "设置成功", Toast.LENGTH_LONG).show();
            }

            @Override
            public void getQuickReplyData(List<QuickReply> quickReplyList) {
                super.getQuickReplyData(quickReplyList);
                if(quickReplyList!=null){
                    for(int i=0; i<quickReplyList.size(); i++){
                        switch (i){
                            case 0:
                                ed_data1.setText(quickReplyList.get(i).getData());
                                break;

                            case 1:
                                ed_data2.setText(quickReplyList.get(i).getData());
                                break;

                            case 2:
                                ed_data3.setText(quickReplyList.get(i).getData());
                                break;

                            case 3:
                                ed_data4.setText(quickReplyList.get(i).getData());
                                break;

                            case 4:
                                ed_data5.setText(quickReplyList.get(i).getData());
                                break;

                            case 5:
                                ed_data6.setText(quickReplyList.get(i).getData());
                                break;

                            case 6:
                                ed_data7.setText(quickReplyList.get(i).getData());
                                break;

                            case 7:
                                ed_data8.setText(quickReplyList.get(i).getData());
                                break;

                            case 8:
                                ed_data9.setText(quickReplyList.get(i).getData());
                                break;

                            case 9:
                                ed_data10.setText(quickReplyList.get(i).getData());
                                break;
                        }
                    }
                }
            }
        });

        btn_set.setOnClickListener(v -> {
            List<String> strings = new ArrayList<>();
            if(!TextUtils.isEmpty(ed_data1.getText())){
                strings.add(ed_data1.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data2.getText())){
                strings.add(ed_data2.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data3.getText())){
                strings.add(ed_data3.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data4.getText())){
                strings.add(ed_data4.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data5.getText())){
                strings.add(ed_data5.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data6.getText())){
                strings.add(ed_data6.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data7.getText())){
                strings.add(ed_data7.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data8.getText())){
                strings.add(ed_data8.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data9.getText())){
                strings.add(ed_data9.getText().toString().trim());
            }
            if(!TextUtils.isEmpty(ed_data10.getText())){
                strings.add(ed_data10.getText().toString().trim());
            }
            FissionSdkBleManage.getInstance().setQuickReplyData(strings);
        });

        btn_get.setOnClickListener(v -> {
            FissionSdkBleManage.getInstance().getQuickReplyData();
        });

    }

    @Override
    protected boolean useEventBus() {
        return true;
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DataMessageEvent event) {
    }


}
