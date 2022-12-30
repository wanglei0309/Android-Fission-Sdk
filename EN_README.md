# Fission-SDK-Android Integration Documentation
### One、Product description
* This document is to guide Android developers to integrate Fission-SDK-Android in Android 5.0 and above systems. It mainly includes some key usage examples. For more detailed API, please refer to the JavaDoc document.
* Fission-SDK-Android is the basic dependency library that Shenzhen Fission Smart Co., Ltd. provides for customer developers to adapt to the company's smart watches. It mainly includes the support of various functions such as Bluetooth scanning, Bluetooth connection, Bluetooth communication, health data synchronization, exercise data synchronization, call reminder, message push, music control and so on.
* Shenzhen Fission Smart Co., Ltd. hereinafter referred to as Fission, Fission-SDK-Android currently only supports REALTEK platform smart watches produced by Fission.
* 本SDK为裂变Android工程师负责开发和维护。联系邮箱:wanglei.hunan@gmail.com，联系电话：15202171127。

### Two、Quick start
#### 1. Import the SDK
Import **fissionsdk_v2-release-vx.x.x.aar**, **rtk-bbpro-core-x.x.x.jar**, **rtk-core-x.x.x.jar**, **rtk-dfu-x.x.x.jar** into the project , generally copied to the libs directory, and then set as follows in build.gradle in the module:
```
repositories {
    flatDir {
        dirs  file('libs')
    }
}

dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation files('libs/rtk-bbpro-core-1.6.9.jar')
    implementation files('libs/rtk-core-1.2.8.jar')
    implementation files('libs/rtk-dfu-3.4.1.jar')
    implementation(name: 'fissionsdk_v2-release-v2.0.27', ext: 'aar')
    api rootProject.ext.dependencies["rx-java2"]
    api rootProject.ext.dependencies["rx-android2"]
}
```
#### 2. Permission setting
SMS, calls, and location information permissions are privacy permissions. If the app needs to be put on Google Play, pay attention to applying for permissions in Google, otherwise it will not pass the review.

Static permission registration in AndroidManifest:
```
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_LOGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION " />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY"/>
```
Example of dynamic registration permission in the code: (For more dynamic permission application examples, please refer to the demo)
```
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
```
#### 3. Initialization
You need to perform SDK initialization in Application, for example:
```
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FissionSdkBleManage.getInstance().initFissionSdk(this);
    }
}
```
#### 4. Bluetooth scanning and connection
You can use your own bluetooth scan codes, or use the ones in the SDK. The scanning of the SDK has dealt with the compatibility and pitfalls of various system API versions. It is recommended to use the scanning function of the SDK. The sample code is as follows:
```
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
                    // add custom filters if needed
                    .build());
}

private void stopScanBleDevices(){
    FissionSdkBleManage.getInstance().stopScanBleDevices();
}
```
By default, the Bluetooth connection will start the automatic reconnection mechanism, the sample code is as follows:
```
public void connectDevice(String deviceAddress, boolean isBind, String fissionKey, BleConnectListener listener) {
    SharedPreferencesUtil.getInstance().getBluetoothName()));
    if (SharedPreferencesUtil.getInstance().getFissionKey().equals("")) {
        long time = System.currentTimeMillis();
        int lastTime = (int) (time % 10000);
        int bindKey = AnyWear.bindDevice((int) (lastTime), deviceAddress);
        SharedPreferencesUtil.getInstance().setFissionKey(lastTime + "," + bindKey);
    }
    BleComConfig bleComConfig = new BleComConfig();
    bleComConfig.setBind(isBind);
    bleComConfig.setBindKeys(SharedPreferencesUtil.getInstance().getFissionKey());
    FissionSdkBleManage.getInstance().connectBleDevice(deviceAddress, bleComConfig, false, listener);
}
```
After completing the above steps, the quick access is complete, and you can connect the smart watch to debug various functions corresponding to it. For detailed descriptions of the configuration parameters BleScanConfig for Bluetooth scanning and BleComConfig for Bluetooth connections, please refer to the following and JavaDoc documents.

### Three、SDK description
#### 1. Description of package contents
SDK currently contains two versions, Fission basic version and Fission_V2 version. In order to be compatible from top to bottom, the aar file of the basic version must be relied on when using the V2 version. The underlying version of Bluetooth search, connection and communication are the original version, not packaged and optimized. Scanning, connection, communication stability V2 version has been fully optimized. The communication protocol is compatible with the base version. This document is the instruction document for Fission_V2 version.

#### 2. Architecture introduction
The SDK uses the RxAndroidBle open source framework for secondary development. The bottom layer optimizes the stability of the Bluetooth connection, supports the automatic reconnection mechanism, supports the prevention of the lock screen system from sleeping, and supports the queue jumping function (when synchronizing data, the call reminder will not be delayed).

#### 3. Supported development environment
Supports Android 5.0 and above systems, and has been adapted to Android 12. It is recommended to use Android Studio for development.

#### 4. Access and Update Guidelines
Fission engineers for initial access and version updates will be responsible for guidance.

#### 5. Problem location process
The log can be turned on during development and turned off when it is put on the shelf. If you encounter a suspected SDK bug, please contact the fission engineer and submit the log. Fission will give timely feedback.
```
 FissionSdkBleManage.getInstance().setDebug(true);
 ```

### Four、SDK function module description
#### 1. Bluetooth scanning
When the App needs to manually search for devices, it can call this function module.
```
public class BleScanConfig {

    /**
     * Whether to keep scanning
     */
    private boolean isContinuousScan = false;

    /**
     * time per scan
     */
    private int scanDuration = 6000;

    public boolean isContinuousScan() {
        return isContinuousScan;
    }

    public void setContinuousScan(boolean continuousScan) {
        isContinuousScan = continuousScan;
    }

    public int getScanDuration() {
        return scanDuration;
    }

    public void setScanDuration(int scanDuration) {
        this.scanDuration = scanDuration;
    }
}
```

```
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
```
* BleScanConfig can be used to configure scanning parameters.
* Scanning in the foreground, you can set the continuous scanning mode when you need to display the scanning results. When scanning in the background, you can set the non-continuous scanning mode and set the scanning duration.
* According to the Android system limit, the number of scans within 30 seconds shall not exceed 5 times, otherwise the system will no longer return the scan result. The duration of each scan should not be less than 6s.
* When the BleScanConfig configuration is not set, the SDK will use the default configuration.
* When scanning fails and an exception occurs, compare and confirm the cause in the constant class according to the exception error code and error message.
* Common problems: incorrect authorization, too frequent scanning, background scanning not configured correctly, etc. resulting in no return result of scanning.

#### 2. Bluetooth connection
This function module needs to be used when the App needs to connect to the Bluetooth watch.
```
public class BleComConfig implements Serializable {

    /**
     *  The default MTU is a minimum of 23
     */
    private int mtu = 247;

    /**
     *  command timeout
     */
    private int timeout = 6000;

    /**
     *  Do you need to bind the device (binding key AT command)
     */
    private boolean isBind = false;

    /**
     *  Whether to configure the high-speed mode for large data instructions
     */
    private HashMap<String, Boolean> cmdHighModeMap;

    /**
     *  Configuration command priority
     */
    private HashMap<String, Integer> cmdPriorityMap;

    /**
     *  Configure whether to wait for the command result
     */
    private HashMap<String, Boolean> cmdNeedTimeoutMap;

    /**
     *  bind key
     */
    private String bindKeys = "";

    /**
     *  Whether to enable the SDK automatic connection back mechanism
     */
    private boolean isAutoReconnect =true;

    public boolean isAutoReconnect() {
        return isAutoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        isAutoReconnect = autoReconnect;
    }

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public boolean isBind() {
        return isBind;
    }

    public void setBind(boolean bind) {
        isBind = bind;
    }

    public String getBindKeys() {
        return bindKeys;
    }

    public void setBindKeys(String bindKeys) {
        this.bindKeys = bindKeys;
    }

    public HashMap<String, Boolean> getCmdHighModeMap() {
        if(cmdHighModeMap == null){
            cmdHighModeMap = new HashMap<>();
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_GET_MEASURE_INFO, true); //默认消息通知推送不需要开启高速模式
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_ST_HOURS_REPORT, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_ST_SLEEP_RECORD, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_EXERCISE_REPORT, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_GET_SLEEP_CUR_RECORD, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_GET_HAND_MEASURE_INFO, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_HEART_RATE_RECORD, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_ST_STEPS_RECORD, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_ST_SPO2_RECORD, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_ST_BLOOD_PRESSURE_RECORD, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_EXERCISE_DETAIL, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_ST_TIMING_INFO, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_ST_CUS_DIAL_DATA, true);
            cmdHighModeMap.put(BigDataCmdID.CMD_ID_ST_CUS_SPORT_DATA, true);
        }
        return cmdHighModeMap;
    }

    public void setCmdHighModeMap(HashMap<String, Boolean> cmdHighModeMap) {
        this.cmdHighModeMap = cmdHighModeMap;
    }

    public HashMap<String, Integer> getCmdPriorityMap() {
        if(cmdPriorityMap == null){
            cmdPriorityMap = new HashMap<>();
            cmdPriorityMap.put(AtCmd.AT_CMD_BDQ, FissionConstant.CMD_PRIORITY_HIGH);
            cmdPriorityMap.put(AtCmd.AT_CMD_MTU, FissionConstant.CMD_PRIORITY_HIGH);
            cmdPriorityMap.put(AtCmd.AT_CMD_SET_TIME, FissionConstant.CMD_PRIORITY_HIGH);
            cmdPriorityMap.put(AtCmd.AT_CMD_SET_TIMEZONE, FissionConstant.CMD_PRIORITY_HIGH);
            cmdPriorityMap.put(AtCmd.AT_CMD_SET_TIME_MODEL, FissionConstant.CMD_PRIORITY_HIGH);
            cmdPriorityMap.put(AtCmd.AT_CMD_SET_LANG, FissionConstant.CMD_PRIORITY_HIGH);
            cmdPriorityMap.put(AtCmd.AT_CMD_CCS, FissionConstant.CMD_PRIORITY_HIGH_PLUS);
            cmdPriorityMap.put(BigDataCmdID.CMD_ID_APPS_MESS, FissionConstant.CMD_PRIORITY_HIGH);
            cmdPriorityMap.put(BigDataCmdID.CMD_ID_ST_CUS_DIAL_DATA, FissionConstant.CMD_PRIORITY_HIGH);
            cmdPriorityMap.put(BigDataCmdID.CMD_ID_ST_CUS_SPORT_DATA, FissionConstant.CMD_PRIORITY_HIGH);
        }
        return cmdPriorityMap;
    }

    public void setCmdPriorityMap(HashMap<String, Integer> cmdPriorityMap) {
        this.cmdPriorityMap = cmdPriorityMap;
    }

    public HashMap<String, Boolean> getCmdNeedTimeoutMap() {
        if(cmdNeedTimeoutMap == null){
            cmdNeedTimeoutMap = new HashMap<>();
//            cmdNeedTimeoutMap.put(AtCmd.AT_CMD_MTU, false);
        }
        return cmdNeedTimeoutMap;
    }

    public void setCmdNeedTimeoutMap(HashMap<String, Boolean> cmdNeedTimeoutMap) {
        this.cmdNeedTimeoutMap = cmdNeedTimeoutMap;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "BleComConfig{" +
                "mtu=" + mtu +
                ", timeout=" + timeout +
                ", isBind=" + isBind +
                ", cmdHighModeMap=" + cmdHighModeMap +
                ", cmdPriorityMap=" + cmdPriorityMap +
                ", bindKeys='" + bindKeys + '\'' +
                ", isAutoReconnect=" + isAutoReconnect +
                '}';
    }
}
```

```
 public void connectBleDevice(String mac, BleComConfig bleComConfig, boolean auto, @NonNull BleConnectListener listener)

 public void disconnectBleDevice()
```

```
public interface BleConnectListener {

    /**
     * BLE connection state change
     * @param newState
     */
    void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState);

    void onConnectionFailure(Throwable throwable);

    void onBinding();

    void onBindSucceeded(String address, String name);

    void onBindFailed(int code);
}

```

* BleComConfig can be used to configure Bluetooth connection and communication parameters.
* The mtu configuration will negotiate the maximum value supported by Bluetooth through the communication protocol with the watch, and there is no need to modify the default value. The minimum instruction timeout cannot be less than 3s, and it is recommended to use the default value. Whether to bind or not needs to be configured by the upper layer of the App according to the connected watch model. For how to generate the binding key, please refer to the demo code. Big data high-speed configuration, command priority configuration, and whether to wait for command response results are recommended to use the default values for these three configuration items. If you need to set it, please contact the fission engineer for guidance.
* The auto-reconnect mechanism is enabled by default. The upper layer of the app can also design its own reconnection mechanism and configure to disable SDK reconnection.
* Common problems: connection compatibility problems, some models cannot connect, automatic connection failure after disconnection, frequent disconnection, etc.
* Location problem needs to set sdk debug. Logs are then provided for analysis by fission engineers.
* Use **connectBleDevice** method to connect/bind the watch, use **disconnectBleDevice** to disconnect/unbind. When unbinding, the app needs to clean up the bindkey by itself, and the sdk is not responsible for saving and cleaning up.
* **onConnectionStateChange** is used to monitor the Bluetooth underlying connection status, and **onConnectionFailure** will call back this method when the Bluetooth connection fails. **onBinding** This method will be called back after the Bluetooth connection is successful. If it is a device that needs to be bound, it means that the device is being bound. If it does not need to be bound, it is initializing the Bluetooth communication service. **onBindSucceeded** This method will be called back after the binding/connection is successful. **onBindFailed** This method will be called back when the binding/connection fails.
* **Note: After the binding fails, pay attention to processing according to the error code. When the error code is equal to FissionConstant.BIND_FAIL_KEY_ERROR, you need to clear the cached key and re-bind. **

