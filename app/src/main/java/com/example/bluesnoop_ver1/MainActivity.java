package com.example.bluesnoop_ver1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import 	java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Collections;
public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter BTAdapter;
    public  ArrayList<DeviceItem> connectedDeviceList = new ArrayList<>();
    public  ArrayList<DeviceItem> recordedDeviceList = new ArrayList<>();
    public connectionGraph deviceConnectionGraph = new connectionGraph();
    public static int REQUEST_BLUETOOTH = 1;
    private DatabaseConnection dbConnection = new DatabaseConnection();
    private Handler handler = new Handler();

    public String userMacAddress = ""; // need to find way to get this

    long lastScan = 0;
    long currentTimeStamp;
    long duration;
    boolean isScanning=false;
    double longitude = 0;
    double latitude =0;

    TextView scanMins;
    ListView lv;
    Button scanButton;
    String scanIntervals[] = {"30 secs", "1 mins", "2 mins", "5 mins","10 mins"};
    int intervalIndex = 0;

    long scanInternalTimes[] = {30*1000, 60*1000,60*1000 * 2, 60*1000 * 5, 60*1000 * 10 };

    LocationTrack locationTrack;

    String userMac;
    /*
         Method: onCreate
         This is the method that is called when the app is launched for the first time.
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //userMacAddress= getUserMAC();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        locationTrack = new LocationTrack(MainActivity.this);
        scanButton = findViewById(R.id.button);
        BTAdapter = BluetoothAdapter.getDefaultAdapter();


        userMac = getBluetoothMacAddress();

        // Test to ensure device has bluetooth

        if (BTAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        // test if we have GPS premissions
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);


        // Tests when a button has been pressed
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                if(!isScanning) {
                    bluetoothScan();
                    Toast.makeText(getBaseContext(), "Button Pressed", Toast.LENGTH_LONG).show();
                }
            }
        });


        // Displays scan interval time
        View v2 = findViewById(R.id.view2);
        scanMins = findViewById(R.id.scanIntervalTime);


        // Allows user to change scan interval.
        // Tests if user has clicked the interval time.
        v2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                mBuilder.setTitle("Scan Interval");
                mBuilder.setSingleChoiceItems(scanIntervals, -1, new DialogInterface.OnClickListener() {

                    // Record new scan interval
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scanMins.setText(scanIntervals[which]);
                        intervalIndex = which;
                        dialog.dismiss();
                    }
                });

                // User chooses to not change the time.
                mBuilder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = mBuilder.create();
                dialog.show();
            }
        });


        // Recursion autoscan function
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothScan();
                handler.postDelayed(this,scanInternalTimes[intervalIndex])
                ;}
        }, scanInternalTimes[intervalIndex]);
    }

    // GPS
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }


  // returns the devices mac address
  private String getBluetoothMacAddress() {
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      String bluetoothMacAddress = "";
      try {
          Field mServiceField = bluetoothAdapter.getClass().getDeclaredField("mService");
          mServiceField.setAccessible(true);

          Object btManagerService = mServiceField.get(bluetoothAdapter);

          if (btManagerService != null) {
              bluetoothMacAddress = (String) btManagerService.getClass().getMethod("getAddress").invoke(btManagerService);
          }
      } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

      }
      return bluetoothMacAddress;
  }


    private void bluetoothScan()
    {
        isScanning=true;
        currentTimeStamp = new Date().getTime();
        duration = currentTimeStamp - lastScan;
        // Enables bluetooth on the device
        if (!BTAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_BLUETOOTH);
        }


        // sets the device to be discoverable
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivity(discoverableIntent);

        // Register for broadcasts when a device is discovered.
        BTAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        IntentFilter scanFinishedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(scanFinishedReceiver, scanFinishedFilter);
        isScanning=false;
    }

    // Add/ update device in connection lists
    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String newMACAddress = device.getAddress();
                int connectedIndex= alreadyInList(newMACAddress);
                if(connectedIndex  != -1 )
                {
                    deviceConnectionGraph.increseWeighting(newMACAddress);
                    long currentDuration =connectedDeviceList.get(connectedIndex).getDurtion();
                    currentDuration+= duration;
                    connectedDeviceList.get(connectedIndex).setDuration(currentDuration);
                }
                else {
                    locationTrack = new LocationTrack(MainActivity.this);

                    if(checkLocationPermission()) {
                        /// added locations to device list
                        if (locationTrack.canGetLocation()) {
                            longitude = locationTrack.getLongitude();
                            latitude = locationTrack.getLatitude();
                        }
                    }
                    deviceConnectionGraph.addNode(userMacAddress,newMACAddress);

                    int newRssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    int newType = device.getBluetoothClass().getMajorDeviceClass();
                    checkType(newType);
                    DeviceItem newDevice = new DeviceItem(newMACAddress, newRssi,
                            checkType(newType), 0, currentTimeStamp, longitude,latitude);
                    connectedDeviceList.add(newDevice);
                }

                generateDeviceList();
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_DISCOVERY_FINISHED.
    private final BroadcastReceiver scanFinishedReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                checkDisconnection(currentTimeStamp, currentTimeStamp - lastScan);
                serverUpload();
                lastScan = currentTimeStamp;
            }
        }
    };

    // see if an device is still connected
    public int alreadyInList( String currentMacAddress)
    {
        for( int i =0; i<connectedDeviceList.size(); i++  )
        {
            String deviceMac = connectedDeviceList.get(i).getAddress();
            if( deviceMac.equals(currentMacAddress))
            {
                return i;
            }
        }
        return -1;
    }

    // test to see if device are currently connected
    public void checkDisconnection(long currentTimeStamp ,long timeSinceLastScan)
    {
        for( int index =connectedDeviceList.size() -1; index >=0; index-- )
        {

            long deviceTimeStamp =  connectedDeviceList.get(index).getTimeStamp();
            if( currentTimeStamp > (deviceTimeStamp + timeSinceLastScan))
            {
                connectedDeviceList.get(index).setDuration(connectedDeviceList.get(index).getDurtion() + timeSinceLastScan);
                recordedDeviceList.add(connectedDeviceList.get(index));
                connectedDeviceList.remove(index);
            }
        }
    }

    // process devices to be displayed.
    public void generateDeviceList( ){
        lv = findViewById(R.id.lv);
        //Adds these device names to the list
        ArrayAdapter<DeviceItem> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(connectedDeviceList));
        lv.setAdapter(ad);
        lv.setSelection(lv.getCount() - 1);

        //When a list item is pressed a dialog box appears displaying Address, RSSSI and type
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {

                //Cast to DeviceItem so that we can use associated methods.
                // Must ensure only DeviceItem Objects are used

                final DeviceItem clickItemObj = (DeviceItem) adapterView.getAdapter().getItem(index);
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle(clickItemObj.getAddress());
                alertDialog.setMessage("\nAddress: " + clickItemObj.getAddress() +
                        "\n\nRSSI: " + clickItemObj.getRssi() +
                        "\n\nDevice Type: " +  clickItemObj.getType() + "\n\nLongitude: "+ longitude+
                        "\n\nLatitude: "+latitude+"\n\nDuration: "+clickItemObj.getDurtion());
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Add to Whitelist",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dbConnection.addToWhiteList(clickItemObj.getAddress());
                                Toast.makeText(getBaseContext(), "Added To whitelist", Toast.LENGTH_LONG).show();
                            }
                        });


                alertDialog.show();
            }
        });

    }
    // testing gps premissions is still enbaled
    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    // uplsoad recordedDevice to the servers
    public void serverUpload()
    {
        if( recordedDeviceList.size() >= 0 )
        {
            dbConnection.uploadData(recordedDeviceList, userMac);
            dbConnection.UploadWeightGraph(userMac,deviceConnectionGraph);
        }
        // reset recordedDevice
        recordedDeviceList = new ArrayList<>();
        deviceConnectionGraph = new connectionGraph();
    }

    //Adds menu items to menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    //When timeline menu option is selected
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //change page
            Intent intent = new Intent(this, Timeline.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    public String checkType(int type){
        switch(type){
            case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER:
                return "Camcorder";
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                return "Car Audio";
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                return "Handsfree";
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                return "Headphones";
            case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                return "HIFI Audio";
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                return "Loudspeaker";
            case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
                return "Microphone";
            case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                return "Portable Audio";
            case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
                return "Set Top Box";
            case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
                return "Audio Uncategorized";
            case BluetoothClass.Device.AUDIO_VIDEO_VCR:
                return "VCR";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA:
                return "Camera";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING:
                return "Conferencing";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
                return "Display and Loudspeaker";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY:
                return "Video Gaming Toy";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR:
                return "Video Monitor";
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                return "Wearble Headset";
            case BluetoothClass.Device.COMPUTER_DESKTOP:
                return "Desktop";
            case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
                return "Handheld PC";
            case BluetoothClass.Device.COMPUTER_LAPTOP:
                return "Laptop";
            case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                return "Palm Size Marketing";
            case BluetoothClass.Device.COMPUTER_SERVER:
                return "Server";
            case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
                return "Computer Uncategorized";
            case BluetoothClass.Device.COMPUTER_WEARABLE:
                return "Wearable Computer";
            case BluetoothClass.Device.HEALTH_BLOOD_PRESSURE:
                return "Blood Pressure Device";
            case BluetoothClass.Device.HEALTH_DATA_DISPLAY:
                return "Health Data Display";
            case BluetoothClass.Device.HEALTH_GLUCOSE:
                return "Glucose Device";
            case BluetoothClass.Device.HEALTH_PULSE_OXIMETER:
                return "Pulse Oximeter";
            case BluetoothClass.Device.HEALTH_PULSE_RATE:
                return "Pulse Rate";
            case BluetoothClass.Device.HEALTH_THERMOMETER:
                return "Thermometer";
            case BluetoothClass.Device.HEALTH_UNCATEGORIZED:
                return "Health Uncategorized";
            case BluetoothClass.Device.HEALTH_WEIGHING:
                return "Weighing Device";
            case BluetoothClass.Device.PHONE_CELLULAR:
                return "Cell Phone";
            case BluetoothClass.Device.PHONE_CORDLESS:
                return "Cordless Phone";
            case BluetoothClass.Device.PHONE_ISDN:
                return "ISDN Phone";
            case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
                return "Modem";
            case BluetoothClass.Device.PHONE_SMART:
                return "Smartphone";
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                return "Uncategorized Phone";
            case BluetoothClass.Device.TOY_CONTROLLER:
                return "Toy Controller";
            case BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE:
                return "Action Figure";
            case BluetoothClass.Device.TOY_GAME:
                return "Game Toy";
            case BluetoothClass.Device.TOY_ROBOT:
                return "Robot";
            case BluetoothClass.Device.TOY_UNCATEGORIZED:
                return "Uncategorized Toy";
            case BluetoothClass.Device.TOY_VEHICLE:
                return "Toy Vehicle";
            case BluetoothClass.Device.WEARABLE_GLASSES:
                return "Wearable Glasses";
            case BluetoothClass.Device.WEARABLE_HELMET:
                return "Wearable helmet";
            case BluetoothClass.Device.WEARABLE_JACKET:
                return "Wearable Jacket";
            case BluetoothClass.Device.WEARABLE_PAGER:
                return "Wearable Pager";
            case BluetoothClass.Device.WEARABLE_UNCATEGORIZED:
                return "Wearable Uncategorized";
            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                return "Wearable Wrist Watch";
        }
        return "Unknown";
    }
}
