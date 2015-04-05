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

    private Button enter_CM, reboot, scan, rssi, show, setup, finish;
    private TextView command_log;
    private StringBuilder log, scan_data, rssi_data;
    private ScrollView log_container;
    boolean obtain_sd = false;

    public CommandFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_command, container, false);

        command_log = (TextView) rootView.findViewById(R.id.textView_commandlog);
        log_container = (ScrollView) rootView.findViewById(R.id.scrollView_holdlog);
        enter_CM = (Button) rootView.findViewById(R.id.button_wifly$$$);
        reboot = (Button) rootView.findViewById(R.id.button_wiflyreboot);
        scan = (Button) rootView.findViewById(R.id.button_wiflyscan);
        setup = (Button) rootView.findViewById(R.id.button_wiflysetup);
        finish = (Button) rootView.findViewById(R.id.button_exitwifisetup);

        log = new StringBuilder();
        scan_data = new StringBuilder();
        rssi_data = new StringBuilder();

        enter_CM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mConnectingDevices.write("W".getBytes()); // sends data to MSP
                updateLog("$$$", false);
            }
        });

        reboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mConnectingDevices.write("R".getBytes()); // sends data to MSP
                updateLog("reboot", false);
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obtain_sd = true; // If command == scan, then start adding received data to a buffer.
                MainActivity.mConnectingDevices.write("S".getBytes()); // sends data to device
                updateLog("scan", false);

                // Start spinner.
                MainActivity.loadFrag = LoadingDialogFragment.newInstance();
                MainActivity.loadFrag.show(getActivity().getSupportFragmentManager(), "loadingFragment");
            }
        });

        setup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mConnectingDevices.write("C".getBytes()); // sends data to device
                updateLog("setup", false);
            }
        });


        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity.manFrag = ManualControlFragment.newInstance();
                MainActivity.appMode = MainActivity.Mode.MANUAL;
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.mainactivity_container, MainActivity.manFrag)
                        .commit();
                MainActivity.title.setText(Constants.TITLE_MANUAL);
                MainActivity.mConnectingDevices.write("G".getBytes()); // want current RSSI value on the screen.
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

    public void updateRSSILog(String additionalText) {

        if(null != additionalText) {
            rssi_data.append(additionalText);
        }
    }

    public String stopCollectingScanData() {
        obtain_sd = false;
        String sd = scan_data.toString();
        updateLog("<< Received network data >>", true);
        scan_data.setLength(0); // Reset

        if(MainActivity.loadFrag != null) {
            MainActivity.loadFrag.dismiss();
            MainActivity.loadFrag = null;
        }

        return sd;
    }

    public boolean determineIfDoneScanning() {

        StringBuilder n = new StringBuilder();
        for(char ch : scan_data.toString().toCharArray()) {
            if('\n' == ch) break;
            n.append(ch);
        }

        try{
            Integer expectedNumNetworks = Integer.valueOf( n.toString().trim() );

            int count = 0;
            for(char ch : scan_data.toString().toCharArray()) {
                if('\n' == ch) count++;
            }


            if(null != expectedNumNetworks && count == 1+expectedNumNetworks) {
                // We're done.
                return true;
            }
            else { // Not done yet!
                return false;
            }

        } catch (Exception e) {
            updateLog("ERROR: Something went wrong with the data. Try scanning again! \""+n.toString()+"\"", true);
            stopCollectingScanData(); // Stop collecting!
            return false;
        }
    }
}