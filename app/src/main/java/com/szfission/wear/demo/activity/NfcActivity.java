package com.szfission.wear.demo.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.CacheDoubleUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.example.application.SlmM1Crack;
import com.example.application.mytoos;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.FssStatus;
import com.fission.wear.sdk.v2.bean.NfcCardInfo;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.config.BleComConfig;
import com.fission.wear.sdk.v2.constant.CacheDoubleKey;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.util.NfcReaderUtil;
import com.szfission.wear.sdk.bean.AppMessageBean;
import com.szfission.wear.sdk.util.FsLogUtil;
import com.szfission.wear.sdk.util.RxTimerUtil;

import java.io.IOException;
import java.util.List;
import java.util.Random;


public class NfcActivity extends BaseActivity {

    static {
        System.loadLibrary("slm_m1_crack");
    }

    TextView tv_tips;

    Button btn_get_all_card_info, btn_create_access_card, btn_send_copy_card_data, btn_activate_card, btn_get_activate_card, btn_modify_specified_card;

    Button btn_create_blank_card, btn_delete_all_card, btn_delete_specified_card, btn_disable_current_card, btn_switch_reading_mode, btn_get_raw_data, btn_push_raw_data;

    EditText ed_card_index;

    private NfcCardInfo mNfcCardInfo;

    private List<NfcCardInfo> nfcCardInfoList;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    byte[] keys;
    byte[] crack_data;
    String keys_string;

    private FissionBigDataCmdResultListener mFissionBigDataCmdResultListener = new FissionBigDataCmdResultListener() {
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
        public void getAllNfcCardInfo(List<NfcCardInfo> cardInfos) {
            super.getAllNfcCardInfo(cardInfos);
            nfcCardInfoList = cardInfos;
            tv_tips.setText("Nfc tips: "+cardInfos);
        }

        @Override
        public void getActivateNfcCardInfo(NfcCardInfo nfcCardInfo) {
            super.getActivateNfcCardInfo(nfcCardInfo);
            mNfcCardInfo = nfcCardInfo;
            tv_tips.setText("Nfc tips: "+nfcCardInfo);
        }

        @Override
        public void getNfcErrorCode(int errorCode) {
            super.getNfcErrorCode(errorCode);
            switch (errorCode){
                case 1:
                    tv_tips.setText("Nfc tips: 错误码："+errorCode+", 错误信息：没有卡片");
                    break;

                case 2:
                    tv_tips.setText("Nfc tips: 错误码："+errorCode+", 错误信息：无激活的卡片");
                    break;

                case 3:
                    tv_tips.setText("Nfc tips: 错误码："+errorCode+", 错误信息：已有相同ID的卡片");
                    break;

                case 4:
                    tv_tips.setText("Nfc tips: 错误码："+errorCode+", 错误信息：没有此ID的卡片");
                    break;

                case 5:
                    tv_tips.setText("Nfc tips: 错误码："+errorCode+", 错误信息：创建空白卡失败");
                    break;

                case 6:
                    tv_tips.setText("Nfc tips: 错误码："+errorCode+", 错误信息：删除全部卡片失败");
                    break;

            }

        }

        @Override
        public void readDecryptionKeyData(String keyData) {
            super.readDecryptionKeyData(keyData);
            FissionLogUtils.d("wl", "----readDecryptionKeyData----"+keyData);
            crack_data = mytoos.hexStrToByteArray(keyData);
            keys = SlmM1Crack.mf_crack_api1(crack_data, (byte) crack_data.length);
            keys_string = mytoos.byteArrayToHexStr_N(keys, (byte) (keys[0]+1));
            FissionLogUtils.d("wl", "-----秘钥破解结果--"+keys_string);
            FissionSdkBleManage.getInstance().setDecryptionKeyResult(keys_string);
        }

        @Override
        public void disableCurrentCard() {
            super.disableCurrentCard();
            tv_tips.setText("Nfc tips: 禁用当前卡片成功");
        }

        @Override
        public void getSpecifiedCardRawData(NfcCardInfo nfcCardInfo) {
            super.getSpecifiedCardRawData(nfcCardInfo);
            CacheDoubleUtils.getInstance().put("nfcCard", nfcCardInfo);
        }
    };

