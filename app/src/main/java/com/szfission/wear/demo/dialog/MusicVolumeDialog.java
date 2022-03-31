package com.szfission.wear.demo.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.szfission.wear.demo.R;

public class MusicVolumeDialog extends Dialog {
    private EditText etMaxProgress;
    private EditText etProgress;
    private OnConfirmClickListener onConfirmClickListener;

    public MusicVolumeDialog(Context context) {
        super(context);
        init(context);
    }

    public MusicVolumeDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(context);
    }

    public MusicVolumeDialog(Context context, int themeResId) {
        super(context, themeResId);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_music_volume, null);
        setContentView(view);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        etMaxProgress = view.findViewById(R.id.etMaxProgress);
        etProgress = view.findViewById(R.id.etProgress);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String maxProgress = etMaxProgress.getText().toString();
                String progress = etProgress.getText().toString();
                if(onConfirmClickListener != null && !TextUtils.isEmpty(maxProgress) && !TextUtils.isEmpty(progress)){
                    onConfirmClickListener.confirm(Integer.parseInt(maxProgress), Integer.parseInt(progress));
                }
                dismiss();
            }
        });
    }

    public void setOnConfirmClickListener(OnConfirmClickListener onConfirmClickListener) {
        this.onConfirmClickListener = onConfirmClickListener;
    }

    public interface OnConfirmClickListener {
        void confirm(int max, int progress);
    }

}
