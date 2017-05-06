package com.example.leanhkhoi.android_recorder_app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer.OnCompletionListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.SearchView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

import android.widget.Button;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareModel;
import com.facebook.share.model.ShareModelBuilder;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.SendButton;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_CODE_RESOLUTION = 1 ;
    //for facebook
    CallbackManager callbackManager;

    //for google drive
    GoogleApiClient mGoogleApiClient = null;
    private static final String TAG = "Google Drive Activity";
    private boolean fileOperation = false;

    private String outputFile = null;
    private Button start, stop,play, pause, resume, backtomain; //backtomain là btn khi ta dang xem danh sach mà muon quay lai man hinh chinh
    private EditText etFileName;
    private TextView timerView;
    private ListView listRecordFileView;
    private RelativeLayout mainLayout;
    private TimerStruct tS;

    private SendButton sendButton;

    ArrayList<String> fileNames = new ArrayList<String>(); //danh sach cac file phuc vu cho chung nang pause/resume

    String currentFileName; //ten file hien tai se luu khi qua trinh ghi am ket thuc
    private static Menu menu; //giu menu cua actionbar

    //thong cac thong so cua ban record
    private static int RECORDER_SAMPLERATE;
    private static int RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static int RECORDER_CHANNELS_IN ;
    private static int RECORDER_AUDIO_ENCODING;

    //doi tuong record
    private AudioRecord recorder = null;

    //doi tuong de phat hoạc dung file am thanh
    private static MediaPlayer m = new MediaPlayer();


    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isPausing = false;
    int bufferSize; //la vung nho luu giu data moi lần ghi vao file (khi dang thu)


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
       // FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        try {
            setWidgets();

            //

            //Kiem tra thu muc va don dep rac trong thu muc
            ValidateFileBeforRun();
            //cancel Notification
            CancelCurrentNotification();

            //dung moi player dang phat
            if(m!=null)
                m.stop();
            Toast.makeText(getBaseContext(),"create",Toast.LENGTH_LONG).show();

            //cai dat cho chuc nang share face


        } catch (IOException e) {
            e.printStackTrace();
        }

        // this part is optional

    }

    //khai bao cac UI
    private void setWidgets() throws IOException {

        start = (Button)findViewById(R.id.start); //button
        stop = (Button)findViewById(R.id.stop); //button
        play = (Button)findViewById(R.id.play); //button
        pause = (Button)findViewById(R.id.pause);   //button
        resume = (Button) findViewById(R.id.resume); //button
        etFileName = (EditText) findViewById(R.id.et_filename); //Edit text
        listRecordFileView = (ListView) findViewById(R.id.listRecordView) ; //listview
        mainLayout = (RelativeLayout) findViewById(R.id.componentLayout); //layout
        backtomain = (Button) findViewById(R.id.backtomain); //button
        timerView = (TextView)findViewById(R.id.timer); //textview

        //send button cho facebook
        sendButton = (SendButton)findViewById(R.id.fb_send_button);



        //setButtonTint(start,ColorStateList.valueOf(Color.GREEN)); //background tint for start button
        SetOnTouchForButtonStart(); // set event touch for start button
        SetDataAndEventForListView(); //set data va event cho ListView
        SetEditTextEvent();

        tS = new TimerStruct();
        tS.setTextView(timerView); // set text view for TimerStruct class

        stop.setEnabled(false);
        play.setEnabled(false);
        pause.setEnabled(false);
        stop.setBackgroundColor(Color.WHITE);
        play.setBackgroundColor(Color.WHITE);
        pause.setBackgroundColor(Color.WHITE);

    }

    //chia se facebook bang viet status
    /*public void shareface (View view){
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                    .setContentTitle("Hello Facebook")
                    .setContentDescription(
                            "The 'Hello Facebook' sample  showcases simple Facebook integration")
                    .setContentUrl(Uri.parse("http://developers.facebook.com/android"))
                    .build();

            shareDialog.show(linkContent);
        }
    }
      @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
    */
    //help perform click facebook
     public void shareface (View view){
        sendButton.performClick();
     }
    //chia se qua messager
    public void sharebymessage (View view){
        callbackManager = CallbackManager.Factory.create();

       ///////////////// //send IMAGE   //////////////////////////////
      /*  String photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/1.png";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        final Bitmap image = BitmapFactory.decodeFile(photoPath, options);

        SharePhoto photo =  new SharePhoto.Builder()
                .setBitmap(image)
                .build();
        ShareContent content = new SharePhotoContent.Builder()
                .addPhoto(photo)
                .build();*/

      //////////// send video /////////////////////////////////
        Uri voiceFileUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Recorders/boemlacanbo.wav");
        ShareVideo video = new ShareVideo.Builder()
                .setLocalUrl(voiceFileUri)
                .build();
        ShareVideoContent content = new ShareVideoContent.Builder()
                .setVideo(video)
                .build();
        Log.d("asdas","aaaa");
        sendButton.setShareContent(content);

        sendButton.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
    }


    //control file google drive
    public void onGoogleDrive (View view){

    }



    //////////////////////////////////Phan nay cho chuc nang Chinh ghi am /////////////////////////////////////
    //////////////////////////////////////////////////////////////////
    //Bắt sự kiên mouse up khi muon bat dau thu am
    private void SetOnTouchForButtonStart() {
        start.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                   // start.setBackgroundColor(Color.BLUE);
                    //setButtonTint(start,ColorStateList.valueOf(Color.BLUE));

                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    //start.setBackgroundColor(Color.TRANSPARENT);
                    //setButtonTint(start,ColorStateList.valueOf(Color.GREEN));
                    //start.setBackgroundResource(android.R.drawable.presence_audio_online);

                    try {
                        recorder = findAudioRecord();
                        if(recorder==null)
                        {
                            //Toast.makeText(getApplicationContext(), "Audio recorded successfully",       Toast.LENGTH_LONG).show();
                            return false;
                        }

                        recorder.startRecording();

                        isRecording = true;
                        recordingThread = new Thread(new Runnable() {
                            public void run() {
                                writeAudioDataToFile();
                            }
                        }, "AudioRecorder Thread");
                        recordingThread.start();

                    } catch (IllegalStateException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    start.setEnabled(false);

                    stop.setEnabled(true);
                    stop.setBackgroundColor(Color.YELLOW);

                    pause.setEnabled(true);
                    pause.setBackgroundColor(Color.YELLOW);

                    DisplayTimer();

                }

                return true;
            }
        });
    }


    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    //trong luc record thi vua ghi du lieu vao file
    private void writeAudioDataToFile() {
        // Write the output audio in byte
        String filePath = new String();
        for(int i = 0; i < 1000 ; i ++)
        {
            //lay duong dan file trong storage
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Recorders/voice8000samplerate" + i + ".pcm";
            File f = new File(filePath);
            if(!f.exists())
                break;
        }

        //them file vao danh sach file
        fileNames.add(filePath);

        //tao ra vung nho de ghi data
        byte saudioBuffer[] = new byte[bufferSize];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(saudioBuffer, 0, bufferSize);

            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                os.write(saudioBuffer, 0, bufferSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop (View view) {
        // stops the recording activity

        if (recorder != null ) {
            isRecording = false;

            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
        else {
            if ((recorder == null) &&
                    (isPausing == false)) {

                return;
            } else {
                if (isPausing) {
                    isPausing = false;
                }
            }
        }
            String pcmfile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorders/Voice.pcm";

            try {
                mergeParts(fileNames,pcmfile);
                for (int i= 0; i < fileNames.size(); i++)
                {
                    File f = new File(fileNames.get(i));
                    f.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            String wavfile = new String();
            wavfile = etFileName.getText().toString();
            if(wavfile.equals("")) {
                for (int i = 0; i < 1000; i++) {
                    wavfile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorders/voice" + i + ".wav";
                    File f = new File(wavfile);
                    if (!f.exists())
                        break;
                }
            }
            else{
                wavfile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorders/" + wavfile + ".wav";
            }


            //chuyen thanh file wav
            currentFileName = wavfile;
            //convert raw to wav
            File f1 = new File(pcmfile);
            File f2 = new File(wavfile); // The location where you want your WAV file
            try {
            rawToWave(f1, f2);
                } catch (IOException e) {
                     e.printStackTrace();
             }
             f1.delete();

            stop.setEnabled(false);
            stop.setBackgroundColor(Color.WHITE);

            pause.setEnabled(false);
            pause.setVisibility(View.VISIBLE);
            pause.setBackgroundColor(Color.WHITE);

            resume.setEnabled(false);
            resume.setVisibility(View.GONE);

            play.setEnabled(true);
            play.setBackgroundColor(Color.YELLOW);
            tS.setStop(true);

    }
    public void PlayShortAudioFileViaAudioTrack(View view) throws IOException{
        // We keep temporarily filePath globally as we have only two sample sounds now..
        Log.d("Catch Event","Catched eventt!");
        String filePath  = "/storage/emulated/0/voice8K16bitmono.pcm";
        if (filePath==null)
            return;

        //Reading the file..
        File file = new File(filePath); // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"
        byte[] byteData = new byte[(int) file.length()];


        FileInputStream in = null;
        try {
            in = new FileInputStream( file );
            in.read( byteData );
            in.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Set and push to audio track..
        int intSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING);


        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING, intSize, AudioTrack.MODE_STREAM);
        if (at!=null) {
            at.play();
            // Write the byte array to the track
            at.write(byteData, 0, byteData.length);
            at.stop();
            at.release();
        }
        else
            Log.d(TELECOM_SERVICE, "audio track is not initialised ");

    }

    public void play(View view) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException{
        if(m.isPlaying()){
            m.stop();
        }
        m = new MediaPlayer();

        if(new File(currentFileName).exists())
        m.setDataSource(currentFileName);

        m.prepare();
        m.start();
        // new File(outputFile).delete();
        Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_LONG).show();

    }
    public void pause(View view) throws  IllegalArgumentException, SecurityException, IllegalStateException, IOException{
        if(recorder!=null) {
            isRecording = false;
            isPausing = true;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;

            pause.setEnabled(false);
            pause.setVisibility(View.GONE);

            stop.setEnabled(true);
            stop.setBackgroundColor(Color.YELLOW);

            resume.setEnabled(true);
            resume.setVisibility(View.VISIBLE);
            resume.setBackgroundColor(Color.YELLOW);

            play.setEnabled(false);
            play.setBackgroundColor(Color.WHITE);
            tS.setStop(true);
        }
    }
    public void resume(View view) throws  IllegalArgumentException, SecurityException, IllegalStateException, IOException{

        try {
            recorder = findAudioRecord();
            recorder.startRecording();

            isRecording = true;
            isPausing = false;
            recordingThread = new Thread(new Runnable() {
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");
            recordingThread.start();

        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        stop.setEnabled(true);
        stop.setBackgroundColor(Color.YELLOW);

        pause.setEnabled(true);
        pause.setBackgroundColor(Color.YELLOW);
        pause.setVisibility(View.VISIBLE);

        resume.setEnabled(false);
        resume.setVisibility(View.GONE);

        DisplayTimer();

    }


    //bat su kien khi nguoi dung nhap vao o edit text
    public void SetEditTextEvent (){
        etFileName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Validate value of edit text
                //Log.d("S values", s.toString() + " " + start + " " + before + " " + count);
                TextView errortv = (TextView)findViewById(R.id.tv_error_filename);

                String fullpath = Environment.getExternalStorageDirectory() + "/Recorders/" + s.toString() + ".wav";
                File f = new File(fullpath);

                if(!s.toString().equals("")) {
                    if (f.exists()) {
                        errortv.setVisibility(View.VISIBLE);
                        stop.setEnabled(false);
                    } else {
                        errortv.setVisibility(View.GONE);
                        stop.setEnabled(true);
                    }
                }
                else{
                    stop.setEnabled(true);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    /////////////////////////////////////////////////////////////
    /////////////////////////////Ket thuc cac chuc nang chinh ////////////////////////////////////



    ///////////////Phan nay cho chuc nang Ve Danh sach Cac ban Record//////////////////////////////////////////

    //////////////////////Hiển thi phan listview ////////////////////////////////////
    public void selectinlist(View view){
        listRecordFileView.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.GONE);
        backtomain.setVisibility(View.VISIBLE);
        SetDataAndEventForListView();
        MenuItem mni = menu.findItem(R.id.action_rename);
        mni.setVisible(false);
    }


    //////////////////////Quay tro lai man hinh chinh
    public void backtomain(View view){
        listRecordFileView.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
        backtomain.setVisibility(View.GONE);

        //khi back lai thi cac item ko dung se an di
        MenuItem mi = menu.findItem(R.id.action_unselected);
        mi.setVisible(false);
        mi = menu.findItem(R.id.action_selectAll);
        mi.setVisible(false);
        mi = menu.findItem(R.id.action_delete);
        mi.setVisible(false);
        mi = menu.findItem(R.id.action_rename);
        mi.setVisible(true);
    }


    //create action bar tren dau app
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //mapping menu
        getMenuInflater().inflate(R.menu.menu, menu);
        this.menu = menu;
        //lay  dịch vụ search cua app
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        //lay searchview tu action_search trong menu

        //search view de hien thi khung search de dien text vao
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
       // if(searchView != null)
       // {
          //  if(searchManager != null)
           // {
                //dong nay de thiet lap search view co the dung dc
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
           // }

            //searchView.setSearchableInfo(searchManager
                   // .getSearchableInfo(getComponentName()));
        //}
        return true;
    }

    //su kien khi chọn 1 item trong menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each action item click
        switch (item.getItemId()) {
            case R.id.action_search:
                // search action
                Toast.makeText(this.getBaseContext(), "press search", Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_selectAll:
                SelectAll();
                return true;
            case R.id.action_unselected:
                CancelSelectAll();
                return true;
            case R.id.action_rename:
                RenameRecordFile();
                return true;
            case R.id.action_help:
                // help action
                return true;
            case R.id.action_settings:

                return true;
            case R.id.action_delete:
                DeleteRecordFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    ///Vung nay de xu lý List File Record
    ////*   ////////////////////////////////   ///////////////////////////////////////  */
    ////////////
    //tinh trnag hien tai cua Ban nhac dang phat ~ hien thi trên notification
    private static String state=null;

    //thoi diem pause ban nhac dang phat
    private static int pauseTime = 0;


    RecordFile rf = null; //item dang phat chọn trong listview
    ///Set du lieu cho listview
    private void SetDataAndEventForListView() {

          final List<RecordFile> listfileinfo = getFileFromStorage();
       // List<RecordFile> listfileinfo = getFileFromStorage();


        listRecordFileView.setAdapter(new RecordListAdapter(this,listfileinfo));
        if(rf != null) {
            RecordListAdapter r = (RecordListAdapter)listRecordFileView.getAdapter();
            r.setCurrentPosRunningByName(rf.getName(),state);

        }
        listRecordFileView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object o = listRecordFileView.getItemAtPosition(position);
                rf = (RecordFile)o;
                String fullPath = Environment.getExternalStorageDirectory().toString()+"/Recorders" + "/" + rf.getName();

                //gui su kien cho adapter thong bao item dang chay
                RecordListAdapter r = (RecordListAdapter)listRecordFileView.getAdapter();
                r.setCurrentPosRunning(rf,"Running");
                final Button btnPauseRun = (Button)view.findViewById(R.id.btn_pause_run);
                btnPauseRun.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(state.equals("Running")){
                            btnPauseRun.setText("Pausing");
                            m.pause();
                            pauseTime = m.getCurrentPosition();
                            state = "Pausing";
                            Notification();
                        }
                        else{
                            btnPauseRun.setText("Running");
                            m.seekTo(pauseTime);
                            m.start();
                            state = "Running";
                            Notification();
                        }

                    }
                });
                listRecordFileView.invalidateViews();

                //run record
                if(m!=null) {
                    if (m.isPlaying()) {
                        m.stop();
                    }
                }

                m = new MediaPlayer();
                SetEventEndPlayer();
                if(new File(fullPath).exists())
                    try {
                        m.setDataSource(fullPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                try {
                    m.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                m.start();
                // new File(outputFile).delete();
                Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_LONG).show();
                //CancelCurrentNotification();
                pauseTime = 0;
                state = "Running";
                Notification();
            }

        });
        listRecordFileView.setLongClickable(true);
        listRecordFileView.setOnItemLongClickListener(new OnItemLongClickListener(){

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {


                listRecordFileView.setAdapter(new RecordListSelectAdapter(getBaseContext(),listfileinfo));
                MenuItem mnitem = menu.findItem(R.id.action_delete);
                mnitem.setVisible(true);
                mnitem = menu.findItem(R.id.action_selectAll);
                mnitem.setVisible(true);
                mnitem = menu.findItem(R.id.action_unselected);
                mnitem.setVisible(true);
                //((BaseAdapter) listRecordFileView.getAdapter()).notifyDataSetChanged();
                //listRecordFileView.invalidate();*/
                return false;
            }
        });
        listRecordFileView.invalidateViews();
    }

    //lay cac file record trong thu muc Recorders
    private List<RecordFile> getFileFromStorage() {
        List<RecordFile> filesInfo = new ArrayList<RecordFile>();
        String path = Environment.getExternalStorageDirectory().toString()+"/Recorders";
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            filesInfo.add(new RecordFile( files[i].getName(),(int)(files[i].length()/1024) + 1 ) );
            Log.d("Files", "FileName:" + files[i].getName() + "\n" + "Size : " + (int)(files[i].length()/1024));
        }
        return filesInfo;
    }

    //dánh ach cac item da chọn trong listview
    private static ArrayList<String> filesSelected = new ArrayList<String>();

    //lay danh sach cac file da chon trong Adapter
    public static void eventBackFromListView(ArrayList<String> listName){
        if(listName.size() <= 1) {
            if(listName.size()==0)
            {
                MenuItem menuitem  = menu.findItem(R.id.action_rename);
                menuitem.setVisible(false);
            }else{
                MenuItem menuitem  = menu.findItem(R.id.action_rename);
                menuitem.setVisible(true);
            }
        }
        else {
            MenuItem menuitem = menu.findItem(R.id.action_rename);
            menuitem.setVisible(false);
        }
        filesSelected = listName;
        //Toast.makeText(null,"Nothing to Delete",Toast.LENGTH_LONG).show();

    }

    //Su kien gui ve từ adapter để pause hoặc run ban player dang chay
   /* public static void PauseOrRunCurrnetPlayerFromAdapter(String st){
        if(st.equals("Running")){
            state = "Pausing";
            m.pause();
            pauseTime = m.getCurrentPosition();
        }
        else {
            state = "Running";
            m.seekTo(pauseTime);
            m.start();
        }
    }*/
    //xoa nhung file da chon
    private void DeleteRecordFile() {
        if(filesSelected.size() > 0){
           // Log.d("info", "" + filesSelected.size());
            for (int i = 0; i < filesSelected.size(); i++){
                String fn = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Recorders/" + filesSelected.get(i);
                File f = new File(fn);
                if(f.exists()){
                    f.delete();
                }
            }
        }
        else{
            return;
        }
     // sau kh xoa thi clear danh sach
        filesSelected.clear();

    //cap nhat du lieu o adapter
     RecordListSelectAdapter r = (RecordListSelectAdapter)listRecordFileView.getAdapter();
     r.receiveEventDeleted(getFileFromStorage());
    }

    //doi ten file da chon
    private void RenameRecordFile() {
        if (filesSelected.size() == 1) {

            String fn = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorders/" + filesSelected.get(0);
            File f = new File(fn);
            if (f.exists()) {
                //rename in this place
                CreateRenameDialog(f);
            }

        }
    }

    //chon tat ca cac item
    private void SelectAll(){
        RecordListSelectAdapter r = (RecordListSelectAdapter)listRecordFileView.getAdapter();
        r.reciveEventSelectAll();
    }

    //huy chon tat ca
    private void CancelSelectAll(){
        backtomain(null);
        selectinlist(null);
    }

    ////////////ket thuc vung xu ly list///////////////////////////////////////////////////
    ///////////Ket thuc vung xu ly chuc nang Ve Danh sach Cac ban Record//////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////


    //Chay Dem thoi gian
    private void DisplayTimer() {
        tS.startCount();
    }
    // tS.setStop(true) de dung viec chay dem thoi gian

    private void CreateRenameDialog(final File f) {

        View promptsView = getLayoutInflater().inflate(R.layout.rename_record_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.newfile_name_record);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                //result.setText(userInput.getText());
                                if(userInput.getText().toString().equals("")){
                                    Toast.makeText(getBaseContext(),"Not rename without character",Toast.LENGTH_LONG).show();
                                    return;
                                }
                                else{
                                    String fulpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Recorders/"+userInput.getText().toString() + ".wav";
                                    File newFile = new File(fulpath);
                                    if(newFile.exists()){
                                        Toast.makeText(getBaseContext(),"This file has existed",Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    else{
                                        boolean rs = f.renameTo(newFile);
                                        if(rs)
                                        {
                                            Toast.makeText(getBaseContext(),"Rename Successfull",Toast.LENGTH_LONG).show();
                                            RecordListSelectAdapter r = (RecordListSelectAdapter)listRecordFileView.getAdapter();
                                            r.receiveEventDeleted(getFileFromStorage());
                                        }
                                    }

                                }
                            } //end onclick
                        }) //end onclicklistener
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();

                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] {AudioFormat.ENCODING_PCM_8BIT ,AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        //Log.d("aaaaaaA", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                        //        + channelConfig);

                        int bufferSize_temp = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize_temp != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                           // Toast.makeText(getApplicationContext(), "Audio recorded successfully",       Toast.LENGTH_LONG).show();

                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize_temp);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                RECORDER_SAMPLERATE = rate;
                                RECORDER_AUDIO_ENCODING = audioFormat;
                                RECORDER_CHANNELS_IN = channelConfig;
                                bufferSize = bufferSize_temp;
                                Toast.makeText(this.getBaseContext(), rate + " " + audioFormat + " " + channelConfig + " " + bufferSize_temp + " " + AudioRecord.ERROR_BAD_VALUE, Toast.LENGTH_LONG).show();
                                return recorder;

                            }
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
        return null;
    }
    public void mergeParts ( ArrayList<String> nameList, String DESTINATION_PATH ) throws IOException {
        File[] file = new File[nameList.size()];
        byte AllFilesContent[] = null;

        int TOTAL_SIZE = 0;
        int FILE_NUMBER = nameList.size();
        int FILE_LENGTH = 0;
        int CURRENT_LENGTH=0;

        for ( int i=0; i<FILE_NUMBER; i++)
        {
            file[i] = new File (nameList.get(i));
            TOTAL_SIZE+=file[i].length();
        }

        try {
            AllFilesContent= new byte[TOTAL_SIZE]; // Length of All Files, Total Size
            InputStream inStream = null;

            for ( int j=0; j<FILE_NUMBER; j++)
            {
                inStream = new BufferedInputStream( new FileInputStream( file[j] ));
                FILE_LENGTH = (int) file[j].length();
                inStream.read(AllFilesContent, CURRENT_LENGTH, FILE_LENGTH);
                CURRENT_LENGTH+=FILE_LENGTH;
                inStream.close();
            }

        }
        catch (FileNotFoundException e)
        {
            System.out.println("File not found " + e);
        }
        catch (IOException ioe)
        {
            System.out.println("Exception while reading the file " + ioe);
        }
        finally
        {
            OutputStream outStream = null;
            outStream = new BufferedOutputStream(new FileOutputStream(DESTINATION_PATH));
            outStream.write(AllFilesContent);
            outStream.flush();
            outStream.close();
        }

    }
    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 8000); // sample rate
            writeInt(output, RECORDER_SAMPLERATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }
    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }
    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }


    boolean isRegisterReceiver = false;
    //Xu ly Notification va BroastcastRecevier
    private void Notification() {

        isRegisterReceiver=true;
        RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.notification_layout);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_voice)
                        .setContent(remoteViews);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        //Quay tro lai MainActivity
        remoteViews.setOnClickPendingIntent(R.id.btn_noti_tomain, resultPendingIntent);


        ///////////////////////////////////////////////////////////
        //Dung hoac tiep tuc viec phat nhac
        //Nho khai bao trong Manifest
        //tao intent
        //ta khong dung cach phia duoi
       /* Intent closeButton = new Intent("Pause_Run");

        closeButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        //tao pending Intent de gui message
        PendingIntent pendingSwitchIntent = PendingIntent.getBroadcast(this, 0, closeButton, 0);

        //bat sự kiện cho button trong remote view
        remoteViews.setOnClickPendingIntent(R.id.btn_noti_pause, pendingSwitchIntent);
        remoteViews.setOnClickPendingIntent(R.id.btn_noti_run, pendingSwitchIntent);*/
        //

        //ta dung cach nay
        Intent pauseRunintent = new Intent("Notification_PauseRun");
        PendingIntent pendintIntentpauserun = PendingIntent.getBroadcast(this, 0, pauseRunintent, 0);
        registerReceiver(pauserunreceiver, new IntentFilter("Notification_PauseRun"));
        remoteViews.setOnClickPendingIntent(R.id.btn_noti_pause, pendintIntentpauserun);

        if(state.equals("GONE")) remoteViews.setViewVisibility(R.id.btn_noti_pause,View.GONE);
        else {
            remoteViews.setTextViewText(R.id.btn_noti_pause, state);
            remoteViews.setViewVisibility(R.id.btn_noti_pause,View.VISIBLE);
        }
        /////////////////////////////////////////////////////////////////////


        //bat su kien clear
        //day la cach lam thu 2 khong can phai khai bao trong Manifest nhu cach lam tren
        Intent intent = new Intent("Notification_Delete");
        PendingIntent pendintIntentclear = PendingIntent.getBroadcast(this, 0, intent, 0);
        registerReceiver(deletereceiver, new IntentFilter("Notification_Delete"));
        mBuilder.setDeleteIntent(pendintIntentclear);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(123, mBuilder.build());
    }




    //ta khong su dung class pauseRecriver nua vi do la static class, no lam ta kho truy cap vao moi truong cua
    //MainActivity ( la moi truong non-static)
    public static class PauseReceiver extends BroadcastReceiver {
        //hàm bên dưới co chức năng
        // khi nhận message trả về từ broastcast
        //neu nhan nut RUNNING thì xử lý tại dừng bài nhạc
        //neu nhân nut PAUSING thì tiêp tục phát nhạc
        @Override
        public void onReceive(Context context, Intent intent) {


            if(intent.getAction() == "Pause_Run"){
                if(state.equals("Running")){

                    if(m.isPlaying()) {
                        //remoteViews.setTextViewText(R.id.btn_noti_pause, "Pausing");
                        //Notification();

                        state = "Pausing";
                        m.pause();
                        pauseTime = m.getCurrentPosition();

                        Toast.makeText(context,state,Toast.LENGTH_LONG).show();
                    }
                }else{
                    // remoteViews.setTextViewText(R.id.btn_noti_pause,"Running");

                    state = "Running";
                    m.seekTo(pauseTime);
                    m.start();
                }
            }



            //update notification view
           // mNotificationManager.notify(123, mBuilder.build());
            /*AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            final ComponentName provider = new ComponentName(context, this.getClass());
            appWidgetManager.updateAppWidget(provider, remoteViews);*/

        }


    }


    //ta dung cach new truc tiep mot BroadcastReceiver
    private final BroadcastReceiver pauserunreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(state.equals("Running")){
            //remoteViews.setTextViewText(R.id.btn_noti_pause, "Pausing");
            // Notification();
                state = "Pausing";
                m.pause();
                pauseTime = m.getCurrentPosition();

            }else{
                // remoteViews.setTextViewText(R.id.btn_noti_pause,"Running");
                state = "Running";
                m.seekTo(pauseTime);
                m.start();
            }
            Toast.makeText(context,state,Toast.LENGTH_LONG).show();
           // CancelCurrentNotification();

            //dong duoi de dam bao khi thay doi o Notification thi se cap nhat o ListView
            if(rf != null) {
                RecordListAdapter r = (RecordListAdapter)listRecordFileView.getAdapter();
                r.setCurrentPosRunningByName(rf.getName(),state);

            }
            Notification();
        }
    };

    //su kien khi clear notificaiton
    private final BroadcastReceiver deletereceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(m.isPlaying()) {
                m.stop();

            }
            //clear lại hết
            pauseTime = 0;
            state = null;
            rf = null;

            SetDataAndEventForListView();
        }
    };

    //su kien khi nhac ket thuc
    private void SetEventEndPlayer() {
        m.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //set cac gia ti ve mac dinh
                pauseTime = 0;
                state = null;
                rf = null;
                //set cac widget thanh gone
                Log.d("END","END");
                state = "GONE";
                Notification();
                SetDataAndEventForListView();
            }
        });
    }

    public void onStart(){
        super.onStart();
      //  Log.d("onStart : ", timerView.getText().toString());
        tS.setRunInBackground(false);
    }
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {

            /**
             * Create the API client and bind it to an instance variable.
             * We use this instance as the callback for connection and connection failures.
             * Since no account name is passed, the user is prompted to choose.
             */
            Scope sc = Drive.SCOPE_FILE;

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(sc)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        mGoogleApiClient.connect();
       // Log.d("onResume : ", timerView.getText().toString());
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection failed: " + connectionResult.toString());

        if (!connectionResult.hasResolution()) {

            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
            return;
        }

        /**
         *  The failure has a resolution. Resolve it.
         *  Called typically when the app is not yet authorized, and an  authorization
         *  dialog is displayed to the user.
         */

        try {

            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);

        } catch (IntentSender.SendIntentException e) {

            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //callbackManager.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    public void onPause() {
        super.onPause();
       // Log.d("onPause : ", timerView.getText().toString());
    }
    public void onStop(){
        super.onStop();
        if (mGoogleApiClient != null) {

            // disconnect Google Android Drive API connection.
            mGoogleApiClient.disconnect();
        }
       Log.d("onStop : ", timerView.getText().toString());
        //neu het thu
        if(tS.getStop())
        tS.setRunInBackground(true);
    }

    public  void onDestroy(){
        ValidateFileBeforRun();

        if(isRegisterReceiver) {
            unregisterReceiver(pauserunreceiver);
            unregisterReceiver(deletereceiver);
        }
        super.onDestroy();
    }
    private void CancelCurrentNotification() {
        String ns = Context.NOTIFICATION_SERVICE;

        NotificationManager nMgr = (NotificationManager) this.getSystemService(ns);

        if(nMgr != null)
        nMgr.cancel(123);
    }

    private void ValidateFileBeforRun(){
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Recorders";

        File directory = new File(path);
        File[] files = directory.listFiles();

        if(files.length!=0) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().substring(files[i].getName().length() - 3, files[i].getName().length()).equals("pcm"))
                    files[i].delete();

            }
        }
    }
    //bat su kien khi mo thanh EXPAN_STATUS_BAR

}
