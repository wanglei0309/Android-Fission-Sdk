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
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.bumptech.glide.Glide;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.utils.QuickLZUtils;
import com.szfission.wear.demo.App;
import com.szfission.wear.demo.C;
import com.szfission.wear.demo.FissionSdk;
import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.dialog.NormalDialog;
import com.szfission.wear.demo.util.CameraPhotoHelper;
import com.szfission.wear.demo.util.PhotoUtils;
import com.szfission.wear.sdk.constant.FissionEnum;
import com.szfission.wear.sdk.util.FissionDialUtil;
import com.szfission.wear.sdk.util.ImageScalingUtil;
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

import static com.szfission.wear.sdk.util.FissionDialUtil.getPreviewImageBitmap;
import static com.szfission.wear.sdk.util.FissionDialUtil.stylePosition_middle;

@ContentView(R.layout.activity_push_dial)
public class CustomDialActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {
    ColorMatrix colorMatrix;

    private RxPermissions rxPermissions;
    @ViewInject(R.id.iv_watch_face)
    ImageView iv_watch_face;
    @ViewInject(R.id.iv_watch_thumb)
    ImageView iv_watch_face2;
    @ViewInject(R.id.btn_send)
    Button btn_send;

    @ViewInject(R.id.btn_get_pic1)
    Button btn_get_pic1;
    @ViewInject(R.id.btn_get_pic2)
    Button btn_get_pic2;
    @ViewInject(R.id.btn_get_pic3)
    Button btn_get_pic3;
    @ViewInject(R.id.iv_watch3)
    ImageView iv_watch3;
    @ViewInject(R.id.bar_R)
    SeekBar seekBarR;
    @ViewInject(R.id.bar_G)
    SeekBar seekBarG;
    @ViewInject(R.id.bar_B)
    SeekBar seekBarB;
    @ViewInject(R.id.yanse)
    TextView yanse;
    @ViewInject(R.id.yanse2)
    TextView yanse2;
    FissionDialUtil.DialModel dialModel;
    com.fission.wear.sdk.v2.utils.FissionDialUtil.DialModel dialModel2;

    int dialWidth = 320;
    int dialHeight = 390;
    int thumbnailWidth = 320;
    int thumbnailHigh = 390;
    int dialShape = 0;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                LogUtils.d("wl", "????????????????????????"+progress);
            }
        });

        seekBarR.setOnSeekBarChangeListener(this);
        seekBarG.setOnSeekBarChangeListener(this);
        seekBarB.setOnSeekBarChangeListener(this);
        rxPermissions = new RxPermissions(this);
        btn_get_pic1.setOnClickListener(v -> {
            NormalDialog normalDialog = new NormalDialog(CustomDialActivity.this,2, ModelConstant.FUNC_PUSH_CUSTOM_DIAL);
            normalDialog.setOnConfirmClickListener(content -> {
                if (content.equals("0")){
                    //????????????
                    PhotoUtils.getInstance().openAlbum(rxPermissions,this);
                }else {
                    //????????????
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
                dialModel.setDialStyleColor(getResources().getColor(R.color.public_custom_dial_8));
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
                dialModel2.setDialPosition(stylePosition_middle);
                dialModel2.setDialStyleColor(getResources().getColor(R.color.public_custom_dial_8));
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
        LogUtils.d("wl", "????????????????????????????????????"+resultData.length);
        FissionSdkBleManage.getInstance().startDial(resultData, FissionEnum.WRITE_DIAL_DATA);
    }

    private void setDiaModelCompress(com.fission.wear.sdk.v2.utils.FissionDialUtil.DialModel dialModel)  {
        Bitmap bitmap1 = com.fission.wear.sdk.v2.utils.FissionDialUtil.getPreviewImageBitmap(this,dialModel);
        iv_watch_face2.setImageBitmap(bitmap1);
        byte[] resultData = com.fission.wear.sdk.v2.utils.FissionDialUtil.getDiaInfoBinData(this, dialModel);
        byte[] outData = QuickLZUtils.compressFission(resultData);
//        String filePath = Environment.getExternalStorageDirectory()+"/custom_dial_1.bin";
//        String filePath2 = Environment.getExternalStorageDirectory()+"/custom_dial_2.bin";
//        FileUtils.createFileByDeleteOldFile(filePath);
//        FileUtils.createFileByDeleteOldFile(filePath2);
//        FileIOUtils.writeFileFromBytesByStream (filePath, resultData);
//        FileIOUtils.writeFileFromBytesByStream (filePath2, outData);
//        ToastUtils.showLong("???????????????????????????");
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
        //???bitmap ?????????byte
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
                //???????????????????????????
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
        if (resultCode == RESULT_OK && requestCode == C.RC_CAMERA) {//?????????
            Uri file = CameraPhotoHelper.getOutputMediaFileUri(this);
            if (file != null) {
//                filePath = UriUtils.uri2File(file).getAbsolutePath();
//                LogUtils.d("????????????",filePath);
//                Glide.with(this).load(filePath).into(iv_watch_face);
                CameraPhotoHelper.cropImage(this, file, dialWidth, dialHeight, false);
            }


//            CameraPhotoHelper.cropImage(UserInfoActivity.this, UriUtils.file2Uri(file), 480, 480, true);
        } else if (resultCode == RESULT_OK && requestCode == C.RC_CHOOSE) {//????????????
            LogUtils.d("??????getData"+data.getData());
            if (data.getData() != null) {
//               File ff =  UriUtils.uri2File(data.getData());
//                LogUtils.d("????????????",ff.getAbsolutePath());
//                Glide.with(this).load(data.getData()).into(iv_watch_face);
//                saveFile(this,((BitmapDrawable)iv_watch_face.getDrawable()).getBitmap());
                CameraPhotoHelper.cropImage(this, data.getData(), dialWidth, dialHeight, false);
            }
        }else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            Uri bgUri = UCrop.getOutput(data);
            Glide.with(this).load(bgUri).into(iv_watch_face);
            LogUtils.d("clx", "_-------" + bgUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            LogUtils.e("clx", "------????????????" + data);
        }
    }

    public String getPath() {
        File dir = null;
        boolean state = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (state) {
            if (Build.VERSION.SDK_INT >= 28) {
                //Android10??????
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
        colorMatrix.setScale(caculate(seekBarR.getProgress()), caculate(seekBarG.getProgress()),
                caculate(seekBarB.getProgress()), caculate(255));
//        Bitmap bitmap1 = getPreviewImageBitmap(this,dialModel,colorMatrix);
//        iv_watch_face2.setImageBitmap(bitmap1);
//        yanse.setText("#"
//                + Integer.toHexString(seekBarR.getProgress())
//                + Integer.toHexString(seekBarG.getProgress())
//                + Integer.toHexString(seekBarB.getProgress()));
//
//        int color =  Color.parseColor(yanse.getText().toString());
//        yanse2.setText("?????????"+Color.red(color)+"??????:"+Color.green(color)+"??????:"+Color.blue(color));

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
