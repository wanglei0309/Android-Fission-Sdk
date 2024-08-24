package com.szfission.wear.demo.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ThreadUtils;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    private TextView tvFilter;
    private ProgressBar pbLoad;
    private List<BluetoothDeviceEntity> bluetoothDeviceEntityList;
    private boolean showSignalStrength; // 信号强度
    private String[] scanFilterName ;

    private String mFilter;

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
        tvFilter = findViewById(R.id.tv_filter);

        String title = getIntent().getStringExtra("title");
        showSignalStrength = getIntent().getBooleanExtra("showSignalStrength", true);
        scanFilterName = getIntent().getStringArrayExtra("scanFilterName");

        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.walle_ble_bind_device);
        }
        tvTitle.setText(title);

        mFilter = SPUtils.getInstance().getString("filterKey", "");

        lvContent.setOnItemClickListener(this);
        btnOption.setOnClickListener(this);
        ibBack.setOnClickListener(this);
        tvFilter.setOnClickListener(this);

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

    /**
     * 解析蓝牙名称
     *
     * @param scanRecord
     * @return
     */
    public static String parseDeviceName(byte[] scanRecord) {
        String ret = null;
        if (null == scanRecord) {
            return ret;
        }

        ByteBuffer buffer = ByteBuffer.wrap(scanRecord).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) {
                continue;
            }

            byte type = buffer.get();
            length -= 1;
            switch (type) {
                case 0x01: // Flags
                    buffer.get(); // flags
                    length--;
                    break;
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                case 0x14: // List of 16-bit Service Solicitation UUIDs
                    while (length >= 2) {
                        buffer.getShort();
                        length -= 2;
                    }
                    break;
                case 0x04: // Partial list of 32 bit service UUIDs
                case 0x05: // Complete list of 32 bit service UUIDs
                    while (length >= 4) {
                        buffer.getInt();
                        length -= 4;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                case 0x15: // List of 128-bit Service Solicitation UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        length -= 16;
                    }
                    break;
                case 0x08: // Short local device name
                case 0x09: // Complete local device name
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    length = 0;
                    ret = new String(sb).trim();
                    return ret;
                case (byte) 0xFF: // Manufacturer Specific Data
                    buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    break;
            }
            if (length > 0) {
                if ((buffer.position() + length) < buffer.capacity()) {
                    buffer.position(buffer.position() + length);
                } else {
                    buffer.position(buffer.capacity());
                }
            }
        }
        return ret;
    }

    private void searchBleDevices(){
        FissionSdkBleManage.getInstance().scanBleDevices(new BleScanResultListener() {
                                                   @Override
                                                   public void onScanResult(ScanResult scanResult) {
                                                       if (scanResult != null) {
                                                           String bleDeviceName = scanResult.getBleDevice().getName();
                                                           if (StringUtils.isEmpty(bleDeviceName)) {
                                                               bleDeviceName = parseDeviceName(scanResult.getScanRecord().getBytes());
                                                           }
                                                           LogUtils.d("wl", "搜索到的蓝牙名称："+bleDeviceName+",过滤关键字："+mFilter);
                                                           BluetoothDeviceEntity device = new BluetoothDeviceEntity();
                                                           device.setRssi(scanResult.getRssi());
                                                           device.setName(bleDeviceName);
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
                                                       mLeDeviceListAdapter.notifyDataSetChanged();
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
        } else if(i == R.id.tv_filter){
            showDialog(this, "蓝牙名称过滤", SPUtils.getInstance().getString("filterKey", "LW"));
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

    private void addBluetoothDeviceEntity(BluetoothDeviceEntity device) {
        if (!TextUtils.isEmpty(device.getName()) && (TextUtils.isEmpty(mFilter) || device.getName().contains(mFilter))) {
            synchronized (bluetoothDeviceEntityList) {
                int index = findDeviceIndex(device);
                if (index >= 0) {
                    bluetoothDeviceEntityList.set(index, device);
                } else {
                    bluetoothDeviceEntityList.add(device);
                }
                Collections.sort(bluetoothDeviceEntityList, Collections.reverseOrder(new RssiComparator()));
            }

            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }

    private int findDeviceIndex(BluetoothDeviceEntity device) {
        for (int i = 0; i < bluetoothDeviceEntityList.size(); i++) {
            if (bluetoothDeviceEntityList.get(i).getAddress().equals(device.getAddress())) {
                return i;
            }
        }
        return -1;
    }

    private static class RssiComparator implements Comparator<BluetoothDeviceEntity> {
        @Override
        public int compare(BluetoothDeviceEntity o1, BluetoothDeviceEntity o2) {
            return o1.getRssi().compareTo(o2.getRssi());
        }
    }


    private void showDialog(Context context, String title,String editText) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_simple_input, null);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        EditText etContent = view.findViewById(R.id.etContent);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvTitle.setText(title);
        etContent.setText(editText);
        final AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filter = etContent.getText().toString().trim();
                mFilter = filter;
                SPUtils.getInstance().put("filterKey", filter);
                searchBleDevices();
                bluetoothDeviceEntityList.clear();
                mLeDeviceListAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}