package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;

import com.szfission.wear.demo.R;

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
