package com.szfission.wear.demo.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.szfission.wear.demo.App;
import com.szfission.wear.demo.LogAdapter;
import com.szfission.wear.demo.R;


import java.util.ArrayList;
import java.util.List;

public class LogActivity extends BaseActivity {
    ListView lvLog;
    LogAdapter logAdapter;

    IntentFilter intentFilter;

    public static final String ACTION_REFRESH = "ACTION_REFRESH";

    private List<String> logList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        setTitle(R.string.log);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        lvLog = findViewById(R.id.lv_log);

        logList = new ArrayList<>();
        logList.addAll(App.logData);
        logAdapter = new LogAdapter(this, logList);
        lvLog.setAdapter(logAdapter);
        lvLog.setSelection(logList.size());

        intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_REFRESH);
        registerReceiver(broadcastReceiver, intentFilter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log, menu);
        return super.onCreateOptionsMenu(menu);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(logList.size() != App.logData.size()){
                logList.clear();
                logList.addAll(App.logData);
            }
            logAdapter.notifyDataSetChanged();
            lvLog.setSelection(logList.size());
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            this.finish();
            return true;
        }else if(item.getItemId() == R.id.menu_clear){
            App.logData.clear();
            logList.clear();
            logAdapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}