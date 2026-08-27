package com.szfission.wear.demo.pay;

import android.content.Context;
import android.text.TextUtils;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.pay.FissionPayManage;
import com.szfission.wear.demo.R;

/**
 * 从 Fission demo 主页进入支付模块前的统一校验与参数传递。
 *
 * <p>蓝牙扫描/配对/连接走主页原有 {@link com.fission.wear.sdk.v2.FissionSdkBleManage} 流程；
 * 支付 SECORA 在共享 GATT 上 discover/read/write（见 {@link PayWalletHostBridge}）。
 */
public final class PayLaunchHelper {

    /** 主页已连接手表的 MAC，传给支付页 */
    public static final String EXTRA_BLE_MAC = "pay_ble_mac";
    /** 主页显示的设备名（可选） */
    public static final String EXTRA_DEVICE_NAME = "pay_device_name";
    /** 进入支付页后是否自动建立支付通道 */
    public static final String EXTRA_AUTO_CONNECT = "pay_auto_connect";

    private PayLaunchHelper() {
    }

    /**
     * 校验主页是否已连接设备，且当前系统支持支付能力。
     *
     * @return true 表示可以进入支付模块
     */
    public static boolean preparePaymentLaunch(Context context, boolean connectSuccessfully) {
        if (!connectSuccessfully) {
            ToastUtils.showShort(context.getString(R.string.pay_connect_first));
            return false;
        }
        if (!FissionSdkBleManage.getInstance().isConnected()) {
            ToastUtils.showShort(context.getString(R.string.pay_ble_not_connected));
            return false;
        }
        String mac = SPUtils.getInstance().getString(SpKey.LAST_MAC);
        if (TextUtils.isEmpty(mac)) {
            ToastUtils.showShort(context.getString(R.string.pay_mac_not_found));
            return false;
        }
        if (!FissionPayManage.isSupported()) {
            ToastUtils.showShort(context.getString(R.string.pay_requires_android_13));
            return false;
        }
        return true;
    }
}
