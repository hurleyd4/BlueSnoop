package com.example.bluesnoop_ver1;

public class connectionNode {
    private String startAddress;
    private String endAddress;
    private int weight;
    connectionNode( String startAddress, String endAddress)
    {
       this.startAddress = startAddress;
       this.endAddress = endAddress;
       weight =1;
    }

    public void increaseWeight() {
        weight++;
    }
    public String getEndAddress(){
        return this.endAddress;
    }
    public int getWeight(){
        return this.weight;
    }


}
