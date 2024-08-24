package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.TimeUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.param.FemalePhysiology;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.Calendar;

public class MenstrualPeriodActivity extends BaseActivity{

    EditText ed_menstrual_days;

    EditText ed_cycle_days;

    EditText ed_date;

    EditText ed_health_settings;

    EditText ed_reminder_days;

    Switch switch_open;

    EditText ed_reminder_time;

    EditText ed_pregnancy_reminder_mode;

    Button btn_save;

    Button btn_get;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menstrual_period);
        setTitle(getString(R.string.set_female_health_data));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ed_menstrual_days = findViewById(R.id.ed_menstrual_days);
        ed_cycle_days = findViewById(R.id.ed_cycle_days);
        ed_date = findViewById(R.id.ed_date);
        ed_health_settings = findViewById(R.id.ed_health_settings);
        ed_reminder_days = findViewById(R.id.ed_reminder_days);

        switch_open = findViewById(R.id.switch_open);
        ed_reminder_time = findViewById(R.id.ed_reminder_time);
        ed_pregnancy_reminder_mode = findViewById(R.id.ed_pregnancy_reminder_mode);
        btn_save = findViewById(R.id.btn_save);
        btn_get = findViewById(R.id.btn_get);

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
            public void setFemalePhysiology() {
                super.setFemalePhysiology();
                Toast.makeText(MenstrualPeriodActivity.this, "设置成功", Toast.LENGTH_LONG).show();
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int femaleModel = Integer.parseInt(ed_health_settings.getText().toString().trim());
                int menstrualAdvance = Integer.parseInt(ed_reminder_days.getText().toString().trim());
                int duration = Integer.parseInt(ed_menstrual_days.getText().toString().trim());
                int intervalPeriod = Integer.parseInt(ed_cycle_days.getText().toString().trim());
                int remindTime = Integer.parseInt(ed_reminder_time.getText().toString().trim());
                int pregnancyRemindType = Integer.parseInt(ed_pregnancy_reminder_mode.getText().toString().trim());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(TimeUtils.string2Date(ed_date.getText().toString().trim(), "yyyy/MM/dd"));
                FemalePhysiology femalePhysiology = new FemalePhysiology(femaleModel, menstrualAdvance, duration, intervalPeriod,
                        calendar, pregnancyRemindType, remindTime, switch_open.isChecked());
                FissionSdkBleManage.getInstance().setFemalePhysiology(femalePhysiology);
            }
        });

        btn_get.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().getFemalePhysiology();
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
}
