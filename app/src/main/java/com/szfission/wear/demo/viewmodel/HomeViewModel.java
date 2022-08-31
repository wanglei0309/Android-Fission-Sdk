package com.szfission.wear.demo.viewmodel;

import androidx.lifecycle.ViewModel;

import com.szfission.wear.demo.R;
import com.szfission.wear.demo.bean.FuncBean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.szfission.wear.demo.ModelConstant.FUNC_BREAK_DEVICE;
import static com.szfission.wear.demo.ModelConstant.FUNC_CAMERA_MODEL;
import static com.szfission.wear.demo.ModelConstant.FUNC_CLEAR_SPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_CLEAR_USER_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_COMPRESS_CMD;
import static com.szfission.wear.demo.ModelConstant.FUNC_FIND_DEVICE;
import static com.szfission.wear.demo.ModelConstant.FUNC_FLASH_WRITE_CMD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_APPS_MESS;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_BATTERY;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_BLOODPRESSURE_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_BURIED_DATA;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_CUR_SLEEP_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_DAYS_REPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXERCISE_DETAIL;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXERCISE_GPS;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXERCISE_LIST;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXERCISE_REPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_EXER_GPS_DETAIL;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_FLASH_DATA;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_GPV;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_HAND_MEASURE_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_HARDWARE_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_HEARTED_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_HOURS_REPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_MEASURE_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_PERSONAL_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_RESTING_HR;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SEDENTARY_DRINK_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SEDENTARY_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SLEEP_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SLEEP_REPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_SPO2_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_STEPS_RECORD;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_TIME;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_TIMEZONE;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_UI_VERSION;
import static com.szfission.wear.demo.ModelConstant.FUNC_GET_VERSION;
import static com.szfission.wear.demo.ModelConstant.FUNC_GIVE_UP_FIND_DEVICE;
import static com.szfission.wear.demo.ModelConstant.FUNC_GPS_SPORT_CMD;
import static com.szfission.wear.demo.ModelConstant.FUNC_LOCATION_INFORMATION;
import static com.szfission.wear.demo.ModelConstant.FUNC_MUSIC_CONTROL;
import static com.szfission.wear.demo.ModelConstant.FUNC_MUSIC_PROGRESS;
import static com.szfission.wear.demo.ModelConstant.FUNC_MUSIC_VOLUME;
import static com.szfission.wear.demo.ModelConstant.FUNC_ONLINE_DIAL_PUSH;
import static com.szfission.wear.demo.ModelConstant.FUNC_OTA;
import static com.szfission.wear.demo.ModelConstant.FUNC_PAGE_SKIP;
import static com.szfission.wear.demo.ModelConstant.FUNC_PUSH_CUSTOM_DIAL;
import static com.szfission.wear.demo.ModelConstant.FUNC_PUSH_CUSTOM_SPORT;
import static com.szfission.wear.demo.ModelConstant.FUNC_QUICK_REPLY_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_REBOOT_DEVICE;
import static com.szfission.wear.demo.ModelConstant.FUNC_RESET;
import static com.szfission.wear.demo.ModelConstant.FUNC_SAFETY_CONFIRM;
import static com.szfission.wear.demo.ModelConstant.FUNC_SELF_INSPECTION_MODE;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_DATA_STREAM;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_DATA_STREAM2;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_DONT_DISTURB_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_DRINK_WATER_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_FEMALE_PHYSIOLOGY;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HIGH_SPEED_CONNECT;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HRLEV_ALGO_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HR_CHECK_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HR_WARN_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_LANG;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_LIFTWRIST_PARA;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_PROMPT;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TARGET_SET;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIME;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIMEZONE;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIME_MODE;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIMING_INFO;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_UNIT;
import static com.szfission.wear.demo.ModelConstant.FUNC_SET_WRIST_BRIGHT_SCREEN;
import static com.szfission.wear.demo.ModelConstant.FUNC_SHUTDOWN;
import static com.szfission.wear.demo.ModelConstant.FUNC_STRU_CALL_DATA;
import static com.szfission.wear.demo.ModelConstant.FUNC_STRU_MUSIC_CONT;
import static com.szfission.wear.demo.ModelConstant.FUNC_SWITCH_HR_RATE;
import static com.szfission.wear.demo.ModelConstant.FUNC_SYN_PHONE_BOOK;
import static com.szfission.wear.demo.ModelConstant.FUNC_VIBRATION;
import static com.szfission.wear.demo.ModelConstant.FUNC_WEATHER;
import static com.szfission.wear.demo.ModelConstant.FUNC_WEATHER_DETAIL;

