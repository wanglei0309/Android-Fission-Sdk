package com.szfission.wear.demo.adapter;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.szfission.wear.demo.R;

/**
 * describe:
 * author: wl
 * createTime: 2023/11/25
 */
public class HiSiliconFunctionAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public HiSiliconFunctionAdapter() {
        super(R.layout.list_item_haisi_test);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, String data) {

        baseViewHolder.setText(R.id.tv_function, data);
    }
}
