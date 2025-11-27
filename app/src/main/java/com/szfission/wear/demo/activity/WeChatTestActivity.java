package com.szfission.wear.demo.activity;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.bumptech.glide.Glide;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionJsiDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.JsiCmd;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.utils.WeChatManage;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.util.RxTimerUtil;

import java.io.File;

public class WeChatTestActivity extends BaseActivity {

    private ImageView iv_qrcode;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wechat_test);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        iv_qrcode = findViewById(R.id.iv_qrcode);

        init();
    }

    private void init(){
//        if(!TextUtils.isEmpty(SPUtils.getInstance().getString(SpKey.LAST_MAC))){
//            WeChatManage.getInstance().getLoginQrCode(this);
//            new RxTimerUtil().timer(5000, new RxTimerUtil.RxAction() {
//                @Override
//                public void action(long number) {
//                    if(WeChatManage.getInstance().getLoginBitmap()!=null){
//                        Glide.with(WeChatTestActivity.this).load(WeChatManage.getInstance().getLoginBitmap()).override(300, 300).into(iv_qrcode);
//                    }
//                }
//            });
//        }else{
//            ToastUtils.showShort("请先连接手表！！");
//        }
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
