# Fission-SDK-Android集成文档
### 一、产品介绍
* 该文档为指导Android开发人员在Android 5.0及以上系统中集成Fission-SDK-Android，主要为一些关键的使用示例，更详细API，请参考JavaDoc文档。
* Fission-SDK-Android是深圳市裂变智能有限公司提供客户开发者适配公司智能手表的基础依赖库。它主要包含了蓝牙扫描、蓝牙连接、蓝牙通讯、健康数据同步、运动数据同步、来电提醒、消息推送、音乐控制等等各种功能的支持。
* 深圳市裂变智能有限公司以下简称裂变，Fission-SDK-Android目前仅支持裂变生产的REALTEK平台智能手表。
* 本SDK为裂变Android工程师负责开发和维护。联系邮箱:wanglei.hunan@gmail.com，联系电话：15202171127。

### 二、快速入门
#### 1，导入SDK
将**fissionsdk-release-vx.x.x.aar**、**fissionsdk_v2-release-vx.x.x.aar**、**rtk-bbpro-core-x.x.x.jar**、**rtk-core-x.x.x.jar**、**rtk-dfu-x.x.x.jar** 导入工程，一般复制到libs目录下，然后在module中的build.gradle中如下设置：
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
    implementation(name: 'fissionsdk-release', ext: 'aar')
    implementation(name: 'fissionsdk_v2-release', ext: 'aar')
    api rootProject.ext.dependencies["rx-java2"]
    api rootProject.ext.dependencies["rx-android2"]
}
```
#### 2，权限设置
短信、通话、位置信息权限属于隐私权限，如果App需要在google play上架， 注意在google申请权限权限，不然无法通过审核。

AndroidManifest中静态权限注册：
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
代码中动态注册权限示例：（更多动态权限申请示例请参考demo）
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
#### 3，初始化
你需要在Application中进行SDK初始化操作，示例：
```
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FissionSdkBleManage.getInstance().initFissionSdk(this);
    }
}
```
#### 4，蓝牙扫描、连接
你可以使用自己的蓝牙扫描代码，也可以使用SDK中的扫描。SDK的扫描处理过各种系统API版本兼容和坑，建议使用SDK的扫描功能。示例代码如下:
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
蓝牙连接默认会启动自动重连机制，示例代码如下：
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
完成以上的步骤快速接入就完成了，可以连接智能手表调试对应的各种功能。蓝牙扫描的配置参数BleScanConfig、蓝牙连接的配置参数BleComConfig的详细说明请参考后文和JavaDoc文档。

### 三、SDK说明
#### 1，包内容说明
SDK目前包含了两个版本，Fission基础版本和Fission_V2版本。为了上下兼容，使用V2版本时必须同时依赖基础版本的aar文件。基础版本蓝牙底层搜索、连接和通讯都是原始的版本，未经过封装和优化。扫描、连接、通讯的稳定性V2版本做了全面的优化。通讯协议使用基础版本兼容。此文档为Fission_V2版本的使用说明文档。

#### 2，架构介绍
SDK使用了RxAndroidBle开源框架二次开发，底层优化蓝牙连接稳定性，支持自动回连机制，支持防止锁屏系统休眠，支持插队功能（同步数据时，来电提醒不会延时）。

#### 3，支持的开发环境
支持Android5.0及以上系统，已适配Android 12。 建议使用Android Studio开发。

#### 4，接入和更新指引
初次接入和版本更新裂变工程师会负责指导。

#### 5，问题定位流程
开发期间可以打开日志，上架时关闭即可。遇到疑似SDK bug，请联系裂变工程师并提交日志。裂变会及时给出反馈。
```
 FissionSdkBleManage.getInstance().setDebug(true);
 ```

### 四、SDK功能模块说明
#### 1，蓝牙扫描
App需要手动搜索设备时，可以调用这个功能模块。
```
public class BleScanConfig {

    /**
     * 是否持续扫描
     */
    private boolean isContinuousScan = false;

    /**
     *  每次扫描时长
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
* BleScanConfig可以用来配置扫描参数。
* 在前台扫描，需要显示扫描结果时可以设置持续扫描模式。在后台扫描时，可以设置非持续扫描模式，并设置扫描时长。
* 根据Android系统限制，30秒内扫描次数不得超过5次，否则系统将不再返回扫描结果。每次扫描时长不要低于6s。
* 未设置BleScanConfig配置时，SDK将使用默认配置。
* 扫描失败出现异常时，根据异常错误码和错误信息在常量类中去比对确认原因。
* 常见问题：未正确授权、扫描太频繁， 后台扫描未正确配置等导致扫描无返回结果。

#### 2，蓝牙连接
App需要连接蓝牙手表时需要使用这个功能模块。
```
public class BleComConfig implements Serializable {

    /**
     *  默认MTU为最小值23
     */
    private int mtu = 247;

