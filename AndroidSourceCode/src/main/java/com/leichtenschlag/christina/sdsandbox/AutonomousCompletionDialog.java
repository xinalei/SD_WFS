package com.leichtenschlag.christina.sdsandbox;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Christy on 4/5/2015.
 */
public class AutonomousCompletionDialog extends DialogFragment {

    private TextView finalRSSI;
    private Button done;

    public static AutonomousCompletionDialog newInstance(String s) {
        AutonomousCompletionDialog myFragment = new AutonomousCompletionDialog();

        Bundle args = new Bundle();
        args.putString("rssi", s);
        myFragment.setArguments(args);

        return myFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_autonomous_complete, container, false);

        getDialog().setTitle("Notification of Autonomous Algorithm Completion");

        String rssi = getArguments().getString("rssi");
        if(null == rssi) {
            Log.v("error making dialog", "for completing autonomous alg");
        }
        else {
            // Set text to display final rssi received by the robot,
            finalRSSI = (TextView) v.findViewById(R.id.textView_alg_finalRSSI);
            finalRSSI.setText(rssi + " dBm");
        }


        done = (Button) v.findViewById(R.id.button_dialogAutoComplete);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.autoComplete = null;
                dismiss();
            }
        });

        return  v;
    }
}


