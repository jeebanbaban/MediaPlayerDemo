package com.ingreens.mediaplayerdemo.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
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

import com.ingreens.mediaplayerdemo.Constant.Allkey;
import com.ingreens.mediaplayerdemo.Listner.CustomTouchListener;
import com.ingreens.mediaplayerdemo.Service.MediaplayerService;
import com.ingreens.mediaplayerdemo.Model.Audio;
import com.ingreens.mediaplayerdemo.Status.PlaybackStatus;
import com.ingreens.mediaplayerdemo.R;
import com.ingreens.mediaplayerdemo.Adapter.RecycleViewAdapter;
import com.ingreens.mediaplayerdemo.Utils.StorageUtil;
import com.ingreens.mediaplayerdemo.Listner.TitleListener;
import com.ingreens.mediaplayerdemo.Timer.Utilities;
import com.ingreens.mediaplayerdemo.Listner.onItemClickListener;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,MediaPlayer.OnCompletionListener,SeekBar.OnSeekBarChangeListener,TitleListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int EXTERNAL_STORAGE_PERMISSION_CONSTANT = 1;
    private static final int REQUEST_PERMISSION_SETTING = 2;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.ingreens.mediaplayerdemo.PlayNewAudio";

    MediaplayerService player;
    SharedPreferences permissionStatus;
    RecyclerView recyclerView;
    RecycleViewAdapter adapter;
    Utilities utils;

    ArrayList<Audio> audioList=new ArrayList<>();
    SeekBar bottomSheetSeekbar;
    Handler seek_handler=new Handler();

    boolean serviceBound = false;
    boolean sentToSettings = false;
    boolean updateProgress=true;

    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds

    TextView bottomSheetcurrentDuration;
    TextView bottomSheettotalDuration;
    TextView bottomSheetTitle,bottomSheetArtist;

    Button bottomSheetBack,bottomSheetFwd,bottomSheetPrev,bottomSheetNext;
    Button bottomSheetRepeat,bottomSheetSuffle;

    ImageView bottomSheetAlbumart,bottomSheetBckground;
    ImageButton bottomSheetPlaybtn,bottomSheetplaypause;
    /*private boolean isShuffle = false;
    private boolean isRepeat = false;
     MediaPlayer mediaPlayer;*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_sheet);

        utils=new Utilities();
        bottomSheetSeekbar=findViewById(R.id.bottomSheetSeekbar);

        //bottom albumart,title,artist
        bottomSheetAlbumart=findViewById(R.id.bottomSheetAlbumArt);
        bottomSheetTitle=findViewById(R.id.bottomSheetTitle);
        bottomSheetArtist=findViewById(R.id.bottomSheetArtist);
        //bottom playpause button
        bottomSheetPlaybtn=findViewById(R.id.bSheetPlaybtn);
        bottomSheetPlaybtn.setOnClickListener(this);
        //bottom sheet albumartimage,current and total duration
        bottomSheetBckground=findViewById(R.id.bottomSheetBckground);
        bottomSheetcurrentDuration=findViewById(R.id.bottomSheetcurrentDuration);
        bottomSheettotalDuration=findViewById(R.id.bottomSheettotalDuration);
       //bottom sheet repeat button
        bottomSheetRepeat=findViewById(R.id.bottomSheetRepeat);
        bottomSheetRepeat.setOnClickListener(this);
        //bottom sheet suffle button
        bottomSheetSuffle=findViewById(R.id.bottomSheetSuffle);
        bottomSheetSuffle.setOnClickListener(this);
        //bottom sheet back button
        bottomSheetBack=findViewById(R.id.bottomSheetBack);
        bottomSheetBack.setOnClickListener(this);
        //bottom sheet forward button
        bottomSheetFwd=findViewById(R.id.bottomSheetFrwd);
        bottomSheetFwd.setOnClickListener(this);
        //bottom sheet playpause button
        bottomSheetplaypause=findViewById(R.id.bottomSheetPlayPause);
        bottomSheetplaypause.setOnClickListener(this);
        //bottom sheet previous button
        bottomSheetPrev=findViewById(R.id.bottomSheetPrev);
        bottomSheetPrev.setOnClickListener(this);
        //bottom sheet next button
        bottomSheetNext=findViewById(R.id.bottomSheetNext);
        bottomSheetNext.setOnClickListener(this);
        //set default button icon when app is launched...
        bottomSheetplaypause.setBackgroundResource(R.drawable.ic_action_playy);
        bottomSheetPlaybtn.setBackgroundResource(R.drawable.ic_action_playy);

        ////bottom sheet behavior
        View bottomSheet = findViewById(R.id.design_bottom_sheet);
        final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
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

       //// working with bottomsheet seebar
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
        ////marsmallow permission checking...
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
                    String name=permissionStatus.getString("songname","");
                    String artist=permissionStatus.getString("songartist","");
                    bottomSheetTitle.setText(name);
                    bottomSheetArtist.setText(artist);
                    bottomSheetPlaybtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            /*int lastsongindex=permissionStatus.getInt(Allkey.LAST_SONG_INDEX,0);
                            String lastsongpath=permissionStatus.getString(Allkey.LAST_SONG,"");*/
                             int index=permissionStatus.getInt("songindex",0);
                            playAudio(index);
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
                        }
                    });
                }

            }

    //initialization recycleview
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
                    updateProgressBar();
                    Audio audio=audioList.get(index);
                    MediaMetadataRetriever metaRetriever= new MediaMetadataRetriever();
                    metaRetriever.setDataSource(audio.getData());
                    byte[] data=metaRetriever.getEmbeddedPicture();
                    if(data != null)
                    {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        setAlbumArt(bitmap);
                        SharedPreferences.Editor editor=permissionStatus.edit();
                        editor.putString("songname",audio.getAlbum());
                        editor.putString("songartist",audio.getArtist());
                        editor.putInt("songindex",index);
                        editor.commit();

                    }
                    else
                    {
                       setAlbumArt();
                    }
                    // set bottom title
                    bottomSheetTitle.setText(audio.getAlbum());
                    bottomSheetTitle.setSingleLine(true);
                    bottomSheetTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    bottomSheetTitle.setSelected(true);
                    // set bottom artist
                    bottomSheetArtist.setText(audio.getArtist());
                    bottomSheetArtist.setSingleLine(true);
                    bottomSheetArtist.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    bottomSheetArtist.setSelected(true);
                    updateProgressBar();
                }
            }));

        }
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

        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(MainActivity.this);
            storage.storeAudioIndex(audioIndex);
            //Service is active
            //Send media with BroadcastReceiver
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);

        }
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
            player.stopMedia();
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
            System.out.println("service unbind hoye geche... ");
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");

        }
    }

    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC+ "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {

                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String totalduration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

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
            //case R.id.btnPrevious:
                if (player==null){
                    return;
                }
                player.skipToPrevious();
                Toast.makeText(player, "previous song bajbe re.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.bottomSheetNext:
            //case R.id.btnNext:
                if (player==null){
                    return;
                }
                player.skipToNext();
                Toast.makeText(player, "next song bajbe re...", Toast.LENGTH_SHORT).show();
                break;
            case R.id.bottomSheetBack:
            //case R.id.btnBackward:
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
            //case R.id.btnForward:
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
            case R.id.bSheetPlaybtn:
            case R.id.bottomSheetPlayPause:
                if (player==null){
                    return;
                }
                if (player.isPlaying()){
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("pause hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.pauseMedia();
                    bottomSheetplaypause.setBackgroundResource(R.drawable.ic_action_playy);
                    bottomSheetPlaybtn.setBackgroundResource(R.drawable.ic_action_playy);
                    player.buildNotification(PlaybackStatus.PAUSED);
                    seek_handler.removeCallbacks(mUpdateTimeTask);
                }else{
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("play hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.playMedia();
                    bottomSheetplaypause.setBackgroundResource(R.drawable.ic_action_pause);
                    bottomSheetPlaybtn.setBackgroundResource(R.drawable.ic_action_pause);
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
                bottomSheetcurrentDuration.setText(utils.milliSecondsToTimer(currentDuration));
                bottomSheettotalDuration.setText(utils.milliSecondsToTimer(totalDuration));
                int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
                // TODO some task
                //songProgressBar.setProgress(progress);/* Running this thread after 100 milliseconds */
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

    //callback method for set title
    @Override
    public void setTitle(String album) {
        bottomSheetTitle.setText(album);
    }

    //callback method for set albumart image
    @Override
    public void setAlbumArt(Bitmap bmp){
        ////associated cover art in bitmap
        bottomSheetAlbumart.setImageBitmap(bmp);
        /*bottomSheetAlbumart.setAdjustViewBounds(true);
        bottomSheetAlbumart.setLayoutParams(new LinearLayout.LayoutParams(500, 500));
*/
        bottomSheetBckground.setImageBitmap(bmp);
    }
    //callback method for set default albumart image
    @Override
    public void setAlbumArt() {
        ////any default cover resourse folder
        bottomSheetAlbumart.setImageResource(R.drawable.ic_action_music);
        bottomSheetBckground.setImageResource(R.drawable.audio_file);
    }



}
