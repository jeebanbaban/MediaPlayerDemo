package com.ingreens.mediaplayerdemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 28/2/18.
 */

public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.MyViewHolder> {

    private List<Audio> list;
    Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView play_pause;

        public MyViewHolder(View view) {
            super(view);
            title =view.findViewById(R.id.title);
            play_pause=view.findViewById(R.id.play_pause);
        }
    }


    public RecycleViewAdapter(List<Audio> list, Context context) {
        this.list = list;
        this.context=context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Audio audio = list.get(position);
        holder.title.setText(audio.getTitle());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}