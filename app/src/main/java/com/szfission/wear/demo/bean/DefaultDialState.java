package com.szfission.wear.demo.bean;

/**
 * describe:
 * author: wl
 * createTime: 2025/2/22
 */
public class DefaultDialState {

    private int id;

    private boolean isOpen;

    public DefaultDialState(int id, boolean isOpen) {
        this.id = id;
        this.isOpen = isOpen;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    @Override
    public String toString() {
        return "DefaultDialState{" +
                "id=" + id +
                ", isOpen=" + isOpen +
                '}';
    }
}
