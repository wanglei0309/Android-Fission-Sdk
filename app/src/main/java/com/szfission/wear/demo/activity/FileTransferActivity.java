package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;

import com.szfission.wear.demo.R;
import com.szfission.wear.demo.fragment.DownloadFragment;
import com.szfission.wear.demo.fragment.UploadFragment;

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
