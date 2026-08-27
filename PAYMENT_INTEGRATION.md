# 支付能力接入说明（设备侧）

> **当前 SDK 版本：** v2.1.46（高于历史交付版 v2.1.45）  
> **版本变更记录：** [SDK_CHANGELOG.md](./SDK_CHANGELOG.md)

本文面向已经在用 `fissionsdk_v2` 的存量客户，说明如何启用手表支付能力、与 Fission 主 BLE 共存，以及升级时需要注意的约束。

---

## 一、兼容性结论（存量客户先看这里）

| 项目 | 结论 |
| --- | --- |
| `minSdk` | **保持 23 不变**，无需修改 |
| 原有 API | **无任何签名变更**，不接支付则代码零改动 |
| 支付可用系统版本 | **Android 13（API 33）及以上**，低版本调用直接回调失败，不会崩溃 |
| aar 体积 | 增加约 200KB（支付 SDK 与 APDU 脚本） |
| 推荐接入方式 | Fission 主 BLE 已连接 → `connectOverFissionBle()`（共享 GATT） |

支付通道用到了 Android 13 才提供的 GATT 接口（`writeCharacteristic(char, value, type)`、`writeDescriptor(desc, value)` 及新的回调重载）。低版本系统上 `FissionPayManage.isSupported()` 返回 `false`，所有操作走失败回调，其余 SDK 功能不受影响。

---

## 二、获取 SDK 产物

```bash
gradlew :bundle_core:assembleRelease
```

| 项目 | 值 |
| --- | --- |
| 产物路径 | `bundle_core/build/outputs/aar/fissionsdk_v2-release-v2.1.46.aar` |
| 构建命令 | `gradlew :bundle_core:assembleRelease` |
| 文件大小 | 约 8.87 MB（2026-08-27 构建） |
| 内嵌模块 | `fissionsdk_v2`（含 `pay` 门面）、`secora_wearable`、原有 `fissionsdk` 等 |
| minSdk | 23 |

宿主 `build.gradle` 引用：

```gradle
repositories {
    flatDir { dirs 'libs' }
}

dependencies {
    implementation(name: 'fissionsdk_v2-release-v2.1.46', ext: 'aar')
    // 见第三节：支付依赖库
}
```

---

## 三、宿主工程需要补的配置

支付链路依赖以下第三方库。**交付 aar 不内嵌它们**，避免与宿主已有版本产生 duplicate class：

```gradle
dependencies {
    // EC 证书解析与 SCP11a/SCP03 安全通道
    implementation 'org.bouncycastle:bcprov-jdk18on:1.83'
    implementation 'org.bouncycastle:bcpkix-jdk18on:1.83'
    // APDU 脚本解析
    implementation 'com.fasterxml.jackson.core:jackson-core:2.20.1'
    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.20'
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.20.1'
    implementation 'commons-codec:commons-codec:1.20.0'
    // 支付密码挑战值的加密存储（完整开卡 demo 需要）
    implementation 'androidx.security:security-crypto:1.0.0'
}

android {
    packagingOptions {
        exclude 'META-INF/versions/9/OSGI-INF/MANIFEST.MF'
    }
}
```

`gradle.properties`（Jetifier 与 BouncyCastle 冲突时）：

```properties
android.jetifier.ignorelist=bcprov-jdk18on,bcpkix-jdk18on
```

**权限：** 支付通道额外需要 `BLUETOOTH_CONNECT`（aar 清单已声明），连接前请确保运行时授权。

**混淆：** 规则已随 aar 的 `consumer-rules.pro` 自动合并，无需手工添加。

---

## 四、接入方式概览

SDK 只负责**设备侧**：APDU 通道、脚本执行、CDCVM（设备侧持卡人验证）。

以下由宿主 App 自行对接：

- 发卡/开卡后端（下发 APDU 脚本、卡片生命周期）
- 账号体系与登录鉴权
- 推送（远程删卡等）
- 卡片管理 UI（可选引用 demo 模块 `:pay_wallet`，不随 aar 交付）

### 4.1 两种连接模式