#### 3. Watch configuration
After the App is successfully connected to the watch, it needs to exchange configuration items between the two parties. It mainly includes the following configuration items:
1. Get the firmware information. After the connection is successful, get the watch firmware information first. Some configuration items may be related to the firmware version and model.
```    
   /**
     *  Get firmware information
     */
    public void getHardwareInfo(HardWareInfo hardWareInfo){

    }

    FissionSdkBleManage.getInstance().getHardwareInfo();
```
FissionSdkBleManage.getInstance().getHardwareInfo() This is the command method to send and obtain firmware information. getHardwareInfo(HardWareInfo hardWareInfo) This is the callback method that returns data to the App after the device executes the command successfully. Please refer to the demo for specific usage. HardWareInfo contains information such as hardware version, firmware version, protocol version, adaptation number, Bluetooth address, Bluetooth name, etc. It is mainly used for function module adaptation and version judgment during future firmware upgrades.
```    
    private long    bodyVersion;//结构体版本
    private String  hardWareTag;
    private String  deviceMac;
    private String  hardwareVersion;
    private String  firmwareVersion;
    private String  agreementVersion;
    private String  deviceName;
    private String  sn;
    private String  updateTime;
    private String  adapterNum;
    private int     deviceId;
```

2. Set the time and time zone
```
   /**
    *  set time zone
    */
    public void setTimezone(String timezone){

    }

  /**
    *  set time
    */  
    public void setTimes(String times){

    }

    FissionSdkBleManage.getInstance().setTimezone(Integer.parseInt(content));
    FissionSdkBleManage.getInstance().setTimes();
```
Please judge and set the time zone and time according to the firmware information. (Some watches may need to set 0 time zone, pay attention to winter and summer time and half time zone, 15 minutes, 45 minutes time zone country and city adaptation)

3. Set language
```    
    /**
     *  language setting
     */
    public void setLanguage(int language){}

    FissionSdkBleManage.getInstance().setLanguage(value);

    /**
      * language
     */
                     int LG_CHN = 0,//Chinese
                     LG_EN = 1,//English
                     LG_JP = 2,//Japanese
                     LG_FRENCH = 3, //French
                     LG_GERMAN = 4, // German
                     LG_SPANISH = 5, //Spanish
                     LG_ITALIAN = 6,//Italian
                     LG_PORTUGUESE = 7,//Portuguese
                     LG_RUSSIAN = 8, //Russian
                     LG_CZECH = 9,//Czech language
                     LG_POLISH = 10,//Polish
                     LG_TR_CHN = 11,//Traditional Chinese
                     LG_ARABIC = 12,//Arabic
                     LG_TURKISH = 13,//Turkish
                     LG_VIETNAMESE = 14,//Vietnamese
                     LG_KOREAN = 15,//Korean
                     LG_HEBREW = 16, //Hebrew
                     LG_THAI = 17,//Thai language
                     LG_INDONESIAN = 18,//Indonesian
                     LG_DUTCH = 19, //Dutch
                     LG_GREEK = 20,//Greek
                     LG_SWEDISH = 21,//Swedish
                     LG_ROMANIAN = 22,//Romanian
                     LG_HINDI = 23,//Hindi
                     LG_BENGALI = 24,//Bengali
                     LG_URDU = 25,//Urdu
                     LG_PERSIAN = 26;//Persian
```

4. Set personal information
```
/**
  * Set user personal information
  */
    public void setUserInfo(){}

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
```
The accuracy of user data is related to the personal information set by the user. After the binding is successful, personal information needs to be set.
5. Set the unit
```   
     /**
       *  Set the unit
       */
      public void setUnit(int unit){

      }
      // value==1?"Metric":"Imperial"
      FissionSdkBleManage.getInstance().setUnit(value);
```

6. Enable and disable monitoring data flow
```
/**
 * Enable and disable monitoring data flow
 */
public void setDataStream(String time){}
// 0=disable， recommendation:1000
FissionSdkBleManage.getInstance().setDataStream(Integer.parseInt(content));
```
After the data stream detection is turned on, you can receive the real-time steps, heart rate, blood pressure, blood oxygen and other data generated by the watch. The larger the setting value, the longer the refresh interval. The smaller the value, the greater the power consumption.

7. Heart rate level determination parameters (According to product design and processing, watch data is the priority to get the data from the device and then synchronize to the App. App data is the priority set to synchronize data from the App to the watch.)
```
    /**
     * Heart rate level judgment parameters
     */
    public void getHrRateLevelPara(HrRateLevel hrRateLevel){}
    FissionSdkBleManage.getInstance().getHrRateLevelPara();

    /**
     *  Set heart rate level judgment parameters
     */
    public void setHrlevAlgoPara(){}
    FissionSdkBleManage.getInstance().setHrlevAlgoPara(hrRateLevel);
```
HrRateLevelThe heart rate level judgment parameters are as follows:
```
private int overMaxHr;//more than this percentage, it is considered as maxHr
private int moderate;//beyond this percentage, considered as moderate Hr
private int vigorous;//more than this percentage, it is considered active hr
private int maxHr;//beyond this percentage, it is considered as maxHr
private int highestHr;//The highest heart rate value
private int hrTimeLimit;//Only when the heart rate value is above a certain level in this time width can the new level be determined
```

8. Sedentary judgment parameters (according to the product design process, take the watch data as the priority to get the data from the device and then synchronize it to the App. Take the App data as the priority set to synchronize the data from the App to the watch.)
```
  /**
   * 久坐判定参数
   */
  public void getSedentaryPara(SedentaryBean sedentaryBean){}
  FissionSdkBleManage.getInstance().getSedentaryPara();

  /**
    *  设置久坐判定参数
    */
  public void setSedentaryPara(){}
  SedentaryBean sedentaryBean = new SedentaryBean();
  sedentaryBean.setEnable(switchOpen.isChecked());
  sedentaryBean.setStartTime(Integer.parseInt(startTime));
  sedentaryBean.setEndTime(Integer.parseInt(endTime));
  sedentaryBean.setDurTime(Integer.parseInt(keepTime));
  sedentaryBean.setTargetStep(Integer.parseInt(targetStep));
  FissionSdkBleManage.getInstance().setSedentaryPara(sedentaryBean);
```
SedentaryBean The sedentary judgment parameters are as follows:
```   
    private boolean enable;//Whether to enable sedentary
    private int startTime;//Start time, the absolute minute of a day, the maximum value is 24*60 = 1440 minutes. The minimum value is 0
    private int endTime;//same as start time
    private int durTime;//Sedentary duration detection, if the number of steps does not reach the standard within this time, a sedentary reminder will be issued
    private int targetStep;//The sedentary target step number, if it is lower than this value within the duration, the sedentary reminder will be issued
```

9. Drinking water reminder parameters (according to the product design process, the watch data is the priority to get the data from the device and then synchronized to the App. The App data is the priority set to synchronize the data from the App to the watch.)
```
 /**
   * Get Drink water reminder parameters
   */
  public void getDrinkWaterPara(DkWaterRemind dkWaterRemind){}
  FissionSdkBleManage.getInstance().getDrinkWaterPara();

  /**
  *  Set drink water reminder parameters
  */
   public void setDrinkWaterPara(){}
   DkWaterRemind dkWaterRemind = new DkWaterRemind();
   dkWaterRemind.setStartTime(Integer.parseInt(startTime));
   dkWaterRemind.setEndTime(Integer.parseInt(endTime));
   dkWaterRemind.setEnable(switchOpen.isChecked());
   dkWaterRemind.setRemindWeek(  Integer.parseInt(weekTime));
   dkWaterRemind.setStartTime(Integer.parseInt(startTime));
   FissionSdkBleManage.getInstance().setDrinkWaterPara(dkWaterRemind);
```
DkWaterRemind Drink water reminder parameters are as follows:
```    
    private int startTime;// Reminder start time, the absolute minute of the day, the default start time is 08:00
    private int endTime;// Reminder end time, the absolute minute of the day, the default start time is 18:00
    private int remindWeek;//Reminder period, if it is 0, only remind once
    private boolean enable;//reminder switch
```

10. Do Not Disturb Parameters (According to the product design process, the watch data is the priority to get the data from the device and then synchronized to the App. The App data is the priority set to synchronize the data from the App to the watch.)
```
    /**
     *  Get Do not disturb parameter
     */
    public void getDndPara(DndRemind dndRemind){}
    FissionSdkBleManage.getInstance().getDndPara();

    /**
      *  Set Do Not Disturb parameters
      */
    public void setDndPara(){}
    DndRemind dndRemind = new DndRemind();
    if (startTime.isEmpty()) {
        Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
        return;
    }
    if (endTime.isEmpty()) {
        Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
        return;
    }
    showProgress();
    dndRemind.setStartTime(Integer.parseInt(startTime));
    dndRemind.setEndTime(Integer.parseInt(endTime));
    dndRemind.setEnable(switchOpen.isChecked());
    FissionSdkBleManage.getInstance().setDndPara(dndRemind);
```
DndRemind Do Not Disturb parameters are as follows:
```   
    private int startTime;// Reminder start time, the absolute minute of the day, the default start time is 08:00
    private int endTime;// Reminder end time, the absolute minute of the day, the default start time is 18:00
    private boolean enable;//reminder switch
```

11. Heart rate detection parameters (according to the product design process, the watch data is the priority to get the data from the device and then synchronized to the App. The App data is the priority set to synchronize the data from the App to the watch.)
```
    /**
     * Get heart rate detection time period parameters
     */
    public void getHrDetectPara(HrDetectPara hrDetectPara){}
    FissionSdkBleManage.getInstance().getHrDetectPara();

    /**
     *  Set heart rate detection time period parameters
     */
    public void setHrDetectPara(){}
    HrDetectPara hrDetectPara = new HrDetectPara();
    hrDetectPara.setOpen(switchOpen.isChecked());
    hrDetectPara.setStartTime(Integer.parseInt(startTime));
    hrDetectPara.setEndTime(Integer.parseInt(endTime));
    hrDetectPara.setWeek(Integer.parseInt(weekTime));
    FissionSdkBleManage.getInstance().setHrDetectPara(hrDetectPara);
```
HrDetectPara The heart rate detection time period parameters are as follows:
```    
    private int startTime;  // Reminder start time, the absolute minute of the day
    private int endTime;    // Reminder end time
    private int week;       // Reminder period, if it is 0, only remind once
    private boolean open;   // Reminder switch: 0: off. 1: open.
```

