package com.ingreens.mediaplayerdemo;

import android.graphics.Bitmap;

/**
 * Created by root on 15/3/18.
 */

public interface TitleListener {
    public void setTitle(String album);
    public void setAlbumArt(Bitmap bmp);
    public void setAlbumArt();
    //public void setDefaltAlbumArt();
}
