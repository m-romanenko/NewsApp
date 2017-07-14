package com.example.android.newsapp;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class StoryAdapter extends ArrayAdapter<Story> {

    private final List<Story> stories;

    public StoryAdapter(Activity context, List<Story> stories) {
        super(context, 0, stories);
        this.stories = stories;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View listItemView = convertView;

        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item, parent, false);
            holder = new ViewHolder(listItemView);
            listItemView.setTag(holder);
        } else {
            holder = (ViewHolder) listItemView.getTag();
        }

        Story currentStory = getItem(position);
        String title = currentStory.getTitle();
        String section = currentStory.getSection();
        String date = currentStory.getDate();

        holder.titleTextView.setText(title);
        holder.sectionTextView.setText(section);
        holder.dateTextView.setText(date);

        return listItemView;
    }

    public List<Story> getItems() {
        return stories;
    }

    static class ViewHolder {
        TextView titleTextView;
        TextView sectionTextView;
        TextView dateTextView;

        public ViewHolder(@NonNull View view) {
            this.titleTextView = (TextView) view
                    .findViewById(R.id.textview_li_title);
            this.sectionTextView = (TextView) view
                    .findViewById(R.id.textview_li_section);
            this.dateTextView = (TextView) view
                    .findViewById(R.id.textview_li_date);

        }
    }
}