12. Parameters for raising the wrist to brighten the screen (according to the product design process, the watch data is the priority to get the data from the device and then synchronized to the App. The App data is the priority set to synchronize the data from the App to the watch.)
```
    /**
     * Get the parameters of the time period for raising the wrist to turn on the screen
     */
    public void getLiftWristPara(LiftWristPara liftWristPara){}

    /**
     *  Get the duration of the bright screen
     */
    public void getScreenKeep(String result){}
    FissionSdkBleManage.getInstance().getScreenKeep();
    FissionSdkBleManage.getInstance().getLiftWristPara();

    /**
     *  Set the parameters of the time period for raising the wrist to turn on the screen
     */
    public void setLiftWristPara(){}
    LiftWristPara liftWristPara = new LiftWristPara();
    liftWristPara.setStartTime(Integer.parseInt(startTime));
    liftWristPara.setEnable(switchOpen.isChecked());
    liftWristPara.setEndTime(Integer.parseInt(endTime));
    FissionSdkBleManage.getInstance().setLiftWristPara(liftWristPara);
    /**
     *  Set the duration of the bright screen
     */
    public void setScreenKeep(){}
    FissionSdkBleManage.getInstance().setScreenKeep(5);
```
The larger the screen-on time value, the higher the power consumption.
The parameters of LiftWristPara's time period for raising the wrist to turn on the screen are as follows:
```  
    private int startTime; //Raise the wrist to turn on the screen start time
    private int endTime; //End time of raising the wrist to turn on the screen
    private boolean enable; //Raising the wrist to brighten the screen function switch
```
#### 4. The watch actively initiates the function
Some functions on the watch will actively send messages to the App, and the App needs to add SDK callbacks to monitor and process.
* find phone
```   
    /**
     *  find phone
     */
    public void findPhone(){}
```
* Cancel Find Phone
```   
    /**
     *  Cancel Find Phone
     */
    public void cancelFindPhone(){}
```
* camera control
```
    /**
     * watch control camera
     */
    public void onTakePhotoCallback(){}
```
* The watch answers/hangs up the call (0: reject; 1: answer; 2: mute)
```   
    /**
     * 手环拒接电话/接听电话
     */
    public void onTakePhoneCallback(int callStatus){}
```
* Turn on GPS positioning
```    
    /**
     *  打开GPS
     */
    public void gpsSuccess(boolean open){}
```
* music control
```
    /**
     *  音乐音量
     */
    public void setMusicVolume(MusicConfig musicConfig){}

    /**
     *  音乐控制
     */
    public void setMusicControl(MusicConfig musicConfig){}

    /**
     *  音乐进度
     */
    public void setMusicProgress(MusicConfig musicConfig){}
```
MusicConfig The music control information is as follows:
``` public final static int MUSIC_STOP = 0;
    public final static int MUSIC_PAUSE = 1;
    public final static int MUSIC_PLAYING = 2;
    public final static int MUSIC_LAST = 3;
    public final static int MUSIC_NEXT = 4;
    public final static int MUSIC_BUFFER = 5;
    public final static int MUSIC_EXIT = 6;

    /**
     *  volume control
     */
    public final static int OPERATION_TYPE_VOLUME = 0;
    /**
     *  Play state control
     */
    public final static int OPERATION_TYPE_CONTROL = 1;
    /**
     *  Progress Control
     */
    public final static int OPERATION_TYPE_PLAYBACK_PROGRESS = 2;

    /**
     *  current volume
     */
    private int currentVolume =0;

    /**
     *  maximum volume
     */
    private int maxVolume =0;

    /**
     *  music name
     */
    private String name;

    /**
     *  music current progress
     */
    private int progress = 0;

    /**
     *  music duration
     */
    private int duration = 0;

    /**
     *  Current music playback status
     */
    private int state =0;

    /**
     *  Music operation type (volume control / playback status control / progress control)
     */
    private int operationType = 0;
```

FissionMusicInfo
```
public class FissionMusicInfo {
  String musicName;//歌曲名称
  String musicSinger;//演唱者
  int musicTotalTime;//歌曲总时长
  String AlbumName;//专辑名
  String playAppName;//播放APP名称

  /**
   *  当前音量
   */
  private int currentVolume =0;

  /**
   *  最大音量
   */
  private int maxVolume =0;

  /**
   *  音乐当前进度
   */
  private int progress = 0;

  /**
   *  当前音乐播放状态
   */
  private int state =0;

  public int getCurrentVolume() {
      return currentVolume;
  }

  public void setCurrentVolume(int currentVolume) {
      this.currentVolume = currentVolume;
  }

  public int getMaxVolume() {
      return maxVolume;
  }

  public void setMaxVolume(int maxVolume) {
      this.maxVolume = maxVolume;
  }

  public int getProgress() {
      return progress;
  }

  public void setProgress(int progress) {
      this.progress = progress;
  }

  public int getState() {
      return state;
  }

  public void setState(int state) {
      this.state = state;
  }

  public String getMusicName() {
      return musicName;
  }

  public void setMusicName(String musicName) {
      this.musicName = musicName;
  }

  public String getMusicSinger() {
      return musicSinger;
  }

  public void setMusicSinger(String musicSinger) {
      this.musicSinger = musicSinger;
  }

  public int getMusicTotalTime() {
      return musicTotalTime;
  }

  public void setMusicTotalTime(int musicTotalTime) {
      this.musicTotalTime = musicTotalTime;
  }

  public String getAlbumName() {
      return AlbumName;
  }

  public void setAlbumName(String albumName) {
      AlbumName = albumName;
  }

  public String getPlayAppName() {
      return playAppName;
  }

  public void setPlayAppName(String playAppName) {
      this.playAppName = playAppName;
  }

  @Override
  public String toString() {
      return "FissionMusicInfo{" +
              "musicName='" + musicName + '\'' +
              ", musicSinger='" + musicSinger + '\'' +
              ", musicTotalTime=" + musicTotalTime +
              ", AlbumName='" + AlbumName + '\'' +
              ", playAppName='" + playAppName + '\'' +
              ", currentVolume=" + currentVolume +
              ", maxVolume=" + maxVolume +
              ", progress=" + progress +
              ", state=" + state +
              '}';
  }
}

```
Push music information to device.(ps:Determine whether the device supports pushing music information once. If FissionMusicInfo is supported, use MusicConfig if not supported, and push with different commands.)
```
public void nowMusicInfo(boolean playState, String musicName, int currentVolume, int maxVolume, int progress, int duration) {
        FissionMusicInfo fissionMusicInfo = new FissionMusicInfo();
        fissionMusicInfo.setMusicName(musicName);
        fissionMusicInfo.setState(playState ? MusicConfig.MUSIC_PLAYING : MusicConfig.MUSIC_PAUSE);
        fissionMusicInfo.setCurrentVolume(currentVolume);
        fissionMusicInfo.setMaxVolume(maxVolume);
        fissionMusicInfo.setMusicTotalTime(duration);
        fissionMusicInfo.setProgress(progress);
        if (!SPUtils.getInstance().getBoolean(SpKey.SUPPORT_PUSH_MUSIC_PLAYER)) {
            FissionSdkBleManage.getInstance().sendMusicInfo(fissionMusicInfo);
            MusicConfig musicConfig = new MusicConfig();
            musicConfig.setState(playState ? MusicConfig.MUSIC_PLAYING : MusicConfig.MUSIC_PAUSE);
            musicConfig.setDuration(duration);
            musicConfig.setProgress(progress);
            musicConfig.setMaxVolume(maxVolume);
            musicConfig.setCurrentVolume(currentVolume);
            FissionSdkBleManage.getInstance().setMusicControl(musicConfig);
            FissionSdkBleManage.getInstance().setMusicVolume(musicConfig);
            FissionSdkBleManage.getInstance().setMusicProgress(musicConfig);
        } else {
            FissionSdkBleManage.getInstance().sendMusicInfo(fissionMusicInfo);
        }
        LogUtils.d("wl", "-------nowMusicInfo------");
    }
```


* Function switch state synchronization
```   
    /**
     *  Functional switch callback monitoring
     */
    public void fssSuccess(FssStatus fssStatus){}
```
FssStatus The function switch status information is as follows:
```    
    /**
     * Function type
     */
    private int fssType;

    /**
     * functional status
     */
    private int fssStatus;

    PS: fssType == 1, the main switch status of physical sign data collection
                     2. Vibration switch state
                     3. Do not disturb switch status
                     4. Enable status of alarm clock 1
                     5. Enable status of alarm clock 2
                     6. Enable status of alarm clock 3
                     7. Enable status of alarm clock 4
                     8. Enable status of alarm clock 5
                     9. Switch status of low voltage reminder function
                     10. Daily target reminder to detect master switch status
                     11. Weekly goal reminder to detect master switch status
                     12. Self-encouragement goal reminder detection master switch
                     state
                     13. The heart rate exceeds the standard reminder switch status
                     14. Wear notification switch status
                     15. Photo mode switch status
                     16. Battery status (only set fssStatus=0: discharge status 1: low voltage status 2: charging status 3: full status)
                     17. Enter the music interface
                     18, Bright screen time setting
                     ->app valid)
                     19. The status of the wrist lift switch is the same as
                     20, the current percentage of the battery
                     efficient)
                     21. Water drinking reminder switch
                     22. Sedentary reminder switch
                     23, OTA percentage (fssStatus=percentage)
                     24, mute switch synchronization
                     25. OTA interface status
       fssStatus: 0, close; 1, open.
```

#### 5. The App initiates the function actively
A simple instruction function initiated by the App. When the function is executed successfully, the App will be notified through the form of a callback, and a callback listener needs to be added.
* Find device
```
  /**
   *  Find device callback monitoring, successful callback means device search is successful
   */
  public void findDevice(){}
  FissionSdkBleManage.getInstance().findDevice();
```
* Reboot the device
```
    /**
     *  重启设备
     */
    public void rebootDevice(){}
    FissionSdkBleManage.getInstance().rebootDevice();
```
* reset
```
  /**
   *  恢复出厂设置
   */
  public void resetDevice(){}
  FissionSdkBleManage.getInstance().resetDevice();
```
* soft shutdown
```
  /**
   *  软关机
   */
  public void shutdown(){}
  FissionSdkBleManage.getInstance().shutdown();
```
* get battery
```
  /**
   *  获取电量状态
   */
  public void getDeviceBattery(DeviceBattery deviceBattery){}
  FissionSdkBleManage.getInstance().getDeviceBattery();
```
* Enable prompts (currently only supports high heart rate prompts, value is the maximum heart rate value)
```
  /**
   *  设置运动心率过高提示
   */
  public void setHeartRateHighTips(String result){}
  FissionSdkBleManage.getInstance().setHeartRateHighTips(1,value);
```
* Turn on/off photo mode (true/false)
```
  /**
   *  设置拍照模式开关
   */
  public void setSwitchPhotoMode(boolean enable){}
  FissionSdkBleManage.getInstance().setSwitchPhotoMode(true);
```
* Vibration reminder switch (true/false)
```  
    /**
     *  设置震动提醒开关
     */
    public void setSwitchVibration(boolean enable){}
    FissionSdkBleManage.getInstance().setSwitchVibration(true);
```
* security confirmation
```  
    /**
     *  安全确认
     */
    public void safetyConfirmation(String result){}
    FissionSdkBleManage.getInstance().safetyConfirmation(content);
```
* Self-test mode switch (true/false)
```    
    /**
     *  设置自检模式
     */
    public void setSwitchSelfInspectionMode(boolean enable){}
    FissionSdkBleManage.getInstance().setSwitchSelfInspectionMode(true);
```
* clear user information
```
    /**
     *  清除用户信息
     */
    public void clearUserInfo(){}
    FissionSdkBleManage.getInstance().clearUserInfo()
```
* clear activity data
```
    /**
     *  清除运动数据
     */
    public void clearSportData(){}
    FissionSdkBleManage.getInstance().clearSportData()
```
* page jump
```
    /**
     *  页面跳转
     */
    public void setPageSkip(String result){}
    FissionSdkBleManage.getInstance().setPageSkip(content);
```
* music control
```
  /**
   *  音乐音量
   */
  public void setMusicVolume(MusicConfig musicConfig){}

  /**
   *  音乐播放状态控制
   */
  public void setMusicControl(MusicConfig musicConfig){}

  /**
   *  音乐进度
   */
  public void setMusicProgress(MusicConfig musicConfig){}

  MusicConfig musicConfig = new MusicConfig();
  musicConfig.setOperationType(MusicConfig.OPERATION_TYPE_VOLUME);
  musicConfig.setCurrentVolume(progress);
  musicConfig.setMaxVolume(max);
  FissionSdkBleManage.getInstance().setMusicVolume(musicConfig);
```

#### 6. Alarm clock setting
The watch only supports 5 alarm clocks by default, and the **index** in **FissionAlarm** of each alarm clock is used as a unique identification number. The alarm clock setting function needs to judge according to the model of the watch whether the alarm clock set by the watch or the alarm clock set by the app is the main one. For detailed usage, please refer to the demo code. An example of use is as follows:
```   
    /**
     * 记事提醒/闹铃信息
     */
    public void getAlarm(List<FissionAlarm> fissionAlarms){}
    FissionSdkBleManage.getInstance().getAlarm();

    /**
     *  设置记事提醒/闹铃信息
     */
    public void setAlarmInfos(){}
      List<FissionAlarm> list = new ArrayList<>();
    FissionAlarm alarm = new FissionAlarm(0,1,true,System.currentTimeMillis()+60000,weekResult);
    list.add(alarm);
    for (int i = 1;i<5;i++){
       list.add(new FissionAlarm(i,1,true,System.currentTimeMillis()+i*120000,weekResult));
    }
    FissionSdkBleManage.getInstance().setAlarmInfos(list);
```
FissionAlarm
```
    private int bodyVersion;    // 结构体版本
    private int index;          // 序号
    private boolean alarmActive;  // 闹钟有效性 0：关 1：开
    private int type;           // 闹铃类别：    0：备忘提醒，年月日小时分钟有效。 1：定时闹钟，仅小时分钟有效
    private boolean open;       // 使能开关 0：关 1：开
    private int alarmState;     // 报警状态，1 响闹，2 稍后提醒，3 结束响闹 or 未响闹
    private boolean alarmDelayAlert;    // 闹钟是否支持稍后提醒，0 支持，1 不支持。默认支持。
    private int weekCode;       // 周期掩码，bit0-bit6:周日、周一....周六，bit7(表示一次/每天有效)
    private int model;      //  提醒方式 有哪些方式不明确
    private int year;       // 年
    private int month;      // 月
    private int day;        // 日
    private int hour;       // 时
    private int minute;     // 分
    private int alertCount;     // 提醒次数
    private int alertedCount;   // 已提醒次数
    private int alertIntervalTime;  //  未关闭再次提醒的间隔，单位分钟
    private int remarkLength;   // 描述长度
    private int shakeType;     //震动方式
    private String remark;      // 描述
    private int isvalied;
    private long times;
```
Unused alarm clock id, when adding an alarm clock from the App, you need to get the unused alarm clock id first:
```
    /**
     *  未使用闹铃id
     */
    public void getNotUsingAlarmId(String result){}
    FissionSdkBleManage.getInstance().getNotUsingAlarmId();
```
#### 7, App message notification
The message notification function requires the app to guide the user to authorize, and use **NotificationListenerService** to complete. The app needs to filter the application package name to determine whether it needs to be pushed to the watch device. The example of using the push message is as follows:
```
      /**
       *  APP消息推送
       */
      public void pushAppNotification(){}

      int id = 1;
      int type = spinnerType.getSelectedItemPosition();
      AppMessageBean  appMessageBean = new AppMessageBean();
      appMessageBean.setMsgId(id);
      appMessageBean.setMsgType(type);
      appMessageBean.setContactName(etName.getText().toString());
      appMessageBean.setMsgContent(etContent.getText().toString());
      appMessageBean.setMsgTime(System.currentTimeMillis()/1000);
      FsLogUtil.d("推送类型"+appMessageBean.getMsgType());
      FissionSdkBleManage.getInstance().pushAppNotification(appMessageBean);
```
Note: If there are multiple Apps on the phone that use the notification monitoring permission, there is a probability that your App will fail to monitor and the message cannot be pushed to the watch normally. At this time, it can be solved by re-switching the permissions.