    private FissionAtCmdResultListener mFissionAtCmdResultListener = new FissionAtCmdResultListener() {
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
        public void fssSuccess(FssStatus fssStatus) {
            super.fssSuccess(fssStatus);
            if(fssStatus.getFssType() == 54){
                FissionLogUtils.d("wl", "---请求远程破解nfc秘钥--");
                FissionSdkBleManage.getInstance().readDecryptionKeyData();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        tv_tips = findViewById(R.id.tv_tips);
        btn_get_all_card_info = findViewById(R.id.btn_get_all_card_info);
        btn_create_access_card = findViewById(R.id.btn_create_access_card);
        btn_send_copy_card_data = findViewById(R.id.btn_send_copy_card_data);
        btn_activate_card = findViewById(R.id.btn_activate_card);
        ed_card_index = findViewById(R.id.ed_card_index);
        btn_get_activate_card = findViewById(R.id.btn_get_activate_card);
        btn_modify_specified_card = findViewById(R.id.btn_modify_specified_card);
        btn_create_blank_card = findViewById(R.id.btn_create_blank_card);
        btn_delete_all_card = findViewById(R.id.btn_delete_all_card);
        btn_delete_specified_card = findViewById(R.id.btn_delete_specified_card);
        btn_disable_current_card = findViewById(R.id.btn_disable_current_card);
        btn_switch_reading_mode = findViewById(R.id.btn_switch_reading_mode);
        btn_get_raw_data = findViewById(R.id.btn_get_raw_data);
        btn_push_raw_data = findViewById(R.id.btn_push_raw_data);

        setTitle(R.string.FUNC_NFC_FUNCTION_MODULE);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // 初始化 NFC 适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "设备不支持NFC", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 创建 NFC 监听 PendingIntent
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE
        );


        addCmdResultListener(mFissionBigDataCmdResultListener);

        addCmdResultListener(mFissionAtCmdResultListener);

        btn_get_all_card_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().getAllNfcCardInfo();
            }
        });

        btn_create_access_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNfcCardInfo == null){
                    ToastUtils.showShort("请将门禁卡贴在手机背面！！");
                }else{
                    FissionSdkBleManage.getInstance().createNfcDuplicateCard(mNfcCardInfo);
                }
            }
        });

        btn_send_copy_card_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNfcCardInfo == null){
                    ToastUtils.showShort("请将门禁卡贴在手机背面！！");
                }else{
                    FissionSdkBleManage.getInstance().setNfcDuplicateCard(mNfcCardInfo);
                }
            }
        });

        btn_activate_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String index = ed_card_index.getText().toString().trim();
                if(TextUtils.isEmpty(index)){
                    ToastUtils.showShort("请输入要激活卡片的位置！！");
                    return;
                }
                if(nfcCardInfoList == null || nfcCardInfoList.isEmpty()){
                    ToastUtils.showShort("请先获取所有的卡片信息！！");
                    return;
                }
                if(Integer.parseInt(index)>=nfcCardInfoList.size()){
                    ToastUtils.showShort("输入的要激活的卡片位置有误！！");
                    return;
                }
                NfcCardInfo nfcCardInfo = nfcCardInfoList.get(Integer.parseInt(index));
                FissionSdkBleManage.getInstance().activateSpecifiedCard(nfcCardInfo);
            }
        });

        btn_get_activate_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().getActivateNfcCardInfo();
            }
        });

        btn_modify_specified_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String index = ed_card_index.getText().toString().trim();
                if(TextUtils.isEmpty(index)){
                    ToastUtils.showShort("请输入要修改片的位置！！");
                    return;
                }
                if(nfcCardInfoList == null || nfcCardInfoList.isEmpty()){
                    ToastUtils.showShort("请先获取所有的卡片信息！！");
                    return;
                }
                if(Integer.parseInt(index)>=nfcCardInfoList.size()){
                    ToastUtils.showShort("输入的要修改的卡片位置有误！！");
                    return;
                }
                NfcCardInfo nfcCardInfo = nfcCardInfoList.get(Integer.parseInt(index));
                nfcCardInfo.setName("修改门卡名称");
                nfcCardInfo.setBgImage(3);
                FissionSdkBleManage.getInstance().modifySpecifiedCard(nfcCardInfo);
            }
        });

        btn_create_blank_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NfcCardInfo nfcCardInfo = new NfcCardInfo(0, "01020304", "0400", "08", "空白卡", 0);
                FissionSdkBleManage.getInstance().createBlankCard(nfcCardInfo);
            }
        });

        btn_disable_current_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().disableCurrentCard();
            }
        });

        btn_delete_all_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().deleteAllCard();
            }
        });

        btn_delete_specified_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String index = ed_card_index.getText().toString().trim();
                if(TextUtils.isEmpty(index)){
                    ToastUtils.showShort("请输入要删除卡片的位置！！");
                    return;
                }
                if(nfcCardInfoList == null || nfcCardInfoList.isEmpty()){
                    ToastUtils.showShort("请先获取所有的卡片信息！！");
                    return;
                }
                if(Integer.parseInt(index)>=nfcCardInfoList.size()){
                    ToastUtils.showShort("输入的要删除的卡片位置有误！！");
                    return;
                }
                NfcCardInfo nfcCardInfo = nfcCardInfoList.get(Integer.parseInt(index));
                FissionSdkBleManage.getInstance().deleteSpecifiedCard(nfcCardInfo);
            }
        });

        btn_switch_reading_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FissionSdkBleManage.getInstance().switchNfcCardReadingMode();
            }
        });

        btn_get_raw_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String index = ed_card_index.getText().toString().trim();
                if(TextUtils.isEmpty(index)){
                    ToastUtils.showShort("请输入要读取原始数据卡片的位置！！");
                    return;
                }
                if(nfcCardInfoList == null || nfcCardInfoList.isEmpty()){
                    ToastUtils.showShort("请先获取所有的卡片信息！！");
                    return;
                }
                if(Integer.parseInt(index)>=nfcCardInfoList.size()){
                    ToastUtils.showShort("输入的要读取原始数据的卡片位置有误！！");
                    return;
                }
                NfcCardInfo nfcCardInfo = nfcCardInfoList.get(Integer.parseInt(index));
                FissionSdkBleManage.getInstance().getSpecifiedCardRawData(nfcCardInfo);
            }
        });

        btn_push_raw_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NfcCardInfo cardInfo = (NfcCardInfo)CacheDoubleUtils.getInstance().getSerializable("nfcCard");
                if(cardInfo == null){
                    ToastUtils.showShort("请先读取NFC卡片原始数据！！");
                }else{
                    FissionSdkBleManage.getInstance().createNfcDuplicateCard(cardInfo);
                    FissionSdkBleManage.getInstance().setNfcDuplicateCard(cardInfo);
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
        removeCmdResultListener(mFissionBigDataCmdResultListener);
        removeCmdResultListener(mFissionAtCmdResultListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 解析 NFC 标签
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            String tagData = NfcReaderUtil.dumpTagData(tag);
            tv_tips.setText("Nfc tips: "+tagData);
            mNfcCardInfo = NfcReaderUtil.dumpTagDataToNfcCardInfo(tag);
            mNfcCardInfo.setType(0);
            mNfcCardInfo.setName("测试门禁卡"+new Random().nextInt(24));
            mNfcCardInfo.setBgImage(new Random().nextInt(5));
            FissionLogUtils.d("wl", "----读卡信息-----"+mNfcCardInfo);
            byte[] data = NfcReaderUtil.readMifareClassic(tag);
            mNfcCardInfo.setData(data);
        }
    }

}
