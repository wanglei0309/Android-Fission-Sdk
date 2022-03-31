package com.szfission.wear.demo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import com.szfission.wear.demo.ActionConstant;
import com.szfission.wear.demo.R;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

@ContentView(R.layout.activity_set_time_formats)
public class SetTimeFormatsActivity extends Activity {

    @ViewInject(R.id.rb_time_model1)
    RadioButton rbTimeModel1;

    @ViewInject(R.id.rb_time_model2)
    RadioButton rbTimeModel2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        x.view().inject(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionConstant.ACTION_USER_SETTING);
        registerReceiver(bleBroadcast, intentFilter);
     //   CMDHelper.readUserSetting(getBaseContext());
    }

    @Event(R.id.ib_close)
    private void close(View v) {
        finish();
    }

    @Event(R.id.btn_send)
    private void send(View v) {
        int hourModel;
        if (rbTimeModel1.isChecked()) {
            hourModel = 12;
        } else {
            hourModel = 24;
        }
        Intent intent = getIntent();
        intent.putExtra("hourModel", Integer.valueOf(hourModel));
        setResult(RESULT_OK, intent);
        finish();
    }

    BroadcastReceiver bleBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActionConstant.ACTION_USER_SETTING.equals(intent.getAction())) {
                int hourModel = intent.getIntExtra("hourModel", 12);
                if (hourModel == 12) {
                    rbTimeModel1.setChecked(true);
                } else {
                    rbTimeModel2.setChecked(true);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(bleBroadcast);
        super.onDestroy();
    }
}