#### 8, call reminder, SMS reminder
Incoming call reminders and SMS reminders require dynamic registration of call permissions and SMS permissions. If the app needs to be put on Google Play, you need to apply for privacy permission. When applying for permission, you need to shoot a video of the demonstration, and provide the app product description and privacy agreement in strict accordance with Google's requirements. An example of use is as follows:
```
    /**
     *  来电提醒
     */
    public void incomingCall(){}
    FissionSdkBleManage.getInstance().incomingCall((System.currentTimeMillis() / 1000 ),name,number);

```
The SMS reminder is the same as the sending code of the App message reminder, and the push type can be set to SMS.

#### 9, set the weather
To set the weather function, you need to get the weekly weather forecast and detailed weather data of the day from the Meteorological Bureau or other external APIs such as Yahoo. The background server can reduce the number of concurrent requests and reduce expenses through caching. After the weather is successfully obtained, the sample code for synchronizing to the watch is as follows:
```
      /**
       *  App 天气消息推送
       */
      public void setWeather(){}

      /**
       *  App 天气消息详情推送
       */
      public void setWeatherDetail(){}

      for (int i = 0;i<7;i++){
          WeatherParam detail = new WeatherParam();
          detail.setLowestTemperature(getRandom(-30,20));
          detail.setMaximumTemperature(getRandom(-10,40));
          detail.setIndex(i);
          detail.setWeather(getRandom(1,18));
          todayWeatherDetails.add(detail);
      }
      FissionSdkBleManage.getInstance().setWeather(todayWeatherDetails);

      TodayWeatherDetail  todayWeatherDetail = new TodayWeatherDetail();
      todayWeatherDetail.setLowSetTmp(lowTmp);
      todayWeatherDetail.setHighSetTmp(highTmp);
      todayWeatherDetail.setTemperature(curTmp);
      todayWeatherDetail.setWeatherCode(weatherCode);

      FissionSdkBleManage.getInstance().setWeatherDetail(todayWeatherDetail);
```
#### 10, set female health data
The female health function needs to be supported by the watch version before it can be set, and the user information needs to be set as female to be displayed normally. The sample code is as follows:
```
      /**
       *  设置女性生理周期
       */
      public void setFemalePhysiology(){}

     int femaleModel = Integer.parseInt(ed_health_settings.getText().toString().trim());
     int menstrualAdvance = Integer.parseInt(ed_reminder_days.getText().toString().trim());
     int duration = Integer.parseInt(ed_menstrual_days.getText().toString().trim());
     int intervalPeriod = Integer.parseInt(ed_cycle_days.getText().toString().trim());
     int remindTime = Integer.parseInt(ed_reminder_time.getText().toString().trim());
     int pregnancyRemindType = Integer.parseInt(ed_pregnancy_reminder_mode.getText().toString().trim());
     Calendar calendar = Calendar.getInstance();
     calendar.setTime(TimeUtils.string2Date(ed_date.getText().toString().trim(), "yyyy/MM/dd"));
     FemalePhysiology femalePhysiology = new FemalePhysiology(femaleModel, menstrualAdvance, duration, intervalPeriod,
             calendar, pregnancyRemindType, remindTime, switch_open.isChecked());
     FissionSdkBleManage.getInstance().setFemalePhysiology(femalePhysiology);
```
FemalePhysiology
```
    private int psyMode;//Period Mode 0 Off 1 Period Mode 2 Pregnancy Mode 3 Pregnancy Mode
    private int periodAdvanceDay;//Days before menstruation start reminder
    private int durationDay;//duration of period
    private int periodWeek;//interval period
    /**
          * @param lastPeriodTime indicates the last menstrual period
          * This field occupies 3 bytes, which are year, month and day
          * The sdk will automatically do calculations, so pass Calender here
          */
    Calendar lastPeriodTime;
    int pregnancyReminderMode;//Pregnancy prompt mode 0: prompts the number of days pregnant 1: prompts the number of days before the expected date of delivery
    long hourOfTime;//Reminder time (hour/minute,  such as 12:00)
    boolean deviceRemind;//Device reminder switch
```

#### 11, data synchronization
Synchronize the health data and exercise data saved on the watch to the App for display. The data modules supported by the SDK for synchronization are as follows:
   > Real-time measurement data\
    Daily Activity Statistics\
    Hourly Activity Statistics\
    Sleep Statistics Report\
    sleep statistics
    Sleep real-time record\
    Sports Statistics Report\
    Sports record list\
    Sports Details Record\
    heart rate record\
    Pedometer record\
    blood oxygen record\
    blood pressure record\
    real-time streaming data

