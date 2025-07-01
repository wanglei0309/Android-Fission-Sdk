package com.szfission.wear.demo.activity;

import static com.szfission.wear.sdk.parse.BigDataCmdID.CMD_ID_ST_NOTES_REMINDERS;

import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.HsDialInfo;
import com.fission.wear.sdk.v2.bean.HsJsFileInfo;
import com.fission.wear.sdk.v2.bean.HsMultimediaFileInfo;
import com.fission.wear.sdk.v2.bean.MeetingFileInfo;
import com.fission.wear.sdk.v2.bean.RingtoneSetting;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.utils.HiSiDownloadFileUtil;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.AppMessageBean;
import com.szfission.wear.sdk.util.FsLogUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.util.List;

public class FileListActivity extends BaseActivity {
    TextView tv_dial_list;

    TextView tv_js_list, tv_media_list;

    Button btn_get_dial, btn_get_js, btn_delete_dial, btn_delete_js, btn_get_ebook, btn_get_music, btn_get_video, btn_delete_ebook, btn_delete_music, btn_delete_video, btn_get_ringtone_msg,  btn_get_ringtone_call, btn_get_ringtone_clock;

    Button btn_delete_ringtone_msg, btn_delete_ringtone_call, btn_delete_ringtone_clock, btn_get_ringtone_setting, btn_meeting_file, btn_download_meeting_file;

    private List<HsJsFileInfo> hsJsFileInfos;

    private List<HsDialInfo> hsDialInfos;

    private List<HsMultimediaFileInfo> hsMultimediaFileInfos;

    private List<MeetingFileInfo> meetingFileInfos;

