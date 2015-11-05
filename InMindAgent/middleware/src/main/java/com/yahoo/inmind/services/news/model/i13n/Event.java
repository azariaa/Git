package com.yahoo.inmind.services.news.model.i13n;

import com.yahoo.inmind.services.news.control.yi13n.LocationTracker;
import com.yahoo.inmind.services.news.model.vo.ValueObject;

import org.json.JSONObject;

import java.util.Date;

public class Event extends ValueObject{
	public String userid;	//The ID of the logged-in user. It will be a random number associated with the MainActivity serving as the session identifier.  
	public String pkgName;	//The component the event occurred in
	public String action;	//The description of the action. e.g. onPause/onResume of the Activity/Fragment, or load a specific URL.   
	public String uuid; 	//The uuid of the article, if this event is associated with an article.
	public JSONObject location;	//The GPS location
	public Date time;		//The time the event occurs.
	public String orientation;	//The screen orientation of the device.
	public String devid;	//Device ID
//	public String title;	//The title of the article
//	public String url;		//The url of the article
	
	public Event(){
		
	}
	
	public Event(String name, String action) {
		super();
		pkgName = name;
		this.action = action;
		time = new Date();
	}

	public Event(Event evt) {
		this.deepCopy(evt);
		this.time = new Date();
	}

	public boolean equals(Event evt){
		if (pkgName.equals(evt.pkgName) && action.equals(evt.action) )
			return true;
		return false;
	}

	public Event setAction(String act) {
		this.action = act;
		return this;
	}

	public void annotateLocation() {
		this.location = LocationTracker.getInstance().getLastLocation().toJSON();
	}

	public Event setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}
}
