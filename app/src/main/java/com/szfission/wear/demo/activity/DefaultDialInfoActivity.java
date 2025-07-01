package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.adapter.DefaultDialStateListAdapter;
import com.szfission.wear.demo.adapter.ExercisesListAdapter;
import com.szfission.wear.demo.bean.DefaultDialState;
import com.szfission.wear.sdk.bean.AppMessageBean;
import com.szfission.wear.sdk.util.FsLogUtil;
import com.szfission.wear.sdk.util.RxTimerUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DefaultDialInfoActivity extends BaseActivity {

    Button btn_get_dial_states, btn_set_dial_states;

    RadioButton rg_all_open, rg_all_close, rg_all_choose;

    RecyclerView recyclerView;

    private DefaultDialStateListAdapter defaultDialStateListAdapter;

    private List<DefaultDialState> defaultDialStateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_dial_states);

        btn_get_dial_states = findViewById(R.id.btn_get_dial_states);
        btn_set_dial_states = findViewById(R.id.btn_set_dial_states);
        rg_all_open = findViewById(R.id.rg_all_open);
        rg_all_close = findViewById(R.id.rg_all_close);
        rg_all_choose = findViewById(R.id.rg_all_choose);
        recyclerView = findViewById(R.id.recyclerView);

        setTitle(R.string.FUNC_DIAL_STATES_INFO);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        defaultDialStateListAdapter = new DefaultDialStateListAdapter(R.layout.list_item_dial_state);
        recyclerView.setAdapter(defaultDialStateListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        defaultDialStateList = new ArrayList<>();
        for(int i=0; i< 13; i++){
            DefaultDialState defaultDialState = new DefaultDialState(i, false);
            defaultDialStateList.add(defaultDialState);
            defaultDialStateListAdapter.addData(defaultDialState);
        }

        if(rg_all_choose.isChecked()){
            recyclerView.setVisibility(View.VISIBLE);
        }else{
            recyclerView.setVisibility(View.GONE);
        }
        addCmdResultListener(new FissionAtCmdResultListener() {
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
            public void getDefaultDialState(int state) {
                super.getDefaultDialState(state);
                if(state == FissionConstant.DEFAULT_DIAL_ALL_CLOSE){
                    for(int i=0; i<defaultDialStateList.size(); i++){
                        defaultDialStateList.get(i).setOpen(false);
                    }
                    rg_all_close.setChecked(true);
                }else if(state == FissionConstant.DEFAULT_DIAL_ALL_OPEN){
                    for(int i=0; i<defaultDialStateList.size(); i++){
                        defaultDialStateList.get(i).setOpen(true);
                    }
                    rg_all_open.setChecked(true);
                }else{
                    String binaryStr = Integer.toBinaryString(state);
                    String paddedBinary = String.format("%16s", binaryStr).replace(' ', '0');
                    char[] charArray = paddedBinary.toCharArray();
                    for(int i=0; i<defaultDialStateList.size(); i++){
                        defaultDialStateList.get(i).setOpen("1".equals(String.valueOf(charArray[charArray.length-1-i])));
                    }
                    rg_all_choose.setChecked(true);
                    FissionLogUtils.d("wl", "---获取内置表盘状态---"+defaultDialStateList.toString());
                }
            }

            @Override
            public void setDefaultDialState() {
                super.setDefaultDialState();
                ToastUtils.showShort("设置成功！！！");
                if(rg_all_open.isChecked()){
                    for(int i=0; i<defaultDialStateList.size(); i++){
                        defaultDialStateList.get(i).setOpen(true);
                    }
                }else if(rg_all_close.isChecked()){
                    for(int i=0; i<defaultDialStateList.size(); i++){
                        defaultDialStateList.get(i).setOpen(false);
                    }
                }
            }
        });

        btn_get_dial_states.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().getDefaultDialState();
            }
        });

        btn_set_dial_states.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rg_all_open.isChecked()){
                    FissionSdkBleManage.getInstance().setDefaultDialState(FissionConstant.DEFAULT_DIAL_ALL_OPEN);
                }else if(rg_all_close.isChecked()){
                    FissionSdkBleManage.getInstance().setDefaultDialState(FissionConstant.DEFAULT_DIAL_ALL_CLOSE);
                }else{
                    FissionLogUtils.d("wl", "---设置内置表盘状态---"+defaultDialStateList.toString());
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("000");
                    for(int i = defaultDialStateList.size()-1; i>=0; i--){
                        stringBuilder.append(defaultDialStateList.get(i).isOpen() ? "1" : "0");
                    }
                    int states = new BigInteger(stringBuilder.toString(), 2).intValue();
                    FissionLogUtils.d("wl", "---内置表盘状态二进制字符串--"+stringBuilder+", 十进制数据："+states);
                    FissionSdkBleManage.getInstance().setDefaultDialState(states);
                }
            }
        });

        rg_all_open.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    recyclerView.setVisibility(View.GONE);
                }
            }
        });

        rg_all_close.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    recyclerView.setVisibility(View.GONE);
                }
            }
        });

        rg_all_choose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    recyclerView.setVisibility(View.VISIBLE);
                }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
