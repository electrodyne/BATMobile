package com.example.michaelasuncion.batmobile;

import android.os.Parcelable;

//import org.parceler.Parcel;
//import org.parceler.ParcelConstructor;
//import org.parceler.ParcelProperty;

import java.util.Random;


//This class shall be used by All types of users. For GO, this will be used for exchanging data from GM to GM or GO
/*
 Contents:
 1.) DISCOVERY_
 2.) PACKET_ID [123123]
 3.) OGM Address [+639229056096]
 4.) Destination address    [FOR GO, all devices on the list]
 5.) Message (optional)
 */

//@Parcel
public class Packet {
    public String distance;
    public String phone_number;
    public String direct_connection;
    public String classification;
}

