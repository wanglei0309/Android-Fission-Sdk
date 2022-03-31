package com.szfission.wear.demo.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.param.TodayWeatherDetail;
import com.szfission.wear.sdk.bean.param.WeatherParam;

import org.jetbrains.annotations.NotNull;

public class MultiWeatherAdapter extends BaseQuickAdapter<WeatherParam, BaseViewHolder> {

    public MultiWeatherAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder holder, WeatherParam todayWeatherDetail) {
        holder.setText(R.id.tvHigh,"最高温度："+todayWeatherDetail.getMaximumTemperature()+"");
        holder.setText(R.id.tvLow,"最低温度："+todayWeatherDetail.getLowestTemperature()+"");
        getWeather(holder,todayWeatherDetail.getWeather());
        if (todayWeatherDetail.getIndex() == 0){
            holder.setText(R.id.tvWeatherTitle,"昨天天气:");
        }else {
            holder.setText(R.id.tvWeatherTitle,"第: "+todayWeatherDetail.getIndex()+ " 天天气:");
        }

    }

    private void getWeather(BaseViewHolder holder, int weatherCode) {
        switch (weatherCode) {
            case 1:// 晴天
                holder.setText(R.id.tvWeather,"晴天");
                break;
            case 2:// 多云
                holder.setText(R.id.tvWeather,"多云");
                break;
            case 3:// 风
                holder.setText(R.id.tvWeather,"大风");
                break;
            case 4:// 阴天
                holder.setText(R.id.tvWeather,"阴天");
                break;
            case 5:// 阵雨
                holder.setText(R.id.tvWeather,"小雨");
                break;
            case 6:// 雷阵雨、雷阵雨伴有冰雹
                holder.setText(R.id.tvWeather,"大雨");
                break;
            case 7:// 小雨
                holder.setText(R.id.tvWeather,"中雪");
                break;
            case 8:// 中雨
                holder.setText(R.id.tvWeather,"雷阵雨");
                break;
            case 9:// 暴雨
                holder.setText(R.id.tvWeather,"夜间晴");
                break;
            case 10:// 夜间多云
                holder.setText(R.id.tvWeather,"夜间多云");
                break;
            case 11:// 沙尘暴
                holder.setText(R.id.tvWeather,"沙尘暴");
                break;
            case 12:// 阵雨
                holder.setText(R.id.tvWeather,"阵雨");
                break;
            case 13:// 夜间阵雨
                holder.setText(R.id.tvWeather,"夜间阵雨");
                break;
            case 14:// 雨夹雪
                holder.setText(R.id.tvWeather,"雨夹雪");
                break;
            case 15:// 雾霾
                holder.setText(R.id.tvWeather,"雾霾");
                break;
            case 16:// 小雪
                holder.setText(R.id.tvWeather,"小雪");
                break;
            case 17:// 大雪
                holder.setText(R.id.tvWeather,"大雪");
                break;
            case 18:// 未知天气
                holder.setText(R.id.tvWeather,"未知天气");
                break;
        }
    }
}
