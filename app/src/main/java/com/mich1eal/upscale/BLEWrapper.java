package com.mich1eal.upscale;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Michael on 4/12/2020.
 */
public class BLEWrapper
{
    private static final String TAG = BLEWrapper.class.getSimpleName();


    private static final UUID uuid = UUID.fromString("218db1b3-920c-4cd9-bf77-dc2580c5b3f9");


    public static final String alarmTime = "ALARM_TIME";
    public static final String username = "USERNAME";
    public static final String hueIP = "HUE_IP";
    public static final String hueTime = "HUE_TIME";

    public static final String dayBright = "DAY_BRIGHT";
    public static final String nightBright = "NIGHT_BRIGHT";

    public static final int MESSAGE_READ = 99;
    public static final String MESSAGE_CANCEL = "cancel";
    public static final String MESSAGE_HEARTBEAT = "heartbeat";
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_SEARCHING = 3;
    public static final int STATE_DISCONNECTED = 4;
    public static final int STATE_NO_BLUETOOTH = 5;
    public static final int STATE_FOUND = 6;

    public static final int BLUETOOTH_RESPONSE = 5999;
    public static final int BLUETOOTH_OK = 1;

    private static boolean autoReconnect = false;

    ConnectedThread conThread;
    SearchThread searchThread;


    private int state;
    private Context context;
    private BluetoothAdapter bAdapter;
    private boolean serverFound = false;
    private Handler handler;
    private final boolean isServer;
    private BroadcastReceiver receiver;

    private ConnectedThread connectedThread;

    public BLEWrapper(Context context, Handler handler)
    {
        assert(context != null);

        this.context = context;
        this.handler = handler;

        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!hasBluetooth()) setState(STATE_NO_BLUETOOTH);

