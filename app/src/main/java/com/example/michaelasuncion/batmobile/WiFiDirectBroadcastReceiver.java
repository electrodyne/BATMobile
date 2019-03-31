package com.example.michaelasuncion.batmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private Home ma;

    public WiFiDirectBroadcastReceiver(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel mChannel, Home ma){
        this.wifiP2pManager = wifiP2pManager;
        this.mChannel = mChannel;
        this.ma = ma;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            //Check to see if Wi-Fi is enabled and notify proper activity
            //Indicates whether WiFi P2P is enabled

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            //if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){} (wifi is enabled
            //if(state != WifiP2pManager.WIFI_P2P_STATE_ENABLED){

            //}

        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            //Call WifiP2pManager.requestPeers() to get a list of current peers
            //Indicates that the available peer list has changed

            if(wifiP2pManager!=null){
                wifiP2pManager.requestPeers(mChannel,ma.peerListListener);
            }

        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            //Respond to new connection or disconnections
            //Indicates that the state of WiFi P2P connectivity has changed
            if(wifiP2pManager==null)
                return;

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected()){
                wifiP2pManager.requestConnectionInfo(mChannel,ma.connectionInfoListener);
            }else{
                ma.show_toast("No Connection");
            }

        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            //Respond to this device's Wi-Fi state changing
            //Indicates that the device's configuration has changed
        }
    }
}