| 模式 | 方法 | 适用场景 |
| --- | --- | --- |
| **共享 GATT（推荐）** | `connectOverFissionBle()` | 宿主已用 Fission SDK 连接手表，支付与运动/健康共用一条 BLE |
| **独立 GATT** | `connect()` | 非 Fission 宿主，或调试对比 |

共享模式下 `disconnect()` **仅释放 SECORA 监听**，不会断开 Fission 主链路。

### 4.2 最小接入示例（仅设备侧）

入口：`com.fission.wear.sdk.v2.pay.FissionPayManage`，回调均在主线程。

```java
if (!FissionPayManage.isSupported()) {
    // Android 13 以下：隐藏支付入口
    return;
}

FissionPayManage pay = FissionPayManage.getInstance();

// 前提：FissionSdkBleManage 已连接手表
pay.connectOverFissionBle(context, new FissionPayCallback<FissionPaySession>() {
    @Override
    public void onSuccess(FissionPaySession session) {
        // 读 CASD 证书，提交给发卡后端
        pay.fetchCasdCertificates(context, session, new FissionPayCallback<CasdCertificates>() {
            @Override
            public void onSuccess(CasdCertificates certs) { /* 开卡入参 */ }

            @Override
            public void onFailure(int code, String message) { }
        });
    }

    @Override
    public void onFailure(int code, String message) { }
});
```

