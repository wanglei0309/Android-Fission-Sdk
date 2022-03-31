package com.szfission.wear.demo.activity;

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

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;

@ContentView(R.layout.activity_log)
public class LogActivity extends BaseActivity {
    @ViewInject(R.id.lv_log)
    ListView lvLog;
    LogAdapter logAdapter;

    IntentFilter intentFilter;

    public static final String ACTION_REFRESH = "ACTION_REFRESH";

    private List<String> logList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.log);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
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
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.menu_clear:
                App.logData.clear();
                logList.clear();
                logAdapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}