//import static com.szfission.wear.demo.ModelConstant.FUNC_CAMERA_MODEL;
//import static com.szfission.wear.demo.ModelConstant.FUNC_FIND_DEVICE;
//import static com.szfission.wear.demo.ModelConstant.FUNC_GET_BATTERY;
//import static com.szfission.wear.demo.ModelConstant.FUNC_GET_GPV;
//import static com.szfission.wear.demo.ModelConstant.FUNC_GET_TIME;
//import static com.szfission.wear.demo.ModelConstant.FUNC_GET_VERSION;
//import static com.szfission.wear.demo.ModelConstant.FUNC_OTA;
//import static com.szfission.wear.demo.ModelConstant.FUNC_REBOOT_DEVICE;
//import static com.szfission.wear.demo.ModelConstant.FUNC_RESET;
//import static com.szfission.wear.demo.ModelConstant.FUNC_SET_HIGH_SPEED_CONNECT;
//import static com.szfission.wear.demo.ModelConstant.FUNC_SET_LANG;
//import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIME;
//import static com.szfission.wear.demo.ModelConstant.FUNC_SET_TIMEZONE;
//import static com.szfission.wear.demo.ModelConstant.FUNC_SET_UNIT;
//import static com.szfission.wear.demo.ModelConstant.FUNC_SET_WRIST_BRIGHT_SCREEN;
//import static com.szfission.wear.demo.ModelConstant.FUNC_SHUTDOWN;
//import static com.szfission.wear.demo.ModelConstant.FUNC_VIBRATION;

public class HomeViewModel extends ViewModel {
//    public final int FUNC_GET_VERSION = 1;
//    public final int FUNC_GET_BATTERY = 2;
//    public final int FUNC_GET_GPV = 3;
//    public final int FUNC_GET_TIME = 4;
//    public final int FUNC_GET_TIMEZONE = 5;
//    public final int FUNC_SET_TIME = 6;
//    public final int FUNC_SET_TIMEZONE = 7;
//    public final int FUNC_SET_LANG = 8;
//    public final int FUNC_SET_UNIT = 9;
//    public final int FUNC_VIBRATION = 10;
//    public final int FUNC_SET_WRIST_BRIGHT_SCREEN = 11;
//    public final int FUNC_CAMERA_MODEL = 12;
//    public final int FUNC_SET_DATA_STREAM = 13;
//    public final int FUNC_SET_HIGH_SPEED_CONNECT = 14;
//    public final int FUNC_SET_HR = 15;
//    public final int FUNC_CAMERA = 16;
//    public final int FUNC_FIND_DEVICE = 17;
//    public final int FUNC_REBOOT_DEVICE = 18;
//    public final int FUNC_RESET = 19;
//    public final int FUNC_SHUTDOWN = 20;
//    public final int FUNC_OTA = 21;
//    public final int FUNC_SAFETY_CONFIRM = 22;
//    public final int FUNC_SELF_INSPECTION_MODE = 23;
//    public final int FUNC_CLEAR_USER_INFO = 24;
//    public final int FUNC_CLEAR_SPORT = 25;
//    public final int FUNC_PAGE_SKIP = 26;
//    public final int FUNC_MUSIC_VOLUME = 27;
//    public final int FUNC_MUSIC_CONTROL = 28;
//    public final int FUNC_MUSIC_PROGRESS = 29;
//    public final int FUNC_BREAK_DEVICE = 30;
//    public final int FUNC_SET_FEMALE_PHYSIOLOGY = 31;
//    public final int FUNC_SET_TIME_MODE = 32;
//
//
//    public final int FUNC_GET_HARDWARE_INFO = 100;
//    public final int FUNC_GET_DAYS_REPORT = 101;
//    public final int FUNC_GET_EXERCISE_LIST = 102;
//    public final int FUNC_GET_EXERCISE_REPORT = 103;
//    public final int FUNC_GET_HEARTRATE_RECORD = 104;
//    public final int FUNC_GET_EXERCISE_DETAIL = 105;
//    public final int FUNC_GET_EXER_GPS_DETAIL = 1051;
//    public final int FUNC_GET_PERSONAL_INFO = 106;
//    public final int FUNC_GET_SEDENTARY_PARA = 107;
//    public final int FUNC_GET_APPS_MESS = 108;
//    public final int FUNC_GET_HOURS_REPORT = 109;
//    public final int FUNC_GET_SLEEP_REPORT = 110;
//    public final int FUNC_GET_SLEEP_RECORD = 1101;
//    public final int FUNC_GET_STEPS_RECORD = 111;
//    public final int FUNC_GET_SPO2_RECORD = 112;
//    public final int FUNC_GET_BLOODPRESSURE_RECORD = 113;
//    public final int FUNC_SET_HRLEV_ALGO_PARA = 114;
//    public final int FUNC_SET_DRINK_WATER_PARA = 115;
//    public final int FUNC_SET_DONT_DISTURB_PARA = 116;
//    public final int FUNC_SET_HR_CHECK_PARA = 117;
//    public final int FUNC_SET_LIFTWRIST_PARA = 118;
//    public final int FUNC_SET_TARGET_SET = 119;
//    public final int FUNC_WEATHER = 120;
//    public final int FUNC_STRU_CALL_DATA = 121;
//    public final int FUNC_STRU_MUSIC_CONT = 122;
//    public final int FUNC_SET_MESSAGE_TYPE_PARA = 123;
//    public static final int FUNC_SET_TIMING_INFO = 124;
//    public  static final int FUNC_WEATHER_DETAIL = 1201;
//    public  static final int FUNC_LOCATION_INFORMATION = 125;


