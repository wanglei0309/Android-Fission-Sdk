package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.UserInfo;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

public class SetUserInfoActivity extends BaseActivity {

    EditText etId;

    EditText etNickname;

    EditText etAge;

    EditText etHeight;

    EditText etWeight;

    EditText etStrideLength;

    RadioButton rbMale;

    RadioButton rbFemale;

    EditText etTimeZone;

    Button btn_get, btn_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_user_info);
        setTitle(R.string.title_profile);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        etId = findViewById(R.id.etId);
        etNickname = findViewById(R.id.etNickname);
        etAge = findViewById(R.id.et_age);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etStrideLength = findViewById(R.id.et_stride_length);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        etTimeZone = findViewById(R.id.etTimeZone);
        btn_get = findViewById(R.id.btn_get);
        btn_send = findViewById(R.id.btn_send);

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


        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionBigDataCmdResultListener() {
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
                showToast(errorMsg);
                dismissProgress();
            }

            @Override
            public void setUserInfo() {
                super.setUserInfo();
                dismissProgress();
                showSuccessToast();
            }

            @Override
            public void getUserInfo(UserInfo userInfo) {
                super.getUserInfo(userInfo);
                dismissProgress();
                String content = "\n结构体版本：" + userInfo.getBodyVersion() +
                        "\n用户ID：" + userInfo.getUserId() +
                        "\n用户昵称：" + userInfo.getNickname() +
                        "\n身高：" + userInfo.getHeight() +
                        "\n体重：" + userInfo.getWeight() +
                        "\n时区：" + userInfo.getTimeZone() +
                        "\n性别：" + userInfo.getSex() +
                        "\n年龄：" + userInfo.getAge() +
                        "\n步幅：" + userInfo.getStride();
                addLog(R.string.FUNC_GET_PERSONAL_INFO, content);
                etId.setText(String.valueOf(userInfo.getUserId()));
                etNickname.setText(userInfo.getNickname());
                etHeight.setText(String.valueOf(userInfo.getHeight()));
                etWeight.setText(String.valueOf(userInfo.getWeight()));
                etTimeZone.setText(String.valueOf(userInfo.getTimeZone()));
                if (userInfo.getSex() == 0) {
                    rbMale.setChecked(true);
                } else {
                    rbFemale.setChecked(true);
                }
                etAge.setText(String.valueOf(userInfo.getAge()));
                etStrideLength.setText(String.valueOf(userInfo.getStride()));
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

    private void get() {
        getData();
        showProgress();
    }


    private void send() {
        String userId = etId.getText().toString();
        String nickname = etNickname.getText().toString();
        String timeZone = etTimeZone.getText().toString();
        String age = etAge.getText().toString();
        String height = etHeight.getText().toString();
        String weight = etWeight.getText().toString();
        String strideLength = etStrideLength.getText().toString();
        if (userId.isEmpty()) {
            Toast.makeText(this, "请输入用户ID", Toast.LENGTH_SHORT).show();
            return;
        }
        int userIdTag = Integer.parseInt(userId);
        if (userIdTag <= 0) {
            Toast.makeText(this, "请输入有效的ID", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nickname.isEmpty()) {
            Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nickname.getBytes().length > 32) {
            Toast.makeText(this, "昵称过长", Toast.LENGTH_SHORT).show();
            return;
        }
        if (timeZone.isEmpty()) {
            Toast.makeText(this, "请输入时区", Toast.LENGTH_SHORT).show();
            return;
        }
        if (age.isEmpty()) {
            Toast.makeText(this, "请输入年龄", Toast.LENGTH_SHORT).show();
            return;
        }
        if (height.isEmpty()) {
            Toast.makeText(this, "请输入身高", Toast.LENGTH_SHORT).show();
            return;
        }
        if (weight.isEmpty()) {
            Toast.makeText(this, "请输入体重", Toast.LENGTH_SHORT).show();
            return;
        }
        if (strideLength.isEmpty()) {
            Toast.makeText(this, "请输入步幅", Toast.LENGTH_SHORT).show();
            return;
        }
        int sex = rbMale.isChecked() ? 0 : 1;
        showProgress();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userIdTag);
        userInfo.setNickname(nickname);
        userInfo.setHeight(Integer.parseInt(height));
        userInfo.setWeight(Integer.parseInt(weight));
        userInfo.setTimeZone(Integer.parseInt(timeZone));
        userInfo.setSex(sex);
        userInfo.setAge(Integer.parseInt(age));
        userInfo.setStride(Integer.parseInt(strideLength));
        FissionSdkBleManage.getInstance().setUserInfo(userInfo);

//        AnyWear.setUserInfo(userInfo, new OnSmallDataCallback() {
//            @Override
//            public void OnEmptyResult() {
//                dismissProgress();
//                showSuccessToast();
//            }
//            @Override
//            public void OnError(String msg) {
//                showToast(msg);
//                dismissProgress();
//            }
//        });
    }


    private void getData() {
        FissionSdkBleManage.getInstance().getUserInfo();

//        AnyWear.getUserInfo(new BigDataCallBack() {
//            @Override
//            public void OnUserInfo(UserInfo userInfo) {
//                dismissProgress();
//                String content = "\n结构体版本：" + userInfo.getBodyVersion() +
//                        "\n用户ID：" + userInfo.getUserId() +
//                        "\n用户昵称：" + userInfo.getNickname() +
//                        "\n身高：" + userInfo.getHeight() +
//                        "\n体重：" + userInfo.getWeight() +
//                        "\n时区：" + userInfo.getTimeZone() +
//                        "\n性别：" + userInfo.getSex() +
//                        "\n年龄：" + userInfo.getAge() +
//                        "\n步幅：" + userInfo.getStride();
//                addLog(R.string.FUNC_GET_PERSONAL_INFO, content);
//                etId.setText(String.valueOf(userInfo.getUserId()));
//                etNickname.setText(userInfo.getNickname());
//                etHeight.setText(String.valueOf(userInfo.getHeight()));
//                etWeight.setText(String.valueOf(userInfo.getWeight()));
//                etTimeZone.setText(String.valueOf(userInfo.getTimeZone()));
//                if (userInfo.getSex() == 0) {
//                    rbMale.setChecked(true);
//                } else {
//                    rbFemale.setChecked(true);
//                }
//                etAge.setText(String.valueOf(userInfo.getAge()));
//                etStrideLength.setText(String.valueOf(userInfo.getStride()));
//            }
//
//            @Override
//            public void OnEmpty(String cmdId) {
//                addLog(R.string.FUNC_GET_PERSONAL_INFO, "记录为空");
//                dismissProgress();
//            }
//
//            @Override
//            public void OnError(String msg) {
//                showToast(msg);
//                dismissProgress();
//            }
//        });
    }


}
