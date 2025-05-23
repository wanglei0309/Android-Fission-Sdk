package com.szfission.wear.demo.adapter;

import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.bean.DefaultDialState;

import org.jetbrains.annotations.NotNull;

public class DefaultDialStateListAdapter extends BaseQuickAdapter<DefaultDialState, BaseViewHolder> {

    public DefaultDialStateListAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder holder, DefaultDialState defaultDialState) {
        holder.setText(R.id.tv_name,"内置表盘编号："+ defaultDialState.getId());
        ((CheckBox)holder.getView(R.id.checkbox)).setChecked(defaultDialState.isOpen());
        ((CheckBox)holder.getView(R.id.checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                defaultDialState.setOpen(b);
            }
        });
    }

}
