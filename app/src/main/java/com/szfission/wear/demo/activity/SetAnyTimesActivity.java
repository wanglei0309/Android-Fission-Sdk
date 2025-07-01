package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.NotesReminders;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SetAnyTimesActivity extends BaseActivity {

    DatePicker datePicker;

    TimePicker timePicker;

    Button btn_set_times;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_any_time);
        setTitle(R.string.FUNC_SET_ANY_TIME);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        datePicker = findViewById(R.id.datePicker);
        timePicker = findViewById(R.id.timePicker);
        btn_set_times = findViewById(R.id.btn_set_times);

        btn_set_times.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimes();
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

    private void setTimes() {
        int year = datePicker.getYear();
        int month = datePicker.getMonth();
        int dayOfMonth = datePicker.getDayOfMonth();

        int hour = timePicker.getCurrentHour();
        int minute = timePicker.getCurrentMinute();

        // 组合日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, hour, minute);
        FissionSdkBleManage.getInstance().setTimes(calendar.getTimeInMillis()/1000);
    }

}
