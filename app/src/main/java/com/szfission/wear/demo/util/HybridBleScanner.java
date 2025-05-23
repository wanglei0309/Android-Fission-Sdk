package com.szfission.wear.demo.util;

/**
 * describe:
 * author: wl
 * createTime: 2025/3/2
 */
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 适用于 Android & 非纯鸿蒙系统（如 EMUI） 的 BLE 扫描工具类
 */
public class HybridBleScanner {

    private static final String TAG = "wl";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private android.bluetooth.le.BluetoothLeScanner bleScanner;
    private boolean isScanning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());


    private ScanCallback mScanCallback;

    public HybridBleScanner(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }

    public void setScanCallback(ScanCallback scanCallback) {
        this.mScanCallback = scanCallback;
    }

    /**
     * 开始扫描 BLE 设备
     */
    @SuppressLint("MissingPermission")
    public void startScan() {
        if (isScanning) {
            Log.d(TAG, "BLE 扫描已在进行中");
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "蓝牙未开启，无法扫描");
            return;
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Log.e(TAG, "BLE 扫描器不可用");
            return;
        }

        Log.d(TAG, "开始扫描 BLE 设备...");

        if(mScanCallback == null){
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    String deviceName = result.getDevice().getName() != null ? result.getDevice().getName() : "未知设备";
                    String macAddress = result.getDevice().getAddress();
                    Log.d(TAG, "发现设备: " + deviceName + " [" + macAddress + "]");
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Log.e(TAG, "BLE 扫描失败，错误码：" + errorCode);
                }
            };
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // 高频扫描模式
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        // 可以在这里添加过滤条件，比如指定特定设备的 UUID
        // filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000180D-0000-1000-8000-00805F9B34FB")).build());

        bleScanner.startScan(filters, scanSettings, mScanCallback);
        isScanning = true;

        // 10 秒后自动停止扫描
        handler.postDelayed(this::stopScan, 6000);
    }

    /**
     * 停止扫描 BLE 设备
     */
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (!isScanning) {
            Log.d(TAG, "BLE 扫描未启动");
            return;
        }

        Log.d(TAG, "停止扫描 BLE 设备");
        if (bleScanner != null && mScanCallback != null) {
            bleScanner.stopScan(mScanCallback);
        }
        isScanning = false;
    }
}
