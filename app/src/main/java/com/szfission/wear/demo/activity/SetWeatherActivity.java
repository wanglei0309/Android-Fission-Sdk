package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.adapter.MultiWeatherAdapter;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.param.WeatherParam;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;
import com.szfission.wear.sdk.util.FsLogUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 设置天气
 */
@ContentView(R.layout.activity_set_weather)
public class SetWeatherActivity extends BaseActivity {

    @ViewInject(R.id.spinnerTime)
    Spinner spinnerTime;

    @ViewInject(R.id.recycleWeather)
    RecyclerView recycleWeather;
    @ViewInject(R.id.suiji)
    Button suiji;
    MultiWeatherAdapter weatherAdapter;
//    @ViewInject(R.id.spinnerAirQuality)
//    Spinner spinnerAirQuality;
//
//    @ViewInject(R.id.spinnerPm25)
//    Spinner spinnerPm25;
//
//    @ViewInject(R.id.etMin)
//    EditText etMin;
//
//    @ViewInject(R.id.etMax)
//    EditText etMax;
List<WeatherParam> weatherParams = new ArrayList<>();
    List<WeatherParam>todayWeatherDetails = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_WEATHER);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String[] spinnerItems = {"第一天", "第二天", "第三天","第四天","第五天","第六天","第七天","第八天"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(spinnerAdapter);

        spinnerItems = new String[]{"晴", "多云", "风", "阴天", "小雨", "大雨", "雪", "雷阵雨", "晴晚上", "多云晚上", "沙尘暴", "阵雨", "阵雨晚上", "雨夹雪", "雾霾","小雪", "大雪", "中雨", "暴雨", "未知天气"};
        spinnerAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerWeather.setAdapter(spinnerAdapter);

        spinnerItems = new String[]{"差", "好", "很好"};
        spinnerAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerAirQuality.setAdapter(spinnerAdapter);

        spinnerItems = new String[]{"PM_LEVEL1", "PM_LEVEL2", "PM_LEVEL3", "PM_LEVEL4", "PM_LEVEL5"};
        spinnerAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerPm25.setAdapter(spinnerAdapter);

        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionBigDataCmdResultListener() {
            @Override
            public void sendSuccess(String cmdId) {

            }

            @Override
            public void sendFail(String cmdId) {

            }

            @Override
            public void onResultTimeout(String cmdId) {

            }

            @Override
            public void onResultError(String errorMsg) {
                showToast(errorMsg);
                dismissProgress();
            }

            @Override
            public void setWeather() {
                super.setWeather();
                dismissProgress();
                showSuccessToast();
            }
        });

         weatherAdapter = new MultiWeatherAdapter(R.layout.item_func);
        recycleWeather.setLayoutManager(new LinearLayoutManager(this));
      recycleWeather.setAdapter(weatherAdapter);
        suiji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             setData();
            }
        });


        for (int i = 0;i<8;i++){
            WeatherParam detail = new WeatherParam();
            detail.setLowestTemperature(getRandom(-30,20));
            detail.setMaximumTemperature(getRandom(-10,40));
            detail.setIndex(i);
            detail.setWeather(getRandom(0,18));
            todayWeatherDetails.add(detail);
            weatherAdapter.addData(detail);
        }
        if (todayWeatherDetails.size()>0) {
            FissionSdkBleManage.getInstance().setWeather(todayWeatherDetails);
//            AnyWear.setWeather(todayWeatherDetails, new OnSmallDataCallback() {
//                @Override
//                public void OnEmptyResult() {
//
//                }
//
//                @Override
//                public void OnError(String msg) {
//
//                }
//            });
        }
    }

    private void setData() {
        todayWeatherDetails.clear();
        weatherAdapter.getData().clear();
        for (int i = 0;i<8;i++){
            WeatherParam detail = new WeatherParam();
            detail.setLowestTemperature(getRandom(-30,20));
            detail.setMaximumTemperature(getRandom(-10,40));
            detail.setIndex(i);
            detail.setWeather(getRandom(1,18));
            todayWeatherDetails.add(detail);
            weatherAdapter.addData(detail);
        }
        weatherAdapter.notifyDataSetChanged();
        FissionSdkBleManage.getInstance().setWeather(todayWeatherDetails);
//        AnyWear.setWeather(todayWeatherDetails, new OnSmallDataCallback() {
//            @Override
//            public void OnEmptyResult() {
//                FsLogUtil.d("设置天气成功");
//            }
//
//            @Override
//            public void OnError(String msg) {
//
//            }
//        });
    }

    Random random = new Random();
    private int getRandom(int small, int bigNum) {
        int num = -1;
        num = random.nextInt(bigNum)%(bigNum - small + 1)+small ;  //产生幸运数
        if(num ==17){
            num = 255;
        }
        return num;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Event(R.id.btn_send)
    private void send(View v) {
        int time = spinnerTime.getSelectedItemPosition();
        int weather = 2;
        int airQuality = 3;
        int pm25 = 1;
        String min = "12";
        String max = "33";
        FsLogUtil.d("time:" + time + " weather:" + weather + " airQuality:" + airQuality + " pm25:" + pm25 + " min:" + min + " max:" + max);
//        if (min.isEmpty()) {
//            Toast.makeText(this, "请输入最低温度", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (Integer.parseInt(min) > 255) {
//            Toast.makeText(this, "最低温度过大", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (max.isEmpty()) {
//            Toast.makeText(this, "请输入最高温度", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (Integer.parseInt(max) > 255) {
//            Toast.makeText(this, "最高温度过大", Toast.LENGTH_SHORT).show();
//            return;
//        }






//        showProgress();
//        AnyWear.setTest();
        FissionSdkBleManage.getInstance().setWeather(weatherParams);
//        AnyWear.setWeather(weatherParams,new OnSmallDataCallback() {
//            @Override
//            public void OnEmptyResult() {
//                dismissProgress();
//                showSuccessToast();
//            }
//
//            @Override
//            public void OnError(String msg) {
//                showToast(msg);
//                dismissProgress();
//            }
//        });
    }




}
