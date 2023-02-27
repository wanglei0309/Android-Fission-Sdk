package com.szfission.wear.demo.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.blankj.utilcode.util.CacheDoubleUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.FissionAlarmCache;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.fission.wear.sdk.v2.constant.CacheDoubleKey;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.FissionAlarm;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;
import com.szfission.wear.sdk.util.DateUtil;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressLint("NonConstantResourceId")
@ContentView(R.layout.activity_set_timing_info)
public class SetTimingInfoActivity extends BaseActivity {
    @ViewInject(R.id.switch_open)
    Switch switchOpen;
    @ViewInject(R.id.tvCmdContent)
    TextView tvCmdContent;
    @ViewInject(R.id.noUseId)
    TextView noUseId;
    @ViewInject(R.id.recycleWeek)
    RecyclerView recycleWeek;
    @ViewInject(R.id.editTextIndex)
    Spinner spinnerType;
    @ViewInject(R.id.spinner_110)
    Spinner spinner_110;
    @ViewInject(R.id.radioGroup)
    RadioGroup radioGroup;
    @ViewInject(R.id.radioGroup_notify)
    RadioGroup radioGroupNotify;
    @ViewInject(R.id.radioBtn1)
    RadioButton radioBtn1;
    @ViewInject(R.id.radioBtn2)
    RadioButton radioBtn2;
    @ViewInject(R.id.radioBtn3)
    RadioButton radioBtn3;
    @ViewInject(R.id.timing_switch)
    Switch timingSwitch;
    @ViewInject(R.id.tvDateResult)
    TextView tvDateResult;
    @ViewInject(R.id.tv_set_time)
    Button tvSetTime;
    @ViewInject(R.id.editRemark)
    EditText editRemark;
    int year, month, day, hour, minute;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_SET_TIMING_INFO);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recycleWeek.setLayoutManager(linearLayoutManager);
