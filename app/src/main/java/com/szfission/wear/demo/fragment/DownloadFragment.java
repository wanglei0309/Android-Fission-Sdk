package com.szfission.wear.demo.fragment;

/**
 * describe:
 * author: wl
 * createTime: 2023/11/25
 */
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.FileInfo;
import com.fission.wear.sdk.v2.bean.HiSiWatchReqTask;
import com.fission.wear.sdk.v2.bean.TransferNotify;
import com.fission.wear.sdk.v2.bean.TransferParameterRequest;
import com.fission.wear.sdk.v2.bean.TransferParameterResponse;
import com.fission.wear.sdk.v2.bean.TransferRequest;
import com.fission.wear.sdk.v2.bean.TransferResponse;
import com.fission.wear.sdk.v2.callback.HiSiliconDataResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.parse.HiSiliconSppCmdHelper;
import com.fission.wear.sdk.v2.parse.HiSiliconSppCmdID;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.HiSiDownloadFileUtil;
import com.fission.wear.sdk.v2.utils.HiSiTaskManage;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.dialogx.interfaces.OnBackPressedListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.util.RxTimerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class DownloadFragment extends Fragment {

    Spinner spinner_side_path, spinner_file_path;

    Button btn_inquire, btn_start, btn_download_all, btn_delete, btn_delete_all, btn_download_all_new;

    private EditText ed_file_directory;

    private ProgressBar horizontalProgressBar;

    private TextView tv_progress;

    private String sidePath;

    private List<String> fileNames;

    private List<FileInfo> fileInfos;

    private List<FileInfo> downloadFileList;

    private List<String> fileDirectoryList;

    private int chooseIndex;

    private int framesCount =0; //文件数据分帧

    private int curFrames = 0; //当前帧数

    private int dataBlockNum = 14;


    private int dataBlockSize = 965;

    private int transferResponseNum = 0;

    private int fileSize = 0;

    private String fileDate;

    private FileInfo fileInfo; //当前操作文件

    private boolean isBatchDownload = false;

    private boolean isDeleteAll = false;

    private int fileDirIndex = 0;

    private boolean isRecursion = false;

    private int transmitId =0;

    // 定义超时时间（以毫秒为单位）
    private static final int TIMEOUT_INTERVAL = 500; // 0.5 秒
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private int downloadedSize;

    private TreeMap<Integer, String> receivedDataMap = new TreeMap<>();
    private int expectedOffset = 0; // 期望的偏移值

    public DownloadFragment() {
        // Required empty public constructor
    }

    public static DownloadFragment newInstance() {
        DownloadFragment fragment = new DownloadFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_file_download, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
        initData();
    }

    private void init(View view){
        spinner_side_path = view.findViewById(R.id.spinner_side_path);
        spinner_file_path = view.findViewById(R.id.spinner_file_path);
        btn_inquire = view.findViewById(R.id.btn_inquire);
        btn_start = view.findViewById(R.id.btn_start);
        btn_download_all = view.findViewById(R.id.btn_download_all);
        btn_delete = view.findViewById(R.id.btn_delete);
        btn_delete_all = view.findViewById(R.id.btn_delete_all);
        horizontalProgressBar = view.findViewById(R.id.horizontalProgressBar);
        tv_progress = view.findViewById(R.id.tv_progress);
        btn_download_all_new =  view.findViewById(R.id.btn_download_all_new);
        ed_file_directory = view.findViewById(R.id.ed_file_directory);

        String[] options = getResources().getStringArray(R.array.haisi_board_side_path_array);

        // 创建 ArrayAdapter 适配器，将数组与 Spinner 绑定
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 设置适配器
        spinner_side_path.setAdapter(adapter);

        // 设置选中项的监听器
        spinner_side_path.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 处理选中项的逻辑
                sidePath = options[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 在没有选中项的情况下执行的逻辑
            }
        });

        spinner_file_path.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 处理选中项的逻辑
                chooseIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 在没有选中项的情况下执行的逻辑
            }
        });

        btn_inquire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBatchDownload = false;
                isRecursion = false;
                fileInfos.clear();
                fileNames.clear();
                downloadFileList.clear();
                fileDirectoryList.clear();
                fileDirIndex = 0;
                String dirPath = ed_file_directory.getText().toString().trim();
                if(!TextUtils.isEmpty(dirPath)){
                    sidePath = dirPath;
                }else{
                    sidePath = options[spinner_side_path.getSelectedItemPosition()];
                }
                FissionSdkBleManage.getInstance().getFileList(sidePath+"\0");
                WaitDialog.show("正在查询...")
                        .setCancelable(false)
                        .setOnBackPressedListener(new OnBackPressedListener<WaitDialog>() {
                            @Override
                            public boolean onBackPressed(WaitDialog dialog) {
                                return false;
                            }
                        });

                new RxTimerUtil().timer(3000, new RxTimerUtil.RxAction() {
                    @Override
                    public void action(long number) {
                        WaitDialog.dismiss();
                    }
                });
            }
        });

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBatchDownload = false;
                isRecursion = false;
                downloadStart();
            }
        });

        btn_download_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecursion = false;
                downloadAll();
            }
        });

        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDeleteAll =false;
                deleteFileStart();
            }
        });

        btn_delete_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAll();
            }
        });

        btn_download_all_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRecursion = true;

                fileInfos.clear();
                downloadFileList.clear();
                fileDirectoryList.clear();
                fileDirIndex = 0;
                String dirPath = ed_file_directory.getText().toString().trim();
                if(!TextUtils.isEmpty(dirPath)){
                    sidePath = dirPath;
                }else{
                    sidePath = options[spinner_side_path.getSelectedItemPosition()];
                }
                FissionSdkBleManage.getInstance().getFileList(sidePath+"\0");

            }
        });
    }

    private void initData(){
        fileNames = new ArrayList<>();
        fileInfos = new ArrayList<>();
        downloadFileList = new ArrayList<>();
        fileDirectoryList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_item, fileNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_file_path.setAdapter(adapter);
        FissionSdkBleManage.getInstance().addCmdResultListener(new HiSiliconDataResultListener() {
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
            public void getFileList(FileInfo fileInfo) {
                super.getFileList(fileInfo);
                String name = fileInfo.getFileName();
                if(isRecursion){
                    fileInfo.setFileName(sidePath+name);
                }
                fileInfos.add(fileInfo);
                if(fileInfo.getFileType() == 0){
                    fileNames.add(name+"（文件-size:"+fileInfo.getFileSize()+")");
                }else if(fileInfo.getFileType() == 1){
                    fileNames.add(name+"（文件夹）");
                }else if(fileInfo.getFileType() == 255 && isRecursion){
                    recursiveDownload(fileInfos);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void parameterNegotiation(TransferParameterResponse response) {
                super.parameterNegotiation(response);
                dataBlockNum = response.getDataBlockNum();
                dataBlockSize = response.getDataBlockSize();
                int size = dataBlockNum * dataBlockSize;
                TransferRequest request = new TransferRequest();
                request.setTransmitId(transmitId);
                request.settCnt(1);
                request.setOffset(0);
                request.setSize(size);
                FissionSdkBleManage.getInstance().transferRequest(request);
                LogUtils.d("wl", "传输参数协商成功，开始下载文件请求");
            }

            @Override
            public void transferResponse(TransferResponse response) {
                super.transferResponse(response);
                try {
                    // 每次收到响应时，重置超时计时器
                    startTimeoutTimer();

                    // 检查 transmitId 是否一致
                    if (transmitId != response.getTransmitId()) {
                        FissionLogUtils.d("wl", "---------transferResponse--------transmitId不一致，不需要响应。 文件协商id:"
                                + transmitId + ", 固件请求id:" + response.getTransmitId());
                        return;
                    }

//                if(response.getOffset()!=downloadedSize){
//                    FissionLogUtils.d("wl", "当前接收到的数据，不是等待Offset的数据："+response.getOffset()+", 已下载数据："+downloadedSize);
//                    return;
//                }

                    // 存储数据，保证有序
                    receivedDataMap.put(response.getOffset(), response.getData());

                    // 尝试拼接数据（保证顺序）
                    while (receivedDataMap.containsKey(expectedOffset)) {
                        String tempData = receivedDataMap.remove(expectedOffset);
                        FissionLogUtils.d("wl", "根据偏移值有序处理接收到的数据, 偏移值："+expectedOffset+", 对应的数据："+tempData);
                        fileDate += tempData; // 取出数据块并拼接
                        expectedOffset = fileDate.length() / 2; // 计算下一个期望的偏移值

                        transferResponseNum++;
                        if(transferResponseNum == 14){
                            break;
                        }
                    }

                    downloadedSize = fileDate.length() / 2; // 当前已下载文件大小
                    int totalFileSize = fileSize;
                    int progress = downloadedSize * 100 / totalFileSize;

                    tv_progress.setText(progress+"%");
                    horizontalProgressBar.setProgress(progress);

                    // 每帧数据处理逻辑
                    if (transferResponseNum == 14) {
                        transferResponseNum = 0;

                        if (curFrames < framesCount - 1) {
                            curFrames++;

                            // 计算当前帧的大小
                            int frameSize = dataBlockNum * dataBlockSize;
                            if (curFrames == framesCount - 1) {
                                frameSize = fileSize - curFrames * frameSize; // 最后一帧可能小于标准大小
                            }

                            // 发起新的帧传输请求
                            TransferRequest request = new TransferRequest();
                            request.setTransmitId(transmitId);
                            request.settCnt(1);
                            request.setOffset(expectedOffset);
                            request.setSize(frameSize);
                            FissionSdkBleManage.getInstance().transferRequest(request);

                            // 重启超时计时器
                            startTimeoutTimer();
                        } else {
                            // 所有帧传输完成
                            downloadFinish();
                            stopTimeoutTimer(); // 停止计时器
                        }
                    } else {
                        // 检查是否最后一帧已下载完成
                        if (curFrames + 1 == framesCount && fileSize == downloadedSize) {
                            downloadFinish();
                            stopTimeoutTimer(); // 停止计时器
                        }
                    }
                    LogUtils.d("wl", "传输请求响应，第几包数据："+transferResponseNum+"，第几帧数据："+curFrames+"，文件大小："+fileSize+", 已下载文件大小："+fileDate.length()/2);
                } catch (Exception e) {
                    FissionSdkBleManage.getInstance().transferNotify(new TransferNotify(transmitId, HiSiliconSppCmdID.COMMAND_ID_NOTIFY_FINISH, 0, ""));
                    FissionLogUtils.d("wl", "文件下载传输响应异常："+e.getMessage());
                }
            }

            @Override
            public void transferNotify(TransferNotify notify) {
                super.transferNotify(notify);
                if(notify.getNotifyCode() == HiSiliconSppCmdID.COMMAND_ID_NOTIFY_FINISH){
                    fileDate = "";
                    transferResponseNum = 0;
                    FissionLogUtils.d("wl", "板侧通知传输结束");
                }
            }

            @Override
            public void deleteFile(boolean isOk) {
                super.deleteFile(isOk);
                if(isOk){
                    if(isDeleteAll){
                        chooseIndex++;
                        if(chooseIndex < fileInfos.size()){
                            deleteFileStart();
                        }else{
                            ToastUtils.showLong("删除成功");
                            chooseIndex = 0;
                            fileInfos.clear();
                            fileNames.clear();
                            adapter.notifyDataSetChanged();
                        }
                    }else{
                        ToastUtils.showLong("删除成功");
                    }
                }
            }
        });
    }

    private void downloadFinish(){
        FissionSdkBleManage.getInstance().transferNotify(new TransferNotify(transmitId, HiSiliconSppCmdID.COMMAND_ID_NOTIFY_FINISH, 0, ""));
        if(fileInfo!=null){
            String filePath = Environment.getExternalStorageDirectory()+"/hs/"+fileInfo.getFileName();
            if(isRecursion){
                filePath = Environment.getExternalStorageDirectory()+"/hs_recursion/"+fileInfo.getFileName();
            }
            FileUtils.createFileByDeleteOldFile(filePath);
            FileIOUtils.writeFileFromBytesByStream (filePath, ConvertUtils.hexString2Bytes(fileDate));
            ToastUtils.showLong(fileInfo.getFileName()+"文件下载成功， 存储目录："+filePath);
            FissionLogUtils.d("wl", fileInfo.getFileName()+"文件下载成功， 存储目录："+filePath);

            if(isBatchDownload && chooseIndex < fileInfos.size()-1){
                chooseIndex++;
                downloadStart();
            }
            tv_progress.setText("100%");
            horizontalProgressBar.setProgress(100);
        }else{
            FissionLogUtils.d("wl", "---fileInfo is null---");
        }
    }

    private void downloadStart(){
        fileInfo = fileInfos.get(chooseIndex);
        downloadStart(fileInfo);
    }

    private void downloadStart(FileInfo file){
        tv_progress.setText(0+"%");
        horizontalProgressBar.setProgress(0);
        transferResponseNum = 0;
        curFrames = 0;
        fileDate = "";
        fileInfo = file;
        if(fileInfo.getFileType() == 0){
            fileSize = fileInfo.getFileSize();
            if(fileSize % (dataBlockNum * dataBlockSize)!=0){
                framesCount = fileInfo.getFileSize() / (dataBlockNum * dataBlockSize) +1;
            }else{
                framesCount = fileInfo.getFileSize() / (dataBlockNum * dataBlockSize);
            }
            long timestamp = System.currentTimeMillis(); // 获取当前毫秒时间戳
            transmitId = (int) (timestamp & 0xFFFFFFFF); // 取低32位
            if (transmitId < 0) {
                transmitId = Math.abs(transmitId); // 确保为正数
            }
            TransferParameterRequest request = new TransferParameterRequest();
            request.setTransmitId(transmitId);
            request.setTransferParameterInfo(TransferParameterRequest.DOWNLOAD_FILE);
            request.setTotalSize(fileSize);
            request.setDataBlockNum(dataBlockNum);
            request.setDataBlockSize(dataBlockSize);
            if(isRecursion){
                request.setFileName(fileInfo.getFileName()+"\0");
            }else{
                request.setFileName(sidePath+fileInfo.getFileName()+"\0");
            }
            FissionSdkBleManage.getInstance().parameterNegotiation(request);
            LogUtils.d("wl", "批量---当前下载文件大小："+fileSize+"，总帧数："+framesCount);
        }else{
            ToastUtils.showLong("请选择文件，而不是文件夹。");
        }
    }

    private void downloadAll(){
        if(fileInfos!=null && fileInfos.size()>0){
            isBatchDownload = true;
            chooseIndex = 0;
            downloadStart();
        }else{
            ToastUtils.showLong("文件列表不能为空");
        }
    }

    private void deleteAll(){
        if(fileInfos!=null && fileInfos.size()>0){
            isDeleteAll = true;
            chooseIndex = 0;
            deleteFileStart();
        }else{
            ToastUtils.showLong("文件列表不能为空");
        }
    }

    private void deleteFileStart(){
        fileInfo = fileInfos.get(chooseIndex);
        String filePath = sidePath+fileInfo.getFileName()+"\0";
        FissionSdkBleManage.getInstance().deleteFile(filePath, fileInfo.getFileType());
    }

    private void recursiveDownload(List<FileInfo> fileInfos){
        for(FileInfo file: fileInfos){
            if(file.getFileType() == 0){
                downloadFileList.add(file);
            }else if(file.getFileType() == 1){
                String fileDirectory = file.getFileName()+"/";
                fileDirectoryList.add(fileDirectory);
                FissionLogUtils.d("wl", "递归查询文件夹列表："+fileDirectoryList);
            }
        }
        FissionLogUtils.d("wl", "递归下载所有文件列表："+downloadFileList);
        queryFileList();
    }

    private void queryFileList(){
        if(fileDirectoryList.size()>0){
            if(fileDirIndex < fileDirectoryList.size()){
                fileInfos.clear();
                fileNames.clear();
                sidePath = fileDirectoryList.get(fileDirIndex);
                FissionSdkBleManage.getInstance().getFileList(sidePath+"\0");
                fileDirIndex++;
            }else{
                fileInfos.clear();
                fileInfos.addAll(downloadFileList);
                downloadAll();
                FissionLogUtils.d("wl", "递归下载所有文件开始1111");
            }
        }else{
            fileInfos.clear();
            fileInfos.addAll(downloadFileList);
            downloadAll();
            FissionLogUtils.d("wl", "递归下载所有文件开始2222");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(fileInfos!=null){
            fileInfos.clear();
        }
        if(fileNames!=null){
            fileNames.clear();
        }
        if(downloadFileList!=null){
            downloadFileList.clear();
        }
        if(fileDirectoryList!=null){
            fileDirectoryList.clear();
        }
        isRecursion = false;
        isBatchDownload = false;
        isDeleteAll = false;
        chooseIndex = 0;
        transferResponseNum = 0;
        curFrames = 0;
        fileDate = "";
        sidePath = "";
        FissionLogUtils.d("wl", "退出海思文件下载界面，释放资源");
    }

    // 定义重新发起传输请求的方法
    private void retryTransferRequest() {
        if(BluetoothAdapter.getDefaultAdapter().isEnabled() && FissionSdkBleManage.getInstance().isSppConnect()){
            if(fileSize == downloadedSize){
                return;
            }
            TransferRequest request = new TransferRequest();
            request.setTransmitId(transmitId);
            request.settCnt(1);
            request.setOffset(downloadedSize);
//            int size = dataBlockNum * dataBlockSize;
//            curFrames = downloadedSize/size;
//            if (curFrames == framesCount - 1) {
//                size = fileSize - curFrames * size; // 最后一帧的大小
//            }
            request.setSize(dataBlockSize);
            FissionSdkBleManage.getInstance().transferRequest(request);
            FissionLogUtils.d("wl", "重新发起传输请求，curFrames：" + curFrames + ", size：" + fileSize+", Offset:"+downloadedSize);

            // 重新启动超时计时器
            startTimeoutTimer();
        }else{
            FissionLogUtils.d("wl", "spp未连接，不重新发起请求。");
        }
    }

    // 定义方法：初始化超时机制
    private void initTimeoutMechanism() {
        if (timeoutHandler == null) {
            timeoutHandler = new Handler(Looper.getMainLooper());
        }
        if (timeoutRunnable == null) {
            timeoutRunnable = () -> {
                FissionLogUtils.d("wl", "transferResponse 超时，重新发起请求");
                retryTransferRequest();
            };
        }
    }

    // 调用超时机制：每次回调或开始传输时调用
    private void startTimeoutTimer() {
        initTimeoutMechanism();
        timeoutHandler.removeCallbacks(timeoutRunnable); // 清除之前的计时
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_INTERVAL); // 启动新计时
    }

    // 停止超时计时器：下载完成时调用
    private void stopTimeoutTimer() {
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }
}

