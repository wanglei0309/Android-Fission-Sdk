package com.szfission.wear.demo.activity;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;

import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;

import java.util.Objects;

public class SetMessageTypeParaActivity extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_SET_HR_CHECK_PARA);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        AnyWear.getMessageTypePara();
        showProgress();
    }

}