    /**
     *  指令超时时间
     */
    private int timeout = 6000;

    /**
     *  是否需要绑定设备（绑定密钥AT指令）
     */
    private boolean isBind = false;

    /**
     *  配置大数据指令是否需要开启高速模式
     */
    private HashMap<String, Boolean> cmdHighModeMap;

    /**
     *  配置指令优先级
     */
    private HashMap<String, Integer> cmdPriorityMap;

    /**
     *  配置是否需要等待指令结果
     */
    private HashMap<String, Boolean> cmdNeedTimeoutMap;

    /**
     *  绑定秘钥
     */
    private String bindKeys = "";

    /**
     *  是否启用SDK自动回连机制
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
     * BLE 连接状态变化
     * @param newState
     */
    void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState);

    void onConnectionFailure(Throwable throwable);

    void onBinding();

    void onBindSucceeded(String address, String name);

    void onBindFailed(int code);
}

```

* BleComConfig可以用来配置蓝牙连接和通讯的参数。
* mtu配置会通过和手表的通讯协议去协商蓝牙所支持的最大值，可以不用修改默认值。 指令超时最低不能小于3s, 建议使用默认值。是否绑定需要App上层根据连接的手表型号去配置。 绑定秘钥的生成方式请参考demo代码。大数据高速配置，指令优先级配置， 是否需要等待指令响应结果这三个配置项建议直接使用默认值就好。如果需要设置，请联系裂变工程师配合指导使用。
* 默认启用自动回连机制。App上层也可以设计自己的重连机制，配置关闭SDK重连。
* 常见问题：连接兼容性问题， 部分机型连接不上， 断连后自动回连失败，经常断连等。
* 定位问题需要设置sdk debug。 然后提供日志给裂变工程师分析。
* 使用 **connectBleDevice** 方法去连接/绑定手表，使用 **disconnectBleDevice** 去断开连接/解除绑定。解除绑定时App需要自己去清理bindkey, sdk不负责保存和清理。
* **onConnectionStateChange** 用来监听蓝牙底层连接状态， **onConnectionFailure** 蓝牙连接失败时会回调这个方法。 **onBinding** 蓝牙连接成功之后会回调这个方法， 如果是需要绑定的设备，就是代表设备正在绑定， 如果不需要绑定，则是在初始化蓝牙通讯服务。 **onBindSucceeded** 绑定成功/连接成功后会回调这个方法。**onBindFailed** 绑定失败/连接失败会回调这个方法。
* **注意：绑定失败之后注意根据错误码进行处理，当错误码等于FissionConstant.BIND_FAIL_KEY_ERROR时，需要清理缓存秘钥，重新绑定。**

#### 3，手表配置
App在和手表连接成功之后，需要先交换双方配置项。主要包含以下配置项：
1. 获取固件信息， 连接成功之后，优先获取手表固件信息。部分配置项可能和固件版本，型号有关。
```    
   /**
     *  获取固件信息
     */
    public void getHardwareInfo(HardWareInfo hardWareInfo){

    }

    FissionSdkBleManage.getInstance().getHardwareInfo();
```
FissionSdkBleManage.getInstance().getHardwareInfo() 这是发送获取固件信息的指令方法。getHardwareInfo(HardWareInfo hardWareInfo)这是设备执行指令成功后，返回数据给App的回调方法。具体使用方式请参考demo。HardWareInfo包含硬件版本，固件版本，协议版本，适配号，蓝牙地址，蓝牙名称等等信息，主要用于功能模块适配和以后固件升级时的版本判断。
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

2. 设置时间、时区
```
   /**
    *  设置时区
    */
    public void setTimezone(String timezone){

    }

  /**
    *  设置时间
    */  
    public void setTimes(String times){

    }

    FissionSdkBleManage.getInstance().setTimezone(Integer.parseInt(content));
    FissionSdkBleManage.getInstance().setTimes();
