package com.lalongooo.videocompressor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yovenny.videocompress.MediaController;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {
    private static final int RESULT_CODE_COMPRESS_VIDEO = 3;
    private static final String TAG = "MainActivity";
    private TextView sourceFilePath , sourceFileSize  , compressionElapsedTime , compressionFileInfo;
    private ProgressBar progressBar;

    public static final String APP_DIR = "VideoCompressor";

    public static final String COMPRESSED_VIDEOS_DIR = "/Compressed Videos/";

    public static final String TEMP_DIR = "/Temp/";

    String outPath = "";

    int initialDelay = 1000; //first update in miliseconds
    int period = 1000;      //nexts updates in miliseconds

    int totalTime = 0;

    Timer timer = null;

    Time time =null;

    private int MAX_FILE_SIZE = 50 ;  // 50 MB


    public static void try2CreateCompressDir() {
        File f = new File(Environment.getExternalStorageDirectory(), File.separator + APP_DIR);
        f.mkdirs();
        f = new File(Environment.getExternalStorageDirectory(), File.separator + APP_DIR + COMPRESSED_VIDEOS_DIR);
        f.mkdirs();
        f = new File(Environment.getExternalStorageDirectory(), File.separator + APP_DIR + TEMP_DIR);
        f.mkdirs();
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        sourceFilePath = (TextView) findViewById(R.id.editText);
        sourceFileSize = (TextView) findViewById(R.id.sourcefilesize);
        compressionElapsedTime = (TextView) findViewById(R.id.time);
        compressionFileInfo = (TextView) findViewById(R.id.file_info);

        findViewById(R.id.btnSelectVideo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sourceFilePath.setVisibility(View.GONE);
                sourceFileSize.setVisibility(View.GONE);
                compressionElapsedTime.setVisibility(View.GONE);
                compressionFileInfo.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(intent, RESULT_CODE_COMPRESS_VIDEO);
            }
        });


    }

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {

            String reqTime = null;

            if(totalTime <60)
            {
                reqTime = totalTime + " seconds" ;
            }
            else
            {
                int minutes = (totalTime % 3600) / 60;
                int seconds = totalTime % 60;
                reqTime = ""+ minutes + "  minute  " + seconds + " seconds" ;
            }

            compressionElapsedTime.setText(reqTime); //this is the textview
        }
    };



    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if (resCode == Activity.RESULT_OK && data != null) {
           Uri uri = data.getData();

            File output = new File(uri.getPath());
//            Uri outputUri= FileProvider.getUriForFile(this,  BuildConfig.APPLICATION_ID+".provider" , output);

            if (reqCode == RESULT_CODE_COMPRESS_VIDEO) {
                if (uri != null) {
                   // Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);

                    String path = "";
                    if (Build.VERSION.SDK_INT < 11)
                        path = RealPathUtils.getRealPathFromURI_BelowAPI11(MainActivity.this, uri);

                        // SDK >= 11 && SDK < 19
                    else if (Build.VERSION.SDK_INT < 19)
                        path = RealPathUtils.getRealPathFromURI_API11to18(MainActivity.this, uri);

                        // SDK > 19 (Android 4.4)
                    else
                        path = RealPathUtils.getRealPathFromURI_API19(MainActivity.this, uri);
                    Log.d(TAG, "File Path: " + path);
                    // Get the file instance

                    sourceFilePath.setVisibility(View.VISIBLE);
                    sourceFileSize.setVisibility(View.VISIBLE);
                    sourceFilePath.setText(path);

                    String fileSize = getFileSize(path);
                    sourceFileSize.setText("Original File Size::  "  +fileSize);

                    /*try {
                        if (cursor != null && cursor.moveToFirst()) {
                            String path=cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                            sourceFilePath.setVisibility(View.VISIBLE);
                            sourceFileSize.setVisibility(View.VISIBLE);
                            sourceFilePath.setText(path);
                            String fileSize = getFileSize(path);
                            sourceFileSize.setText("Original File Size::  "  +fileSize);

                        }else {
                            sourceFilePath.setVisibility(View.VISIBLE);
                            sourceFileSize.setVisibility(View.VISIBLE);
                            sourceFilePath.setText(uri.getPath());

                            String fileSize = getFileSize(uri.getPath());
                            sourceFileSize.setText("Original File Size::  "  +fileSize);
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }*/
                }
            }
        }
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void compress(View v) {

        int sourceFileSize = originalFileSize (sourceFilePath.getText().toString());

        int width = getVideoWidth(sourceFilePath.getText().toString());

        Log.d("debug" , "Original file size:" + sourceFileSize );

        Log.d("debug" , "width:: " + width );

        if(sourceFileSize <= MAX_FILE_SIZE && width > 640)
        {
        try2CreateCompressDir();

        compressionElapsedTime.setVisibility(View.VISIBLE);
        compressionElapsedTime.setText("");
        totalTime =0;
        timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                totalTime = totalTime + 1;
                mHandler.obtainMessage(1).sendToTarget();
            }
        };
        timer.scheduleAtFixedRate(task, initialDelay, period);

         outPath = "";
         outPath=Environment.getExternalStorageDirectory()
                        + File.separator
                        + APP_DIR
                        + COMPRESSED_VIDEOS_DIR
                        +"VIDEO_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4";
        new VideoCompressor().execute(sourceFilePath.getText().toString(),outPath);

        }
        else if ( width <= 640)
        {
            compressionFileInfo.setVisibility(View.VISIBLE);

            String fileSize = getFileSize(sourceFilePath.getText().toString());

            String text = String.format(Locale.US, "%s\nOutput: %s\n \n  File Size: %s", "", "File is already compressed one", fileSize);
            //   compressionFileInfo.setText("Compressed File Size::  "  +fileSize);
            compressionFileInfo.setText(text);
        }
        else
        {
            Toast.makeText(MainActivity.this, "You can't upload a file more than " + MAX_FILE_SIZE + " MB !!", Toast.LENGTH_SHORT).show();
        }
    }

    class VideoCompressor extends AsyncTask<String, Void, Boolean>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG,"Start video compression");
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return MediaController.getInstance().convertVideo(params[0],params[1]);
        }

        @Override
        protected void onPostExecute(Boolean compressed) {
            super.onPostExecute(compressed);
            progressBar.setVisibility(View.GONE);
            compressionFileInfo.setVisibility(View.VISIBLE);

            timer.cancel();

            String fileSize = getFileSize(outPath);

            String text = String.format(Locale.US, "%s\nOutput Path: %s\n \n Compressed File Size: %s", "", outPath, fileSize);
         //   compressionFileInfo.setText("Compressed File Size::  "  +fileSize);
            compressionFileInfo.setText(text);

            if(compressed){
                Log.d(TAG,"Compression successfully!");
            }
        }
    }

    private String getFileSize(String path)
    {
        String value = null;
        if(path!=null && !path.equalsIgnoreCase(""))
        {
        File imageFile = new File(path);
        float length = imageFile.length() / 1024f; // Size in KB

        if(length >= 1024)
            value = length/1024f+" MB";
        else
            value = length+" KB";

        }
        return value;
    }


    private int originalFileSize(String path)
    {
            int value = 0;
            if(path!=null && !path.equalsIgnoreCase(""))
            {
                File imageFile = new File(path);
                float length = imageFile.length() / 1024f; // Size in KB

                if(length >= 1024)
                    value = (int) Math.ceil(length/1024f);
                else
                    value = 1;
            }
            return value;
    }

    private int getVideoWidth(String path)
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

        int w =0;
        if(width!=null)
        {
             w = Integer.parseInt(width);
        }

        return w;
    }
}