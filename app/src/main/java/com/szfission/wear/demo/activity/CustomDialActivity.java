package com.szfission.wear.demo.activity;


import android.content.Context;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.bumptech.glide.Glide;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.FssStatus;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.utils.QuickLZUtils;
import com.fission.wear.sdk.v2.utils.RtkDialUtil;
import com.szfission.wear.demo.App;
import com.szfission.wear.demo.C;
import com.szfission.wear.demo.FissionSdk;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.dialog.NormalDialog;
import com.szfission.wear.demo.util.CameraPhotoHelper;
import com.szfission.wear.demo.util.PhotoUtils;
import com.szfission.wear.sdk.bean.HardWareInfo;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.FissionDialUtil;
import com.szfission.wear.sdk.util.ImageScalingUtil;
import com.szfission.wear.sdk.util.RxTimerUtil;
import com.szfission.wear.sdk.util.StringUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yalantis.ucrop.UCrop;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.fission.wear.sdk.v2.utils.FissionDialUtil.stylePosition_top;
import static com.szfission.wear.sdk.util.FissionDialUtil.getPreviewImageBitmap;
import static com.szfission.wear.sdk.util.FissionDialUtil.stylePosition_middle;

public class CustomDialActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {
    ColorMatrix colorMatrix;

    private RxPermissions rxPermissions;
    ImageView iv_watch_face;
    ImageView iv_watch_face2;

    Button btn_get_pic1;
    Button btn_get_pic2;
    Button btn_get_pic3;
    Button btn_get_pic4;
    SeekBar seekBarR;
    SeekBar seekBarR2;
    TextView tv_color;
    TextView tv_color2;
    TextView tv_progress;
    FissionDialUtil.DialModel dialModel;
    com.fission.wear.sdk.v2.utils.FissionDialUtil.DialModel dialModel2;

    int dialWidth = 320;
    int dialHeight = 390;
    int thumbnailWidth = 320;
    int thumbnailHigh = 390;
    int dialShape = 0;
    boolean isSupportAntiAliasing;
    boolean isSupportCrcChecksum;

    private int colorValue ;

    private boolean isRePush = false; //是否反复推送

    private RxTimerUtil mRxTimerUtil;

    private int num = 0;

