package com.szfission.wear.demo.adapter;

import android.content.Context;
import android.graphics.Bitmap;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.fission.wear.sdk.v2.bean.CustomWatchFaceInfo;
import com.fission.wear.sdk.v2.utils.FissionDialUtil;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.bean.MultiCustomDial;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * describe:
 * author: wl
 * createTime: 2023/5/29
 */
public class MultiCustomDialAdapter extends BaseMultiItemQuickAdapter<MultiCustomDial, BaseViewHolder> {

    private Context mContext;

    private String mDirectory;
    public MultiCustomDialAdapter(Context context, @Nullable List<MultiCustomDial> data, String directory) {
        super(data);
        mContext = context;
        mDirectory = directory;
        addItemType(MultiCustomDial.TYPE_TITLE, R.layout.list_item_title_name);
        addItemType(MultiCustomDial.TYPE_DATA_BG, R.layout.list_item_content_picture);
        addItemType(MultiCustomDial.TYPE_DATA_SCALE, R.layout.list_item_content_picture);
        addItemType(MultiCustomDial.TYPE_DATA_POINTER, R.layout.list_item_content_picture);
        addItemType(MultiCustomDial.TYPE_DATA_FUNCTION, R.layout.list_item_content_picture2);
        addItemType(MultiCustomDial.TYPE_DATA_DATETIME, R.layout.list_item_content_picture2);
        addItemType(MultiCustomDial.TYPE_DATA_BATTERY, R.layout.list_item_content_picture2);
        addItemType(MultiCustomDial.TYPE_DATA_BLE, R.layout.list_item_content_picture2);
        addItemType(MultiCustomDial.TYPE_DATA_BT, R.layout.list_item_content_picture2);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, MultiCustomDial multiCustomDial) {
        Bitmap bitmap;
        switch (multiCustomDial.getItemType()){
            case MultiCustomDial.TYPE_TITLE:
                baseViewHolder.setText(R.id.tv_name, multiCustomDial.getData().toString());
                break;

            case MultiCustomDial.TYPE_DATA_BG:
            case MultiCustomDial.TYPE_DATA_SCALE:
            case MultiCustomDial.TYPE_DATA_POINTER:
                bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(mDirectory+multiCustomDial.getData().toString()));
                baseViewHolder.setImageBitmap(R.id.iv_picture, bitmap);
                break;

            case MultiCustomDial.TYPE_DATA_FUNCTION:
                CustomWatchFaceInfo.Function function = (CustomWatchFaceInfo.Function) multiCustomDial.getData();
                bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(mDirectory+function.functionName));
                baseViewHolder.setImageBitmap(R.id.iv_picture2, bitmap);
                break;

            case MultiCustomDial.TYPE_DATA_BATTERY:
            case MultiCustomDial.TYPE_DATA_BLE:
            case MultiCustomDial.TYPE_DATA_BT:
            case MultiCustomDial.TYPE_DATA_DATETIME:
                bitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(mDirectory+multiCustomDial.getData().toString()));
                baseViewHolder.setImageBitmap(R.id.iv_picture2, bitmap);
                break;
        }
    }
}
