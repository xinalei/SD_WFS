package com.leichtenschlag.christina.sdsandbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class BluetoothFragment extends Fragment {


    private Button bt_start_scanning;
    private Button bt_stop_scanning;

    public BluetoothFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        // Get ListView and create / add an adapter for it.
        MainActivity.btDeviceList = (ListView) rootView.findViewById(R.id.listView_bt_devices); // this is null.
        MainActivity.btArrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
        MainActivity.btDeviceList.setAdapter(MainActivity.btArrayAdapter);

        // OnClick for each device in the list
        MainActivity.btDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                // We've clicked on an item; now what?

                MainActivity.connectionStatus.setText(Constants.BT_TRY_CONNECT); // Set textview so user can see what's going on.

                // Cancel discovery because it's costly and we're about to connect
                MainActivity.mBluetoothAdapter.cancelDiscovery();

                // Get the device MAC address, which is the last 17 chars in the View
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                // Create the result Intent and include the MAC address
                Intent intent = new Intent();
                intent.putExtra(Constants.EXTRA_DEVICE_ADDRESS, address);

                address = intent.getExtras().getString(Constants.EXTRA_DEVICE_ADDRESS);
                // Get the BluetoothDevice object
//                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                MainActivity.device = MainActivity.mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                MainActivity.mConnectingDevices.connect(MainActivity.device);

                // Set MainActivity class variable name for titles & whatever [= !
                MainActivity.btDeviceName = MainActivity.device.getName();
            }
        });

        // OnClick to scan for bluetooth devices
        bt_start_scanning = (Button) rootView.findViewById(R.id.button_bt_scan);
        bt_start_scanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity.connectionStatus.setText(Constants.BT_SEARCH);
                if (MainActivity.mBluetoothAdapter.isDiscovering()) {
                    MainActivity.mBluetoothAdapter.cancelDiscovery();
                }

                // Begin discovering devices.
                MainActivity.mBluetoothAdapter.startDiscovery();
            }
        });

        // OnClick to stop app from scanning for BT devices
        bt_stop_scanning = (Button) rootView.findViewById(R.id.button_bt_stop);
        bt_stop_scanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity.connectionStatus.setText(Constants.NO_DEVICE_CONNECTED);
                if (MainActivity.mBluetoothAdapter.isDiscovering()) {
                    MainActivity.mBluetoothAdapter.cancelDiscovery();
                }
            }
        });

        return rootView;
    }
}