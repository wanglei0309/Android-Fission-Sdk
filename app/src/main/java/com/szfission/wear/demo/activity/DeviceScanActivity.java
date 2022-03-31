package com.szfission.wear.demo.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.BleScanResultListener;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.szfission.wear.demo.BluetoothDeviceEntity;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.constant.WalleAction;
import com.szfission.wear.sdk.util.BleUtil;
import com.szfission.wear.sdk.util.FsLogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 扫描蓝牙设备
 */
public class DeviceScanActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private ListView lvContent;
    private ImageButton ibBack;
    private Button btnOption;
    private TextView tvTitle;
    private ProgressBar pbLoad;
    private List<BluetoothDeviceEntity> bluetoothDeviceEntityList;
    private boolean showSignalStrength; // 信号强度
    private String[] scanFilterName ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.activity_scan_ble);
        lvContent = findViewById(R.id.lv_content);
        ibBack = findViewById(R.id.ib_back);
        btnOption = findViewById(R.id.btn_option);
        tvTitle = findViewById(R.id.tv_title);
        pbLoad = findViewById(R.id.pb_load);

        String title = getIntent().getStringExtra("title");
        showSignalStrength = getIntent().getBooleanExtra("showSignalStrength", true);
        scanFilterName = getIntent().getStringArrayExtra("scanFilterName");

        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.walle_ble_bind_device);
        }
        tvTitle.setText(title);

        lvContent.setOnItemClickListener(this);
        btnOption.setOnClickListener(this);
        ibBack.setOnClickListener(this);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.walle_ble_ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        bluetoothDeviceEntityList = new ArrayList<>();
        mLeDeviceListAdapter = new LeDeviceListAdapter(bluetoothDeviceEntityList);
        lvContent.setAdapter(mLeDeviceListAdapter);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WalleAction.SCAN_RESULT);
        intentFilter.addAction(WalleAction.SCAN_TIMEOUT);
        registerReceiver(scanResultBroadcastReceiver, intentFilter);


        bluetoothDeviceEntityList.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();
        validPermission();
        searchBleDevices();
    }

    private void searchBleDevices(){
        FissionSdkBleManage.getInstance().scanBleDevices(new BleScanResultListener() {
                                                   @Override
                                                   public void onScanResult(ScanResult scanResult) {
                                                       if (scanResult != null) {
                                                           BluetoothDeviceEntity device = new BluetoothDeviceEntity();
                                                           device.setRssi(scanResult.getRssi());
                                                           device.setName(scanResult.getBleDevice().getName());
                                                           device.setAddress(scanResult.getBleDevice().getMacAddress());
                                                           addBluetoothDeviceEntity(device);
                                                       }
                                                   }

                                                   @Override
                                                   public void onScanFailure(Throwable throwable) {
                                                       if (throwable instanceof BleScanException) {
                                                           int reason = ((BleScanException) throwable).getReason();
                                                           LogUtils.d("wl", "Ble扫描异常码："+reason);
                                                       }

                                                   }

                                                   @Override
                                                   public void onScanFinish() {
                                                       refreshOptionStatus();
                                                   }
                                               }, null, new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build(),
                new ScanFilter.Builder()
//                            .setDeviceAddress("B4:99:4C:34:DC:8B")
                        // add custom filters if needed
                        .build());
    }

    private void stopScanBleDevices(){
        FissionSdkBleManage.getInstance().stopScanBleDevices();
    }

    private void refreshOptionStatus() {
        if (!FissionSdkBleManage.getInstance().isScanning()) {
            btnOption.setText(getString(R.string.walle_ble_scan));
            pbLoad.setVisibility(View.GONE);
        } else {
            btnOption.setText(getString(R.string.walle_ble_stop));
            pbLoad.setVisibility(View.VISIBLE);
        }
    }
    private void validPermission() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            PermissionUtils.permission(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION).callback(new PermissionUtils.FullCallback() {
                @Override
                public void onGranted(@NonNull List<String> granted) {
                    searchBleDevices();
                    refreshOptionStatus();
                }

                @Override
                public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                    Toast.makeText(DeviceScanActivity.this,"没有权限,请检查权限",Toast.LENGTH_SHORT).show();
                }
            }).request();
        }else{
            PermissionUtils.permission(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN).callback(new PermissionUtils.FullCallback() {
                @Override
                public void onGranted(@NonNull List<String> granted) {
                    searchBleDevices();
                    refreshOptionStatus();
                }

                @Override
                public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                    Toast.makeText(DeviceScanActivity.this,"没有权限,请检查权限",Toast.LENGTH_SHORT).show();
                }
            }).request();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        FsLogUtil.d("PERMISSION"+PackageManager.PERMISSION_DENIED);
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION && grantResults.length > 0) {
            if (grantResults[0]  == PackageManager.PERMISSION_DENIED) {
                searchBleDevices();
                refreshOptionStatus();
            } else if  (grantResults[0]  == PackageManager.PERMISSION_GRANTED) {
                searchBleDevices();
                refreshOptionStatus();
            }else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        BleUtil.stopScan(this);
        unregisterReceiver(scanResultBroadcastReceiver);
        stopScanBleDevices();
        super.onDestroy();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        BleUtil.stopScan(this);
        BluetoothDeviceEntity bluetoothDeviceEntity = bluetoothDeviceEntityList.get(position);
        Intent intent = getIntent();
        intent.putExtra("name", bluetoothDeviceEntity.getName());
        intent.putExtra("macAddress", bluetoothDeviceEntity.getAddress());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.btn_option) {
            if (!FissionSdkBleManage.getInstance().isScanning()) {
                searchBleDevices();
                bluetoothDeviceEntityList.clear();
                mLeDeviceListAdapter.notifyDataSetChanged();
            } else {
                stopScanBleDevices();
            }
            refreshOptionStatus();
        } else if (i == R.id.ib_back) {
            finish();
        }
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private List<BluetoothDeviceEntity> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(List<BluetoothDeviceEntity> mLeDevices) {
            super();
            this.mLeDevices = mLeDevices;
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.item_scan_ble, null);
                viewHolder = new ViewHolder();
                viewHolder.tvName = view.findViewById(R.id.tv_name);
                viewHolder.tvMacAddress = view.findViewById(R.id.tv_mac_address);
                viewHolder.tvRssi = view.findViewById(R.id.tv_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDeviceEntity device = mLeDevices.get(i);
            final String deviceName = device.getName();
            StringBuffer name = new StringBuffer();
            if (deviceName != null && deviceName.length() > 0)
                name.append(deviceName);
            else
                name.append(R.string.walle_ble_unknown_device);
            viewHolder.tvName.setText(name.toString());
            viewHolder.tvMacAddress.setText(device.getAddress());
            if (showSignalStrength) {
                viewHolder.tvRssi.setVisibility(View.VISIBLE);
                viewHolder.tvRssi.setText(String.valueOf(device.getRssi()));
            } else {
                viewHolder.tvRssi.setVisibility(View.GONE);
            }
            return view;
        }
    }

    static class ViewHolder {
        TextView tvName;
        TextView tvMacAddress;
        TextView tvRssi;
    }

    BroadcastReceiver scanResultBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WalleAction.SCAN_RESULT.equals(intent.getAction())) {
                BluetoothDeviceEntity device = new BluetoothDeviceEntity();
                device.setRssi(intent.getIntExtra("rssi", 0));
                device.setName(intent.getStringExtra("name"));
                device.setAddress(intent.getStringExtra("address"));
                addBluetoothDeviceEntity(device);
            } else if (WalleAction.SCAN_TIMEOUT.equals(intent.getAction())) {
                refreshOptionStatus();
            }
        }
    };

    private synchronized void addBluetoothDeviceEntity(BluetoothDeviceEntity device) {
        int index = -1;
        for (int i = 0; i < bluetoothDeviceEntityList.size(); i++) {
            if (bluetoothDeviceEntityList.get(i).getAddress().equals(device.getAddress())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            bluetoothDeviceEntityList.set(index, device);
        } else {
            bluetoothDeviceEntityList.add(device);
        }
        Collections.sort(bluetoothDeviceEntityList, new Comparator<BluetoothDeviceEntity>() {

            @Override
            public int compare(BluetoothDeviceEntity o1, BluetoothDeviceEntity o2) {
                return o2.getRssi().compareTo(o1.getRssi());
            }
        });
        mLeDeviceListAdapter.notifyDataSetChanged();
    }
}