package com.example.michaelasuncion.batmobile;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Home extends AppCompatActivity{
    Dialog dlg;
    WifiManager wifimanager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    private GUI_Receiver rec;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] device_names;
    WifiP2pDevice[] device_array;
    FileTransferService ftservice;
    boolean isBound = false;
    boolean isHost = false;
    boolean fts_started = false;
    RoutingService routingService;

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    //note this part; onPause() is the state of an app if a new activity is on the foreground
    //could become a problem later on
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;

            if (info.groupFormed && info.isGroupOwner) {

                isHost = true;
                TelephonyManager tmg = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                if (ContextCompat.checkSelfPermission(Home.this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Home.this,
                            new String[] {Manifest.permission.READ_PHONE_STATE}, 1);
                }
                else{
                    routingService.update(tmg.getLine1Number()); //TODO: ADD HOST's OWN ADDRESS.
                    ftservice.set_mnumber(tmg.getLine1Number());
                }

                if(isBound && !fts_started) {
                    mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                        @Override
                        public void onGroupInfoAvailable(WifiP2pGroup group) {
                            String passphrase = group.getPassphrase();
                            show_toast("You are a host\nPassword is " + passphrase);
                        }
                    });
                    ftservice.set_socket_host();

                    fts_started = true;
                    //TODO: 1st event. Master is defined. (start routing service.) (disable discovery loop)
                    //TODO: 2nd event. Master knows it's clients
                    //TODO: register each of the client to Routing database
                    //TODO: Use "ACK" message on FTS (where client numbers are being registered) as an event
                    //-----ON this case all clients being registered are recorded on the database.
                    //TODO: catch all messages coming from client/s ( because clients only knows you! (as a GO) )
                    //TODO: route them accordingly through the ROUTING Table.
                    //TODO: On Routing table, boolean findByDestinationAddress(String address) , if TRUE, send SENT to sender and Message to RECEIVER.

                }
                else if(!isBound)
                    show_toast("Service is not bound");

            } else if (info.groupFormed) {
                //show_toast("You are a client");
                if(isBound) {
                    TelephonyManager tmg = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                    if (ContextCompat.checkSelfPermission(Home.this, Manifest.permission.READ_PHONE_STATE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(Home.this,
                                new String[] {Manifest.permission.READ_PHONE_STATE}, 1);
                    }
                    else{
                        ftservice.set_mnumber(tmg.getLine1Number());
                    }

                    ftservice.set_socket_client(groupOwnerAddress);
                }
                else
                    show_toast("Client socket failed");
            }
        }
    };

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                //update list; does nothing in backend
                device_names = new String[peerList.getDeviceList().size()];
                device_array = new WifiP2pDevice[peerList.getDeviceList().size()];
                int i = 0;

                for(WifiP2pDevice device : peerList.getDeviceList()) {
                    device_names[i] = device.deviceName;
                    device_array[i] = device;
                    i++;
                }
                compare_connections(device_names.length - 1);

                //update list
                /*if(device_names.length > 0) {
                    StringBuilder device_list = new StringBuilder();

                    for (i = 0; i < device_names.length; i++)
                        device_list.append(device_names[i] + '\n');

                    device_list.append("Device List");
                    String dev = device_list.toString();

                    show_toast(dev);
                }*/
                //update list ends here; code is useless and can be removed

                /*if(isBound){
                    ftservice.send_broadcast("MULTIGROUP_START");
                }*/
            }
            if(peers.size()==0)
                show_toast("No Devices Found");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Intent i = new Intent(this, FileTransferService.class);
        bindService(i,mConnection,Context.BIND_AUTO_CREATE);

        Intent iRouting = new Intent(Home.this, RoutingService.class);
        bindService(iRouting, mRoutingServiceConn, Context.BIND_AUTO_CREATE);

        final String[] PERMISSIONS = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.INTERNET
        };

        //Intent testing, do not remove
        rec = new GUI_Receiver(new Handler());
        IntentFilter oq = new IntentFilter();
        oq.addAction("UPDATE_GUI");
        oq.addAction("SHOW_TOAST");
        oq.addAction("COMP_DEV");
        registerReceiver(rec, oq);
        sendBroadcast(new Intent("UPDATE_GUI"));


        //initialization
        dlg = new Dialog(this);

        //create runnable for permission asking
        Runnable r = new Runnable() {
            public void run() {
                if(!hasPermissions(Home.this, PERMISSIONS)){
                    ActivityCompat.requestPermissions(Home.this, PERMISSIONS, 1);
                }
            }
        };

        Thread thread = new Thread(r);
        thread.start();

        //thread = new Thread(this);
        //thread.start();

        wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifimanager.isWifiEnabled())
            wifimanager.setWifiEnabled(true);

        //WiFi Direct related shit
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);


        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //show_toast("Discovery Started");
            }

            @Override
            public void onFailure(int reason) {
                //show_toast("Discovery failed to begin");
            }
        });

        //startService(iRouting); //IAN: Disable for a while... Use only for discovery.
    }

    public void ShowPopUp(View v){
        dlg.setContentView(R.layout.popupmenu);
        dlg.show();
    }

    public void Show_IO_Test(View v){
        dlg.setContentView(R.layout.io_test);
        dlg.show();

        Button create_text = (Button) dlg.findViewById(R.id.create_button);
        create_text.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                EditText test_text = (EditText)dlg.findViewById(R.id.input_text_test);
                String test = test_text.getText().toString();
                //String filename = String.valueOf(System.currentTimeMillis()) + ".txt";
                FileOutputStream fo;

                String phone_number;
                TelephonyManager tmg = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                if (ContextCompat.checkSelfPermission(Home.this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Home.this,
                            new String[] {Manifest.permission.READ_PHONE_STATE}, 1);
                }
                //else{
                    phone_number = "Phone Number is 0" + tmg.getLine1Number();
                    String filename = "MSSG_" + tmg.getLine1Number() + "_" + tmg.getLine1Number() +"_" + String.valueOf(System.currentTimeMillis()) + ".txt";
                //}


                //for testing purposes; will show input string
                try{
                    //create file v1
                    fo = openFileOutput(filename, Context.MODE_PRIVATE);
                    fo.write(test.getBytes());
                    fo.close();

                }catch(Exception e){
                    e.printStackTrace();
                }

                Intent i = new Intent();
                i.setAction("UPDATE_GUI");
                sendBroadcast(i);

                dlg.dismiss();
            }
        });
    }

    public void New_Messg(View v){
        dlg.setContentView(R.layout.send_message);
        dlg.show();

        Button create_text = (Button) dlg.findViewById(R.id.send_messg_button);
        create_text.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                EditText send_to = (EditText)dlg.findViewById(R.id.phone_number_input);
                EditText message = (EditText)dlg.findViewById(R.id.message_input);
                String test = message.getText().toString();
                String send_to_string = send_to.getText().toString();
                String number_and_test = send_to_string + ' ' + test;

                //remove below if not necessary
                //send_to_string = "Sending to... " + send_to_string;

                TelephonyManager tmg = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                if (ContextCompat.checkSelfPermission(Home.this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Home.this,
                            new String[] {Manifest.permission.READ_PHONE_STATE}, 1);
                }
                //remove below if not necessary
                test = "MSSG_" + ftservice.mnumber + "_" + send_to_string + "_" +test; //TODO: NEW MESSAGE FORMAT: MSSG_[my number]_[Destination number]_[message]
                if(test.length() > 1024)
                    show_toast("String length > 1024");
                else{
                    ftservice.sendmessage(Long.parseLong(send_to_string),test,false,tmg.getLine1Number());
                    dlg.dismiss();
                }

            }
        });
    }

    /*public void MessageTest(View v){
        dlg.setContentView(R.layout.getmessage);
        dlg.show();
    }*/

    public void show_toast(String text){
        Toast gathered_string = Toast.makeText(Home.this,text,Toast.LENGTH_LONG);
        gathered_string.setGravity(Gravity.CENTER,0,450);
        gathered_string.show();
    }


    public class GUI_Receiver extends BroadcastReceiver{

        private final Handler GUI_update;

        public GUI_Receiver(Handler handler){
            GUI_update = handler;
        }

        @Override
        public void onReceive(Context context, final Intent intent) {
            if(intent.getAction().equals("UPDATE_GUI")){
                GUI_update.post(new Runnable() {
                    @Override
                    public void run() {
                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View v = inflater.inflate(R.layout.activity_home,null);
                        LinearLayout inside = (LinearLayout) v.findViewById(R.id.inside_ll);

                        String[] filenames = fileList();

                        for(int i = 0; i < filenames.length; i++){
                            try {
                                FileInputStream file = openFileInput(filenames[i]);
                                InputStreamReader is = new InputStreamReader(file);
                                BufferedReader br = new BufferedReader(is);
                                StringBuilder sb = new StringBuilder();
                                String content;
                                while((content = br.readLine()) != null){
                                    sb.append(content + "\n");
                                }
                                content = sb.toString();

                                file.close();
                                is.close();
                                br.close();

                                LinearLayout l = new LinearLayout(Home.this);
                                LinearLayout pad = new LinearLayout(Home.this);
                                LinearLayout.LayoutParams param_pad = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,40);
                                pad.setBackgroundColor(Color.parseColor("#373737"));
                                pad.setLayoutParams(param_pad);
                                pad.setOrientation(LinearLayout.VERTICAL);


                                l.setOrientation(LinearLayout.VERTICAL);
                                l.setPadding(10,10,10,10);

                                TextView tv1 = new TextView(Home.this);
                                TextView tv2 = new TextView(Home.this);

                                tv1.setText(filenames[i] + "\n");
                                tv1.setTextColor(Color.WHITE);
                                tv1.setPadding(10,20,10,10);
                                tv2.setText(content);
                                tv2.setTextColor(Color.WHITE);
                                tv2.setPadding(10,0,10,0);

                                l.addView(tv1);
                                l.addView(tv2);

                                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                p1.gravity = Gravity.CENTER_HORIZONTAL;
                                l.setBackgroundColor(Color.parseColor("#5e5e5e"));
                                l.setPadding(10,20,10,20);
                                l.setLayoutParams(p1);

                                inside.addView(l);
                                inside.addView(pad);
                                setContentView(v);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });
            }
            else if(intent.getAction().equals("SHOW_TOAST")){
                show_toast(intent.getStringExtra("toast_name"));
            }
            else if(intent.getAction().equals("COMP_DEV")){
                compare_connections(device_names.length - 1);
            }
        }
    }

    public void compare_connections(int peer_size){
        if(isBound){
            int num_of_connections = ftservice.active_connections();
            //show_toast("Delta\n peer_size = " + peer_size + "\n # of connections = " + num_of_connections);
            /*if(isHost && (peer_size >= num_of_connections) && (peer_size != 0)){
                ftservice.send_broadcast("MULTIGROUP_START");
            }*/
            if(!isHost && (peer_size >= num_of_connections) && (peer_size != 0)){
                ftservice.set_socket_client_multigroup();
            }
        }
        else{
            show_toast("Delta but not bound");
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileTransferService.LocalBinder b = (FileTransferService.LocalBinder) service;
            ftservice = b.getservice(routingService);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private ServiceConnection mRoutingServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RoutingService.LocalBinder b = (RoutingService.LocalBinder) service;

            //Transfer OGM address to routing service.
            TelephonyManager tmg = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            if (ContextCompat.checkSelfPermission(Home.this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Home.this,
                        new String[] {Manifest.permission.READ_PHONE_STATE}, 1);
            } else routingService = b.getService(tmg.getLine1Number());

            //isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //isBound = false;
        }
    };
}