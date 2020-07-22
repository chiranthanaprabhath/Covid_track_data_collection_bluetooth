package com.example.covid_track;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.R.layout;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.content.pm.PackageManager.PERMISSION_DENIED;

public class MainActivity extends AppCompatActivity {

    /** UI Search Button */


    /** Bluetooth interface provided by Android SDK */
    private BluetoothAdapter mBluetoothAdapter;

    private DatabaseReference mDatabase= FirebaseDatabase.getInstance().getReference("CollectionData");
    /** Interfaces between the list view and the model */
    private ArrayAdapter<String> mArrayAdapter;

    /** List of bluetooth devices */
    private ArrayList<String> mBluetoothDevices;
    private String output;
    private RadioButton yes;
    private RadioButton no;


    /** Broadcast Bluetooth signal to discover new Bluetooth devices */
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        /**
         * Once a new Bluetooth device is discovered, onReceive is called and the device can be
         * processed.
         * @param context Application Context.
         * @param intent Intent.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                addDevice(device,rssi);
            } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
                toggleSearchButton(true, "Search");
            }
        }
    };


    /**
     * When the search button is clicked, the button is disabled and and the Bluetooth Adapter is
     * used to discover new devices.
     *
     * @param view the button view clicked.
     */
    public void onSearch(View view) {
        toggleSearchButton(false, "Searching");
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        yes =  findViewById(R.id.radioButton2);
        no =  findViewById(R.id.radioButton1);
        yes.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                no.setChecked(false);
                yes.setChecked(true);
                output="1";
            }
        });
        no.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                yes.setChecked(false);
                no.setChecked(true);
                output="0";
            }
        });
        // Initialize views

        ListView mListViewBluetoothDevices = findViewById(R.id.list_bluetooth_devices);

        // Initialize bluetooth adapter
        mBluetoothAdapter = getDefaultAdapter();
        backgroundNameChange newbackGP = new backgroundNameChange();
        newbackGP.start();
        // Setup list view
        mBluetoothDevices = new ArrayList<>();
        mArrayAdapter = new ArrayAdapter<>(this, layout.simple_list_item_1, mBluetoothDevices);
        mListViewBluetoothDevices.setAdapter(mArrayAdapter);
        // Ensure bluetooth is enabled
        ensureBluetoothIsEnable();
        // Request location permission on API Level >= 26
        requestAccessFineLocation();
        // Register BroadcastReceiver
        registerBroadcastReceiver(new String[]{ACTION_FOUND, ACTION_DISCOVERY_FINISHED});
    }
    class backgroundNameChange extends Thread{

        @Override
        public void run(){
            while (true){
            String name=getDeviceName()+","+String.valueOf(deviceBattery())+","+String.valueOf(cpuTemperature())+","+String.valueOf(getBatteryCapacity(MainActivity.this));;
            mBluetoothAdapter.setName(name);
            GlobslVariableClass.name=name;
            mBluetoothAdapter.startDiscovery();
            }
        }
        public String getDeviceName() {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
                return capitalize(model);
            } else {
                return capitalize(manufacturer) + " " + model;
            }
        }
        private String capitalize(String s) {
            if (s == null || s.length() == 0) {
                return "";
            }
            char first = s.charAt(0);
            if (Character.isUpperCase(first)) {
                return s;
            } else {
                return Character.toUpperCase(first) + s.substring(1);
            }
        }
        public float deviceBattery(){
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level * 100 / (float)scale;
            return batteryPct;
        }
        public double getBatteryCapacity(Context context) {
            Object mPowerProfile;
            double batteryCapacity = 0;
            final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

            try {
                mPowerProfile = Class.forName(POWER_PROFILE_CLASS)
                        .getConstructor(Context.class)
                        .newInstance(context);

                batteryCapacity = (double) Class
                        .forName(POWER_PROFILE_CLASS)
                        .getMethod("getBatteryCapacity")
                        .invoke(mPowerProfile);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return batteryCapacity;

        }
        public float cpuTemperature()
        {
            Process process;
            try {
                process = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
                process.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                if(line!=null) {
                    float temp = Float.parseFloat(line);
                    if(temp<100.0f){
                        return temp;
                    }
                    else{
                        return temp / 1000.0f;
                    }
                }else{
                    return 51.0f;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0.0f;
            }
        }

        public String getDate() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date today = Calendar.getInstance().getTime();
            return dateFormat.format(today);
        }
        public String getTime() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date today = Calendar.getInstance().getTime();
            return dateFormat.format(today);
        }




    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * Adds a new Bluetooth device to the model and notifies the List View adapter of the data
     * set changed.
     *
     * @param device Bluetooth device to add to the model.
     */
    private void addDevice(BluetoothDevice device,int dbm) {
        String deviceName = device.getName();
        String deviceMacAddress = device.getAddress();
        String signalDbm=Integer.toString(dbm);


        StringBuilder newDeviceString = new StringBuilder();

        if (deviceName != null)
            newDeviceString.append(deviceName).append(" ");

        if (deviceMacAddress != null)
            newDeviceString.append(deviceMacAddress).append("--");
        if (signalDbm != null)
            newDeviceString.append(signalDbm);

        if (!mBluetoothDevices.contains(newDeviceString.toString())) {
            mBluetoothDevices.add(newDeviceString.toString());
            mArrayAdapter.notifyDataSetChanged();
        }
        String str = deviceName;
        List<String> detailList = Arrays.asList(str.split(","));
        GlobslVariableClass.name=detailList.get(0);
        GlobslVariableClass.BetteryValue=detailList.get(1);
        GlobslVariableClass.Tem=detailList.get(2);
        GlobslVariableClass.Cap=detailList.get(3);
        String blueValue=String.valueOf(GlobslVariableClass.deviceBle.get(GlobslVariableClass.deviceModel.indexOf(GlobslVariableClass.name)));
        String blueValueOwner=String.valueOf(GlobslVariableClass.deviceBle.get(GlobslVariableClass.deviceModel.indexOf(getDeviceName())));
        if(!(output==null)){
            deviceData Ddata = new deviceData(GlobslVariableClass.name,GlobslVariableClass.BetteryValue,GlobslVariableClass.Tem,deviceMacAddress,signalDbm,output,GlobslVariableClass.Cap,blueValue,String.valueOf(deviceBattery()),String.valueOf(cpuTemperature()),blueValueOwner);
            mDatabase.child(Geanrate_Name()).setValue(Ddata);
        }


    }
    public float deviceBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float)scale;
        return batteryPct;
    }
    public float cpuTemperature()
    {
        Process process;
        try {
            process = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            if(line!=null) {
                float temp = Float.parseFloat(line);
                if(temp<100.0f){
                    return temp;
                }
                else{
                    return temp / 1000.0f;
                }

            }else{
                return 51.0f;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }
    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }
    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public String Geanrate_Name() {
        String DATA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghtjklmnopqrstunvzxyz";
        Random RANDOM = new Random();
        StringBuilder sb = new StringBuilder(14);

        for (int i = 0; i < 14; i++) {
            sb.append(DATA.charAt(RANDOM.nextInt(DATA.length())));
        }

        return sb.toString();
    }

    /**
     * Enables or disables the search button while simultaneously changing the text.
     *
     * @param enabled if the search button should be enabled.
     * @param text    text to set the button to.
     */
    private void toggleSearchButton(boolean enabled, CharSequence text) {




    }
    private void registerBroadcastReceiver(@NonNull String[] actions) {
        IntentFilter intentFilter = new IntentFilter();

        for (String action : actions)
            intentFilter.addAction(action);

        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    /**
     * Register the Broadcast Receiver with the following actions.
     *
     * @param actions actions to add to the Broadcast Receiver
     */


    /**
     * Ensure Bluetooth is enabled.
     */
    private void ensureBluetoothIsEnable() {
        final int REQUEST_ENABLE_BLUETOOTH = 1;

        if (!mBluetoothAdapter.isEnabled())
            startActivityForResult(new Intent(ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH);
    }

    /**
     * Request Access Fine Location permission.
     */
    private void requestAccessFineLocation() {
        final int REQUEST_ACCESS_FINE_LOCATION = 1;

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_DENIED) {
            // ACCESS_FINE_LOCATION Permission is not granted
            // Request the permission
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }
    }
}
