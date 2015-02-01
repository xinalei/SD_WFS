package com.leichtenschlag.christina.sdsandbox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements SelectNetworkDialog.NetworkSelectListener {

    static BluetoothAdapter mBluetoothAdapter;
    static ListView btDeviceList;
    static ArrayAdapter<String> btArrayAdapter;
    static ConnectingDevices mConnectingDevices = null;
    static BluetoothDevice device = null;
    static String btDeviceName = null, availableWifiNetworks = null;
    static CommandFragment commandFrag;

    private TextView connectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mainactivity_container, new BluetoothFragment())
                    .commit();
        }

        // Lock orientation to portrait.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Create instance of ConnectingDevices
        mConnectingDevices = new ConnectingDevices(mHandler);
        // Specify TextView
        connectionStatus = (TextView) findViewById(R.id.textView_connectionstatus);

        // Set up bluetooth adapter stuff.
        setupBluetoothFragment();

        // Register receiver
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    private void setupBluetoothFragment() {
        // Get the Android device's bluetooth radio. We need bluetooth for this to work!
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            this.finish(); // kill the app
        }

        // Set TextView connectionStatus
        connectionStatus.setText(Constants.NO_DEVICE_CONNECTED);

        // At this point we have the Android device's bluetooth radio. Now make sure bluetooth is ON.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Constants.REQUEST_ENABLE_BT) {

            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled and we're good to go
            } else {
                Context context = getApplicationContext();
                CharSequence text = "Bluetooth was not enabled. The application will exit now.";
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();

                this.finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(ActionFoundReceiver);
    }

    // Class variable, for when we discover bluetooth devices.
    private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) { // We've found a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                btArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * The Handler that gets information back from the ConnectingDevices
     */
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Context context = getApplicationContext();

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    if(commandFrag.obtain_sd) {
                        commandFrag.updateScanLog(readMessage);
                        for(int i=0; i<readBuf.length; i++) {
                            if(0x7F == readBuf[i]) {

                                availableWifiNetworks = commandFrag.stopCollectingScanData();
                                break;
                            }
                        }
                    } else {
                        commandFrag.updateLog(readMessage, true);
                    }

                    if(null != availableWifiNetworks) {
                        selectANetwork();
                    }
//                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != context) {
                        Toast.makeText(context, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:

                    if(null != context) {
                        Toast.makeText(context, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                        // this doesn't exactly work.
                    }
                    break;
                case Constants.CONNECTION_ESTABLISHED:
                    if(null != context) {
                        connectionStatus.setText("Connection Established with " + btDeviceName);
                        enterCommandMode();
                    }
                    break;
                case Constants.DISCONNECT: // Lost connection with bluetooth module.
                    // First replace command fragment with bluetooth fragment
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.mainactivity_container, new BluetoothFragment())
                            .commitAllowingStateLoss();
                    // Now put back bluetooth fragment
                    setupBluetoothFragment();

                    if(null != context) {
                        Toast.makeText(context, "Lost Bluetooth connection. Please reconnect to a device.", Toast.LENGTH_LONG).show();
                        // this doesn't exactly work.
                    }
                    break;
            }
        }
    };

    // We've successfully "connected to" another device, so open up the command window!
    private void enterCommandMode() {
        commandFrag = new CommandFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainactivity_container, commandFrag)
                .commit();
    }

    // We have data to be able to choose which wifi network to connect to. So lets do it!
    private void selectANetwork() {
//        commandFrag = new CommandFragment();
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.mainactivity_container, commandFrag)
//                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
