package com.szfission.wear.demo.activity;


import static com.szfission.wear.sdk.util.FissionDialUtil.stylePosition_middle;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.bumptech.glide.Glide;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.utils.FissionDialUtil;
import com.fission.wear.sdk.v2.utils.HsDialUtils;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.dialogx.interfaces.OnBackPressedListener;
import com.szfission.wear.demo.App;
import com.szfission.wear.demo.C;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.dialog.NormalDialog;
import com.szfission.wear.demo.util.CameraPhotoHelper;
import com.szfission.wear.demo.util.PhotoUtils;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.ImageScalingUtil;
import com.szfission.wear.sdk.util.RxTimerUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yalantis.ucrop.UCrop;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.File;

public class HsCustomDialActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {
    ColorMatrix colorMatrix;

    private RxPermissions rxPermissions;
    ImageView iv_watch_face;
    ImageView iv_watch_face2;
    Button btn_get_pic1;
    Button btn_get_pic2;
    SeekBar seekBarR;
    TextView tv_color;
    TextView tv_progress;


    Button btn_get_video;

    Button btn_push_video_dial;

    FissionDialUtil.DialModel dialModel;
    com.fission.wear.sdk.v2.utils.FissionDialUtil.DialModel dialModel2;

    int dialWidth = 466;
    int dialHeight = 466;
    int thumbnailWidth = 300;
    int thumbnailHigh = 300;
    int dialShape = 1;
    private int colorValue ;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_dial_hs);
        HsDialUtils.getInstance().init();

        setTitle(R.string.FUNC_PUSH_CUSTOM_DIAL);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        colorMatrix = new ColorMatrix();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        if(App.mHardWareInfo!=null){
            dialWidth = App.mHardWareInfo.getDeviceWidth();
            dialHeight = App.mHardWareInfo.getDeviceHigh();
            thumbnailWidth = App.mHardWareInfo.getThumbnailWidth();
            thumbnailHigh = App.mHardWareInfo.getThumbnailHigh();
            dialShape = App.mHardWareInfo.getDialShape();
        }

        iv_watch_face = findViewById(R.id.iv_watch_face);
        iv_watch_face2 = findViewById(R.id.iv_watch_thumb);
        btn_get_pic1 = findViewById(R.id.btn_get_pic1);
        btn_get_pic2 = findViewById(R.id.btn_get_pic2);
        seekBarR = findViewById(R.id.bar_R);
        tv_color = findViewById(R.id.tv_color);
        tv_progress = findViewById(R.id.tv_progress);
        btn_get_video = findViewById(R.id.btn_get_video);
        btn_push_video_dial = findViewById(R.id.btn_push_video_dial);

        colorValue = getResources().getColor(R.color.public_custom_dial_1);

        seekBarR.setOnSeekBarChangeListener(this);
        rxPermissions = new RxPermissions(this);
        btn_get_pic1.setOnClickListener(v -> {
            NormalDialog normalDialog = new NormalDialog(HsCustomDialActivity.this,2, ModelConstant.FUNC_PUSH_CUSTOM_DIAL);
            normalDialog.setOnConfirmClickListener(content -> {
                if (content.equals("0")){
                    //打开相册
                    PhotoUtils.getInstance().openAlbum(rxPermissions,this);
                }else {
                    //打开相机
                    PhotoUtils.getInstance().openCamera(rxPermissions,this);
                }
            });
        });
        btn_get_pic2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ((BitmapDrawable) iv_watch_face.getDrawable()).getBitmap();
                dialModel = new FissionDialUtil.DialModel();
                dialModel.setDialShape(dialShape);
                dialModel.setDialWidth(dialWidth);
                dialModel.setDialHeight(dialHeight);
                dialModel.setBackgroundImage(bitmap);
                dialModel.setDialPosition(1);
                dialModel.setPreImageWidth(thumbnailWidth);
                dialModel.setPreImageHeight(thumbnailHigh);
                dialModel.setDialStyleColor(colorValue);
                Bitmap thumbBitmap = ImageScalingUtil.extractMiniThumb(dialModel.getBackgroundImage(),
                        dialModel.getPreImageWidth(), dialModel.getPreImageHeight());
                dialModel.setPreviewImage(thumbBitmap);
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        WaitDialog.show("正在打包表盘...")
                                .setCancelable(false)
                                .setOnBackPressedListener(new OnBackPressedListener<WaitDialog>() {
                                    @Override
                                    public boolean onBackPressed(WaitDialog dialog) {
                                        return false;
                                    }
                                });
                        byte[] resultData = HsDialUtils.getInstance().getSimpleDialBinFile(HsCustomDialActivity.this, dialModel);
                        WaitDialog.dismiss();
