package com.azariaa.lia;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class simpleUtils 
{
	
	public static Date addMillisec(Date base, int millisec) {
		return new Date(base.getTime()+millisec);
	}
	
	//returns date1 - date2 in milliseconds
	public static int subtractDatesInMillisec(Date date1, Date date2) {
		return (int)(date1.getTime() - date2.getTime());
	}

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    public static String getCurrentTimeStr()
    {
        return dateFormat.format(new Date());
    }
}