//        AnyWear.getNoUseTimingInfo(new OnStringCallback() {
//            @Override
//            public void OnSuccess(String content) {
//            FsLogUtil.d("拿到at指令"+content);
//            }
//
//            @Override
//            public void OnError(String msg) {
//
//            }
//        });
        initData();
        FissionSdkBleManage.getInstance().addCmdResultListener(new FissionAtCmdResultListener() {
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

            }

            @Override
            public void getNotUsingAlarmId(String result) {
                super.getNotUsingAlarmId(result);
                noUseId.setText("未使用闹铃ID："+ result);
            }
        });
    }
    TimePickerView pvTime;
    private void initData() {
//        FissionAlarmCache fissionAlarmCache = new FissionAlarmCache();
//        fissionAlarmCache.setFissionAlarms(new ArrayList<FissionAlarm>());
//        CacheDoubleUtils.getInstance().put(CacheDoubleKey.CD_KEY_ALARM_CACHE, fissionAlarmCache);

        FissionSdkBleManage.getInstance().getAlarm();

//        AnyWear.getNoUseTimingInfo(new OnSmallDataCallback(){
//            @Override
//            public void OnStringResult(String content) {
//              noUseId.setText("未使用闹铃ID："+ content);
//            }
//        });

        initSpinner(1, R.array.index);
        initSpinner(2, R.array.police);
        List<Integer> index = new ArrayList<>();

        List<String> a = new ArrayList<String>();
        a.add("周日");
        a.add("周一");
        a.add("周二");
        a.add("周三");
        a.add("周四");
        a.add("周五");
        a.add("周六");
        a.add("每一天");
        Collections.reverse(a);
        final WeekAdapter adapter = new WeekAdapter(this, a);
        recycleWeek.setAdapter(adapter);

         pvTime = new TimePickerBuilder(this, new OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {
                String times = DateUtil.format(date, "yyyy年MM月dd日 HH时mm分ss秒");
                tvDateResult.setText(times);
                year = Integer.parseInt(times.substring(0, 4));
                month = Integer.parseInt(times.substring(5, 7));
                day = Integer.parseInt(times.substring(8, 10));
                hour = Integer.parseInt(times.substring(12, 14));
                minute = Integer.parseInt(times.substring(15, 17));
            }
        }).setType(new boolean[]{true, true, true, true, true, false}).build();
     for (int i = 7;i>0;i--){
         map.put(i,9);
     }
        final StringBuffer abc = new StringBuffer("00000000");
        adapter.setOnItemListener(new OnItemListener() {
            @Override
            public void onClick(View v, int pos, String projectc) {
                adapter.setDefSelect(pos);
                StringBuffer strAbc;
                  if (map.get(pos)!=9){
                   strAbc  = abc.replace(pos, pos + 1, "1");
                  }else {
                      strAbc  = abc.replace(pos, pos + 1, "0");

                }
                weekResult = Integer.valueOf(abc.toString(), 2);
            }
//              else   if (pos ==1) weekResult = StringUtil.BitToByte("01000000");
//                if (pos == 0) weekResult = StringUtil.BitToByte("10000000");
//                if (pos == 0) weekResult = StringUtil.BitToByte("10000000");
//                if (pos == 0) weekResult = StringUtil.BitToByte("10000000");
//                if (pos == 0) weekResult = StringUtil.BitToByte("10000000");


        });

        tvSetTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTimeSelect();
            }
        });

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
                showToast("设置失败：" + errorMsg);
            }

            @Override
            public void getAlarm(List<FissionAlarm> fissionAlarms) {
                super.getAlarm(fissionAlarms);
                dismissProgress();
                for (int i = 0; i < fissionAlarms.size(); i++) {
                    FissionAlarm timingInfo = fissionAlarms.get(i);
                    content = new StringBuilder();
                    content.append("\n结构体版本：").append(timingInfo.getBodyVersion());
                    content.append("\n序号：").append(timingInfo.getIndex());
                    content.append("\n闹钟有效性：").append(timingInfo.isAlarmActive());
                    content.append("\n闹铃类别：").append(timingInfo.getType());
                    content.append("\n使能开关：").append(timingInfo.isOpen());
                    content.append("\n报警状态：").append(timingInfo.getAlarmState());
                    content.append("\n是否稍后提醒：").append(timingInfo.isAlarmDelayAlert());
                    content.append("\n周期掩码：").append(timingInfo.getWeekCode());
                    content.append("\n提醒震动方式：").append(timingInfo.getShakeType());
                    content.append("\n时间：").append(timingInfo.getYear()).append("  年").append(timingInfo.getMonth()).append("  月").append(timingInfo.getDay()).append("  日").append(timingInfo.getHour()).append("  时").append(timingInfo.getMinute()).append("  分");
                    content.append("\n提醒次数：").append(timingInfo.getAlertCount());
                    content.append("\n已提醒次数：").append(timingInfo.getAlertedCount());
                    content.append("\n再次提醒的间隔：").append(timingInfo.getAlertIntervalTime());
                    content.append("\n描述的长度：").append(timingInfo.getRemarkLength());
                    content.append("\n描述：").append(timingInfo.getRemark());
                    content.append("\n");
                    tvCmdContent.append(content);
                }
            }

            @Override
            public void setAlarmInfos() {
                super.setAlarmInfos();
                showToast("设置成功");
            }
        });
    }


    int weekResult = 128;  //默认只响一次

    private void initSpinner(int i, int index) {
        String[] mItems = getResources().getStringArray(index);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mItems);
        if (i == 1) {
            spinnerType.setAdapter(adapter);
        } else if (i == 2) {
            spinner_110.setAdapter(adapter);
        }
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

    @Event(R.id.btn_get)
    private void get(View v) {
        FissionSdkBleManage.getInstance().getAlarm();
//        AnyWear.getAlarm(new BigDataCallBack() {
//
//            @Override
//            public void OnAlarmCallBack(List<FissionAlarm> fissionAlarms) {
//                dismissProgress();
//                for (int i = 0; i < fissionAlarms.size(); i++) {
//                    FissionAlarm timingInfo = fissionAlarms.get(i);
//                    content = new StringBuilder();
//                    content.append("\n结构体版本：").append(timingInfo.getBodyVersion());
//                    content.append("\n序号：").append(timingInfo.getIndex());
//                    content.append("\n闹钟有效性：").append(timingInfo.isAlarmActive());
//                    content.append("\n闹铃类别：").append(timingInfo.getType());
//                    content.append("\n使能开关：").append(timingInfo.isOpen());
//                    content.append("\n报警状态：").append(timingInfo.getAlarmState());
//                    content.append("\n是否稍后提醒：").append(timingInfo.isAlarmDelayAlert());
//                    content.append("\n周期掩码：").append(timingInfo.getWeekCode());
//                    content.append("\n提醒震动方式：").append(timingInfo.getShakeType());
//                    content.append("\n时间：").append(timingInfo.getYear()).append("  年").append(timingInfo.getMonth()).append("  月").append(timingInfo.getDay()).append("  日").append(timingInfo.getHour()).append("  时").append(timingInfo.getMinute()).append("  分");
//                    content.append("\n提醒次数：").append(timingInfo.getAlertCount());
//                    content.append("\n已提醒次数：").append(timingInfo.getAlertedCount());
//                    content.append("\n再次提醒的间隔：").append(timingInfo.getAlertIntervalTime());
//                    content.append("\n描述的长度：").append(timingInfo.getRemarkLength());
//                    content.append("\n描述：").append(timingInfo.getRemark());
//                    content.append("\n");
//                    tvCmdContent.append(content);
//                }
//            }
//
//            @Override
//            public void OnEmpty(String cmdId) {
//
//            }
//
//            @Override
//            public void OnError(String msg) {
//
//            }
//        });
        tvCmdContent.setText("");
        showProgress();
    }

    int alarmType = 0;
    //是否稍后提醒
    int alarmLater = 0;


    @Event(R.id.btn_send)
    private void send(View v) {
//        String startTime = etStartTime.getText().toString();
//        String endTime = etEndTime.getText().toString();
//        if (startTime.isEmpty()) {
//            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (endTime.isEmpty()) {
//            Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        showProgress();
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == radioBtn1.getId()) {
                    alarmType = 0;
                } else {
                    alarmType = 1;
                }
            }
        });
        radioGroupNotify.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == radioBtn3.getId()) {
                    alarmLater = 0;
                } else {
                    alarmLater = 1;
                }
            }
        });
        List<FissionAlarm> lw33s = new ArrayList<>();
        FissionAlarm alarmLw33 = new FissionAlarm(0,1,true,System.currentTimeMillis()+60000,weekResult, "起床");
        lw33s.add(alarmLw33);
        for (int i = 1;i<5;i++){
            lw33s.add(new FissionAlarm(i,1,true,System.currentTimeMillis()+i*120000,weekResult, "开会这名字够长了吧"+i));
        }

        FissionSdkBleManage.getInstance().setAlarmInfos(lw33s);