```
请根据固件信息来判断设置时区和时间。（部分手表可能需要设置0时区，注意冬夏令时和半时区，15分,45分时区国家和城市的适配）

3. 设置语言
```    
    /**
     *  设置语言
     */
    public void setLanguage(int language){}

    FissionSdkBleManage.getInstance().setLanguage(value);

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
```

4. 设置个人信息
```
/**
  * 设置用户个人信息
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
用户数据的准确度都和用户设置的个人信息相关，绑定成功之后需要设置个人信息。
5. 设置单位
```   
     /**
       *  设置单位
       */
      public void setUnit(int unit){

      }
      // value==1?"公制":"英制"
      FissionSdkBleManage.getInstance().setUnit(value);
```

6. 启动关闭监测数据流
```
/**
 * 启动关闭监测数据流
 */
public void setDataStream(String time){}
// 0关闭， 建议1000
FissionSdkBleManage.getInstance().setDataStream(Integer.parseInt(content));
```
开启数据流检测之后可以接收到手表实时产生的步数，心率，血压，血氧等数据。设置数值越大，刷新间隔时间越长。数值越小，带来的功耗越大。

7. 心率等级判定参数（根据产品设计处理，以手表数据为准是优先get从设备拿数据之后同步到App。 以App数据为准是优先set从App把数据同步到手表。）
```
    /**
     * 心率等级判定参数
     */
    public void getHrRateLevelPara(HrRateLevel hrRateLevel){}
    FissionSdkBleManage.getInstance().getHrRateLevelPara();

    /**
     *  设置心率等级判定参数
     */
    public void setHrlevAlgoPara(){}
    FissionSdkBleManage.getInstance().setHrlevAlgoPara(hrRateLevel);
```
HrRateLevel心率等级判定参数如下：
```
private int overMaxHr;//超过这个百分比,认定为maxHr
private int moderate;//超过这个百分比,认定为适度的Hr
private int vigorous;//超过这个百分比,认为是活跃的hr
private int maxHr;//超过这个百分比，认定为maxHr
private int highestHr;//最高心率值
private int hrTimeLimit;//只有心率值在这个时间宽度都在某个级别以上，才确定新等级
```

8. 久坐判定参数（根据产品设计处理，以手表数据为准是优先get从设备拿数据之后同步到App。 以App数据为准是优先set从App把数据同步到手表。）
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
SedentaryBean久坐判定参数如下：
```   
    private boolean enable;//是否开启久坐
    private int startTime;//起始时间,一天的绝对分钟，最大值 24*60 = 1440 分钟.最小值0
    private int endTime;//同起始时间一致
    private int durTime;//久坐持续时间检测,在这个时间内步数不达标，进行久坐提醒
    private int targetStep;//久坐目标步数,在持续时间内低于这个值则进行久坐提醒
```

9. 喝水提醒参数（根据产品设计处理，以手表数据为准是优先get从设备拿数据之后同步到App。 以App数据为准是优先set从App把数据同步到手表。）
```
 /**
   * 喝水提醒参数
   */
  public void getDrinkWaterPara(DkWaterRemind dkWaterRemind){}
  FissionSdkBleManage.getInstance().getDrinkWaterPara();

  /**
  *  设置喝水提醒参数
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
DkWaterRemind喝水提醒参数如下:
```    
    private int startTime;// 提醒起始时间,当天的绝对分钟，默认起始时间为08:00
    private int endTime;// 提醒结束时间,当天的绝对分钟，默认起始时间为18:00
    private int remindWeek;//提醒周期,如果为0只提醒一次
    private boolean enable;//提醒开关
```

10. 勿扰参数（根据产品设计处理，以手表数据为准是优先get从设备拿数据之后同步到App。 以App数据为准是优先set从App把数据同步到手表。）
```
    /**
     * 勿扰参数
     */
    public void getDndPara(DndRemind dndRemind){}
    FissionSdkBleManage.getInstance().getDndPara();

    /**
      *  设置勿扰参数
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
DndRemind勿扰参数如下：
```   
    private int startTime;// 提醒起始时间,当天的绝对分钟，默认起始时间为08:00
    private int endTime;// 提醒结束时间,当天的绝对分钟，默认起始时间为18:00
    private boolean enable;//提醒开关
```

11. 心率检测参数（根据产品设计处理，以手表数据为准是优先get从设备拿数据之后同步到App。 以App数据为准是优先set从App把数据同步到手表。）
```
    /**
     * 心率检测时间段参数
     */
    public void getHrDetectPara(HrDetectPara hrDetectPara){}
    FissionSdkBleManage.getInstance().getHrDetectPara();

    /**
     *  设置心率检测时间段参数
     */
    public void setHrDetectPara(){}
    HrDetectPara hrDetectPara = new HrDetectPara();
    hrDetectPara.setOpen(switchOpen.isChecked());
    hrDetectPara.setStartTime(Integer.parseInt(startTime));
    hrDetectPara.setEndTime(Integer.parseInt(endTime));
    hrDetectPara.setWeek(Integer.parseInt(weekTime));
    FissionSdkBleManage.getInstance().setHrDetectPara(hrDetectPara);
