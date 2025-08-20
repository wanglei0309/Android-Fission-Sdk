package com.szfission.wear.demo.fragment;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.HsJsFileInfo;
import com.fission.wear.sdk.v2.bean.HsMultimediaFileInfo;
import com.fission.wear.sdk.v2.bean.TransferNotify;
import com.fission.wear.sdk.v2.bean.TransferParameterRequest;
import com.fission.wear.sdk.v2.bean.TransferRequest;
import com.fission.wear.sdk.v2.bean.TransferResponse;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.HiSiliconDataResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.utils.HiSiliconFileTransferUtils;
import com.fission.wear.sdk.v2.utils.HsDialUtils;
import com.realsil.sdk.dfu.model.DfuProgressInfo;
import com.realsil.sdk.dfu.model.OtaDeviceInfo;
import com.realsil.sdk.dfu.model.Throughput;
import com.realsil.sdk.dfu.utils.DfuAdapter;
import com.szfission.wear.demo.R;

import java.io.File;
import java.util.List;

/**
 * describe:
 * author: wl
 * createTime: 2023/11/25
 */

public class UploadFragment extends Fragment {

    private String path;

    private String filePath;

    private String uploadPath;

    TextView tv_file_path, tv_progress, tv_rate;

    Button btn_select, btn_ota, btn_send_dial, btn_send_file, btn_send_js_app, btn_send_ebook, btn_send_music, btn_send_video,
            btn_send_ringtone_msg, btn_send_ringtone_call, btn_send_ringtone_clock, btn_stop, btn_send_image;

    Spinner spinner;

    ProgressBar horizontalProgressBar;

    private int dataBlockNum = 50;

    private int dataBlockNumCount; //数据总包数

    private int dataBlockSize = 965;


    private int fileSize = 0;

    private String fileName;

    byte[] resultData = null;

    private int framesCount =0; //文件数据分帧

    private int curFrames = 0; //当前帧数

    private List<String> fileDataHexList;

    private long startTime;

    private int otaType;

    public UploadFragment() {
        // Required empty public constructor
    }

    public static UploadFragment newInstance() {
        UploadFragment fragment = new UploadFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_file_upload, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
//        initData();
    }

