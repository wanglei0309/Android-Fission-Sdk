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
                    array = new String[]{"12小时", "24小时"};
                    break;
                case ModelConstant.FUNC_SET_UNIT:
                    array = new String[]{"英制", "公制"};
                    break;
                case ModelConstant.FUNC_SET_LANG:
                    array  = new String[]{"中文", "英文", "日语", "法语", "德语", "西班牙", "意大利", "葡萄牙", "俄语", "捷克", "波兰", "繁体中文", "阿拉伯语",
                            "土耳其语","越南语", "韩语", "希伯来语", "泰语", "印度尼西亚语", "荷兰语", "希腊语", "瑞典语", "罗马尼亚语"};
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
                    array = new String[]{"关", "开"};
                    break;
                case ModelConstant.FUNC_SET_HIGH_SPEED_CONNECT:
                    array = new String[]{"低速连接", "高速连接"};
                    break;
                case ModelConstant.FUNC_PUSH_CUSTOM_DIAL:
                    array = new String[]{"相册选择", "拍照"};
                    break;

            }
            setModelDialog(context, array);
        }else if (dialogType == 1){
            switch (type) {
                case ModelConstant.FUNC_SET_TIMEZONE:
                    setContent(context, "设置时区", "8");
                    break;
                case ModelConstant.FUNC_SET_DATA_STREAM:
                case ModelConstant.FUNC_SET_DATA_STREAM2:
                    setContent(context, "请输入流时间（毫秒，0为关闭", "1000");
                    break;
                case ModelConstant.FUNC_SAFETY_CONFIRM:
                    setContent(context, "请输入安全确认内容", "xxxxx");
                    break;
                case ModelConstant.FUNC_PAGE_SKIP:
                    setContent(context, "请输入页面跳转内容", "FF02");
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
                    Toast.makeText(context,"数据不能为空",Toast.LENGTH_SHORT).show();
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
