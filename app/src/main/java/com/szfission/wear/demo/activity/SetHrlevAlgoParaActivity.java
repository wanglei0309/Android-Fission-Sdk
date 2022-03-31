package com.szfission.wear.demo.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;

import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener;
import com.szfission.wear.demo.R;
import com.szfission.wear.sdk.AnyWear;
import com.szfission.wear.sdk.bean.param.HrRateLevel;
import com.szfission.wear.sdk.ifs.BigDataCallBack;
import com.szfission.wear.sdk.ifs.OnSmallDataCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.util.Objects;

/**
 * 心率等级判定参数
 */
@ContentView(R.layout.activity_set_hrlev_algo_para)
public class SetHrlevAlgoParaActivity extends BaseActivity  {
    @ViewInject(R.id.etModerate)
    EditText etModerate;

    @ViewInject(R.id.etVigorous)
    EditText etVigorous;

    @ViewInject(R.id.etMaxHr2)
    EditText etMaxHr2;

    @ViewInject(R.id.etMaxHr)
    EditText etMaxHr;


    @ViewInject(R.id.etHighHr)
    EditText etHighHr;

    @ViewInject(R.id.etNewLevel)
    EditText etNewLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.FUNC_SET_HRLEV_ALGO_PARA);
        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        getData();
        showProgress();

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
            public void getHrRateLevelPara(HrRateLevel hrRateLevel) {
                super.getHrRateLevelPara(hrRateLevel);
                dismissProgress();
                showSuccessToast(R.string.FUNC_SET_HRLEV_ALGO_PARA);
                String content = "\n最大心率参数：" + hrRateLevel.getOverMaxHr() +
                        "\n适中心率参数：" + hrRateLevel.getModerate() +
                        "\n较大心率参数：" + hrRateLevel.getVigorous() +
                        "\n最大心率参数：" + hrRateLevel.getMaxHr() +
                        "\n最高心率值：" + hrRateLevel.getHighestHr() +
                        "\n新等级：" + hrRateLevel.getHrTimeLimit();
                addLog(R.string.FUNC_GET_SEDENTARY_PARA, content);
                etVigorous.setText(String.valueOf(hrRateLevel.getVigorous()));
                etMaxHr2.setText(String.valueOf(hrRateLevel.getMaxHr()));
                etMaxHr.setText(String.valueOf(hrRateLevel.getOverMaxHr()));
                etModerate.setText(String.valueOf(hrRateLevel.getModerate()));
                etHighHr.setText(String.valueOf(hrRateLevel.getHighestHr()));
                etNewLevel.setText(String.valueOf(hrRateLevel.getHrTimeLimit()));
            }

            @Override
            public void setHrlevAlgoPara() {
                super.setHrlevAlgoPara();
                dismissProgress();
                showSuccessToast();
            }
        });
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
    private void get(View v){
      getData();
        showProgress();
    }

    @Event(R.id.btn_send)
    private void send(View v) {
        String vigorous = etVigorous.getText().toString();
        String maxHr2 = etMaxHr2.getText().toString();
        String maxHr = etMaxHr.getText().toString();
        String moderate = etModerate.getText().toString();
        String highHr = this.etHighHr.getText().toString();
        String newLevelHr = etNewLevel.getText().toString();
//        if (startTime.isEmpty()) {
//            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (endTime.isEmpty()) {
//            Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (keepTime.isEmpty()) {
//            Toast.makeText(this, "请输入持续检测时间", Toast.LENGTH_SHORT).show();
//            return;
//        }
        showProgress();
        HrRateLevel hrRateLevel = new HrRateLevel();
        hrRateLevel.setOverMaxHr(Integer.parseInt(maxHr));
        hrRateLevel.setModerate(Integer.parseInt(moderate));
        hrRateLevel.setVigorous(Integer.parseInt(vigorous));
        hrRateLevel.setMaxHr(Integer.parseInt(maxHr2));
        hrRateLevel.setHighestHr(Integer.parseInt(highHr));
        hrRateLevel.setHrTimeLimit(Integer.parseInt(newLevelHr));
        FissionSdkBleManage.getInstance().setHrlevAlgoPara(hrRateLevel);
//        AnyWear.setHrlevAlgoPara(hrRateLevel,new OnSmallDataCallback(){
//
//            @Override
//            public void OnError(String msg) {
//                showToast(msg);
//                dismissProgress();
//            }
//
//            @Override
//            public void OnEmptyResult() {
//                addLog(R.string.FUNC_SET_HRLEV_ALGO_PARA, "设置成功");
//                dismissProgress();
//                showSuccessToast();
//            }
//        });

    }

//    @Override
//    public void success(boolean enable, int beginTime, int endTime, int detectionTime, int targetSteps) {
//        dismissProgress();
//        showSuccessToast(R.string.sedentary_para);
//        StringBuilder content = new StringBuilder();
//        content.append("\n久坐提醒开关：").append(enable);
//        content.append("\n检测起始时间：").append(beginTime);
//        content.append("\n检测结束时间：").append(endTime);
//        content.append("\n久坐持续时间检测时间：").append(detectionTime);
//        content.append("\n目标步数：").append(targetSteps);
//        addLog(R.string.sedentary_para, content.toString());
//        etStartTime.setText(String.valueOf(beginTime));
//        etEndTime.setText(String.valueOf(endTime));
//        etKeepTime.setText(String.valueOf(detectionTime));
//        etTargetStep.setText(String.valueOf(targetSteps));
//    }




    private void getData() {
        FissionSdkBleManage.getInstance().getHrRateLevelPara();

//        AnyWear.getHrRateLevelPara(new BigDataCallBack(){
//            @Override
//            public void OnEmpty(String cmdId) {
//                dismissProgress();
//            }
//
//            @Override
//            public void OnError(String msg) {
//                showToast(msg);
//                dismissProgress();
//            }
//
//
//            public void OnHrRateLevelPara(HrRateLevel hrRateLevel) {
//                dismissProgress();
//                showSuccessToast(R.string.FUNC_SET_HRLEV_ALGO_PARA);
//                String content = "\n最大心率参数：" + hrRateLevel.getOverMaxHr() +
//                        "\n适中心率参数：" + hrRateLevel.getModerate() +
//                        "\n较大心率参数：" + hrRateLevel.getVigorous() +
//                        "\n最大心率参数：" + hrRateLevel.getMaxHr() +
//                        "\n最高心率值：" + hrRateLevel.getHighestHr() +
//                        "\n新等级：" + hrRateLevel.getHrTimeLimit();
//                addLog(R.string.FUNC_GET_SEDENTARY_PARA, content);
//                etVigorous.setText(String.valueOf(hrRateLevel.getVigorous()));
//                etMaxHr2.setText(String.valueOf(hrRateLevel.getMaxHr()));
//                etMaxHr.setText(String.valueOf(hrRateLevel.getOverMaxHr()));
//                etModerate.setText(String.valueOf(hrRateLevel.getModerate()));
//                etHighHr.setText(String.valueOf(hrRateLevel.getHighestHr()));
//                etNewLevel.setText(String.valueOf(hrRateLevel.getHrTimeLimit()));
//            }
//        });
    }
}
