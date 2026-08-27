package com.szfission.wear.demo.pay;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.infineon.secora.wallet.PayHostFssSync;

/**
 * 将 pay_wallet 添卡流程与 Fission 主 BLE 的 AT+FSS 功能开关同步对接。
 */
public final class PayHostFssBridge {

    private PayHostFssBridge() {
    }

    public static void register() {
        PayHostFssSync.INSTANCE.setDelegate(new PayHostFssSync.Delegate() {
            @Override
            public void notifyInfineonPayAddCardStarting() {
                FissionSdkBleManage.getInstance().notifyInfineonPayAddCardStarting();
            }

            @Override
            public void notifyInfineonPayAddCardSuccess() {
                FissionSdkBleManage.getInstance().notifyInfineonPayAddCardSuccess();
            }

            @Override
            public void notifyInfineonPayAddCardFailed() {
                FissionSdkBleManage.getInstance().notifyInfineonPayAddCardFailed();
            }
        });
    }
}
