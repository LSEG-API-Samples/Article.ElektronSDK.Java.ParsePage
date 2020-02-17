///*|----------------------------------------------------------------------------------------------------
// *|            This source code is provided under the Apache 2.0 license      	--
// *|  and is provided AS IS with no warranty or guarantee of fit for purpose.  --
// *|                See the project's LICENSE.md for details.                  					--
// *|           Copyright (C) 2019 Refinitiv. All rights reserved.            		--
///*|----------------------------------------------------------------------------------------------------

package com.thomsonreuters.ema.examples.training.consumer.series100.example120__MarketPrice__FieldListWalk;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import com.thomsonreuters.ema.access.FieldEntry;
import com.thomsonreuters.ema.access.Msg;
import com.thomsonreuters.ema.access.AckMsg;

import com.thomsonreuters.ema.access.GenericMsg;
import com.thomsonreuters.ema.access.RefreshMsg;
import com.thomsonreuters.ema.access.StatusMsg;
import com.thomsonreuters.ema.access.UpdateMsg;
import com.thomsonreuters.ema.access.DataType;

import com.thomsonreuters.ema.access.EmaFactory;
import com.thomsonreuters.ema.access.FieldList;
import com.thomsonreuters.ema.access.OmmConsumer;
import com.thomsonreuters.ema.access.OmmConsumerClient;
import com.thomsonreuters.ema.access.OmmConsumerEvent;
import com.thomsonreuters.ema.access.OmmException;
//for partial update
import java.util.TreeMap;
import com.thomsonreuters.ema.access.RmtesBuffer;
import com.thomsonreuters.ema.access.Data;
import com.thomsonreuters.ema.access.DataType.DataTypes;

class AppClient implements OmmConsumerClient
{
	//a Map keeps field Id as a key with RmtesBuffer
    TreeMap <Integer, RmtesBuffer> pageMap = new TreeMap <Integer, RmtesBuffer>();
	public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event)
	{
		if (refreshMsg.hasName())
			System.out.println("Item Name: " + refreshMsg.name());
		
		if (refreshMsg.hasServiceName())
			System.out.println("Service Name: " + refreshMsg.serviceName());
		
		System.out.println("Item State: " + refreshMsg.state());
		
		if (DataType.DataTypes.FIELD_LIST == refreshMsg.payload().dataType())
			decode(refreshMsg.payload().fieldList());
		
		System.out.println("\n");
	}
	
	public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) 
	{
		if (updateMsg.hasName())
			System.out.println("Item Name: " + updateMsg.name());
		
		if (updateMsg.hasServiceName())
			System.out.println("Service Name: " + updateMsg.serviceName());
		
		if (DataType.DataTypes.FIELD_LIST == updateMsg.payload().dataType())
			decode(updateMsg.payload().fieldList());
		
		System.out.println("\n");
	}

	public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event) 
	{
		if (statusMsg.hasName())
			System.out.println("Item Name: " + statusMsg.name());
		
		if (statusMsg.hasServiceName())
			System.out.println("Service Name: " + statusMsg.serviceName());
		
		if (statusMsg.hasState())
			System.out.println("Service State: " + statusMsg.state());
		
		System.out.println("\n");
	}

	
	public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent consumerEvent){}
	public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent consumerEvent){}
	public void onAllMsg(Msg msg, OmmConsumerEvent consumerEvent){}

								
	void decode(FieldList fieldList)
	{
		Iterator<FieldEntry> iter = fieldList.iterator();
		FieldEntry fieldEntry;
		while (iter.hasNext())
		{
			fieldEntry = iter.next();
			//page 64x14 or 80x25
			//TBD Page x
			if( (fieldEntry.fieldId() >= 215 && fieldEntry.fieldId() <= 228) || 
				(fieldEntry.fieldId() >= 315 && fieldEntry.fieldId() <= 339) ||
				(fieldEntry.fieldId() >= 1359 && fieldEntry.fieldId() <= 1378)) {	
                  //if the field id does not exist in the map, create new RmtesBuffer object
                  if(!pageMap.containsKey(fieldEntry.fieldId())) {
                	  pageMap.put(fieldEntry.fieldId(), EmaFactory.createRmtesBuffer());
                  }
                  //call apply() to interpret the full update and the partial update
                  pageMap.get(fieldEntry.fieldId()).apply(fieldEntry.rmtes());
            }
		}
		System.out.println("=================================================================================================================");
		//prints all page fields in the map on the console to display the page
		try {
		//BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Consumer"), "UTF16"));
        for (Integer fieldId: pageMap.keySet()){
        	
        	byte[] utf16Bytes=pageMap.get(fieldId).toString().getBytes("UTF16");
           System.out.println((new String(utf16Bytes, Charset.forName("UTF-16"))));
          //  writer.append((new String(utf16Bytes, Charset.forName("UTF-16"))));
        }
        //writer.close();
		}catch(Exception e) {
			e.printStackTrace();
        }
		
	}

}

public class Consumer 
{
	public static void main(String[] args)
	{
		OmmConsumer consumer = null;
		try
		{
			AppClient appClient = new AppClient();
			
			consumer  = EmaFactory.createOmmConsumer(EmaFactory.createOmmConsumerConfig().host("192.168.27.48:14002").username("user"));
			
			consumer.registerClient( EmaFactory.createReqMsg().serviceName("API_ELEKTRON_EDGE_TOKYO").name("FXFX"), appClient, 0);
			
			Thread.sleep(30000);			// API calls onRefreshMsg(), onUpdateMsg() and onStatusMsg()
		}
		catch (InterruptedException | OmmException excp)
		{
			System.out.println(excp.getMessage());
		}
		finally 
		{
			if (consumer != null) consumer.uninitialize();
		}
	}
}


