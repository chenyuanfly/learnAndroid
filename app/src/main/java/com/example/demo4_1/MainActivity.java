package com.example.demo4_1;

import androidx.annotation.LongDef;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private BottomNavigationView navigation;
    private TextView tvBottomTitle;
    private TextView tvBottomArtist;
    private ImageView ivAlbumThumbnail;
    private MediaPlayer mMediaPlayer = null;
    private final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSION_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private ContentResolver mContentResolver;
    private ListView mPlaylist;
    private MediaCursorAdapter mCursorAdapter;
    private Cursor mCursor;
    public static final String DATA_URI = "com.example.demo4_1.DATE_URI";
    public static final String TITLE = "com.example.demo4_1.TITLE";
    public static final String ARTIST = "com.example.demo4_1.ARTIST";
    private final String SELECTION =
            MediaStore.Audio.Media.IS_MUSIC + " = ? " + " AND " +
                    MediaStore.Audio.Media.MIME_TYPE + " LIKE ? ";
    private final String[] SELECTION_ARGS = {
            Integer.toString(1),
            "audio/mpeg"
    };

    /*          ！！！！！！！       */
    private MusicService mService;
    private boolean mBound = false;
    private Boolean mPlayStatus = true;
    private ImageView ivPlay;
    public static final int UPDATE_PROGRESS = 1;
    private ProgressBar pbProgress;
    public static final String ACTION_MUSIC_START =
            "com.example.demo4_1.ACTION_MUSIC_START";
    public static final String ACTION_MUSIC_STOP =
            "com.example.demo4_1.ACTION_MUSIC_STOP";
    private MusicReceiver musicReceiver;

    /*          ？？？？？？？      */
    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(
                ComponentName componentName, IBinder iBinder) {
            MusicService.MusicServiceBinder binder =
                    (MusicService.MusicServiceBinder) iBinder;

            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(
                ComponentName componentName) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onStart(){
        super.onStart();
//        if(mMediaPlayer == null){
//            mMediaPlayer = new MediaPlayer();
//        }
        Intent intent = new Intent(MainActivity.this ,
                MusicService.class);
        bindService(intent , mConn , Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop(){
//        if(mMediaPlayer != null){
//            mMediaPlayer.stop();
//            mMediaPlayer.release();
//            mMediaPlayer = null;
//            Log.d("this: ", "onStop invoked!");
//        }
        unbindService(mConn);
        mBound = false;
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iv_play) {
            mPlayStatus = !mPlayStatus;
            if (mPlayStatus == true) {
                mService.play();
                ivPlay.setImageResource(
                        R.drawable.ic_baseline_pause_circle_outline_24);
            } else {
                mService.pause();
                ivPlay.setImageResource(
                        R.drawable.ic_baseline_play_circle_outline_24);
            }
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_PROGRESS:
                    int position = msg.arg1;
                    pbProgress.setProgress(position);
                    break;
                default:
                    break;
            }
        }
    };

    private class MusicProgressRunnable implements Runnable {
        public MusicProgressRunnable() {
        }

        @Override
        public void run() {
            boolean mThreadWorking = true;
            while (mThreadWorking) {
                try {
                    if (mService != null) {
                        int position =
                                mService.getCurrentPosition();
                        Message message = new Message();
                        message.what = UPDATE_PROGRESS;
                        message.arg1 = position;
                        mHandler.sendMessage(message);
                    }
                    mThreadWorking = mService.isPlaying();
                    Thread.sleep(100);
                } catch (InterruptedException mie) {
                    mie.printStackTrace();
                }
            }
        }
    }

    //有问题
    public class MusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context , Intent intent) {
            if (mService != null) {
                pbProgress = navigation.findViewById(R.id.progress);
                pbProgress.setMax(mService.getDuration());
                new Thread(new MusicProgressRunnable()).start();
            }
        }
    }

    //初始化数据
    private void initPlaylist(){
        mCursor = mContentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                SELECTION,
                SELECTION_ARGS,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );

        mCursorAdapter.swapCursor(mCursor);
        mCursorAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navigation = findViewById(R.id.navigation);

        mContentResolver = getContentResolver();
        mCursorAdapter = new MediaCursorAdapter(MainActivity.this);

        mPlaylist = findViewById(R.id.lv_playlist);
        mPlaylist.setAdapter(mCursorAdapter);
        //首先检查checkSelfPermission函数是否获得权限
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)){

            }else{
                requestPermissions(PERMISSION_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        }else{
            initPlaylist();
        }

        LayoutInflater.from(MainActivity.this).inflate(R.layout.bottom_media_toolbar, navigation, true);

        ivPlay = navigation.findViewById(R.id.iv_play);
        tvBottomTitle = navigation.findViewById(R.id.tv_bottom_title);
        tvBottomArtist = navigation.findViewById(R.id.tv_bottom_artist);
        ivAlbumThumbnail = navigation.findViewById(R.id.iv_thumbnail);

        if(ivPlay != null){
            ivPlay.setOnClickListener(MainActivity.this);
        }

//      navigation.setVisibility(View.GONE);
        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MUSIC_START);
        intentFilter.addAction(ACTION_MUSIC_STOP);
        registerReceiver(musicReceiver , intentFilter);

        //绑定音乐切换监听事件
        mPlaylist.setOnItemClickListener(setAdapter);
    }

    //请求获取权限并判断如何后续操作
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch(requestCode){
            case REQUEST_EXTERNAL_STORAGE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    initPlaylist();
                }
                break;
            default:
                break;
        }
    }

    private ListView.OnItemClickListener setAdapter = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView <?> adapterView , View view, int i, long l) {
            Cursor cursor = mCursorAdapter.getCursor();

            if (cursor != null && cursor.moveToPosition(i)) {

                int titleIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.TITLE);
                int artistIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.ARTIST);
                int albumIdIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.ALBUM_ID);
                int dataIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.DATA);

                String title = cursor.getString(titleIndex);
                String artist = cursor.getString(artistIndex);
                Long albumId = cursor.getLong(albumIdIndex);
                String data = cursor.getString(dataIndex);

                Uri dataUri = Uri.parse(data);

                if (mMediaPlayer != null) {
                    try {
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(
                                MainActivity.this , dataUri);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                navigation.setVisibility(View.VISIBLE);

                if (tvBottomTitle != null) {
                    tvBottomTitle.setText(title);
                }

                if (tvBottomArtist != null) {
                    tvBottomArtist.setText(artist);
                }

                Uri albumUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        albumId);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        DisplayMetrics dm = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(dm);
                        float density  = dm.density;

                        int width = (int) (40*density);
                        int height = (int) (40*density);

                        Bitmap bitmap = mContentResolver.loadThumbnail(albumUri, new Size(width, height), null);
                        Glide.with(MainActivity.this).load(bitmap).into(ivAlbumThumbnail);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    cursor = mContentResolver.query(
                            albumUri,
                            null,
                            null,
                            null,
                            null
                    );

                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int albumArtIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
                        String albumArt = cursor.getString(albumArtIndex);

                        Glide.with(MainActivity.this).load(albumArt).into(ivAlbumThumbnail);
                        cursor.close();
                    }
                }

                Intent serviceIntent = new Intent(MainActivity.this, MusicService.class);
                serviceIntent.putExtra(MainActivity.DATA_URI, data);
                serviceIntent.putExtra(MainActivity.TITLE, title);
                serviceIntent.putExtra(MainActivity.ARTIST, artist);

                startService(serviceIntent);
            }
        }
    };
}