    private int type;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list_hs);
        setTitle(R.string.FUNC_GET_HS_FILE_LIST);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        tv_dial_list = findViewById(R.id.tv_dial_list);
        tv_js_list = findViewById(R.id.tv_js_list);
        tv_media_list = findViewById(R.id.tv_media_list);
        btn_get_dial = findViewById(R.id.btn_get_dial);
        btn_get_js = findViewById(R.id.btn_get_js);
        btn_delete_dial = findViewById(R.id.btn_delete_dial);
        btn_delete_js = findViewById(R.id.btn_delete_js);
        btn_get_ebook = findViewById(R.id.btn_get_ebook);
        btn_get_music = findViewById(R.id.btn_get_music);
        btn_get_video = findViewById(R.id.btn_get_video);
        btn_delete_ebook = findViewById(R.id.btn_delete_ebook);
        btn_delete_music = findViewById(R.id.btn_delete_music);
        btn_delete_video = findViewById(R.id.btn_delete_video);
        btn_get_ringtone_msg = findViewById(R.id.btn_get_ringtone_msg);
        btn_get_ringtone_call = findViewById(R.id.btn_get_ringtone_call);
        btn_get_ringtone_clock = findViewById(R.id.btn_get_ringtone_clock);
        btn_delete_ringtone_msg = findViewById(R.id.btn_delete_ringtone_msg);
        btn_delete_ringtone_call = findViewById(R.id.btn_delete_ringtone_call);
        btn_delete_ringtone_clock = findViewById(R.id.btn_delete_ringtone_clock);
        btn_get_ringtone_setting = findViewById(R.id.btn_get_ringtone_setting);
        btn_meeting_file = findViewById(R.id.btn_meeting_file);
        btn_download_meeting_file = findViewById(R.id.btn_download_meeting_file);

        btn_get_dial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialList();
            }
        });

        btn_get_js.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getJsList();
            }
        });

        btn_get_ebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = FissionConstant.EBOOK_FILE_LIST;
                getMultimediaFileInfoList(type);
            }
        });

        btn_get_music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = FissionConstant.MUSIC_FILE_LIST;
                getMultimediaFileInfoList(type);
            }
        });

        btn_get_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = FissionConstant.VIDEO_FILE_LIST;
                getMultimediaFileInfoList(type);
            }
        });

        btn_get_ringtone_msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = FissionConstant.RINGTONE_SETTING_MSG;
                getMultimediaFileInfoList(type);
            }
        });

        btn_get_ringtone_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = FissionConstant.RINGTONE_SETTING_CALL;
                getMultimediaFileInfoList(type);
            }
        });

        btn_get_ringtone_clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = FissionConstant.RINGTONE_SETTING_CLOCK;
                getMultimediaFileInfoList(type);
            }
        });


        btn_delete_dial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                deleteDialList();
            }
        });

        btn_delete_js.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                deleteJsList();
            }
        });

        btn_delete_ebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type == FissionConstant.EBOOK_FILE_LIST){
                    deleteMultimediaFileInfoList();
                }else{
                    ToastUtils.showShort("请先获取电子书列表，再删除");
                }
            }
        });

        btn_delete_music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type == FissionConstant.MUSIC_FILE_LIST){
                    deleteMultimediaFileInfoList();
                }else{
                    ToastUtils.showShort("请先获取音乐列表，再删除");
                }
            }
        });

        btn_delete_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type == FissionConstant.VIDEO_FILE_LIST){
                    deleteMultimediaFileInfoList();
                }else{
                    ToastUtils.showShort("请先获取视频列表，再删除");
                }
            }
        });

        btn_delete_ringtone_msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type == FissionConstant.RINGTONE_SETTING_MSG){
                    deleteMultimediaFileInfoList();
                }else{
                    ToastUtils.showShort("请先获取消息铃声列表，再删除");
                }
            }
        });

        btn_delete_ringtone_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type == FissionConstant.RINGTONE_SETTING_CALL){
                    deleteMultimediaFileInfoList();
                }else{
                    ToastUtils.showShort("请先获取来电铃声列表，再删除");
                }
            }
        });

        btn_delete_ringtone_clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type == FissionConstant.RINGTONE_SETTING_CLOCK){
                    deleteMultimediaFileInfoList();
                }else{
                    ToastUtils.showShort("请先获取闹钟铃声列表，再删除");
                }
            }
        });

        btn_get_ringtone_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FissionSdkBleManage.getInstance().getRingtoneSettings();
            }
        });

        btn_meeting_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = FissionConstant.MEETING_FILE_LIST;
                getMultimediaFileInfoList(type);
            }
        });

        btn_download_meeting_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(meetingFileInfos == null || meetingFileInfos.isEmpty()){
                    ToastUtils.showShort("没有会议文件，无法下载");
                }else{
                    String filePath = "/user/mediaaudio/meeting/"+meetingFileInfos.get(0).getFullName();
                    String downloadPath = getExternalCacheDir()+"/"+meetingFileInfos.get(0).getFullName()+meetingFileInfos.get(0).getSuffix();
                    long offset = 0;
                    if (FileUtils.isFileExists(downloadPath)) {
                        offset = new File(downloadPath).length();
                    }
                    FissionSdkBleManage.getInstance().downloadFileByOffset(downloadPath, filePath, offset, new HiSiDownloadFileUtil.HiSiDownloadFileListener() {
                        @Override
                        public void onProgressChanged(int currentFileIndex, int fileNum, int progress) {

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
                }
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
                if(CMD_ID_ST_NOTES_REMINDERS.equals(cmdId)){
                    ToastUtils.showLong("指令超时， 删除失败");
                }
            }

            @Override
            public void onResultError(String errorMsg) {

            }

            @Override
            public void getHsJsAppFileList(List<HsJsFileInfo> list) {
                super.getHsJsAppFileList(list);
                hsJsFileInfos = list;
                tv_js_list.setText(hsJsFileInfos.toString());
            }

            @Override
            public void getHsDialFileList(List<HsDialInfo> list) {
                super.getHsDialFileList(list);
                hsDialInfos = list;
                tv_dial_list.setText(hsDialInfos.toString());
            }

            @Override
            public void getHsEbookFileList(List<HsMultimediaFileInfo> list) {
                super.getHsEbookFileList(list);
                hsMultimediaFileInfos = list;
                tv_media_list.setText(hsMultimediaFileInfos.toString());
            }

            @Override
            public void getHsMusicFileList(List<HsMultimediaFileInfo> list) {
                super.getHsMusicFileList(list);
                hsMultimediaFileInfos = list;
                tv_media_list.setText(hsMultimediaFileInfos.toString());
            }

            @Override
            public void getHsVideoFileList(List<HsMultimediaFileInfo> list) {
                super.getHsVideoFileList(list);
                hsMultimediaFileInfos = list;
                tv_media_list.setText(hsMultimediaFileInfos.toString());
            }

            @Override
            public void getHsRingtoneMsgFileList(List<HsMultimediaFileInfo> list) {
                super.getHsRingtoneMsgFileList(list);
                hsMultimediaFileInfos = list;
                tv_media_list.setText(hsMultimediaFileInfos.toString());
            }

            @Override
            public void getHsRingtoneCallFileList(List<HsMultimediaFileInfo> list) {
                super.getHsRingtoneCallFileList(list);
                hsMultimediaFileInfos = list;
                tv_media_list.setText(hsMultimediaFileInfos.toString());
            }

            @Override
            public void getHsRingtoneClockFileList(List<HsMultimediaFileInfo> list) {
                super.getHsRingtoneClockFileList(list);
                hsMultimediaFileInfos = list;
                tv_media_list.setText(hsMultimediaFileInfos.toString());
            }

            @Override
            public void deleteFileInfoList() {
                super.deleteFileInfoList();
                ToastUtils.showLong("删除成功");
            }

            @Override
            public void getRingtoneSettings(List<RingtoneSetting> ringtoneSettings) {
                super.getRingtoneSettings(ringtoneSettings);
                tv_media_list.setText(ringtoneSettings.toString());
            }

            @Override
            public void getMeetingFileInfoList(List<MeetingFileInfo> list) {
                super.getMeetingFileInfoList(list);
                meetingFileInfos = list;
                tv_media_list.setText(list.toString());
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

    private void getDialList() {
        FissionSdkBleManage.getInstance().getFileInfoList(FissionConstant.DIAL_FILE_LIST);
    }

    private void getJsList() {
        FissionSdkBleManage.getInstance().getFileInfoList(FissionConstant.JS_FILE_LIST);
    }

    private void deleteDialList() {
        if(hsDialInfos!=null && hsDialInfos.size()>0){
            FissionSdkBleManage.getInstance().deleteDialFileInfoList(hsDialInfos);
        }
    }

    private void deleteJsList() {
        if(hsJsFileInfos!=null && hsJsFileInfos.size()>0){
            FissionSdkBleManage.getInstance().deleteJsFileInfoList(hsJsFileInfos);
        }
    }

    private void getMultimediaFileInfoList(int type) {
        FissionSdkBleManage.getInstance().getFileInfoList(type);
    }

    private void deleteMultimediaFileInfoList() {
        if(hsMultimediaFileInfos!=null && !hsMultimediaFileInfos.isEmpty()){
            FissionSdkBleManage.getInstance().deleteMultimediaFileInfoList(hsMultimediaFileInfos, type);
        }
    }

}
