package com.szfission.wear.demo.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.szfission.wear.demo.R;
import com.szfission.wear.demo.bean.FuncBean;
import com.szfission.wear.sdk.bean.param.WeatherParam;

import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.List;

public class WeatherAdapter extends BaseAdapter {
    private Context context;
    private List<WeatherParam> funcBeans;

    public WeatherAdapter(Context context, List<WeatherParam> funcBeans) {
        this.context = context;
        this.funcBeans = funcBeans;

    }

    @Override
    public int getCount() {
        return funcBeans.size();
    }

    @Override
    public Object getItem(int i) {
        return funcBeans.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(context).inflate(R.layout.item_func, viewGroup, false);
            x.view().inject(holder, view);
            view.setTag(holder);
            holder.tvTitle = view.findViewById(R.id.tvTitle);
            holder.tvWeather = view.findViewById(R.id.tvWeather);
            holder.tvAir = view.findViewById(R.id.tvAir);
            holder.tvHigh = view.findViewById(R.id.tvHigh);
            holder.tvLow = view.findViewById(R.id.tvLow);
            holder.tvPm = view.findViewById(R.id.tvPm);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        String []spinnerItems = new String[]{"晴", "多云", "风", "阴天", "小雨", "大雨", "雪", "雷阵雨", "晴晚上", "多云晚上", "沙尘暴", "阵雨", "阵雨晚上", "雨夹雪", "雾霾", "未知天气"};
        String []spinnerItems1 = new String[]{"差", "好", "很好"};
        holder.tvTitle.setText("天数:"+funcBeans.get(i).getDay()+"");
        holder.tvWeather.setText("天气："+spinnerItems[funcBeans.get(i).getWeather()]+"");
        holder.tvAir.setText("空气："+spinnerItems1[funcBeans.get(i).getAirQuality()]+"");
        holder.tvHigh.setText("最高温度："+funcBeans.get(i).getMaximumTemperature()+"");
        holder.tvLow.setText("最低温度："+funcBeans.get(i).getLowestTemperature()+"");
        holder.tvPm.setText("PM2.5："+"PM_LEVEL"+funcBeans.get(i).getPm25()+"");


        return view;
    }

    class ViewHolder {
        TextView tvTitle;
        TextView tvWeather;
        TextView tvAir;
        TextView tvHigh;
        TextView tvLow;
        TextView tvPm;
    }

}
