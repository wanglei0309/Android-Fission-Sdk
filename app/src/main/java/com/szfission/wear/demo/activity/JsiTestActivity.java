package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionJsiDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.JsiCmd;
import com.szfission.wear.demo.R;

public class JsiTestActivity extends BaseActivity {

    private Button btn_question, btn_answer, btn_unauthorized_recording, btn_record_fail, btn_unauthorized_device, btn_sensitive_words, btn_network_error, btn_other_error,btn_stream_content;
    private Button btn_original_content, btn_translation_content, btn_in_translate_page;
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
        btn_original_content = findViewById(R.id.btn_original_content);
        btn_translation_content = findViewById(R.id.btn_translation_content);
        btn_in_translate_page = findViewById(R.id.btn_in_translate_page);

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

        btn_original_content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByTranslate("世界，你好", JsiCmd.AI_TRANSLATE, JsiCmd.SEND_ORIGINAL_CONTENT, true);
            }
        });

        btn_translation_content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByTranslate("Hello word", JsiCmd.AI_TRANSLATE, JsiCmd.SEND_TRANSLATION_CONTENT, true);
            }
        });

        btn_in_translate_page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().sendJsiCmdByTranslate("1", JsiCmd.AI_TRANSLATE, JsiCmd.SEND_IN_TRANSLATE_PAGE, false);
            }
        });

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionJsiDataCmdResultListener() {
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
            public void getAppStates() {
                super.getAppStates();
                FissionSdkBleManage.getInstance().notifyAppStates(1, 0);
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
