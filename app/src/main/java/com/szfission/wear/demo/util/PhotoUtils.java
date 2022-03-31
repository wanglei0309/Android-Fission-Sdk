package com.szfission.wear.demo.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;

import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.util.LogUtils;
import com.szfission.wear.demo.C;
import com.szfission.wear.demo.FissionSdk;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class PhotoUtils {

    public static PhotoUtils photoUtils;


    public static PhotoUtils getInstance() {
        if (null == photoUtils) {
            synchronized (FissionSdk.class) {
                if (null == photoUtils) {
                    photoUtils = new PhotoUtils();
                }
            }
        }
        return photoUtils;
    }

    //拍照
    @SuppressLint("CheckResult")
    public  void openCamera(RxPermissions rxPermissions, Activity activity) {
        String[] StringPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            StringPermission = new String[]{Manifest.permission.CAMERA};
        } else {
            StringPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        rxPermissions.request(StringPermission)
                .subscribe(granted -> {
                    if (granted) {//获取权限
                        CameraPhotoHelper.open(activity, true);
                    } else {//用户拒绝
                        PermissionUtilSetting.openAppDetailSetting(activity);
                    }
                }, throwable -> {
                    LogUtils.d("clx", "-----------拍照失败");
                });
    }

    //选照片
    @SuppressLint("CheckResult")
    public void openAlbum(RxPermissions rxPermissions, FragmentActivity fragmentActivity) {
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                            if (granted) {//获取权限
                                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                fragmentActivity.startActivityForResult(intent, C.RC_CHOOSE);
                            } else {//用户拒绝
//                                PermissionUtilSetting.openAppDetailSetting(fragmentActivity);
                            }
                        }
                );
    }
}
