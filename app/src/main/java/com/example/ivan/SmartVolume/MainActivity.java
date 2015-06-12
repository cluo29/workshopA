package com.example.ivan.SmartVolume;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;


import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Light;
import com.aware.Proximity;
import com.aware.Rotation;
import com.aware.providers.Aware_Provider;

import java.io.IOException;

import libsvm.*;


public class MainActivity extends ActionBarActivity {

    //private static AccelerometerObserver accelObs;
    public static svm_model model; // the sexy model of svm

    //Media Player implementation
    private MediaPlayer mediaPlayer;
    private Button controlPlayerButton;
    private AudioManager audio;
    public static final int UPDATE_THE_VOLUME = 0;
    public VolumeHandler volumeHandler;
    public static boolean isPlayed = false;

    //Accelerometer
    public static double acc_x=0;
    public static double acc_y=0;
    public static double acc_z=0;
    public static long timestamp_acc;
    //rotation
    public static double rotation_x=0;
    public static double rotation_y=0;
    public static double rotation_z=0;
    //public static double rotation_cos;
    public static long timestamp_rot;
    //light
    public static double light=0;
    public static long timestamp_lig;
    //proximity
    public static double proximity=0;
    public static long timestamp_prx;
    //microphone
    public static double microphone=0;
    //volume
    public static int volume=0;
    public static long timestamp_vol;

    private boolean acc_cache = false;
    private boolean rot_cache = false;
    private boolean light_cache = false;
    private boolean prox_cache = false;
    private String label;  //'ear'=0 'table'=1 'other'=2

    public static double[][] sensors_data = new double[60][7];
    public static int trainingState = 0;// 0 means not trained, 1 in training, 2 training complete

//UI Views
    public static RelativeLayout layout;
    public static TextView Text_R_values_0;
    public static TextView Text_R_values_1;
    public static TextView Text_R_values_2;
    public static TextView Text_Lux;
    public static TextView Text_Proximity;
    public static TextView Text_Microphone;
    public static TextView Text_Volume;
    public static TextView Text_Output;


