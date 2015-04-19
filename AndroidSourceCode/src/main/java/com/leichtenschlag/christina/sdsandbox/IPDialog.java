package com.leichtenschlag.christina.sdsandbox;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class IPDialog extends DialogFragment {

    private EditText addr;
    private Button cancel, enter;

    // Use this instance of the interface to deliver action events
    static IPListener mListener;

    // Interface; these methods are implemented by the MainActivity, allowing communication
    // between this dialog and the MainActivity.
    public interface IPListener {
        void updateIPaddr(String p);
    }

    public IPDialog() {
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (IPListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement IPListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_ip_update, container, false);

       getDialog().setTitle("Enter the new address for the IP camera");

        addr = (EditText) v.findViewById(R.id.editText_ipaddr_update);

        enter = (Button) v.findViewById(R.id.button_enter);
        cancel = (Button) v.findViewById(R.id.button_cancel);

        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pw = addr.getText().toString();
                if (null != pw && 0 == pw.length()) {
                    mListener.updateIPaddr(pw);
                }

                // If no address, just dismiss w/o updating.
                dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });


        return  v;
    }
}


