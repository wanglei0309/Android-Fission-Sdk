package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.constant.JsiCmd;
import com.szfission.wear.demo.R;

public class JsiTestActivity extends BaseActivity {

    private Button btn_question, btn_answer, btn_unauthorized_recording, btn_record_fail, btn_unauthorized_device, btn_sensitive_words, btn_network_error, btn_other_error,btn_stream_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jsi_test);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        btn_question = findViewById(R.id.btn_question);
        btn_answer = findViewById(R.id.btn_answer);
        btn_unauthorized_recording = findViewById(R.id.btn_unauthorized_recording);
        btn_record_fail = findViewById(R.id.btn_record_fail);
        btn_unauthorized_device = findViewById(R.id.btn_unauthorized_device);
        btn_sensitive_words = findViewById(R.id.btn_sensitive_words);
        btn_network_error = findViewById(R.id.btn_network_error);
        btn_other_error = findViewById(R.id.btn_other_error);
        btn_stream_content = findViewById(R.id.btn_stream_content);


        btn_question.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("test 录音结束, 11111,  22222,  3333", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_QUESTION, true);
            }
        });

        btn_answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("test 这就是答案！！", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_ANSWER, true);
            }
        });

        btn_unauthorized_recording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("未授权录音权限", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_UNAUTHORIZED_RECORDING_PERMISSION, true);
            }
        });

        btn_record_fail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("录音失败", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_RECORDING_FAILED, true);
            }
        });

        btn_unauthorized_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("第三方未授权设备", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_UNAUTHORIZED_DEVICES, true);
            }
        });

        btn_sensitive_words.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("敏感词", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_SENSITIVE_WORDS, true);
            }
        });

        btn_network_error.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("网络异常", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_NETWORK_ANOMALY, true);
            }
        });

        btn_other_error.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("其他异常", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_OTHER_ERROR, true);
            }
        });

        btn_stream_content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat("流式数据", JsiCmd.XIAO_DU_AI, JsiCmd.SEND_STREAM_CONTENT, false);
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
