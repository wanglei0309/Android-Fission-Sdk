package com.szfission.wear.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.szfission.wear.demo.R;

import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.List;

public class LogAdapter extends BaseAdapter {
    private Context context;
    private List<String> logs;

    public LogAdapter(Context context, List<String> logs) {
        this.context = context;
        this.logs = logs;

    }

    @Override
    public int getCount() {
        return logs != null ? logs.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return logs != null ? logs.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_log, parent, false);
            x.view().inject(holder, convertView);
            convertView.setTag(holder);
            holder.tvContent = convertView.findViewById(R.id.tv_content);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tvContent.setText(logs.get(position));
        return convertView;
    }

    class ViewHolder {
        TextView tvContent;

    }
}
