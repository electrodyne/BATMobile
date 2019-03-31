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

import org.parceler.Parcels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class FileTransferService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private final String TAG = "BATMOBILE";
    Socket[] socket = new Socket[14];
    ServerSocket serverSocket;

    BufferedReader[] in = new BufferedReader[14];
    PrintWriter[] out = new PrintWriter[14];

    Intent test;
    boolean[] port_status = new boolean[7];
    boolean[] port_activated = new boolean[7];
    long[] phone_number_list = new long[14];    //Check if we can use this just set this as static. to be able to access outside
    public String mnumber;
    boolean multi_group_socket_running = false;
    boolean[] ip_addr_status = new boolean[256];

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
                //TODO: send to clients.. packet format: DISCOVERY_[PACKETID]_[OTHER CLIENT's ADDRESS]_[DestinationAddress]
                //destination address can be from phone number array.
                //send_broadcast(Packet.DISCOVERY_PACKET + );
                //sendmessage((long) p,"SYN_" + mnumber,true,mnumber);
                Packet packet = Parcels.unwrap(intent.getParcelableExtra("packet"));
                for(int f = 0; f < 7; f++){
                    if(port_status[f] && port_activated[f]) {
                        for (int g = 0; g < 7; g++) {
                            if(port_status[g] && port_activated[g] && g != f) {
                                sendmessage(phone_number_list[f], Packet.DISCOVERY_PACKET + "_" + packet.getPacketID()+ "_" + String.valueOf(phone_number_list[g]) + "_" + String.valueOf(phone_number_list[f]), false, packet.getOGMAddress());
                            }
                        }
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
                    if(x < 2530){
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
        FileTransferService getservice(){
            return FileTransferService.this;
        }
    }

    public void sendmessage(long num_receiver,String s, boolean port, String num_sender){
        int f;
        for(f = 0; f < 14; f++){
            if(phone_number_list[f] == num_receiver)
                break;
        }
        if(f >= 14){
            Intent i = new Intent();
            i.setAction("SHOW_TOAST");
            i.putExtra("toast_name","Number does not exist");
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
                    if(dummy[0].equals("SYN")){ //Makareceive lng nito is HOST. and [x] is a fixed value depending on port of client. therefore phone_number_list is legit.
                        phone_number_list[x] = Long.valueOf(dummy[1]);
                        //sendmessage((long) x, "ACK_"+mnumber,true,mnumber);
                        Runnable y = new sendmessage_thread(x,"ACK_" + mnumber);
                        new Thread(y).start();

                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name","SYN message: "+ parsed);
                        sendBroadcast(test);
                    }else if(dummy[0].equals("ACK")){
                        phone_number_list[x] = Long.valueOf(dummy[1]);

                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name","ACK message: "+ parsed);
                        sendBroadcast(test);
                    }else if(dummy[0].equals("MSSG")){
                        test = new Intent();
                        test.setAction("SHOW_TOAST");
                        test.putExtra("toast_name","Incoming message: "+ dummy[1]);
                        sendBroadcast(test);
                    }else if(dummy[0].equals("DISCOVERY")){
                        //TODO: (Client side) parse the data important: Destination address. If = to my number (soon)

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