    private long lastTime = 0; //上次升级成功的时候， 屏蔽固件重复返回进度100% 引起的逻辑问题。
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_dial);
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
            isSupportAntiAliasing = SPUtils.getInstance().getBoolean(SpKey.SUPPORT_ANTI_ALIASING);
            isSupportCrcChecksum = SPUtils.getInstance().getBoolean(SpKey.SUPPORT_DAIL_CRC_CHECKSUM);
        }

        iv_watch_face = findViewById(R.id.iv_watch_face);
        iv_watch_face2 = findViewById(R.id.iv_watch_thumb);
        btn_get_pic1 = findViewById(R.id.btn_get_pic1);
        btn_get_pic2 = findViewById(R.id.btn_get_pic2);
        btn_get_pic3 = findViewById(R.id.btn_get_pic3);
        btn_get_pic4 = findViewById(R.id.btn_get_pic4);
        seekBarR = findViewById(R.id.bar_R);
        seekBarR2 = findViewById(R.id.bar_R2);
        tv_color = findViewById(R.id.tv_color);
        tv_color2 = findViewById(R.id.tv_color2);
        tv_progress = findViewById(R.id.tv_progress);

        colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_1);

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionAtCmdResultListener(){

            @Override
            public void sendSuccess(String cmdId) {

            }

            @Override
            public void sendFail(String cmdId) {

            }

            @Override
            public void onResultTimeout(String cmdId) {

            }

            @Override
            public void onResultError(String errorMsg) {

            }

            @Override
            public void fssSuccess(FssStatus fssStatus) {
                super.fssSuccess(fssStatus);
                if(fssStatus.getFssType() == 23 && fssStatus.getFssStatus() == 100 && isRePush && System.currentTimeMillis() - lastTime > 2000){
                    lastTime = System.currentTimeMillis();
                    num++;
                    if(mRxTimerUtil!=null){
                        mRxTimerUtil.cancelTimer();
                        mRxTimerUtil = null;
                    }
                    mRxTimerUtil =  new RxTimerUtil();
                    mRxTimerUtil.timer(10000, number -> {
                        setDiaModelCompress(dialModel2);
                    });
                }
                tv_progress.setText("推送进度:"+fssStatus.getFssStatus()+"%"+", 推送成功次数："+num);
            }
        });

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionBigDataCmdResultListener() {
            @Override
            public void sendSuccess(String cmdId) {

            }

            @Override
            public void sendFail(String cmdId) {

            }

            @Override
            public void onResultTimeout(String cmdId) {

            }

            @Override
            public void onResultError(String errorMsg) {

            }

            @Override
            public void onUpdateDialProgress(int state, int progress) {
                super.onUpdateDialProgress(state, progress);
                LogUtils.d("wl", "自定义表盘推送："+progress);
            }
        });

        seekBarR.setOnSeekBarChangeListener(this);
        seekBarR2.setOnSeekBarChangeListener(this);
        rxPermissions = new RxPermissions(this);
        btn_get_pic1.setOnClickListener(v -> {
            NormalDialog normalDialog = new NormalDialog(CustomDialActivity.this,2, ModelConstant.FUNC_PUSH_CUSTOM_DIAL);
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
                dialModel.setPreviewImage(bitmap);
                dialModel.setBackgroundImage(bitmap);
                dialModel.setDialPosition(1);
                dialModel.setPreImageWidth(thumbnailWidth);
                dialModel.setPreImageHeight(thumbnailHigh);
                dialModel.setDialPosition(stylePosition_middle);
                dialModel.setDialStyleColor(colorValue);
                Bitmap thumbBitmap2 = ImageScalingUtil.extractMiniThumb(dialModel.getPreviewImage(),
                        dialModel.getPreImageWidth(), dialModel.getPreImageHeight());


                File file = new File(getPath() + File.separator + "customDial.bin");
                dialModel.setFile(file);
                setDiaModel(dialModel);



            }
        });

        btn_get_pic3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRePush = false;
                Bitmap bitmap = ((BitmapDrawable) iv_watch_face.getDrawable()).getBitmap();
                dialModel2 = new com.fission.wear.sdk.v2.utils.FissionDialUtil.DialModel();
                dialModel2.setDialShape(dialShape);
                dialModel2.setDialWidth(dialWidth);
                dialModel2.setDialHeight(dialHeight);
                dialModel2.setPreviewImage(bitmap);
                dialModel2.setBackgroundImage(bitmap);
                dialModel2.setDialPosition(1);
                dialModel2.setPreImageWidth(thumbnailWidth);
                dialModel2.setPreImageHeight(thumbnailHigh);
                dialModel2.setDialStyleColor(colorValue);
                dialModel2.setSupportAntiAliasing(isSupportAntiAliasing);
                dialModel2.setSupportCrcChecksum(isSupportCrcChecksum);
                Bitmap thumbBitmap2 = ImageScalingUtil.extractMiniThumb(dialModel2.getPreviewImage(),
                        dialModel2.getPreImageWidth(), dialModel2.getPreImageHeight());


                File file = new File(getPath() + File.separator + "customDial.bin");
                dialModel2.setFile(file);
                setDiaModelCompress(dialModel2);
            }
        });

        btn_get_pic4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRePush = true;
                Bitmap bitmap = ((BitmapDrawable) iv_watch_face.getDrawable()).getBitmap();
                dialModel2 = new com.fission.wear.sdk.v2.utils.FissionDialUtil.DialModel();
                dialModel2.setDialShape(dialShape);
                dialModel2.setDialWidth(dialWidth);
                dialModel2.setDialHeight(dialHeight);
                dialModel2.setPreviewImage(bitmap);
                dialModel2.setBackgroundImage(bitmap);
                dialModel2.setDialPosition(1);
                dialModel2.setPreImageWidth(thumbnailWidth);
                dialModel2.setPreImageHeight(thumbnailHigh);
                dialModel2.setDialPosition(stylePosition_top);
                dialModel2.setDialStyleColor(colorValue);
                dialModel2.setSupportAntiAliasing(isSupportAntiAliasing);
                Bitmap thumbBitmap2 = ImageScalingUtil.extractMiniThumb(dialModel2.getPreviewImage(),
                        dialModel2.getPreImageWidth(), dialModel2.getPreImageHeight());


                File file = new File(getPath() + File.separator + "customDial.bin");
                dialModel2.setFile(file);
                setDiaModelCompress(dialModel2);
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

    private void setDiaModel(FissionDialUtil.DialModel dialModel)  {
        Bitmap bitmap1 = getPreviewImageBitmap(this,dialModel);
        iv_watch_face2.setImageBitmap(bitmap1);
        byte[] resultData =  FissionDialUtil.getDiaInfoBinData(this,dialModel);
//        FissionSdk.getInstance().startDial(resultData, FissionEnum.WRITE_DIAL_DATA);
        LogUtils.d("wl", "相册自定义表盘字节大小："+resultData.length);
        FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_DIAL_DATA);
    }

    private void setDiaModelCompress(com.fission.wear.sdk.v2.utils.FissionDialUtil.DialModel dialModel)  {
        Bitmap bitmap1 = com.fission.wear.sdk.v2.utils.FissionDialUtil.getPreviewImageBitmap(this,dialModel);
        iv_watch_face2.setImageBitmap(bitmap1);
        byte[] resultData = null;
        if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK){
            resultData = com.fission.wear.sdk.v2.utils.FissionDialUtil.getDiaInfoBinData(this, dialModel);
        }else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
            resultData = RtkDialUtil.getInstance().getSimpleDialBinFile(this, dialModel);
        }
        byte[] outData = QuickLZUtils.compressFission(resultData);
        LogUtils.d("wl", "相册自定义表盘字节大小(压缩前)："+resultData.length);