```
HrDetectPara心率检测时间段参数如下：
```    
    private int startTime;  // 提醒起始时间，当天的绝对分钟
    private int endTime;    // 提醒结束时间
    private int week;       // 提醒周期，如果为 0 只提醒一次
    private boolean open;   // 提醒开关：0：关闭。1：打开。
```

12. 抬腕亮屏参数（根据产品设计处理，以手表数据为准是优先get从设备拿数据之后同步到App。 以App数据为准是优先set从App把数据同步到手表。）
```
    /**
     * 抬腕亮屏时间段参数
     */
    public void getLiftWristPara(LiftWristPara liftWristPara){}

    /**
     *  获取亮屏时长
     */
    public void getScreenKeep(String result){}
    FissionSdkBleManage.getInstance().getScreenKeep();
    FissionSdkBleManage.getInstance().getLiftWristPara();

    /**
     *  设置抬腕亮屏时间段参数
     */
    public void setLiftWristPara(){}
    LiftWristPara liftWristPara = new LiftWristPara();
    liftWristPara.setStartTime(Integer.parseInt(startTime));
    liftWristPara.setEnable(switchOpen.isChecked());
    liftWristPara.setEndTime(Integer.parseInt(endTime));
    FissionSdkBleManage.getInstance().setLiftWristPara(liftWristPara);
    /**
     *  设置亮屏时长
     */
    public void setScreenKeep(){}
    FissionSdkBleManage.getInstance().setScreenKeep(5);
```
亮屏时长数值越大，功耗越高。
LiftWristPara抬腕亮屏时间段参数如下：
```  
    private int startTime; //抬腕亮屏开始时间
    private int endTime; //抬腕亮屏结束时间
    private boolean enable; //抬腕亮屏功能开关
```
#### 4，手表主动发起功能
手表上面有些功能会主动发送消息给App, App需要添加SDK的回调来监听处理。
* 查找手机
```   
    /**
     *  查找手机
     */
    public void findPhone(){}
```
* 放弃查找手机
```   
    /**
     *  取消查找手机
     */
    public void cancelFindPhone(){}
```
* 拍照控制
```
    /**
     * 手表控制拍照
     */
    public void onTakePhotoCallback(){}
```
* 手表接听/挂断电话(0：拒接； 1：接听； 2：静音)
```   
    /**
     * 手环拒接电话/接听电话
     */
    public void onTakePhoneCallback(int callStatus){}
```
* 打开GPS定位
```    
    /**
     *  打开GPS
     */
    public void gpsSuccess(boolean open){}
```
* 音乐控制
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
MusicConfig音乐控制信息如下:
``` public final static int MUSIC_STOP = 0;
    public final static int MUSIC_PAUSE = 1;
    public final static int MUSIC_PLAYING = 2;
    public final static int MUSIC_LAST = 3;
    public final static int MUSIC_NEXT = 4;
    public final static int MUSIC_BUFFER = 5;
    public final static int MUSIC_EXIT = 6;

    /**
     *  音量控制
     */
    public final static int OPERATION_TYPE_VOLUME = 0;
    /**
     *  播放状态控制
     */
    public final static int OPERATION_TYPE_CONTROL = 1;
    /**
     *  进度控制
     */
    public final static int OPERATION_TYPE_PLAYBACK_PROGRESS = 2;

    /**
     *  当前音量
     */
    private int currentVolume =0;

    /**
     *  最大音量
     */
    private int maxVolume =0;

    /**
     *  音乐名称
     */
    private String name;

    /**
     *  音乐当前进度
     */
    private int progress = 0;

    /**
     *  音乐时长
     */
    private int duration = 0;

    /**
     *  当前音乐播放状态
     */
    private int state =0;

    /**
     *  音乐操作类型（音量控制 / 播放状态控制 / 进度控制）
     */
    private int operationType = 0;
```
* 功能开关状态同步
```   
    /**
     *  功能性开关
     */
    public void fssSuccess(FssStatus fssStatus){}
