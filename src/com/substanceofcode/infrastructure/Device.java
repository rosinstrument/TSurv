/*
 * Device.java
 *
 * Copyright (C) 2005-2008 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.substanceofcode.infrastructure;
import com.substanceofcode.utils.StringUtil;

/**
 *
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public class Device {

    public static boolean isNokia() {
        try {
            Class.forName("com.nokia.mid.ui.DeviceControl");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isSonyEricsson() {
        try {
            Class.forName("com.nokia.mid.ui.DeviceControl");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isSiemens() {
        try {
            /* if this class is found, the phone is a siemens phone */
            Class.forName("com.siemens.mp.game.Light");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isSamsung() {
        try {
            /* if this class is found, the phone is a samsung phone */
            Class.forName("com.samsung.util.LCDLight");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static String getVendorName() {
        if (isNokia()) {
            return "Nokia";
        } else if (isSonyEricsson()) {
            return "Sony Ericsson";
        } else if (isSiemens()) {
            return "Siemens";
        } else if (isSamsung()) {
            return "Samsung";
        } else {
            return "Unknown";
        }
    }

    public static String getPhoneType() {
        try {
            return System.getProperty("microedition.platform");
        } catch (Exception ex) {
            return "Unknown";
        }
    }

    public static String getSnapshotLocator() {
        return "capture://image";
    }

    public static String[] getSnapshotEncodings() {
        try {
            return StringUtil.split(System.getProperty("video.snapshot.encodings"), " ");
        } catch (Exception ex) {
            return null;
        }
    }

    public static int getCellID() {
        String[] cidProperties = {
            "com.sonyericsson.net.cellid",
            "Cell-ID",
            "com.nokia.mid.cellid",
            "com.samsung.cellid",
            "com.siemens.cellid",
            "CellID",
            "phone.cid",
            "cid",};
        try {
            return Integer.parseInt(lookupProperty(cidProperties));
        } catch (Exception ex) {
            return 0;
        }
    }

    public static int getLAC() {
        String[] lacProperties = {
            "com.sonyericsson.net.lac",
            "LocAreaCode",
            "phone.lac",
            "com.nokia.mid.cellid",
            "com.samsung.cellid",
            "com.siemens.cellid",
            "cid",};
        try {
            return Integer.parseInt(lookupProperty(lacProperties));
        } catch (Exception ex) {
            return 0;
        }
    }

    public static String lookupProperty(String[] properties) {
        String result = null;
        for (int i = 0; i < properties.length; i++) {
            result = System.getProperty(properties[i]);
            if (result != null) {
                break;
            }
        }
        return result;
    }
}
