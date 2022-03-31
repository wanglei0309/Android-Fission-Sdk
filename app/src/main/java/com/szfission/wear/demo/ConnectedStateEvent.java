package com.szfission.wear.demo;

public class ConnectedStateEvent {
    private int     state;
    private String name;

    public ConnectedStateEvent(int state,String name) {
        this.state = state;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
