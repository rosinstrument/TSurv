package com.sugree.utils;

import com.substanceofcode.infrastructure.Device;
import com.substanceofcode.twitter.Settings;
import com.substanceofcode.twitter.TwitterApi;
import com.substanceofcode.utils.StringUtil;

public class Location {
	public static final int MODE_GPS = 0;
	public static final int MODE_REVERSE_GEOCODER = 1;
	public static final int MODE_CELL_ID = 2;

	private String altitude;
	private String latitude;
	private String longitude;
	private Settings settings;

	public Location(Settings settings) {
		altitude = "0.0";
		latitude = "0.0";
		longitude = "0.0";
		this.settings = settings;
	}

	public String getAltitude() {
		return altitude;
	}

	public String getLatitude() {
		return latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public String toString() {
		String s = settings.getStringProperty(Settings.LOCATION_FORMAT, "l:%lat,%lon http://maps.google.com/maps?q=%lat%2c%lon");
		return s;
	}

	public String getCellId() {
		int cid = Device.getCellID();
		int lac = Device.getLAC();

		String s = settings.getStringProperty(Settings.CELLID_FORMAT, "c2l:%cid,%lac");
		s = StringUtil.replace(s, "%cid", ""+cid);
		s = StringUtil.replace(s, "%lac", ""+lac);
		return s;
	}

	public String refresh(TwitterApi api, int mode) throws Exception {
		if (mode == MODE_CELL_ID) {
			return getCellId();
		} else if (api != null && mode == MODE_REVERSE_GEOCODER) {
		}
		return toString();
	}

}
