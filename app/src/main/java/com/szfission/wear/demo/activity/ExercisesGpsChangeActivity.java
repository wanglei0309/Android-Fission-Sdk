package com.szfission.wear.demo.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.szfission.wear.demo.DataMessageEvent;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.adapter.ExercisesListAdapter;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.ExerciseDetail;
import com.szfission.wear.sdk.bean.ExerciseDetailRecord;
import com.szfission.wear.sdk.bean.ExerciseList;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnExerciseDetailCallback;
import com.szfission.wear.sdk.util.DateUtil;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.List;

/**
 * 运动记录列表
 */
public class ExercisesGpsChangeActivity extends BaseActivity {
    RecyclerView recyclerView;

    ExercisesListAdapter exercisesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_list);
        setTitle(R.string.FUNC_GET_EXERCISE_GPS);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.getList);

//        exercisesListAdapter = new ExercisesListAdapter(R.layout.adapter_exercies_list);
//        recyclerView.setAdapter(exercisesListAdapter);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//
//        Intent intent = getIntent();
//        Bundle bundle = intent.getExtras();
//        long startTime = bundle.getLong("startTime");
//        long endTime = bundle.getLong("endTime");
//        getData(startTime, endTime);




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

    //获取运动列表
    private void getData(long startTime, long endTime) {
        AnyWear.getExerciseList(startTime, endTime, new BigDataCallBack() {
            @Override
            public void OnExerciseListCallback(List<ExerciseList> exerciseLists) {

//                for (ExerciseList exerciseList:exerciseLists) {
//                    LogUtils.d("获取列表信息" + exerciseList.getModel());
//                }
            }

            @Override
            public void OnEmpty(String result) {
                LogUtils.d(result);
            }
        });



        exercisesListAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull @NotNull BaseQuickAdapter<?, ?> adapter, @NonNull @NotNull View view, int position) {
                showProgress();

            }
        });
    }

    void getDialog(List<ExerciseDetail> exerciseDetails) {
        AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(ExercisesGpsChangeActivity.this);
        final View dialogView = LayoutInflater.from(ExercisesGpsChangeActivity.this)
                .inflate(R.layout.dialog_customize, null);
        TextView tvContent = dialogView.findViewById(R.id.tvContent);
        StringBuilder content = new StringBuilder();
        for (ExerciseDetail exerciseDetail : exerciseDetails) {
            LogUtils.d("循环了即便啊啊啊啊");
            content.append("\n utc记录时间：").append(DateUtil.gmtToStrDate(exerciseDetail.getTime()));
            content.append("\n 结构体版本：").append(DateUtil.gmtToStrDate(exerciseDetail.getBodyVersion()));
            content.append("\n 记录生成周期：").append(exerciseDetail.getWeek());
            content.append("\n 有效记录条数：").append(exerciseDetail.getEffectiveNumber());
            content.append("\n 单条记录长度：").append(exerciseDetail.getRecordLength());
            content.append("\n 记录类型：").append(exerciseDetail.getType());
            for (ExerciseDetailRecord exerciseDetailRecord : exerciseDetail.getExerciseDetailRecords()) {
                content.append("\n 实时配速:").append(exerciseDetailRecord.getPace());
                content.append("\n 实时步频:").append(exerciseDetailRecord.getFrequency());
                content.append("\n 卡路里:").append(exerciseDetailRecord.getCalorie());
                content.append("\n 步数:").append(exerciseDetailRecord.getSteps());
                content.append("\n 距离:").append(exerciseDetailRecord.getDistance());
                content.append("\n 心率:").append(exerciseDetailRecord.getHeartRate());
                content.append("\n 实时体力:").append(exerciseDetailRecord.getStamina());
                content.append("\n 状态:").append(exerciseDetailRecord.getState());
                content.append("\n-------------------------------\n\n");
            }
            EventBus.getDefault().post(new DataMessageEvent(R.string.FUNC_GET_EXERCISE_DETAIL,content.toString()));
        }
        tvContent.setText(content);
//        customizeDialog.setTitle(content);
        customizeDialog.setView(dialogView);
        customizeDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 获取EditView中的输入内容
                        customizeDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                dialog.dismiss();
                            }
                        });
                    }
                });
        customizeDialog.show();
    }


}