```
FssStatus功能开关状态信息如下：
```    
    /**
     * 功能类型
     */
    private int fssType;

    /**
     * 功能状态
     */
    private int fssStatus;

    PS: fssType ==  1， 体征数据采集总开关状态
                    2， 振动开关状态
                    3， 勿扰开关状态
                    4， 闹钟1 的使能状态
                    5， 闹钟2 的使能状态
                    6， 闹钟3 的使能状态
                    7， 闹钟4 的使能状态
                    8， 闹钟5 的使能状态
                    9， 低压提醒功能开关状态
                    10， 日目标提醒检测总开关状态
                    11， 周目标提醒检测总开关状态
                    12， 自我鼓励目标提醒检测总开关
                    状态
                    13， 心率超标提醒开关状态
                    14， 佩戴通知开关状态
                    15， 拍照模式开关状态
                    16， 电池状态（仅设fssStatus=0:放电状态  1：低压状态2：充电状态3：充满状态)
                    17， 进入音乐界面状
                    18， 亮屏时长设置
                    ->app 有效）
                    19， 抬腕开关状态同
                    20， 电池当前百分比
                    有效）
                    21， 喝水提醒开关
                    22， 久坐提醒开关
                    23，  OTA 百分比 （fssStatus=百分比）
                    24， 静音开关同步
                    25，  OTA 界面状态
      fssStatus: 0， 关闭；1，打开。
```

#### 5，App主动发起功能
App主动发起的简单指令功能。功能执行成功时会通过回调的形式通知App, 需要添加回调监听。
* 查找设备
```
  /**
   *  查找设备
   */
  public void findDevice(){}
  FissionSdkBleManage.getInstance().findDevice();
```
* 重启设备
```
    /**
     *  重启设备
     */
    public void rebootDevice(){}
    FissionSdkBleManage.getInstance().rebootDevice();
```
* 恢复出厂设置
```
  /**
   *  恢复出厂设置
   */
  public void resetDevice(){}
  FissionSdkBleManage.getInstance().resetDevice();
```
* 软关机
```
  /**
   *  软关机
   */
  public void shutdown(){}
  FissionSdkBleManage.getInstance().shutdown();
```
* 获取电量
```
  /**
   *  获取电量状态
   */
  public void getDeviceBattery(DeviceBattery deviceBattery){}
  FissionSdkBleManage.getInstance().getDeviceBattery();
```
* 启用提示(目前仅支持心率过高提示， value是最大心率值)
```
  /**
   *  设置运动心率过高提示
   */
  public void setHeartRateHighTips(String result){}
  FissionSdkBleManage.getInstance().setHeartRateHighTips(1,value);
```
* 打开/关闭 拍照模式(true/false)
```
  /**
   *  设置拍照模式开关
   */
  public void setSwitchPhotoMode(boolean enable){}
  FissionSdkBleManage.getInstance().setSwitchPhotoMode(true);
```
* 震动提醒开关(true/false)
```  
    /**
     *  设置震动提醒开关
     */
    public void setSwitchVibration(boolean enable){}
    FissionSdkBleManage.getInstance().setSwitchVibration(true);
```
* 安全确认
```  
    /**
     *  安全确认
     */
    public void safetyConfirmation(String result){}
    FissionSdkBleManage.getInstance().safetyConfirmation(content);
```
* 自检模式开关(true/false)
```    
    /**
     *  设置自检模式
     */
    public void setSwitchSelfInspectionMode(boolean enable){}
    FissionSdkBleManage.getInstance().setSwitchSelfInspectionMode(true);
```
* 清除用户信息
```
    /**
     *  清除用户信息
     */
    public void clearUserInfo(){}
    FissionSdkBleManage.getInstance().clearUserInfo()
```
* 清除运动数据
```
    /**
     *  清除运动数据
     */
    public void clearSportData(){}
    FissionSdkBleManage.getInstance().clearSportData()
```
* 页面跳转
```
    /**
     *  页面跳转
     */
    public void setPageSkip(String result){}
    FissionSdkBleManage.getInstance().setPageSkip(content);
