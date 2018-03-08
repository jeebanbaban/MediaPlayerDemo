package com.ingreens.mediaplayerdemo;

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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.SQLOutput;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,MediaPlayer.OnCompletionListener,SeekBar.OnSeekBarChangeListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int EXTERNAL_STORAGE_PERMISSION_CONSTANT = 1;
    private static final int REQUEST_PERMISSION_SETTING = 2;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.ingreens.mediaplayerdemo.PlayNewAudio";
    MediaplayerService player;
    boolean serviceBound = false;
    ArrayList<Audio> audioList=new ArrayList<>();
    Button btnPrevious,btnNext,btnBackward,btnForward,btnPlayPause,btnStop;
    boolean sentToSettings = false;
    SharedPreferences permissionStatus;
     RecyclerView recyclerView;
     RecycleViewAdapter adapter;
     int duration,currentDuration;
     SeekBar songProgressBar;
     TextView songCurrentDurationLabel;
     TextView songTotalDurationLabel;
      Utilities utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnPrevious=findViewById(R.id.btnPrevious);
        btnNext=findViewById(R.id.btnNext);
        btnBackward=findViewById(R.id.btnBackward);
        btnForward=findViewById(R.id.btnForward);
        btnPlayPause=findViewById(R.id.btnPlayPause);
        btnStop=findViewById(R.id.btnStop);
        songProgressBar=findViewById(R.id.songProgressBar);
        songCurrentDurationLabel=findViewById(R.id.currentDuration);
        songTotalDurationLabel=findViewById(R.id.totalDuration);
        utils=new Utilities();
        //player=new MediaplayerService();
        btnStop.setOnClickListener(this);
        btnPlayPause.setOnClickListener(this);
        btnPrevious.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnBackward.setOnClickListener(this);
        btnForward.setOnClickListener(this);
        // Listeners
       /* songProgressBar.setOnSeekBarChangeListener(this); // Important
        player.mediaPlayer.setOnCompletionListener(this); // Important*/
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
            //recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            /*final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(layoutManager);*/
            recyclerView.setAdapter(adapter);
            //recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    playAudio(index);
                    getMediaPlayerDuration();
                    getMediaPlayerCurrentPosition();
                    //songCurrentDurationLabel.setText(currentDuration);
                    //songTotalDurationLabel.setText(duration);
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


    private void playAudio(int audioIndex) {
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

    //This method gets the MediaPlayer Duration from service.
    public int getMediaPlayerDuration(){
        if(serviceBound==true){
            if(player.mediaPlayer!=null){
                duration=player.seekBarGetTotalDuration();
                System.out.println("##############################");
                System.out.println("##############################");
                System.out.println("song er total duration===="+duration);
                System.out.println("##############################");
                System.out.println("##############################");
            }
        }
        return duration;
    }
    //This method get MediaPlayerCurrent Position from service
    public int getMediaPlayerCurrentPosition(){
        if(serviceBound==true){
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&77 service bound checkeddddd...");
            if(player.mediaPlayer!=null){
                currentDuration=player.seekBarGetCurrentPosition();
                System.out.println("##############################");
                System.out.println("##############################");
                System.out.println("song er current duration=="+currentDuration);
                System.out.println("##############################");
                System.out.println("##############################");
            }
        }
        return currentDuration;
    }

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

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                System.out.println("##############################");
                System.out.println("audiolist er size==="+audioList.size());
                System.out.println("##############################");

                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String totalduration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                System.out.println("data==="+data);
                System.out.println("title==="+title);
                System.out.println("album==="+album);
                System.out.println("artist=="+artist);
                System.out.println("duration=="+totalduration);
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

                // Save to audioList
                audioList.add(new Audio(data, title, album, artist,totalduration));
            }
        }
        cursor.close();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btnPrevious:
                player.skipToPrevious();
                Toast.makeText(player, "previous song bajbe re.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnNext:
                player.skipToNext();
                Toast.makeText(player, "next song bajbe re...", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnBackward:
                break;
            case R.id.btnForward:
                break;
            case R.id.btnPlayPause:
                if (player.isPlaying()){
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("pause hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.pauseMedia();
                    player.buildNotification(PlaybackStatus.PAUSED);
                }else{
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    System.out.println("play hoye geche");
                    System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                    player.playMedia();
                    player.buildNotification(PlaybackStatus.PLAYING);
                }
                /*player.pauseMedia();
                player.buildNotification(PlaybackStatus.PAUSED);
                Toast.makeText(player, "pause hoye geche re", Toast.LENGTH_SHORT).show();*/
                break;
            case R.id.btnStop:
                player.stopMedia();
                break;

        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
