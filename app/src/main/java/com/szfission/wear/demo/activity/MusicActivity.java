package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.blankj.utilcode.util.LogUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.MusicConfig;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.param.FissionMusicInfo;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

/**
 * 推送当前歌曲信息
 */
public class MusicActivity extends BaseActivity {

    EditText etName;

    EditText etSinger;

    EditText etTime;

    EditText etTitle;

    EditText etPlayAppName;

    Button btn_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        setTitle(R.string.FUNC_STRU_MUSIC_CONT);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        etName = findViewById(R.id.etName);
        etSinger = findViewById(R.id.etSinger);
        etTime = findViewById(R.id.etTime);
        etTitle = findViewById(R.id.etTitle);
        etPlayAppName = findViewById(R.id.etPlayAppName);
        btn_send = findViewById(R.id.btn_send);
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

//        showProgress();
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
                showToast(errorMsg);
                dismissProgress();
            }

            @Override
            public void sendMusicInfo() {
                super.sendMusicInfo();
                LogUtils.d("wl", "推送歌曲信息成功");
                dismissProgress();
                showSuccessToast();
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

    private void send() {
        String name = etName.getText().toString();
        String singer = etSinger.getText().toString();
        String time = etTime.getText().toString();
        String title = etTitle.getText().toString();
        String playAppName = etPlayAppName.getText().toString();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入歌曲名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.getBytes().length > 32) {
            Toast.makeText(this, "歌曲名称过长", Toast.LENGTH_SHORT).show();
            return;
        }
        if (singer.isEmpty()) {
            Toast.makeText(this, "请输入演唱者", Toast.LENGTH_SHORT).show();
            return;
        }
        if (singer.getBytes().length > 32) {
            Toast.makeText(this, "演唱者过长", Toast.LENGTH_SHORT).show();
            return;
        }
        if (time.isEmpty()) {
            Toast.makeText(this, "请输入歌曲长", Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入专辑名", Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.getBytes().length > 32) {
            Toast.makeText(this, "演唱者过长", Toast.LENGTH_SHORT).show();
            return;
        }
        if (playAppName.isEmpty()) {
            Toast.makeText(this, "请输入播放app名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (playAppName.getBytes().length > 32) {
            Toast.makeText(this, "播放app名称过长", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();
        FissionMusicInfo fissionMusicInfo = new FissionMusicInfo();
        fissionMusicInfo.setMusicName(name);
        fissionMusicInfo.setMusicSinger(singer);
        fissionMusicInfo.setAlbumName(title);
        fissionMusicInfo.setPlayAppName(playAppName);
        fissionMusicInfo.setMusicTotalTime(Integer.parseInt(time));
        FissionSdkBleManage.getInstance().sendMusicInfo(fissionMusicInfo);
//        for(int i=0; i<500; i++){
//            fissionMusicInfo.setMusicName(name+i);
//            FissionSdkBleManage.getInstance().sendMusicInfo(fissionMusicInfo);
//            MusicConfig musicConfig = new MusicConfig();
//            musicConfig.setState(i%2==0 ? MusicConfig.MUSIC_PLAYING : MusicConfig.MUSIC_PAUSE);
//            musicConfig.setDuration(1000);
//            musicConfig.setProgress(i);
//            musicConfig.setMaxVolume(10);
//            musicConfig.setCurrentVolume(5);
//            FissionSdkBleManage.getInstance().setMusicControl(musicConfig);
//            FissionSdkBleManage.getInstance().setMusicVolume(musicConfig);
//            FissionSdkBleManage.getInstance().setMusicProgress(musicConfig);
//        }

    }
}