```
* 音乐控制
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

#### 6，闹钟设置
手表只支持5个闹钟，每一个闹钟 **FissionAlarm** 中的 **index** 作为唯一标示序号。 闹钟设置功能需要根据手表的型号来判断是手表设置的闹钟为主，还是App设置的闹钟为主。详细的使用方式请参考demo代码。使用示例如下：
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
FissionAlarm闹钟信息：
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
未使用的闹钟id, 从App上添加闹钟时，需要先获取未使用的闹钟id:
```
    /**
     *  未使用闹铃id
     */
    public void getNotUsingAlarmId(String result){}
    FissionSdkBleManage.getInstance().getNotUsingAlarmId();
```
#### 7，App消息通知
消息通知功能需要App引导用户授权，使用**NotificationListenerService**来完成。App需要过滤应用包名来判断是否需要推送给手表设备，推送消息的使用示例如下：
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
注意事项：如果手机上面存在多个App使用通知监听权限，有概率导致你的App会监听失败导致消息无法正常推送到手表。这个时候可以通过重新开关权限的方式解决。

#### 8，来电提醒、短信提醒
来电提醒、短信提醒需要动态注册通话权限和短信权限。 如果App需要在google play上架，需要申请隐私权限，申请权限时需要拍摄演示的视频，严格按照google要求，提供app商品详情信息描述和隐私协议。使用示例如下：
```
    /**
     *  来电提醒
     */
    public void incomingCall(){}
    FissionSdkBleManage.getInstance().incomingCall((System.currentTimeMillis() / 1000 ),name,number);

```
短信提醒同App消息提醒的发送代码，推送类型设置短信即可。

#### 9，设置天气
设置天气功能需要先从气象局或者雅虎等等其他外部API拿到一周天气预报和当天天气详情数据。后台服务器可以通过缓存的方式来降低并发请求数，减少开支。天气获取成功之后，同步给手表的示例代码如下：
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
#### 10，设置女性健康数据
女性健康功能需要手表版本支持才可以设置，而且需要将用户信息设置为女性才能正常显示。示例代码如下：
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
FemalePhysiology参数说明如下：
```
    private int psyMode;//生理周期模式 0关闭 1经期模式 2备孕模式 3怀孕模式
    private int periodAdvanceDay;//经期开始提醒前天数
    private int durationDay;//经期持续时间
    private int periodWeek;//间隔周期
    /**
     * @param lastPeriodTime 说明 最近一次月经
     *  这个字段占位3个字节 分别是年月日
     *  sdk会自动做计算，所以这里传Calender
     */
    Calendar lastPeriodTime;
    int pregnancyReminderMode;//孕期提示方式 0 提示已怀孕天数 1 提示距离预产期天数
    long hourOfTime;//提醒时间(小时/分钟 暂时只支持整点 如 12:00)
    boolean deviceRemind;//设备提醒开关
```

#### 11，数据同步
将手表上保存的健康数据和运动数据同步到App上展示。SDK支持同步的数据模块如下：
  > 实时测量数据\
   每日活动统计数据\
   整点活动统计数据\
   睡眠统计报告\
   睡眠统计记录\
   睡眠实时记录\
   运动统计报告\
   运动记录列表\
   运动详情记录\
   心率记录\
   计步记录\
   血氧记录\
   血压记录

SDK需要添加回调监听，然后调用同步数据的方法。详情请参考demo，示例代码如下：
* 获取实时测量数据
```
  /**
   *  获取实时测量数据
   */
  public void getMeasureInfo(MeasureInfo measureInfo){}
  FissionSdkBleManage.getInstance().getMeasureInfo();
```
MeasureInfo实时测量数据包含以下内容：
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
* 获取每日活动统计数据(startTime,endTime 秒时间戳)
```
    /**
     *  获取每日活动统计
     */
    public void getDaysReport(List<DaysReport> daysReports){}
    FissionSdkBleManage.getInstance().getDaysReport(startTime,endTime);
```
DaysReport每日活动统计数据包含以下内容：
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
* 获取整点活动统计数据(startTime,endTime 秒时间戳)
```
    /**
     *  获取整点活动统计
     */
    public void getHoursReport(List<HoursReport> hoursReports){}
    FissionSdkBleManage.getInstance().getHoursReport(startTime, endTime);
```
HoursReport整点活动统计数据包含以下内容：
```
    private int time;           // 时间戳GMT秒
    private int bodyVersion;    // 结构体版本
    private int step;           // 当天累计计步数
    private int calorie;        // 当天累计消耗卡洛里
    private int distance;       // 当天累计行程
```
* 获取睡眠统计报告
```
    /**
     * 获取睡眠统计报告
     */
    public void getSleepReport(List<SleepReport> sleepReports){}
    FissionSdkBleManage.getInstance().getSleepReport(startTime, endTime);