    public List<FuncBean> funcBeans;


    List<String> groupList = new ArrayList<>();


    ArrayList<ArrayList<FuncBean>> childList = new ArrayList<>();
    ArrayList<FuncBean> childList1 = new ArrayList<>();
    ArrayList<FuncBean> childList2 = new ArrayList<>();
    ArrayList<FuncBean> childList3 = new ArrayList<>();
    ArrayList<FuncBean> childList4 = new ArrayList<>();


    /**
     * public final int FUNC_GET_GPV = 3;
     * public final int FUNC_GET_TIME = 4;
     * public final int FUNC_GET_TIMEZONE = 5;
     * public final int FUNC_SET_TIME = 6;
     * public final int FUNC_SET_TIMEZONE = 7;
     * public final int FUNC_SET_LANG = 8;
     * public final int FUNC_SET_UNIT = 9;
     * public final int FUNC_VIBRATION = 10;
     * public final int FUNC_SET_WRIST_BRIGHT_SCREEN = 11;
     * public final int FUNC_CAMERA_MODEL = 12;
     * public final int FUNC_SET_DATA_STREAM = 13;
     * public final int FUNC_SET_HIGH_SPEED_CONNECT = 14;
     * public final int FUNC_SET_HR = 15;
     * public final int FUNC_CAMERA = 16;
     * public final int FUNC_FIND_DEVICE = 17;
     *
     * @return
     */

