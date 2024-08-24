package com.szfission.wear.demo.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.szfission.wear.demo.ActionConstant;
import com.szfission.wear.demo.GridChartView;
import com.szfission.wear.demo.R;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;


public class MeasureHeartRateActivity extends BaseActivity {

    GridChartView gridChartView;

    Button btnTest;

    TextView tvHeart;

    boolean testing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_heart_rate);

        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.title_heart_rate_measure);
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        gridChartView = findViewById(R.id.gridChart);
        btnTest = findViewById(R.id.btn_test);
        tvHeart = findViewById(R.id.tv_heart);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionConstant.ACTION_MEASUREMENT_HR);
        registerReceiver(broadcastReceiver, intentFilter);

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testing = !testing;
            }
        });
    }


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ActionConstant.ACTION_MEASUREMENT_HR.equals(action)) {
                int heartRate = intent.getIntExtra("heartRate", 0);
                if (heartRate == 255) {
                    tvHeart.setText("--bpm");
                    gridChartView.addValue(0);
                } else {
                    tvHeart.setText(heartRate + "bpm");
                    gridChartView.addValue(heartRate);
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
      /*  if (testing) {
            CMDHelper.measurementHeartRate(getBaseContext(), false);
        }*/
        super.onDestroy();
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
}