```
SleepReport睡眠统计报告数据包含以下内容：
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
* 获取睡眠统计记录
```
    /**
     *获取睡眠状态记录
     */
    public void getSleepRecord(List<SleepRecord> sleepRecords){}
    FissionSdkBleManage.getInstance().getSleepRecord(startTime, endTime);
```
SleepRecord睡眠统计记录数据包含以下内容：
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
         * 0：清醒状态。
         * 1：浅层睡眠
         * 2：深层睡眠
         * 3：眼动
         * 4：小睡
         * 5：工作中
         */
        public int status;
        public int time; // 持续时间
```
* 获取睡眠实时记录
```
    /**
     * 获取当前睡眠实时状态记录
     */
    public void getCurSleepRecord(SleepRecord sleepRecord){}
    FissionSdkBleManage.getInstance().getCurSleepRecord();
```
* 获取运动统计报告
```
    /**
      * 获取运动统计报告
      */
     public void getExerciseReport(List<ExerciseReport> exerciseReports){}
     FissionSdkBleManage.getInstance().getExerciseReport(startTime, endTime);
```
ExerciseReport运动统计报告包含以下内容：
```
     private int                        utcTime;// 时间戳UTC秒
     private int                        bodyVersion;// 结构体版本
     private int                        beginTime;// 运动开始时间戳，作为每笔运动的唯一识别id，单位秒
     private int                        endTime;// 运动结束时间戳，单位秒
     private int                        totalTime;// 运动总时间，单位秒
     private int                        totalStep;// 运动总步数
     private int                        totalCalorie;// 运动总卡路里
     private int                        totalDistance;// 运动总距离（单位米，通过计步估算）
     private int                        totalTrackDistance;// 本次运动轨迹运动距离（单位米，通过gps定位计算）
     private int                        model; // 运动模式:
     private int                        highHR;// 本次运动最大心率（单位：次/分钟）
     private int                        lowHR;// 本次运动最小心率（单位：次/分钟）
     private int                        avgHR;// 本次运动平均心率，运动结束时计算，心率和/记录次数（单位：次/分钟）
     private int                        maxStride; // 本次运动最大步频（单位：步/分钟）
     private int                        avgStride; // 本次运动平均步频 = 步频和/记录次数（单位：步/分钟）
     private int                        sportCount;// 运动次数，中途休息次数
     private List<ExerciseReportDetail> details; //中断UTC记录，同时用于统计运动总时间，暂停运动时间，单位秒，连续20组
     private float                      maxSpeed;// 本次运动最大速度（单位：米/秒）
     private float                      avgSpeed;// 本次运动平均速度 = 距离/用时（单位：米/秒）
     private int                        notTrackAvgSpeed;// 本次无轨迹运动平均配速（单位：小时/公里）
     private int                        hasTrackAvgSpeed;// 本次有轨迹运动配速（单位：小时/公里）
     private int                        repeatSportWeek;// 重复运动的周期数（来回次数，圈数）（单位：圈）
     private int                        swingNumber;// 摆臂次数，划水次数（单位：次）
     private int warmUpEsTime;//热身运动时间
     private int fatBurningTime;//燃脂运动时间
     private int aerobicEnduranceTime;//有氧耐力运动时间
     private int highAerobicEnduranceTime;//高强有氧耐力运动时间
     private int anaerobicTime;//无氧运动时间
```
* 获取运动记录列表
```
    /**
     * 获取运动记录列表
     */
    public void getExerciseList(List<ExerciseList> exerciseLists){}
    FissionSdkBleManage.getInstance().getExerciseList(startTime, endTime);
```
ExerciseList运动记录列表包含以下内容：
```
  private int time; // 时间戳GMT秒
    private int bodyVersion; // 结构体版本
    private int beginTime; // 运动开始时间，GMT秒。
    private int endTime; // 运动开始时间，GMT秒。
    /**
     * 运动模式
     * 0:健走
     * 1:跑步
     * 2:登山
     * 3:骑行
     * 4:足球
     * 5:游泳
     * 6:篮球
     * 7:无指定
     * 8:户外跑步
     * 9:室内跑步
     * 10:减脂跑步
     * 11:户外健走
     * 12:室内健走
     * 13:户外骑行
     * 14:室内骑行
     * 15:自由训练
     * 16:健身训练
     * 17:羽毛球
     * 18:排球
     * 19:乒乓球
     * 20:椭圆机
     * 21:划船机
     * 22:其他预留
     */
    private int model;
```