//                        String filePath = Environment.getExternalStorageDirectory()+"/custom_dial_hs.bin";
//                        FileUtils.createFileByDeleteOldFile(filePath);
//                        FileIOUtils.writeFileFromBytesByStream (filePath, resultData);
                        ToastUtils.showLong("自定义表盘打包成功");
                        FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_DIAL_DATA_V2);
                    }
                }.start();
            }
        });

        btn_get_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 30 ){
                    // 先判断有没有权限
                    if (Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        Uri uri = Uri.parse(getPath());
                        intent.setDataAndType(uri, "*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 1);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" +getApplication().getPackageName()));
                        startActivity(intent);
                    }
                }else{
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    Uri uri = Uri.parse(getPath());
                    intent.setDataAndType(uri, "*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 1);
                }
            }
        });

        btn_push_video_dial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ((BitmapDrawable) iv_watch_face.getDrawable()).getBitmap();
                dialModel = new FissionDialUtil.DialModel();
                dialModel.setDialShape(dialShape);
                dialModel.setDialWidth(466);
                dialModel.setDialHeight(466);
                dialModel.setBackgroundImage(bitmap);
                dialModel.setDialPosition(1);
                dialModel.setPreImageWidth(300);
                dialModel.setPreImageHeight(300);
                dialModel.setDialStyleColor(colorValue);
                Bitmap thumbBitmap = ImageScalingUtil.extractMiniThumb(dialModel.getBackgroundImage(),
                        dialModel.getPreImageWidth(), dialModel.getPreImageHeight());
                dialModel.setPreviewImage(thumbBitmap);
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        WaitDialog.show("正在打包表盘...")
                                .setCancelable(false)
                                .setOnBackPressedListener(new OnBackPressedListener<WaitDialog>() {
                                    @Override
                                    public boolean onBackPressed(WaitDialog dialog) {
                                        return false;
                                    }
                                });
                        byte[] resultData = HsDialUtils.getInstance().getVideoDialBinFile(HsCustomDialActivity.this, dialModel, filePath);
                        WaitDialog.dismiss();
                        String filePath = Environment.getExternalStorageDirectory()+"/custom_video_dial_hs.bin";
                        FileUtils.createFileByDeleteOldFile(filePath);
                        FileIOUtils.writeFileFromBytesByStream (filePath, resultData);
                        ToastUtils.showLong("自定义表盘打包成功");
                        FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_DIAL_DATA_V2);
                    }
                }.start();
            }
        });
    }

    public static Bitmap drawBg4Bitmap(int color, Bitmap originBitmap) {
        Bitmap updateBitmap = Bitmap.createBitmap(originBitmap.getWidth(),
                originBitmap.getHeight(), originBitmap.getConfig());
        Paint paint = new Paint();
        Canvas canvas = new Canvas(updateBitmap);
         ColorMatrix colorMatrix = new ColorMatrix();
         int R = Color.red(color);
        int G = Color.green(color);
        int B = Color.blue(color);
        int A = Color.alpha(color);
//        colorMatrix.setScale(intToHex(R), intToHex(G),intToHex(B),0xff);
        paint.setAntiAlias(true);
        final Matrix matrix = new Matrix();
        canvas.drawBitmap(originBitmap, matrix, paint);
        return updateBitmap;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Bitmap toGray(Bitmap bmpOriginal) {
        int width = bmpOriginal.getWidth();
        int height = bmpOriginal.getHeight();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    String filePath = "";
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == C.RC_CAMERA) {//【拍照
            Uri file = CameraPhotoHelper.getOutputMediaFileUri(this);
            if (file != null) {
                CameraPhotoHelper.cropImage(this, file, dialWidth, dialHeight, false);
            }

        } else if (resultCode == RESULT_OK && requestCode == C.RC_CHOOSE) {//选择照片
            LogUtils.d("获取getData"+data.getData());
            if (data.getData() != null) {
                CameraPhotoHelper.cropImage(this, data.getData(), dialWidth, dialHeight, false);
            }
        }else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            Uri bgUri = UCrop.getOutput(data);
            Glide.with(this).load(bgUri).override(dialWidth, dialHeight).into(iv_watch_face);
            LogUtils.d("clx", "_-------" + bgUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            LogUtils.e("clx", "------裁剪错误" + data);
        }else{
            if(data!=null){
                Uri uri = data.getData();
                String dd = uri.toString();
                String prefix = dd.substring(dd.lastIndexOf(".")+1);
                LogUtils.d("prefix",prefix);
                String name = null;
                if ("mp4".equals(prefix)){
                    String path = uri.getPath();
                    String []split = path.split(":");
                    ToastUtils.showShort("视频文件名:"+split[split.length-1]);
                }else {
                    ToastUtils.showShort("后缀名不对,请重新选择");
                }
                filePath = UriUtils.uri2File(uri).getAbsolutePath();
                LogUtils.d("获取文件路径getData",filePath);
            }
        }

    }

    public String getPath() {
        File dir = null;
        boolean state = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (state) {
            if (Build.VERSION.SDK_INT >= 28) {
                //Android10之后
                dir = this.getExternalFilesDir(null);
            } else {
                dir = Environment.getExternalStorageDirectory();
            }
        } else {
            dir = Environment.getRootDirectory();
        }
        return dir.toString();
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar.getId() == R.id.bar_R){
            switch (progress){
                case 0:
                    tv_color.setText("当前选中小字体颜色：白色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_1);
                    break;

                case 1:
                    tv_color.setText("当前选中小字体颜色：绿色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_2);
                    break;

                case 2:
                    tv_color.setText("当前选中小字体颜色：红色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_3);
                    break;

                case 3:
                    tv_color.setText("当前选中小字体颜色：黄色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_4);
                    break;

                case 4:
                    tv_color.setText("当前选中小字体颜色：橘红色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_5);
                    break;

                case 5:
                    tv_color.setText("当前选中小字体颜色：紫色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_6);
                    break;

                case 6:
                    tv_color.setText("当前选中小字体颜色：天蓝色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_7);
                    break;

                case 7:
                    tv_color.setText("当前选中小字体颜色：黑色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_8);
                    break;

                case 8:
                    tv_color.setText("当前选中小字体颜色：深蓝色");
                    colorValue = getResources().getColor(R.color.public_custom_dial_9);
                    break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    protected float caculate(int progress) {
        float scale = progress / 128f;
        return scale;
    }
}