    public Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          while (2==2){ //should be trainingState==2
              //label could be //'ear' 'table' 'other'
              //for ear training!
              //'ear'=0 'table'=1 'other'=2
              int rows_of_data_ear = 0;
              Cursor cursor_ear = getContentResolver().query(Provider.Smart_Volume_Data.CONTENT_URI, null, Provider.Smart_Volume_Data.LABEL + " = 'ear'", null, Provider.Smart_Volume_Data.TIMESTAMP + " DESC LIMIT 20");
              if (cursor_ear != null && cursor_ear.moveToFirst()) {
                int i = 0;
                do{
                    final double column_r_value_0 = cursor_ear.getDouble(cursor_ear.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_0));
                    final double column_r_value_1 = cursor_ear.getDouble(cursor_ear.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_1));
                    final double column_r_value_2 = cursor_ear.getDouble(cursor_ear.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_2));
                    final double column_lux = cursor_ear.getDouble(cursor_ear.getColumnIndex(Provider.Smart_Volume_Data.LUX));
                    final double column_proximity = cursor_ear.getDouble(cursor_ear.getColumnIndex(Provider.Smart_Volume_Data.PROXIMITY));
                    final double column_microphone = cursor_ear.getDouble(cursor_ear.getColumnIndex(Provider.Smart_Volume_Data.MICROPHONE));
                    final double column_volume = cursor_ear.getDouble(cursor_ear.getColumnIndex(Provider.Smart_Volume_Data.VOLUME));
                    //'ear'=0 'table'=1 'other'=2 label
                    sensors_data[i][0] = 0;
                    sensors_data[i][1] = column_r_value_0;
                    sensors_data[i][2] = column_r_value_1;
                    sensors_data[i][3] = column_r_value_2;
                    sensors_data[i][4] = column_lux;
                    sensors_data[i][5] = column_proximity;
                    sensors_data[i][6] = column_microphone;
                    //sensors_data[i][7] = column_volume;
                    i++;
                    rows_of_data_ear++;
                    if(i>19){
                        break;
                    }
                }
                while (cursor_ear.moveToNext());
              }
              if (cursor_ear != null && !cursor_ear.isClosed()) {
                Log.d("SENSORS_DATA_ear","Saved data: " + rows_of_data_ear);
                cursor_ear.close();
              }
              if(rows_of_data_ear<20) //don't do learning, not enough data
              {
                  continue;
              }
              //label could be //'ear' 'table' 'other'
              //for table training!
              //'ear'=0 'table'=1 'other'=2
              int rows_of_data_table = 0;
              Cursor cursor_table = getContentResolver().query(Provider.Smart_Volume_Data.CONTENT_URI, null, Provider.Smart_Volume_Data.LABEL + " = 'table'", null, Provider.Smart_Volume_Data.TIMESTAMP + " DESC LIMIT 20");
              if (cursor_table != null && cursor_table.moveToFirst()) {
                  int i = 20;
                  do{
                      final double column_r_value_0 = cursor_table.getDouble(cursor_table.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_0));
                      final double column_r_value_1 = cursor_table.getDouble(cursor_table.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_1));
                      final double column_r_value_2 = cursor_table.getDouble(cursor_table.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_2));
                      final double column_lux = cursor_table.getDouble(cursor_table.getColumnIndex(Provider.Smart_Volume_Data.LUX));
                      final double column_proximity = cursor_table.getDouble(cursor_table.getColumnIndex(Provider.Smart_Volume_Data.PROXIMITY));
                      final double column_microphone = cursor_table.getDouble(cursor_table.getColumnIndex(Provider.Smart_Volume_Data.MICROPHONE));
                      final double column_volume = cursor_table.getDouble(cursor_table.getColumnIndex(Provider.Smart_Volume_Data.VOLUME));
                      //'ear'=0 'table'=1 'other'=2 label
                      sensors_data[i][0] = 1;
                      sensors_data[i][1] = column_r_value_0;
                      sensors_data[i][2] = column_r_value_1;
                      sensors_data[i][3] = column_r_value_2;
                      sensors_data[i][4] = column_lux;
                      sensors_data[i][5] = column_proximity;
                      sensors_data[i][6] = column_microphone;
                      //sensors_data[i][7] = column_volume;
                      i++;
                      rows_of_data_table++;
                      if(i>39){
                          break;
                      }
                  }
                  while (cursor_table.moveToNext());
              }
              if (cursor_table != null && !cursor_table.isClosed()) {
                  Log.d("SENSORS_DATA_table","Saved data: " + rows_of_data_table);
                  cursor_table.close();
              }
              if(rows_of_data_table<20) //don't do learning, not enough data
              {
                  continue;
              }
              //label could be //'ear' 'table' 'other'
              //for other training!
              int rows_of_data_other = 0;
              Cursor cursor_other = getContentResolver().query(Provider.Smart_Volume_Data.CONTENT_URI, null, Provider.Smart_Volume_Data.LABEL + " = 'other'", null, Provider.Smart_Volume_Data.TIMESTAMP + " DESC LIMIT 20");
              if (cursor_other != null && cursor_other.moveToFirst()) {
                  int i = 40;
                  do{
                      final double column_r_value_0 = cursor_other.getDouble(cursor_other.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_0));
                      final double column_r_value_1 = cursor_other.getDouble(cursor_other.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_1));
                      final double column_r_value_2 = cursor_other.getDouble(cursor_other.getColumnIndex(Provider.Smart_Volume_Data.R_VALUES_2));
                      final double column_lux = cursor_other.getDouble(cursor_other.getColumnIndex(Provider.Smart_Volume_Data.LUX));
                      final double column_proximity = cursor_other.getDouble(cursor_other.getColumnIndex(Provider.Smart_Volume_Data.PROXIMITY));
                      final double column_microphone = cursor_other.getDouble(cursor_other.getColumnIndex(Provider.Smart_Volume_Data.MICROPHONE));
                      final double column_volume = cursor_other.getDouble(cursor_other.getColumnIndex(Provider.Smart_Volume_Data.VOLUME));
                      //'ear'=0 'table'=1 'other'=2 label
                      sensors_data[i][0] = 2;
                      sensors_data[i][1] = column_r_value_0;
                      sensors_data[i][2] = column_r_value_1;
                      sensors_data[i][3] = column_r_value_2;
                      sensors_data[i][4] = column_lux;
                      sensors_data[i][5] = column_proximity;
                      sensors_data[i][6] = column_microphone;
                      //sensors_data[i][7] = column_volume;
                      i++;
                      rows_of_data_other++;
                      if(i>59){
                          break;
                      }
                  }
                  while (cursor_other.moveToNext());
              }
              if (cursor_other != null && !cursor_other.isClosed()) {
                  Log.d("SENSORS_DATA_other","Saved data: " + rows_of_data_other);
                  cursor_other.close();
              }
              if(rows_of_data_other<20) //don't do learning, not enough data
              {
                  continue;
              }

              //enough data, train  //sensors_data
              svm_parameter para1 = new svm_parameter();
              para1.svm_type = svm_parameter.C_SVC;
			  /* svm_type
			  public static final int C_SVC = 0;
			  public static final int NU_SVC = 1;
			  public static final int ONE_CLASS = 2;
			  public static final int EPSILON_SVR = 3;
			  public static final int NU_SVR = 4; */
              para1.kernel_type = svm_parameter.LINEAR;
        	  /*
			  public static final int LINEAR = 0;
			  public static final int POLY = 1;
			  public static final int RBF = 2;
			  public static final int SIGMOID = 3;
			  public static final int PRECOMPUTED = 4;*/
              para1.degree = 2;
              //degree, for poly
              para1.gamma = 0.0203;
              // gamma,	for poly/rbf/sigmoid
              para1.coef0 = 0;
              //coef0, for poly/sigmoid
              para1.cache_size = 300;
              //cache_size; in MB
              para1.C = 1;
              //C, for C_SVC, EPSILON_SVR and NU_SVR
              para1.eps = 1e-3;
              //eps, stopping criteria
              para1.nu = 0.1;
              //nu; for NU_SVC, ONE_CLASS, and NU_SVR
              para1.shrinking = 0;
              //shrinking;	 use the shrinking heuristics
              para1.probability = 0;
              // probability,  do probability estimates
              svm_problem prob1 = new svm_problem();
              Log.d("sensors_data","sensors_data.length: " + sensors_data.length);
              int dataCount = sensors_data.length;
              prob1.y = new double[dataCount];
              prob1.l = dataCount;
              prob1.x = new svm_node[dataCount][];

              for (int i = 0; i < dataCount; i++){
                  double[] features = sensors_data[i];
                  prob1.x[i] = new svm_node[features.length-1];
                  for (int j = 1; j < features.length; j++){
                      svm_node node = new svm_node();
                      node.index = j;
                      node.value = features[j];
                      prob1.x[i][j-1] = node;
                  }
                  prob1.y[i] = features[0];
              }
              Log.d("sensors_data","so far so good" );
              model = svm.svm_train(prob1, para1);
              Log.d("sensors_data","model seems cool" );
              svm_node node1 = new svm_node();
              node1.index=1;
              svm_node node2 = new svm_node();
              node2.index=2;
              svm_node node3 = new svm_node();
              node3.index=3;
              svm_node node4 = new svm_node();
              node4.index=4;
              svm_node node5 = new svm_node();
              node5.index=5;
              svm_node node6 = new svm_node();
              node6.index=6;
              node1.value=rotation_x;
              node2.value=rotation_y;
              node3.value=rotation_z;
              node4.value=light;
              node5.value=proximity;
              node6.value=microphone;
              //node7.value=volume;
              svm_node[] pred = new svm_node[6];
              pred[0]=node1;
              pred[1]=node2;
              pred[2]=node3;
              pred[3]=node4;
              pred[4]=node5;
              pred[5]=node6;
             // pred[6]=node7;
              final double prediction = svm.svm_predict(model,pred);
              Log.d("sensors_data","prediction: " +prediction);

              int buffer_size = AudioRecord.getMinBufferSize(AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM), AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT ) * 10;
              AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM), AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);

              //Audio data buffer
              short[] audio_data = new short[buffer_size];
              double sound_strength=0;

              if( recorder.getState() == AudioRecord.STATE_INITIALIZED ) {
                  if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                      recorder.startRecording();
                  }

                  double now = System.currentTimeMillis();
                  double elapsed = 0;
                  while (elapsed < 700) {
                      //wait...
                      elapsed = System.currentTimeMillis() - now;
                      //Log.d("audio", "Recording... " + elapsed / 1000 + " seconds");
                  }
                  recorder.stop();
                  recorder.read(audio_data, 0, buffer_size);


                  if (audio_data.length != 0) {
                      double amplitude = -1;
                      for (short data : audio_data) {
                          if (amplitude < data) {
                              amplitude = data;
                          }
                      }
                      Log.d("audio", "Recording... "+amplitude);
                      if(amplitude>0)
                          sound_strength=amplitude;
                  }
              }
              final double noise =sound_strength;



              runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      Text_Microphone.setText(""+noise);


                      Text_Output.setText(""+prediction);

                  }
              });

              /*
              //rotation
                public static double rotation_x;
                public static double rotation_y;
                public static double rotation_z;
                 //public static double rotation_cos;
                public static long timestamp_rot;
                //light
                    public static double light;
                    public static long timestamp_lig;
                    //proximity
                    public static double proximity;
                 public static long timestamp_prx;
    //microphone
    public static double microphone;
    //volume
    public static int volume;
    public static long timestamp_vol;
               */







              //send to machine learning
                  //train SVM



              try {
                  Thread.currentThread().sleep(1000);
                  Thread.currentThread().yield();
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
          }



        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //button to play/stop player
        controlPlayerButton = (Button) findViewById(R.id.togglebutton);
        //initializing the MediaPlayer

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        volumeHandler = new VolumeHandler(Looper.getMainLooper());


        Text_R_values_0 = (TextView)findViewById(R.id.R_values_0);
        Text_R_values_1 = (TextView)findViewById(R.id.R_values_1);
        Text_R_values_2 = (TextView)findViewById(R.id.R_values_2);
        Text_Lux = (TextView)findViewById(R.id.Lux);
        Text_Proximity = (TextView)findViewById(R.id.Proximity);
        Text_Microphone = (TextView)findViewById(R.id.Microphone);
        Text_Volume = (TextView)findViewById(R.id.Volume);
        Text_Output = (TextView)findViewById(R.id.Output);

        thread.start();


        //a thread infinitely runs //
        // thread asks database if there are 100 rows of data,
        // yes, i retrieve data. i store it in variables, array, display array to ui; call machine learning. display machine learning result.


        //Register the Content Observer
        //   accelObs = new AccelerometerObserver(new Handler());
        // getContentResolver().registerContentObserver(Accelerometer_Provider.Accelerometer_Data.CONTENT_URI, true, accelObs);

        //Tell AWARE that I want to modify the settings and activate the accelerometer


    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            //Do something
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);

            Log.d("PLAYER","Down " + String.valueOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)));
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            //Do something

            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            Log.d("PLAYER", "UP " + String.valueOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)));
            return true;
        }


        return false;



    }

    @Override
    protected void onResume() {
        super.onResume();
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ACCELEROMETER, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LIGHT, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ROTATION, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_PROXIMITY, true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Accelerometer.ACTION_AWARE_ACCELEROMETER);
        filter.addAction(Light.ACTION_AWARE_LIGHT);
        filter.addAction(Rotation.ACTION_AWARE_ROTATION);
        filter.addAction(Proximity.ACTION_AWARE_PROXIMITY);
        registerReceiver(contextBR, filter);


        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ACCELEROMETER, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LIGHT, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ROTATION, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_PROXIMITY, false);


        if(contextBR != null)
            unregisterReceiver(contextBR);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    //Listener for tougle button start/stop play
    public void onToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        Log.d("PLAYER", "Button toggled");
        if (on) {
            // Start play

            mediaPlayer = MediaPlayer.create(this, R.raw.song);
            mediaPlayer.start();
            isPlayed = true;


            /*
            ContentValues data = new ContentValues();
            data.put(Provider.Smart_Volume_Data.TIMESTAMP, System.currentTimeMillis());
            data.put(Provider.Smart_Volume_Data.DEVICE_ID, Aware.getSetting(this, Aware_Preferences.DEVICE_ID));
            data.put(Provider.Smart_Volume_Data.A_VALUES_0,0);
            data.put(Provider.Smart_Volume_Data.A_VALUES_1,0);
            data.put(Provider.Smart_Volume_Data.A_VALUES_2,0);
            data.put(Provider.Smart_Volume_Data.R_VALUES_0,0);
            data.put(Provider.Smart_Volume_Data.R_VALUES_1,0);
            data.put(Provider.Smart_Volume_Data.R_VALUES_2,0);
            data.put(Provider.Smart_Volume_Data.LUX,0);
            data.put(Provider.Smart_Volume_Data.PROXIMITY,0);
            data.put(Provider.Smart_Volume_Data.MICROPHONE,0);
            data.put(Provider.Smart_Volume_Data.VOLUME,0);

            getContentResolver().insert(Provider.Smart_Volume_Data.CONTENT_URI, data);*/

        } else {
            // Stop play
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;




            isPlayed = false;
        }
    }


    //RadioButtons
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {

            case R.id.ear:
                if (checked)

                    label = "ear";
                    break;
            case R.id.table:
                if (checked)
                    label = "table";
                break;
            case R.id.other:
                if (checked)
                    label = "other";
                break;
        }
    }




    //Handler to update the volume
    private final class VolumeHandler extends Handler {

        public VolumeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_THE_VOLUME: {

                    Log.d("VOLUME","SET VOLUME to " + msg.arg1);

//                    Toast t = Toast.makeText(context, (String) msg.obj,
//                            Toast.LENGTH_SHORT);
//                    t.show();
                }
                default:
                    break;
            }
        }
    }


    private ContextReceiver contextBR = new ContextReceiver();
    public class ContextReceiver extends BroadcastReceiver {



        @Override
        public void onReceive(Context context, Intent intent) {
            //Sensors Data

            //Get the raw data
            //Accelerometer data
            if (intent.getAction().equals(Accelerometer.ACTION_AWARE_ACCELEROMETER)) {
                Log.d("SENSORS", "Received accelerometer data");
                ContentValues acc_data = (ContentValues) intent.getParcelableExtra(Accelerometer.EXTRA_DATA);
                acc_x = acc_data.getAsDouble("double_values_0");
                acc_y = acc_data.getAsDouble("double_values_1");
                acc_z = acc_data.getAsDouble("double_values_2");
                timestamp_acc = (long) acc_data.get("timestamp");
                acc_cache = true;
            }
            //Rotation data
            else if (intent.getAction().equals(Rotation.ACTION_AWARE_ROTATION)) {
                Log.d("SENSORS", "Received rotation data");
                ContentValues rotation_data = (ContentValues) intent.getParcelableExtra(Rotation.EXTRA_DATA);
                rotation_x = rotation_data.getAsDouble("double_values_0");
                rotation_y = rotation_data.getAsDouble("double_values_1");
                rotation_z = rotation_data.getAsDouble("double_values_2");
                //rotation_cos = rotation_data.getAsDouble("double_values_3");
                timestamp_rot = (long) rotation_data.get("timestamp");
                rot_cache = true;
            }
            else if (intent.getAction().equals(Light.ACTION_AWARE_LIGHT)) {
                Log.d("SENSORS", "Received light data");
                //Light
                ContentValues light_data = (ContentValues) intent.getParcelableExtra(Light.EXTRA_DATA);
                if (light_data != null) {
                    Log.d("SENSORS", "Light sensor AVAILABLE");
                    light = light_data.getAsDouble("double_light_lux");
                    timestamp_lig = (long) light_data.get("timestamp");
                    light_cache = true;
                } else {
                    Log.d("SENSORS", "Light sensor UNAVAILABLE");
                    light = 0;
                }
            }
            //Proximity
            else if (intent.getAction().equals(Proximity.ACTION_AWARE_PROXIMITY)) {
                Log.d("SENSORS", "Received proximity data");
                ContentValues proximity_data = (ContentValues) intent.getParcelableExtra(Proximity.EXTRA_DATA);
                if (proximity_data != null) {
                    Log.d("SENSORS", "Proximity sensor AVAILABLE");
                    proximity = proximity_data.getAsDouble("double_proximity");
                    timestamp_prx = (long) proximity_data.get("timestamp");
                    prox_cache = true;
                } else {
                    proximity = 0;
                    Log.d("SENSORS", "Proximity sensor UNAVAILABLE");
                }
            }
            microphone = 0;
            volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

            // If I have a new sample in all sensors do a cache => synchronize to the slowest sensor
            if (acc_cache && rot_cache /*&& prox_cache*/ && light_cache && isPlayed) {
                Log.d("SENSORS", "Updating database");
                acc_cache = false;
                rot_cache = false;
                prox_cache = false;
                light_cache = false;
                ContentValues data = new ContentValues();
                data.put(Provider.Smart_Volume_Data.TIMESTAMP, System.currentTimeMillis());
                data.put(Provider.Smart_Volume_Data.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
                data.put(Provider.Smart_Volume_Data.A_VALUES_0,acc_x);
                data.put(Provider.Smart_Volume_Data.A_VALUES_1,acc_y);
                data.put(Provider.Smart_Volume_Data.A_VALUES_2,acc_z);
                data.put(Provider.Smart_Volume_Data.R_VALUES_0,rotation_x);
                data.put(Provider.Smart_Volume_Data.R_VALUES_1,rotation_y);
                data.put(Provider.Smart_Volume_Data.R_VALUES_2,rotation_z);
                data.put(Provider.Smart_Volume_Data.LUX,light);
                data.put(Provider.Smart_Volume_Data.PROXIMITY,proximity);
                data.put(Provider.Smart_Volume_Data.MICROPHONE,microphone);
                data.put(Provider.Smart_Volume_Data.VOLUME, volume);
                data.put(Provider.Smart_Volume_Data.LABEL, label);
                getContentResolver().insert(Provider.Smart_Volume_Data.CONTENT_URI, data);




                //Process raw data
                //Log.d("DEMO", "ACC: " + acc_data.get("timestamp").toString()+ "Light: " + light_data.get("timestamp").toString());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Text_R_values_0.setText("" + rotation_x);
                    Text_R_values_1.setText("" + rotation_y);
                    Text_R_values_2.setText("" + rotation_z);
                    Text_Lux.setText("" + light);
                    Text_Proximity.setText("" + proximity);
                    //Text_Microphone.setText("" + microphone);
                    Text_Volume.setText("" + volume);
                    //Text_Output.setText("0");

                }
            });
        }


    }