* 获取运动详情记录
```
    /**
     *  运动详情记录
     */
    public void getExerciseDetail(List<ExerciseDetail> exerciseDetails){}
    FissionSdkBleManage.getInstance().getExerciseDetail(exerciseList.getBeginTime(), exerciseList.getEndTime());
```
ExerciseDetail运动详情记录包含以下内容：
```
    private int                        time; // 第一条记录（结构体）的形成时间戳GMT秒
    private int                        bodyVersion;// 结构体版本
    private int                        week; // 记录生成周期，单位秒
    private int                        effectiveNumber; // 此记录块包含有效记录条数
    private int                        recordLength;  // 单条记录长度，即单个记录结构体大小
    private int                        type; // 记录类型：0：心率记录
    private List<ExerciseDetailRecord> exerciseDetailRecords;
    public class ExerciseDetailRecord {
        private int utcTime;//utc
        private int pace;      // 实时配速，单位 s/km
        private int frequency;  // 实时步频
        private int calorie;    // 一分钟内消耗的卡路里值
        private int steps;      // 一分钟内的步数
        private int distance;   // 运动中的实时距离
        private int heartRate;  // 实时心率
        private int stamina;    // 实时体力，0~100
        private int state;      // 状态。0 正常，1 暂停
```
* 获取心率记录
```
    /**
     * 获取心率记录
     */
    public void getHeartRateRecord(List<HeartRateRecord> heartRateRecords){}
    FissionSdkBleManage.getInstance().getHeartRateRecord(startTime,endTime);
```
HeartRateRecord心率记录包含以下内容：
```
    private long          time; // 第一条记录（结构体）的形成时间戳GMT秒
    private int           bodyVersion;// 结构体版本
    private long          week; // 记录生成周期，单位秒
    private int           effectiveNumber; // 此记录块包含有效记录条数
    private int           recordLength;  // 单条记录长度，即单个记录结构体大小
    private int           type; // 记录类型：0：心率记录
    private List<Integer> hrList; //心率值集合
    private List<Long>    hrListTime; //每条心率测试时间的集合
```
* 获取计步记录
```
    /**
     * 获取计步记录
     */
    public void getStepsRecord(List<StepsRecord> stepsRecords){}
    FissionSdkBleManage.getInstance().getStepsRecord(startTime,endTime);
```
StepsRecord计步记录包含以下内容：
```
    private int time;               // 时间戳GMT秒
    private int bodyVersion;        // 结构体版本
    private int week;               // 记录生成周期，单位秒
    private int number;             // 此记录块包含有效记录条数
    private int length;             // 单条记录长度，即单个记录结构体大小
    private int type;               // 记录类型：1：计步明细记录。
    private  List<Long> utcTime;// 每个步数记录的时间

    private List<Integer> steps; //每个时间记录的步数
```
* 获取血氧记录
```
    /**
     *  获取血氧记录
     */
    public void getSpo2Record(List<Spo2Record> spo2Records){}
    FissionSdkBleManage.getInstance().getSpo2Record(startTime,endTime);
```
Spo2Record血氧记录包含以下内容：
```
    private int           time;               // 时间戳GMT秒
    private int           bodyVersion;        // 结构体版本
    private int           week;               // 记录生成周期，单位秒
    private int           number;             // 此记录块包含有效记录条数
    private int           length;             // 单条记录长度，即单个记录结构体大小
    private int           type;               // 记录类型：1：计步明细记录。
    private int           keepData;//保留字节
    private List<Integer> spList;  // 血氧值
    private List<Long>    utc;  // utc时间
```
* 获取血压记录
```
    /**
     * 获取血压记录
     */
    public void getBloodPressureRecord(List<BloodPressureRecord> bloodPressureRecords){}
    FissionSdkBleManage.getInstance().getBloodPressureRecord(startTime,endTime);
```
BloodPressureRecord血氧记录包含以下内容：
```
    private int          time;               // 时间戳GMT秒
    private int          bodyVersion;        // 结构体版本
    private int          week;               // 记录生成周期，单位秒
    private int          number;             // 此记录块包含有效记录条数
    private int          length;             // 单条记录长度，即单个记录结构体大小
    private int          type;               // 记录类型：1：计步明细记录。
    public  List<Detail> details;

    public class Detail {
        public int  pbMax; //最大心率
        public int  pbMin; //最小心率
        public long utc;  // 测试时间
```
