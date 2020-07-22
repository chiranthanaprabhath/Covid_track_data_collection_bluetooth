package com.example.covid_track;

public class deviceData {
    public String DeviceName;
    public String BluVersion;
    public String BetteryValue;
    public String Tem;
    public String Mac;
    public String rssi;
    public String output;
    public String BetteryCp;
    public String BetteryValueOwner;
    public String TemOwner;
    public String bleOwner;

    public deviceData(String DeviceName,String BetteryValue,String Tem,String Mac,String rssi,String output,String BetteryCp,String BluVersion,String BetteryValueOwner,String TemOwner,String bleOwner) {
        this.DeviceName=DeviceName;
        this.BetteryValue=BetteryValue;
        this.Tem=Tem;
        this.Mac=Mac;
        this.rssi=rssi;
        this.output=output;
        this.BetteryCp=BetteryCp;
        this.BluVersion=BluVersion;
        this.BetteryValueOwner=BetteryValueOwner;
        this.TemOwner=TemOwner;
        this.bleOwner=bleOwner;
    }
}
