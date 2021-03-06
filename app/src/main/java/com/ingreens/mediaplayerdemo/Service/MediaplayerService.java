package com.ingreens.mediaplayerdemo.Service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.ingreens.mediaplayerdemo.Activity.MainActivity;
import com.ingreens.mediaplayerdemo.Constant.Allkey;
import com.ingreens.mediaplayerdemo.Model.Audio;
import com.ingreens.mediaplayerdemo.Status.PlaybackStatus;
import com.ingreens.mediaplayerdemo.R;
import com.ingreens.mediaplayerdemo.Utils.StorageUtil;
import com.ingreens.mediaplayerdemo.Listner.TitleListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by root on 28/2/18.
 */

public class MediaplayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,

        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "com.ingreens.mediaplayerdemo.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.ingreens.mediaplayerdemo.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.ingreens.mediaplayerdemo.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.ingreens.mediaplayerdemo.ACTION_NEXT";
    public static final String ACTION_STOP = "com.ingreens.mediaplayerdemo.ACTION_STOP";
    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    SharedPreferences preferences;

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    public MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();

    public MediaPlayer mediaPlayer;

    private String mediaFile;

    private int totalDuration,currentPosition;
    private int resumePosition;
    private int audioIndex = -1;

    private boolean ongoingCall = false;
    private boolean musicPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;

    private MainActivity mainActivity;
    private AudioManager audioManager;

    //List of available Audio files
    private ArrayList<Audio> audioList;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    public Audio activeAudio; //an object of the currently playing audio
    TitleListener titleListener;

    public void setTitleListener(TitleListener titleListener){
        this.titleListener=titleListener;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();
        preferences=getSharedPreferences(Allkey.PREF_NAME,MODE_PRIVATE);
    }

    public void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(activeAudio.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }


    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
                System.out.println("***********************");
                System.out.println("***********************");
                System.out.println("song title ==="+activeAudio.getTitle());
                System.out.println("song album ==="+activeAudio.getAlbum());
                System.out.println("song artist ==="+activeAudio.getArtist());
                System.out.println("***********************");

            }else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }
        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed.
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.println("media player complete hoye geche");
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        stopMedia();
        stopSelf();
        if (isRepeat){
            repeatSong();
        }else if(isShuffle) {
            suffleSong();
        }else {
            skipToNext();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    }

    public void playMediaPlayer(){
       // int lastsongindex=preferences.getInt(Allkey.LAST_SONG_INDEX,0);


    }


    //Handle errors
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation.
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info.
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        playMedia();
        System.out.println("LSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSL");
        System.out.println("last song===="+activeAudio.getData());
        System.out.println("last song===="+audioIndex);
        System.out.println("LSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSLSL");
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString(Allkey.LAST_SONG,activeAudio.getData());
        editor.putInt(Allkey.LAST_SONG_INDEX,audioIndex);
        editor.commit();
        //getSharedPreferences("pref",MODE_PRIVATE).edit().putString("lastsong",activeAudio.getData()).commit()
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //Invoked when the audio focus of the system is updated.
        switch (focusChange) {
            //The service gained audio focus, so it needs to start playing.
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null)
                    initMediaPlayer();

                else if (!mediaPlayer.isPlaying())
                    mediaPlayer.start();
                    mediaPlayer.setVolume(1.0f, 1.0f);

                break;

                //The service lost audio focus,
            // the user probably moved to playing media on another app, so release the media player.
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    public class LocalBinder extends Binder {
        public MediaplayerService getService() {
            return MediaplayerService.this;
        }
    }

    public void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopMedia() {
        System.out.println("@@@@@@@@@@@@@");
        System.out.println("stopMedia()");
        System.out.println("@@@@@@@@@@@@@");
        if (mediaPlayer == null){
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@");
            System.out.println("media player stop hoye geche");
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@");
        }
    }

    public void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    public void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();

        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);

        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                            musicPlaying=false;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                       if (musicPlaying==false){
                            musicPlaying=true;
                           if (mediaPlayer != null) {
                               if (ongoingCall) {
                                   ongoingCall = false;
                                   resumeMedia();
                               }
                           }

                       }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get the new media index form SharedPreferences
            StorageUtil storageUtil=new StorageUtil(getApplicationContext());
            audioIndex =storageUtil.loadAudioIndex();
            //audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    public void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
                R.drawable.image5); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }
    public void repeatSong() {

        if (audioIndex !=-1 &&  audioIndex<=audioList.size()) {
            activeAudio = audioList.get(audioIndex);
        } else {
            activeAudio = audioList.get(++audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
        System.out.println("####################################################################");
        System.out.println("repeat song index=="+audioIndex);
        System.out.println("####################################################################");

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }
    public void suffleSong(){
        if (audioList.size()>0){
            Random random=new Random();
            audioIndex=random.nextInt((audioList.size() - 1) - 0 + 1) + 0;
            activeAudio=audioList.get(audioIndex);
            new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();

        }
    }
    public void skipToNext() {
        if (isShuffle){
            suffleSong();
           setTitleImage();
        }else if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
            setTitleImage();
        } else {
            //get next in playlist
            activeAudio = audioList.get(++audioIndex);
            setTitleImage();
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    public void skipToPrevious() {
        if (isShuffle){
            suffleSong();
            setTitleImage();
        }else if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
            setTitleImage();
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
            setTitleImage();
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }
    public void repeat(){

        if(isRepeat){
            isRepeat = false;
            Toast.makeText(getApplicationContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
            //btnRepeat.setImageResource(R.drawable.btn_repeat);
        }else{
            // make repeat to true
            isRepeat = true;
            Toast.makeText(getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
            // make shuffle to false
            isShuffle = false;
            //btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
            //btnShuffle.setImageResource(R.drawable.btn_shuffle);
        }
    }
    public void suffle(){
        if(isShuffle){
            isShuffle = false;
            Toast.makeText(getApplicationContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
            //btnShuffle.setImageResource(R.drawable.btn_shuffle);
        }else{
            // make repeat to true
            isShuffle= true;
            Toast.makeText(getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
            // make shuffle to false
            isRepeat = false;
            //btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
            // btnRepeat.setImageResource(R.drawable.btn_repeat);
        }
    }
    public void setTitleImage(){
        Audio audio=audioList.get(audioIndex);
        this.titleListener.setTitle(audio.getAlbum());
        MediaMetadataRetriever metaRetriever= new MediaMetadataRetriever();
        metaRetriever.setDataSource(audio.getData());
        byte[] data=metaRetriever.getEmbeddedPicture();
        if(data != null)
        {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            this.titleListener.setAlbumArt(bitmap);
        }
        else
        {
            this.titleListener.setAlbumArt();
        }

    }
    public void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.image2); //replace with your own image

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(activeAudio.getArtist())
                .setContentTitle(activeAudio.getAlbum())
                .setContentInfo(activeAudio.getTitle())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    public void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
    public PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaplayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }
    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    public int seekBarGetCurrentPosition(){
        //This method is created to get SongCurrentPosition from mediaplayer for seekbar
        if(mediaPlayer!=null&&mediaPlayer.isPlaying()){
            currentPosition=mediaPlayer.getCurrentPosition();
        }
        return currentPosition;
    }
    public int seekBarGetTotalDuration(){
        if (mediaPlayer!=null&&mediaPlayer.isPlaying()){
            totalDuration=mediaPlayer.getDuration();
        }
        return totalDuration;
    }
}