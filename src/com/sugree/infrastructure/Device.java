/**
 * http://wiki.forum.nokia.com/index.php/Platform_independent_key_events_processing
 */

package com.sugree.infrastructure;

import com.substanceofcode.utils.StringUtil;

public class Device {
	public final static String PLATFORM_NOKIA = "Nokia";
	public final static String PLATFORM_SONY_ERICSSON = "Sony Ericsson";
	public final static String PLATFORM_WELLCOM = "WellcoM";
	public final static String PLATFORM_SAMSUNG = "Samsung";
	public final static String PLATFORM_MOTOROLA = "Motorola";
	public final static String PLATFORM_SIEMENS = "Siemens";
	public final static String PLATFORM_LG = "LG";
	public final static String PLATFORM_NOT_DEFINED = "Not defined";

	public static String getPlatform() {
		// detecting NOKIA or SonyEricsson
		try {
			final String currentPlatform = System.getProperty("microedition.platform");
			if (currentPlatform.indexOf("Nokia") != -1) {
				return PLATFORM_NOKIA;
			} else if (currentPlatform.indexOf("SonyEricsson") != -1) {
				return PLATFORM_SONY_ERICSSON;
			} else if (currentPlatform.indexOf("WellcoM") != -1) {
				return PLATFORM_WELLCOM;
			}
		} catch (Throwable ex) {
		}
		// detecting SAMSUNG
		try {
			Class.forName("com.samsung.util.Vibration");
			return PLATFORM_SAMSUNG;
		} catch (Throwable ex) {
		}
		// detecting MOTOROLA
		try {
			Class.forName("com.motorola.multimedia.Vibrator");
			return PLATFORM_MOTOROLA;
		} catch (Throwable ex) {
			try {
				Class.forName("com.motorola.graphics.j3d.Effect3D");
				return PLATFORM_MOTOROLA;
			} catch (Throwable ex2) {
				try {
					Class.forName("com.motorola.multimedia.Lighting");
					return PLATFORM_MOTOROLA;
				} catch (Throwable ex3) {
					try {
						Class.forName("com.motorola.multimedia.FunLight");
						return PLATFORM_MOTOROLA;
					} catch (Throwable ex4) {
					}
				}
			}
		}
		/*
		try {
			if (adaptorCanvas.getKeyName(SOFT_KEY_LEFT_MOTOROLA).toUpperCase().indexOf(SOFT_WORD) > -1) {
				return PLATFORM_MOTOROLA;
			}
		} catch (Throwable e) {
			try {
				if (adaptorCanvas.getKeyName(SOFT_KEY_LEFT_MOTOROLA1).toUpperCase().indexOf(SOFT_WORD) > -1) {
					return PLATFORM_MOTOROLA;
				}
			} catch (Throwable e1) {
				try {
					if (adaptorCanvas.getKeyName(SOFT_KEY_LEFT_MOTOROLA2).toUpperCase().indexOf(SOFT_WORD) > -1) {
						return PLATFORM_MOTOROLA;
					}
				} catch (Throwable e2) {
				}
			}
		}
		*/
		// detecting SIEMENS
		try {
			Class.forName("com.siemens.mp.io.File");
			return PLATFORM_SIEMENS;
		} catch (Throwable ex) {
		}
		// detecting LG
		try {
			Class.forName("mmpp.media.MediaPlayer");
			return PLATFORM_LG;
		} catch (Throwable ex) {
			try {
				Class.forName("mmpp.phone.Phone");
				return PLATFORM_LG;
			} catch (Throwable ex1) {
				try {
					Class.forName("mmpp.lang.MathFP");
					return PLATFORM_LG;
				} catch (Throwable ex2) {
					try {
						Class.forName("mmpp.media.BackLight");
						return PLATFORM_LG;
					} catch (Throwable ex3) {
					}
				}
			}
		}
		return PLATFORM_NOT_DEFINED;
	}

	public static String[] getSnapshotEncodings() {
		try {
			String encodings = System.getProperty("video.snapshot.encodings");
			if (encodings == null) {
				return null;
			}
			return StringUtil.split(encodings, " ");
		} catch(Exception ex) {
			return null;
		}
	}

}
