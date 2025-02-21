package com.szfission.wear.demo.util;

/**
 * describe:
 * author: wl
 * createTime: 2024/10/16
 */
import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSession;

import com.fission.wear.sdk.v2.service.BleComService;

public class ExternalMusicControl {

    private MediaSessionManager mediaSessionManager;
    private MediaController mediaController;

    public ExternalMusicControl(Context context) {
        mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        ComponentName mComponentName = new ComponentName(context,
                BleComService.class);
        // 获取当前活跃的 MediaSession 列表
        for (MediaController controller : mediaSessionManager.getActiveSessions(mComponentName)) {
            // 在这里可以找到控制音乐播放的 MediaSession
            if (controller.getPackageName().equals("com.tencent.qqmusic")) {
                mediaController = controller;
                break;
            }
        }
    }

    // 调整音量
    public void adjustVolume(int direction) {
        if (mediaController != null) {
            mediaController.adjustVolume(direction, 0);
        }
    }
}