    public ArrayList<ArrayList<FuncBean>> getFuncBeans() {
        childList1.clear();
        childList2.clear();
        childList3.clear();
        childList4.clear();

        childList1.add(new FuncBean(R.string.FUNC_GET_UI_VERSION, FUNC_GET_UI_VERSION));
        childList1.add(new FuncBean(R.string.FUNC_GET_VERSION, FUNC_GET_VERSION));
        childList1.add(new FuncBean(R.string.FUNC_GET_BATTERY, FUNC_GET_BATTERY));
        childList1.add(new FuncBean(R.string.FUNC_GET_GPV, FUNC_GET_GPV));
        childList1.add(new FuncBean(R.string.FUNC_GET_TIME, FUNC_GET_TIME));
        childList1.add(new FuncBean(R.string.FUNC_SET_TIME, FUNC_SET_TIME));
        childList1.add(new FuncBean(R.string.FUNC_GET_RESTING_HR,FUNC_GET_RESTING_HR));
        childList1.add(new FuncBean(R.string.FUNC_GET_TIMEZONE, FUNC_GET_TIMEZONE));
        childList1.add(new FuncBean(R.string.FUNC_SET_TIMEZONE, FUNC_SET_TIMEZONE));
        childList1.add(new FuncBean(R.string.FUNC_SET_TIME_MODE, FUNC_SET_TIME_MODE));
        childList1.add(new FuncBean(R.string.FUNC_SET_LANG, FUNC_SET_LANG));
        childList1.add(new FuncBean(R.string.FUNC_SET_UNIT, FUNC_SET_UNIT));
        childList1.add(new FuncBean(R.string.FUNC_SET_PROMPT, FUNC_SET_PROMPT));
        childList1.add(new FuncBean(R.string.FUNC_SET_FEMALE_PHYSIOLOGY, FUNC_SET_FEMALE_PHYSIOLOGY));
        childList1.add(new FuncBean(R.string.FUNC_VIBRATION, FUNC_VIBRATION));
        childList1.add(new FuncBean(R.string.FUNC_SET_WRIST_BRIGHT_SCREEN, FUNC_SET_WRIST_BRIGHT_SCREEN));
        childList1.add(new FuncBean(R.string.FUNC_CAMERA_MODEL, FUNC_CAMERA_MODEL));
        childList1.add(new FuncBean(R.string.FUNC_SET_HIGH_SPEED_CONNECT, FUNC_SET_HIGH_SPEED_CONNECT));
//        childList1.add(new FuncBean(R.string.FUNC_CAMERA, FUNC_CAMERA));
        childList1.add(new FuncBean(R.string.FUNC_FIND_DEVICE, FUNC_FIND_DEVICE));
        childList1.add(new FuncBean(R.string.FUNC_GIVE_UP_FIND_DEVICE, FUNC_GIVE_UP_FIND_DEVICE));
        childList1.add(new FuncBean(R.string.FUNC_REBOOT_DEVICE, FUNC_REBOOT_DEVICE));
        childList1.add(new FuncBean(R.string.FUNC_RESET, FUNC_RESET));
        childList1.add(new FuncBean(R.string.FUNC_SHUTDOWN, FUNC_SHUTDOWN));
        childList1.add(new FuncBean(R.string.FUNC_SAFETY_CONFIRM, FUNC_SAFETY_CONFIRM));
        childList1.add(new FuncBean(R.string.FUNC_SELF_INSPECTION_MODE, FUNC_SELF_INSPECTION_MODE));
        childList1.add(new FuncBean(R.string.FUNC_CLEAR_USER_INFO, FUNC_CLEAR_USER_INFO));
        childList1.add(new FuncBean(R.string.FUNC_CLEAR_SPORT, FUNC_CLEAR_SPORT));
        childList1.add(new FuncBean(R.string.FUNC_PAGE_SKIP, FUNC_PAGE_SKIP));
        childList1.add(new FuncBean(R.string.FUNC_MUSIC_VOLUME, FUNC_MUSIC_VOLUME));
        childList1.add(new FuncBean(R.string.FUNC_MUSIC_CONTROL, FUNC_MUSIC_CONTROL));
        childList1.add(new FuncBean(R.string.FUNC_MUSIC_PROGRESS, FUNC_MUSIC_PROGRESS));
        childList1.add(new FuncBean(R.string.FUNC_BREAK_DEVICE, FUNC_BREAK_DEVICE));


        childList2.add(new FuncBean(R.string.FUNC_SET_DATA_STREAM, FUNC_SET_DATA_STREAM));
        childList2.add(new FuncBean(R.string.FUNC_MSG_TASK, FUNC_SET_DATA_STREAM2));
        childList2.add(new FuncBean(R.string.FUNC_SWITCH_HR_RATE, FUNC_SWITCH_HR_RATE));
        childList2.add(new FuncBean(R.string.FUNC_OTA, FUNC_OTA));
        childList2.add(new FuncBean(R.string.FUNC_PUSH_CUSTOM_DIAL, FUNC_PUSH_CUSTOM_DIAL));
        childList2.add(new FuncBean(R.string.FUNC_PUSH_CUSTOM_SPORT, FUNC_PUSH_CUSTOM_SPORT));
        childList2.add(new FuncBean(R.string.FUNC_GPS_SPORT_CMD, FUNC_GPS_SPORT_CMD));
        childList2.add(new FuncBean(R.string.FUNC_FLASH_WRITE_CMD, FUNC_FLASH_WRITE_CMD));
        childList2.add(new FuncBean(R.string.FUNC_COMPRESS_CMD, FUNC_COMPRESS_CMD));
        childList2.add(new FuncBean(R.string.FUNC_ONLINE_DIAL_PUSH, FUNC_ONLINE_DIAL_PUSH));

        childList3.add(new FuncBean(R.string.FUNC_GET_HARDWARE_INFO, FUNC_GET_HARDWARE_INFO));
        childList3.add(new FuncBean(R.string.FUNC_GET_MEARURE_INFO, FUNC_GET_MEASURE_INFO));
        childList3.add(new FuncBean(R.string.FUNC_GET_DAYS_REPORT, FUNC_GET_DAYS_REPORT));
        childList3.add(new FuncBean(R.string.FUNC_GET_HOURS_REPORT, FUNC_GET_HOURS_REPORT));
        childList3.add(new FuncBean(R.string.FUNC_GET_SLEEP_REPORT, FUNC_GET_SLEEP_REPORT));
        childList3.add(new FuncBean(R.string.FUNC_GET_SLEEP_RECORD, FUNC_GET_SLEEP_RECORD));
        childList3.add(new FuncBean(R.string.FUNC_GET_CUR_SLEEP_RECORD,FUNC_GET_CUR_SLEEP_RECORD));
        childList3.add(new FuncBean(R.string.FUNC_GET_EXERCISE_LIST, FUNC_GET_EXERCISE_LIST));
        childList3.add(new FuncBean(R.string.FUNC_GET_EXERCISE_REPORT, FUNC_GET_EXERCISE_REPORT));
        childList3.add(new FuncBean(R.string.FUNC_GET_EXERCISE_GPS, FUNC_GET_EXERCISE_GPS));
        childList3.add(new FuncBean(R.string.FUNC_GET_HEARTRATE_RECORD, FUNC_GET_HEARTED_RECORD));
        childList3.add(new FuncBean(R.string.FUNC_SET_HR_WARN_PARA, FUNC_SET_HR_WARN_PARA));
        childList3.add(new FuncBean(R.string.FUNC_GET_STEPS_RECORD, FUNC_GET_STEPS_RECORD));
        childList3.add(new FuncBean(R.string.FUNC_GET_SPO2_RECORD, FUNC_GET_SPO2_RECORD));
        childList3.add(new FuncBean(R.string.FUNC_GET_BLOODPRESSURE_RECORD, FUNC_GET_BLOODPRESSURE_RECORD));
        childList3.add(new FuncBean(R.string.FUNC_GET_EXERCISE_DETAIL, FUNC_GET_EXERCISE_DETAIL));
        childList3.add(new FuncBean(R.string.FUNC_GET_EXER_GPS_DETAIL, FUNC_GET_EXER_GPS_DETAIL));
        childList3.add(new FuncBean(R.string.FUNC_GET_PERSONAL_INFO, FUNC_GET_PERSONAL_INFO));
        childList3.add(new FuncBean(R.string.FUNC_SET_HRLEV_ALGO_PARA, FUNC_SET_HRLEV_ALGO_PARA));
        childList3.add(new FuncBean(R.string.FUNC_GET_SEDENTARY_PARA, FUNC_GET_SEDENTARY_PARA));
        childList3.add(new FuncBean(R.string.FUNC_SET_DRINK_WATER_PARA, FUNC_SET_DRINK_WATER_PARA));
        childList3.add(new FuncBean(R.string.FUNC_SET_DONT_DISTURB_PARA, FUNC_SET_DONT_DISTURB_PARA));
        childList3.add(new FuncBean(R.string.FUNC_SET_HR_CHECK_PARA, FUNC_SET_HR_CHECK_PARA));
        childList3.add(new FuncBean(R.string.FUNC_SET_LIFTWRIST_PARA, FUNC_SET_LIFTWRIST_PARA));
        childList3.add(new FuncBean(R.string.FUNC_SET_TARGET_SET, FUNC_SET_TARGET_SET));
        childList3.add(new FuncBean(R.string.FUNC_WEATHER, FUNC_WEATHER));
        childList3.add(new FuncBean(R.string.FUNC_WEATHER_DETAIL, FUNC_WEATHER_DETAIL));
        childList3.add(new FuncBean(R.string.FUNC_GET_APPS_MESS, FUNC_GET_APPS_MESS));
        childList3.add(new FuncBean(R.string.FUNC_SET_TIMING_INFO, FUNC_SET_TIMING_INFO));
        childList3.add(new FuncBean(R.string.FUNC_STRU_CALL_DATA, FUNC_STRU_CALL_DATA));
        childList3.add(new FuncBean(R.string.FUNC_STRU_MUSIC_CONT, FUNC_STRU_MUSIC_CONT));
        childList3.add(new FuncBean(R.string.FUNC_LOCATION_INFORMATION, FUNC_LOCATION_INFORMATION));
        childList3.add(new FuncBean(R.string.FUNC_GET_HAND_MEASURE_INFO, FUNC_GET_HAND_MEASURE_INFO));
        childList3.add(new FuncBean(R.string.FUNC_QUICK_REPLY_CMD, FUNC_QUICK_REPLY_INFO));
        childList3.add(new FuncBean(R.string.FUNC_GET_BURIED_DATA, FUNC_GET_BURIED_DATA));
        childList3.add(new FuncBean(R.string.FUNC_SYN_PHONE_BOOK, FUNC_SYN_PHONE_BOOK));
        childList3.add(new FuncBean(R.string.FUNC_GET_FLASH_DATA, FUNC_GET_FLASH_DATA));

        childList4.add(new FuncBean(R.string.FUNC_GET_SEDENTARY_DRINK_PARA,FUNC_GET_SEDENTARY_DRINK_PARA));

        childList.add(childList1);
        childList.add(childList2);
        childList.add(childList3);
        childList.add(childList4);
        return childList;
    }


    Date dateResult = null;


}