    private void init(View view){
        tv_file_path = view.findViewById(R.id.tv_file_path);
        tv_progress = view.findViewById(R.id.tv_progress);
        btn_select = view.findViewById(R.id.btn_select);
        btn_ota = view.findViewById(R.id.btn_ota);
        btn_stop = view.findViewById(R.id.btn_stop);
        btn_send_dial = view.findViewById(R.id.btn_send_dial);
        btn_send_file = view.findViewById(R.id.btn_send_file);
        btn_send_js_app = view.findViewById(R.id.btn_send_js_app);
        btn_send_ebook = view.findViewById(R.id.btn_send_ebook);
        btn_send_music = view.findViewById(R.id.btn_send_music);
        btn_send_video = view.findViewById(R.id.btn_send_video);
        btn_send_ringtone_msg = view.findViewById(R.id.btn_send_ringtone_msg);
        btn_send_ringtone_call = view.findViewById(R.id.btn_send_ringtone_call);
        btn_send_ringtone_clock = view.findViewById(R.id.btn_send_ringtone_clock);
        btn_send_image = view.findViewById(R.id.btn_send_image);
        spinner = view.findViewById(R.id.spinner_side_path);
        horizontalProgressBar = view.findViewById(R.id.horizontalProgressBar);
        path = getPath();

        btn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });

        String[] options = getResources().getStringArray(R.array.haisi_board_side_path_array);

        // 创建 ArrayAdapter 适配器，将数组与 Spinner 绑定
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 设置适配器
        spinner.setAdapter(adapter);

        // 设置选中项的监听器
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 处理选中项的逻辑
                uploadPath = options[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 在没有选中项的情况下执行的逻辑
            }
        });

        HiSiliconFileTransferUtils.getInstance().setHiSiliconFileTransferListener(new HiSiliconFileTransferUtils.HiSiliconFileTransferListener() {
            @Override
            public void onProgressChanged(long curFrames, long framesCount, int fileListIndex, int fileSize) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int progress = (int)(curFrames*100/framesCount);
                        tv_progress.setText(progress+"%");
                        horizontalProgressBar.setProgress(progress);
                    }
                });
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onTimeOut() {

            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onTransmitting() {

            }
        });

        btn_ota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                clear();
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择bin文件");
                }else{
//                    startTime = System.currentTimeMillis();
//                    otaType = FissionConstant.OTA_TYPE_FIRMWARE;
//                    FissionSdkBleManage.getInstance().setSwitchHighCh(true);
//                    FissionSdkBleManage.getInstance().checkOTA(String.valueOf(FissionConstant.OTA_TYPE_FIRMWARE));
                    sendOtaCmd(FissionConstant.OTA_TYPE_FIRMWARE);
                }
            }
        });

        btn_send_dial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                clear();
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择bin文件");
                }else{
//                    startTime = System.currentTimeMillis();
//                    otaType = FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL;
//                    FissionSdkBleManage.getInstance().setSwitchHighCh(true);
//                    FissionSdkBleManage.getInstance().checkOTA(String.valueOf(FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL));
                    sendOtaCmd(FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL);
                }
            }
        });

        btn_send_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择bin文件");
                }else{
//                    sendFile();
                    HiSiliconFileTransferUtils.getInstance().init();
                    HiSiliconFileTransferUtils.getInstance().sendFile(filePath,-1, uploadPath);
                }
            }
        });

        btn_send_js_app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择bin文件");
                }else{
                    File file = new File(filePath);
                    HsJsFileInfo hsJsFileInfo = new HsJsFileInfo();
                    hsJsFileInfo.setPageName(file.getName().replace(".bin", "").trim());
                    hsJsFileInfo.setSize((int)file.length());
                    hsJsFileInfo.setVersion("V1.0.0");
                    hsJsFileInfo.setTimestampId((int)(System.currentTimeMillis()/1000));
                    FissionSdkBleManage.getInstance().pushJsApp(filePath, hsJsFileInfo);
                }
            }
        });

        btn_send_ebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择txt文件");
                }else{
                    File file = new File(filePath);
                    HsMultimediaFileInfo hsMultimediaFileInfo = new HsMultimediaFileInfo();
                    hsMultimediaFileInfo.setFileName(file.getName());
                    hsMultimediaFileInfo.setFileSize((int)file.length());
                    hsMultimediaFileInfo.setResId("1001");
                    hsMultimediaFileInfo.setTimestampId(System.currentTimeMillis()/1000);
                    FissionSdkBleManage.getInstance().pushMultimediaFile(filePath, FissionConstant.OTA_TYPE_EBOOK, hsMultimediaFileInfo);
                }
            }
        });

        btn_send_music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择mp3文件");
                }else{
                    File file = new File(filePath);
                    HsMultimediaFileInfo hsMultimediaFileInfo = new HsMultimediaFileInfo();
                    hsMultimediaFileInfo.setFileName(file.getName());
                    hsMultimediaFileInfo.setFileSize((int)file.length());
                    hsMultimediaFileInfo.setResId("1002");
                    hsMultimediaFileInfo.setTimestampId(System.currentTimeMillis()/1000);
                    FissionSdkBleManage.getInstance().pushMultimediaFile(filePath, FissionConstant.OTA_TYPE_MUSIC, hsMultimediaFileInfo);
                }
            }
        });

        btn_send_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择mp4文件");
                }else{
                    File file = new File(filePath);
                    HsMultimediaFileInfo hsMultimediaFileInfo = new HsMultimediaFileInfo();
                    hsMultimediaFileInfo.setFileName(file.getName());
                    hsMultimediaFileInfo.setFileSize((int)file.length());
                    hsMultimediaFileInfo.setResId("1003");
                    hsMultimediaFileInfo.setTimestampId(System.currentTimeMillis()/1000);
                    FissionSdkBleManage.getInstance().pushMultimediaFile(filePath, FissionConstant.OTA_TYPE_VIDEO, hsMultimediaFileInfo);
                }
            }
        });

        btn_send_ringtone_msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择消息铃声mp3文件");
                }else{
                    File file = new File(filePath);
                    HsMultimediaFileInfo hsMultimediaFileInfo = new HsMultimediaFileInfo();
                    hsMultimediaFileInfo.setFileName(file.getName());
                    hsMultimediaFileInfo.setFileSize((int)file.length());
                    hsMultimediaFileInfo.setResId("1004");
                    hsMultimediaFileInfo.setTimestampId(System.currentTimeMillis()/1000);
                    FissionSdkBleManage.getInstance().pushMultimediaFile(filePath, FissionConstant.OTA_TYPE_RINGTONE_SETTING_MSG, hsMultimediaFileInfo);
                }
            }
        });

        btn_send_ringtone_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择来电铃声mp3文件");
                }else{
                    File file = new File(filePath);
                    HsMultimediaFileInfo hsMultimediaFileInfo = new HsMultimediaFileInfo();
                    hsMultimediaFileInfo.setFileName(file.getName());
                    hsMultimediaFileInfo.setFileSize((int)file.length());
                    hsMultimediaFileInfo.setResId("1005");
                    hsMultimediaFileInfo.setTimestampId(System.currentTimeMillis()/1000);
                    FissionSdkBleManage.getInstance().pushMultimediaFile(filePath, FissionConstant.OTA_TYPE_RINGTONE_SETTING_CALL, hsMultimediaFileInfo);
                }
            }
        });

        btn_send_ringtone_clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择闹钟铃声mp3文件");
                }else{
                    File file = new File(filePath);
                    HsMultimediaFileInfo hsMultimediaFileInfo = new HsMultimediaFileInfo();
                    hsMultimediaFileInfo.setFileName(file.getName());
                    hsMultimediaFileInfo.setFileSize((int)file.length());
                    hsMultimediaFileInfo.setResId("1006");
                    hsMultimediaFileInfo.setTimestampId(System.currentTimeMillis()/1000);
                    FissionSdkBleManage.getInstance().pushMultimediaFile(filePath, FissionConstant.OTA_TYPE_RINGTONE_SETTING_CLOCK, hsMultimediaFileInfo);
                }
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HiSiliconFileTransferUtils.getInstance().transferStop();
            }
        });

        btn_send_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (filePath.equals("")){
                    ToastUtils.showShort("还未选择图片文件");
                }else{
                    byte[] imageData = FileIOUtils.readFile2BytesByStream(filePath);
                    Bitmap bitmap = ConvertUtils.bytes2Bitmap(imageData);
                    String binFilePath = Environment.getExternalStorageDirectory()+"/Photo_"+ "L"+ imageData.length + "_" +System.currentTimeMillis()/1000+".bin";
                    HsDialUtils.getInstance().image2ImageBinFile(bitmap, binFilePath);
                    FissionSdkBleManage.getInstance().pushImageFile(binFilePath, FissionConstant.OTA_TYPE_IMAGE_FILE);
                }
            }
        });
    }

