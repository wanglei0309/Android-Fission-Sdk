package com.szfission.wear.demo.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.szfission.wear.demo.C;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author chengliuxu
 * @name LinWear
 * @class name：com.lw.commonsdk.utlis
 * @class describe 拍照
 * @time 2020/5/22 10:22 AM
 * @change
 * @chang time
 * @class describe
 */
public class CameraPhotoHelper {

    public static void open(Activity activity, boolean isCrop) {
        Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        openCameraIntent.putExtra("crop", isCrop);
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, getOutputMediaFileUri(activity));
        openCameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        activity.startActivityForResult(openCameraIntent, C.RC_CAMERA);
    }

    // 获取缩略图uri
    public static File getThumbUri(Activity activity) {
        try {
            File mediaStorageDir;
            mediaStorageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file         = new File(mediaStorageDir.getPath() + "/camera_photo.jpg");
            int  rotateDegree = ImageUtils.getRotateDegree(file.getPath());
            LogUtils.d("图片地址：" + file.getPath() + "\n旋转角度：" + rotateDegree);
            Uri outputMediaFileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                outputMediaFileUri = FileProvider.getUriForFile(activity, C.APP_FILE_PATH, file);
            } else {
                outputMediaFileUri = Uri.fromFile(file);
            }
            Bitmap       bitmap       = getBitmapFormUri(activity, outputMediaFileUri);
            OutputStream outputStream = activity.getContentResolver().openOutputStream(outputMediaFileUri);
            bitmap = ImageUtils.rotate(bitmap, rotateDegree, 0, 0);//旋转的图片
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.e("clx", "-------------e：" + e.getMessage());
        }
        return null;
    }

    // 输出图片路径
    public static Uri getOutputMediaFileUri(Context context) {
        File mediaFile = null;
        try {
            File mediaStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }
            mediaFile = new File(mediaStorageDir.getPath() + "/camera_photo.jpg");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, C.APP_FILE_PATH, mediaFile);
        } else {
            return Uri.fromFile(mediaFile);
        }
    }

    // 根据uri获取bitmap
    private static Bitmap getBitmapFormUri(Context context, Uri uri) throws IOException {
        return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
//        InputStream input = context.getContentResolver().openInputStream(uri);
//        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
//        onlyBoundsOptions.inJustDecodeBounds = true;
//        onlyBoundsOptions.inDither = true;
//        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.RGB_565;
//        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
//        input.close();
//        int originalWidth = onlyBoundsOptions.outWidth;
//        int originalHeight = onlyBoundsOptions.outHeight;
//        if ((originalWidth == -1) || (originalHeight == -1))
//            return null;
//
//        float hh = 800f, ww = 480f;
//        int be = 1;
//        if (originalWidth > originalHeight && originalWidth > ww) {
//            be = (int) (originalWidth / ww);
//        } else if (originalWidth < originalHeight && originalHeight > hh) {
//            be = (int) (originalHeight / hh);
//        }
//        if (be <= 0)
//            be = 1;
//
//
//        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
//        bitmapOptions.inSampleSize = be;
//        bitmapOptions.inDither = true;
//        bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
//        input = context.getContentResolver().openInputStream(uri);
//        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
//        input.close();
//
//        return compressImage(bitmap);
    }

    // 获取缩略图uri
    public static File getAlbumThumbUri(Bitmap bitmap, Activity activity) {
        try {
            File mediaStorageDir;
            mediaStorageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            LogUtils.d("clx", "1---------Album：" + mediaStorageDir);
            File file = new File(mediaStorageDir.getPath() + "/album_photo.jpg");
            Uri  albumUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                albumUri = FileProvider.getUriForFile(activity, C.APP_FILE_PATH, file);
            } else {
                albumUri = Uri.fromFile(file);
            }
            OutputStream outputStream = activity.getContentResolver().openOutputStream(albumUri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            LogUtils.d("clx", "2---------Album：" + file);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.e("clx", "-------------e：" + e.getMessage());
        }
        return null;
    }

    private static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {
            baos.reset();

            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;//每次都减少10
            if (options <= 0)
                break;
        }

        ByteArrayInputStream isBm   = new ByteArrayInputStream(baos.toByteArray());
        Bitmap               bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    /**
     * 读取照片旋转角度
     *
     * @param path 照片路径
     * @return 角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int           orientation   = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
            LogUtils.d("clx", "----------图片旋转角度" + degree);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }


    public static void cropImage(Activity activity, @NonNull Uri source, int width, int height, boolean isCircle) {

        UCrop uCrop = UCrop.of(source, Uri.fromFile(new File(activity.getCodeCacheDir(), System.currentTimeMillis() + ".jpg")));
        //手动设置高级选项
        UCrop.Options options = new UCrop.Options();
//        options.setCompressionFormat(Bitmap.CompressFormat.PNG);//图片类型
//        options.setCompressionQuality(100); //图片质量
        options.setToolbarTitle("Crop");//设置标题栏文字
        options.setToolbarWidgetColor(Color.parseColor("#FFFFFF"));//标题字的颜色以及按钮颜色
//        options.setDimmedLayerColor(Color.parseColor("#00FFFFFF"));//设置裁剪外颜色
        options.withAspectRatio(width, height);//设置裁剪框的宽高比例
        options.withMaxResultSize(width, height);//结果图片大小
        options.setCropFrameStrokeWidth(2);//设置裁剪框的宽度
        options.setMaxScaleMultiplier(2);//设置最大缩放比例
        options.setShowCropGrid(false);  //设置是否显示裁剪网格
        options.setShowCropFrame(true); //设置是否显示裁剪边框(true为方形边框)
        options.setCircleDimmedLayer(isCircle);//是否为圆形
        options.setHideBottomControls(true);//隐藏底部的按钮些
        options.setCropFrameColor(Color.WHITE);//设置裁剪框的颜色
//        options.setFreeStyleCropEnabled(true);//调整边框大小
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);//设置裁剪图片可操作的手势
//        options.setToolbarColor(ActivityCompat.getColor(activity, R.color.material_white));
//        options.setStatusBarColor(ActivityCompat.getColor(activity, R.color.material_white));    //设置状态栏颜色
        uCrop.withOptions(options);
        uCrop.start(activity);
    }

}