        if (isServer) initServer();
        else connect();
    }

    public void connect()
    {
        setState(STATE_SEARCHING);
    }

    public int getState()
    {
        return state;
    }


    private void setState(int state)
    {

        final int newState = state;
        if (newState == this.state)
        {
            Log.d(TAG, "Set state: " + state + ". State is already set, no action.");
            return;
        }

        this.state = newState;

        Log.d(TAG, "setState: " + newState);

        //Notify handler
        if (handler != null)
        {
            handler.sendEmptyMessage(newState);
        }

        // If no bluetooth, don't start searching
        if (newState == STATE_SEARCHING && !hasBluetooth())
        {
            if (connectedThread != null) connectedThread.cancel();
            if (searchThread != null) searchThread.cancel();

            setState(STATE_NO_BLUETOOTH);
        }
        else if (newState == STATE_DISCONNECTED)
        {
            if (connectedThread != null) connectedThread.cancel();
            if (searchThread != null) searchThread.cancel();

            // Server automatically starts searching again
            if (autoReconnect) setState(STATE_SEARCHING);
        }
        else if (newState == STATE_SEARCHING)
        {
            if (isServer) startServer();
            else startClient();
        }
        else if (newState == STATE_FOUND)
        {
            //If a device is found, stop looking for new ones
            Log.d(TAG, "Device found, unregistering receiver");
            unregister();
        }
    }

    public void setAutoReconnect(boolean autoReconnect){this.autoReconnect = autoReconnect;}

    private boolean hasBluetooth()
    {
        return (bAdapter != null && bAdapter.isEnabled());
    }

    public void unregister()
    {
        if (receiver != null)
        {
            context.unregisterReceiver(receiver);
            receiver = null;
        }
    }

    private void initServer()
    {
        bAdapter.setName(SERVER_NAME);
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        ((Activity) context).startActivityForResult(enableIntent, BLUETOOTH_RESPONSE);
    }

    //This method needs to be called by the host activity after getting the activity result back
    // from initServer(). This is a terrible implementation and should be fixed
    public void onServerInit()
    {
        setState(STATE_SEARCHING);
    }

    private void startServer()
    {
        searchThread = new ServerThread();
        searchThread.start();
    }

    private void startClient()
    {
        // First check for already established pairings
        ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
        pairedDevices.addAll(bAdapter.getBondedDevices());

        for (BluetoothDevice device : pairedDevices)
        {
            String name = device.getName();
            //Log.d(TAG, "Found a pre-paired device: " + name);

            if (name != null && device.getName().equals(SERVER_NAME))
            {
                Log.d(TAG, "Pre-paired server found: " + device.toString());
                searchThread = new ClientThread(device);
                searchThread.start();
                return;
            }
        }
        // Otherwise check for new devices
        Log.d(TAG, "No server device paried, searching now");

        if (receiver != null)
        {
            Log.d(TAG, "Receiver is already registered!");
        }
        else {
            //Need to create a reciever
            receiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(BluetoothDevice.ACTION_FOUND)) //device found
                    {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            //Log.d(TAG, "Device found: " + device.toString());
                            String name = device.getName();

                            if (name != null && name.equals(SERVER_NAME)) {
                                Log.d(TAG, "New server found: " + name);
                                searchThread = new ClientThread(device);
                                searchThread.start();
                                return;
                            }
                        }
                    } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                            && state != STATE_FOUND
                            && state != STATE_CONNECTED) {
                        setState(STATE_DISCONNECTED);
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            context.registerReceiver(receiver, filter);


        }

        bAdapter.startDiscovery();
    }

    public void close()
    {
        Log.d(TAG, "Closing connections");
        setState(STATE_DISCONNECTED);

        unregister();
    }

    // Wraper to hold ServerThread and ClientThread
    abstract class SearchThread
            extends Thread
    {
        abstract public void run();
        abstract public void cancel();
    }

    public class ServerThread
            extends SearchThread
    {
        private final BluetoothServerSocket serverSocket;

        public ServerThread()
        {
            // Use a temporary object that is later assigned to serverSocket,
            // because serverSocket is final
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = bAdapter.listenUsingRfcommWithServiceRecord(SERVER_NAME, uuid);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            serverSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Server Thread is running!");
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned
            while (true)
            {
                try
                {
                    socket = serverSocket.accept();
                }
                catch (IOException e)
                {
                    Log.e(TAG, e.getMessage());
                    break;
                }
                // If a connection was accepted
                if (socket != null)
                {
                    Log.d(TAG, "Socket not null");
                    // Do work to manage the connection (in a separate thread)

                    try
                    {
                        connectedThread = new ConnectedThread(socket);
                        connectedThread.start();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        setState(STATE_DISCONNECTED);
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel()
        {
            try
            {
                serverSocket.close();
            }
            catch (IOException e){e.printStackTrace();}
        }
    }

    private class ClientThread
            extends SearchThread
    {
        private final BluetoothSocket socket;
        private final BluetoothDevice client;

        public ClientThread(BluetoothDevice client)
        {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            setState(STATE_FOUND);
            BluetoothSocket tmp = null;
            this.client = client;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try
            {
                tmp = client.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            socket = tmp;
        }

        public void run()
        {
            Log.d(TAG, "Client thread running!");
            // Cancel discovery because it will slow down the connection
            bAdapter.cancelDiscovery();

            try
            {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                socket.connect();
                connectedThread = new ConnectedThread(socket);
                connectedThread.start();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                setState(STATE_DISCONNECTED);
            }
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel()
        {
            try
            {
                socket.close();
            }
            catch (Exception e){e.printStackTrace();}
            finally {searchThread = null;}
        }
    }

    public void write(String str)
    {
        if (state == STATE_CONNECTED && connectedThread != null)
        {
            Log.d(TAG, "Writing string: " + str);
            connectedThread.write(str);
        }
        else Log.d(TAG, "Cannot write string: " + str);
    }

    private class ConnectedThread
            extends Thread
    {
        private final BluetoothSocket socket;
        private final DataInputStream inStream;
        private final DataOutputStream outStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            this.socket = socket;
            DataInputStream tmpIn = null;
            DataOutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try
            {
                tmpIn = new DataInputStream(socket.getInputStream());
                tmpOut = new DataOutputStream(socket.getOutputStream());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run()
        {
            Log.d(TAG, "ConnectedThread running!");
            setState(STATE_CONNECTED);

            // Keep listening to the InputStream until an exception occurs
            while (true)
            {
                if (!socket.isConnected()) setState(STATE_DISCONNECTED);
                try
                {
                    // Read from the InputStream
                    String msg = inStream.readUTF();
                    Log.d(TAG, "Read something: " + msg);
                    // Send the obtained bytes to the UI activity
                    handler.obtainMessage(MESSAGE_READ, msg)
                            .sendToTarget();
                }
                catch (IOException e)
                {
                    setState(STATE_DISCONNECTED);
                    break; // get out of loop
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String msg)
        {
            try
            {
                outStream.writeUTF(msg);
            }
            catch (IOException e)
            {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel()
        {
            try
            {
                outStream.close();
                inStream.close();
                socket.close();
            } catch (IOException e) {e.printStackTrace();}
            finally{connectedThread = null;}
        }
    }
}
