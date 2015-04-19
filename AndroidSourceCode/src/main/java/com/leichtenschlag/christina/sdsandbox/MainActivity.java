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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements SelectNetworkDialog.NetworkSelectListener,
                                                                PasswordDialog.PasswordListener,
                                                                VideoSetupFragment.VideoFragmentListener,
                                                                IPDialog.IPListener
{
    static BluetoothAdapter mBluetoothAdapter;
    static ListView btDeviceList;
    static ArrayAdapter<String> btArrayAdapter;
    static ConnectingDevices mConnectingDevices=null;
    static BluetoothDevice device=null;
    static String btDeviceName=null, availableWifiNetworks = null,
            userSelectedNetwork=null, userSelectedNetworkPos=null,
            ip = Constants.WFS_CAM_ADDR,
            currentRSSI=null;
    static TextView connectionStatus, title;
    static StringBuilder rssi;

    enum Mode { AUTONOMOUS, MANUAL, SETUP };
    static Mode appMode = null;

    // Fragments
    static CommandFragment commandFrag;
    static LoadingDialogFragment loadFrag=null;
    static ManualControlFragment manFrag=null;
    static AutonomousCompletionDialog autoComplete=null;

    /// Android Activity Functions /////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mainactivity_container, new BluetoothFragment())
                    .commit();
        }

        // Force the logo to show
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.drawable.ic_launcher);
        actionBar.setDisplayUseLogoEnabled(true);

        // Lock orientation to portrait.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Create instance of ConnectingDevices
        mConnectingDevices = new ConnectingDevices(mHandler);
        // Specify TextView
        connectionStatus = (TextView) findViewById(R.id.textView_connectionstatus);
        title = (TextView) findViewById(R.id.textView_maintitle);
        title.setText(Constants.TITLE_BLUETOOTH); // set title of screen.

        // Set up bluetooth adapter stuff.
        setupBluetoothFragment();

        // Register receiver
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        rssi = new StringBuilder();
        appMode = Mode.SETUP;
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
    public void onResume() {
        super.onResume();
        Log.v("app is resuming", "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(ActionFoundReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_bt_disconnect) {

            if(null != btDeviceName) {
                // User clicked on Disconnect from Bluetooth Device.
                mConnectingDevices.write("E".getBytes()); // Tell wifly module to exit.
                mConnectingDevices.write("E".getBytes()); // Tell wifly module to exit.
                mConnectingDevices.disconnect(); // Stop thread for BT comm.
                btDeviceName = null;
                title.setText(Constants.TITLE_BLUETOOTH);
            }
            return true;
        }
        else if(id == R.id.action_camera_refresh) {
            if(null != manFrag) {
                if(null != manFrag.feedTimer) {
                    manFrag.feedTimer.cancel();
                    manFrag.startTimer();
                }
            }
        }
        else if(id == R.id.action_camera_stop) {
            if(null != manFrag) {
                if(null != manFrag.feedTimer) {
                    manFrag.feedTimer.cancel();
                }
            }
        }
        else if(id == R.id.action_camera_change) {
            // Launch dialog to change ip address

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog_changeIP");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the dialog.
            DialogFragment newFragment = new IPDialog();
            if(null == newFragment) {
                Toast.makeText(getApplicationContext(), "Could not make IP dialog.", Toast.LENGTH_SHORT).show();
            }
            else {
                newFragment.show(ft, "dialog_changeIP");
            }
        }

        return super.onOptionsItemSelected(item);
    }


    /// User-Defined Class Variables /////////////////////////////////////////////////////////////////////////////////////

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
//                    byte[] writeBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ: // We've received data from the Wifly module.

                    // Construct a string from the valid bytes in the buffer
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    if(Mode.AUTONOMOUS == appMode || Mode.MANUAL == appMode) { // Want to update the RSSI.
                        rssi.append(readMessage);

                        // First check if it's the signal that the autonomous algorithm
                        if(!haveWeCompletedAlgorithm()) {

							// Not done with the algorithm, so it must be RSSI data.
							String ret = determineIfRSSI();
							if(null != ret && 0 != ret.length()) {
								manFrag.updateSignalStrength(ret);
							}
						}
                        else {
                            MainActivity.title.setText(Constants.TITLE_MANUAL); // set title of screen.
                            addAutoCompleteDialog();
                        }
                    }
                    else if(commandFrag.obtain_sd) { // We're in the midst of collecting scan data.

                        commandFrag.updateScanLog(readMessage); // First add message to commandFrag variable
                        // Have we collected data for all the networks?
                        // TODO: Implement a timeout in case we lose data (?!??!)
                        if(commandFrag.determineIfDoneScanning()) {
                            // Done collecting.
                            availableWifiNetworks = commandFrag.stopCollectingScanData();
                            selectANetwork(); // Open dialog for user to select a network from list.
                        }
                    }
                    else { // It's not scan data. Just put it in the log.
                        commandFrag.updateLog(readMessage, true);
                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME: // Save the connected device's name

                    String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != context) {
                        Toast.makeText(context, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST: // Make a Toast message

                    if(null != context) {
                        Toast.makeText(context, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                        // this doesn't exactly work.
                    }
                    break;
                case Constants.CONNECTION_ESTABLISHED: // BT Connection has been made.
                    if(null != context) {
                        connectionStatus.setText("Connection Established with " + btDeviceName);

                        addVideoSetupFragment();
                    }
                    break;
                case Constants.DISCONNECT: // Lost connection with bluetooth module.

                    title.setText(Constants.TITLE_BLUETOOTH); // Set title to indicate lost connection

                    // What if manual control

                    // Kill SelectNetworkDialog if it exists.
                    Fragment f = getSupportFragmentManager().findFragmentByTag("dialog_selectnetwork");
                    if(null != f) {
                        getSupportFragmentManager().beginTransaction().remove(f).commit();
                    }

                    // Kill PasswordDialog if it exists
                    f = getSupportFragmentManager().findFragmentByTag("dialog_enterpw");
                    if(null != f) {
                        getSupportFragmentManager().beginTransaction().remove(f).commit();
                    }

                    // Back to the main activity! i.e. replace bluetooth fragment.
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.mainactivity_container, new BluetoothFragment())
                            .commitAllowingStateLoss();

                    // Now put back bluetooth fragment
                    setupBluetoothFragment();
                    btDeviceName = null;
                    appMode = Mode.SETUP;

                    if(null != context) {
                        Toast.makeText(context, "Lost Bluetooth connection. Please reconnect to a device.", Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    };


    /// User-Defined Methods /////////////////////////////////////////////////////////////////////////////////////

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

    // Add fragment for video setup.
    private void addVideoSetupFragment() {
        VideoSetupFragment vFrag = VideoSetupFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainactivity_container, vFrag)
                .commit();
        title.setText(Constants.TITLE_CAMERA);
    }

    // Open up the command window! Replace current fragment with command frag.
    private void addCommandFragment() {
        commandFrag = new CommandFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainactivity_container, commandFrag)
                .commit();
        title.setText(Constants.TITLE_WIFI);
    }

    // Open up the command window! Replace current fragment with command frag.
    private void addAutoCompleteDialog() {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog_autonomousComplete");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        autoComplete = AutonomousCompletionDialog.newInstance(currentRSSI);
        if(null == autoComplete) {
            Toast.makeText(getApplicationContext(), "Could not make autocomplete dialog.", Toast.LENGTH_SHORT).show();
        }
        else {
            autoComplete.show(ft, "dialog_autonomousComplete");
        }
    }


    // We have data to be able to choose which wifi network to connect to. So lets do it!
    private void selectANetwork() {

        // Only start the dialog to select a network if we have enough data.
        if(null != availableWifiNetworks && availableWifiNetworks.length() >  8) {

            // DialogFragment.show() will take care of adding the fragment
            // in a transaction.  We also want to remove any currently showing
            // dialog, so make our own transaction and take care of that here.
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog_selectnetwork");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the dialog.
            SelectNetworkDialog newFragment = SelectNetworkDialog.newInstance(availableWifiNetworks);
            ArrayList<String> list = newFragment.parseScanData(); // parse the data, create a hashmap to remove duplicate networks.

            Bundle arg = new Bundle();
            arg.putStringArrayList("networkList", list);
            newFragment.setArguments(arg);

            newFragment.show(ft, "dialog_selectnetwork");
        }
        else {
            // Nothing was found.
            Toast.makeText(getApplicationContext(), "No networks were found. Try scanning again.", Toast.LENGTH_SHORT).show();
        }

        availableWifiNetworks = null; // reset to null.
    }

    // Methods that must be implemented
    public void wifiNetworkSelected(String ssid, String position) {

        userSelectedNetwork = ssid;
        userSelectedNetworkPos = position;
        // Prompt user for a password.

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog_enterpw");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = PasswordDialog.newInstance(ssid);
        if(null == newFragment) {
            Toast.makeText(getApplicationContext(), "Could not make password dialog.", Toast.LENGTH_SHORT).show();
        }
        else {
            newFragment.show(ft, "dialog_enterpw");
        }
    }

    public void updatePassword(String p) {

        // Send 'P' for Password command
        mConnectingDevices.write("P".getBytes()); // sends data to MSP

        // Send "password \n networkPos \n"
        String pwData = p + "\n" + userSelectedNetworkPos + "\n"; // construct pw data.
        mConnectingDevices.write(pwData.getBytes()); // send password to MSP
        commandFrag.updateLog("sending pw " + p + "and network num " +userSelectedNetworkPos, false);
    }

    public void updateIPaddr(String p)
    {
        // Simply need to change the ip for main activity.
        MainActivity.ip = p;

//        if(null != manFrag) {
//            if(null != manFrag.feedTimer) {
//                manFrag.feedTimer.cancel();
//                manFrag.startTimer();
//            }
//        }
    }

    public void camSetupComplete(String ipaddr) {
        this.ip = ipaddr;
        addCommandFragment();
    }

    // Function to determine whether or not the data returned is the full RSSI value.
    // This gets called when the app is in either autonomous or manual mode, to update the TextView
    public String determineIfRSSI() {
        String ret = null;
        String rssidata = rssi.toString();
        if(null != rssidata) {
            // can extract the data.

            Integer start=null, end=null;
            for(int i=0; i<rssidata.length(); i++) {
                if(null==start && '*' == rssidata.charAt(i)) {
                    start = i+1;
                }
                else if(null!=start && '*' == rssidata.charAt(i)) {
                    end = i;
                    break;
                }
            }

            if(null != start && null != end) {
                rssi.setLength(0);
                ret = rssidata.substring(start, end).trim();
            }
        }
        return ret;
    }

    // Function to check whether the data we received indicates that the autonomous algorithm
    // was completed or not.
    public boolean haveWeCompletedAlgorithm() {
        String data = rssi.toString();
        if(null != data)
        {

            if(data.contains("@fin")) {
                // Did we get the final RSSI value??
                String[] finval = data.split(String.valueOf('@'));
                /// @fin@-42@ => "", "fin", "-42", "" // need this last char to ensure we got the entire RSSI
                if(null != finval && 4==finval.length)
                {
                    currentRSSI = finval[2];
                    rssi.setLength(0);
                    return true;
                }
                // else we didn't get the rssi yet. so we'll return false & wait for it
            }
        }
        return false;
    }

}
