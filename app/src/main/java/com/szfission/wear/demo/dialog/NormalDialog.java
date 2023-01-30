package com.szfission.wear.demo.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.szfission.wear.demo.ModelConstant;
import com.szfission.wear.demo.R;

import static com.szfission.wear.demo.ModelConstant.FUNC_SWITCH_HR_RATE;

public class NormalDialog extends Dialog {
    private OnConfirmClickListener onConfirmClickListener;


    public NormalDialog(@NonNull Context context,int dialogType,int type) {
        super(context,type);
        initDialog(context,dialogType,type);
    }

    String[] array ;
    private void initDialog(Context context, int dialogType,int type) {
        if (dialogType == 2){
            switch (type) {
                case ModelConstant.FUNC_SET_TIME_MODE:
                    array = new String[]{context.getString(R.string.time_format_12), context.getString(R.string.time_format_24)};
                    break;
                case ModelConstant.FUNC_SET_UNIT:
                    array = new String[]{context.getString(R.string.imperial), context.getString(R.string.metric)};
                    break;
                case ModelConstant.FUNC_SET_LANG:
                    array  = new String[]{context.getString(R.string.chinese), context.getString(R.string.english), context.getString(R.string.japanese), context.getString(R.string.french), context.getString(R.string.german), context.getString(R.string.spain), context.getString(R.string.italy), context.getString(R.string.portugal), context.getString(R.string.russian), context.getString(R.string.czech), context.getString(R.string.polish), context.getString(R.string.traditional_chinese), context.getString(R.string.arabic),
                            context.getString(R.string.turkish),context.getString(R.string.vietnamese), context.getString(R.string.korean), context.getString(R.string.hebrew), context.getString(R.string.thai), context.getString(R.string.indonesian), context.getString(R.string.dutch), context.getString(R.string.greek), context.getString(R.string.swedish), context.getString(R.string.romanian)};
                    break;
                case ModelConstant.FUNC_SET_FEMALE_PHYSIOLOGY:
                    array = new String[]{"未启用", "怀孕期", "月经期", "安全期", "排卵期", "排卵日"};;
                    break;
                case ModelConstant.FUNC_VIBRATION:
                case FUNC_SWITCH_HR_RATE:
                case ModelConstant.FUNC_CAMERA_MODEL:
                case ModelConstant.FUNC_SET_WRIST_BRIGHT_SCREEN:
                case ModelConstant.FUNC_SELF_INSPECTION_MODE:
                case ModelConstant.FUNC_SET_PROMPT:
                case ModelConstant.FUNC_SET_BLOOD_OXYGEN_SWITCH:
                case ModelConstant.FUNC_SET_MENTAL_STRESS_SWITCH:
                case ModelConstant.FUNC_SET_HEART_RATE_SWITCH:
                case ModelConstant.FUNC_SET_CALL_AUDIO_SWITCH:
                case ModelConstant.FUNC_SET_MEDIA_AUDIO_SWITCH:
                    array = new String[]{context.getString(R.string.disabled), context.getString(R.string.enable)};
                    break;
                case ModelConstant.FUNC_SET_HIGH_SPEED_CONNECT:
                    array = new String[]{context.getString(R.string.low_speed_connection), context.getString(R.string.high_speed_connection)};
                    break;
                case ModelConstant.FUNC_PUSH_CUSTOM_DIAL:
                    array = new String[]{context.getString(R.string.album_selection), context.getString(R.string.photograph)};
                    break;

            }
            setModelDialog(context, array);
        }else if (dialogType == 1){
            switch (type) {
                case ModelConstant.FUNC_SET_TIMEZONE:
                    setContent(context, context.getString(R.string.FUNC_SET_TIMEZONE), "8");
                    break;
                case ModelConstant.FUNC_SET_DATA_STREAM:
                case ModelConstant.FUNC_SET_DATA_STREAM2:
                    setContent(context, context.getString(R.string.stream_time), "1000");
                    break;
                case ModelConstant.FUNC_SAFETY_CONFIRM:
                    setContent(context, context.getString(R.string.security_confirmation), "xxxxx");
                    break;
                case ModelConstant.FUNC_PAGE_SKIP:
                    setContent(context, context.getString(R.string.page_jump), "FF02");
                    break;
            }
        }

    }

    private void setModelDialog(Context context, String[] array) {
        new AlertDialog.Builder(context)
                .setItems(array, (dialogInterface, i) -> {
                    onConfirmClickListener.confirm(i+"");
                    dismiss();
                }).create().show();
    }

    private void setContent(Context context, String title,String editText) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_simple_input, null);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);
         EditText etContent = view.findViewById(R.id.etContent);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvTitle.setText(title);
        etContent.setText(editText);
        final AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(etContent.getText().toString())) {
                    onConfirmClickListener.confirm(etContent.getText().toString().trim());
                    dialog.dismiss();
                }else {
                    Toast.makeText(context,context.getString(R.string.data_not_empty),Toast.LENGTH_SHORT).show();
                }

            }
        });
        dialog.show();
    }


    public void setOnConfirmClickListener(OnConfirmClickListener onConfirmClickListener) {
        this.onConfirmClickListener = onConfirmClickListener;
    }

    public interface OnConfirmClickListener {
        void confirm(String content);
    }
}
