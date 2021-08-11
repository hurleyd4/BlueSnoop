package com.example.bluesnoop_ver1;

import java.util.Date;

class DeviceItem {
    private String address;
    private int rssi;
    private String type;
    private long time;
    private long duration;
    long timeStamp;
    double lon;
    double lat;


    public DeviceItem(String address, int rssi, String type,
                      long duration, long timeStamp, double lon, double lat ) {
        this.address = address;
        this.rssi = rssi;
        this.type = type;
        this.time = new Date().getTime()*1000;
        this.duration = duration;
        this.timeStamp = timeStamp;
        this. lon = lon;
        this.lat = lat;
    }



    public String getAddress()  { return this.address;        }
    public int getRssi()        {   return this.rssi;         }
    public String getType()        {   return this.type;         }
    public long getTime()       {   return this.time;         }
    public long getDurtion()    {   return this.duration;     }
    public long getTimeStamp()  {   return this.timeStamp;    }
    public double getLat()        {   return this.lat;    }
    public double getLon()        {   return this.lon;    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    //This method is called for the title of the listview boxes. ALter this to change the title.
    @Override
    public String toString() { return getAddress();}
}






