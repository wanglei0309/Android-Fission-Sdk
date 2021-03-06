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
                showToast("????????????????????????:"+errorMsg);
            }

            @Override
            public void setWeatherDetail() {
                super.setWeatherDetail();
                showToast("????????????????????????");
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
//                        showToast("????????????????????????");
//                    }
//
//                    @Override
//                    public void OnError(String msg) {
//                        showToast("????????????????????????"+msg);
//                    }
//                });
            }
        });


        String[] spinnerItems = {"??????", "??????", "??????","??????","??????","??????","??????","?????????","?????????","????????????","?????????","??????","????????????","?????????","??????","??????","??????","????????????"};
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
        FsLogUtil.d("??????low" + lowTmp);
        tvLowTmp.setText("???????????????"+lowTmp );
        tvHighTmp.setText("???????????????"+highTmp );
        tvCurTmp.setText("???????????????"+curTmp );
        TodayWeatherDetail  todayWeatherDetail = new TodayWeatherDetail();
        todayWeatherDetail.setLowSetTmp(lowTmp);
        todayWeatherDetail.setHighSetTmp(highTmp);
        todayWeatherDetail.setTemperature(curTmp);
        todayWeatherDetail.setWeatherCode(weatherCode);

        FissionSdkBleManage.getInstance().setWeatherDetail(todayWeatherDetail);

//        AnyWear.setWeatherDetail(todayWeatherDetail, new OnSmallDataCallback() {
//            @Override
//            public void OnEmptyResult() {
//                showToast("????????????????????????");
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
        num = random.nextInt(bignum)%(bignum - small + 1)+small ;  //???????????????
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
            case 1:// ??????
                weatherCodes = 0;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 2:// ??????
                weatherCodes = 1;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 3:// ???
                weatherCodes = 2;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 4:// ??????
                weatherCodes = 3;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 5:// ??????
                weatherCodes = 4;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 6:// ?????????????????????????????????
                weatherCodes = 5;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 7:// ??????
                weatherCodes = 6;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 8:// ??????
                weatherCodes = 7;
                tvSingleWeather.setText("????????????:?????????");
                break;
            case 9:// ??????
                weatherCodes = 8;
                tvSingleWeather.setText("????????????:?????????");
                break;
            case 10:// ????????????
                weatherCodes = 9;
                tvSingleWeather.setText("????????????:????????????");
                break;
            case 11:// ?????????
                weatherCodes = 10;
                tvSingleWeather.setText("????????????:?????????");
                break;
            case 12:// ??????
                weatherCodes = 11;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 13:// ????????????
                weatherCodes = 12;
                tvSingleWeather.setText("????????????:????????????");
                break;
            case 14:// ?????????
                weatherCodes = 13;
                tvSingleWeather.setText("????????????:?????????");
                break;
            case 15:// ??????
                weatherCodes = 14;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 16:// ??????
                weatherCodes = 15;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 17:// ??????
                weatherCodes = 16;
                tvSingleWeather.setText("????????????:??????");
                break;
            case 18:// ????????????
                weatherCodes = 255;
                tvSingleWeather.setText("????????????:????????????");
                break;
        }
        return weatherCodes;
    }
}
