package com.example.herhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    Button herHomeButton;
    MediaPlayer mp;
    String AudioSavePathInDevice = null;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    Random random ;
    MediaRecorder myAudioRecorder;
    public static final int RequestPermissionCode = 1;
    LinearLayout recordingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        herHomeButton=(Button)findViewById(R.id.herHome);
        final Context context =this;
        mp = MediaPlayer.create(context, R.raw.herhomevoice);
        random = new Random();
        recordingLayout=(LinearLayout)findViewById(R.id.recordingAudio);

        AWSMobileClient.getInstance().initialize(this).execute();

        herHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission()){
                    mp.start();
                    voidInitilizeRecorder();
                }else {
                    requestPermission();
                }

            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                try {
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();
                    recordingLayout.setVisibility(View.VISIBLE);
                    delayStopRecording();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
    }

    private void voidInitilizeRecorder(){
        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        AudioSavePathInDevice = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + CreateRandomAudioFileName(5) + "AudioRecording.3gp";
        Toast.makeText(MainActivity.this,AudioSavePathInDevice,Toast.LENGTH_LONG).show();
        myAudioRecorder.setOutputFile(AudioSavePathInDevice);
    }

    MediaPlayer mediaPlayer;

    private void delayStopRecording(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(myAudioRecorder != null){
                    myAudioRecorder.stop();
                    myAudioRecorder.release();
                    uploadFileonS3();
                }
            }
        }, 10000);
    }

    public void uploadFileonS3(){
        // KEY and SECRET are gotten when we create an IAM user above

        AmazonS3Client s3Client = new AmazonS3Client(credentials);

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(s3Client)
                        .build();

        // "jsaS3" will be the folder that contains the file
        File file=new File(AudioSavePathInDevice); //
        TransferObserver uploadObserver =
                transferUtility.upload("herhack/" + "herhome.3gp", file);


        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed download.
                    recordingLayout.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this,"Done",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;
//                Toast.makeText(MainActivity.this,Integer.toString(percentDone),Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
                Toast.makeText(MainActivity.this,ex.getMessage().toLowerCase(),Toast.LENGTH_LONG).show();

            }

        });

        // If your upload does not trigger the onStateChanged method inside your
        // TransferListener, you can directly check the transfer state as shown here.
                if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
        }
    }














    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    public String CreateRandomAudioFileName(int string){
        StringBuilder stringBuilder = new StringBuilder( string );
        int i = 0 ;
        while(i < string ) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));

            i++ ;
        }
        return stringBuilder.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length> 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(MainActivity.this, "Permission Granted",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this,"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }



}
