package com.szfission.wear.demo;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {
    private static final String SHARED_NAME = "anyWear";
    private static SharedPreferencesUtil mSharedPreferencesUtil;
    private SharedPreferences mPreferences;
    String FISSION_BIND_KEY = "fission_bind_key";//裂变请求设备绑定密钥
    String BLUETOOTH_ADDRESS = "bluetooth_address";// 设备连接地址。下次自动重连需要
    String BLUETOOTH_NAME = "bluetooth_name";// 设备连接地址。下次自动重连需要


    public static SharedPreferencesUtil getInstance() {
        if (mSharedPreferencesUtil == null) {
            synchronized (SharedPreferencesUtil.class) {
                if (mSharedPreferencesUtil == null) {
                    mSharedPreferencesUtil = new SharedPreferencesUtil();
                }
            }
        }
        return mSharedPreferencesUtil;
    }

    public void init(Context context) {
        mPreferences = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
    }


    public void setDistance(String distance, String key) {
        mPreferences.edit().putString(key, distance).apply();
    }

    public String getDistance(String key) {
        return mPreferences.getString(key, "");
    }


    public void setStep(int step, String key) {
        mPreferences.edit().putInt(key, step).apply();
    }

    public int getStep(String key) {
        return mPreferences.getInt(key, 0);
    }


    public void setFissionKey(String key) {
        mPreferences.edit().putString(FISSION_BIND_KEY, key).apply();
    }

    public String getFissionKey() {
        return mPreferences.getString(FISSION_BIND_KEY, "");
    }



    public void setBluetoothAddress(String bluetoothAddress) {
        mPreferences.edit().putString(BLUETOOTH_ADDRESS, bluetoothAddress).apply();
    }

    public String getBluetoothAddress() {
        return mPreferences.getString(BLUETOOTH_ADDRESS, "");
    }

    public void setBluetoothName(String bluetoothName) {
        mPreferences.edit().putString(BLUETOOTH_NAME, bluetoothName).apply();
    }

    public String getBluetoothName() {
        return mPreferences.getString(BLUETOOTH_NAME, "");
    }
}