The SDK needs to add a callback listener, and then call the method of synchronizing data. For details, please refer to the demo, the sample code is as follows:
* Get real-time measurement data
```
  /**
   *  获取实时测量数据
   */
  public void getMeasureInfo(MeasureInfo measureInfo){}
  FissionSdkBleManage.getInstance().getMeasureInfo();
```
MeasureInfo
```
     private int bodyVersion; // 结构体版本
     private int step;       // 累计步数
     private int calorie;    // 累计消耗卡路里
     private int distance;   // 累计行程
     private int sumDistance;   // 轨迹累计行程
     private int sumHR;      // 轨迹累计行程
     private int hrNumber;   // 心率和
     private int avgHR;      // 平均心率
     private int hr;         // 心率
     /** 实时心率等级
      * 0:NORMAL（正常的）
      * 1:MODERATE（缓和的）
      * 2:VIGOROUS（充沛的）
      * 3:MAX_HR
      * 4:TAKE_IT_EASY
      * 5:WATCH_YOUR_LIMITS
      * 6:DONT_OVEREXERT
      */
     private int hrLevel;
     private int maxHR;  // 最高心率
     private int minHR;  // 最低心率
     private int sumBloodOxygen;     // 血氧和
     private int bloodOxygenNumber;  // 血氧检测次数
     private int avgBloodOxygen;     // 平均血氧
     private int bloodOxygen;        // 血氧
     /**
      * 血氧等级
      * 0：正常缺氧
      * 1：轻度缺氧
      * 2：中度缺氧
      * 3：重度缺氧
      */
     private int bloodOxygenLevel;
     private int maxBloodOxygen; // 最大血氧
     private int minBloodOxygen; // 最小血氧
     private int reliability;    // 可信度，取值范围0-3
     private int batteryLevel;   // 保存电池电量等级
     private int SBP;            // 收缩血压
     private int DBP;            // 舒张血压
     private int sumSBP;         // 收缩压和
     private int sumDBP;         // 舒张压和
     private int bpNumber;       // 当天血压检测次数
     private int avgSBP;         // 平均收缩血压
     private int avgDBP;         // 平均舒张血压
     private int sumExerciseTime;        // 目前累计运动时间
     private int sumViolentExerciseTime; // 目前累计激烈运动时间
     private int startSleepTime;         // 本次开始睡觉时间
     private int endSleepTime;           // 本次结束睡觉时间
     private int sleepTime;              // 睡眠时间（分）
     private int sumSleepLatency;       // 本次睡眠中累计入睡时间(分钟)
     private int sumSleepAwakeTime;     // 本次睡眠中累计清醒时间(分钟)
     private int sumSleepDeepTime;      // 本次睡眠深睡累计时间(分钟)
     private int sumSleepLightTime;     // 本次睡眠浅睡累计时间(分钟)
     private int sumSittingTime;       // 当天发生久坐累计时间
     private int sittingAvgStep;       // 当天久坐期间平均步数，步数/小时
     private int doneTargetStep;       // 目前已经完成的目标步数
     private int meditationTime;       // 当天冥想时间分钟
     private int dayExerciseTime;      // 当天累计运动时间
     private int dayActivityNumber;      // 当天活动次数
     private int pace;                 // 实时配速，由应用统计，分钟/千米
     private int gpsPace;              // gps模式下，实时配速
     private double gpsSpeed;             // gps模式下，实时速度，单位m/s
```
* Get daily activity statistics (startTime, endTime second timestamp)
```
    /**
     *  获取每日活动统计
     */
    public void getDaysReport(List<DaysReport> daysReports){}
    FissionSdkBleManage.getInstance().getDaysReport(startTime,endTime);
```
DaysReport
```
    private int time;          // 时间戳GMT秒
    private int bodyVersion; // 结构体版本
    private int step;       // 当天累计计步数
    private int calorie;    // 当天累计消耗卡洛里
    private int distance;   // 当天累计行程
    private int avgHR;      //当天平均心率
    private int highHR;     //当天最高心率
    private int lowHR;      // 当天最低心率
    private int avgBloodOxygen; // 当天平均血氧
    private int sportTime;  // 当天累计运动时间
    private int intenseTime;   // 当天累计激烈运动时间
    private int deepSleepTime; // 当天深度睡眠时间，分钟
    private int lightSleepTime; // 当天浅睡时间，分钟
    private int highBloodPressure;  // 当天最高血压
    private int lowBloodPressure;   // 当天最低血压
    private int eyeTime; //当天睡眠累计眼动时间(分钟)
```
* Get hourly activity statistics (startTime, endTime second timestamp)
```
    /**
     *  获取整点活动统计
     */
    public void getHoursReport(List<HoursReport> hoursReports){}
    FissionSdkBleManage.getInstance().getHoursReport(startTime, endTime);
```
HoursReport
```
    private int time;           // 时间戳GMT秒
    private int bodyVersion;    // 结构体版本
    private int step;           // 当天累计计步数
    private int calorie;        // 当天累计消耗卡洛里
    private int distance;       // 当天累计行程
```
* Get sleep statistics report
```
    /**
     * 获取睡眠统计报告
     */
    public void getSleepReport(List<SleepReport> sleepReports){}
    FissionSdkBleManage.getInstance().getSleepReport(startTime, endTime);
```
SleepReport
```
    private int time;               // 时间戳GMT秒
    private int bodyVersion;        // 结构体版本
    //保留字节
    private int startTime;          // 本次开始睡觉时间,单位秒
    private int endTime;            // 本次结束睡觉时间,单位秒
    private int totalTime;          // 本次睡眠持续总时间(分钟)
    private int totalSoberTime;     // 本次睡眠清醒累计时间(分钟)
    private int totalLightTime;     // 本次睡眠浅睡累计时间(分钟)
    private int totalDeepTime;      // 本次睡眠深睡累计时间(分钟)
    private int maxBloodOxygen;     // 本次睡眠时最大血氧
    private int minBloodOxygen;     // 本次睡眠时最小血氧
    private int maxHR;              // 本次睡眠时最大心率
    private int minHR;              // 本次睡眠时最小心率
    private boolean effectivity;    // 本次报告结果 0:无效 1:有效
    private int eyeMovementTime;    //本次睡觉眼动时间(分钟)
```
* Get sleep statistics record
```
    /**
     *获取睡眠状态记录
     */
    public void getSleepRecord(List<SleepRecord> sleepRecords){}
    FissionSdkBleManage.getInstance().getSleepRecord(startTime, endTime);
```
SleepRecord
```
    private int          utcTime;               // 时间戳GMT秒
    private int          bodyVersion;        // 结构体版本 v=0 不支持眼动,睡眠结构体个数 N=50. v=1 支持眼动,睡眠结构体个数 N=200
    private boolean      isNap;             //是否支持小睡
    private int          napStartPosition;  //小睡开始位置
    private int          startTime;          // 开始睡眠时间,单位秒
    private int          endTime;            // 结束睡眠时间,单位秒
    private int          deepTime;           // 当天深度睡眠时间单位分钟
    private int          lightTime;           // 当天浅度睡眠时间单位分钟
    private int          eyeTime;             //当天眼动的时间,单位分钟
    private int          number;             // 此记录块包含有效记录条数,无效为0
    public  List<Detail> details;

    public class Detail {
        /**
         * 0：awake state
         * 1：light sleep
         * 2：deep sleep
         * 3：eye movement
         * 4：take a nap
         * 5：at work
         * 6：wakefulness between naps
         */
        public int status;
        public int time; // duration
```
* Get real-time sleep records
```
    /**
     * 获取当前睡眠实时状态记录
     */
    public void getCurSleepRecord(SleepRecord sleepRecord){}
    FissionSdkBleManage.getInstance().getCurSleepRecord();
```
* Get Sports Statistics Report
```
    /**
      * 获取运动统计报告
      */
     public void getExerciseReport(List<ExerciseReport> exerciseReports){}
     FissionSdkBleManage.getInstance().getExerciseReport(startTime, endTime);
```
ExerciseReport
```
     private int                        utcTime;// 时间戳UTC秒
     private int                        bodyVersion;// 结构体版本
     private int                        beginTime;// The timestamp of the start of the exercise, as the unique identification id of each exercise, in seconds
     private int                        endTime;// Movement end timestamp, in seconds
     private int                        totalTime;// Total exercise time, unit second
     private int                        totalStep;// total number of steps
     private int                        totalCalorie;// total exercise calories
     private int                        totalDistance;// Total exercise distance (in meters, estimated by step counting)
     private int                        totalTrackDistance;// The movement distance of this movement track (in meters, calculated by GPS positioning)
     private int                        model; // Sport mode:
     private int                        highHR;// The maximum heart rate of this exercise (unit: times/minute)
     private int                        lowHR;// The minimum heart rate of this exercise (unit: times/minute)
     private int                        avgHR;// The average heart rate of this exercise, calculated at the end of the exercise, heart rate and/recorded times (unit: times/minute)
     private int                        maxStride; // The maximum stride frequency of this exercise (unit: step/minute)
     private int                        avgStride; // Average stride frequency of this exercise = stride frequency and/record times (unit: step/minute)
     private int                        sportCount;// Number of exercises, number of breaks
     private List<ExerciseReportDetail> details; //Interrupt the UTC record, and at the same time, it is used to count the total exercise time and pause the exercise time, in seconds, 20 consecutive groups
     private float                      maxSpeed;// The maximum speed of this movement (unit: m/s)
     private float                      avgSpeed;//Average speed of this movement = distance/time (unit: m/s)
     private int                        notTrackAvgSpeed;// The average pace of this trackless exercise (unit: hour/km)
     private int                        hasTrackAvgSpeed;// This time there is track exercise pace (unit: hour/km)
     private int                        repeatSportWeek;// The number of cycles of repeated motion (number of back and forth, number of turns) (unit: circle)
     private int                        swingNumber;// Number of arm swings, number of strokes (unit: times)
     private int warmUpEsTime;//Warm up time
     private int fatBurningTime;//fat burning exercise time
     private int aerobicEnduranceTime;//Aerobic endurance exercise time
     private int highAerobicEnduranceTime;//high-intensity aerobic endurance exercise time
     private int anaerobicTime;//Anaerobic exercise time
```
* Get list of exercise records
```
    /**
     * 获取运动记录列表
     */
    public void getExerciseList(List<ExerciseList> exerciseLists){}
    FissionSdkBleManage.getInstance().getExerciseList(startTime, endTime);
```
ExerciseList
```
  private int time; // 时间戳GMT秒
    private int bodyVersion; // 结构体版本
    private int beginTime; // 运动开始时间，GMT秒。
    private int endTime; // 运动开始时间，GMT秒。
    /**
     * sports mode

        int SPORT_WALK                = 0,
            SPORT_RUNNING             = 1,
            SPORT_MOUNTAINEERING      = 2,
            SPORT_CYCLING             = 3,
            SPORT_FOOTBALL            = 4,
            SPORT_SWIMMING            = 5,
            SPORT_BASKETBALL          = 6,
            SPORT_NO_DESIGNATION      = 7,
            SPORT_OUTDOOR_RUNNING     = 8,
            SPORT_INDOOR_RUNNING      = 9,
            SPORT_REDUCE_FAT_RUNNING  = 10,
            SPORT_OUTDOOR_WALKING     = 11,
            SPORT_INDOOR_WALKING      = 12,
            SPORT_OUTDOOR_CYCLE       = 13,
            SPORT_INDOOR_CYCLING      = 14,
            SPORT_FREE_TRAINING       = 15,
            SPORT_FITNESS_TRAINING    = 16,
            SPORT_BADMINTON           = 17,
            SPORT_VOLLEYBALL          = 18,
            SPORT_PING_PONG           = 19,
            SPORT_ELLIPTICAL          = 20,
            SPORT_ROWING_MACHINE      = 21,
            SPORT_YOGA                = 22,
            SPORT_STRENGTH_TRAINING   = 23,
            SPORT_CRICKET             = 24,
            SPORT_JUMP_ROPE           = 25,
            SPORT_AEROBIC_EXERCISE    = 26,
            SPORT_DANCING             = 27,
            SPORT_TAICHI              = 28,
            SPORT_AUTO_RUNNING        = 29,
            SPORT_AUTO_WALKING        = 30,
            SPORT_INDOOR_WALK         = 31,
            SPORT_STEP_TRAINING       = 32,
            SPORT_HORSE_RIDING        = 33,
            SPORT_HOCKEY              = 34,
            SPORT_INDOOR_CYCLE        = 35,
            SPORT_SHUTTLECOCK         = 36,
            SPORT_BOXING              = 37,
            SPORT_OUTDOOR_WALK        = 38,
            SPORT_TRAIL_RUNNING       = 39,
            SPORT_SKIING              = 40,
            SPORT_GYMNASTICS          = 41,
            SPORT_ICE_HOCKEY          = 42,
            SPORT_TAEKWONDO           = 43,
            SPORT_VO2MAX_TEST         = 44,
            SPORT_AIR_WALKER          = 45,
            SPORT_HIKING              = 46,
            SPORT_TENNIS              = 47,
            SPORT_DANCE               = 48,
            SPORT_ATHLETICS           = 49,
            SPORT_WAIST_TRAINING      = 50,
            SPORT_KARATE              = 51,
            SPORT_COOL_DOWN           = 52,
            SPORT_CROSS_TRAINING      = 53,
            SPORT_PILATES             = 54,
            SPORT_CROSS_FIT           = 55,
            SPORT_FUNCTIONAL_TRAINING = 56,
            SPORT_PHYSICAL_TRAINING   = 57,
            SPORT_ARCHERY             = 58,
            SPORT_FLEXIBILITY         = 59,
            SPORT_MIXED_CARDIO        = 60,
            SPORT_LATIN_DANCE         = 61,
            SPORT_STREET_DANCE        = 62,
            SPORT_KICKBOXING          = 63,
            SPORT_BARRE               = 64,
            SPORT_AUSTRALIAN_FOOTBALL = 65,
            SPORT_MARTIAL_ARTS        = 66,
            SPORT_STAIRS              = 67,
            SPORT_HANDBALL            = 68,
            SPORT_BASEBALL            = 69,
            SPORT_BOWLING             = 70,
            SPORT_RACQUETBALL         = 71,
            SPORT_CURLING             = 72,
            SPORT_HUNTING             = 73,
            SPORT_SNOWBOARDING        = 74,
            SPORT_PLAY                = 75,
            SPORT_AMERICAN_FOOTBALL   = 76,
            SPORT_HAND_CYCLING        = 77,
            SPORT_FISHING             = 78,
            SPORT_DISC                = 79,
            SPORT_RUGBY               = 80,
            SPORT_GOLF                = 81,
            SPORT_FOLK_DANCE          = 82,
            SPORT_DOWNHILL_SKIING     = 83,
            SPORT_SNOW                = 84,
            SPORT_MIND_BODY           = 85,
            SPORT_CORE_TRAINING       = 86,
            SPORT_SKATING             = 87,
            SPORT_FITNESS_GAMING      = 88,
            SPORT_AEROBICS            = 89,
            SPORT_GROUP_TRAINING      = 90,
            SPORT_KENDO               = 91,
            SPORT_LACROSSE            = 92,
            SPORT_ROLLING             = 93,
            SPORT_WRESTLING           = 94,
            SPORT_FENCING             = 95,
            SPORT_SOFTBALL            = 96,
            SPORT_SINGLE_BAR          = 97,
            SPORT_PARALLEL_BARS       = 98,
            SPORT_ROLLER_SKATING      = 99,
            SPORT_HULA_HOOP           = 100,
            SPORT_DARTS               = 101,
            SPORT_PICKLE_BALL         = 102,
            SPORT_SIT_UP              = 103,
            SPORT_HIIT                = 104,
            SPORT_TREADMILL           = 106,
            SPORT_BOATING             = 107,
            SPORT_JUDO                = 108,
            SPORT_TRAMPOLINE          = 109,
            SPORT_SKATEBOARDING       = 110,
            SPORT_HOVERBOARD          = 111,
            SPORT_BLADING             = 112,
            SPORT_PARKOUR             = 113,
            SPORT_DIVING              = 114,
            SPORT_SURFING             = 115,
            SPORT_SNORKELING          = 116,
            SPORT_PULL_UP             = 117,
            SPORT_PUSH_UP             = 118,
            SPORT_PLANKING            = 119,
            SPORT_ROCK_CLIMBING       = 120,
            SPORT_HIGH_JUMP           = 121,
            SPORT_BUNGEE_JUMPING      = 122,
            SPORT_LONG_JUMP           = 123,
            SPORT_SHOOTING            = 124,
            SPORT_MARATHON            = 125;
     */
    private int model;
```

* Get exercise details record
```
    /**
     *  运动详情记录
     */
    public void getExerciseDetail(List<ExerciseDetail> exerciseDetails){}
    FissionSdkBleManage.getInstance().getExerciseDetail(exerciseList.getBeginTime(), exerciseList.getEndTime());
```
ExerciseDetail
```
    private int                        time; // The formation timestamp of the first record (structure) in GMT seconds
    private int                        bodyVersion;// struct version
    private int                        week; // Record generation cycle, in seconds
    private int                        effectiveNumber; // This record block contains the number of valid records
    private int                        recordLength;  // The length of a single record, that is, the size of a single record structure
    private int                        type; // Record type: 0: heart rate record
    private List<ExerciseDetailRecord> exerciseDetailRecords;
    public class ExerciseDetailRecord {
        private int utcTime;//utc
        private int pace;      // Real-time pace, unit s/km
        private int frequency;  // real-time pace
        private int calorie;    // Calories burned in one minute
        private int steps;      // steps in one minute
        private int distance;   // real-time distance in motion
        private int heartRate;  // real-time heart rate
        private int stamina;    // Real-time physical strength, 0~100
        private int state;      // state. 0 normal, 1 pause
```
* Get heart rate records
```
    /**
     * 获取心率记录
     */
    public void getHeartRateRecord(List<HeartRateRecord> heartRateRecords){}
    FissionSdkBleManage.getInstance().getHeartRateRecord(startTime,endTime);
```
HeartRateRecord
```
    private long          time; // 第一条记录（结构体）的形成时间戳GMT秒
    private int           bodyVersion;// 结构体版本
    private long          week; // Record generation cycle, in seconds
    private int           effectiveNumber; // This record block contains the number of valid records
    private int           recordLength;  // The length of a single record, that is, the size of a single record structure
    private int           type; // Record type: 0: heart rate record
    private List<Integer> hrList; //collection of heart rate values
    private List<Long>    hrListTime; //Collection of each heart rate test time
```
* Get pedometer records
```
    /**
     * 获取计步记录
     */
    public void getStepsRecord(List<StepsRecord> stepsRecords){}
    FissionSdkBleManage.getInstance().getStepsRecord(startTime,endTime);
```
StepsRecord
```
    private int time;               // 时间戳GMT秒
    private int bodyVersion;        // 结构体版本
    private int week;               // Record generation cycle, in seconds
    private int number;             // This record block contains the number of valid records
    private int length;             // The length of a single record, that is, the size of a single record structure
    private int type;               // Record type: 1: step counting detail record.
    private  List<Long> utcTime;// The time recorded for each step

    private List<Integer> steps; //The number of steps recorded per time
```
* Get Blood Oxygen Records
```
    /**
     *  获取血氧记录
     */
    public void getSpo2Record(List<Spo2Record> spo2Records){}
    FissionSdkBleManage.getInstance().getSpo2Record(startTime,endTime);
```
Spo2Record
```
    private int           time;               // 时间戳GMT秒
    private int           bodyVersion;        // 结构体版本
    private int           week;               // Record generation cycle, in seconds
    private int           number;             // This record block contains the number of valid records
    private int           length;             // The length of a single record, that is, the size of a single record structure
    private int           type;               // Record type: 2: blood oxygen detailed record.
    private int           keepData;//reserved bytes
    private List<Integer> spList;  //blood oxygen value
    private List<Long>    utc;  // utc time
```

