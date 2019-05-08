package com.example.michaelasuncion.batmobile;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;

//import org.parceler.Parcels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class FileTransferService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private final String TAG = "BATMOBILE";
    Socket[] socket = new Socket[14];
    ServerSocket serverSocket;

    BufferedReader[] in = new BufferedReader[14];
    PrintWriter[] out = new PrintWriter[14];

    //RoutingService mRoutingService;
    Intent test;
    boolean[] port_status = new boolean[7];
    boolean[] port_activated = new boolean[7];
    long[] phone_number_list = new long[14];    //Check if we can use this just set this as static. to be able to access outside
    public static String mnumber;
    boolean multi_group_socket_running = false;
    boolean[] ip_addr_status = new boolean[256];
    public static String gonumber;
    Home home;

    /* IAN EDITS */
    private CommunicationReceiver communicationReceiver;
    IntentFilter mIntentFilter;
    final Handler mHandler = new Handler();

    public FileTransferService() {
        int f;
        for(f = 0; f < 7; f++) {
            port_status[f] = false;
            port_activated[f] = false;
        }
        mnumber = new String();
        for(f = 0; f < 256; f++){
            ip_addr_status[f] = false;
        }
        multi_group_socket_running = false;

        communicationReceiver = new CommunicationReceiver(mHandler);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(RoutingService.ACTION_ROUTE_TO_MSG_SRVC);
    }

    @Override
    public void onCreate() {
        //super.onCreate();
        registerReceiver(communicationReceiver,mIntentFilter);
    }

    @Override
    public void onDestroy() {
        //super.onDestroy();
        unregisterReceiver(communicationReceiver);
    }

    public class CommunicationReceiver extends BroadcastReceiver {
        private final Handler CommsHandler;

        public CommunicationReceiver (Handler handler) {
            CommsHandler = handler; //use only if there is something you want to process. else not needed. -IAN
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(RoutingService.ACTION_ROUTE_TO_MSG_SRVC)){

                //destination address can be from phone number array.
                //send_broadcast(Packet.DISCOVERY_PACKET + );
                //sendmessage((long) p,"SYN_" + mnumber,true,mnumber);
               // Packet packet = Parcels.unwrap(intent.getParcelableExtra("packet"));
                for(int f = 0; f < 7; f++){
                    String tempStr = "";
                    if(port_status[f] && port_activated[f]) {
                        //for #1: DISCOVERY_#2_#3_#1 (last number is destination address)
                        for (int g = 0; g < 7; g++) {
                            if(port_status[g] && port_activated[g] && g != f) {
                                tempStr += phone_number_list[g];
                                tempStr += "_";
                            }
                        }
                        //DISCOVERY_#packetID#_#2_#3_#1
               //         sendmessage(phone_number_list[f], Packet.DISCOVERY_PACKET + packet.getPacketID()+ "_" + tempStr + String.valueOf(phone_number_list[f]), false, packet.getOGMAddress());
                    }
                }

            }


            /*
            Intent mIntent = new Intent();
            mIntent.setAction("TO_ACTIVITY");
            mIntent.putExtra("HOP_MSG", intent.getStringExtra("SRC_MSG"));
            sendBroadcast(mIntent);
            */
        }
    }

    public void set_mnumber(String n){
        mnumber = n;
    }

    private int available_port(){
        int i;
        for(i = 0; i < 7; i++){
            if(!port_status[i])
                return i;
        }
        return -1;
    }

    public void set_socket_host(){
        Runnable r = new set_socket_host_thread();
        new Thread(r).start();
        Runnable s = new set_socket_host_multigroup_thread();
        new Thread(s).start();
    }

    private class set_socket_host_multigroup_thread implements Runnable{
        @Override
        public void run() {
            try{
                serverSocket = new ServerSocket(2094);
            }catch (IOException e){
                e.printStackTrace();
            }
            while(true){
                int port_num = available_port();
                if(port_num == -1){
                    test = new Intent();
                    test.setAction("SHOW_TOAST");
                    test.putExtra("toast_name","No more available ports");
                }
                else{
                    try{
                        port_status[port_num] = true;
                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name", "Multigroup Socket " + port_num + " waiting...");
                        sendBroadcast(test);
                        socket[port_num] = serverSocket.accept();

                        port_activated[port_num] = true;
                        out[port_num] = new PrintWriter(socket[port_num].getOutputStream(), true);
                        in[port_num] = new BufferedReader(new InputStreamReader(socket[port_num].getInputStream()));

                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name", "Multigroup Socket " + port_num + " set-up successfully");
                        sendBroadcast(test);

                        Runnable rec = new recmessage(port_num);
                        new Thread(rec).start();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class set_socket_host_thread implements Runnable{
        @Override
        public void run() {
            try{
                serverSocket = new ServerSocket(2088);
            }catch (IOException e){
                e.printStackTrace();
            }
            while(true){
                int port_num = available_port();
                if(port_num == -1){
                    test = new Intent();
                    test.setAction("SHOW_TOAST");
                    test.putExtra("toast_name","No more available ports");
                }
                else{
                    try{
                        port_status[port_num] = true;
                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name", "Socket " + port_num + " waiting...");
                        sendBroadcast(test);
                        socket[port_num] = serverSocket.accept();

                        port_activated[port_num] = true;
                        out[port_num] = new PrintWriter(socket[port_num].getOutputStream(), true);
                        in[port_num] = new BufferedReader(new InputStreamReader(socket[port_num].getInputStream()));

                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name", "Socket " + port_num + " set-up successfully");
                        sendBroadcast(test);

                        Runnable rec = new recmessage(port_num);
                        new Thread(rec).start();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void set_socket_client(InetAddress IP_addr){
        test = new Intent();
        test.setAction("SHOW_TOAST");
        test.putExtra("toast_name","Socket client initializing...");
        sendBroadcast(test);

        Runnable r = new set_socket_client_thread(IP_addr,0);
        new Thread(r).start();
    }

    private class set_socket_client_thread implements Runnable {
        int x;
        InetAddress IP_addr;

        public set_socket_client_thread(InetAddress ina, int num){
                x = num;
                IP_addr = ina;
        }


        @Override
        public void run() {
            int p = x % 7;
            try{
                socket[p] = new Socket();
                socket[p].connect(new InetSocketAddress(IP_addr,2088),1000);

                out[p] = new PrintWriter(socket[p].getOutputStream(), true);
                in[p] = new BufferedReader(new InputStreamReader(socket[p].getInputStream()));
                port_status[p] = true;
                port_activated[p] = true;

                test.putExtra("toast_name","Client Socket " + socket[p].getLocalPort() + " connected to " + socket[p].getPort());
                sendBroadcast(test);

                Runnable y = new recmessage(p);
                new Thread(y).start();

                sendmessage((long) p,"SYN_" + mnumber,true,mnumber);

                //home.Print_IO("CHECK MNUMBER ", mnumber );
                //get IP address here once connection is established
                InetAddress inetAddress = socket[p].getLocalAddress();
                byte[] bleh = inetAddress.getAddress();
                int pil = bleh[3] & 0xff;
                ip_addr_status[pil] = true;
                ip_addr_status[0] = true;
                ip_addr_status[1] = true;
            }catch(IOException e){
                e.printStackTrace();
                if(x < 35){
                    Runnable s = new set_socket_client_thread(IP_addr,x + 1);
                    new Thread(s).start();
                }
                else{
                    Intent i = new Intent();
                    i.setAction("SHOW_TOAST");
                    i.putExtra("toast_name","No ports available");
                }
            }
        }
    }

    public void set_socket_client_multigroup(){
        if(!multi_group_socket_running){
            Intent tent = new Intent();
            tent.setAction("SHOW_TOAST");
            tent.putExtra("toast_name","Starting multigroup socket");
            sendBroadcast(tent);

            Runnable r = new set_socket_client_multigroup_thread(0);
            new Thread(r).start();
            multi_group_socket_running = true;
        }
    }

    private class set_socket_client_multigroup_thread implements Runnable {
        int x;
        byte[] ip = new byte[4];

        public set_socket_client_multigroup_thread(int num){
            x = num;
            ip[0] = (byte) 192;
            ip[1] = (byte) 168;
            ip[2] = (byte) 49;
            ip[3] = (byte) ((num % 253) + 2);
        }
        @Override
        public void run() {
            int p = available_port();
            if(p == -1){
                test = new Intent();
                test.setAction("SHOW_TOAST");
                test.putExtra("toast_name","No more available ports");
            }
            else if(ip_addr_status[(x % 253) + 2]){
                Runnable s = new set_socket_client_multigroup_thread(x + 1);
                new Thread(s).start();
            }
            else{
                try{
                    InetAddress IP_addr = InetAddress.getByAddress(ip);
                    socket[p] = new Socket();
                    socket[p].connect(new InetSocketAddress(IP_addr,2094),75);

                    out[p] = new PrintWriter(socket[p].getOutputStream(), true);
                    in[p] = new BufferedReader(new InputStreamReader(socket[p].getInputStream()));
                    port_status[p] = true;
                    port_activated[p] = true;

                    test.putExtra("toast_name","Special Socket " + socket[p].getLocalPort() + " connected to " + socket[p].getPort());
                    sendBroadcast(test);

                    Runnable y = new recmessage(p);
                    new Thread(y).start();

                    sendmessage((long) p,"SYN_" + mnumber,true,mnumber);
                }catch(IOException e){
                    e.printStackTrace();
                    if(x < 253){
                        Runnable s = new set_socket_client_multigroup_thread(x + 1);
                        new Thread(s).start();
                    }
                    else{
                        /*Intent i = new Intent();
                        i.setAction("SHOW_TOAST");
                        i.putExtra("toast_name","Multigroup Socket failed\nRetrying...");
                        sendBroadcast(i);*/

                        Runnable k = new set_socket_client_multigroup_thread(0);
                        new Thread(k).start();
                    }
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder{
        FileTransferService getservice(Home mHome){
            home = mHome;
            //mRoutingService = routingService;
            return FileTransferService.this;
        }
    }

    public void sendmessage(long num_receiver,String s, boolean port, String num_sender){

        int f;
        for(f = 0; f < 14; f++){
            if(phone_number_list[f] == num_receiver)
                break;
        }
        int dbcount = Home.routingService.getDBCount();
        Packet tempPacket = new Packet();
        String tempStr = "";
        for( int l = 0; l < dbcount; l++) {

            tempPacket = Home.routingService.getData(l);
            //select 1 until the last and send it.
            tempStr += tempPacket.phone_number;
            tempStr += ",";
            tempStr += tempPacket.direct_connection;
            tempStr += ",";
            tempStr += tempPacket.distance;
            tempStr += "\n";

        }
        home.Print_IO("Routing table (address, relay, distance)", tempStr);
        if(f >= 14){
            Intent i = new Intent();
            i.setAction("SHOW_TOAST");
            i.putExtra("toast_name","Number does not exist");
            //home.Print_IO("Number Does not exist" , );
            sendBroadcast(i);
        }
        if(port){
            Runnable x = new sendmessage_thread((int) num_receiver,s);
            new Thread(x).start();
        }
        else if(f < 14){
            Runnable x = new sendmessage_thread(f,s);
            new Thread(x).start();
        }

    }

    private class sendmessage_thread implements Runnable{
        int x;
        String string;

        public sendmessage_thread(int y, String p){
            x = y;
            string = p;
        }
        @Override
        public void run() {
            out[x].println(string);

            /*Intent test = new Intent();
            test.setAction("SHOW_TOAST");
            test.putExtra("toast_name","sending " + string);
            sendBroadcast(test);*/
        }
    }

    public void send_broadcast(String message){
        int f;
        for(f = 0; f < 7; f++){
            if(port_status[f] && port_activated[f]){
                Runnable x = new sendmessage_thread(f,message);
                new Thread(x).start();
            }
        }
    }

    public int active_connections(){
        int num_active = 0;
        int i;

        for(i = 0; i < 7; i++){
            if(port_activated[i])
                num_active++;
        }
        return num_active;
    }

    private class recmessage implements Runnable{
        int x;

        public recmessage(int y){
            x = y;
        }

        @Override
        public void run() {
            String[] dummy;
            char[] incomingmessage = new char[1024];
            StringBuilder sb = new StringBuilder(1024);
            int num = 0;
            Packet tempPacket = new Packet();
            int tempDistance = 0;
            while(true){
                try{

                    in[x].read(incomingmessage,0,1024);
                    dummy = String.valueOf(incomingmessage).split("\\n");
                    for(num = 0; num < dummy.length - 1; num++) {
                        sb.append(dummy[num]);
                        if(num != dummy.length -2)
                            sb.append('\n');
                    }
                    //test = new Intent();
                    //test.setAction("SHOW_TOAST");
                    //test.putExtra("toast_name","Incoming message: "+ sb.toString());
                    //sendBroadcast(test);

                    String parsed = sb.toString();
                    dummy = parsed.split("_");
                    if(dummy[0].equals("SYN")){
                        //Makareceive lng nito is HOST. and [x] is a fixed value depending on port of client. therefore phone_number_list is legit.
                        //CLIENT lang nagsesend nito.

                        home.startRouting();
                        //Start Routing.

                        phone_number_list[x] = Long.valueOf(dummy[1]);
                        //sendmessage((long) x, "ACK_"+mnumber,true,mnumber);
                        //home.Print_IO("SYN CONTENT (GO SIDE)", dummy[0] + "_" + dummy[1] + "_" + dummy[2]);

                        /*
                        Home.routingService.update(mnumber, "INTER", mnumber, "0");
                        try {
                            Home.routingService.update(dummy[2], dummy[1], mnumber, "0");
                        } catch (Exception e) {
                            home.Print_IO("ERROR", e.toString());
                        }
                        */
                        Runnable y = new sendmessage_thread(x,"ACK_" + mnumber);
                        //send discovery packet also.
                        new Thread(y).start();
                        /*
                        int dbcount = Home.routingService.getDBCount();

                        for( int l = 0; l < dbcount; l++) {

                            tempPacket = Home.routingService.getData(l);
                            //select 1 until the last and send it.
                            send_broadcast("DISCO_" + tempPacket.classification + "_" + tempPacket.phone_number + "_" + tempPacket.direct_connection + "_" + tempPacket.distance);
                        }
                        */

                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                                test.putExtra("toast_name","SYN message: "+ parsed);
                        sendBroadcast(test);
                    }else if(dummy[0].equals("ACK")){   ////Makareceive lng nito is CLIENT.
                        phone_number_list[x] = Long.valueOf(dummy[1]);
                        gonumber = dummy[1];
                        home.startRouting();
                        //home.Print_IO("ACK CONTENT (GM SIDE)", dummy[0] + "_" + dummy[1] + "_" + dummy[2]);

                        /*
                        try{
                            Home.routingService.update(dummy[2], dummy[1], dummy[2], "0");
                        } catch (Exception e) {
                            home.Print_IO("ERROR", e.toString());
                        }

                        */
                                // Register the phon    e number above. dummy[1] is string.
                        //id, phone#
                         // updates the database. returns true if update is successful. //REGISTER EACH CLIENTS.
                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name","ACK message: "+ parsed);
                        sendBroadcast(test);
                    }else if(dummy[0].equals("MSSG")){
                        // @TODO Composition : MSSG_[FROM ADDRESS]_[TO ADDRESS]_[SOURCE NUMBER]_[MESSAGE]
                        if(dummy.length < 5) dummy[4] = " "; // to avoid no message error.
                        //home.Print_IO("Content Of Message", dummy[0] + "_" + dummy[1] + "_" + dummy[2] + "_" + dummy[3] + "_" + dummy[4]);
                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        try {
                            if (dummy[2].equals(mnumber)){
                                home.Print_IO("From " + dummy[3], dummy[4]);
                            }
                            else {
                                // if ( destination address is exsisting in routing table ) resend the message to [TO_ADDRESS]
                                sendmessage(Long.parseLong(Home.routingService.getDirectLineAddress(dummy[2])), "MSSG_" + mnumber + "_" + dummy[2] + "_" + dummy[3] + "_" + dummy[4], false, dummy[1]); //"MSSG_" + mnumber + "_" + dummy[2] + "_" +dummy[3]
                                //test.putExtra("toast_name", "Routed message: " + dummy[3]);
                                home.Print_IO("Routed from " + dummy[1] + " to " + dummy[2], dummy[4]);

                            }
                            sendBroadcast(test);
                        } catch (Exception e){
                           // home.Print_IO("Runtime Error",e.toString() + "parsed: " + parsed);
                        }


                    }else if(dummy[0].equals("DISCO")){
                        //DISCO_[gm1]_[gm1]_1
                            //update table
                            //increment distance + 1
                            //broadcast a disco packet to all sockets DISCO_[gm1]_[go1]_2
                            //reply to gm1: DISCOREC_[gm1]_[go1]_[go1]_1
                        //DISCO_[go2]_[gm1]_2

                        //@TODO DISCO_[phone_number endorsed]_[direct_connection_address]_[distance]

                        try {
                            if (!dummy[1].equals(mnumber)) {
                                tempPacket.phone_number = dummy[1];
                                tempPacket.direct_connection = dummy[2];
                                tempPacket.distance = dummy[3];
                                tempPacket.classification = "INTRA";    //just a placeholder. no use anymore
                                Home.routingService.update(tempPacket);
                                tempDistance = Integer.parseInt(dummy[3]) + 1;
                                send_broadcast("DISCO_" + dummy[1] + "_" + mnumber + "_" + tempDistance);
                                sendmessage(Long.parseLong(Home.routingService.getDirectLineAddress(dummy[1])), "DISCOREC_" + dummy[1] + "_" + mnumber + "_" + mnumber + "_1", false, mnumber); //Instance 1
                            }
                                /*
                            if (!dummy[2].equals(mnumber)) {
                                tempPacket.classification = dummy[1];
                                tempPacket.phone_number = dummy[2];
                                tempPacket.direct_connection = dummy[3];
                                if (gonumber.equals(dummy[2]))
                                    tempPacket.distance = String.valueOf(Integer.parseInt(dummy[4]));
                                else
                                    tempPacket.distance = String.valueOf(Integer.parseInt(dummy[4]) + 1);
                                Home.routingService.update(tempPacket);
                            }
                            */
                        }catch (Exception e) {
                         //   home.Print_IO("Discovery", e.toString());

                        }
                        //check existance of the address on DB.
                        //if existing check the distance.
                        //  if distance.new < distance.old) replace the old with new.
                        //else


                        //parsable data: dummy[0] : DISCOVERY
                        // dummy[1] : neighbor1
                        // dummy[2] : neighbor2
                        // dummy[n] : neighbor[n]
                        // dummy[n+1] : should be my address. loop store until I get my address.
                        //String addressContainer = "";



                    } else if(dummy[0].equals("DISCOREC")){
                        try {
                            //@TODO DISCOREC_[phone_number endorsed]_[last hop device/path]_[responder address]_distance
                            //2 receipeintes: 1-relay, 1-destination
                            //DISCOREC_gm1_go2_go2_1
                            //check the content of dummy[1]. if == to mnumber, update table with [add = responder add, relay=last hop, distance = distance]
                            if (dummy[1].equals(mnumber)) {
                                tempPacket.phone_number = dummy[3];
                                tempPacket.direct_connection = dummy[2];
                                tempPacket.distance = dummy[4];
                                tempPacket.classification = "INTRA";    //just a placeholder. no use anymore
                                Home.routingService.update(tempPacket);
                            } else {
                                tempDistance = Integer.parseInt(dummy[4]) + 1;
                                sendmessage(Long.parseLong(Home.routingService.getDirectLineAddress(dummy[1])), "DISCOREC_" + dummy[1] + "_" + mnumber + "_" + dummy[3] + "_" + tempDistance, false, mnumber);
                            }
                        } catch (Exception e) {
                           // home.Print_IO("Discovery", e.toString());

                        }

                    }
                    /*else if(dummy[0].equals("MULTIGROUP") && dummy[1].equals("START")){
                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name","MULTIGROUP_START Command Received");
                        sendBroadcast(test);

                        Runnable j = new set_socket_client_multigroup_thread(0);
                        new Thread(j).start();
                    }*/
                    sb = new StringBuilder(1024);
                    incomingmessage = new char[1024];
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
