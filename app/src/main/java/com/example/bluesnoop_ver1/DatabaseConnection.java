package com.example.bluesnoop_ver1;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class DatabaseConnection {

    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    public HashSet<String> getWhiteList() {
        return whiteList;
    }

    private HashSet<String> whiteList;
    public DatabaseConnection()
    {
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword("jenninpa@tcd.ie", "jenninpa");
        whiteList = new HashSet<>();
        ReadWhitelist();
    }

    private void ReadWhitelist() {
        DatabaseReference ref = database.getReference().child("whitelist");
        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        HashMap<String, Map<String, String>> list = (HashMap<String, Map<String, String>>) dataSnapshot.getValue();
                        Iterator iterator = list.entrySet().iterator();
                        while(iterator.hasNext()) {
                            Map.Entry<String, Map> entry = (Map.Entry<String, Map>) iterator.next();
                            String temp = String.valueOf(entry.getValue().get("macAddress"));
                            whiteList.add(temp);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });
    }
    private boolean duplicateMac(String newMac){
        return whiteList.contains(newMac);
    }
    public void addToWhiteList(String macAddress){
        if(upgradeWhiteList(macAddress)){
            System.out.println("Successfully");
        }else{
            System.out.println("Failed");
        }

    }

    public void uploadData(ArrayList<DeviceItem> list,String scannerMac){
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        ArrayList<DeviceItem> inWhiteList = new ArrayList<DeviceItem>();
        for (DeviceItem item: list){
            if(whiteList.contains(item.getAddress())){
                inWhiteList.add(item);
            }
        }
        list.clear();
        uploading(inWhiteList,scannerMac,cal.getTimeInMillis());
    }

    private void uploading(final ArrayList<DeviceItem> inWhiteList, String scannerMac,long startOfDay){
        DatabaseReference ref;
        HashSet<String> devices = new HashSet<String>();
        for (DeviceItem item:inWhiteList) {
            devices.add(item.getAddress());
        }
        ArrayList<DeviceItem> instancesOfDevice = new ArrayList<DeviceItem>();
        for(String s:devices) {
            for (int i = 0; i < inWhiteList.size(); i++) {
                if(inWhiteList.get(i).getAddress().equals(s)){
                    instancesOfDevice.add(inWhiteList.get(i));
                    inWhiteList.remove(i);
                }
            }
            ref = database.getReference().child("data").child(scannerMac).child(String.valueOf(startOfDay)).child(s);
            ref.child("type").setValue(instancesOfDevice.get(0).getType());
            ref=ref.child("instance");
            for(DeviceItem item:instancesOfDevice){
                String key =ref.child(s).push().getKey();
                Map<String, String> a = new HashMap<String, String>();
                a.put("RSSI",String.valueOf(item.getRssi()));
                a.put("Duration",String.valueOf(item.getDurtion()));
                a.put("Time",String.valueOf(item.getTime()));
                a.put("Longitude",String.valueOf(item.getLon()));
                a.put("Latitude",String.valueOf(item.getLat()));
                ref.child(key).setValue(a);
                a.clear();
            }
            instancesOfDevice.clear();
        }
    }
    public void UploadWeightGraph(String scannerMac,final connectionGraph graph){
      uploadingWeightGraph(scannerMac,graph);
    }
    private void uploadingWeightGraph(String scannerMac,final connectionGraph graph){
        DatabaseReference ref;
        for (int i =0;i<graph.connectionNodeArrayList.size();i++){
            ref=database.getReference().child("weightGraph").child(scannerMac).child(graph.connectionNodeArrayList.get(i).getEndAddress()).child("weight");
            final int weight = graph.connectionNodeArrayList.get(i).getWeight();
            ref.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if (mutableData.getValue() == null) {
                        mutableData.setValue(1);
                    } else {
                        int count = mutableData.getValue(Integer.class);
                        mutableData.setValue(count + weight);
                    }
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean success, DataSnapshot dataSnapshot) {
                    // Analyse databaseError for any error during increment
                }
            });

            }

        }


    private boolean upgradeWhiteList(String macAddress){
        DatabaseReference ref = database.getReference().child("whitelist");
        if(!duplicateMac(macAddress)){
            String key =ref.child("whitelist").push().getKey();
            Map<String, String> a = new HashMap<String, String>();
            a.put("macAddress",macAddress);
            ref.child(key).setValue(a);
            whiteList.add(macAddress);
            return true;
        }
        else
            {
            return false;
        }
    }
}