//        AnyWear.setAlarmInfos(lw33s, new OnSmallDataCallback() {
//                    @Override
//                    public void OnError(String msg) {
//                        showToast("设置失败：" + msg);
//
//                    }
//
//                    @Override
//                    public void OnEmptyResult() {
//                        showToast("设置成功");
//
//                    }
//                });

    }

    @Event(R.id.btn_send2)
    private void send2(View v) {
        List<FissionAlarm> lw33s = new ArrayList<>();
        FissionAlarm alarmLw33 = new FissionAlarm(0,1,true,System.currentTimeMillis()+60000,weekResult, "起床");
        lw33s.add(alarmLw33);
        for (int i = 1;i<10;i++){
            if(i == 9){
                lw33s.add(new FissionAlarm(i,0,false,System.currentTimeMillis()+i*120000,weekResult, "开会这名字够长了吧，测试字符串截取功能。"+i));
            }else{
                lw33s.add(new FissionAlarm(i,1,true,System.currentTimeMillis()+i*120000,weekResult, "开会这名字够长了吧，测试字符串截取功能。"+i));
            }
        }

        FissionSdkBleManage.getInstance().setAlarmInfos(lw33s);
    }

    @Event(R.id.btn_add)
    private void add(View v) {
        FissionAlarm alarmLw33 = new FissionAlarm(1,true,System.currentTimeMillis()+60000,weekResult, "开会这名字够长了吧，测试字符串截取功能。");
        FissionSdkBleManage.getInstance().addFissionAlarm(alarmLw33, 10);
    }

    @Event(R.id.btn_delete)
    private void delete(View v) {
        FissionAlarm alarmLw33 = new FissionAlarm(0,0,false,0,weekResult, "起床");
        FissionSdkBleManage.getInstance().deleteFissionAlarm(alarmLw33, 10);
    }

    @Event(R.id.btn_update)
    private void update(View v) {
        FissionAlarm alarmLw33 = new FissionAlarm(Integer.parseInt(spinnerType.getSelectedItem().toString()),1,true,System.currentTimeMillis()+120000,weekResult, "起床");
        FissionSdkBleManage.getInstance().updateFissionAlarm(alarmLw33, 10);
    }


    StringBuilder content = null;
    SparseBooleanArray mSelectedPositions = new SparseBooleanArray();

    private void setItemChecked(int position, boolean isChecked) {
        mSelectedPositions.put(position, isChecked);
    }

    //根据位置判断条目是否选中
    private boolean isItemChecked(int position) {
        return mSelectedPositions.get(position);
    }

    private ArrayList<String> mList = new ArrayList<>();

    //获得选中条目的结果
