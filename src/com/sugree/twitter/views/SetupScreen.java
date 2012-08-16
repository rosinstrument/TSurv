package com.sugree.twitter.views;

import com.substanceofcode.infrastructure.Device;
import com.substanceofcode.twitter.Settings;
import com.substanceofcode.utils.Log;
import com.sugree.twitter.TwitterController;
import java.io.IOException;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStoreException;

public class SetupScreen extends Form implements CommandListener, ItemStateListener, ItemCommandListener {

    private final String[] gatewaysLabel = {
        "Custom",
        "api.twitter.com",
        "twitter.com",};
    private final String[] gatewaysValue = {
        null,
        "http://api.twitter.com/1/",
        "http://twitter.com/",};
    private final String[] pictureGatewaysLabel = {
        "Custom",
        "upload.twitter.com",
        "rosinstrument.com",};
    private final String[] pictureGatewaysValue = {
        null,
        "https://upload.twitter.com/1/statuses/update_with_media.json",
        "basic:http://twi.rosinstrument.com/cgi-bin/put_media_json.pl",};
    private final String[] startsLabel = {
        "Empty",
        "Tweet",
        "Home",
        "Friends",
        "@Replies",};
    private final String[] flagsLabel = {
        "Optimize bandwidth",
        "Alternate authentication",
        "Fullscreen picture",
        "160-characters tweet (non-standard)",
        "Resize/Auto pics",
        "Wrap timeline",
        "squeeze",
        "GPS",
        "reverse geocoder",
        "cell ID",
        "refresh",
        "refresh alert",
        "refresh vibrate",
        "refresh counter",
        "Swap minimize/refresh",
        "auto tweet",
        "auto picture",
        "Force no Host",
        "GZIP",
        "Switch logging off",};
    public static String[] captureDevicesValue = {
        null,
        "capture://image",
        "capture://video",
        "capture://devcam0",
        "capture://devcam1",};
    private final String[] captureDevicesLabel = {
        "Custom",
        captureDevicesValue[1],
        captureDevicesValue[2],
        captureDevicesValue[3],
        captureDevicesValue[4],};
    private String[] snapshotEncodingsLabel;
    private TwitterController controller;
    private TextField usernameField, passwordField, timelineLengthField,
            suffixTextField, refreshIntervalField, autoUpdateTextField,
            gatewayField, pictureGatewayField, captureDeviceField,
            snapshotEncodingField, customWordsField, locationFormatField,
            cellIdFormatField, hackField, timeBetweenShotsField, maxPicSizeField;
    private ChoiceGroup gatewaysField, pictureGatewaysField,
            captureDevicesField, snapshotEncodingsField, startsField, flagsField;
    private Command saveCommand, cancelCommand, togglePasswordCommand, oauthSetup,
            oauthRequestCommand, oauthAccessCommand, oauthDisableCommand;

    private Displayable prevDisplay;

