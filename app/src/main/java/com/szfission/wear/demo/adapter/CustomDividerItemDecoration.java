package com.szfission.wear.demo.adapter;

/**
 * describe:
 * author: wl
 * createTime: 2023/11/25
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szfission.wear.demo.R;

public class CustomDividerItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint;

    public CustomDividerItemDecoration(Context context) {
        paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.black)); // 替换成您的颜色
        paint.setStrokeWidth(2); // 替换成您的高度
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + (int) paint.getStrokeWidth();

            canvas.drawRect(left, top, right, bottom, paint);
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        // 在这里定义分割线的高度，例如，将底部偏移设置为 8px
        outRect.set(0, 0, 0, 2);
    }
}


