package com.leichtenschlag.christina.sdsandbox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by Christy on 1/7/2015.
 */
public class CommandFragment extends Fragment {


    private Button send_command, clear_log;
    private Button enter_CM, exit_CM, reboot, scan;
    private TextView command_log;
    private StringBuilder log, scan_data;
    private ScrollView log_container;
    boolean obtain_sd = false;

    public CommandFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_command, container, false);

        clear_log = (Button) rootView.findViewById(R.id.button_clearlog);
        command_log = (TextView) rootView.findViewById(R.id.textView_commandlog);
        log_container = (ScrollView) rootView.findViewById(R.id.scrollView_holdlog);
        enter_CM = (Button) rootView.findViewById(R.id.button_wifly$$$);
        exit_CM = (Button) rootView.findViewById(R.id.button_wiflyexit);
        reboot = (Button) rootView.findViewById(R.id.button_wiflyreboot);
        scan = (Button) rootView.findViewById(R.id.button_wiflyscan);

        log = new StringBuilder();
        scan_data = new StringBuilder();

        enter_CM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mConnectingDevices.write("W".getBytes()); // sends data to device
                updateLog("$$$", false);
            }
        });

        exit_CM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mConnectingDevices.write("E".getBytes()); // sends data to device
                updateLog("exit", false);
            }
        });

        reboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mConnectingDevices.write("R".getBytes()); // sends data to device
                updateLog("reboot", false);
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mConnectingDevices.write("S".getBytes()); // sends data to device
                updateLog("scan", false);
                obtain_sd = true; // If command == scan, then start adding received data to a buffer.
            }
        });

        // Delete the current log.
        clear_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                command_log.setText("");
                log.setLength(0);
            }
        });

        return rootView;
    }

    /**
     *
     * @param additionalText - the text to add to the log
     * @param module - true if the text is being sent from to module; false if being sent from Android
     */
    public void updateLog(String additionalText, boolean module) {

        if(null != additionalText) {

            if(module) log.append("Received: ");
            else log.append("Sent:     ");
            log.append(additionalText).append("\n");
            command_log.setText(log.toString());
            log_container.fullScroll(View.FOCUS_DOWN); // scroll to bottom
        }
    }

    /**
     * This function is used to collect the scan data.
     * @param additionalText - the text to add to the log of parsed scan data
     */
    public void updateScanLog(String additionalText) {

        if(null != additionalText) {
            scan_data.append(additionalText);
        }
    }

    public String stopCollectingScanData() {
        obtain_sd = false;
        String sd = scan_data.toString();
        Log.v("received 0x7F", sd);
        Log.v("receivedF", "stop holding data");
        updateLog(sd, true);
        scan_data.setLength(0); // Reset
        return sd;
    }
}