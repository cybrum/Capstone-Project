package com.udacity.nanodegree.mystockhealth.news;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.udacity.nanodegree.mystockhealth.R;

import java.util.ArrayList;

public class NewsAdapter extends BaseAdapter {

    private Context context;
    ArrayList<NewsEntry> entries;

    public NewsAdapter(Context context, ArrayList<NewsEntry> entries) {
        super();
        this.context = context;
        this.entries = new ArrayList<NewsEntry>();
        this.entries.addAll(entries);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public ArrayList<NewsEntry> getEntries() {
        return entries;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.news_layout, null);
            holder.headlineView = (TextView) convertView.findViewById(R.id.textView1);
            holder.publishDate = (TextView) convertView.findViewById(R.id.textView2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (entries == null) {
            Log.e("Error", "Entries null");
        }
        holder.headlineView.setText(entries.get(position).getTitle());
        holder.publishDate.setText(entries.get(position).getPubDate());
        return convertView;
    }

    class ViewHolder {
        TextView headlineView;
        TextView publishDate;
    }

}