//    public ArrayList<Integer> getSelectedItem() {
//        ArrayList<Integer> selectList = new ArrayList<>();
//
//        FsLogUtil.d("拿到这个" + selectList.toString());
//        return selectList;
//    }

    Map<Integer, Integer> map = new HashMap<Integer, Integer>();

    class WeekAdapter extends RecyclerView.Adapter<WeekHolder> {
        private Context context;
        private List<String> list;
        private int defItem = -1;//默认值
        public OnItemListener onItemListener;

        public void setOnItemListener(OnItemListener onItemListener) {
            this.onItemListener = onItemListener;
        }

        public void setDefSelect(int position) {
            this.defItem = position;
            notifyDataSetChanged();
        }

        public WeekAdapter(Context context, List<String> list) {
            this.context = context;
            this.list = list;
        }


        @NonNull
        @Override
        public WeekHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(context).inflate(R.layout.adapter_timing_item, viewGroup, false);
            return new WeekHolder(view);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onBindViewHolder(@NonNull final WeekHolder viewHolder, @SuppressLint("RecyclerView") final int position) {
//            FsLogUtil.d("那得看是多少"+list.get(position));
            viewHolder.title.setText(list.get(position));
            if (isItemChecked(position)) {
                viewHolder.title.setTextColor(getColor(R.color.colorAccent));
            } else {
                viewHolder.title.setTextColor(getColor(R.color.bg_click_pressed));
            }
//            if (defItem!=-1){
//                if (defItem == position){
//                    viewHolder.title.setTextColor(getColor(R.color.colorAccent));
//                }else {
//                    viewHolder.title.setTextColor(getColor(R.color.bg_click_pressed));
//                }
//            }
            viewHolder.title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemListener != null) {
                        if (isItemChecked(position)) {
                            map.put(position, 9);
                            setItemChecked(position, false);
                        } else {
                            map.put(position, position);
                            setItemChecked(position, true);
                        }
                        notifyItemChanged(position);
                        onItemListener.onClick(v, viewHolder.getLayoutPosition(), list.get(viewHolder.getLayoutPosition()));
                    }
                }
            });
        }


        @Override
        public int getItemCount() {
            return list.size();
        }


    }


    public interface OnItemListener {
        void onClick(View v, int pos, String projectc);
    }


    static class WeekHolder extends RecyclerView.ViewHolder {
        TextView title;

        public WeekHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvItemTiming);
        }
    }


    public void getTimeSelect() {
        pvTime.show();
    }

}
