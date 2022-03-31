package com.szfission.wear.demo.Listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.szfission.wear.demo.FissionSdk;

public class CustomBroadcastReceiver  extends BroadcastReceiver {
    protected CallListener listener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())){
            String state          = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            LogUtils.d("clx", "来电状态发生变化" + state,"电话号码"+incomingNumber);
            if (incomingNumber!=null){
                FissionSdk.getInstance().sendCallPhone(state,incomingNumber);
            }
        }

    }

    //回调
    public void setCallListener(CallListener callListener) {
        this.listener = callListener;
    }

    //回调接口
    public interface CallListener {
        void onCallIdle(String number);
        void onCallOffHook(String number);
        void onCallRinging(String number);
    }
}