    public SetupScreen(TwitterController controller) {
        super("Setup");
        this.controller = controller;
        prevDisplay = controller.getCurrentScreen();
        Settings settings = controller.getSettings();
        StringItem gotoOauth = new StringItem(null, "OAuth Setup", StringItem.BUTTON);
        gotoOauth.addCommand(oauthSetup = new Command("OAuth", Command.OK, 1));
        gotoOauth.setItemCommandListener(this);
        append(gotoOauth);
        String username = settings.getStringProperty(Settings.USERNAME, "");
        append(usernameField = new TextField("Username (basic auth)",
                username, 32, TextField.ANY));
        String password = settings.getStringProperty(Settings.PASSWORD, "");
        passwordField = new TextField("Password (basic auth)",
                password, 32, TextField.PASSWORD);
        append(passwordField);
        int timelineLength = settings.getIntProperty(Settings.TIMELINE_LENGTH, 20);
        timelineLengthField = new TextField("Items in Timeline", String.valueOf(timelineLength), 32, TextField.NUMERIC);
        append(timelineLengthField);
        int timeBetweenShots = settings.getIntProperty(Settings.TIME_BETWEEN_SHOTS, 2);
        timeBetweenShotsField = new TextField("Time Between shots (sec)", String.valueOf(timeBetweenShots), 32, TextField.NUMERIC);
        append(timeBetweenShotsField);

        int maxPicSize = settings.getIntProperty(Settings.MAX_PIC_SIZE, 512);
        maxPicSizeField = new TextField("Max Pic Size (kB)", String.valueOf(maxPicSize), 32, TextField.NUMERIC);
        append(maxPicSizeField);

        String suffixText = settings.getStringProperty(Settings.SUFFIX_TEXT, "");
        suffixTextField = new TextField("Suffix", suffixText, 140, TextField.ANY);
        append(suffixTextField);

        int refreshInterval = settings.getIntProperty(Settings.REFRESH_INTERVAL, 120);
        refreshIntervalField = new TextField("Refresh Interval", String.valueOf(refreshInterval), 32, TextField.NUMERIC);
        append(refreshIntervalField);

        String autoUpdateText = settings.getStringProperty(Settings.AUTO_UPDATE_TEXT, "%H:%M");
        autoUpdateTextField = new TextField("Auto tweet text", autoUpdateText, 140, TextField.ANY);
        append(autoUpdateTextField);

        String gateway = settings.getStringProperty(Settings.GATEWAY, gatewaysValue[1]);
        gatewayField = new TextField("Gateway", gateway, 128, TextField.URL);
        append(gatewayField);

        gatewaysField = new ChoiceGroup("Preset Gateways", Choice.EXCLUSIVE, gatewaysLabel, null);
        append(gatewaysField);

        String pictureGateway = settings.getStringProperty(Settings.PICTURE_GATEWAY, pictureGatewaysValue[1]);
        pictureGatewayField = new TextField("Picture Gateway", pictureGateway, 128, TextField.URL);
        append(pictureGatewayField);

        pictureGatewaysField = new ChoiceGroup("Preset Picture Gateways", Choice.EXCLUSIVE, pictureGatewaysLabel, null);
        append(pictureGatewaysField);

        String captureDevice = settings.getStringProperty(Settings.CAPTURE_DEVICE, Device.getSnapshotLocator());
        captureDeviceField = new TextField("Capture Device", captureDevice, 128, TextField.ANY);
        append(captureDeviceField);

        captureDevicesField = new ChoiceGroup("Preset Capture Devices", Choice.EXCLUSIVE, captureDevicesLabel, null);
        captureDevicesField.setSelectedIndex(0, true);
        append(captureDevicesField);

        String snapshotEncoding = settings.getStringProperty(Settings.SNAPSHOT_ENCODING, "");
        snapshotEncodingField = new TextField("Picture Options", snapshotEncoding, 128, TextField.ANY);
        append(snapshotEncodingField);

        snapshotEncodingsField = new ChoiceGroup("Preset Picture Options", Choice.EXCLUSIVE);
        snapshotEncodingsField.append("Custom", null);
        snapshotEncodingsLabel = Device.getSnapshotEncodings();
        if (snapshotEncodingsLabel != null) {
            for (int i = 0; i < snapshotEncodingsLabel.length; i++) {
                snapshotEncodingsField.append(snapshotEncodingsLabel[i], null);
            }
        }
        snapshotEncodingsField.setSelectedIndex(0, true);
        append(snapshotEncodingsField);
        String customWords = settings.getStringProperty(Settings.CUSTOM_WORDS, "#test,@TwiDiff,#look");
        customWordsField = new TextField("Custom Words", customWords, 1000, TextField.ANY);
        append(customWordsField);

        String locationFormat = settings.getStringProperty(Settings.LOCATION_FORMAT, "l:%lat,%lon http://maps.google.com/maps?q=%lat%2c%lon");
        locationFormatField = new TextField("Location Format", locationFormat, 1000, TextField.ANY);
        append(locationFormatField);

        String cellIdFormat = settings.getStringProperty(Settings.CELLID_FORMAT, "c2l:%cid,%lac");
        cellIdFormatField = new TextField("Cell ID Format", cellIdFormat, 1000, TextField.ANY);
        append(cellIdFormatField);

        int startScreen = settings.getIntProperty(Settings.START_SCREEN, 0);
        startsField = new ChoiceGroup("Start Screen", Choice.EXCLUSIVE, startsLabel, null);
        startsField.setSelectedIndex(startScreen, true);
        append(startsField);

        boolean[] flags = {
            settings.getBooleanProperty(Settings.OPTIMIZE_BANDWIDTH, true),
            settings.getBooleanProperty(Settings.ALTERNATE_AUTHEN, false),
            settings.getBooleanProperty(Settings.SNAPSHOT_FULLSCREEN, false),
            settings.getBooleanProperty(Settings.STATUS_LENGTH_MAX, false),
            settings.getBooleanProperty(Settings.RESIZE_THUMBNAIL, false),
            settings.getBooleanProperty(Settings.WRAP_TIMELINE, false),
            settings.getBooleanProperty(Settings.ENABLE_SQUEEZE, true),
            settings.getBooleanProperty(Settings.ENABLE_GPS, true),
            settings.getBooleanProperty(Settings.ENABLE_REVERSE_GEOCODER, true),
            settings.getBooleanProperty(Settings.ENABLE_CELL_ID, true),
            settings.getBooleanProperty(Settings.ENABLE_REFRESH, false),
            settings.getBooleanProperty(Settings.ENABLE_REFRESH_ALERT, true),
            settings.getBooleanProperty(Settings.ENABLE_REFRESH_VIBRATE, true),
            settings.getBooleanProperty(Settings.ENABLE_REFRESH_COUNTER, false),
            settings.getBooleanProperty(Settings.SWAP_MINIMIZE_REFRESH, false),
            settings.getBooleanProperty(Settings.ENABLE_AUTO_UPDATE, false),
            settings.getBooleanProperty(Settings.ENABLE_AUTO_UPDATE_PICTURE, false),
            settings.getBooleanProperty(Settings.FORCE_NO_HOST, false),
            settings.getBooleanProperty(Settings.ENABLE_GZIP, true),
            settings.getBooleanProperty(Settings.LOG_OFF, false),};
        flagsField = new ChoiceGroup("Advanced Options", Choice.MULTIPLE, flagsLabel, null);
        flagsField.setSelectedFlags(flags);
        append(flagsField);
        String hack = settings.getStringProperty(Settings.HACK, "");
        hackField = new TextField("Hack", hack, 1024, TextField.ANY);
        append(hackField);
        saveCommand = new Command("Save", Command.OK, 1);
        addCommand(saveCommand);
        cancelCommand = new Command("Cancel", Command.CANCEL, 2);
        addCommand(cancelCommand);
        togglePasswordCommand = new Command("Toggle Password", Command.SCREEN, 3);
        addCommand(togglePasswordCommand);
        oauthRequestCommand = new Command("OAuth Request", Command.SCREEN, 4);
        addCommand(oauthRequestCommand);
        oauthAccessCommand = new Command("OAuth Access", Command.SCREEN, 5);
        addCommand(oauthAccessCommand);
        oauthDisableCommand = new Command("OAuth Disable", Command.SCREEN, 6);
        addCommand(oauthDisableCommand);
        setCommandListener(this);
        setItemStateListener(this);
    }

