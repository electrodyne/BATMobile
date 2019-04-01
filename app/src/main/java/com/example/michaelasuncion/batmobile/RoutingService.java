package com.example.michaelasuncion.batmobile;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

//import org.parceler.Parcels;

import static android.content.ContentValues.TAG;

public class RoutingService extends Service {
    SQLiteDatabase RoutingTable;

    Thread WorkingThread;
    private int PacketID;   //for debug only
    String OGM_address;
    Boolean mIsHost = false;
    Packet packet;
    public static final String ACTION_ROUTE_TO_MSG_SRVC = "com.example.michaelasuncion.batmobile.ACTION_ROUTE_TO_MSG_SRVC";

    /* for BCR */
    RoutingBCR mRoutingBCR;
    IntentFilter mRoutingBCRIFilter;

    @Override
    public void onCreate() {
        RoutingTable = openOrCreateDatabase("routing_table", MODE_PRIVATE, null);
        //RoutingTable.delete("route", null, null); //to refresh database every run. FOR DEBUG ONLY.
        //RoutingTable.execSQL("CREATE TABLE IF NOT EXISTS route(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, destination_address VARCHAR NOT NULL, next_hop_address VARCHAR NOT NULL, ttl VARCHAR NOT NULL);");
        RoutingTable.execSQL("CREATE TABLE IF NOT EXISTS route(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, phone_number VARCHAR NOT NULL);");
        PacketID = 0;
        mRoutingBCR = new RoutingBCR();
        mRoutingBCRIFilter = new IntentFilter();
        mRoutingBCRIFilter.addAction("TO_ROUTING");
        registerReceiver(mRoutingBCR, mRoutingBCRIFilter);
        packet = new Packet(OGM_address);   //Register own OGM address.
    }

    @Override
    public void onDestroy() {
        //super.onDestroy();
        unregisterReceiver(mRoutingBCR);
    }

    public class RoutingBCR extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //TODO: Receive the Packet for client..
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*
        * A host must literate it's Group members.
        * so send group members the table content.
        * */
        Toast.makeText(this, "Routing Service has started.", Toast.LENGTH_SHORT).show();
        if (mIsHost) {  //Fire discovery thread only for hosts.
            WorkingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) { //discovery packet.
                        try {
                            Thread.sleep(2500);
                            //send discovery packet request to main thread (which will be passed on to FileTransferService
                            Intent localIntent = new Intent();
                            //localIntent.setAction(DISCOVERY_PACKET_REQUEST);
                            //localIntent.putExtra(DISCOVERY_PACKET_REQUEST, PacketID);
                            localIntent.setAction(ACTION_ROUTE_TO_MSG_SRVC);   //TODO: TELL FileTransferService to "SEND_DATA_FROM_ROUTING_SRVC"

                            packet.generateRandomID();
                           // Parcelable wrappedPacket = Parcels.wrap(packet);
                         //   localIntent.putExtra("packet", wrappedPacket); //sends the ogm address, type of packet (SOON)
                            //Send packet..
                            //localIntent.putExtra("HOP_MSG", OGM_address);

                            sendBroadcast(localIntent);


                        } catch (InterruptedException e) {
                            Log.e(TAG, "run: " + e.toString());
                        }
                    }
                }
            });

            //WorkingThread.start();
        }
        return START_STICKY;
    }

    public boolean update(String DestAddress, String HopAddress, String TTL){

        //Search the dest address. If there is a dest address similar to the incoming packet. check TTL.
        //if TTL_of_packet_to_be_inserted > TTL_@database, replace entry. else, disregard packet
        //return TRUE if successfully replaced/inserted else FALSE.

        Cursor selection = RoutingTable.rawQuery("SELECT * from route WHERE destination_address = '"+DestAddress+"'",null );

        int CursorColumnIndex;
        boolean willInsert = false;

        if ( selection.getCount() > 0) {
            selection.moveToFirst();

            for(int i = 0; i < selection.getCount(); i++) {
                //if TTL of incoming packet is Greater than of at the database, replace the content of database.
                CursorColumnIndex = selection.getColumnIndex("ttl");
                if (Integer.parseInt(TTL) > Integer.parseInt(selection.getString(CursorColumnIndex))) {
                    //get ID of the initial data on the database
                    RoutingTable.delete("route","id = ? ", new String[]{selection.getString(selection.getColumnIndex("id"))});
                    willInsert = true;
                }
            }
        } else willInsert = true;

        if ( willInsert ) {
            RoutingTable.execSQL(
                    "INSERT INTO route(destination_address, next_hop_address, ttl)" +
                            "VALUES ('" + DestAddress + "', '" + HopAddress + "', '" + TTL + "' )"

            );
            return true;
        }
        return false;
    }

    public boolean update(String phone_number){

        //Search the dest address. If there is a dest address similar to the incoming packet. check TTL.
        //if TTL_of_packet_to_be_inserted > TTL_@database, replace entry. else, disregard packet
        //return TRUE if successfully replaced/inserted else FALSE.

        Cursor selection = RoutingTable.rawQuery("SELECT * from route WHERE phone_number = '"+phone_number+"'",null );

        int CursorColumnIndex;
        boolean willInsert = false;

        if ( selection.getCount() > 0) {
           /* selection.moveToFirst();

            for(int i = 0; i < selection.getCount(); i++) {
                //if TTL of incoming packet is Greater than of at the database, replace the content of database.
                CursorColumnIndex = selection.getColumnIndex("ttl");
                if (Integer.parseInt(TTL) > Integer.parseInt(selection.getString(CursorColumnIndex))) {
                    //get ID of the initial data on the database
                    RoutingTable.delete("route","id = ? ", new String[]{selection.getString(selection.getColumnIndex("id"))});
                    willInsert = true;
                }
            }*/
           willInsert = false;
        } else willInsert = true;

        if ( willInsert ) {
            RoutingTable.execSQL(
                    "INSERT INTO route(phone_number)" +
                            "VALUES ('" + phone_number + "' )"

            );
            return true;
        }
        return false;
    }

    public boolean findByDestinationAddress(String destAddress) {
        Cursor selection = RoutingTable.rawQuery("SELECT * from route WHERE phone_number = '"+destAddress+"'",null );
        if ( selection. getCount() > 0) return true;
        else return false;
    }

    /*
    public Envelope get(){
        //mEnvelope.set();
        //For get,
        //Store on Routing Service's mEnvelope the requested stuff..
        //What will be the stuff???
        // So Communication service will send to MainActivity the following flags:
        // WIFI_P2P_STATE_CHANGED_ACTION Check to see if Wi-Fi is enabled and notify proper activity

        // WIFI_P2P_THIS_DEVICE_CHANGED_ACTION Indicates that the device's configuration has changed

        return mEnvelope;
    }
    */

    //TO SIMULATE COMMUNICATING BETWEEN SERVICES, SEND ON START OF THIS ACTIVITY, BROADCAST CONTINUES STUFF. ON COMMUNICATION SERVICE PART FIRE A BROADCAST RECEIVER
    //AND WHENEVER COMMUNICATION SERVICE RECEIVES SOMETHING SEND A BROADCAST TO MAIN_ACTIVITY. ACTIVITY SHOULD HAVE A BROADCAST RECEIVER FOR THAT.
    //USING THIS CONCEPT, 2 SERVICES AND AN ACTIVITY IS COMMUNICATING WITH EACH OTHER.






    public class LocalBinder extends Binder {
        RoutingService getService(String OGM_add) {
            OGM_address = OGM_add;

            return RoutingService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    public RoutingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
