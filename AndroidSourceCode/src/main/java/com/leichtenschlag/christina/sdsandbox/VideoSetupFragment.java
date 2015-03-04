package com.leichtenschlag.christina.sdsandbox;

import android.app.Activity;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import java.util.Timer;
import java.util.TimerTask;

public class VideoSetupFragment extends Fragment {

    static VideoFragmentListener mListener;

    public interface VideoFragmentListener {
        public void camSetupComplete(String ipaddr);
    }

    private EditText editIPaddr;
    private Button refrehCam, finish;
    private Timer videoTimer;
    String ipaddr;

    public static VideoSetupFragment newInstance() {
        VideoSetupFragment fragment = new VideoSetupFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public VideoSetupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video_setup, container, false);

        editIPaddr = (EditText) rootView.findViewById(R.id.editText_ipaddr);
        editIPaddr.setText(MainActivity.ip);
        refrehCam = (Button) rootView.findViewById(R.id.button_refreshcam);
        finish = (Button) rootView.findViewById(R.id.button_finishvideosetup);

        // Video feed is held by the main activity.
        MainActivity.videoFeed = (WebView) rootView.findViewById(R.id.webview_videofeed);


        refrehCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipaddr = editIPaddr.getText().toString();
                videoTimerSetup();
            }
        });

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(videoTimer != null) {
                    videoTimer.cancel();
                }
                mListener.camSetupComplete(ipaddr);
            }
        });

        return rootView;
    }


    public void videoTimerSetup() {

        videoTimer = new Timer();
        videoTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Update the video feed.
                        MainActivity.videoFeed.loadUrl(MainActivity.ip);
                    }
                });
            }
        }, 0, 500);//refresh rate time interval (ms)
        // Can't go much faster, else nothing actually loads.
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (VideoFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement VideoFragmentListener");
        }
    }

}