### 4.3 推荐接入流程（Fission 宿主）

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Application 初始化 FissionSdkBleManage（原有流程）        │
│ 2. 扫描 → 连接手表（DeviceScanActivity / 自有连接页）         │
│ 3. FissionPayManage.isSupported() == true 时展示支付入口      │
│ 4. connectOverFissionBle() 建立 SECORA 通道                  │
│ 5. 按业务调用下方 API（CPLC/CASD/脚本/PPSE/CDCVM）           │
│ 6. 退出支付页：disconnect(session)，主 BLE 保持连接           │
└─────────────────────────────────────────────────────────────┘
```

**与英飞凌钱包 UI 共存（demo 做法）：**

1. 主页连接成功后，调用 `PayWalletHostBridge.prepareSharedSecoraChannel()` 注册 Host 协议  
2. `Application.onCreate()` 中调用 `PayHostFssBridge.register()`，将 `:pay_wallet` 添卡流程与 Fission 主 BLE 的 `AT+FSS` 同步对接  
3. 进入 `:pay_wallet` 的 `SplashActivity` → `MainActivity`（跳过 BLE 扫描与二次 GATT）  
4. 退出钱包时 Host `MainActivity.onDestroy` **不要** `disconnectBle()`（v2.1.46 已修复）

**英飞凌添卡 FSS 时序（手表协议要求，demo 已内置）：**

| 时机 | AT 指令 | 说明 |
| --- | --- | --- |
| 用户点击「+」添卡 | `AT+FSS:62,1` | 准备添加英飞凌支付卡片；发送后延时 50ms 再进入原添卡流程 |
| notify-provision 成功 | `AT+FSS:63,1` | 卡片添加成功 |
| 添卡流程失败 | `AT+FSS:64,1` | 卡片添加失败 |

宿主若自行实现开卡 UI（不引用 `:pay_wallet`），需在对应节点调用 `FissionSdkBleManage.getInstance().notifyInfineonPayAddCardStarting/Success/Failed()`，或通用方法 `syncFeatureSwitchState(f, s)`。

---

## 五、API 参考

### 5.1 连接与配对

| 方法 | 说明 |
| --- | --- |
| `isSupported()` | API 33+ 且 aar 含支付模块 |
| `getPaySdkVersion()` | 内嵌 Secora SDK 版本号 |
| `setConfig(FissionPayConfig)` | 覆盖 GATT UUID（固件改过 UUID 时） |
| `connect(context, device, callback)` | 独立 GATT 连接 |
| `connectOverFissionBle(context, callback)` | **推荐** 复用 Fission RxBLE 连接 |
| `disconnect(session)` | 释放支付通道 |
| `waitForBond` / `removeBond` | 配对等待 / 解绑 |

默认 GATT UUID（`FissionPayConfig.getDefault()`）：

| 参数 | UUID |
| --- | --- |
| Service | `78133E8A-4157-4929-9F7E-2E7C7C0A8F2E` |
| Request（写） | `78133E8A-4157-4929-9F7E-2E7C7C0A8F2F` |
| Response（Indication） | `78133E8A-4157-4929-9F7E-2E7C7C0A8F30` |
| CCCD | `00002902-0000-1000-8000-00805f9b34fb` |

### 5.2 安全单元与脚本

| 方法 | 说明 |
| --- | --- |
| `fetchCplcData` | 读 CPLC（芯片出厂信息 / SEID） |
| `fetchCasdCertificates` | 读 CASD 叶证书（MDES/VTS），开卡入参 |
| `executeScript` | 执行后端下发的 APDU JSON 脚本（开卡/删卡） |
| `setDefaultCard` | 设置默认支付卡（PPSE 两阶段：CRS GET STATUS → Activate → Put Template） |
| `transceive` | 单条 APDU 透传（高级用法） |

**PPSE 说明（v2.1.46）：** Titan/Fission 部分 SE 对 CRS GET STATUS 返回 `6700`。SDK 会自动合成 BF0C 模板并继续第二阶段，无需宿主额外处理。

### 5.3 CDCVM（设备支付密码）

| 方法 | 说明 |
| --- | --- |
| `readCvmState` | 是否已设密、是否锁定 |
| `onboardDevice` | VerifyDevice / ClaimDevice 纳管 |
| `setupDevicePasscode` | 首次设密 |
| `verifyDevicePasscode` | 校验解锁 |
| `changeDevicePasscode` | 修改密码 |
| `resetDeviceCvm` | 重置 CVM（通常需先删卡） |

### 5.4 功能开关同步（FSS，主 BLE）

通过 Fission 主链路向手表发送 `AT+FSS:f,s`（App → 设备）。英飞凌支付添卡相关编号：

| 方法 | f | s | 时机 |
| --- | --- | --- | --- |
| `notifyInfineonPayAddCardStarting()` | 62 | 1 | 用户点击「+」开始添卡 |
| `notifyInfineonPayAddCardSuccess()` | 63 | 1 | 发卡全流程成功（notify-provision 成功） |
| `notifyInfineonPayAddCardFailed()` | 64 | 1 | 添卡失败 |
| `syncFeatureSwitchState(f, s)` | 任意 | 任意 | 通用 FSS 同步 |

常量见 `FissionConstant.FSS_TYPE_INFINEON_PAY_*` / `FissionEnum.SC_INFINEON_PAY_*`。

**前提：** `FissionSdkBleManage` 主 BLE 已连接；未连接时调用静默忽略。

### 5.5 错误码

| 常量 | 值 | 含义 |
| --- | --- | --- |
| `ERR_UNSUPPORTED` | 4001 | 系统低于 API 33，或未集成支付模块 |
| `ERR_SESSION_INVALID` | 4002 | 通道未建立或主 BLE 未连接 |
| `ERR_EXECUTE_FAILED` | 4003 | 底层执行失败，`message` 为原始异常 |

---

## 六、典型业务串联

### 6.1 开卡（设备侧步骤）

宿主自行对接发卡后端；SDK 负责在手表上执行脚本。

```
fetchCasdCertificates → 提交后端 register/checkEligibility
→ executeScript(后端下发 install 脚本)
→ executeScript(后端下发 prep/digitize 脚本)
→ setDefaultCard(appletAid, cardType, otherAids)   // 可选
```

### 6.2 设默认卡

```java
pay.setDefaultCard(context, session,
    "A000000004101011",           // appletInstanceAid
    "Master Card",                // cardType 标签
    Arrays.asList("A000000003101011"),  // 其他卡 AID，可为 null
    new FissionPayCallback<Boolean>() {
        @Override public void onSuccess(Boolean ok) { }
        @Override public void onFailure(int code, String msg) { }
    });
```

### 6.3 删卡

```java
pay.executeScript(session, deleteScriptJsonBytes,
    true,   // isDeleteScript
    true,   // clearDefaultCard：删卡后清除 PPSE 默认
    callback);
