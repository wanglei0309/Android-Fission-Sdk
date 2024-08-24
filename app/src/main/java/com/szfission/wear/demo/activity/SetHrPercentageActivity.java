package com.szfission.wear.demo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.szfission.wear.demo.ActionConstant;
import com.szfission.wear.demo.R;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

public class SetHrPercentageActivity extends Activity {

    EditText etNormalPercent;

    EditText etModerate;

    EditText etVigorousPercent;

    EditText etMaxHRPercent;

    EditText etAntiRepetitiveTime;

    EditText etMaxHR;

    Button btn_send;

    ImageButton ib_close;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_hr_percentage);
        x.view().inject(this);

        etNormalPercent = findViewById(R.id.et_value1);
        etModerate = findViewById(R.id.et_value2);
        etVigorousPercent = findViewById(R.id.et_value3);
        etMaxHRPercent = findViewById(R.id.et_value4);
        etAntiRepetitiveTime = findViewById(R.id.et_t1);
        etMaxHR = findViewById(R.id.et_max_hr);
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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionConstant.ACTION_READ_HR_PERCENTAGE_SUCCESS);
        registerReceiver(bleBroadcast, intentFilter);
      //  CMDHelper.readHrPercentage(getBaseContext());
    }

    private void close() {
        finish();
    }

    private void send() {
        String normalPercent = etNormalPercent.getText().toString();
        String moderatePercent = etModerate.getText().toString();
        String vigorousPercent = etVigorousPercent.getText().toString();
        String maxHRPercent = etMaxHRPercent.getText().toString();
        String maxHR = etMaxHR.getText().toString();
        String antiRepetitiveTime = etAntiRepetitiveTime.getText().toString();
        if (normalPercent.isEmpty()) {
            Toast.makeText(this, "Please enter light percent", Toast.LENGTH_SHORT).show();
            return;
        }
        if (moderatePercent.isEmpty()) {
            Toast.makeText(this, "Please enter moderate percent", Toast.LENGTH_SHORT).show();
            return;
        }
        if (vigorousPercent.isEmpty()) {
            Toast.makeText(this, "Please enter vigorous percent", Toast.LENGTH_SHORT).show();
            return;
        }
        if (maxHRPercent.isEmpty()) {
            Toast.makeText(this, "Please enter max HR percent", Toast.LENGTH_SHORT).show();
            return;
        }
        if (maxHR.isEmpty()) {
            Toast.makeText(this, "Please enter max HR value", Toast.LENGTH_SHORT).show();
            return;
        }
        if (antiRepetitiveTime.isEmpty()) {
            Toast.makeText(this, "Please enter anti Repetitive time", Toast.LENGTH_SHORT).show();
            return;
        }
        int antiRepetitiveTimeTag = Integer.valueOf(antiRepetitiveTime);
        if (antiRepetitiveTimeTag <= 0) {
            Toast.makeText(this, "Please  anti repetitive time cannot be less than 0", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = getIntent();
        intent.putExtra("normalPercent", Integer.valueOf(normalPercent));
        intent.putExtra("moderatePercent", Integer.valueOf(moderatePercent));
        intent.putExtra("vigorousPercent", Integer.valueOf(vigorousPercent));
        intent.putExtra("maxHRPercent", Integer.valueOf(maxHRPercent));
        intent.putExtra("maxHR", Integer.valueOf(maxHR));
        intent.putExtra("antiRepetitiveTime", Integer.valueOf(antiRepetitiveTime));
        setResult(RESULT_OK, intent);
        finish();
    }

    BroadcastReceiver bleBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActionConstant.ACTION_READ_HR_PERCENTAGE_SUCCESS.equals(intent.getAction())) {
                int normalPercent = intent.getIntExtra("normalPercent", 0);
                int moderatePercent = intent.getIntExtra("moderatePercent", 0);
                int vigorousPercent = intent.getIntExtra("vigorousPercent", 0);
                int maxHRPercent = intent.getIntExtra("maxHRPercent", 0);
                int maxHR = intent.getIntExtra("maxHR", 0);
                int antiRepetitiveTime = intent.getIntExtra("antiRepetitiveTime", 0);
                etNormalPercent.setText(String.valueOf(normalPercent));
                etModerate.setText(String.valueOf(moderatePercent));
                etVigorousPercent.setText(String.valueOf(vigorousPercent));
                etMaxHRPercent.setText(String.valueOf(maxHRPercent));
                etMaxHR.setText(String.valueOf(maxHR));
                etAntiRepetitiveTime.setText(String.valueOf(antiRepetitiveTime));
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(bleBroadcast);
        super.onDestroy();
    }
}
