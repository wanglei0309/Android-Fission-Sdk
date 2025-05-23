package com.szfission.wear.demo.activity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.JsAiJsonResult;
import com.fission.wear.sdk.v2.bean.JsAiVoiceJsonResult;
import com.fission.wear.sdk.v2.utils.AFlashChatGptUtils;
import com.fission.wear.sdk.v2.utils.BaiDuAiUtils;
import com.fission.wear.sdk.v2.utils.ChatGptUtils;
import com.fission.wear.sdk.v2.utils.ColorUtils;
import com.fission.wear.sdk.v2.utils.FissionDialUtil;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.HsDialUtils;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.dialogx.interfaces.OnBackPressedListener;
import com.szfission.wear.demo.App;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.ImageScalingUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;

import java.io.File;
import java.util.List;

public class BaiDuAiTestActivity extends BaseActivity {

    private Button btn_open_ai, btn_send_question_data, btn_send_answer_data, btn_send_tts_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baidu_ai);
        String[] testArray = getResources().getStringArray(R.array.haisi_test_array);
        setTitle(testArray[2]);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        BaiDuAiUtils.setBdAiVoiceListener(new BaiDuAiUtils.BdAiVoiceListener() {
            @Override
            public void onChat(String question, String answer) {

                JsAiJsonResult resultQ = new JsAiJsonResult(JsAiJsonResult.TYPE_QUESTION, question);

                JsAiJsonResult resultA = new JsAiJsonResult(JsAiJsonResult.TYPE_ANSWER, answer);

                FissionSdkBleManage.getInstance().sendQuestionData(GsonUtils.toJson(resultQ));

                FissionSdkBleManage.getInstance().sendAnswerData(GsonUtils.toJson(resultA));

                BaiDuAiUtils.startTTS(BaiDuAiTestActivity.this, answer);
            }

            @Override
            public void onVoiceSearch(String text) {
                JsAiVoiceJsonResult voiceJsonResult = new JsAiVoiceJsonResult(JsAiVoiceJsonResult.TYPE_VOICE_CONTENT, text);
                if(TextUtils.isEmpty(text)){
                    voiceJsonResult.setCode(1); //语音搜索失败
                }else{
                    voiceJsonResult.setCode(0); //语音搜索成功
                }
                FissionSdkBleManage.getInstance().sendQuestionData(GsonUtils.toJson(voiceJsonResult));
            }

            @Override
            public void onCreateDial(String text) {

                FissionLogUtils.d("wl", "---Ai创建表盘--"+text);
            }

            @Override
            public void onVoiceFile(File file) {

            }

            @Override
            public void onSpeechResult(String result, String type) {

            }

            @Override
            public void onError(int code, String msg) {

            }
        });

        ChatGptUtils.getInstance().setGptAiVoiceListener(new ChatGptUtils.GptAiVoiceListener() {
            @Override
            public void onChat(String question, String answer) {
                JsAiJsonResult resultQ = new JsAiJsonResult(JsAiJsonResult.TYPE_QUESTION, question);

                JsAiJsonResult resultA = new JsAiJsonResult(JsAiJsonResult.TYPE_ANSWER, answer);

                FissionSdkBleManage.getInstance().sendQuestionData(GsonUtils.toJson(resultQ));

                FissionSdkBleManage.getInstance().sendAnswerData(GsonUtils.toJson(resultA));
            }

            @Override
            public void onCreateDial(List<String> imgPaths) {

                FissionLogUtils.d("wl", "Ai表盘生成图片路径:"+imgPaths);
            }

            @Override
            public void onError(int code, String msg) {

            }

            @Override
            public void onSpeechResult(String result) {

            }
        });

        AFlashChatGptUtils.getInstance().setGptAiVoiceListener(new AFlashChatGptUtils.GptAiVoiceListener() {
            @Override
            public void onChat(String question, String answer) {
                JsAiJsonResult resultQ = new JsAiJsonResult(JsAiJsonResult.TYPE_QUESTION, question);

                JsAiJsonResult resultA = new JsAiJsonResult(JsAiJsonResult.TYPE_ANSWER, answer);

                FissionSdkBleManage.getInstance().sendQuestionData(GsonUtils.toJson(resultQ));

                FissionSdkBleManage.getInstance().sendAnswerData(GsonUtils.toJson(resultA));
            }

            @Override
            public void onCreateDial(List<String> imgPaths) {
                // /storage/emulated/0/Android/data/com.szfission.wear.demo/files/1733901802514.jpeg
                String imagePath = imgPaths.get(0);
                Bitmap bitmap = ImageUtils.getBitmap(imagePath);
                int colorValue = ColorUtils.getColorBasedOnBackground(bitmap);
                FissionDialUtil.DialModel dialModel = new FissionDialUtil.DialModel();
                dialModel.setDialShape(App.mHardWareInfo.getDialShape());
                dialModel.setDialWidth(App.mHardWareInfo.getDeviceWidth());
                dialModel.setDialHeight(App.mHardWareInfo.getDeviceHigh());
                dialModel.setBackgroundImage(bitmap);
                dialModel.setDialPosition(1);
                dialModel.setPreImageWidth(App.mHardWareInfo.getThumbnailWidth());
                dialModel.setPreImageHeight(App.mHardWareInfo.getThumbnailHigh());
                dialModel.setDialStyleColor(colorValue);
                Bitmap thumbBitmap = ImageScalingUtil.extractMiniThumb(dialModel.getBackgroundImage(),
                        dialModel.getPreImageWidth(), dialModel.getPreImageHeight());
                dialModel.setPreviewImage(thumbBitmap);
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        WaitDialog.show("正在打包表盘...")
                                .setCancelable(false)
                                .setOnBackPressedListener(new OnBackPressedListener<WaitDialog>() {
                                    @Override
                                    public boolean onBackPressed(WaitDialog dialog) {
                                        return false;
                                    }
                                });
                        byte[] resultData = HsDialUtils.getInstance().getSimpleDialBinFile(BaiDuAiTestActivity.this, dialModel);
                        WaitDialog.dismiss();
//                        String filePath = Environment.getExternalStorageDirectory()+"/custom_dial_hs.bin";
//                        FileUtils.createFileByDeleteOldFile(filePath);
//                        FileIOUtils.writeFileFromBytesByStream (filePath, resultData);
                        ToastUtils.showLong("自定义表盘打包成功");
                        FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_DIAL_DATA_V2);
                    }
                }.start();
            }

            @Override
            public void onSpeechResult(String result, String type) {

            }

            @Override
            public void onError(int code, String msg) {

            }
        });

        btn_open_ai = findViewById(R.id.btn_open_ai);
        btn_send_question_data = findViewById(R.id.btn_send_question_data);
        btn_send_answer_data = findViewById(R.id.btn_send_answer_data);
        btn_send_tts_data = findViewById(R.id.btn_send_tts_data);

        btn_open_ai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBaiDuAi();
            }
        });

        btn_send_question_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendQuestionData();
            }
        });

        btn_send_answer_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendAnswerData();
            }
        });

        btn_send_tts_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendTtsData();
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

    private void openBaiDuAi() {
        FissionSdkBleManage.getInstance().openBaiDuAi();
    }

    private void sendQuestionData() {
        FissionSdkBleManage.getInstance().sendQuestionData("hello world");
    }

    private void sendAnswerData() {
        FissionSdkBleManage.getInstance().sendAnswerData("yes, gm");
    }

    private void sendTtsData() {
        FissionSdkBleManage.getInstance().sendTtsData("yes, how are you");
    }
}
