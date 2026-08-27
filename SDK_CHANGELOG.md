# Fission SDK 版本变更说明

本文记录 `fissionsdk_v2-release-v*.aar`（由 `bundle_core` 打包）各版本的**新增能力与破坏性变更**。
完整支付接入步骤见 [PAYMENT_INTEGRATION.md](./PAYMENT_INTEGRATION.md)。

---

## v2.1.46（2026-08-27）

> 本版本号 **高于** 此前交付客户的 `v2.1.45`（2026-03-07），可直接作为升级包替换。

### 新增

| 类别 | 说明 |
| --- | --- |
| **支付模块（设备侧）** | 交付 aar 内嵌 `secora_wearable`，对外门面为 `com.fission.wear.sdk.v2.pay.FissionPayManage` |
| **共享 GATT 通道** | `FissionPayManage.connectOverFissionBle()`：在 Fission 主 BLE 已连接时复用同一条 GATT 收发 SECORA APDU，无需第二次 `connectGatt()` |
| **Host 协议注册** | `IHostSharedBleProtocol` / `RxBleSecoraProtocol`：宿主可将已建立的 RxBLE 连接注册给支付栈使用 |
| **PPSE 默认卡** | `FissionPayManage.setDefaultCard()`：设备侧设置默认支付卡（含 CRS Activate + PPSE Put Template 两阶段流程） |
| **CDCVM** | 设备侧支付密码：设置 / 校验 / 修改 / 重置，及 CVM 状态查询 |
| **APDU 脚本** | `executeScript()` 执行发卡后端下发的开卡/删卡 JSON 脚本；内置 `assets/PPSE-*.json`、`CPLC-getdata.json`、`CASD-getdata.json` |
| **Titan OEM 适配** | PPSE 流程对 Titan/Fission SE 增加 GET STATUS 失败回退：当 CRS GET STATUS 返回 `6700` 时，自动合成 BF0C 模板并继续第二阶段 |
| **英飞凌添卡 FSS 同步** | `FissionSdkBleManage.syncFeatureSwitchState(f,s)` 及 `notifyInfineonPayAddCardStarting/Success/Failed()`：通过主 BLE 发送 `AT+FSS:62/63/64,1`，通知手表进入/完成/失败添卡状态（demo 中由 `:pay_wallet` + `PayHostFssBridge` 自动触发） |

### 修复

| 问题 | 说明 |
| --- | --- |
| Host 进入钱包后 BLE 断开 | 宿主 `MainActivity.onDestroy` 在 Host 拉起钱包时不再调用 `disconnectBle()` |
| Card Settings PPSE 无协议 | `ScriptHandler` 支持复用 `BluetoothStateManager.activeProtocol`，Host 模式下禁止二次 GATT |
| GET STATUS 长度 | `SetDefaultCardGenerator.buildGetStatusRequest()` 补全 `Le=00` |
| PPSE 结果误判 | `SecoraSEImpl` 修正 PPSE 异步链始终 `complete(true)` 的问题 |

### 兼容性

| 项目 | 结论 |
| --- | --- |
| `minSdk` | **23**（不变） |
| 原有 Fission SDK API | **无签名变更**；不接支付则代码零改动 |
| 支付能力运行期要求 | **Android 13（API 33）及以上**；低版本 `FissionPayManage.isSupported()` 为 `false` |
| aar 体积 | 较无支付版本增加约 **200KB**（含 `secora_wearable` 与 APDU 脚本） |
| 宿主额外依赖 | BouncyCastle、Jackson、commons-codec、security-crypto（见接入文档） |

### 打包

```bash
gradlew :bundle_core:assembleRelease
```

| 项目 | 值 |
| --- | --- |
| 产物路径 | `bundle_core/build/outputs/aar/fissionsdk_v2-release-v2.1.46.aar` |
| 构建日期 | 2026-08-27 |
| 文件大小 | 约 8.87 MB（9,300,124 字节） |
| versionCode | 151 |

---

## v2.1.45（2026-03-07，历史交付版）

客户已收到的版本，**不含**本次 Titan Host 共享 BLE 与 PPSE GET STATUS 回退修复。请升级至 **v2.1.46**。

---

## v2.1.42 及更早

v2.1.42 为支付模块首次合入交付 aar 的内部版本；更早版本不含 `FissionPayManage` 与 `secora_wearable`。

升级自 v2.1.45 → v2.1.46：**建议升级**（支付 Host/PPSE 稳定性修复 + 支付模块完整合入），API 无破坏性变更。
