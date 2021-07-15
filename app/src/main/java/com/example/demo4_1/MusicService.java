package com.example.demo4_1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.icu.text.CaseMap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.LongDef;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

import static com.example.demo4_1.MainActivity.TITLE;

public class MusicService extends Service {

    MediaPlayer mMediaPlayer;

    String title;
    String artist;

    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "Music channel";
    NotificationManager mNotificationManager;

    public MusicService() {
    }

    @Override
    public void onDestroy(){
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        super.onDestroy();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        String data = intent.getStringExtra(MainActivity.DATA_URI);
        Uri dataUri = Uri.parse(data);

        if(mMediaPlayer != null){
            try {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(getApplicationContext(), dataUri);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Music Channel", NotificationManager.IMPORTANCE_HIGH);

            if(mNotificationManager != null){
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);

        title = intent.getStringExtra(MainActivity.TITLE);
        artist = intent.getStringExtra(MainActivity.ARTIST);

        Notification notification = builder.setContentTitle(title).setContentText(artist).setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent).build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        return super.onStartCommand(intent, flags, startID);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}