//    private static AccelerometerBR accelBR = new AccelerometerBR();
//    public static class AccelerometerBR extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent){
//            //Get the raw data
//            ContentValues raw_data = (ContentValues)intent.getParcelableExtra(Accelerometer.EXTRA_DATA);
//
//            //Process raw data
//            Log.d("DEMO", raw_data.toString() );
//        }
//    }

    //Observe changes in the database
    //ATTENTION THE CONTENT OBSERVER BUFFER 250 samples before launching onChange.
//    public class AccelerometerObserver extends ContentObserver {
//        /**
//         * Creates a content observer.
//         *
//         * @param handler The handler to run {@link #onChange} on, or null if none.
//         */
//        public AccelerometerObserver(Handler handler) {
//            super(handler);
//        }
//
//        @Override
//        public void onChange(boolean selfChange){
//            super.onChange(selfChange);
//            Log.d("AXIS X", "Inside onChange");
//            //This is not working right now
//            Cursor raw_data = getContentResolver().query(Accelerometer_Provider.Accelerometer_Data.CONTENT_URI,
//                    null,
//                    null,
//                    null,
//                    null);
//            if (raw_data != null && raw_data.moveToFirst()){
//                do {
//                    double x = raw_data.getDouble(raw_data.getColumnIndex(Accelerometer_Provider.Accelerometer_Data.VALUES_0));
//                    Log.d("AXIS X", "X="+x);
//                }
//                while (raw_data.moveToNext());
//            }
//            if (raw_data!=null && ! raw_data.isClosed()){
//                raw_data.close();
//            }
//        }
//
//
//    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }


//        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ACCELEROMETER, false);
//        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
//        if(contextBR != null){
//            try {
//                unregisterReceiver(contextBR);
//
//            } catch (Exception e) {
//
//                e.printStackTrace();
//            }
//        }

        //getContentResolver().unregisterContentObserver(accelObs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
