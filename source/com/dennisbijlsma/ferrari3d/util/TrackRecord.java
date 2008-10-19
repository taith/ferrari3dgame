//--------------------------------------------------------------------------------
// Ferrari3D
// TrackRecord
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.dennisbijlsma.util.data.DataLoader;
import com.dennisbijlsma.util.xml.XMLParser;

/**
 * Stores a fastest lap for the specified track. The name of this class can be
 * misleading, as it doesn't have to be the all-time fastest lap that is being
 * stored.
 */

public class TrackRecord {
	
	private String driverName;
	private String carName;
	private String circuitName;
	private Laptime laptime;
	private String version;
	private String date;
	
	
	
	public TrackRecord(String driver,String car,String circuit,Laptime laptime,String version,String date) {

		this.driverName=driver;
		this.carName=car;
		this.circuitName=circuit;
		this.laptime=laptime;
		this.version=version;
		this.date=date;
	}
	
	
	
	public String getDriverName() { return driverName; }
	public String getCarName() { return carName; }
	public String getCircuitName() { return circuitName; }
	public Laptime getLaptime() { return laptime; }
	public String getVersion() { return version; }
	public String getDate() { return date; }

	
	
	public static void setRecords(TrackRecord record) throws Exception {
		
		HashMap<String,String> parameters=new HashMap<String,String>();
		parameters.put("driverName",record.getDriverName());
		parameters.put("carName",record.getCarName());
		parameters.put("circuitName",record.getCircuitName());
		parameters.put("laptime",""+record.getLaptime().getLaptime());
		parameters.put("version",record.getVersion());
		parameters.put("date",record.getDate());
		
		String xml=DataLoader.openURL(Settings.SAVE_RECORDS_URL,parameters,"UTF-8");
		Document document=XMLParser.parseXML(xml);
	}
	
	
	
	public static TrackRecord[] getRecords() throws Exception {
	
		String xml=DataLoader.openURL(new URL(Settings.LOAD_RECORDS_URL),"UTF-8");
		Document document=XMLParser.parseXML(xml);
		ArrayList<TrackRecord> records=new ArrayList<TrackRecord>();
		
		for (Node i : XMLParser.getChildNodes(document.getDocumentElement())) {
			String driver=XMLParser.getChildValue(i,"driverName");
			String car=XMLParser.getChildValue(i,"carName");
			String circuit=XMLParser.getChildValue(i,"circuitName");
			Laptime laptime=new Laptime(Integer.parseInt(XMLParser.getChildValue(i,"laptime")));
			String version=XMLParser.getChildValue(i,"version");
			String date=XMLParser.getChildValue(i,"date");
			
			TrackRecord record=new TrackRecord(driver,car,circuit,laptime,version,date);
			records.add(record);
		}
		
		return records.toArray(new TrackRecord[0]);
	}
}