//        String filePath = Environment.getExternalStorageDirectory()+"/custom_dial_1.bin";
//        String filePath2 = Environment.getExternalStorageDirectory()+"/custom_dial_2.bin";
//        FileUtils.createFileByDeleteOldFile(filePath);
//        FileUtils.createFileByDeleteOldFile(filePath2);
//        FileIOUtils.writeFileFromBytesByStream (filePath, resultData);
//        FileIOUtils.writeFileFromBytesByStream (filePath2, outData);
        ToastUtils.showLong("自定义表盘打包成功");
        FissionSdkBleManage.getInstance().startDial(outData, FissionEnum.WRITE_DIAL_DATA_V2);
    }

    private void startDfu(String absolutePath) {
//        OtaUtils.startDfu(this,absolutePath,false);

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




    private File saveFile(Context context, Bitmap body) {
        //把bitmap 转换为byte
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        body.compress(Bitmap.CompressFormat.PNG, 100, arrayOutputStream);
        byte[] bitmapByte = arrayOutputStream.toByteArray();
        File futureStudioIconFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "fission_ota.png");

        InputStream inputStream  = null;
        OutputStream outputStream = null;

        byte[] fileReader         = new byte[4096];
        int   fileSize           = bitmapByte.length;
        long   fileSizeDownloaded = 0;
        try {
//            inputStream = body.getRowBytes();
            int read;
            outputStream = new FileOutputStream(futureStudioIconFile);
//            while ((read = inputStream.read(fileReader)) != -1) {
                outputStream.write(bitmapByte, 0, fileSize);
                //统计这个下载的数量
//            }
            outputStream.flush();
            return futureStudioIconFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    String filePath = "";
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == C.RC_CAMERA) {//【拍照
            Uri file = CameraPhotoHelper.getOutputMediaFileUri(this);
            if (file != null) {
//                filePath = UriUtils.uri2File(file).getAbsolutePath();
//                LogUtils.d("获取路径",filePath);
//                Glide.with(this).load(filePath).into(iv_watch_face);
                CameraPhotoHelper.cropImage(this, file, dialWidth, dialHeight, false);
            }


//            CameraPhotoHelper.cropImage(UserInfoActivity.this, UriUtils.file2Uri(file), 480, 480, true);
        } else if (resultCode == RESULT_OK && requestCode == C.RC_CHOOSE) {//选择照片
            LogUtils.d("获取getData"+data.getData());
            if (data.getData() != null) {
//               File ff =  UriUtils.uri2File(data.getData());
//                LogUtils.d("获取路径",ff.getAbsolutePath());
//                Glide.with(this).load(data.getData()).into(iv_watch_face);
//                saveFile(this,((BitmapDrawable)iv_watch_face.getDrawable()).getBitmap());
                CameraPhotoHelper.cropImage(this, data.getData(), dialWidth, dialHeight, false);
            }
        }else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            Uri bgUri = UCrop.getOutput(data);
            Glide.with(this).load(bgUri).into(iv_watch_face);
            LogUtils.d("clx", "_-------" + bgUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            LogUtils.e("clx", "------裁剪错误" + data);
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
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_1);
                    break;

                case 1:
                    tv_color.setText("当前选中小字体颜色：绿色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_2);
                    break;

                case 2:
                    tv_color.setText("当前选中小字体颜色：红色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_3);
                    break;

                case 3:
                    tv_color.setText("当前选中小字体颜色：黄色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_4);
                    break;

                case 4:
                    tv_color.setText("当前选中小字体颜色：橘红色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_5);
                    break;

                case 5:
                    tv_color.setText("当前选中小字体颜色：紫色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_6);
                    break;

                case 6:
                    tv_color.setText("当前选中小字体颜色：天蓝色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_7);
                    break;

                case 7:
                    tv_color.setText("当前选中小字体颜色：黑色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_8);
                    break;

                case 8:
                    tv_color.setText("当前选中小字体颜色：深蓝色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_9);
                    break;
            }
        }else{
            switch (progress){
                case 0:
                    tv_color2.setText("当前选中大字体颜色：白色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_white_big);
                    break;

                case 1:
                    tv_color2.setText("当前选中大字体颜色：绿色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_green_big);
                    break;

                case 2:
                    tv_color2.setText("当前选中大字体颜色：红色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_red_big);
                    break;

                case 3:
                    tv_color2.setText("当前选中大字体颜色：黄色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_yellow_big);
                    break;

                case 4:
                    tv_color2.setText("当前选中大字体颜色：橘红色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_orange_big);
                    break;

                case 5:
                    tv_color2.setText("当前选中大字体颜色：紫色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_purple_big);
                    break;

                case 6:
                    tv_color2.setText("当前选中大字体颜色：天蓝色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_sky_blue_big);
                    break;

                case 7:
                    tv_color2.setText("当前选中大字体颜色：灰色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_gray_big);
                    break;

                case 8:
                    tv_color2.setText("当前选中大字体颜色：浅蓝色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_light_blue_big);
                    break;

                case 9:
                    tv_color2.setText("当前选中大字体颜色：深蓝色");
                    colorValue = getResources().getColor(com.linwear.baidu.map.watch.R.color.public_custom_dial_dark_blue_big);
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
