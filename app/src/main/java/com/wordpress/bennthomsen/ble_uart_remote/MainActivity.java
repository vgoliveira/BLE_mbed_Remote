
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wordpress.bennthomsen.ble_uart_remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;

    private static final int OPERATION = 0x00;
    private static final int NO_OPERATION = 0x00;
    private static final int COMMAND = 0x01;
    private static final int PROGRAM = 0x02;

    private static final int PRESSED = 0x01;
    private static final int NOT_PRESSED = 0x00;

    private static final int TIME_MORE = 0x04;
    private static final int TIME_LESS = 0x05;
    private static final int DOUGH_QNT = 0x02;
    private static final int INIT_STOP = 0x06;
    private static final int OPTIONS = 0x01;
    private static final int COLOR = 0x03;
    private static final int TEST_LED = 0x07;

    private int mState = UART_PROFILE_DISCONNECTED;
    private boolean isBound = false;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect,led2Hold,time_more, time_less, dough_qnt, options, color, init_stop;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        led2Hold = (Button) findViewById(R.id.button_hold);
        time_more = (Button) findViewById(R.id.button_time_more);
        time_less = (Button) findViewById(R.id.button_time_less);
        dough_qnt = (Button) findViewById(R.id.dough_qnt);
        options = (Button) findViewById(R.id.options);
        color = (Button) findViewById(R.id.color);
        init_stop = (Button) findViewById(R.id.init_stop);

        // don´t allow the user click on these buttons if not connected and GATT service discovered.
        led2Hold.setEnabled(false);
        time_more.setEnabled(false);
        time_less.setEnabled(false);
        dough_qnt.setEnabled(false);
        options.setEnabled(false);
        color.setEnabled(false);
        init_stop.setEnabled(false);

        service_init();

        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();

                        }
                    }
                }
            }
        });

        // TEST PROGRAM
        led2Hold.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                byte[] value;
                value = new byte[] {NO_OPERATION,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED};
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    value[OPERATION] = PROGRAM;
                    value[OPTIONS] = 1;
                    value[DOUGH_QNT] = 1;
                    value[COLOR] = 1;
                    value[TIME_MORE] = 2;
                    value[TIME_LESS] = 1;
                    value[INIT_STOP] = 1;
                    mService.writeRXCharacteristic(value);
                    led2Hold.setBackgroundColor(0xFFFDFBB3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //value[OPERATION] = COMMAND;
                    //value[TEST_LED] = NOT_PRESSED;
                    //mService.writeRXCharacteristic(value);
                    led2Hold.setBackgroundColor(0xFFCAC7C7);
                }
                return true;
            }
        });

        time_more.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                byte[] value;
                value = new byte[] {NO_OPERATION,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED};
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    value[OPERATION] = COMMAND;
                    value[TIME_MORE] = PRESSED;
                    mService.writeRXCharacteristic(value);
                    time_more.setBackgroundColor(0xFFFDFBB3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    value[OPERATION] = COMMAND;
                    value[TIME_MORE] = NOT_PRESSED;
                    mService.writeRXCharacteristic(value);
                    time_more.setBackgroundColor(0xFFCAC7C7);
                }
                return true;
            }
        });

        time_less.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                byte[] value;
                value = new byte[] {NO_OPERATION,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED};
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    value[OPERATION] = COMMAND;
                    value[TIME_LESS] = PRESSED;
                    mService.writeRXCharacteristic(value);
                    time_less.setBackgroundColor(0xFFFDFBB3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    value[OPERATION] = COMMAND;
                    value[TIME_LESS] = NOT_PRESSED;
                    mService.writeRXCharacteristic(value);
                    time_less.setBackgroundColor(0xFFCAC7C7);
                }
                return true;
            }
        });

        dough_qnt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                byte[] value;
                value = new byte[] {NO_OPERATION,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED};
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    value[OPERATION] = COMMAND;
                    value[DOUGH_QNT] = PRESSED;
                    mService.writeRXCharacteristic(value);
                    dough_qnt.setBackgroundColor(0xFFFDFBB3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    value[OPERATION] = COMMAND;
                    value[DOUGH_QNT] = NOT_PRESSED;
                    mService.writeRXCharacteristic(value);
                    dough_qnt.setBackgroundColor(0xFFCAC7C7);
                }
                return true;
            }
        });

        options.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                byte[] value;
                value = new byte[] {NO_OPERATION,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED};
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    value[OPERATION] = COMMAND;
                    value[OPTIONS] = PRESSED;
                    mService.writeRXCharacteristic(value);
                    options.setBackgroundColor(0xFFFDFBB3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    value[OPERATION] = COMMAND;
                    value[OPTIONS] = NOT_PRESSED;
                    mService.writeRXCharacteristic(value);
                    options.setBackgroundColor(0xFFCAC7C7);
                }
                return true;
            }
        });

        color.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                byte[] value;
                value = new byte[] {NO_OPERATION,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED};
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    value[OPERATION] = COMMAND;
                    value[COLOR] = PRESSED;
                    mService.writeRXCharacteristic(value);
                    color.setBackgroundColor(0xFFFDFBB3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    value[OPERATION] = COMMAND;
                    value[COLOR] = NOT_PRESSED;
                    mService.writeRXCharacteristic(value);
                    color.setBackgroundColor(0xFFCAC7C7);
                }
                return true;
            }
        });

        init_stop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                byte[] value;
                value = new byte[] {NO_OPERATION,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED,NOT_PRESSED};
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    value[OPERATION] = COMMAND;
                    value[INIT_STOP] = PRESSED;
                    mService.writeRXCharacteristic(value);
                    init_stop.setBackgroundColor(0xFFFDFBB3);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    value[OPERATION] = COMMAND;
                    value[INIT_STOP] = NOT_PRESSED;
                    mService.writeRXCharacteristic(value);
                    init_stop.setBackgroundColor(0xFFCAC7C7);
                }
                return true;
            }
        });

    }
    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            nfc_init(); // only check for NFC after ServiceConnection is finished;
        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;

            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        led2Hold.setEnabled(false);
                        time_more.setEnabled(false);
                        time_less.setEnabled(false);
                        dough_qnt.setEnabled(false);
                        options.setEnabled(false);
                        color.setEnabled(false);
                        init_stop.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();

                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
               // mService.enableTXNotification();
                led2Hold.setEnabled(true);
                time_more.setEnabled(true);
                time_less.setEnabled(true);
                dough_qnt.setEnabled(true);
                options.setEnabled(true);
                color.setEnabled(true);
                init_stop.setEnabled(true);

            }

            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }


        }
    };
    /*
    Known bugs of nfc_init:
        - Does not check if the device is there since .getRemoteDevice(deviceAddress) does not do it
        - mDevice.getName() is returning null if the app connects to the machine before any manual attempt (using scanlist)
    Possible solution:
        Implements scan here using the MAC Address from tha NFC tag, but only connect if this was found in the scan process
     */
    private void nfc_init(){
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            NdefMessage[] msgs = null;
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            String deviceAddress;

            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; ++i) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
                NdefRecord[] records = msgs[0].getRecords();
                deviceAddress = new String(records[1].getPayload(), 1, records[1].getPayload().length-1, Charset.forName("UTF-8")); // record 1 contains the MAC Address
                deviceAddress = deviceAddress.substring(2); // remove the language mark "en" coded in the NDEF text/plain record
                mDevice = mBtAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress); // não testa só retorna mDevice com o endereço passado
                ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                mService.connect(deviceAddress);
                Log.d(TAG, "... NFC_init mDevice= " + mDevice + " mService= " + mService);
                }
            }
        }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
}