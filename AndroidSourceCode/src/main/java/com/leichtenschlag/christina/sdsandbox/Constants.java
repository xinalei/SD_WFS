package com.leichtenschlag.christina.sdsandbox;

public interface Constants {

    // Message types sent from the ConnectingDevices Handler
    public static final int MESSAGE_STATE_CHANGE = 11;
    public static final int MESSAGE_READ = 12;
    public static final int MESSAGE_WRITE = 13;
    public static final int MESSAGE_DEVICE_NAME = 14;
    public static final int MESSAGE_TOAST = 15;

    public static final int CONNECTION_ESTABLISHED = 16;
    public static final int DISCONNECT = 17;
    public final static int REQUEST_ENABLE_BT = 101;
    public final static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Key names received from the ConnectingDevices Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    public static final String NO_DEVICE_CONNECTED = "No Connection";
    public static final String BT_SEARCH = "Scanning for Bluetooth Devices";
    public static final String BT_TRY_CONNECT = "Attempting to Connect to Selected Bluetooth Device";
    public static final String BT_FAIL_CONNECT = "Failed to Connect to Selected Bluetooth Device. Please Try Again!\nNo Connection";

    public static final String TITLE_BLUETOOTH = "Bluetooth Setup";
    public static final String TITLE_WIFI = "Wireless Network Setup";
    public static final String TITLE_CAMERA = "IP Camera Setup";
    public static final String TITLE_MANUAL = "Manual Mode";
    public static final String TITLE_AUTO = "Autonomous Mode";

    public static final String WFS_CAM_ADDR = "http://wfs:group30@192.168.1.19/image.jpg";

}