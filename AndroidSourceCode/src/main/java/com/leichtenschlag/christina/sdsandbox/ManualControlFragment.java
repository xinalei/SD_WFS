package com.leichtenschlag.christina.sdsandbox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.leichtenschlag.christina.sdsandbox.R;

import java.util.Timer;
import java.util.TimerTask;

public class ManualControlFragment extends Fragment {

    TextView signalStrength;
    Button forward, left, right, reverse, start;
    WebView feed = null;
    Timer feedTimer = null;


    public ManualControlFragment() {
        // Required empty public constructor
    }

    public static ManualControlFragment newInstance() {
        return new ManualControlFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manual_control, container, false);


        signalStrength = (TextView) rootView.findViewById(R.id.textView_signalstrength);
        forward = (Button) rootView.findViewById(R.id.button_n);
        left = (Button) rootView.findViewById(R.id.button_w);
        right = (Button) rootView.findViewById(R.id.button_e);
        reverse = (Button) rootView.findViewById(R.id.button_s);
        start = (Button) rootView.findViewById(R.id.button_startAuto);

        if(null != MainActivity.currentRSSI) {
            signalStrength.setText(MainActivity.currentRSSI);
        }

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send command to move forward two tire rotations
                MainActivity.mConnectingDevices.write("I".getBytes()); // sends data to MSP
            }
        });

        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send command to rotate wheels left (?)
                MainActivity.mConnectingDevices.write("J".getBytes()); // sends data to MSP
            }
        });

        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mConnectingDevices.write("K".getBytes()); // sends data to MSP
                // Send command to move in reverse two tire rotations
            }
        });

        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send command to rotate wheels right(?)
                MainActivity.mConnectingDevices.write("L".getBytes()); // sends data to MSP
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Start autonomous functionality
                MainActivity.appMode = MainActivity.Mode.AUTONOMOUS;
                MainActivity.title.setText(Constants.TITLE_AUTO); // set title of screen.
                MainActivity.mConnectingDevices.write("A".getBytes()); // sends data to MSP
            }
        });


        // Start the video feed.
        feed = (WebView) rootView.findViewById(R.id.webview_videofeed);
        startTimer();

        return rootView;
    }


    public void startTimer() {
        feedTimer = new Timer();
        feedTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Update the video feed.
                            feed.loadUrl(MainActivity.ip);
                        }
                    });
                }
                catch(Exception e) {
                    Log.v("timer task throw", "not today! " + e.toString());
                    this.cancel();
                }
            }
        }, 0, 600);//refresh rate time interval (ms)
        // Can't go much faster, else nothing actually loads.
    }

    public void updateSignalStrength(String rssi) {
        this.signalStrength.setText( rssi + " dBm");
    }

}