package com.szfission.wear.demo.adapter;

import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.bean.ExerciseReport;
import com.szfission.wear.sdk.util.DateUtil;

import org.jetbrains.annotations.NotNull;

public class ExercisesListAdapter extends BaseQuickAdapter<ExerciseReport, BaseViewHolder> {

    public ExercisesListAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder holder, ExerciseReport exerciseReport) {
        holder.setText(R.id.curUtc,"数据生成时间："+ DateUtil.gmtToStrDate(exerciseReport.getUtcTime()));
        holder.setText(R.id.tvStartTime,"开始时间："+ DateUtil.gmtToStrDate(exerciseReport.getBeginTime()));
        holder.setText(R.id.tvEndTime,"结束时间："+ DateUtil.gmtToStrDate(exerciseReport.getEndTime()));
        getSportMode(holder,exerciseReport.getModel());
        holder.setText(R.id.tvSportTime,"运动总时间："+ exerciseReport.getTotalTime() +"秒");
        holder.setText(R.id.tvSportStep,"运动总步数："+ exerciseReport.getTotalStep() +"步");
        holder.setText(R.id.tvAvgHr,"运动平均心率："+ exerciseReport.getAvgHR() +"次");
        holder.setText(R.id.tvSportCalorie,"运动总卡路里："+ exerciseReport.getTotalCalorie() +"KCAL");
        holder.setText(R.id.tvSportDistance,"运动总距离："+exerciseReport.getTotalDistance() +"m");
        holder.setText(R.id.tvSportAvgStride,"运动平均步频："+exerciseReport.getAvgStride() +"m");
        holder.setText(R.id.tvSportSpeed,"运动平均速度："+exerciseReport.getAvgSpeed() +"m");
        holder.setText(R.id.tvTotalTrackDistance,"本次运动轨迹距离："+exerciseReport.getTotalTrackDistance() +" m");
           String peisu = DateUtil.getMinuteSecond((long) exerciseReport.getNotTrackAvgSpeed());
        holder.setText(R.id.tvNotTrackDistance,"本次无轨迹运动平均配速："+peisu +" KM/h");
        holder.setText(R.id.tvHasTrackDistance,"本次有轨迹运动平均配速："+exerciseReport.getHasTrackAvgSpeed() +" KM/h");

        float distance =(float) exerciseReport.getTotalDistance()/1000;
        float cauPace = exerciseReport.getTotalTime() / distance;
        String pacc = DateUtil.getMinuteSecond(cauPace);
        LogUtils.d("石达开大家"+exerciseReport.getTotalTime()+"求一个配速"+cauPace,"爬上山坡"+pacc);

        holder.setText(R.id.tvWarmupTime,"热身运动时间："+exerciseReport.getWarmUpEsTime() +" 秒");
        holder.setText(R.id.tvFatTime,"燃脂运动时间："+exerciseReport.getFatBurningTime() +" 秒");
        holder.setText(R.id.tvAeTime,"有氧耐力运动时间："+exerciseReport.getAerobicEnduranceTime() +" 秒");
        holder.setText(R.id.tvHighAeTime,"高强有氧耐力运动时间："+exerciseReport.getHighAerobicEnduranceTime() +" 秒");
        holder.setText(R.id.tvAnTime,"无氧运动时间："+exerciseReport.getAnaerobicTime() +" 秒");




    }

    private void getSportMode(BaseViewHolder holder, int model) {
        switch (model) {
            case 0://健走
                holder.setText(R.id.tvMode,"健走");
                break;
            case 1:// 晴天
                holder.setText(R.id.tvMode,"跑步");
                break;
            case 2:// 多云
                holder.setText(R.id.tvMode,"登山");
                break;
            case 3:// 风
                holder.setText(R.id.tvMode,"骑行");
                break;
            case 4:// 阴天
                holder.setText(R.id.tvMode,"足球");
                break;
            case 5:// 阵雨
                holder.setText(R.id.tvMode,"游泳");
                break;
            case 6:// 雷阵雨、雷阵雨伴有冰雹
                holder.setText(R.id.tvMode,"篮球");
                break;
            case 7:// 小雨
                holder.setText(R.id.tvMode,"无指定");
                break;
            case 8:// 中雨
                holder.setText(R.id.tvMode,"户外跑步");
                break;
            case 9:// 暴雨
                holder.setText(R.id.tvMode,"室内跑步");
                break;
            case 10:// 夜间多云
                holder.setText(R.id.tvMode,"减脂跑步");
                break;
            case 11:// 沙尘暴
                holder.setText(R.id.tvMode,"户外健走");
                break;
            case 12:// 阵雨
                holder.setText(R.id.tvMode,"室内健走");
                break;
            case 13:// 夜间阵雨
                holder.setText(R.id.tvMode,"户外骑行");
                break;
            case 14:// 雨夹雪
                holder.setText(R.id.tvMode,"室内骑行");
                break;
            case 15:// 雾霾
                holder.setText(R.id.tvMode,"自由训练");
                break;
            case 16:// 小雪
                holder.setText(R.id.tvMode,"健身训练");
                break;
            case 17:// 大雪
                holder.setText(R.id.tvMode,"羽毛球");
                break;
            case 18:// 未知天气
                holder.setText(R.id.tvMode,"排球");
                break;
            case 19:// 未知天气
                holder.setText(R.id.tvMode,"兵乓球");
                break;
            case 20:// 未知天气
                holder.setText(R.id.tvMode,"椭圆机");
                break;
            case 21:// 未知天气
                holder.setText(R.id.tvMode,"划船机");
                break;
            case 22:// 未知天气
                holder.setText(R.id.tvMode,"瑜伽");
                break;
            case 23:// 未知天气
                holder.setText(R.id.tvMode,"力量训练");
                break;
            case 24:// 未知天气
                holder.setText(R.id.tvMode,"板球");
                break;
            case 25:// 未知天气
                holder.setText(R.id.tvMode,"跳绳");
                break;
            case 26:// 未知天气
                holder.setText(R.id.tvMode,"有氧运动");
                break;
            case 27:// 未知天气
                holder.setText(R.id.tvMode,"健身舞");
                break;
            case 28:// 未知天气
                holder.setText(R.id.tvMode,"太极拳");
                break;
            case 29:// 未知天气
                holder.setText(R.id.tvMode,"自动识别跑步运动");
                break;
            case 30:// 未知天气
                holder.setText(R.id.tvMode,"自动识别健走运动");
                break;
        }
    }
}
