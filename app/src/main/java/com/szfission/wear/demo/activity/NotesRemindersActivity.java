package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.NotesReminders;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

public class NotesRemindersActivity extends BaseActivity {
    EditText etTitle;

    EditText etAddress;

    EditText etContent;

    EditText etStartTimeHour;

    EditText etStartTimeMin;

    EditText etEndTimeHour;

    EditText etEndTimeMin;

    Button btn_send, btn_get;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_reminders);
        setTitle(R.string.FUNC_NOTES_REMINDERS);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        etTitle = findViewById(R.id.etTitle);
        etAddress = findViewById(R.id.etAddress);
        etContent = findViewById(R.id.etContent);
        etStartTimeHour = findViewById(R.id.etStartTimeHour);
        etStartTimeMin = findViewById(R.id.etStartTimeMin);
        etEndTimeHour = findViewById(R.id.etEndTimeHour);
        etEndTimeMin = findViewById(R.id.etEndTimeMin);
        btn_send = findViewById(R.id.btn_send);
        btn_get = findViewById(R.id.btn_get);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

        btn_get.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                get();
            }
        });

        addCmdResultListener(new FissionBigDataCmdResultListener() {
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
                dismissProgress();
            }

            @Override
            public void getNotesReminders(List<NotesReminders> notesReminders) {
                super.getNotesReminders(notesReminders);
                dismissProgress();
                if(notesReminders!=null && notesReminders.size()>0){
                    for(NotesReminders notes: notesReminders){
                        if(notes.isAlarmActive()){
                            etTitle.setText(notes.getTitle());
                            etAddress.setText(notes.getAddress());
                            etContent.setText(notes.getContent());
                        }
                    }
                }
            }

            @Override
            public void setNotesReminders() {
                super.setNotesReminders();
                showToast("设置成功");
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

    private void send() {
        int weekResult = 128;
        List<NotesReminders> notesReminders = new ArrayList<>();
        NotesReminders notesReminder = new NotesReminders(0,1,true,System.currentTimeMillis()+60000,weekResult, "开会", "深圳市领为创新科技有限公司", "开年激励大会", (int)(System.currentTimeMillis()/1000),  (int)(System.currentTimeMillis()/1000)+1800);
        notesReminders.add(notesReminder);
        for (int i = 1;i<5;i++){
            notesReminders.add(new NotesReminders(i,1,true,System.currentTimeMillis()+i*120000,weekResult, "开会"+i, "深圳市领为创新科技有限公司", "开年激励大会", (int)(System.currentTimeMillis()/1000)+i*300,  (int)(System.currentTimeMillis()/1000)+1800+i*300));
        }
        FissionSdkBleManage.getInstance().setNotesReminders(notesReminders);
    }

    private void get() {
        FissionSdkBleManage.getInstance().getNotesReminders();
        showProgress();
    }
}
