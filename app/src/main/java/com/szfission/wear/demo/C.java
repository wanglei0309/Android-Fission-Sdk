package com.szfission.wear.demo;

public interface C {
    //连接状态
    int CONNECT_NOT = 0,//未连接
            CONNECTED = 1,//已连接
            DISCONNECT = 2,//断开连接
            CONNECT_LOADING = 3,//连接中
           CONNECT_OVER_TIME =4;//连接超时
    int battery = 10;

    int FSS_DND = 3;
    String DATE_STYLE1 = "yyyy-mm-dd HH:MM:SS";

    String APP_FILE_PATH   =  "com.szfission.wear.demo.provider";


    //Activity返回码
    int RC_CAMERA_AND_WRITE = 1, // 申请相机及写入权限
            RC_CHOOSE           = 2, // 选择图片
            RC_GAME_ZONE        = 3, // 游戏专区
            RC_VIDEO            = 4, // 视频
            RC_RECORD_AUDIO     = 5, // 录音
            RC_LOCATION         = 6, // 位置
            RC_CAMERA           = 7, //拍照
            RC_SCAN             = 8, //扫码
            RC_CROP             = 9, //裁剪
            RC_EXIT_CAMERA      = 10,//退出拍照
            RC_SIGN_IN          = 11;//Google 登录

}
