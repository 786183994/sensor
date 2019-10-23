package com.example.firstapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 输入监听
 * @author: bill
 * @date: 2019/9/4
 * @email: 17835697447@163.com
 */

public class MyAdapter extends BaseAdapter{

    private List<String> list1 = new ArrayList<String>();
    private Context context;

    public MyAdapter(List<String> list, Context context) {
        this.list1 = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return list1.size();
    }

    @Override
    public String getItem(int position) {
        // TODO Auto-generated method stub
        return list1.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.activity_set_up, null);
            holder.tv_ss = convertView.findViewById(R.id.tv_ss);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
            holder.tv_ss.setTextColor(context.getResources().getColor(R.color.black));
            holder.tv_ss.setTextSize(15);
            holder.tv_ss.setPadding(70,5,5,5);
            holder.tv_ss.setText(list1.get(position));
        }

        //holder = (ViewHolder) convertView.getTag();
        //holder.tv_ss.setText(list.get(position));
        return convertView;
    }

    /**
     * 控件缓存类
     *
     * @author 邹奇
     *
     */
    class ViewHolder {
        TextView tv_ss;

    }
}
