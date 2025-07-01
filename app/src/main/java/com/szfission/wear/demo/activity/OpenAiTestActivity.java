package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.LogUtils;
import com.lw.common.api.data.AiRemoteDataStore;
import com.lw.common.api.enums.AiDomain;
import com.szfission.wear.demo.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * @author chengliuxu
 * @name Android-Fission
 * @class name：com.szfission.wear.demo.activity
 * @class describe
 * @time 2025/3/17 17:28
 * @change
 * @chang time
 * @class describe
 */
public class OpenAiTestActivity extends BaseActivity{
    private TextView btn_chat;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_open_ai_test);

        btn_chat = findViewById(R.id.btn_chat);

        btn_chat.setOnClickListener(v->{
            AiRemoteDataStore.INSTANCE.initialize(AiDomain.DEV_DOMAIN,"BC:16:A7:FC:2D:8F","WATCH S2 Pro","Q1601");
            JSONObject json = new JSONObject();
            try {
                json.put("modelType", "1"); //【1: deepseek v3】【2：deepseek r1】【3：通义千问2.5-7B】
                json.put("prompt", "深圳北站");
                json.put("convId", "BC:16:A7:FC:2D:8F");
                json.put("lang", "zh");
                json.put("enableNetwork", true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
            AiRemoteDataStore.INSTANCE.getInstance().onChat(body).enqueue(new retrofit2.Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        LogUtils.e("请求失败: " + response.code());
                        return;
                    }

                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data:")) {
                                String jsonString = line.substring(5).trim();
                                try {
                                    JSONObject jsonObject = new JSONObject(jsonString);
                                    String content = jsonObject.optString("content", "");
                                    boolean ended = jsonObject.optBoolean("ended", false);

                                    LogUtils.d("解析数据: " + content);

                                    // 如果是最后一条消息，退出
                                    if (ended) {
                                        LogUtils.d("数据传输结束");
                                        break;
                                    }
                                } catch (JSONException e) {
                                    LogUtils.e("JSON 解析错误: " + jsonString);
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (IOException e) {
                        LogUtils.e("读取流错误：" + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {

                }
            });

        });
    }
}