```

---

## 七、工程结构

| 模块 | 是否交付 | 说明 |
| --- | --- | --- |
| `secora_wearable` | ✅ 合入 aar | 安全单元 / APDU / CDCVM 源码 |
| `fissionsdk_v2` → `pay` 包 | ✅ 合入 aar | `FissionPayManage`、`RxBleSecoraProtocol` 门面 |
| `bundle_core` | 打包模块 | `embed` 合并所有子模块 |
| `pay_wallet` | ❌ 仅 demo | 英飞凌钱包完整 UI（登录、开卡、卡片管理） |
| `secora-wallet-sdk*.aar` | ❌ 仅 demo | 发卡后端中间件（二进制） |

---

## 八、Demo 工程入口（参考实现）

| 入口 | 类 / 模块 | 说明 |
| --- | --- | --- |
| 支付功能调试 | `PayTestActivity` | 纯设备侧：`FissionPayManage` 全 API 测试 |
| 支付开卡/发卡 | `:pay_wallet` | 完整英飞凌流程；需 UAT 账号与 OAuth 配置 |
| Host 桥接 | `PayWalletHostBridge` | 注册共享 SECORA 协议 |
| FSS 桥接 | `PayHostFssBridge` | 注册 `PayHostFssSync` → `FissionSdkBleManage` 的 AT+FSS 62/63/64 |
| 启动辅助 | `PayLaunchHelper` | 主页 → 钱包 / 调试页 |

**运行要求：** 完整开卡 demo 需 **API 33+**；先在主页用 Fission SDK **连接手表**，再进入支付模块。

### 外部依赖限制（完整开卡 demo）

| 依赖 | 限制 |
| --- | --- |
| `config.properties` | 默认指向英飞凌 UAT；Titan 项目 OEM：`OEM_ID=3502`，`SE_TYPE_GROUP=DE81-3502` |
| Cognito / MSAL / Google Sign-In | 绑定英飞凌 demo 包名与签名；宿主需单独申请配置 |
| Firebase FCM | 推送删卡通知；需自建 Firebase 项目 |

设备侧 BLE 与 PPSE **不依赖** 上述云端配置；仅用 `FissionPayManage` 即可完成连接、脚本与设默认卡。

---

## 九、从 v2.1.45 升级到 v2.1.46

1. 替换 aar 为 `fissionsdk_v2-release-v2.1.46.aar`（版本号高于 2026-03-07 交付的 v2.1.45）
2. **无需改代码**（相对 v2.1.45，支付 API 为新增/兼容扩展；相对旧版非支付 SDK，原有 API 无破坏性变更）
3. 若使用 Host + 钱包 UI：确认主页 `onDestroy` 不在 Host 拉起钱包时断开 BLE；`Application` 注册 `PayHostFssBridge` 以同步添卡 FSS
4. 若遇 PPSE 失败：升级后 SDK 自动处理 GET STATUS `6700` 回退  

详见 [SDK_CHANGELOG.md](./SDK_CHANGELOG.md)。

---

## 十、移植上游时的两处源码改动

从 Infineon `SecoraWearableSDK` 移植时保留以下改动；合并上游新版本时需同步：

1. **`BleConnectConfig`、`CasdCertificates`**：由 Java `record` 改为普通 final 类（避免 Android 脱糖 `Record` 合成类构建失败）。  
2. **`BlePairingHelper`**：`ContextCompat.registerReceiver` 改为 `context.registerReceiver`（避免抬高 `androidx.core` 最低版本）。

---

## 十一、FAQ

**Q：低版本 Android 会崩溃吗？**  
A：不会。先调 `isSupported()`，为 false 时隐藏入口即可。

**Q：支付会和运动数据 BLE 冲突吗？**  
A：使用 `connectOverFissionBle()` 时共用一条 GATT；SECORA 与主业务通过队列串行收发，勿并发调用 `transceive`。

**Q：必须集成 `:pay_wallet` 吗？**  
A：不必。只需 `FissionPayManage` + 自建后端即可实现开卡；钱包 UI 模块仅供 demo 参考。

**Q：如何确认 aar 已含支付模块？**  
A：解压 aar，检查 `classes.jar` 是否含 `com/fission/wear/sdk/v2/pay/FissionPayManage.class` 与 `com/infineon/secora/`，以及 `assets/PPSE-01.json`。
