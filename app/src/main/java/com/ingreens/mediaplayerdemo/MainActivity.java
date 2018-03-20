package com.ingreens.mediaplayerdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,MediaPlayer.OnCompletionListener,SeekBar.OnSeekBarChangeListener,TitleListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int EXTERNAL_STORAGE_PERMISSION_CONSTANT = 1;
    private static final int REQUEST_PERMISSION_SETTING = 2;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.ingreens.mediaplayerdemo.PlayNewAudio";
    MediaplayerService player;
    boolean serviceBound = false;
    ArrayList<Audio> audioList=new ArrayList<>();
    //Audio audio;
    Button btnPrevious,btnNext,btnBackward,btnForward,btnPlayPause,btnStop;
    Button bottomSheetplaypause,bottomSheetBack,bottomSheetFwd,bottomSheetPrev,bottomSheetNext;
    boolean sentToSettings = false;
    SharedPreferences permissionStatus;
     RecyclerView recyclerView;
     RecycleViewAdapter adapter;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
     MediaPlayer mediaPlayer;
     int totalDuration,currentDuration;
    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds
     SeekBar songProgressBar,bottomSheetSeekbar;
     Handler seek_handler=new Handler();
     TextView songCurrentDurationLabel,bottomSheetcurrentDuration;
     TextView songTotalDurationLabel,bottomSheettotalDuration;
     TextView songtitle;
     Button repeat,suffle,bottomSheetRepeat,bottomSheetSuffle;
     ImageView albumart;
     boolean updateProgress=true;
      Utilities utils;
      TextView bottomSheetTitle,bottomSheetArtist;
      ImageView bottomSheetAlbumart,bottomSheetBckground,bottomBackground;
      ImageButton bottomSheetPlaybtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_sheet);
        btnPrevious=findViewById(R.id.btnPrevious);
        btnNext=findViewById(R.id.btnNext);
        btnBackward=findViewById(R.id.btnBackward);
        btnForward=findViewById(R.id.btnForward);
        btnPlayPause=findViewById(R.id.btnPlayPause);
        btnStop=findViewById(R.id.btnStop);
        songProgressBar=findViewById(R.id.songProgressBar);
        bottomSheetSeekbar=findViewById(R.id.bottomSheetSeekbar);
        repeat=findViewById(R.id.btnRepeat);
        suffle=findViewById(R.id.btnSuffle);
        albumart=findViewById(R.id.idAlbumArt);
        //audio=new Audio();
        songCurrentDurationLabel=findViewById(R.id.currentDuration);
        songTotalDurationLabel=findViewById(R.id.totalDuration);
        songtitle=findViewById(R.id.songTitle);
        bottomSheetAlbumart=findViewById(R.id.bottomSheetAlbumArt);
        bottomSheetTitle=findViewById(R.id.bottomSheetTitle);
        bottomSheetArtist=findViewById(R.id.bottomSheetArtist);
        bottomSheetPlaybtn=findViewById(R.id.bSheetPlaybtn);
        bottomSheetBckground=findViewById(R.id.bottomSheetBckground);
        bottomSheetcurrentDuration=findViewById(R.id.bottomSheetcurrentDuration);
        bottomSheettotalDuration=findViewById(R.id.bottomSheettotalDuration);
        bottomSheetRepeat=findViewById(R.id.bottomSheetRepeat);
        bottomSheetSuffle=findViewById(R.id.bottomSheetSuffle);
        bottomSheetplaypause=findViewById(R.id.bottomSheetPlayPause);
        bottomSheetBack=findViewById(R.id.bottomSheetBack);
        bottomSheetFwd=findViewById(R.id.bottomSheetFrwd);
        bottomSheetPrev=findViewById(R.id.bottomSheetPrev);
        bottomSheetNext=findViewById(R.id.bottomSheetNext);
       // bottomBackground=findViewById(R.id.bottombackground);
        utils=new Utilities();
        //player=new MediaplayerService();
        btnStop.setOnClickListener(this);
        btnPlayPause.setOnClickListener(this);
        btnPrevious.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnBackward.setOnClickListener(this);
        btnForward.setOnClickListener(this);
        repeat.setOnClickListener(this);
        suffle.setOnClickListener(this);
        bottomSheetPlaybtn.setOnClickListener(this);
        bottomSheetRepeat.setOnClickListener(this);
        bottomSheetSuffle.setOnClickListener(this);
        bottomSheetplaypause.setOnClickListener(this);
        bottomSheetBack.setOnClickListener(this);
        bottomSheetFwd.setOnClickListener(this);
        bottomSheetPrev.setOnClickListener(this);
        bottomSheetNext.setOnClickListener(this);
        View bottomSheet = findViewById(R.id.design_bottom_sheet);
        final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        // Listeners

        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_DRAGGING");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_SETTLING");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_EXPANDED");
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_COLLAPSED");
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_HIDDEN");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.i("BottomSheetCallback", "slideOffset: " + slideOffset);
            }
        });



       bottomSheet.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                   behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
               } else {
                   //behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
               }
           }
       });


        songProgressBar.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()){

                    case MotionEvent.ACTION_DOWN: {
//                        player.mediaPlayer.pause();
                        updateProgress=false;
                    } break;
                    case MotionEvent.ACTION_UP:
                        updateProgress=false;
                        SeekBar seekBar=(SeekBar)view;
                        int progress=player.mediaPlayer.getDuration()*seekBar.getProgress()/100;
                        player.mediaPlayer.seekTo(progress);
//                        player.mediaPlayer.start();
                        updateProgress=true;
                     break;
                }
                return false;
            }
        });
        bottomSheetSeekbar.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()){

                    case MotionEvent.ACTION_DOWN: {
//                        player.mediaPlayer.pause();
                        updateProgress=false;
                    } break;
                    case MotionEvent.ACTION_UP:
                        updateProgress=false;
                        SeekBar seekBar=(SeekBar)view;
                        int progress=player.mediaPlayer.getDuration()*seekBar.getProgress()/100;
                        player.mediaPlayer.seekTo(progress);
//                        player.mediaPlayer.start();
                        updateProgress=true;
                        break;
                }
                return false;
            }
        });
        permissionStatus = getSharedPreferences("permissionStatus",MODE_PRIVATE);

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //Show Informat ion about why you need the permission
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Need Storage Permission");
                        builder.setMessage("This app needs storage permission.");
                        builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_CONSTANT);
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.show();
                    } else if (permissionStatus.getBoolean(Manifest.permission.WRITE_EXTERNAL_STORAGE,false)) {
                        //Previously Permission Request was cancelled with 'Dont Ask Again',
                        // Redirect to Settings after showing Information about why you need the permission
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Need Storage Permission");
                        builder.setMessage("This app needs storage permission.");
                        builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                sentToSettings = true;
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                                Toast.makeText(getBaseContext(), "Go to Permissions to Grant Storage", Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.show();
                    } else {
                        //just request the permission
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_CONSTANT);
                    }
                    SharedPreferences.Editor editor = permissionStatus.edit();
                    editor.putBoolean(Manifest.permission.WRITE_EXTERNAL_STORAGE,true);
                    editor.commit();

                } else {
                    //You already have the permission, just go ahead.
                    loadAudio();
                    initRecyclerView();
                }

            }






    private void initRecyclerView() {
        if (audioList.size() > 0) {
             recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
            adapter = new RecycleViewAdapter(audioList, getApplicationContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            //recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    playAudio(index);
                    //player.repeat();
//                    setAlbumArt(index);
                    Audio audio=audioList.get(index);
                    MediaMetadataRetriever metaRetriever= new MediaMetadataRetriever();
                    metaRetriever.setDataSource(audio.getData());
                    byte[] data=metaRetriever.getEmbeddedPicture();
                    if(data != null)
                    {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        setAlbumArt(bitmap);
                    }
                    else
                    {
                       setAlbumArt();
                    }

                    //// get title,album from metdata retriver for individual song
                    /*String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    String album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    songtitle.setText(title);
                    songtitle.setText(album);*/

                    songtitle.setText(audio.getTitle());
                    songtitle.setText(audio.getAlbum());
                    bottomSheetTitle.setText(audio.getAlbum());
                    bottomSheetArtist.setText(audio.getArtist());
                    bottomSheetTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    bottomSheetTitle.setSelected(true);
                    bottomSheetTitle.setSingleLine(true);
                    bottomSheetArtist.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    bottomSheetArtist.setSelected(true);
                    bottomSheetArtist.setSingleLine(true);



                    //albumart.setImageBitmap(audio.getAlbumart());
                    //albumart.setImageResource(audio.getAlbumart());
                    updateProgressBar();
                }
            }));

        }
    }
    @Override
    public void setAlbumArt(Bitmap bmp){
        albumart.setImageBitmap(bmp); //associated cover art in bitmap
        bottomSheetAlbumart.setImageBitmap(bmp);
        bottomSheetBckground.setImageBitmap(bmp);
        //bottomBackground.setImageBitmap(bmp);
        albumart.setAdjustViewBounds(true);
        albumart.setLayoutParams(new LinearLayout.LayoutParams(500, 500));
    }

    @Override
    public void setAlbumArt() {
        albumart.setImageResource(R.drawable.audio_file); //any default cover resourse folder
        bottomSheetAlbumart.setImageResource(R.drawable.audio_file);
        albumart.setAdjustViewBounds(true);
        albumart.setLayoutParams(new LinearLayout.LayoutParams(500,500 ));
    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaplayerService.LocalBinder binder = (MediaplayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            player.setTitleListener(MainActivity.this);
            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            System.out.println("###########################");
            System.out.println("service disconnected...");
            System.out.println("###########################");
        }
    };


    public void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {

            StorageUtil storageUtil=new StorageUtil(MainActivity.this);
            storageUtil.storeAudio(audioList);
            storageUtil.storeAudioIndex(audioIndex);
            Intent playerIntent = new Intent(this, MediaplayerService.class);
            //playerIntent.putExtra("media", media);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            /*MediaMetadataRetriever metaRetriever= new MediaMetadataRetriever();
            metaRetriever.setDataSource(audio.getData());
            String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            System.out.println("##############################");
            System.out.println("song name hochhee==="+album);
            System.out.println("##############################");*/
           // songtitle.setText(album);

        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(MainActivity.this);
            storage.storeAudioIndex(audioIndex);
            //Service is active
            //Send media with BroadcastReceiver
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
            /*MediaMetadataRetriever metaRetriever= new MediaMetadataRetriever();
            metaRetriever.setDataSource(audio.getData());
            String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            System.out.println("song name hochhee==="+album);
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");*/


        }
    }

    public void setSongTitle(){

    }


   /* //This method gets the MediaPlayer Duration from service.
    public void getMediaPlayerDuration(){
        if (serviceBound==true){
            System.out.println("@@@@@@@@@@@ servicebound checked for total duration @@@@@@@@@@@@");
            if (player.mediaPlayer!=null){
                totalDuration=player.seekBarGetTotalDuration();
                System.out.println("############### activity total duration #################"+totalDuration);
            }

        }

    }*/
    //This method get MediaPlayerCurrent Position from service
