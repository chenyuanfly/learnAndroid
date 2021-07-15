package com.example.demo4_1;

import androidx.annotation.LongDef;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private BottomNavigationView navigation;
    private TextView tvBottomTitle;
    private TextView tvBottomArtist;
    private ImageView ivAlbumThumbnail;
    private ImageView ivPlay;

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

    private final String SELECTION =
            MediaStore.Audio.Media.IS_MUSIC + " = ? " + " AND " +
                    MediaStore.Audio.Media.MIME_TYPE + " LIKE ? ";
    private final String[] SELECTION_ARGS = {
            Integer.toString(1),
            "audio/mpeg"
    };

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
    public void onClick(View v){
        Log.d("this: ", "click!");
    }

    @Override
    protected void onStart(){
        super.onStart();
        if(mMediaPlayer == null){
            mMediaPlayer = new MediaPlayer();
        }
    }

    @Override
    protected void onStop(){
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            Log.d("this: ", "onStop invoked!");
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        navigation = findViewById(R.id.navigation);
        LayoutInflater.from(MainActivity.this).inflate(R.layout.bottom_media_toolbar, navigation, true);


        ivPlay = navigation.findViewById(R.id.iv_play);
        tvBottomTitle = navigation.findViewById(R.id.tv_bottom_title);
        tvBottomArtist = navigation.findViewById(R.id.tv_bottom_artist);
        ivAlbumThumbnail = navigation.findViewById(R.id.iv_thumbnail);

        if(ivPlay != null){
            ivPlay.setOnClickListener(MainActivity.this);
        }

        navigation.setVisibility(View.GONE);

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

                        Log.d("this", "albumArt: " + albumArt);

                        Glide.with(MainActivity.this).load(albumArt).into(ivAlbumThumbnail);
                        cursor.close();
                    }
                }
            }
        }
    };
}