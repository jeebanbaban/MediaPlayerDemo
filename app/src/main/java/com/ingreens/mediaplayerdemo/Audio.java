package com.ingreens.mediaplayerdemo;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by root on 28/2/18.
 */

public class Audio implements Serializable {
    private String data;
    private String title;
    private String album;
    private String artist;
    private String duration;
//    private Bitmap bitmap;
    public Audio(String data, String title, String album, String artist, String duration) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.duration=duration;
//        this.bitmap=bitmap;
    }
/*

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
*/

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getData() {

        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