    public void itemStateChanged(Item item) {
        if (item == gatewaysField) {
            String url = gatewaysValue[gatewaysField.getSelectedIndex()];
            if (url != null) {
                gatewayField.setString(url);
            }
        } else if (item == pictureGatewaysField) {
            String url = pictureGatewaysValue[pictureGatewaysField.getSelectedIndex()];
            if (url != null) {
                pictureGatewayField.setString(url);
            }
        } else if (item == captureDevicesField) {
            String device = captureDevicesValue[captureDevicesField.getSelectedIndex()];
            if (device != null) {
                captureDeviceField.setString(device);
            }
        } else if (item == snapshotEncodingsField) {
            snapshotEncodingsLabel = Device.getSnapshotEncodings();
            int index = snapshotEncodingsField.getSelectedIndex();
            if (index > 0) {
                snapshotEncodingField.setString(snapshotEncodingsLabel[index - 1]);
            }
        }
    }

    public void commandAction(Command cmd, Displayable display) {
        if (cmd == saveCommand) {
            try {
                String username = usernameField.getString();
                String password = passwordField.getString();
                String gateway = gatewayField.getString();
                int timelineLength = Integer.parseInt(timelineLengthField.getString());
                int timeBetweenShots = Integer.parseInt(timeBetweenShotsField.getString());
                int maxPicSize = Integer.parseInt(maxPicSizeField.getString());
                String suffixText = suffixTextField.getString();
                Log.verbose(suffixText);
                int refreshInterval = Integer.parseInt(refreshIntervalField.getString());
                String autoUpdateText = autoUpdateTextField.getString();
                String customWords = customWordsField.getString();
                String locationFormat = locationFormatField.getString();
                String cellIdFormat = cellIdFormatField.getString();
                int startScreen = startsField.getSelectedIndex();
                String pictureGateway = pictureGatewayField.getString();
                String captureDevice = captureDeviceField.getString().trim();
                String snapshotEncoding = snapshotEncodingField.getString().trim();
                boolean[] flags = new boolean[flagsField.size()];
                flagsField.getSelectedFlags(flags);
                String hack = hackField.getString();

                if (!gateway.endsWith("/")) {
                    gateway += "/";
                }
                Log.verbose("getSettings");
                Settings settings = controller.getSettings();
                settings.setStringProperty(Settings.USERNAME, username);
                settings.setStringProperty(Settings.PASSWORD, password);
                settings.setStringProperty(Settings.GATEWAY, gateway);
                settings.setIntProperty(Settings.TIMELINE_LENGTH, timelineLength);
                settings.setIntProperty(Settings.TIME_BETWEEN_SHOTS, timeBetweenShots);
                settings.setIntProperty(Settings.MAX_PIC_SIZE, maxPicSize);
                settings.setStringProperty(Settings.SUFFIX_TEXT, suffixText);
                settings.setIntProperty(Settings.REFRESH_INTERVAL, refreshInterval);
                settings.setStringProperty(Settings.AUTO_UPDATE_TEXT, autoUpdateText);
                settings.setStringProperty(Settings.CUSTOM_WORDS, customWords);
                settings.setStringProperty(Settings.LOCATION_FORMAT, locationFormat);
                settings.setStringProperty(Settings.CELLID_FORMAT, cellIdFormat);
                settings.setIntProperty(Settings.START_SCREEN, startScreen);
                settings.setBooleanProperty(Settings.OPTIMIZE_BANDWIDTH, flags[0]);
                settings.setBooleanProperty(Settings.ALTERNATE_AUTHEN, flags[1]);
                settings.setBooleanProperty(Settings.SNAPSHOT_FULLSCREEN, flags[2]);
                settings.setBooleanProperty(Settings.STATUS_LENGTH_MAX, flags[3]);
                settings.setBooleanProperty(Settings.RESIZE_THUMBNAIL, flags[4]);
                settings.setBooleanProperty(Settings.WRAP_TIMELINE, flags[5]);
                settings.setBooleanProperty(Settings.ENABLE_SQUEEZE, flags[6]);
                settings.setBooleanProperty(Settings.ENABLE_GPS, flags[7]);
                settings.setBooleanProperty(Settings.ENABLE_REVERSE_GEOCODER, flags[8]);
                settings.setBooleanProperty(Settings.ENABLE_CELL_ID, flags[9]);
                settings.setBooleanProperty(Settings.ENABLE_REFRESH, flags[10]);
                settings.setBooleanProperty(Settings.ENABLE_REFRESH_ALERT, flags[11]);
                settings.setBooleanProperty(Settings.ENABLE_REFRESH_VIBRATE, flags[12]);
                settings.setBooleanProperty(Settings.ENABLE_REFRESH_COUNTER, flags[13]);
                settings.setBooleanProperty(Settings.SWAP_MINIMIZE_REFRESH, flags[14]);
                settings.setBooleanProperty(Settings.ENABLE_AUTO_UPDATE, flags[15]);
                settings.setBooleanProperty(Settings.ENABLE_AUTO_UPDATE_PICTURE, flags[16]);
                settings.setBooleanProperty(Settings.FORCE_NO_HOST, flags[17]);
                settings.setBooleanProperty(Settings.ENABLE_GZIP, flags[18]);
                settings.setBooleanProperty(Settings.LOG_OFF, flags[19]);
                Log.setLogOff(flags[19]);
                settings.setStringProperty(Settings.HACK, hack);
                settings.setStringProperty(Settings.PICTURE_GATEWAY, pictureGateway);
                settings.setStringProperty(Settings.CAPTURE_DEVICE, captureDevice);
                settings.setStringProperty(Settings.SNAPSHOT_ENCODING, snapshotEncoding);
                controller.loadSettings();
                Log.verbose("save");
                settings.save(true);
            } catch (IOException e) {
                Log.error(e.toString());
                controller.showError(e);
            } catch (RecordStoreException e) {
                Log.error(e.toString());
                controller.showError(e);
            } catch (Exception e) {
                Log.error(e.toString());
                controller.showError(e);
            }
            //controller.showTimeline();
            controller.setCurrent(prevDisplay);
        } else if (cmd == cancelCommand) {
             controller.setCurrent(prevDisplay);
//           controller.showTimeline();
        } else if (cmd == togglePasswordCommand) {
            passwordField.setConstraints(passwordField.getConstraints() ^ TextField.PASSWORD);
        } else if (cmd == oauthRequestCommand) {
            controller.startOAuthRequestToken();
        } else if (cmd == oauthAccessCommand) {
            controller.showOAuth(null);
        } else if (cmd == oauthDisableCommand) {
            controller.resetOAuth();
        }
    }

    public void commandAction(Command c, Item item) {
        if (c == oauthSetup) {
            controller.startOAuthRequestToken();
        }
    }
}
