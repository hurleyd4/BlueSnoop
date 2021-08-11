package com.example.bluesnoop_ver1;

import java.util.ArrayList;

public class connectionGraph {
    ArrayList< connectionNode> connectionNodeArrayList = new  ArrayList< connectionNode>();
    int size;

    connectionGraph() {
        this.size =0;
    }

    public void addNode(String startAddress, String endAddress)
    {
        connectionNodeArrayList.add( new connectionNode(startAddress, endAddress));
        size++;
    }

    public void increseWeighting(String endAddress )
    {
        connectionNode currentNode = get(endAddress);
        if(currentNode != null )
        {
            currentNode.increaseWeight();
        }
    }

    public connectionNode get(String endAddress )
    {
        connectionNode returnNode = null;
        for( int index =0; index < size; index++)
        {
            connectionNode testNode = connectionNodeArrayList.get(index);
            if(testNode.getEndAddress().equals(endAddress))
            {
                returnNode = testNode;
            }
        }
        return returnNode;
    }
}
