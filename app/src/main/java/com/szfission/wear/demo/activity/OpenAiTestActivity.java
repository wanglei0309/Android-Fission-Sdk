package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.ToastUtils;
import com.szfission.wear.demo.R;

/**
 * AI 接口测试页。完整 ChatApi 依赖未接入时仅作占位入口。
 */
public class OpenAiTestActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_ai_test);

        TextView btnChat = findViewById(R.id.btn_chat);
        btnChat.setOnClickListener(v ->
                ToastUtils.showShort("ChatApi SDK 未接入，请参考 Android-Fission 配置 com.github.artillerymans:ChatApi"));
    }
}
