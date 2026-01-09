package com.szfission.wear.demo.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * 简单的本地音频播放工具类
 */
public class SimpleAudioPlayer {

    private static final String TAG = "SimpleAudioPlayer";
    private MediaPlayer mediaPlayer;

    private static SimpleAudioPlayer instance;

    private SimpleAudioPlayer() {
        mediaPlayer = new MediaPlayer();
    }

    public static SimpleAudioPlayer getInstance() {
        if (instance == null) {
            synchronized (SimpleAudioPlayer.class) {
                if (instance == null) {
                    instance = new SimpleAudioPlayer();
                }
            }
        }
        return instance;
    }

    /**
     * 播放本地音频文件
     *
     * @param context  上下文
     * @param filePath 本地文件路径，如 /sdcard/Download/tts.mp3
     * @param listener 播放完成监听
     */
    public void play(Context context, String filePath, OnCompleteListener listener) {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "播放完成: " + filePath);
                if (listener != null) listener.onComplete();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "播放错误 what=" + what + ", extra=" + extra);
                return true;
            });

        } catch (IOException e) {
            Log.e(TAG, "播放失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public interface OnCompleteListener {
        void onComplete();
    }
}

