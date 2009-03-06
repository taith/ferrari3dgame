//--------------------------------------------------------------------------------
// Ferrari3D
// TrackRecord
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.dennisbijlsma.util.DataLoader;
import com.dennisbijlsma.util.XMLParser;

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
	
	private static final int MAX_RECORDS=50;
	private static final String CHARSET="UTF-8";
	
	/**
	 * Creates a new track record with the specified fields.
	 */
	
	public TrackRecord(String driver,String car,String circuit,Laptime laptime,String version,String date) {

		this.driverName=driver;
		this.carName=car;
		this.circuitName=circuit;
		this.laptime=laptime;
		this.version=version;
		this.date=date;
	}
	
	
	
	public String getDriverName() { 
		return driverName; 
	}
	
	public String getCarName() { 
		return carName; 
	}
	
	public String getCircuitName() { 
		return circuitName; 
	}
	
	public Laptime getLaptime() { 
		return laptime; 
	}
	
	public String getVersion() { 
		return version; 
	}
	
	public String getDate() { 
		return date; 
	}

	/**
	 * Sends the specified rack record to the server. After this method is called
	 * {@see #getRecords()} should be called to refresh the list of track records.
	 * @throws IOException when a connection to the server could not be made.
	 */
	
	public static void setRecords(TrackRecord record) throws Exception {
		
		Map<String,String> parameters=new HashMap<String,String>();
		parameters.put("driverName",record.getDriverName());
		parameters.put("carName",record.getCarName());
		parameters.put("circuitName",record.getCircuitName());
		parameters.put("laptime",""+record.getLaptime().getLaptime());
		parameters.put("version",record.getVersion());
		parameters.put("date",record.getDate());
		
		String xml=DataLoader.openURL(Settings.SAVE_RECORDS_URL,parameters,CHARSET);
		XMLParser.parseXML(xml);
	}
	
	/**
	 * Loads all track records from the server. The records will be returned as
	 * an array.
	 * @throws IOException when a connection to the server could not be made.
	 */
	
	public static TrackRecord[] getRecords() throws Exception {
		
		List<TrackRecord> records=new ArrayList<TrackRecord>();
	
		Map<String,String> parameters=new HashMap<String,String>();
		parameters.put("maxRecords",""+MAX_RECORDS);
		String xml=DataLoader.openURL(Settings.LOAD_RECORDS_URL,parameters,CHARSET);
		Document document=XMLParser.parseXML(xml);
		
		for (Element i : XMLParser.getChildNodes(document.getDocumentElement())) {
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