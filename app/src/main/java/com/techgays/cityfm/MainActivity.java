package com.techgays.cityfm;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static boolean displayAd;

    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Button nextButton;
    private Button previousButton;
    private ImageView stationImageView,btnExit,btnMenu,btnMute;

    private TextView titleTextView;
    private TextView albumTextView;
    private TextView artistTextView;
    private TextView trackTextView;
    private TextView statusTextView;
    private TextView timeTextView;

    private Intent bindIntent;
    private TelephonyManager telephonyManager;
    private boolean wasPlayingBeforePhoneCall = false;
    private RadioUpdateReceiver radioUpdateReceiver;
    public static RadioService radioService;
    private AdView adView;

    private static final String TYPE_AAC = "aac";
    private static final String TYPE_MP3 = "mp3";
    private Handler handler;
    private AudioManager leftAm;
    private SeekBar volControl;
    private ContentObserver mVolumeObserver;
    private static boolean isExitMenuClicked;
    public static int stationID = 0;
    public static boolean isStationChanged = false;
    //Contact button email address
    private String emailAddress = "slbc.cityfm@gmail.com";
    //Contact button phone number
    private String phoneNumber = "0112692983";
    //Contact button website URL
    private String websiteURL = "http://slbc.lk/index.php/contact-us-city-fm";
    //Contact button SMS number
    private int smsNumber = 112692983;
    boolean isMute;
    private boolean blockSeekBar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bind to the service
        try {
            bindIntent = new Intent(this, RadioService.class);
            bindService(bindIntent, radioConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {

        }

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE);
        }

        handler = new Handler();
        initialize();
        clickListeners(); //Start click listeners
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent i;

        final String thisClassName = this.getClass().getName();
        final String thisPackageName = this.getPackageName();

        if (item.getItemId() == R.id.radio) {
            if (!thisClassName.equals(thisPackageName + ".MainActivity")) {
                i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            }

        } else if (item.getItemId() == R.id.stations) {
            if (!thisClassName.equals(thisPackageName + ".StationList")) {
                i = new Intent(this, StationList.class);
                startActivity(i);
                return true;
            }
        } else if (item.getItemId() == R.id.exit) {
            String title = "Exit Radio";
            String message = "Are you sure to exit the app?";
            String buttonYesString = "Yes";
            String buttonNoString = "No";

            isExitMenuClicked = true;

            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setTitle(title);
            ad.setMessage(message);
            ad.setCancelable(true);
            ad.setPositiveButton(buttonYesString,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (radioService != null) {
                                radioService.exitNotification();
                                radioService.stop();
                                radioService.stopService(bindIntent);
                                isExitMenuClicked = true;
                                finish();
                            }
                        }
                    });

            ad.setNegativeButton(buttonNoString, null);

            ad.show();

            return true;

        } else if (item.getItemId() == R.id.about) {
            if (!thisClassName.equals(thisPackageName + ".AboutActivity")) {
                i = new Intent(this, AboutActivity.class);
                startActivity(i);
                return true;
            }

        } else if (item.getItemId() == R.id.facebook) {
            if (!thisClassName.equals(thisPackageName + ".FacebookActivity")) {
                i = new Intent(this, FacebookActivity.class);
                startActivity(i);
                return true;
            }
        } else if (item.getItemId() == R.id.twitter) {
            if (!thisClassName.equals(thisPackageName + ".TwitterActivity")) {
                i = new Intent(this, TwitterActivity.class);
                startActivity(i);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
                || newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            try {
                setContentView(R.layout.activity_main);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        initialize();

                        if (radioService.getTotalStationNumber() <= 1) {
                            nextButton.setEnabled(false);
                            nextButton.setVisibility(View.INVISIBLE);
                            previousButton.setEnabled(false);
                            previousButton.setVisibility(View.INVISIBLE);
                        }

                        updateStatus();
                        updateMetadata();
                        updateAlbum();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initialize() {
        try {

            displayAd = Boolean.parseBoolean(getResources().getString(
                    R.string.is_display_ad));

            String STATUS_BUFFERING = getResources().getString(
                    R.string.status_buffering);

            //titleTextView = (TextView) this.findViewById(R.id.titleTextView);
            //albumTextView = (TextView) this.findViewById(R.id.albumTextView);
            //artistTextView = (TextView) this.findViewById(R.id.artistTextView);
            //trackTextView = (TextView) this.findViewById(R.id.trackTextView);
            statusTextView = (TextView) this.findViewById(R.id.statusTextView);
            timeTextView = (TextView) this.findViewById(R.id.timeTextView);

            playButton = (Button) this.findViewById(R.id.PlayButton);
            pauseButton = (Button) this.findViewById(R.id.PauseButton);
            stopButton = (Button) this.findViewById(R.id.StopButton);
            nextButton = (Button) this.findViewById(R.id.NextButton);
            previousButton = (Button) this.findViewById(R.id.PreviousButton);
            btnExit = (ImageView) findViewById(R.id.btnExit);
            btnMenu  = (ImageView) findViewById(R.id.btnMenu);
            btnMute = (ImageView) findViewById(R.id.mute);
            pauseButton.setEnabled(false);
            pauseButton.setVisibility(View.GONE);
            stationImageView = (ImageView) findViewById(R.id.stationImageView);

            playButton.setEnabled(true);
            stopButton.setEnabled(false);

            // volume
            leftAm = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            int maxVolume = leftAm.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curVolume = leftAm.getStreamVolume(AudioManager.STREAM_MUSIC);
            volControl = (SeekBar) findViewById(R.id.volumebar);
            volControl.setMax(maxVolume);
            volControl.setProgress(curVolume);
            volControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                        @Override
                        public void onStopTrackingTouch(SeekBar arg0) {
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar arg0) {
                        }

                        @Override
                        public void onProgressChanged(SeekBar a0, int a1, boolean a2) {
                            leftAm.setStreamVolume(AudioManager.STREAM_MUSIC, a1, 0);
                        }
                    });

            Handler mHandler = new Handler();
            // in onCreate put
            mVolumeObserver = new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    if (volControl != null && leftAm != null) {
                        int volume = leftAm
                                .getStreamVolume(AudioManager.STREAM_MUSIC);
                        volControl.setProgress(volume);
                    }
                }

            };
            //this.getContentResolver().registerContentObserver(System.getUriFor(System.SOUND_EFFECTS_ENABLED[AudioManager.STREAM_MUSIC]), false, mVolumeObserver);

            // vloume
            startService(new Intent(this, RadioService.class));

            volControl.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return blockSeekBar;
                }
            });

            //displayAd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        volControl = (SeekBar) findViewById(R.id.volumebar);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            int index = volControl.getProgress();
            volControl.setProgress(index + 1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            int index = volControl.getProgress();
            volControl.setProgress(index - 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /*public void displayAd() {
        if (displayAd == true) {
            // Create the adView
            try {

                if (adView != null) {
                    adView.destroy();
                }

                adView = new AdView(this, AdSize.SMART_BANNER,
                        this.getString(R.string.admob_publisher_id));
                LinearLayout layout = (LinearLayout) findViewById(R.id.adLayout);
                layout.addView(adView);
                adView.loadAd(new AdRequest());
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            LinearLayout layout = (LinearLayout) findViewById(R.id.adLayout);
            layout.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            layout.setVisibility(View.INVISIBLE);
        }
    }*/

    /**
     * Listens for contact button clicks
     */
    private void clickListeners() {
        //Play button
        /*playButton = (Button)findViewById(R.id.PlayButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        RadioMediaPlayerService.class);
                intent.putExtra(RadioMediaPlayerService.START_PLAY, true);
                startService(intent);

            }
        });

        //Stop button
        stopButton = (Button)findViewById(R.id.StopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Get new MediaPlayerService activity
                Intent intent = new Intent(getApplicationContext(),
                        RadioMediaPlayerService.class);
                stopService(intent);
            }
        });*/

        //Email Button click list
        final View EmailPress = (Button) this.findViewById(R.id.emailBtn);
        EmailPress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
                //i.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
                //i.putExtra(Intent.EXTRA_TEXT   , "body of email");
                try {
                    startActivity(Intent.createChooser(emailIntent, "Send email..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        //Phone Button
        final View PhonePress = (Button) this.findViewById(R.id.phoneBtn);
        PhonePress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                String phoneNum = phoneNumber;
                Intent phoneIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNum));
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivity(phoneIntent);

            }
        });

        //Website Button
        final View WWWPress = (Button) this.findViewById(R.id.websiteBtn);
        WWWPress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(websiteURL)); //URL
                startActivity(browserIntent);

            }
        });

        //SMS Button
        final View TxtPress = (Button) this.findViewById(R.id.txtBtn);
        TxtPress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + smsNumber));
                smsIntent.putExtra("sms_body", "AIR ");
                startActivity(smsIntent);

            }
        });


        btnExit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if (radioService != null) {
                    radioService.exitNotification();
                    radioService.stop();
                    radioService.stopService(bindIntent);
                    isExitMenuClicked = true;
                    finish();
                }

            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                openOptionsMenu();

            }
        });

        btnMute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {


                if (!isMute){

                    leftAm.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    btnMute.setImageResource(R.drawable.volume_muted);
                    blockSeekBar = true;
                    isMute=true;

                }else {


                    leftAm.setStreamVolume(AudioManager.STREAM_MUSIC, volControl.getProgress(), 0);
                    btnMute.setImageResource(R.drawable.volume_on);
                    blockSeekBar = false;
                    isMute=false;
                }



            }
        });
    }

    public void updatePlayTimer() {
        timeTextView.setText(radioService.getPlayingTime());

        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        timeTextView.setText(radioService.getPlayingTime());
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 1000);
    }

    public void onClickPlayButton(View view) {
        radioService.play();
    }

    public void onClickPauseButton(View view) {
        radioService.pause();
    }

    public void onClickStopButton(View view) {
        radioService.stop();
        resetMetadata();
        updateDefaultCoverImage();
    }

    public void onClickNextButton(View view) {
        resetMetadata();
        playNextStation();
        updateDefaultCoverImage();
    }

    public void onClickPreviousButton(View view) {
        resetMetadata();
        playPreviousStation();
        updateDefaultCoverImage();
    }

    public void playNextStation() {
        radioService.stop();
        radioService.setNextStation();
		/*
		 * if(radioService.isPlaying()) {
		 * radioService.setStatus(STATUS_BUFFERING); updateStatus();
		 * radioService.stop(); radioService.play(); } else {
		 * radioService.stop(); }
		 */
    }

    public void playPreviousStation() {
        radioService.stop();
        radioService.setPreviousStation();
		/*
		 * if(radioService.isPlaying()) {
		 * radioService.setStatus(STATUS_BUFFERING); updateStatus();
		 * radioService.stop(); radioService.play(); } else {
		 * radioService.stop(); }
		 */
    }

    public void updateDefaultCoverImage() {
        try {
            String mDrawableName = "station_"
                    + (radioService.getCurrentStationID() + 1);
            int resID = getResources().getIdentifier(mDrawableName, "drawable",
                    getPackageName());

            int resID_default = getResources().getIdentifier("station_default",
                    "drawable", getPackageName());

            if (resID > 0)
                stationImageView.setImageResource(resID);
            else
                stationImageView.setImageResource(resID_default);

            albumTextView.setText("");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAlbum() {
        String album = radioService.getAlbum();
        String artist = radioService.getArtist();
        String track = radioService.getTrack();
        Bitmap albumCover = radioService.getAlbumCover();

        //albumTextView.setText(album);

        if (albumCover == null || (artist.equals("") && track.equals("")))
            updateDefaultCoverImage();

    }

    public void updateMetadata() {
        String artist = radioService.getArtist();
        String track = radioService.getTrack();
        // if(artist.length()>30)
        // artist = artist.substring(0, 30)+"...";
        //artistTextView.setText(artist);
        //trackTextView.setText(track);
        //albumTextView.setText("");
    }

    public void resetMetadata() {
        radioService.resetMetadata();
        //artistTextView.setText("");
        //albumTextView.setText("");
        //trackTextView.setText("");
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (radioService != null) {
            if (!radioService.isPlaying() && !radioService.isPreparingStarted()) {
                // radioService.stopSelf();
                radioService.stopService(bindIntent);
            }
        }

        if (adView != null) {
            adView.destroy();
        }

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }

        unbindDrawables(findViewById(R.id.RootView));
        Runtime.getRuntime().gc();
    }

    private void unbindDrawables(View view) {
        try {
            if (view.getBackground() != null) {
                view.getBackground().setCallback(null);
            }
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    unbindDrawables(((ViewGroup) view).getChildAt(i));
                }
                ((ViewGroup) view).removeAllViews();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (radioUpdateReceiver != null)
            unregisterReceiver(radioUpdateReceiver);
        // finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

		/* Register for receiving broadcast messages */
        if (radioUpdateReceiver == null)
            radioUpdateReceiver = new RadioUpdateReceiver();
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_CREATED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_DESTROYED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_STARTED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_CONNECTING));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_START_PREPARING));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_PREPARED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_PLAYING));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_PAUSED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_STOPPED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_COMPLETED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_ERROR));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_BUFFERING_START));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_BUFFERING_END));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_METADATA_UPDATED));
        registerReceiver(radioUpdateReceiver, new IntentFilter(
                RadioService.MODE_ALBUM_UPDATED));

        if (wasPlayingBeforePhoneCall) {
            radioService.play();
            wasPlayingBeforePhoneCall = false;
        }

        if (radioService != null) {
            if (isStationChanged) {
                if (stationID != radioService.getCurrentStationID()) {
                    radioService.stop();
                    radioService.setCurrentStationID(stationID);
                    resetMetadata();
                    updateDefaultCoverImage();
                }
                if (!radioService.isPlaying())
                    radioService.play();
                isStationChanged = false;
            }
        }
    }

    /* Receive Broadcast Messages from RadioService */
    private class RadioUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(RadioService.MODE_CREATED)) {

            } else if (intent.getAction().equals(RadioService.MODE_DESTROYED)) {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                updateDefaultCoverImage();
                updateMetadata();
                updateStatus();
            } else if (intent.getAction().equals(RadioService.MODE_STARTED)) {
                pauseButton.setEnabled(false);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);

                playButton.setEnabled(true);
                stopButton.setEnabled(false);
                updateStatus();
            } else if (intent.getAction().equals(RadioService.MODE_CONNECTING)) {
                pauseButton.setEnabled(false);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                playButton.setEnabled(false);
                stopButton.setEnabled(true);
                updateStatus();
            } else if (intent.getAction().equals(
                    RadioService.MODE_START_PREPARING)) {
                pauseButton.setEnabled(false);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                playButton.setEnabled(false);
                stopButton.setEnabled(true);
                updateStatus();
            } else if (intent.getAction().equals(RadioService.MODE_PREPARED)) {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                updateStatus();
            } else if (intent.getAction().equals(
                    RadioService.MODE_BUFFERING_START)) {
                updateStatus();
            } else if (intent.getAction().equals(
                    RadioService.MODE_BUFFERING_END)) {
                updateStatus();
            } else if (intent.getAction().equals(RadioService.MODE_PLAYING)) {
                if (radioService.getCurrentStationType().equals(TYPE_AAC)) {
                    playButton.setEnabled(false);
                    stopButton.setEnabled(true);
                } else {
                    playButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    stopButton.setEnabled(true);
                    playButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.VISIBLE);
                }
                updateStatus();
            } else if (intent.getAction().equals(RadioService.MODE_PAUSED)) {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(true);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.INVISIBLE);
                updateStatus();
            } else if (intent.getAction().equals(RadioService.MODE_STOPPED)) {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.INVISIBLE);
                updateStatus();
            } else if (intent.getAction().equals(RadioService.MODE_COMPLETED)) {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.INVISIBLE);
                // radioService.setCurrentStationURL2nextSlot();
                // radioService.play();
                updateStatus();
            } else if (intent.getAction().equals(RadioService.MODE_ERROR)) {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.INVISIBLE);
                updateStatus();
            } else if (intent.getAction().equals(
                    RadioService.MODE_METADATA_UPDATED)) {
                updateMetadata();
                updateStatus();
                updateDefaultCoverImage();
            } else if (intent.getAction().equals(
                    RadioService.MODE_ALBUM_UPDATED)) {
                updateAlbum();
            }
        }
    }

    PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                wasPlayingBeforePhoneCall = radioService.isPlaying();
                radioService.stop();
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                if (wasPlayingBeforePhoneCall) {
                    radioService.play();
                }
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // A call is dialing,
                // active or on hold
                wasPlayingBeforePhoneCall = radioService.isPlaying();
                radioService.stop();
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    public void updateStatus() {
        String status = radioService.getStatus();
        if (radioService.getTotalStationNumber() > 1) {

			 /* if (status != "stop") {

                  statusTextView.setText("Stop");
              }*/

            String title = radioService.getCurrentStationName();
            try {
                titleTextView.setText(title);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        try {
            statusTextView.setText(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handles the connection between the service and activity
    private final ServiceConnection radioConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            radioService = ((RadioService.RadioBinder) service).getService();

            if (radioService.getTotalStationNumber() <= 1) {
                nextButton.setEnabled(false);
                nextButton.setVisibility(View.INVISIBLE);
                previousButton.setEnabled(false);
                previousButton.setVisibility(View.INVISIBLE);
            }

            updateStatus();
            updateMetadata();
            updateAlbum();
            updatePlayTimer();
            radioService.showNotification();

			/*
			 * Uncomment following line if you want auto-play while starting the
			 * app
			 */
            // radioService.play();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            radioService = null;
        }
    };


}
