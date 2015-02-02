package com.leichtenschlag.christina.sdsandbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class PasswordDialog extends DialogFragment {

    private EditText password;
    private Button noPw, pw;

    // Use this instance of the interface to deliver action events
    static PasswordListener mListener;

    // Interface; these methods are implemented by the MainActivity, allowing communication
    // between this dialog and the MainActivity.
    public interface PasswordListener {
        void updatePassword(String p);
    }

    public static PasswordDialog newInstance(String s) {
        PasswordDialog myFragment = new PasswordDialog();

        Bundle args = new Bundle();
        args.putString("ssid", s);
        myFragment.setArguments(args);

        return myFragment;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (PasswordListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PasswordListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_wifi_password, container, false);

        String networkSSID = getArguments().getString("ssid");
        if(null == networkSSID) {
            Log.v("error making dialog", "for password");
        }
        else {

            getDialog().setTitle("Enter the password for " + networkSSID);

            password = (EditText) v.findViewById(R.id.editText_wifipassword);

            pw = (Button) v.findViewById(R.id.button_yespassword);
            noPw = (Button) v.findViewById(R.id.button_nopassword);

            pw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String pw = password.getText().toString();
                    if (null == pw || 0 == pw.length()) {
                        Toast.makeText(getActivity().getApplicationContext(), "Please enter a password", Toast.LENGTH_SHORT).show();
                    }
                    else { // PASSWORD ACCEPTED; kill the dialog.
                        mListener.updatePassword(pw);
                        dismiss();
                    }
                }
            });

            noPw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }


        return  v;
    }
}


