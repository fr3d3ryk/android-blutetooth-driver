package com.bluetoothdriverspp.driver;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class BluetoothSingleton extends Activity {

    private static BluetoothSPP bt = null;
    private static ProgressDialog progressDialog;
    public static final int TIME_OUT = 20000;
    private static Activity activity;

    public static BluetoothSPP getInstance(Activity activity) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(activity);
        String deviceName = preference.getString("bluetooth", "");
        if (bt == null) {
            createBSPP(activity, deviceName);
        }

        return bt;
    }

    private static void createBSPP(final Activity activity, String deviceName) {
        bt = new BluetoothSPP(activity);

        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            activity.finish();
        }

        BluetoothSingleton.activity = activity;
        bt.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener() {
            @Override
            public void onServiceStateChanged(int state) {
                if (state == BluetoothState.STATE_CONNECTED) {
                    Toast.makeText(activity, "Connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Connecting Bluetooth...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (!bt.isBluetoothEnabled()) {
            bt.enable();
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                try {
                    Toast.makeText(activity, "Conectando com " + deviceName, Toast.LENGTH_SHORT).show();
                    setConnectionTimeOut(activity);
                    bt.autoConnect(deviceName);

                    /*
                      Method responsible for returning data received via bluetooth
                     */
                    bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
                        @Override
                        public void onDataReceived(final byte[] buffer) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String messageReceived = new String(buffer); // make what you want with the data received
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    stopSingleton();
                    activity.finish();
                    Toast.makeText(activity, "Error connecting bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static void stopSingleton() {
        if (bt != null) {
            bt.disconnect();
            bt.stopService();
            bt = null;
        }
    }

    private static void setConnectionTimeOut(final Activity activity) {
        final Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            int count = 0;

            @Override
            public void run() {
                if (bt != null && bt.getServiceState() != BluetoothState.STATE_CONNECTED) {
                    if (count == 10) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                stopSingleton();
                                Toast.makeText(activity, "Timeout", Toast.LENGTH_SHORT).show();
                            }
                        });
                        t.cancel();
                    }
                } else {
                    t.cancel();
                }
                count++;
            }
        }, 0, TIME_OUT / 10);
    }

    /**
     * Method responsible for sending data via bluetooth
     * @param message
     */
    public static void send(final String message) {
        if (bt != null) {
            bt.send(message, false);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Activity.text.append("Bluetooth TX " + message + "\n");
                }
            });
        }
    }
}
