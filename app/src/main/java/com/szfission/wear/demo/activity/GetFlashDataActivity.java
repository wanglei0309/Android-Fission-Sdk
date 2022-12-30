package com.szfission.wear.demo.activity;


import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;


@ContentView(R.layout.activity_get_flash_data)
public class GetFlashDataActivity extends BaseActivity{
    @ViewInject(R.id.ed_data1)
    EditText ed_data1;
    @ViewInject(R.id.ed_data2)
    EditText ed_data2;
    @ViewInject(R.id.btn_get)
    Button btn_get;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        setTitle(R.string.FUNC_GET_FLASH_DATA);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        super.onCreate(savedInstanceState);

        btn_get.setOnClickListener(v -> {
            String address = ed_data1.getText().toString();
            String length = ed_data2.getText().toString();
            FissionSdkBleManage.getInstance().getFlashData(address, length);
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
