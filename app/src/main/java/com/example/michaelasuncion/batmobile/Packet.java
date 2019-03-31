package com.example.michaelasuncion.batmobile;

import android.os.Parcelable;

import org.parceler.Parcel;

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

@Parcel
public class Packet {
    long packetID;
    String ogm_address;
    String destination_address;
    public static final String DISCOVERY_PACKET =  "DISCOVERY_";
    public static final String MESSAGE_PACKET =  "MSSG_";

    public Packet(String OGM) {
        this.ogm_address = OGM;
    }


    /* GETTER */
    public  String getPacketID() {
        return String.valueOf(this.packetID );
    }
    public String getOGMAddress() { return this.ogm_address; }

    public String getDestinationAddress() { return this.destination_address; }

    /* SETTER */

    public void setOGMAddress(String ogm) { this.ogm_address = ogm; }
    public long generateRandomID() {
        Random rnd = new Random(14343);
        packetID = rnd.nextLong();
        return packetID;
    }
    public void setDestinationAddress(long dest) { this.destination_address = String.valueOf(dest); }

}

