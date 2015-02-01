package com.leichtenschlag.christina.sdsandbox;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.LinkedList;

/**
 * Created by Christy on 1/31/2015.
 */
public class SelectNetworkDialog extends DialogFragment {

    ArrayAdapter<String> wifiArrayAdapter;
    LinkedList<String> networks;

    // Use this instance of the interface to deliver action events
    static NetworkSelectListener mListener;

    // Interface; these methods are implemented by the MainActivity, allowing communication
    // between this dialog and the MainActivity.
    public interface NetworkSelectListener {
    }

    static SelectNetworkDialog newInstance() {
        return new SelectNetworkDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_select_network, container, false);

        getDialog().setTitle("Select A Wireless Network");

        //wifiArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, networks);

        return v;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NetworkSelectListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NetworkSelectListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }
}


