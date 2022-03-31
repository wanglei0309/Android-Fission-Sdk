package com.szfission.wear.demo.Listener;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.blankj.utilcode.util.LogUtils;

public class MyPhoneStateListener extends PhoneStateListener {
    protected CallListener listener;
    private static final String TAG = "MyPhoneStateListener";

    @Override
    public void onCallStateChanged(int state, String phoneNumber) {
        super.onCallStateChanged(state, phoneNumber);
        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                LogUtils.d(TAG ,"电话挂断..."+phoneNumber);
                listener.onCallIdle();
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                LogUtils.d(TAG ,"正在通话..."+phoneNumber);
                listener.onCallOffHook();
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                LogUtils.d(TAG ,"电话响铃..."+phoneNumber);
                listener.onCallRinging();
                break;
        }
    }

    //回调
    public void setCallListener(CallListener callListener) {
        this.listener = callListener;
    }

    //回调接口
    public interface CallListener {
        void onCallIdle();
        void onCallOffHook();
        void onCallRinging();
    }
}
