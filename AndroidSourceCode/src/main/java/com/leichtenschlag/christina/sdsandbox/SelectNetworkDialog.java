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
import java.util.HashMap;

/**
 * Created by Christy on 1/31/2015.
 */
public class SelectNetworkDialog extends DialogFragment {

    /// Class Variables /////////////////////////////////////////////

    ArrayAdapter<String> wifiArrayAdapter;
    ArrayList<String> networks;
    ListView allNetworks;
    HashMap<String, WFSNetwork> networkData;

    /// Interface ////////////////////////////////////////////////

    // Use this instance of the interface to deliver action events
    static NetworkSelectListener mListener;

    // Interface; these methods are implemented by the MainActivity, allowing communication
    // between this dialog and the MainActivity.
    public interface NetworkSelectListener {
        void wifiNetworkSelected(String ssid, String position);
    }

    /// Fragment Methods /////////////////////////////////////////////////

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_select_network, container, false);

        getDialog().setTitle("Select A Wireless Network");

        networks = getArguments().getStringArrayList("networkList");

        wifiArrayAdapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_list_item_1, networks);

        allNetworks = (ListView) v.findViewById(R.id.listView_wireless_networks);
        allNetworks.setAdapter(wifiArrayAdapter);
        allNetworks.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String ssid = networks.get(position);
                Log.v("listener for adapter:", ssid);
                dismiss();

                Log.v("selected network #-list", " "+ networkData.get(ssid).networkNum);
                mListener.wifiNetworkSelected(ssid, networkData.get(ssid).networkNum);
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

    /// User-Defined Methods /////////////////////////////////////////////////

    public static SelectNetworkDialog newInstance(String s) {
        SelectNetworkDialog myFragment = new SelectNetworkDialog();

        if(null == s) {
            return null;
        }

        Bundle arg = new Bundle();
        arg.putString("inpData", s);
        myFragment.setArguments(arg);
        return myFragment;
    }

    public ArrayList<String> parseScanData() {

        String data = getArguments().getString("inpData");
        if(null == data) return null;

        networkData = new HashMap<>();

        // Get number of networks.

        StringBuilder curr = new StringBuilder();
        int index = 0;
        // First line is the number of networks found.
        while(index < data.length() && '\n' != data.charAt(index)) {
            curr.append(data.charAt(index++));
        }
        index++; // index is at the newline for line containing "numnetworks\n"

        int numnetworks = Integer.valueOf(curr.toString().trim()); // will throw if not an int.
        Log.v("numnetworks",String.valueOf(numnetworks));
        curr.setLength(0); // Clear curr

        // Retrieve the (numnetworks) networks & create hashmap of WFSNetwork s

        while(index < data.length()) {

            if(0x0D == data.charAt(index) || 0x0A == data.charAt(index)) { // carriage return
                WFSNetwork potential = new WFSNetwork(curr.toString());

                // Don't add network if there's no visible SSID
                if(null == potential.SSID || 0 == potential.SSID.trim().length()) {
                    curr.setLength(0);
                    index++;
                    continue;
                }

                if(networkData.containsKey( potential.SSID) ) {
                    WFSNetwork preexisting = networkData.get(potential.SSID);
                    if( Integer.parseInt(preexisting.RSSI) < Integer.parseInt(potential.RSSI) ) { // potential throw here
                        // Replace preexisting with potential
                        networkData.put(potential.SSID, potential);
                    }
                }
                else {
                    networkData.put(potential.SSID, potential);
                }
                curr.setLength(0); // reset curr
            }
            else if(0x7F == data.charAt(index)) {
                break;
            }
            else { // append to curr.
                curr.append( data.charAt(index) );
            }

            index++;
        }


        networks = new ArrayList<>();
        for(String ssid : networkData.keySet()) {
            networks.add(ssid);
            Log.v("final network list:", ssid);
        }
        return networks;
    }

    /// Private subclass //////////////////////////////////////////////

    private class WFSNetwork {

        String networkNum;
        String RSSI;
        String SSID;

        public WFSNetwork(String i) {

            String[] data = i.split(",");
            if(data.length != 3) {
                // Error case
                networkNum = null;
                RSSI = null;
                SSID = null;
            }
            else {
                networkNum = data[0];
                RSSI = data[1];
                SSID = data[2];
            }
        }


    }
}


