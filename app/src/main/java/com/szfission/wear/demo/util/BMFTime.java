package com.szfission.wear.demo.util;

import java.text.SimpleDateFormat;

/**
 * describe:
 * author: wl
 * createTime: 2024/2/29
 */
public class BMFTime {
    private static final SimpleDateFormat format =  new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    /**
     * 时间段，单位（天）
     */
    public int dates;

    /**
     * 时间段，单位（小时）
     */
    public int hours;

    /**
     * 时间段，单位（分）
     */
    public int minutes;

    /**
     * 时间段，单位（秒）
     */
    public int seconds;

    public BMFTime(int duration) {
        this.dates = duration / (60 * 60 * 24);
        duration -= this.dates * 60  * 60  * 24;
        this.hours = duration / (60 * 60);
        duration -= this.hours * 60 * 60;
        this.minutes = duration / 60;
        duration -= this.minutes * 60;
        this.seconds = duration;
    }
}