* Set blood oxygen automatic detection switch
```
  FissionSdkBleManage.getInstance().setBloodOxygenSwitch(value);  // 0 = off ; 1 = turn on

  FissionAtCmdResultListener  This monitor callback method with the same name can get the status of the instruction execution.
```

* Set the mental pressure automatic detection switch
```
   FissionSdkBleManage.getInstance().setMentalStressSwitch(value); // 0 = off ; 1 = turn on

   FissionAtCmdResultListener  This monitor callback method with the same name can get the status of the instruction execution.
```

* Get automatic detection of mental stress historical data
```
  FissionSdkBleManage.getInstance().getMentalStressRecord(long startTime, long endTime)

  //startTime, endTime  seconds timestamp.  FissionBigDataCmdResultListener  The monitoring method with the same name can get the corresponding data.

  FissionSdkBleManage.getInstance().getHandMeasureInfo(long startTime, long endTime)   Here's how to get manual measurements. Blood oxygen, mental stress, heart rate data measured manually

```


* Get real-time streaming data
```
    /**
     * 流数据读取成功
     * @param streamData
     */
    public abstract void readStreamDataSuccess(StreamData streamData);

    /**
     * 流数据读取失败
     * @param msg
     */
    public abstract void readStreamDataFail(String msg);

    FissionSdkBleManage.getInstance().setDataStream(Integer.parseInt(content));
```
StreamData
```
    private int number;

    /**
     * heart rate
     */
    private int heartRate;

    /**
     * heart rate level
     */
    private int level;

    /**
     *  step count
     */
    private int step;

    /**
     *  distance
     */
    private int distance;

    /**
     *  calories
     */
    private int calorie;
```
#### 12, callback monitoring
All operations of the SDK need to add callback monitoring, otherwise the execution status of the operation cannot be obtained. The following monitors are mainly used:
* BleScanResultListener, the only listener, the new listener callback overrides the old one.
```
    public interface BleScanResultListener {

        /**
         * The Bluetooth scan is successful, and the scan result is returned
         * @param scanResult
         */
        void onScanResult(ScanResult scanResult);

        /**
         * The bluetooth scan failed and an exception message was returned
         * @param throwable
         */
        void onScanFailure(Throwable throwable);

        /**
         *  bluetooth scan ended
         */
        void onScanFinish();
    }
```
The exception error message is as follows:
```
      <!-- Ble scan error messages -->
      <string name="error_bluetooth_not_available">Bluetooth is not available</string>
      <string name="error_bluetooth_disabled">Enable bluetooth and try again</string>
      <string name="error_location_permission_missing">On Android 6.0 location permission is required. Please enable it in phone settings</string>
      <string name="error_location_services_disabled">Location services need to be enabled on Android 6.0</string>
      <string name="error_scan_failed_already_started">Scan with the same filters is already started</string>
      <string name="error_scan_failed_application_registration_failed">Failed to register application for bluetooth scan</string>
      <string name="error_scan_failed_feature_unsupported">Scan with specified parameters is not supported</string>
      <string name="error_scan_failed_internal_error">Scan failed due to internal error</string>
      <string name="error_scan_failed_out_of_hardware_resources">Scan cannot start due to limited hardware resources</string>
      <string name="error_undocumented_scan_throttle">Android 7+ does not allow more scans.</string>
      <string name="error_undocumented_scan_throttle_retry">Try in %d seconds</string>
      <string name="error_bluetooth_cannot_start">Unable to start scanning</string>
      <string name="error_unknown_error">Unknown error</string>
```
* BleConnectListener，The only listener, the new listener callback overrides the old one.
```
    public interface BleConnectListener {
        /**
         * BLE connection state change
         * @param newState
         */
        void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState);

        /**
         * BLE Connection failed
         * @param throwable
         */
        void onConnectionFailure(Throwable throwable);

        /**
         * Ble After the connection is successful, binding/discovering services (depending on whether binding is required)
         */
        void onBinding();

        /**
         * The binding is successful/the discovery service is successful (the connection is successful in the real sense, and the communication can be normal)
         * @param name not enabled, empty string
         */
        void onBindSucceeded(String address, String name);

        /**
         * binding failed
         * @param code
         */
        void onBindFailed(int code);
    }
```
Binding failure error code:
```
    /**
     *  绑定指令返回code
     */
    int BIND_SUCCESS_1 = 1, BIND_SUCCESS_5 = 5, BIND_SUCCESS_6 = 6,       //bind successfully
            BIND_FAIL_REPEAT = 2,         //The device is bound
            BIND_FAIL_KEY_ERROR =4,       //Binding key error
            BIND_FAIL_USER_REJECT =0,     //user declined
            BIND_FAIL_OUT_TIME =3;        //time out
```
The following three monitors can add multiple monitor objects, and add them where they need to be received. If you add multiple times, the App needs to handle the problems caused by multiple callbacks by itself. The suggestion is to make an intermediate layer to handle it, add a callback listener, and then distribute it to the place that needs to be received after the callback.
* FissionAtCmdResultListener/FissionBigDataCmdResultListener
This monitor mainly handles the functions of some simple commands, similar to the functions initiated by the App and the functions initiated by the device. When the instruction is executed successfully, the method with the same name as the sending instruction will be called back. For the specific callback method name, refer to the code example of the function module above. For details, please refer to the demo.
```
  FissionSdkBleManage.getInstance().addCmdResultListener(new FissionAtCmdResultListener() {
            @Override
            public void sendSuccess(String cmdId) {
              //指令发送成功，代表SDK已经发送成功，不代表设备端接收成功。

            }

            @Override
            public void sendFail(String cmdId) {
              //指令发送失败，SDK指令未能发送出去。

            }

            @Override
            public void onResultTimeout(String cmdId) {
               //指令发送成功， 设备端超时没有回复
            }

            @Override
            public void onResultError(String errorMsg) {
              //指令发送成功， 设备端回复的数据处理异常
            }

            //重写需要监听的方法，示例如下
            /**
             *  查找设备
             */
            @Override
            public void findDevice(){
              super.findDevice();
              //设备查找指令执行成功
            }
        });
    }

    FissionSdkBleManage.getInstance().addCmdResultListener(new FissionBigDataCmdResultListener() {
              @Override
              public void sendSuccess(String cmdId) {
                //指令发送成功，代表SDK已经发送成功，不代表设备端接收成功。

              }

              @Override
              public void sendFail(String cmdId) {
                //指令发送失败，SDK指令未能发送出去。

              }

              @Override
              public void onResultTimeout(String cmdId) {
                 //指令发送成功， 设备端超时没有回复
              }

              @Override
              public void onResultError(String errorMsg) {
                //指令发送成功， 设备端回复的数据处理异常
              }

              //重写需要监听的方法，示例如下
              @Override
              public void incomingCall() {
                  super.incomingCall();
                //来电提醒功能指令执行成功
              }
          });
      }
```
* FissionFmDataResultListener,There are examples of using the streaming data monitoring function above, please refer to the demo for details.
```
    private FissionFmDataResultListener fmDataResultListener =new FissionFmDataResultListener() {
            @Override
            public void readStreamDataSuccess(StreamData streamData) {

            }

            @Override
            public void readStreamDataFail(String msg) {

            }

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
        };
        FissionSdkBleManage.getInstance().addCmdResultListener(fmDataResultListener);
```
#### 13, DFU upgrade
This function is relatively complicated, and upgrading with the wrong file may cause the watch device to become bricked and unusable. When using this function, please be sure to communicate with the fission engineer to confirm that you have obtained the correct upgrade package.
DFU upgrade mainly includes the following functions:
> Firmware Upgrade\
   Dial Upgrade\
   Custom watch face upgrade\
   Firmware UI upgrade\
   Sports push function

     The DFU function needs to be registered in the AndroidManifest ``` <service android:name="com.realsil.sdk.dfu.DfuService"/> ```For details, please refer to the demo.

* Firmware upgrade

     Use **HardWareInfo** to read the firmware version number and adaptation number to request the server to check whether there is a new firmware version that needs to be updated. If there is a new version of the firmware, the App needs to download the firmware bin file to the phone locally. Then use the following sample code to upgrade:
    ```
        FissionSdkBleManage.getInstance().startDfu(this, filePath, FissionConstant.OTA_TYPE_FIRMWARE, new DfuAdapter.DfuHelperCallback() {
                     @Override
                     public void onStateChanged(int i) {
                         super.onStateChanged(i);
                     }

                     @Override
                     public void onTargetInfoChanged(OtaDeviceInfo otaDeviceInfo) {
                         super.onTargetInfoChanged(otaDeviceInfo);
                     }

                     @Override
                     public void onError(int i, int i1) {
                         super.onError(i, i1);
                         //处理升级失败
                     }

                     @Override
                     public void onProcessStateChanged(int i, Throughput throughput) {
                         super.onProcessStateChanged(i, throughput);
                     }

                     @Override
                     public void onProgressChanged(DfuProgressInfo dfuProgressInfo) {
                         super.onProgressChanged(dfuProgressInfo);
                         //升级中，刷新进度条
                     }
                 });
    ```
    Note: **filePath** downloads and saves the file address to the mobile phone to ensure that the file can be found normally. Pay attention to user authorization.
  * Watch face upgrade

       Use **HardWareInfo** to read the firmware version number and adaptation number to request the server and query the list of supported dials. If there is a self-supporting watch face that can be downloaded, the App needs to download the watch face bin file to the local phone.
       The dial upgrade method is the same as the firmware upgrade **startDfu**, please replace **FissionConstant.OTA_TYPE_FIRMWARE** with **FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL**.
  * Custom watch face upgrade

       Currently only the original dial with a fixed style is supported. For detailed code, please refer to the demo project **CustomDialActivity**. The main sample code is as follows:
    ```
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

                }

                @Override
                public void onUpdateDialProgress(int state, int progress) {
                    super.onUpdateDialProgress(state, progress);
                    LogUtils.d("wl", "自定义表盘推送："+progress);
                }
            });

        Bitmap bitmap = ((BitmapDrawable) iv_watch_face.getDrawable()).getBitmap();
        dialModel = new FissionDialUtil.DialModel();
        dialModel.setDialWidth(240);
        dialModel.setDialHeight(280);
        dialModel.setPreviewImage(bitmap);
        dialModel.setBackgroundImage(bitmap);
        dialModel.setDialPosition(1);
        dialModel.setPreImageWidth(240 / 3 * 2);
        dialModel.setPreImageHeight(187);
        dialModel.setDialPosition(stylePosition_middle);
        dialModel.setDialStyleColor(getResources().getColor(R.color.public_custom_dial_8));
        Bitmap thumbBitmap2 = ImageScalingUtil.extractMiniThumb(dialModel.getPreviewImage(),
                dialModel.getPreImageWidth(), dialModel.getPreImageHeight());
        File file = new File(getPath() + File.separator + "customDial.bin");
        dialModel.setFile(file);
        setDiaModel(dialModel);

        private void setDiaModel(FissionDialUtil.DialModel dialModel)  {
            Bitmap bitmap1 = getPreviewImageBitmap(this,dialModel);
            iv_watch_face2.setImageBitmap(bitmap1);
            byte[] resultData =  FissionDialUtil.getDiaInfoBinData(this,dialModel);
            FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_DIAL_DATA);
       }
    ```
    * Motion push function

         Use **HardWareInfo** to read the firmware version number and adaptation number to request the server to query the type of motion supported by the device. If there is a supported motion push, the App needs to download and save it by itself, and then convert it into **byte[]** in the form of a byte array. For detailed code, please refer to the demo project **PushSportModeActivity** Use the following codes of sdk to push:
```
    byte[] resultData =  FissionDialUtil.inputBin(this,name);
    FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_SPORT_DATA);
```

* 固件UI升级（参考demo）

