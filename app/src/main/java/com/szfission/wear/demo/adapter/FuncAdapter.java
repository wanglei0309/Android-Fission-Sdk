package com.szfission.wear.demo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.szfission.wear.demo.R;
import com.szfission.wear.demo.bean.FuncBean;

import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.List;

public class FuncAdapter extends BaseAdapter {
    private Context context;
    private List<FuncBean> funcBeans;

    public FuncAdapter(Context context, List<FuncBean> funcBeans) {
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

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(context).inflate(R.layout.item_func, viewGroup, false);
            x.view().inject(holder, view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        holder.tvTitle = view.findViewById(R.id.tvTitle);
        holder.tvTitle.setText(funcBeans.get(i).getTitle());
        return view;
    }

    class ViewHolder {
        TextView tvTitle;
    }

}
