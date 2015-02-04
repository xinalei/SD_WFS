package com.leichtenschlag.christina.sdsandbox;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Christy on 1/31/2015.
 */
public class SelectNetworkDialog extends DialogFragment {

    ArrayAdapter<String> wifiArrayAdapter;
    ArrayList<String> networks;
    ListView allNetworks;

    // Use this instance of the interface to deliver action events
    static NetworkSelectListener mListener;

    // Interface; these methods are implemented by the MainActivity, allowing communication
    // between this dialog and the MainActivity.
    public interface NetworkSelectListener {
        void wifiNetworkSelected(String ssid);
    }

    public static SelectNetworkDialog newInstance(String s) {
        SelectNetworkDialog myFragment = new SelectNetworkDialog();
        ArrayList<String> networks = parseScanData(s);

        if(null == networks || 0 == networks.size()) {
            Log.v("arraylist of networks was null", "exiting");
            return null;
        }

        Bundle args = new Bundle();
        args.putStringArrayList("networkList", networks);
        myFragment.setArguments(args);
        return myFragment;
    }

    private static ArrayList<String> parseScanData(String s) {

        if(null == s) return null;

        int index = 0;
        while('\n' != s.charAt(index)) { // Sometimes "END" is prepended to the data.
            index++;
            if(index >= s.length()) return null;
        }
        index++; // move past initial newline.

        ArrayList<String> wifiNetworks = new ArrayList<>();
        StringBuilder curr = new StringBuilder();
        while(index < s.length()) {

            if(0x0D == s.charAt(index)) { // newline
                wifiNetworks.add( curr.toString() );
                curr.setLength(0); // reset curr
            }
            else if(0x7F == s.charAt(index)) {
                break;
            }
            else { // append to curr.
                curr.append( s.charAt(index) );
            }

            index++;
        }

        if(wifiNetworks.isEmpty()) return null;
        return  wifiNetworks;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_select_network, container, false);

        getDialog().setTitle("Select A Wireless Network");

        networks = getArguments().getStringArrayList("networkList");

        wifiArrayAdapter = new ArrayAdapter<String>(v.getContext(), android.R.layout.simple_list_item_1, networks);

        allNetworks = (ListView) v.findViewById(R.id.listView_wireless_networks);
        allNetworks.setAdapter(wifiArrayAdapter);
        allNetworks.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String ssid = networks.get(position);
                Log.v("listener for networks adapter, user selected:", ssid);
                dismiss();

                mListener.wifiNetworkSelected(ssid);
            }
        });

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