/*
    public void getMediaPlayerCurrentPosition(){
        if(serviceBound==true){
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&77 service bound checked for current duration");
            if(player.mediaPlayer!=null){
                currentDuration=player.seekBarGetCurrentPosition();
                //System.out.println("############### activity current duration #################"+currentDuration);
            }
        }
    }
*/

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
            System.out.println("service unbind hoye geche... ");
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");

        }
    }
    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        /*final String[] columns  = new String[]{ android.provider.MediaStore.Audio.Albums._ID,
                android.provider.MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART };*/
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        /*final String[] cursor_cols = { MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION };*/
        String selection = MediaStore.Audio.Media.IS_MUSIC+ "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        //String selection = MediaStore.Audio.Media.IS_MUSIC+ "=1";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                /*System.out.println("##############################");
                System.out.println("audiolist er size==="+audioList.size());
                System.out.println("##############################");*/

                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String totalduration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                /*String pathId = cursor.getString(Integer.parseInt(data) );
                MediaMetadataRetriever metaRetriever= new MediaMetadataRetriever();
                metaRetriever.setDataSource(pathId);
                byte[] dataa=metaRetriever.getEmbeddedPicture();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(dataa, 0, dataa.length);*/
                   // setAlbumArt(bitmap);
                // Save to audioList
                Audio audio=new Audio(data,title,album,artist,totalduration);
                audioList.add(audio);
            }
        }
        cursor.close();
    }
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.bottomSheetPrev:
            case R.id.btnPrevious:
                if (player==null){
                    return;
                }
                player.skipToPrevious();
                Toast.makeText(player, "previous song bajbe re.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.bottomSheetNext:
            case R.id.btnNext:
                if (player==null){
                    return;
                }
                player.skipToNext();
                Toast.makeText(player, "next song bajbe re...", Toast.LENGTH_SHORT).show();
                break;
            case R.id.bottomSheetBack:
            case R.id.btnBackward:
                if (player==null){
                    return;
                }
                // get current song position
                int currentposition = player.seekBarGetCurrentPosition();
                // check if seekBackward time is greater than 0 sec
                if(currentposition - seekBackwardTime >= 0){
                    // forward song
                    player.mediaPlayer.seekTo(currentposition - seekBackwardTime);
                }else{
                    // backward to starting position
                    player.mediaPlayer.seekTo(0);
                }
                break;
            case R.id.bottomSheetFrwd:
            case R.id.btnForward:
                if (player==null){
                    return;
                }
                // get current song position
                int currentPosition = player.seekBarGetCurrentPosition();
                // check if seekForward time is lesser than song duration
                if(currentPosition + seekForwardTime <= player.seekBarGetTotalDuration()){
                    // forward song
                    player.mediaPlayer.seekTo(currentPosition+seekForwardTime);
                }else{
                    // forward to end position
                    player.mediaPlayer.seekTo(player.seekBarGetTotalDuration());
                }
                break;
            case R.id.btnPlayPause:
                if (player==null){
                    return;
                }
                if (player.isPlaying()){
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("pause hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.pauseMedia();
                    player.buildNotification(PlaybackStatus.PAUSED);
                    seek_handler.removeCallbacks(mUpdateTimeTask);
                }else{
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("play hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.playMedia();
                    player.buildNotification(PlaybackStatus.PLAYING);
                    updateProgressBar();
                }
                /*player.pauseMedia();
                player.buildNotification(PlaybackStatus.PAUSED);
                Toast.makeText(player, "pause hoye geche re", Toast.LENGTH_SHORT).show();*/
                break;
            case R.id.btnStop:
                //toggleBottomSheet();
                if (player==null){
                    return;
                }
                player.stopMedia();
                break;
                case R.id.btnRepeat:
                if (player==null){
                    return;
                }
                player.repeat();
                break;
            case R.id.btnSuffle:
                if (player==null){
                    return;
                }
                player.suffle();
                break;
            case R.id.bSheetPlaybtn:
                if (player==null){
                    return;
                }
                if (player.isPlaying()){
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("pause hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.pauseMedia();
                    player.buildNotification(PlaybackStatus.PAUSED);
                    seek_handler.removeCallbacks(mUpdateTimeTask);
                }else{
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("play hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.playMedia();
                    player.buildNotification(PlaybackStatus.PLAYING);
                    updateProgressBar();
                }
                break;
            case R.id.bottomSheetRepeat:
                if (player==null){
                    return;
                }
                player.repeat();
                break;
            case R.id.bottomSheetSuffle:
                if (player==null){
                    return;
                }
                player.suffle();
                break;
            case R.id.bottomSheetPlayPause:
                if (player==null){
                    return;
                }
                if (player.isPlaying()){
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("pause hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.pauseMedia();
                    player.buildNotification(PlaybackStatus.PAUSED);
                    seek_handler.removeCallbacks(mUpdateTimeTask);
                }else{
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("play hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.playMedia();
                    player.buildNotification(PlaybackStatus.PLAYING);
                    updateProgressBar();
                }
                break;
            /*case R.id.bottomSheetBack:
                if (player==null){
                    return;
                }
                // get current song position
                //int currentposition = player.seekBarGetCurrentPosition();
                // check if seekBackward time is greater than 0 sec
                if(currentposition - seekBackwardTime >= 0){
                    // forward song
                    player.mediaPlayer.seekTo(currentposition - seekBackwardTime);
                }else{
                    // backward to starting position
                    player.mediaPlayer.seekTo(0);
                }
                break;*/
            /*case R.id.bottomSheetFrwd:
                break;*/
           /* case R.id.bottomSheetPrev:
                if (player==null){
                    return;
                }
                player.skipToPrevious();
                break;*/
            /*case R.id.bottomSheetNext:
                break;*/

        }
    }
    public void updateProgressBar(){
        try{
            seek_handler.postDelayed(mUpdateTimeTask, 100);
        }catch(Exception e){

        }
    }

    Runnable mUpdateTimeTask = new Runnable() {
        public void run(){
            long totalDuration = 0;
            long currentDuration = 0;

            try {
                totalDuration = player.seekBarGetTotalDuration();
                currentDuration = player.seekBarGetCurrentPosition();
                songCurrentDurationLabel.setText(utils.milliSecondsToTimer(currentDuration)); // Displaying time completed playing
                songTotalDurationLabel.setText(utils.milliSecondsToTimer(totalDuration)); // Displaying time completed playing
                bottomSheetcurrentDuration.setText(utils.milliSecondsToTimer(currentDuration));
                bottomSheettotalDuration.setText(utils.milliSecondsToTimer(totalDuration));
                int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
                // TODO some task
                songProgressBar.setProgress(progress);/* Running this thread after 100 milliseconds */
                bottomSheetSeekbar.setProgress(progress);

                seek_handler.postDelayed(this, 100);

            } catch(Exception e){
                e.printStackTrace();
            }

        }
    };

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //seek_handler.removeCallbacks(mUpdateTimeTask);

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
       /* seek_handler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = player.seekBarGetTotalDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(),totalDuration);
        player.mediaPlayer.seekTo(currentPosition);
        updateProgressBar();
*/
    }

    @Override
    public void setTitle(String album) {
        songtitle.setText(album);
        bottomSheetTitle.setText(album);
    }


}
