package com.szfission.wear.demo.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.UriUtils;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.fragment.DownloadFragment;
import com.szfission.wear.demo.fragment.UploadFragment;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

/**
 * describe:
 * author: wl
 * createTime: 2023/11/25
 */
public class FileTransferActivity extends BaseActivity{

    private UploadFragment uploadFragment;

    private DownloadFragment downloadFragment;

    RadioButton rb_file_upload;

    RadioButton rb_file_download;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer);
        String[] testArray = getResources().getStringArray(R.array.haisi_test_array);
        setTitle(testArray[0]);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        rb_file_upload = findViewById(R.id.rb_file_upload);
        rb_file_download = findViewById(R.id.rb_file_download);

        init();
    }

    private void init(){
        FragmentManager fragmentManager = getSupportFragmentManager();

        uploadFragment = UploadFragment.newInstance();
        downloadFragment = DownloadFragment.newInstance();

        rb_file_upload.setChecked(true);

        // 初始时显示 UploadFragment
        fragmentManager.beginTransaction()
                .replace(R.id.frameLayout, uploadFragment)
                .commit();

        rb_file_upload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    // 创建新的 FragmentTransaction
                    fragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, uploadFragment)
                            .commit();
                }
            }
        });

        rb_file_download.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    // 创建新的 FragmentTransaction
                    fragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, downloadFragment)
                            .commit();
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
