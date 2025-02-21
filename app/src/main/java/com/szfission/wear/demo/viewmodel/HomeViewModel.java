package com.szfission.wear.demo.viewmodel;

import static com.szfission.wear.demo.ModelConstant.*;

import androidx.lifecycle.ViewModel;

import com.szfission.wear.demo.R;
import com.szfission.wear.demo.bean.FuncBean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class HomeViewModel extends ViewModel {

    public List<FuncBean> funcBeans;


    List<String> groupList = new ArrayList<>();


    ArrayList<ArrayList<FuncBean>> childList = new ArrayList<>();
    ArrayList<FuncBean> childList1 = new ArrayList<>();
    ArrayList<FuncBean> childList2 = new ArrayList<>();
    ArrayList<FuncBean> childList3 = new ArrayList<>();
//    ArrayList<FuncBean> childList4 = new ArrayList<>();


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
//        childList4.clear();

        childList1.add(new FuncBean(R.string.FUNC_GET_UI_VERSION, FUNC_GET_UI_VERSION));
        childList1.add(new FuncBean(R.string.FUNC_GET_VERSION, FUNC_GET_VERSION));
        childList1.add(new FuncBean(R.string.FUNC_GET_BATTERY, FUNC_GET_BATTERY));
        childList1.add(new FuncBean(R.string.FUNC_GET_GPV, FUNC_GET_GPV));
        childList1.add(new FuncBean(R.string.FUNC_GET_TIME, FUNC_GET_TIME));
        childList1.add(new FuncBean(R.string.FUNC_SET_TIME, FUNC_SET_TIME));
        childList1.add(new FuncBean(R.string.FUNC_SET_ANY_TIME, FUNC_SET_ANY_TIME));
        childList1.add(new FuncBean(R.string.FUNC_GET_RESTING_HR,FUNC_GET_RESTING_HR));
        childList1.add(new FuncBean(R.string.FUNC_GET_TIMEZONE, FUNC_GET_TIMEZONE));
        childList1.add(new FuncBean(R.string.FUNC_SET_TIMEZONE, FUNC_SET_TIMEZONE));
        childList1.add(new FuncBean(R.string.FUNC_SET_TIME_MODE, FUNC_SET_TIME_MODE));
        childList1.add(new FuncBean(R.string.FUNC_SET_LANG, FUNC_SET_LANG));
        childList1.add(new FuncBean(R.string.FUNC_SET_UNIT, FUNC_SET_UNIT));
        childList1.add(new FuncBean(R.string.FUNC_SET_TEMPERATURE_UNIT, FUNC_SET_TEMPERATURE_UNIT));
        childList1.add(new FuncBean(R.string.FUNC_SET_PROMPT, FUNC_SET_PROMPT));
//        childList1.add(new FuncBean(R.string.FUNC_SET_FEMALE_PHYSIOLOGY, FUNC_SET_FEMALE_PHYSIOLOGY));
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
        childList1.add(new FuncBean(R.string.FUNC_SHUTDOWN_STATE, FUNC_SHUTDOWN_STATE));
        childList1.add(new FuncBean(R.string.FUNC_SET_BLOOD_OXYGEN_SWITCH, FUNC_SET_BLOOD_OXYGEN_SWITCH));
        childList1.add(new FuncBean(R.string.FUNC_SET_MENTAL_STRESS_SWITCH, FUNC_SET_MENTAL_STRESS_SWITCH));
        childList1.add(new FuncBean(R.string.FUNC_SET_HEART_RATE_SWITCH, FUNC_SET_HEART_RATE_SWITCH));
        childList1.add(new FuncBean(R.string.FUNC_SET_CALL_AUDIO_SWITCH, FUNC_SET_CALL_AUDIO_SWITCH));
        childList1.add(new FuncBean(R.string.FUNC_GET_CALL_AUDIO_SWITCH, FUNC_GET_CALL_AUDIO_SWITCH));
        childList1.add(new FuncBean(R.string.FUNC_SET_MEDIA_AUDIO_SWITCH, FUNC_SET_MEDIA_AUDIO_SWITCH));
        childList1.add(new FuncBean(R.string.FUNC_GET_MEDIA_AUDIO_SWITCH, FUNC_GET_MEDIA_AUDIO_SWITCH));
        childList1.add(new FuncBean(R.string.FUNC_SET_STO, FUNC_SET_FUNC_SET_STO));
        childList1.add(new FuncBean(R.string.FUNC_SET_MAC, FUNC_SET_MAC));
        childList1.add(new FuncBean(R.string.FUNC_SET_SVM, FUNC_SET_SVM));
        childList1.add(new FuncBean(R.string.FUNC_SET_GPS_DATA_MODE, FUNC_SET_GPS_DATA_MODE));

        childList2.add(new FuncBean(R.string.FUNC_SET_DATA_STREAM, FUNC_SET_DATA_STREAM));
        childList2.add(new FuncBean(R.string.FUNC_MSG_TASK, FUNC_SET_DATA_STREAM2));
        childList2.add(new FuncBean(R.string.FUNC_GPS_DATA_MONITOR, FUNC_GPS_DATA_MONITOR));
        childList2.add(new FuncBean(R.string.FUNC_SWITCH_HR_RATE, FUNC_SWITCH_HR_RATE));
        childList2.add(new FuncBean(R.string.FUNC_OTA, FUNC_OTA));
        childList2.add(new FuncBean(R.string.FUNC_PUSH_CUSTOM_DIAL, FUNC_PUSH_CUSTOM_DIAL));
        childList2.add(new FuncBean(R.string.FUNC_PUSH_CUSTOM_DIAL_NEW, FUNC_PUSH_CUSTOM_DIAL_NEW));
        childList2.add(new FuncBean(R.string.FUNC_PUSH_CUSTOM_SPORT, FUNC_PUSH_CUSTOM_SPORT));
        childList2.add(new FuncBean(R.string.FUNC_GPS_SPORT_CMD, FUNC_GPS_SPORT_CMD));
        childList2.add(new FuncBean(R.string.FUNC_FLASH_WRITE_CMD, FUNC_FLASH_WRITE_CMD));
        childList2.add(new FuncBean(R.string.FUNC_COMPRESS_CMD, FUNC_COMPRESS_CMD));
        childList2.add(new FuncBean(R.string.FUNC_ONLINE_DIAL_PUSH, FUNC_ONLINE_DIAL_PUSH));
        childList2.add(new FuncBean(R.string.FUNC_PUSH_QLZ_DATA, FUNC_PUSH_QLZ_DATA));
        childList2.add(new FuncBean(R.string.FUNC_PUSH_MORE_CUSTOM_SPORT, FUNC_PUSH_MORE_SPORT));

        childList3.add(new FuncBean(R.string.FUNC_GET_HARDWARE_INFO, FUNC_GET_HARDWARE_INFO));
        childList3.add(new FuncBean(R.string.FUNC_GET_SYSTEM_INFO, FUNC_GET_SYSTEM_INFO));
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
        childList3.add(new FuncBean(R.string.FUNC_GET_MENTALSTRESS_RECORD, FUNC_GET_MENTALSTRESS_RECORD));
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
        childList3.add(new FuncBean(R.string.FUNC_GET_HRPS_DETAIL, FUNC_GET_HRPS_DETAIL));
        childList3.add(new FuncBean(R.string.FUNC_GET_SPORT_LIST_INFO, FUNC_GET_SPORT_LIST_INFO));
        childList3.add(new FuncBean(R.string.FUNC_GET_SYSTEM_FUNCTION_SWITCH, FUNC_GET_SYSTEM_FUNCTION_SWITCH));
        childList3.add(new FuncBean(R.string.FUNC_SET_AGPS_LOCATION, FUNC_SET_AGPS_LOCATION));
        childList3.add(new FuncBean(R.string.FUNC_SET_AGPS_DATA, FUNC_SET_AGPS_DATA));
        childList3.add(new FuncBean(R.string.FUNC_SET_SN_CMEI, FUNC_SET_SN_CMEI));
        childList3.add(new FuncBean(R.string.FUNC_NOTES_REMINDERS, FUNC_NOTES_REMINDERS));
        childList3.add(new FuncBean(R.string.FUNC_GET_DISK_SPACE_INFO, FUNC_GET_DISK_SPACE_INFO));
        childList3.add(new FuncBean(R.string.FUNC_GET_HS_FILE_LIST, FUNC_GET_HS_FILE_LIST));

//        childList4.add(new FuncBean(R.string.FUNC_GET_SEDENTARY_DRINK_PARA,FUNC_GET_SEDENTARY_DRINK_PARA));

        childList.add(childList1);
        childList.add(childList2);
        childList.add(childList3);
//        childList.add(childList4);
        return childList;
    }


    Date dateResult = null;


}
