package com.szfission.wear.demo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import com.szfission.wear.demo.ActionConstant;
import com.szfission.wear.demo.R;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

public class SetTimeFormatsActivity extends Activity {

    RadioButton rbTimeModel1;

    RadioButton rbTimeModel2;

    Button btn_send;

    ImageButton ib_close;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_time_formats);
        x.view().inject(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionConstant.ACTION_USER_SETTING);
        registerReceiver(bleBroadcast, intentFilter);

        rbTimeModel1 = findViewById(R.id.rb_time_model1);
        rbTimeModel2 = findViewById(R.id.rb_time_model2);
        btn_send = findViewById(R.id.btn_send);
        ib_close = findViewById(R.id.ib_close);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        ib_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                close();
            }
        });
    }

    private void close() {
        finish();
    }

    private void send() {
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
