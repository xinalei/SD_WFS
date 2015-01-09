package com.leichtenschlag.christina.sdsandbox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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


    private Button send_command;
    private Button clear_log;
    private EditText edit_command;
    private TextView command_log;
    private StringBuilder log;
    private ScrollView log_container;

    public CommandFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_command, container, false);

        send_command = (Button) rootView.findViewById(R.id.button_sendcommand);
        clear_log = (Button) rootView.findViewById(R.id.button_clearlog);
        edit_command = (EditText) rootView.findViewById(R.id.editText_entercommand);
        command_log = (TextView) rootView.findViewById(R.id.textView_commandlog);
        log_container = (ScrollView) rootView.findViewById(R.id.scrollView_holdlog);

        log = new StringBuilder();

        // Send a command by using ConnectingDevices.write function
        send_command.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = edit_command.getText().toString();
                MainActivity.mConnectingDevices.write(command.getBytes()); // sends data to device
                updateLog(command, false);
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
}