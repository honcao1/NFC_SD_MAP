package com.example.myapplication;

public class ListLatLng {
    private String date;
    private String time;
    private double latitude;
    private double longitude;
    private String speed;

    public ListLatLng(String date, String time, double latitude, double longitude, String speed) {
        this.date = date;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
    }

    public String getDate() {
        String mdate = date.substring(0,2)+"/"+date.substring(2,4)+"/"+ "20" +date.substring(4);

        return mdate;
    }

    public String getTime() {
        String mtime=null;
        if (time.length()==7) {
            int hh = Integer.parseInt(time.substring(0, 1)) + 7;
            mtime = hh + ":" + time.substring(1, 3) + ":" + time.substring(3, 5);
        } else if (time.length()==8){
            int hh = Integer.parseInt(time.substring(0, 2)) + 7;
            mtime = hh + ":" + time.substring(2, 4) + ":" + time.substring(4, 6);
        }

        return mtime;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getSpeed() {
        return speed;
    }
}
