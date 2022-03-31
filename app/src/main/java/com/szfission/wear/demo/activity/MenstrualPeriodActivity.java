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

@ContentView(R.layout.activity_menstrual_period)
public class MenstrualPeriodActivity extends BaseActivity{

    @ViewInject(R.id.ed_menstrual_days)
    EditText ed_menstrual_days;

    @ViewInject(R.id.ed_cycle_days)
    EditText ed_cycle_days;

    @ViewInject(R.id.ed_date)
    EditText ed_date;

    @ViewInject(R.id.ed_health_settings)
    EditText ed_health_settings;

    @ViewInject(R.id.ed_reminder_days)
    EditText ed_reminder_days;

    @ViewInject(R.id.switch_open)
    Switch switch_open;

    @ViewInject(R.id.ed_reminder_time)
    EditText ed_reminder_time;

    @ViewInject(R.id.ed_pregnancy_reminder_mode)
    EditText ed_pregnancy_reminder_mode;

    @ViewInject(R.id.btn_save)
    Button btn_save;

    @ViewInject(R.id.btn_get)
    Button btn_get;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("设置女性健康数据");
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
