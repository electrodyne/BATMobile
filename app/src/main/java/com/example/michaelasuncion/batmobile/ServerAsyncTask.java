package com.example.michaelasuncion.batmobile;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerAsyncTask extends AsyncTask {

    private Context context;
    private Home ma;

    public ServerAsyncTask(Context context, Home ma){
        this.context = context;
        this.ma = ma;
    }

    @Override
    protected String doInBackground(Object[] objects) {
        try{
            /*
            Creates a blocking (ServerSocket.accept()) until connection is received
             */
            ServerSocket serverSocket = new ServerSocket(2000);
            Socket client = serverSocket.accept();

            /*
            If this code is reached, connection has occured
             */
            String current_time = String.valueOf(System.currentTimeMillis());
            InputStream is = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;

            while((line = br.readLine()) != null);

            if(line.length() > 0){
                FileOutputStream fo = context.openFileOutput(current_time, Context.MODE_PRIVATE);
                fo.write(line.getBytes());
                fo.close();
                context.sendBroadcast(new Intent("UPDATE_GUI"));
            }else{
                ma.show_toast("Line length > 0");
                return null;
            }
            return current_time;

        }catch (IOException e){
            Log.e("BATMOBILE",e.getMessage());
            return null;
        }
    }
}