### Five、Description of SDK constants
* AT command
```
    /**
     * AT指令
     */
     public interface AtCmd {
     String AT_CMD_READ_BATTERY        = "GBS";         // battery charging status
     String AT_CMD_READ_DEVICE_VERSION = "GSV";  // Read device version number
     String AT_CMD_READ_GPV            = "GPV";             // Get protocol version
     String AT_CMD_READ_TIME           = "GUT";            // Get UTC time
     String AT_CMD_READ_TIMEZONE       = "GTZ";        // get time zone

     String AT_CMD_CDC             = "CDC";                  //Disconnect
     String AT_CMD_SET_TIME        = "SUT";             // set time
     String AT_CMD_SET_TIMEZONE    = "STZ";         // set time zone
     String AT_CMD_SET_SCREEN_KEEP = "SBT";         // Set the duration of the bright screen
     String AT_CMD_GET_SCREEN_KEEP = "GBT";         // Get the duration of the bright screen
     String AT_CMD_SET_TIME_MODEL  = "SHM";       // Set the time display mode
     String AT_CMD_SET_LANG        = "SLG";             // language settings
     String AT_CMD_SET_UNIT        = "SDU";             // 0: imperial unit 1: metric unit
     String AT_CMD_SET_SFP         = "SFP";              //Set female cycle
     String AT_CMD_SET_SMI         = "SMI";                    //activate a prompt
     String AT_CMD_CVS             = "CVS";                  // Vibration reminder switch
     String AT_CMD_CWS             = "CWS";                  // Wrist up screen switch
     String AT_CMD_CPM             = "CPM";                  // Enter and exit photo mode
     String AT_CMD_CFD             = "CFD";                  // Enable and disable monitoring data flow
     String AT_CMD_CCS             = "CCS";                  // High speed connection Low speed connection
     String AT_CMD_CHD             = "CHD";                  // Heart rate mode switch
     String AT_CMD_TTP             = "TTP";                  // Instant Photo (C->H)
     String AT_CMD_TFD             = "TFD";                  // find device
     String AT_CMD_TFP             = "TFP";                  // find phone
     String AT_CMD_DFP             = "DFP";                  // Cancel Find Phone
     String AT_CMD_GRH             = "GRH";                  // resting heart rate of the day
     //    String AT_CMD_DHU = "DHU";                  // 手环拒接电话
 //    String AT_CMD_PHU = "PHU";                  // 手机拒接电话
     String AT_CMD_RST             = "RST";                  // Reboot the device
     String AT_CMD_RES             = "RES";                  // reset
     String AT_CMD_OFF             = "OFF";                  // soft shutdown
     String AT_CMD_OTA             = "OTA";                  // Start OTA upgrade
     String AT_CMD_CSC             = "CSC";                  // security confirmation
     String AT_CMD_AUT             = "AUT";                  // Start self-test mode
     String AT_CMD_CLU             = "CLU";                  // clear user information
     String AT_CMD_CLS             = "CLS";                  // clear activity data
     String AT_CMD_JMP             = "JMP";                  // interface jump
     String AT_CMD_MCS             = "MCS";                  // Music Control Sync
     String AT_CMD_MTV             = "MTV";                  // The bracelet is sent to the mobile phone MTU
     String AT_CMD_MTU             = "MTU";                  // The MTU sent by the mobile phone to the ring
     String AT_CMD_SPS             = "SPS";                  // Mobile phone system identification
     String AT_CMD_GAI             = "GAI";                  // Alarm id not used
     String AT_CMD_GPS             = "GPS";                  // GPS mode
     String AT_CMD_SUM             = "SUM";                  // ota upgrade type

     String AT_CMD_FSS = "FSS";                   //The function switch status is synchronized, and the upper and lower sides can send each other

     String AT_CMD_PCC = "PCC";                    //Answer/end calls from mobile
     String AT_CMD_DCC = "DCC";                    //Answer/end calls from the wristband
     String AT_CMD_ETM = "ETM";                    //production test mode
     String AT_CMD_STU = "STU";                    //set temperature unit
     String AT_CMD_BDQ = "BDQ";                    //submit key
     String AT_CMD_BDC = "BDC";                    //untie
     String AT_CMD_SWF = "SWF";                    //Switch to the specified watch face
     String AT_CMD_NRS = "NRS";                    //next time status
     String AT_CMD_ICF = "ICF";                    //will result in an error
     String AT_PARA    = "INVAL PARA";                    //参数错误
     String AT_CMD_ESS = "ESS";
     String AT_CMD_ICM = "ICM"; //Quick reply to incoming calls

         String AT_CMD_GUV = "GUV"; // get UI version
         String AT_CMD_SUI = "SUI"; // Incremental upgrade UI notification
         String AT_CMD_ERR = "ERR"; // exception
         String AT_CMD_GES = "GES"; // Get the movement status of the watch

         String AT_CMD_VAS = "VAS"; // language assistant switch
         String AT_CMD_VSW = "VSW"; // Master audio switch
         String AT_CMD_MVS = "MVS"; // media audio switch
 }

```
* big data command
```
public interface BigDataCmdID {
      String CMD_ID_READ_HARDWARE = "0101"; // Get device hardware information
      String CMD_ID_GET_MEASURE_INFO = "0102"; // Get the activity measurement data of the day
      String CMD_ID_GET_SYSTEM_INFO = "0103"; // Get current system dynamic information

      String CMD_ID_DAYS_REPORT = "0110"; // Get daily activity statistics
      String CMD_ID_ST_HOURS_REPORT = "0111"; // Get hourly activity statistics
      String CMD_ID_ST_SLEEP_REPORT = "0112"; // Get sleep statistics report
      String CMD_ID_ST_SLEEP_RECORD = "0113"; // Get sleep state record
      String CMD_ID_GET_SLEEP_CUR_REPORT = "0116"; // Get the current real-time sleep statistics report
      String CMD_ID_GET_SLEEP_CUR_RECORD = "0117"; // Get the current sleep real-time status record
      String CMD_ID_EXERCISE_LIST = "0114"; // Get exercise record list
      String CMD_ID_EXERCISE_REPORT = "0115"; // Get exercise statistics report

      String CMD_ID_HEART_RATE_RECORD = "0180"; // get heart rate record
      String CMD_ID_ST_STEPS_RECORD = "0181"; // Get the step count record
      String CMD_ID_ST_SPO2_RECORD = "0182"; // Get the blood oxygen record
      String CMD_ID_ST_MENTAL_STRESS_RECORD = "0183"; // Get mental stress record
      String CMD_ID_EXERCISE_DETAIL = "0184"; // Record of exercise details
      String CMD_ID_ST_EXER_GPS_DETAIL = "0185"; // Sports positioning record
      String CMD_ID_ST_EXER_HRPS_DETAIL = "0186";

      String CMD_ID_PERSONAL_INFO = "0200"; // user personal information
      String CMD_ID_GET_TIMING_INFO = "0201"; // note reminder/alarm information
      String CMD_ID_GET_CUSTOM_INFO = "0202"; // user habit data
      String CMD_ID_SEDENTARY_PARA = "0203"; // sedentary judgment parameter
      String CMD_ID_GET_HRLEV_ALGO_PARA = "0204"; // heart rate level judgment parameters
      String CMD_ID_GET_DRINK_WATER_PARA = "0205"; // drink water reminder parameters
      String CMD_GET_DONT_DISTURB_PARA = "0206"; // Do not disturb parameter
      String CMD_ID_GET_HR_CHECK_PARA = "0207"; // heart rate detection time period parameters
      String CMD_ID_GET_LIFTWRIST_PARA = "0208"; // Parameters of the time period for lifting the wrist to brighten the screen
      String CMD_ID_GET_TARGET_SET = "0209"; // Motion target parameters
      String CMD_ID_GET_HR_WARN_PARA = "020B"; // abnormal heart rate warning get
      String CMD_ID_GET_MESSAGE_TYPE_PARA = "0210"; // push message switch parameters
      String CMD_ID_GET_HAND_MEASURE_INFO = "0118"; // Get the current manual measurement data
      String CMD_ID_GET_FEMALE_PARAM = "020A";

      // String CMD_ID_ST_FLASH_ACCESS = "02F0"; // off-chip flash space data block
      String CMD_ID_GT_FLASH_ACCESS = "02E0"; // off-chip flash space data block

      String CMD_ID_SET_PERSONAL_INFO = "0300"; // Set user personal information
      String CMD_ID_ST_TIMING_INFO = "0301"; // Set note reminder/alarm information
      String CMD_ID_ST_CUSTOM_INFO = "0302"; // Set user habit data
      String CMD_ID_ST_SEDENTARY_PARA = "0303"; // sedentary judgment parameter
      String CMD_ID_ST_HRLEV_ALGO_PARA = "0304"; // heart rate level judgment parameters
      String CMD_ID_ST_HR_WARN_PARA = "030B"; // heart rate level judgment parameters
      String CMD_ID_SET_HRLEV_ALGO_PARA = "0305"; // heart rate level judgment parameters
      String CMD_ID_ST_DRINK_WATER_PARA = "0305"; // drinking water reminder parameters
      String CMD_ST_DONT_DISTURB_PARA = "0306"; // Do not disturb parameter
      String CMD_ID_ST_HR_CHECK_PARA = "0307"; // heart rate detection time period parameters
      String CMD_ID_ST_LIFTWRIST_PARA = "0308"; // Parameters of the time period for raising the wrist to brighten the screen
      String CMD_ID_ST_TARGET_SET = "0309"; // motion target parameters
      String CMD_ID_ST_MESSAGE_TYPE_PARA = "0310"; // push message switch parameters
      String CMD_ID_ST_FEMALE_PARAM = "030A"; //Set female menstrual cycle

      String CMD_ID_APPS_MESS = "0401"; // App social message push
      String CMD_ID_SET_WEATHER_MESS = "0402"; // App weather message push
      String CMD_ID_SET_WEATHER_DETAIL_MESS = "0405"; // App push weather message details
      String CMD_ID_SET_LOCATION_INFORMATION = "0406"; // App pushes mobile location information
      String CMD_ID_STRU_CALL_DATA = "0403"; // APP incoming call message push
      String CMD_ID_STRU_MUSIC_CONT = "0404"; // App pushes current song information

      String CMD_ID_ST_CUS_DIAL_DATA = "03F0"; // App writes custom dial data
      String CMD_ID_ST_CUS_SPORT_DATA = "03F1"; // App writes custom sports push data



      //Set the weather of the day
      String CMD_ID_STRU_SINGLE_WEATHER = "0405"; //weather
      String CMD_ID_ST_GPS_DATA = "02A0"; //GPS interconnection

      String CMD_ID_SET_QUICK_REPLY_DATA = "0350"; //Set quick reply data
      String CMD_ID_GET_QUICK_REPLY_DATA = "0250"; //Get quick reply data

      String CMD_ID_ST_FLASH_ACCESS = "03E0"; // Write specified data to off-chip flash space

      String CMD_ID_ST_CUS_DIAL_DATA_V2 = "03F3"; // App writes custom dial data compressed data version

      String CMD_ID_GET_BURIED_DATA = "02F2"; //App reads buried point data

      String CMD_ID_GET_CALL_CONTACT = "0251"; //App reads device call contacts
      String CMD_ID_SET_CALL_CONTACT = "0351"; //App set device call contact

      String CMD_ID_ST_REMOTE_DIAL_DATA = "03F4";

      String CMD_ID_ST_QLZ_DATA = "03F5"; //App pushes any qlz compressed data to the specified location
}
```
* FissionEnum
```
    /**
         * 支持功能
         */
        String SUPPORT_ZONE = "support_zone";//是否支持时区

        /**
         * 天气
         */
        int WT_SUNNY               = 0,//晴
            WT_PARTLY_CLOUDY       = 1,//多云
            WT_WIND                = 2,//风
            WT_CLOUDY              = 3,//阴天
            WT_LIGHT_RAIN          = 4,//小雨
            WT_HEAVY_RAIN          = 5,//大雨
            WT_SNOW                = 6,//中雪
            WT_THUNDER_SHOWER      = 7,//雷阵雨
            WT_SUNNY_NIGHT         = 8,//夜间晴
            WT_PARTLY_CLOUDY_NIGHT = 9,//夜间多云
            WT_SANDSTORM           = 10,//沙尘暴
            WT_SHOWERS             = 11,//阵雨
            WT_NIGHT_SHOWERS       = 12,//夜间阵雨
            WT_SLEET               = 13,//雨夹雪
            WT_SMOG                = 14,//雾霾
            WT_LIGHT_SNOW          = 15,//小雪
            WT_HEAVY_SNOW          = 16,//大雪
            WT_UNKNOWN             = 255;

        /**
         * 语言
         */
        int LG_CHN        = 0,//中文
            LG_EN         = 1,//英文
            LG_JP         = 2,//日语
            LG_FRENCH     = 3,//法语
            LG_GERMAN     = 4,//德语
            LG_SPANISH    = 5,//西班牙语
            LG_ITALIAN    = 6,//意大利语
            LG_PORTUGUESE = 7,//葡萄牙语
            LG_RUSSIAN    = 8,//俄语
            LG_CZECH      = 9,//捷克语
            LG_POLISH     = 10,//波兰语
            LG_TR_CHN     = 11,//繁体中文
            LG_ARABIC     = 12,//阿拉伯语
            LG_TURKISH    = 13,//土耳其语
            LG_VIETNAMESE = 14,//越南语
            LG_KOREAN     = 15,//韩语
            LG_HEBREW     = 16,//希伯来语
            LG_THAI       = 17,//泰语
            LG_INDONESIAN = 18,//印度尼西亚语
            LG_DUTCH      = 19,//荷兰语
            LG_GREEK      = 20;//希腊语
        //功能开关状态同步
        int SC_BODY                = 1,
            SC_VIBRATION           = 2,
            SC_DND                 = 3,
            SC_ALARM1              = 4,
            SC_ALARM2              = 5,
            SC_ALARM3              = 6,
            SC_ALARM4              = 7,
            SC_ALARM5              = 8,
            SC_LOW_BLOOD           = 9,
            SC_DAY_TARGET          = 10,
            SC_WEEK_TARGET         = 11,
            SC_SELF_ENCOURAGEMENT  = 12,
            SC_HEAR_RATE_EXCEEDED  = 13,
            SC_WEAR_NOTIFICATION   = 14,
            SC_CAMERA_MODE         = 15,//拍照模式开关状态
            SC_BATTERY_STATUS      = 16,
            SC_MUSIC               = 17,
            SC_BRIGHT_SCREEN_TIME  = 18,
            SC_WRIST_SCREEN_ENABLE = 19,
            SC_CUR_BATTERY_PERCENT = 20,//电池当前百分比
            SC_SWITCH_WATER        = 21,//喝水提醒开关
            SC_SWITCH_SIT_DOW      = 22,//久坐提醒开关
            SC_OTA_PERCENT         = 23,//OTA百分比
            SC_PHONE_SILENT        = 24;//手机静音

        int WRITE_DIAL_DATA  = 1, //写入表盘数据
            WRITE_SPORT_DATA = 2; //写入运动推送数据

        //自定义表盘升级状态
        int CUS_DIAL_UPDATE_SUCCESS = 0,
            CUS_DIAL_UPDATE_FAILED  = -1;

        //运动类型
        int SPORT_WALK                = 0,
            SPORT_RUNNING             = 1,
            SPORT_MOUNTAINEERING      = 2,
            SPORT_CYCLING             = 3,
            SPORT_FOOTBALL            = 4,
            SPORT_SWIMMING            = 5,
            SPORT_BASKETBALL          = 6,
            SPORT_NO_DESIGNATION      = 7,
            SPORT_OUTDOOR_RUNNING     = 8,
            SPORT_INDOOR_RUNNING      = 9,
            SPORT_REDUCE_FAT_RUNNING  = 10,
            SPORT_OUTDOOR_WALKING     = 11,
            SPORT_INDOOR_WALKING      = 12,
            SPORT_OUTDOOR_CYCLE       = 13,
            SPORT_INDOOR_CYCLING      = 14,
            SPORT_FREE_TRAINING       = 15,
            SPORT_FITNESS_TRAINING    = 16,
            SPORT_BADMINTON           = 17,
            SPORT_VOLLEYBALL          = 18,
            SPORT_PING_PONG           = 19,
            SPORT_ELLIPTICAL          = 20,
            SPORT_ROWING_MACHINE      = 21,
            SPORT_YOGA                = 22,
            SPORT_STRENGTH_TRAINING   = 23,
            SPORT_CRICKET             = 24,
            SPORT_JUMP_ROPE           = 25,
            SPORT_AEROBIC_EXERCISE    = 26,
            SPORT_DANCING             = 27,
            SPORT_TAICHI              = 28,
            SPORT_AUTO_RUNNING        = 29,
            SPORT_AUTO_WALKING        = 30,
            SPORT_INDOOR_WALK         = 31,
            SPORT_STEP_TRAINING       = 32,
            SPORT_HORSE_RIDING        = 33,
            SPORT_HOCKEY              = 34,
            SPORT_INDOOR_CYCLE        = 35,
            SPORT_SHUTTLECOCK         = 36,
            SPORT_BOXING              = 37,
            SPORT_OUTDOOR_WALK        = 38,
            SPORT_TRAIL_RUNNING       = 39,
            SPORT_SKIING              = 40,
            SPORT_GYMNASTICS          = 41,
            SPORT_ICE_HOCKEY          = 42,
            SPORT_TAEKWONDO           = 43,
            SPORT_VO2MAX_TEST         = 44,
            SPORT_AIR_WALKER          = 45,
            SPORT_HIKING              = 46,
            SPORT_TENNIS              = 47,
            SPORT_DANCE               = 48,
            SPORT_ATHLETICS           = 49,
            SPORT_WAIST_TRAINING      = 50,
            SPORT_KARATE              = 51,
            SPORT_COOL_DOWN           = 52,
            SPORT_CROSS_TRAINING      = 53,
            SPORT_PILATES             = 54,
            SPORT_CROSS_FIT           = 55,
            SPORT_FUNCTIONAL_TRAINING = 56,
            SPORT_PHYSICAL_TRAINING   = 57,
            SPORT_ARCHERY             = 58,
            SPORT_FLEXIBILITY         = 59,
            SPORT_MIXED_CARDIO        = 60,
            SPORT_LATIN_DANCE         = 61,
            SPORT_STREET_DANCE        = 62,
            SPORT_KICKBOXING          = 63,
            SPORT_BARRE               = 64,
            SPORT_AUSTRALIAN_FOOTBALL = 65,
            SPORT_MARTIAL_ARTS        = 66,
            SPORT_STAIRS              = 67,
            SPORT_HANDBALL            = 68,
            SPORT_BASEBALL            = 69,
            SPORT_BOWLING             = 70,
            SPORT_RACQUETBALL         = 71,
            SPORT_CURLING             = 72,
            SPORT_HUNTING             = 73,
            SPORT_SNOWBOARDING        = 74,
            SPORT_PLAY                = 75,
            SPORT_AMERICAN_FOOTBALL   = 76,
            SPORT_HAND_CYCLING        = 77,
            SPORT_FISHING             = 78,
            SPORT_DISC                = 79,
            SPORT_RUGBY               = 80,
            SPORT_GOLF                = 81,
            SPORT_FOLK_DANCE          = 82,
            SPORT_DOWNHILL_SKIING     = 83,
            SPORT_SNOW                = 84,
            SPORT_MIND_BODY           = 85,
            SPORT_CORE_TRAINING       = 86,
            SPORT_SKATING             = 87,
            SPORT_FITNESS_GAMING      = 88,
            SPORT_AEROBICS            = 89,
            SPORT_GROUP_TRAINING      = 90,
            SPORT_KENDO               = 91,
            SPORT_LACROSSE            = 92,
            SPORT_ROLLING             = 93,
            SPORT_WRESTLING           = 94,
            SPORT_FENCING             = 95,
            SPORT_SOFTBALL            = 96,
            SPORT_SINGLE_BAR          = 97,
            SPORT_PARALLEL_BARS       = 98,
            SPORT_ROLLER_SKATING      = 99,
            SPORT_HULA_HOOP           = 100,
            SPORT_DARTS               = 101,
            SPORT_PICKLE_BALL         = 102,
            SPORT_SIT_UP              = 103,
            SPORT_HIIT                = 104,
            SPORT_TREADMILL           = 106,
            SPORT_BOATING             = 107,
            SPORT_JUDO                = 108,
            SPORT_TRAMPOLINE          = 109,
            SPORT_SKATEBOARDING       = 110,
            SPORT_HOVERBOARD          = 111,
            SPORT_BLADING             = 112,
            SPORT_PARKOUR             = 113,
            SPORT_DIVING              = 114,
            SPORT_SURFING             = 115,
            SPORT_SNORKELING          = 116,
            SPORT_PULL_UP             = 117,
            SPORT_PUSH_UP             = 118,
            SPORT_PLANKING            = 119,
            SPORT_ROCK_CLIMBING       = 120,
            SPORT_HIGH_JUMP           = 121,
            SPORT_BUNGEE_JUMPING      = 122,
            SPORT_LONG_JUMP           = 123,
            SPORT_SHOOTING            = 124,
            SPORT_MARATHON            = 125;

        static int getWeatherCode(int weatherCode, String deviceName) {
            int weatherCodes = FissionEnum.WT_SUNNY;
            try {
                switch (weatherCode) {
                    case 0://未知
                        if (deviceName.contains("LW39") || deviceName.contains("DIZO Watch 2 Sports") || deviceName.contains("G20")) {
                            weatherCodes = FissionEnum.WT_UNKNOWN;
                        } else {

                        }
                    case 1:// 晴天
                        weatherCodes = FissionEnum.WT_SUNNY;
                        break;
                    case 2:// 多云
                        weatherCodes = FissionEnum.WT_PARTLY_CLOUDY;
                        break;
                    case 3:// 阴天
                        weatherCodes = FissionEnum.WT_CLOUDY;
                        break;
                    case 4:// 阵雨
                        weatherCodes = FissionEnum.WT_SHOWERS;
                        break;
                    case 5:// 雷阵雨、雷阵雨伴有冰雹
                        weatherCodes = FissionEnum.WT_THUNDER_SHOWER;
                        break;
                    case 6:// 小雨
                        weatherCodes = FissionEnum.WT_LIGHT_RAIN;
                        break;
                    case 7:// 中雨
                    case 9:// 暴雨
                    case 8:// 大雨
                        weatherCodes = FissionEnum.WT_HEAVY_RAIN;
                        break;
                    case 10:// 雨夹雪、冻雨
                        weatherCodes = FissionEnum.WT_SLEET;
                        break;
                    case 11:// 小雪
                        weatherCodes = FissionEnum.WT_LIGHT_SNOW;
                        break;
                    case 12:// 大雪
                    case 13:// 暴雪
                        if (deviceName.contains("LW39") || deviceName.contains("DIZO Watch 2 Sports")) {
                            weatherCodes = FissionEnum.WT_HEAVY_SNOW;
                        } else {
                            weatherCodes = FissionEnum.WT_LIGHT_SNOW;
                        }
                        break;
                    case 14:// 沙尘暴、浮尘
                        weatherCodes = FissionEnum.WT_SANDSTORM;
                        break;
                    case 15:// 雾、雾霾
                        weatherCodes = FissionEnum.WT_SMOG;
                        break;
                    default:
                        weatherCodes = FissionEnum.WT_UNKNOWN;
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return weatherCodes;
        }

        //语言
        //语言
        static int getFissionLg(int languageType) {
            int lgType = 0;
            switch (languageType) {
                case 0:
                    lgType = FissionEnum.LG_EN;
                    break;
                case 1:
                    lgType = FissionEnum.LG_CHN;
                    break;
                case 2:
                    lgType = FissionEnum.LG_TR_CHN;
                    break;
                case 3:
                    lgType = FissionEnum.LG_JP;
                    break;
                case 4:
                    //法语
                    lgType = FissionEnum.LG_FRENCH;
                    break;
                case 5:
                    //德语
                    lgType = FissionEnum.LG_GERMAN;
                    break;
                case 6:
                    //意大利
                    lgType = FissionEnum.LG_ITALIAN;
                    break;
                case 7:
                    //西班牙
                    lgType = FissionEnum.LG_SPANISH;
                    break;
                case 8:
                    //俄语
                    lgType = FissionEnum.LG_RUSSIAN;
                    break;
                case 9:
                    //葡萄牙
                    lgType = FissionEnum.LG_PORTUGUESE;
                    break;
                case 18:
                    //捷克语
                    lgType = FissionEnum.LG_CZECH;
                    break;
                case 12:
                    //波兰语
                    lgType = LG_POLISH;
                    break;
            }
            return lgType;
        }
```
* FissionConstant
```
    /**
     * 指令发送失败
     */
    int SEND_CMD_ERROR = 404;
    /**
     *  AT 绑定指令返回code
     */
    int BIND_SUCCESS_1 = 1, BIND_SUCCESS_5 = 5, BIND_SUCCESS_6 = 6,       //绑定成功
            BIND_FAIL_REPEAT = 2,         //设备已被绑定
            BIND_FAIL_KEY_ERROR =4,       //绑定秘钥错误
            BIND_FAIL_USER_REJECT =0,     //用户拒绝
            BIND_FAIL_OUT_TIME =3;        //超时

    /**
     *  OTA type
     *                 0 升级固件
     *                 1.升级默认动态表盘
     *                 2.升级小字库 3.升级大字库 4.升级UI图片资源 5.同时升级2，3，4
     *                 6.推送运动模式
     *                 255.放弃当前升级
     *                 10+n 升级动态表盘n
     *                 20+n 升级自定义表盘n
     */
    int OTA_TYPE_FIRMWARE = 0, OTA_TYPE_DEFAULT_DYNAMIC_DIAL = 1, OTA_TYPE_SMALL_FONT = 2, OTA_TYPE_LARGE_FONT =3,

    OTA_TYPE_UI = 4, OTA_TYPE_FONT_AND_UI = 5, OTA_TYPE_SPORT = 6, OTA_TYPE_CANCEL = 255, OTA_TYPE_DYNAMIC_DIAL =10, OTA_TYPE_CUSTOMIZE_DIAL = 20;
```
### Six, FAQ
   Common problems in the smart wearable App industry:

   1. Bluetooth connection stability and automatic reconnection cannot be solved.

     Fission is based on the secondary development of the RxAndroidBle bluetooth library. In the bluetooth development, the device can not be found, can not be connected, 133 can not be connected and other abnormal scenarios have been optimized for the system. Millions of users use it stably, and the connection complaint rate can be compared with peers in the industry.

   2. Incoming call reminders and App message reminders that the watch cannot receive the problem.

     The system adaptation scheme and exception handling scheme are very mature. (adapted to Android12)

   3. Authority guidance and keep-alive mechanism

     The SDK is not included for the time being. If the customer needs it, the SDK can be customized separately. Generally, it is implemented by App developers themselves.

   4. App power consumption optimization

     The SDK reconnection mechanism supports automatic switching between high-frequency and low-frequency reconnection, effectively reducing connection power consumption. Ble search supports continuous and non-continuous scanning, which is suitable for various scenarios and effectively reduces the power consumption caused by Bluetooth.