//    private void initData(){
//        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionAtCmdResultListener() {
//            @Override
//            public void sendSuccess(String cmdId) {
//
//            }
//
//            @Override
//            public void sendFail(String cmdId) {
//
//            }
//
//            @Override
//            public void onResultTimeout(String cmdId) {
//
//            }
//
//            @Override
//            public void onResultError(String errorMsg) {
//
//            }
//
//            @Override
//            public void fssSuccess(FssStatus fssStatus) {
//                super.fssSuccess(fssStatus);
//                if(fssStatus.getFssType() == 23){
//                    horizontalProgressBar.setProgress(fssStatus.getFssStatus());
//                    tv_progress.setText(fssStatus.getFssStatus()+"%");
//                }else if(fssStatus.getFssType() == 25 && fssStatus.getFssStatus() == 1 && (otaType == FissionConstant.OTA_TYPE_FIRMWARE || otaType == FissionConstant.OTA_TYPE_DEFAULT_DYNAMIC_DIAL)){
//                    sendFile();
//                }
//            }
//
//        });
//        FissionSdkBleManage.getInstance().addCmdResultListener(new HiSiliconDataResultListener() {
//            @Override
//            public void sendSuccess(String cmdId) {
//
//            }
//
//            @Override
//            public void sendFail(String cmdId) {
//
//            }
//
//            @Override
//            public void onResultTimeout(String cmdId) {
//
//            }
//
//            @Override
//            public void onResultError(String errorMsg) {
//
//            }
//
//            @Override
//            public void transferRequest(TransferRequest request) {
//                super.transferRequest(request);
//                    if(curFrames < framesCount){
//                        int num = dataBlockNum;
//                        if(curFrames +1 == framesCount){
//                            num = dataBlockNumCount - curFrames * dataBlockNum;
//                        }else if(framesCount == 1){
//                            num = dataBlockNumCount;
//                        }
//                        for(int i =0; i< num; i++){
//                            String dataHex = fileDataHexList.get(curFrames*dataBlockNum+i);
//                            TransferResponse response = new TransferResponse();
//                            response.setTransmitId(2);
//                            response.setOffset(curFrames * dataBlockSize * dataBlockNum + (i * dataBlockSize));
//                            response.setSize(dataHex.length()/2);
//                            response.setData(dataHex);
//                            FissionSdkBleManage.getInstance().transferResponse(response);
//                            LogUtils.d("wl", "文件上传， 当前上传第"+curFrames+"帧， 第"+i+"包数据。");
//                        }
//                        curFrames++;
//                    }
//            }
//
//            @Override
//            public void transferNotify(TransferNotify notify) {
//                super.transferNotify(notify);
//                if(notify.getNotifyCode() == HiSiliconSppCmdID.COMMAND_ID_NOTIFY_FINISH){
//                    clear();
//                    LogUtils.d("wl", "OTA文件传输完毕，耗时："+(System.currentTimeMillis() - startTime)/1000+"s");
//                    ToastUtils.showLong("文件传输完毕，耗时："+(System.currentTimeMillis() - startTime)/1000+"s");
//                    FissionSdkBleManage.getInstance().checkOTA(String.valueOf(FissionConstant.OTA_TYPE_FIRMWARE_INSTALL));
//                }
//            }
//        });
//    }
//
//    private void sendFile(){
//        resultData = FileIOUtils.readFile2BytesByStream(filePath);
//        fileSize = resultData.length;
//        fileName =  new File(filePath).getName();
//        fileDataHexList = new ArrayList<>();
//        if(fileSize % (dataBlockNum * dataBlockSize)!=0){
//            framesCount = fileSize / (dataBlockNum * dataBlockSize) +1;
//        }else{
//            framesCount = fileSize/ (dataBlockNum * dataBlockSize);
//        }
//        String dataHex = ConvertUtils.bytes2HexString(resultData);
//        if(fileSize % dataBlockSize!=0){
//            dataBlockNumCount = fileSize / dataBlockSize + 1;
//            for(int i =0; i< dataBlockNumCount; i++){
//                String data;
//                if(i == dataBlockNumCount-1){
//                    data = dataHex.substring(i*dataBlockSize*2);
//                }else{
//                    data = dataHex.substring(i*dataBlockSize*2, (i+1)*dataBlockSize*2);
//                }
//                fileDataHexList.add(data);
//            }
//        }else{
//            dataBlockNumCount = fileSize / dataBlockSize;
//            for(int i =0; i< dataBlockNumCount; i++){
//                String data = dataHex.substring(i*dataBlockSize*2, (i+1)*dataBlockSize*2);
//                fileDataHexList.add(data);
//            }
//        }
//
//        TransferParameterRequest request = new TransferParameterRequest();
//        request.setTransmitId(2);
//        request.setTransferParameterInfo(TransferParameterRequest.UPLOAD_FILE);
//        request.setTotalSize(fileSize);
//        request.setDataBlockNum(dataBlockNum);
//        request.setDataBlockSize(dataBlockSize);
//        request.setFileName(uploadPath+fileName+"\0");
//        FissionSdkBleManage.getInstance().parameterNegotiation(request);
//        LogUtils.d("wl", "当前上传文件信息："+"， 数据总帧数："+framesCount+"，数据总包数："+dataBlockNumCount);
//    }

    private void selectFile(){
        if (Build.VERSION.SDK_INT >= 30 ){
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                Uri uri = Uri.parse(path);
                intent.setDataAndType(uri, "*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + requireActivity().getApplication().getPackageName()));
                startActivity(intent);
            }
        }else{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(path);
            intent.setDataAndType(uri, "*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 1);
        }
    }
    public String getPath() {
        File dir = null;
        boolean state = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (state) {
            if (Build.VERSION.SDK_INT >= 28) {
                //Android10之后
                dir = requireActivity().getExternalFilesDir(null);
            } else {
                dir = Environment.getExternalStorageDirectory();
            }
        } else {
            dir = Environment.getRootDirectory();
        }
        return dir.toString();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                filePath = UriUtils.uri2File(uri).getAbsolutePath();
                tv_file_path.setText("选择文件路径:"+filePath);
                LogUtils.d("获取文件路径getData",filePath);
            }
        }
    }

//    private void clear(){
//        curFrames =0;
//        resultData = null;
//        if(fileDataHexList!=null){
//            fileDataHexList.clear();
//        }
//    }

    private void sendOtaCmd(int otaType){
        FissionSdkBleManage.getInstance().startDfu(getContext(), filePath, otaType, new DfuAdapter.DfuHelperCallback() {
            @Override
            public void onStateChanged(int i) {
                super.onStateChanged(i);
            }

            @Override
            public void onTargetInfoChanged(OtaDeviceInfo otaDeviceInfo) {
                super.onTargetInfoChanged(otaDeviceInfo);
            }

            @Override
            public void onError(int i, int i1) {
                super.onError(i, i1);
            }

            @Override
            public void onProcessStateChanged(int i, Throughput throughput) {
                super.onProcessStateChanged(i, throughput);
            }

            @Override
            public void onProgressChanged(DfuProgressInfo dfuProgressInfo) {
                super.onProgressChanged(dfuProgressInfo);
            }
        });
    }
}
