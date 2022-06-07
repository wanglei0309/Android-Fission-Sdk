package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.param.TodayWeatherDetail;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;
import com.szfission.wear.sdk.util.FsLogUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.Random;

@ContentView(R.layout.activity_weather_detail)
public class SetWeatherDetailActivity extends BaseActivity {
    @ViewInject(R.id.btnData)
    Button btnData;
    @ViewInject(R.id.tvSingleWeather)
    TextView tvSingleWeather;
    @ViewInject(R.id.tvCurTmp)
    TextView tvCurTmp;
    @ViewInject(R.id.tvHighTmp)
    TextView tvHighTmp;
    @ViewInject(R.id.tvLowTmp)
    TextView tvLowTmp;
    @ViewInject(R.id.btn_send)
            Button btnSend;
    int lowTmp, highTmp, curTmp, weatherCode;
    Random random = new Random();
    int weatherCodes = 0;

    @ViewInject(R.id.spinnerTime)
    Spinner spinnerTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_WEATHER_DETAIL);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

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
                showToast("推送当日天气失败:"+errorMsg);
            }

            @Override
            public void setWeatherDetail() {
                super.setWeatherDetail();
                showToast("推送当日天气成功");
            }
        });

        btnData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData();

            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FissionSdkBleManage.getInstance().setSingleWeather(curTmp, highTmp, lowTmp, spinnerTime.getSelectedItemPosition(), "");
//                AnyWear.setSingleWeather(curTmp, highTmp, lowTmp, spinnerTime.getSelectedItemPosition(), "", new OnSmallDataCallback() {
//                    @Override
//                    public void OnEmptyResult() {
//                        showToast("推送当日天气成功");
//                    }
//
//                    @Override
//                    public void OnError(String msg) {
//                        showToast("推送当日天气失败"+msg);
//                    }
//                });
            }
        });


        String[] spinnerItems = {"晴天", "多云", "大风","阴天","小雨","大雨","中雪","雷阵雨","夜间晴","夜间多云","沙尘暴","阵雨","夜间阵雨","雨夹雪","雾霾","小雪","大雪","未知天气"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(spinnerAdapter);


        spinnerTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getWeather(position+1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getData() {
        lowTmp = getLowTemp(-30, 21);
        highTmp = getLowTemp(-10,40);
        curTmp = getLowTemp(-30,40);
        weatherCode = getLowTemp(1,18);
        weatherCode = getWeather(weatherCode);
        FsLogUtil.d("查询low" + lowTmp);
        tvLowTmp.setText("最低气温："+lowTmp );
        tvHighTmp.setText("最高气温："+highTmp );
        tvCurTmp.setText("当前气温："+curTmp );
        TodayWeatherDetail  todayWeatherDetail = new TodayWeatherDetail();
        todayWeatherDetail.setLowSetTmp(lowTmp);
        todayWeatherDetail.setHighSetTmp(highTmp);
        todayWeatherDetail.setTemperature(curTmp);
        todayWeatherDetail.setWeatherCode(weatherCode);

        FissionSdkBleManage.getInstance().setWeatherDetail(todayWeatherDetail);

//        AnyWear.setWeatherDetail(todayWeatherDetail, new OnSmallDataCallback() {
//            @Override
//            public void OnEmptyResult() {
//                showToast("推送当日天气成功");
//            }
//
//            @Override
//            public void OnError(String msg) {
//
//            }
//        });

    }

    private int getLowTemp(int small, int bignum) {
        int num = -1;
        num = random.nextInt(bignum)%(bignum - small + 1)+small ;  //产生幸运数
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


    private int getWeather(int weatherCode) {
        FsLogUtil.d("jskdajk j"+weatherCode);
        switch (weatherCode) {
            case 1:// 晴天
                weatherCodes = 0;
                tvSingleWeather.setText("当天天气:晴天");
                break;
            case 2:// 多云
                weatherCodes = 1;
                tvSingleWeather.setText("当天天气:多云");
                break;
            case 3:// 风
                weatherCodes = 2;
                tvSingleWeather.setText("当天天气:大风");
                break;
            case 4:// 阴天
                weatherCodes = 3;
                tvSingleWeather.setText("当天天气:阴天");
                break;
            case 5:// 阵雨
                weatherCodes = 4;
                tvSingleWeather.setText("当天天气:小雨");
                break;
            case 6:// 雷阵雨、雷阵雨伴有冰雹
                weatherCodes = 5;
                tvSingleWeather.setText("当天天气:大雨");
                break;
            case 7:// 小雨
                weatherCodes = 6;
                tvSingleWeather.setText("当天天气:中雪");
                break;
            case 8:// 中雨
                weatherCodes = 7;
                tvSingleWeather.setText("当天天气:雷阵雨");
                break;
            case 9:// 暴雨
                weatherCodes = 8;
                tvSingleWeather.setText("当天天气:夜间晴");
                break;
            case 10:// 夜间多云
                weatherCodes = 9;
                tvSingleWeather.setText("当天天气:夜间多云");
                break;
            case 11:// 沙尘暴
                weatherCodes = 10;
                tvSingleWeather.setText("当天天气:沙尘暴");
                break;
            case 12:// 阵雨
                weatherCodes = 11;
                tvSingleWeather.setText("当天天气:阵雨");
                break;
            case 13:// 夜间阵雨
                weatherCodes = 12;
                tvSingleWeather.setText("当天天气:夜间阵雨");
                break;
            case 14:// 雨夹雪
                weatherCodes = 13;
                tvSingleWeather.setText("当天天气:雨夹雪");
                break;
            case 15:// 雾霾
                weatherCodes = 14;
                tvSingleWeather.setText("当天天气:雾霾");
                break;
            case 16:// 小雪
                weatherCodes = 15;
                tvSingleWeather.setText("当天天气:小雪");
                break;
            case 17:// 大雪
                weatherCodes = 16;
                tvSingleWeather.setText("当天天气:大雪");
                break;
            case 18:// 未知天气
                weatherCodes = 255;
                tvSingleWeather.setText("当天天气:未知天气");
                break;
        }
        return weatherCodes;
    }
}
