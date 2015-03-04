package com.leichtenschlag.christina.sdsandbox;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.leichtenschlag.christina.sdsandbox.R;

public class ManualControlFragment extends Fragment {

    TextView signalStrength;
    Button forward, left, right, reverse, start;
    WebView feed;


    public ManualControlFragment() {
        // Required empty public constructor
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

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Send command to move forward two tire rotations
            }
        });

        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Send command to rotate wheels left (?)
            }
        });

        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Send command to rotate wheels right(?)
            }
        });

        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Send command to move in reverse two tire rotations
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Start autonomous functionality
            }
        });


        return rootView;
    }


    public void updateSignalStrength(String rssi) {

        this.signalStrength.setText( rssi );
    }

}