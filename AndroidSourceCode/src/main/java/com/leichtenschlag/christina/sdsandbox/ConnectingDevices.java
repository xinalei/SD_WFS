package com.leichtenschlag.christina.sdsandbox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ConnectingDevices {

    boolean bconnectedthread = false;
    boolean bconnectthread = false;
    boolean bconnectedthreadstart = false;
    boolean bconnectthreadstart = false;
    boolean brun = false;
    boolean bconnectedsynchronized = false;

    private BluetoothAdapter mBluetoothAdapter = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;
    private Handler mHandler; // Handler so that other threads can send information back to this UI thread

    public ConnectingDevices(Handler uiHandler) {
        // Defines a Handler object that's attached to the UI thread
        mHandler = uiHandler;
    }

    public void write(byte[] out) {
        boolean a = bconnectedthread;
        boolean b = bconnectthread;
        boolean c = bconnectedthreadstart;
        boolean d = bconnectthreadstart;
        boolean e = brun;
        boolean f = bconnectedsynchronized;

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {

            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);

    }

    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;


        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            bconnectthread = true;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.cancelDiscovery();
            brun = true;
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //bconnectedthreadstart = true;

            connected(mmSocket);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    // Listener for the Wifly Module
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            bconnectedthread = true;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            // Listening to messages being sent from Wifly module
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI activity
                    // We've received a message from the Wifly module!
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }

            // When this thread ends, we're no longer connected to the bluetooth device
            mHandler.obtainMessage(Constants.DISCONNECT).sendToTarget();
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.v("ConnectedThread", "connect");
        bconnectthreadstart = true;
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    public synchronized void connected(BluetoothSocket socket) {
        Log.v("ConnectedThread", "connected");

        String t = "test message";
        //t.getBytes(); // byte array

        //mHandler.obtainMessage(Constants.MESSAGE_TOAST, t.getBytes().length, -1, t.getBytes()).sendToTarget();
        mHandler.obtainMessage(Constants.CONNECTION_ESTABLISHED).sendToTarget();

        bconnectedsynchronized = true;
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }
}