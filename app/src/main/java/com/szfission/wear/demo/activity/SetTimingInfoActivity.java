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
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
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
//            FsLogUtil.d("??????at??????"+content);
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
                noUseId.setText("???????????????ID???"+ result);
            }
        });
    }
    TimePickerView pvTime;
    private void initData() {

        FissionSdkBleManage.getInstance().getNotUsingAlarmId();

//        AnyWear.getNoUseTimingInfo(new OnSmallDataCallback(){
//            @Override
//            public void OnStringResult(String content) {
//              noUseId.setText("???????????????ID???"+ content);
//            }
//        });

        initSpinner(1, R.array.index);
        initSpinner(2, R.array.police);
        List<Integer> index = new ArrayList<>();

        List<String> a = new ArrayList<String>();
        a.add("??????");
        a.add("??????");
        a.add("??????");
        a.add("??????");
        a.add("??????");
        a.add("??????");
        a.add("??????");
        a.add("?????????");
        Collections.reverse(a);
        final WeekAdapter adapter = new WeekAdapter(this, a);
        recycleWeek.setAdapter(adapter);

         pvTime = new TimePickerBuilder(this, new OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {
                String times = DateUtil.format(date, "yyyy???MM???dd??? HH???mm???ss???");
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
                showToast("???????????????" + errorMsg);
            }

            @Override
            public void getAlarm(List<FissionAlarm> fissionAlarms) {
                super.getAlarm(fissionAlarms);
                dismissProgress();
                for (int i = 0; i < fissionAlarms.size(); i++) {
                    FissionAlarm timingInfo = fissionAlarms.get(i);
                    content = new StringBuilder();
                    content.append("\n??????????????????").append(timingInfo.getBodyVersion());
                    content.append("\n?????????").append(timingInfo.getIndex());
                    content.append("\n??????????????????").append(timingInfo.isAlarmActive());
                    content.append("\n???????????????").append(timingInfo.getType());
                    content.append("\n???????????????").append(timingInfo.isOpen());
                    content.append("\n???????????????").append(timingInfo.getAlarmState());
                    content.append("\n?????????????????????").append(timingInfo.isAlarmDelayAlert());
                    content.append("\n???????????????").append(timingInfo.getWeekCode());
                    content.append("\n?????????????????????").append(timingInfo.getShakeType());
                    content.append("\n?????????").append(timingInfo.getYear()).append("  ???").append(timingInfo.getMonth()).append("  ???").append(timingInfo.getDay()).append("  ???").append(timingInfo.getHour()).append("  ???").append(timingInfo.getMinute()).append("  ???");
                    content.append("\n???????????????").append(timingInfo.getAlertCount());
                    content.append("\n??????????????????").append(timingInfo.getAlertedCount());
                    content.append("\n????????????????????????").append(timingInfo.getAlertIntervalTime());
                    content.append("\n??????????????????").append(timingInfo.getRemarkLength());
                    content.append("\n?????????").append(timingInfo.getRemark());
                    content.append("\n");
                    tvCmdContent.append(content);
                }
            }

            @Override
            public void setAlarmInfos() {
                super.setAlarmInfos();
                showToast("????????????");
            }
        });
    }


    int weekResult = 0;

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
//                    content.append("\n??????????????????").append(timingInfo.getBodyVersion());
//                    content.append("\n?????????").append(timingInfo.getIndex());
//                    content.append("\n??????????????????").append(timingInfo.isAlarmActive());
//                    content.append("\n???????????????").append(timingInfo.getType());
//                    content.append("\n???????????????").append(timingInfo.isOpen());
//                    content.append("\n???????????????").append(timingInfo.getAlarmState());
//                    content.append("\n?????????????????????").append(timingInfo.isAlarmDelayAlert());
//                    content.append("\n???????????????").append(timingInfo.getWeekCode());
//                    content.append("\n?????????????????????").append(timingInfo.getShakeType());
//                    content.append("\n?????????").append(timingInfo.getYear()).append("  ???").append(timingInfo.getMonth()).append("  ???").append(timingInfo.getDay()).append("  ???").append(timingInfo.getHour()).append("  ???").append(timingInfo.getMinute()).append("  ???");
//                    content.append("\n???????????????").append(timingInfo.getAlertCount());
//                    content.append("\n??????????????????").append(timingInfo.getAlertedCount());
//                    content.append("\n????????????????????????").append(timingInfo.getAlertIntervalTime());
//                    content.append("\n??????????????????").append(timingInfo.getRemarkLength());
//                    content.append("\n?????????").append(timingInfo.getRemark());
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
    //??????????????????
    int alarmLater = 0;


    @Event(R.id.btn_send)
    private void send(View v) {
//        String startTime = etStartTime.getText().toString();
//        String endTime = etEndTime.getText().toString();
//        if (startTime.isEmpty()) {
//            Toast.makeText(this, "?????????????????????", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (endTime.isEmpty()) {
//            Toast.makeText(this, "?????????????????????", Toast.LENGTH_SHORT).show();
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
        FissionAlarm alarmLw33 = new FissionAlarm(0,1,true,System.currentTimeMillis()+60000,weekResult);
        lw33s.add(alarmLw33);
        for (int i = 1;i<5;i++){
            lw33s.add(new FissionAlarm(i,1,true,System.currentTimeMillis()+i*120000,weekResult));
        }

        FissionSdkBleManage.getInstance().setAlarmInfos(lw33s);
//        AnyWear.setAlarmInfos(lw33s, new OnSmallDataCallback() {
//                    @Override
//                    public void OnError(String msg) {
//                        showToast("???????????????" + msg);
//
//                    }
//
//                    @Override
//                    public void OnEmptyResult() {
//                        showToast("????????????");
//
//                    }
//                });

    }

    StringBuilder content = null;
    SparseBooleanArray mSelectedPositions = new SparseBooleanArray();

    private void setItemChecked(int position, boolean isChecked) {
        mSelectedPositions.put(position, isChecked);
    }

    //????????????????????????????????????
    private boolean isItemChecked(int position) {
        return mSelectedPositions.get(position);
    }

    private ArrayList<String> mList = new ArrayList<>();

    //???????????????????????????
//    public ArrayList<Integer> getSelectedItem() {
//        ArrayList<Integer> selectList = new ArrayList<>();
//
//        FsLogUtil.d("????????????" + selectList.toString());
//        return selectList;
//    }

    Map<Integer, Integer> map = new HashMap<Integer, Integer>();

    class WeekAdapter extends RecyclerView.Adapter<WeekHolder> {
        private Context context;
        private List<String> list;
        private int defItem = -1;//?????????
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
//            FsLogUtil.d("??????????????????"+list.get(position));
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
