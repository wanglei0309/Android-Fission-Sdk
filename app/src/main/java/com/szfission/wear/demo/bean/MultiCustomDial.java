package com.szfission.wear.demo.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;

/**
 * describe:
 * author: wl
 * createTime: 2023/5/29
 */
public class MultiCustomDial implements MultiItemEntity {

    public static final int TYPE_TITLE = 1;
    public static final int TYPE_DATA_BG = 2;
    public static final int TYPE_DATA_SCALE = 3;
    public static final int TYPE_DATA_POINTER = 4;
    public static final int TYPE_DATA_FUNCTION = 5;
    public static final int TYPE_DATA_DATETIME = 6;
    public static final int TYPE_DATA_BLE = 7;
    public static final int TYPE_DATA_BT = 8;
    public static final int TYPE_DATA_BATTERY = 9;

    private int type;

    private Object data;

    public MultiCustomDial() {
    }

    public MultiCustomDial(int type, Object data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public int getItemType() {
        return type;
    }
}
