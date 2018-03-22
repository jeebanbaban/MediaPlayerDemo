package com.ingreens.mediaplayerdemo.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ingreens.mediaplayerdemo.Model.Audio;
import com.ingreens.mediaplayerdemo.R;

import java.util.List;

/**
 * Created by root on 28/2/18.
 */

public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.MyViewHolder> {

    private List<Audio> list;
    Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView idAlbumArt;

        public MyViewHolder(View view) {
            super(view);
            title =view.findViewById(R.id.title);
            idAlbumArt=view.findViewById(R.id.idAlbumArt);
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



        MediaMetadataRetriever metaRetriever= new MediaMetadataRetriever();
        metaRetriever.setDataSource(audio.getData());
        byte[] data=metaRetriever.getEmbeddedPicture();
        if(data != null)
        {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            holder.idAlbumArt.setImageBitmap(bitmap);
        }
        else
        {
            holder.idAlbumArt.setImageResource(R.drawable.ic_action_music);
        }

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