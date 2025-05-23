package com.szfission.wear.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.constant.SpKey;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.HiSiDownloadFileUtil;
import com.fission.wear.sdk.v2.utils.HsDialUtils;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.adapter.CustomDividerItemDecoration;
import com.szfission.wear.demo.adapter.HiSiliconFunctionAdapter;
import com.szfission.wear.sdk.bean.AppMessageBean;
import com.szfission.wear.sdk.bean.HardWareInfo;
import com.szfission.wear.sdk.util.FsLogUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Arrays;

public class HiSiliconTestActivity extends BaseActivity {
    RecyclerView recyclerView;
    TextView tv_name;

    TextView tv_mac;

    TextView tv_state;

    TextView tv_version;

    private HiSiliconFunctionAdapter adapter;

    private int num = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HsDialUtils.getInstance().init();
        setContentView(R.layout.activity_haisi_test);
        setTitle(R.string.FUNC_HAISI_TEST);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        tv_name = findViewById(R.id.tv_name);
        tv_mac = findViewById(R.id.tv_mac);
        tv_state = findViewById(R.id.tv_state);
        tv_version = findViewById(R.id.tv_version);

        init();
    }

    private void init(){
        SPUtils.getInstance().put(SpKey.CHIP_CHANNEL_TYPE, HardWareInfo.CHANNEL_TYPE_HS);

        String[] testArray = getResources().getStringArray(R.array.haisi_test_array);

        adapter = new HiSiliconFunctionAdapter();
        recyclerView.addItemDecoration(new CustomDividerItemDecoration(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter.setList(Arrays.asList(testArray));

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionAtCmdResultListener() {
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
            public void setADS(String ads) {
                super.setADS(ads);
                if("0".equals(ads)){
                    ToastUtils.showShort("关闭音频数据保存开关成功");
                }else if("1".equals(ads)){
                    ToastUtils.showShort("开启ai节点数据保存成功");
                }else if("2".equals(ads)){
                    ToastUtils.showShort("开启sea节点数据保存成功");
                }
            }

            @Override
            public void checkOTA(String otaType) {
                super.checkOTA(otaType);
                if(Integer.parseInt(otaType) == FissionConstant.OTA_TYPE_DOWNLOAD_FINISH){
                    num++;
                    String savePath = Environment.getExternalStorageDirectory()+"/hs";
                    FissionSdkBleManage.getInstance().downloadHiSiLogFiles(savePath);
                    ToastUtils.showShort("下载日志成功："+num+"次");
                    FissionLogUtils.d("wl", "下载日志成功："+num);
                }
            }
        });

        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                Intent intent;
                switch (position){
                    case 0:
                        intent = new Intent(HiSiliconTestActivity.this, FileTransferActivity.class);
                        startActivity(intent);
                        break;

                    case 1:
                        intent = new Intent(HiSiliconTestActivity.this, BaiduMapManageActivity.class);
                        startActivity(intent);
                        break;

                    case 2:
                        intent = new Intent(HiSiliconTestActivity.this, BaiDuAiTestActivity.class);
                        startActivity(intent);
                        break;

                    case 3:
                        intent = new Intent(HiSiliconTestActivity.this, PushMoreGpsDataActivity.class);
                        startActivity(intent);
                        break;

                    case 4:
                        intent = new Intent(HiSiliconTestActivity.this, HsCustomDialActivity.class);
                        startActivity(intent);
                        break;

                    case 5:
                        FissionSdkBleManage.getInstance().sendNetworkStatus("1");
//                        FissionSdkBleManage.getInstance().synTimes((int)(System.currentTimeMillis()/1000));
                        break;

                    case 6:
//                        HiSiDownloadFileUtil.getInstance().init();
//                        String savePath = Environment.getExternalStorageDirectory()+"/hs/";
//                        HiSiDownloadFileUtil.getInstance().setSaveFilePath("");
//                        HiSiDownloadFileUtil.getInstance().setHiSiDownloadFileListener(new HiSiDownloadFileUtil.HiSiDownloadFileListener() {
//                            @Override
//                            public void onProgressChanged(int currentFileIndex, int fileNum, int progress) {
//                                FissionLogUtils.d("wl", "日志文件下载信息， 第"+currentFileIndex+"个文件， 总共"+fileNum+"个文件， 当前文件进度："+progress+"%" );
//
//                                ToastUtils.showLong("日志文件下载信息， 第"+currentFileIndex+"个文件， 总共"+fileNum+"个文件， 当前文件进度："+progress+"%" );
//                            }
//
//                            @Override
//                            public void onComplete() {
//                                FissionLogUtils.d("wl", "日志下载完成");
//                                ToastUtils.showLong("日志下载完成");
//                            }
//
//                            @Override
//                            public void onTimeOut() {
//
//                            }
//
//                            @Override
//                            public void onError(Exception e) {
//
//                            }
//
//                            @Override
//                            public void onTransmitting() {
//
//                            }
//                        });
//
//                        HiSiDownloadFileUtil.getInstance().recursionDownloadAllLog();
                        String savePath = Environment.getExternalStorageDirectory()+"/hs";
                        FissionSdkBleManage.getInstance().downloadHiSiLogFiles(savePath);
                        break;

                    case 7:
                        FissionSdkBleManage.getInstance().setADS("0");
                        break;

                    case 8:
                        FissionSdkBleManage.getInstance().setADS("1");
                        break;

                    case 9:
                        FissionSdkBleManage.getInstance().setADS("2");
                        break;
                }
            }
        });
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
}
