package com.szfission.wear.demo.pay;

import android.content.Context;
import android.text.TextUtils;

import com.blankj.utilcode.util.SPUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.pay.FissionPayCallback;
import com.fission.wear.sdk.v2.pay.FissionPayManage;
import com.fission.wear.sdk.v2.pay.FissionPaySession;
import com.infineon.secora.wallet.domain.wearable.ble.BluetoothStateManager;
import com.infineon.secora.wearable.protocolapi.IHostSharedBleProtocol;
import com.infineon.secora.wearable.protocolapi.ISecoraBleProtocol;

import java.util.Locale;

/**
 * 在 Fission 主 BLE 上建立 SECORA 通道并注册到 pay_wallet {@link BluetoothStateManager}。
 */
public final class PayWalletHostBridge {

    private PayWalletHostBridge() {
    }

    public interface PrepareCallback {
        void onReady(FissionPaySession session);

        void onFailure(String message);
    }

    /**
     * 复用 Fission RxBLE 连接 discover SECORA 特征，并注册为 pay_wallet 的活动协议。
     */
    public static void prepareSharedSecoraChannel(Context context, PrepareCallback callback) {
        if (callback == null) {
            return;
        }
        FissionPayManage.getInstance().connectOverFissionBle(context, new FissionPayCallback<FissionPaySession>() {
            @Override
            public void onSuccess(FissionPaySession session) {
                if (session == null || !(session.getProtocol() instanceof ISecoraBleProtocol)) {
                    callback.onFailure("SECORA 协议实例无效");
                    return;
                }
                ISecoraBleProtocol protocol = (ISecoraBleProtocol) session.getProtocol();
                String mac = resolveMac(protocol);
                if (TextUtils.isEmpty(mac)) {
                    callback.onFailure("未获取到手表 MAC");
                    return;
                }
                BluetoothStateManager.INSTANCE.registerHostProtocol(protocol, mac, null);
                callback.onReady(session);
            }

            @Override
            public void onFailure(int code, String message) {
                callback.onFailure(message != null ? message : ("错误码 " + code));
            }
        });
    }

    /**
     * 释放共享 SECORA 通道（不断开 Fission 主 BLE）。
     */
    public static void releaseSharedSecoraChannel(FissionPaySession session) {
        if (session != null && session.getProtocol() instanceof IHostSharedBleProtocol) {
            ((IHostSharedBleProtocol) session.getProtocol()).releaseSharedChannel();
        }
        BluetoothStateManager.INSTANCE.setActiveProtocol(null);
    }

    private static String resolveMac(ISecoraBleProtocol protocol) {
        String mac = protocol.getBluetoothDevice().getAddress();
        if (!TextUtils.isEmpty(mac)) {
            return mac.trim().toUpperCase(Locale.US);
        }
        return SPUtils.getInstance().getString(SpKey.LAST_MAC);
    }
}
