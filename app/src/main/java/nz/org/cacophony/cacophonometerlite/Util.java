package nz.org.cacophony.cacophonometerlite;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
//import android.util.Log;
import android.widget.Toast;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Scanner;

//import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.BasicLogcatConfigurator;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;


class Util {
    private static final String LOG_TAG = Util.class.getName();

    private static final String DEFAULT_RECORDINGS_FOLDER = "recordings";
    private static final String DEFAULT_LOGS_FOLDER = "logs";

    private static boolean logbackConfigured = false;
    private static Logger logger = null;

    static {
        BasicLogcatConfigurator.configureDefaultContext();
    }

    // For airplane mode
    private final static String COMMAND_FLIGHT_MODE_1 = "settings put global airplane_mode_on";
    private final static String COMMAND_FLIGHT_MODE_2 = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state";


    // --Commented out by Inspection (12-Jun-17 2:21 PM):private final static String COMMAND_FLIGHT_MODE_3 = "settings put global airplane_mode_on";

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean checkPermissionsForRecording(Context context) {
        boolean permissionForRecording = false;
        try{
            if (context == null) {
//                Log.e(LOG_TAG, "Context was null when checking permissions");
                logger.error("Context was null when checking permissions");
            }else{
                boolean storagePermission =
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                boolean microphonePermission =
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                boolean locationPermission =
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                permissionForRecording = (storagePermission && microphonePermission && locationPermission);
            }

        }catch (Exception ex){
//            Log.e(LOG_TAG, "Error with checkPermissionsForRecording");
//            Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "Error with checkPermissionsForRecording");
            logger.error("Error with checkPermissionsForRecording");
        }
      return permissionForRecording;

    }

    private static File getAppDataFolder(Context context) {
    //    logger.info("getAppDataFolder method called");
        // 15/8/16 Tim Hunt - Going to change file storage location to always use internal phone storage rather than rely on sdcard
        // This is because if sdcard is 'dodgy' can get inconsistent results.
        // Need context to get internal storage location, so need to pass this around the code.
        // If I could be sure the homeFile was set before any of the other directories are needed then I
        // wouldn't need to pass context around to the other methods :-(


        //https://developer.android.com/reference/android/content/Context.html#getDir(java.lang.String, int)
        File appDataFolder = null;
        try {
            String appName = context.getResources().getString(R.string.main_activity_name);
//            homeFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/cacophony2");
            String externalStorageState = Environment.getExternalStorageState();
            if (externalStorageState.equalsIgnoreCase("mounted")){
                appDataFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + appName);
            }else{
                appDataFolder = context.getFilesDir();
            }


            if (appDataFolder == null) {
//                Log.e(LOG_TAG, "HomeFile location problem");
//                Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "HomeFile location problem");

            } else {
                if (!appDataFolder.exists()){
                 boolean appDataFolderCreated =   appDataFolder.mkdirs();
                    if (!appDataFolderCreated){
                 //       logger.error("Could not create AppDataFolder");

                        return null;
                    }
                    // check it got created
                    if (!appDataFolder.exists()){

                 //       logger.error("Could not create AppDataFolder");
                        return null;
                    }
                }



//                if (!homeFile.exists() && !homeFile.isDirectory() && !homeFile.mkdirs()) {
////                    Log.e(LOG_TAG, "HomeFile location problem");
////                    Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "HomeFile location problem");
//                    logger.error("HomeFile location problem");
//                }
            }
        } catch (Exception ex) {
//            Log.e(LOG_TAG, "HomeFile location problem");
//            Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "HomeFile location problem");
          //  logger.error(ex.getLocalizedMessage());
          //  logger.error("AppDataFolder problem");
        }
        return appDataFolder;
    }


    static File getRecordingsFolder(Context context){
        return getLocalFolder(context, DEFAULT_RECORDINGS_FOLDER);
    }

    static File getLogFolder(Context context){
        return getLocalFolder(context, DEFAULT_LOGS_FOLDER);
    }

    static File getLocalFolder(Context context, String localFolderStr) {

        File localFolderFile = null;
        try {

            File appDataFolder = getAppDataFolder(context);
            if (appDataFolder == null){
            //    logger.error("appDataFolder is null");
                return null;
            }
          //  localFolderFile = new File(getAppDataFolder(context), localFolderStr);
            localFolderFile = new File(appDataFolder, localFolderStr);
            if (localFolderFile == null) {
//                Log.e(LOG_TAG, "Folder location is null");
//                Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "Folder location is null");
            //    logger.error("Folder location is null");
            } else {
                    if (!localFolderFile.exists()){
                        localFolderFile.mkdirs();

                        // now check it exists
                         if (!localFolderFile.exists()){
                   //         logger.error("Folder location problem");
                            localFolderFile = null;
                        }
                    }

                    // now check it is there


                   // !localFolderFile.exists() && !localFolderFile.isDirectory() && !localFolderFile.mkdirs())
              //  {
//                Log.e(LOG_TAG, "Folder location problem");
//                Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "Folder location problem");

              //  }
            }
            return localFolderFile;
        } catch (Exception ex) {
//            Log.e(LOG_TAG, ex.getLocalizedMessage());
//            Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, ex.getLocalizedMessage());
        //    logger.error(ex.getLocalizedMessage());
            return null;
        }
    }



    static String getLocalLogStr(Context context){
//        Date date = new Date(); // your date
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(date);
//        int year = cal.get(Calendar.YEAR);
//        int month = cal.get(Calendar.MONTH);
//        int day = cal.get(Calendar.DAY_OF_MONTH);
//        String todaysLogFileNameStr = Integer.toString(year) + "_" + Integer.toString(month + 1)+ "_" + Integer.toString(day);
        try {
            return getLogFolder(context).getAbsolutePath() + "/" + "log" + ".txt";
        }catch (Exception ex){
            return null;
        }

    }

static Logger getAndConfigureLogger(Context context, String callingLogTag){
//    if (!logbackConfigured){
//        Util.configureLogbackDirectly(context);
//        logbackConfigured = true;
//        // And log that this happened!
//        //getAndConfigureLogger(context, LOG_TAG);
//        LoggerFactory.getLogger(LOG_TAG).info("Configured Logger");
//    }

    Logger logger = (Logger)LoggerFactory.getLogger(callingLogTag);

    Prefs prefs = new Prefs(context, logger); // needed this constructor to fix loop of Prefs calling Util calling Prefs
    // https://stackoverflow.com/questions/3837801/how-to-change-root-logging-level-programmatically
    if (prefs.getUseFullLogging()){
        if (!logbackConfigured){
           if (!Util.configureLogbackDirectly(context, true)){
               return null;
           }
            LoggerFactory.getLogger(LOG_TAG).info("Configured Logger");
            logbackConfigured = true;
        }
        logger.setLevel(Level.DEBUG);
    }else {
        if (!logbackConfigured){
           if( !Util.configureLogbackDirectly(context, false)){
               return null;
           }
            LoggerFactory.getLogger(LOG_TAG).info("Configured Logger");
            logbackConfigured = true;
        }
        logger.setLevel(Level.ERROR);
    }
    setLogger(logger);
    return logger;

}

//    static void writeLocalLogEntryUsingLogback(Context context, Logger callingLogger, String message){
//        if (logger == null){
//            if (!logbackConfigured){
//                if (context != null){ // can only configure if context not null
//                    Util.configureLogbackDirectly(context);
//                    logbackConfigured = true;
//                    logger = LoggerFactory.getLogger(LOG_TAG);
//                    logger.info("Configured Logback");
//                }
//            }
//
//        }else{
//            logger.info(message);
//        }
//
//
//
//
//        if (context != null){
//            if (!logbackConfigured){
//                Util.configureLogbackDirectly(context);
//                logbackConfigured = true;
//
//
//            }
//        }else{
//            logger = LoggerFactory.getLogger(callingLogTag);
//        }
//
//
//
//
//    }

//    static void writeLocalLogEntryUsingLogback(Context context, String LOG_TAG, String message){
//        Prefs prefs = new Prefs(context);
//        if (!prefs.isLocalLog()){
//            return;
//        }
//        // https://stackoverflow.com/questions/8210616/printwriter-append-method-not-appending
//        String todaysLocalLogStr = getLocalLogStr(context);
//        PrintWriter out = null;
//        try {
//            out = new PrintWriter(new BufferedWriter(new FileWriter(todaysLocalLogStr, true)));
//            out.println(LOG_TAG);
//            out.println(" ");
//            out.println(message);
//            out.println(" ");
//        }catch (IOException e) {
////            Log.e(LOG_TAG, e.getLocalizedMessage());
//            logger.error(e.getLocalizedMessage());
//        }finally{
//            if(out != null){
//                out.close();
//            }
//        }
//    }



    static String getDeviceID(Context context, String webToken) throws Exception {
        if (webToken == null){
            return "";
        }

        String webTokenBody = Util.decoded(context, webToken);
        JSONObject jObject = new JSONObject(webTokenBody);
        return jObject.getString("id");
    }

    private static String decoded(Context context, String JWTEncoded) {
        // http://stackoverflow.com/questions/37695877/how-can-i-decode-jwt-token-in-android#38751017
        String webTokenBody = null;
        try {
            String[] split = JWTEncoded.split("\\.");
          //  Log.d("JWT_DECODED", "Header: " + getJson(split[0]));

            // Log.d("JWT_DECODED", "Body: " + getJson(split[1]));
            webTokenBody = getJson(split[1]);
        } catch (UnsupportedEncodingException e) {
//            Log.e(LOG_TAG, "Error decoding JWT");
//            Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "Error decoding JWT");
            logger.error("Error decoding JWT");
        }
        return webTokenBody;
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }

    static double getBatteryLevel(Context context) {
        double batteryLevel;
        batteryLevel = getBatteryLevelUsingSystemFile(context);
        if (batteryLevel == -1) {
            batteryLevel = getBatteryLevelByIntent(context);

        }
        return batteryLevel;
    }

    static double getBatteryLevelUsingSystemFile(Context context) {

        // https://longtrieuquang.wordpress.com/2013/04/08/android-battery-information-from-file-system/
        // found the file volt that stores battery voltage
        String batteryLevelStr = null;
        double batteryLevel = -1;
        String voltFilePathName = "/sys/class/power_supply/battery/volt";
        File voltFile = new File(voltFilePathName);
        if (voltFile.exists()) {
            try {
                batteryLevelStr = getStringFromFile(voltFilePathName);
            } catch (Exception ex) {
                ex.printStackTrace();
//                Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, ex.getLocalizedMessage());
                logger.error( ex.getLocalizedMessage());
            }

        }

        if (batteryLevelStr != null) {
            try {
                batteryLevel = Double.parseDouble(batteryLevelStr);
            } catch (Exception ex) {
//                Log.e(LOG_TAG, "converting double");
//                Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "converting double");
                logger.error("converting double");
            }
        }
        return batteryLevel;
    }

    static double getBatteryLevelByIntent(Context context) {

        //Will use the method of checking battery level that may only give an update if phone charging status changes
        // this will return a percentage
        try {
            double batteryLevel;
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.getApplicationContext().registerReceiver(null, ifilter);
            batteryLevel = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            return batteryLevel;
        } catch (Exception ex) {
//            Log.e(LOG_TAG, "Error with getBatteryLevelByIntent");
//            Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "Error with getBatteryLevelByIntent");
            logger.error("Error with getBatteryLevelByIntent");
            return -1;
        }

    }

    private static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);

        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        //  StringBuilder sb = new StringBuilder();
        String line;

        line = reader.readLine();
        reader.close();
        return line;
    }

    static String getBatteryStatus(Context context) {
        // https://developer.android.com/training/monitoring-device-state/battery-monitoring.html
        // http://stackoverflow.com/questions/24934260/intentreceiver-components-are-not-allowed-to-register-to-receive-intents-when

        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.getApplicationContext().registerReceiver(null, ifilter);
            // Are we charging / charged?
            int status = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;

            String batteryStatusToReturn;
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    batteryStatusToReturn = "CHARGING";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    batteryStatusToReturn = "DISCHARGING";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    batteryStatusToReturn = "NOT_CHARGING";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    batteryStatusToReturn = "FULL";
                    break;
                case BatteryManager.BATTERY_STATUS_UNKNOWN:
                    batteryStatusToReturn = "UNKNOWN";
                    break;
                default:
                    batteryStatusToReturn = Integer.toString(status);
            }
            return batteryStatusToReturn;
        } catch (Exception ex) {
//            Log.e(LOG_TAG, "getBatteryStatus");
//            Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "getBatteryStatus");
            logger.error("getBatteryStatus");
            return "Error";
        }

    }

    /**
     * Gets the state of Airplane Mode.
     * http://stackoverflow.com/questions/4319212/how-can-one-detect-airplane-mode-on-android
     *
     * @return true if enabled.
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }


    /**
     * Returns the sunrise time for the current device location
     *
     * @param context - for getting the location
     * @return Calendar time of the sunrise
     */
    private static Calendar getSunrise(Context context, Calendar todayOrTomorrow) {

        Location location = getLocation(context);
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, "Pacific/Auckland");
        //Log.d("DEBUG: ", "Sunrise time is: " + officialSunrise);
        return calculator.getOfficialSunriseCalendarForDate(todayOrTomorrow);
    }

    static Calendar getDawn(Context context, Calendar todayOrTomorrow) {
        Prefs prefs = new Prefs(context);
        Calendar sunRise = getSunrise(context, todayOrTomorrow);
        Calendar dawn = (Calendar) sunRise.clone();
        int lengthOfTwilight = (int) prefs.getLengthOfTwilightSeconds();
        dawn.add(Calendar.SECOND, -lengthOfTwilight);
        return dawn;
    }

    /**
     * Returns the sunset time for the current device location
     *
     * @param context - for getting the location
     * @return Calendar time of the sunset
     */
    private static Calendar getSunset(Context context, Calendar todayOrTomorrow) {

        Location location = getLocation(context);
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, "Pacific/Auckland");

        // Log.d("DEBUG: ", "Sunset time is: " + officialSunset);
        return calculator.getOfficialSunsetCalendarForDate(todayOrTomorrow);
    }

    static Calendar getDusk(Context context, Calendar todayOrTomorrow) {
        Prefs prefs = new Prefs(context);
        Calendar sunSet = getSunset(context, todayOrTomorrow);
        Calendar dusk = (Calendar) sunSet.clone();
        int lengthOfTwilight = (int) prefs.getLengthOfTwilightSeconds();
        dusk.add(Calendar.SECOND, lengthOfTwilight);
        return dusk;
    }

    private static Location getLocation(Context context) {
        Prefs prefs = new Prefs(context);
        String lat;
        String lon;

        if (prefs.getLatitude() == 0.0 && prefs.getLongitude() == 0.0) {
            // gps not yet set, so to avoid errors/too complex code to check, just use coordinates for Hamilton NZ
            lat = Double.toString(-37.805294);
            lon = Double.toString(175.306775);

        } else {
            lat = Double.toString(prefs.getLatitude());
            lon = Double.toString(prefs.getLongitude());
        }


        return new Location(lat, lon);
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean waitForNetworkConnection(Context context, boolean networkConnectionRequired) {
        int numberOfLoops = 0;

        while (isNetworkConnected(context) != networkConnectionRequired) {

            try {
                Thread.sleep(1000); // give time for airplane mode to turn off
            } catch (InterruptedException ex) {
                ex.printStackTrace();
//                Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, ex.getLocalizedMessage());
                logger.error( ex.getLocalizedMessage());
            }

            numberOfLoops += 1;
            if (numberOfLoops > 20) {
//                Log.e(LOG_TAG, "Number of loops > 20");
                logger.error("Number of loops > 20");
                break;
            }
        }
        //noinspection RedundantIfStatement
        if (numberOfLoops > 20) {
            return false;
        }
        return true;
    }


    static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }



    static void disableFlightMode(Context context) {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                // API 17 onwards.
                // Log.d(LOG_TAG, "Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN");
                // Must be a rooted device
                Prefs prefs = new Prefs(context);
                if (!prefs.getHasRootAccess()) {

                    logger.info("Do NOT have required ROOT access");
                    Util.getToast(context, "Root access required to change airplane mode", true).show();


                    return ;
                }

                // Set Airplane / Flight mode using su commands.
                String command = COMMAND_FLIGHT_MODE_1 + " " + "0";
                executeCommandWithoutWait("-c", command);
                command = COMMAND_FLIGHT_MODE_2 + " " + "false";
                executeCommandWithoutWait("-c", command);

            } else {
                // API 16 and earlier.
                // Log.d(LOG_TAG, "API 16 and earlier.");
                //  boolean enabled = isFlightModeEnabled(context);
                //noinspection deprecation
                Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", false);
                context.sendBroadcast(intent);
            }

        }catch (Exception ex){
            logger.error(ex.getLocalizedMessage());

        }
        return ;
    }

    static void enableFlightMode(Context context) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            // API 17 onwards.

            // Must be a rooted device
            Prefs prefs = new Prefs(context);
            if (!prefs.getHasRootAccess()) {
//                Log.e(LOG_TAG, "Do NOT have required ROOT access");
//                Util.writeLocalLogEntryUsingLogback(context, LOG_TAG, "Do NOT have required ROOT access");

                logger.error("Do NOT have required ROOT access");

                Util.getToast(context,"Root access required to change airplane mode", true ).show();
                return;
            }


            // Set Airplane / Flight mode using su commands.
            String command = COMMAND_FLIGHT_MODE_1 + " " + "1";
            executeCommandWithoutWait("-c", command);
            command = COMMAND_FLIGHT_MODE_2 + " " + "true";
            executeCommandWithoutWait("-c", command);

        } else {
            // API 16 and earlier.
          //  Log.d(LOG_TAG, "API 16 and earlier.");
            //    boolean enabled = isFlightModeEnabled(context);
            //noinspection deprecation
            Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", true);
            context.sendBroadcast(intent);
        }
    }




    private static void executeCommandWithoutWait(@SuppressWarnings("SameParameterValue") String option, String command) {
        // http://muzso.hu/2014/04/02/how-to-programmatically-enable-and-disable-airplane-flight-mode-on-android-4.2
        // http://stackoverflow.com/questions/23537467/enable-airplane-mode-on-all-api-levels-programmatically-android
        boolean success = false;
        String su = "su";
        for (int i = 0; i < 3; i++) {
            // "su" command executed successfully.
            if (success) {
                // Stop executing alternative su commands below.
                break;
            }
            if (i == 1) {
                su = "/system/xbin/su";
            } else if (i == 2) {
                su = "/system/bin/su";
            }
            try {
                // execute command
                Runtime.getRuntime().exec(new String[]{su, option, command});
                success = true;

            } catch (IOException e) {

                success = false;
            }
        }
    }

    static String getSimStateAsString(int simState) {
        String simStateStr;
        switch (simState) {
            case 0:
                simStateStr = "SIM_STATE_UNKNOWN";
                break;
            case 1:
                simStateStr = "SIM_STATE_ABSENT";
                break;
            case 2:
                simStateStr = "SIM_STATE_PIN_REQUIRED";
                break;
            case 3:
                simStateStr = "SIM_STATE_PUK_REQUIRED";
                break;
            case 4:
                simStateStr = "SIM_STATE_NETWORK_LOCKED";
                break;
            case 5:
                simStateStr = "SIM_STATE_READY";
                break;
            case 6:
                simStateStr = "SIM_STATE_NOT_READY";
                break;
            case 7:
                simStateStr = "SIM_STATE_PERM_DISABLED";
                break;
            case 8:
                simStateStr = "SIM_STATE_CARD_IO_ERROR";
                break;
            case 9:
                simStateStr = "SIM_STATE_CARD_RESTRICTED";
                break;
            case 10:
                simStateStr = "October";
                break;
            case 11:
                simStateStr = "November";
                break;
            case 12:
                simStateStr = "December";
                break;
            default:
                simStateStr = "noMatch";
                break;
        }
        return simStateStr;
    }


    static Toast getToast(Context context, String message, boolean standOut){

        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        if (standOut){
            toast.getView().setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
        }else{
            toast.getView().setBackgroundColor(context.getResources().getColor(R.color.colorGreen));
        }

        return toast;
    }

    static boolean configureLogbackDirectly(Context context, boolean includeCodeLineNumber) {
        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it
        LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        lc.reset();

        // setupButtonClick FileAppender
        PatternLayoutEncoder encoder1 = new PatternLayoutEncoder();
        encoder1.setContext(lc);
        //https://stackoverflow.com/questions/23123934/logback-show-logs-with-line-number
        if (includeCodeLineNumber){
            encoder1.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36}.%M\\(%line\\) - %msg%n");
        }else{
            encoder1.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        }

        encoder1.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setContext(lc);
        String logFileStr = Util.getLocalLogStr(context);
        if (logFileStr == null){
            Util.getToast(context, "Could not create required folder - try inserting a memory card into your phone", true).show();
            return false;
        }

        //   fileAppender.setFile(this.getFileStreamPath("app.log").getAbsolutePath());
        fileAppender.setFile(logFileStr);
        fileAppender.setEncoder(encoder1);
        fileAppender.start();

        // setupButtonClick LogcatAppender
        PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
        encoder2.setContext(lc);
        encoder2.setPattern("[%thread] %msg%n");
        encoder2.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(lc);
        logcatAppender.setEncoder(encoder2);
        logcatAppender.start();

        // add the newly created appenders to the root logger;
        // qualify Logger to disambiguate from org.slf4j.Logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppender);
        root.addAppender(logcatAppender);
        return true;
    }

    static String readLocalLog(Context context, File logFileToRead)  {
        // https://stackoverflow.com/questions/8210616/printwriter-append-method-not-appending

        StringBuilder sb = new StringBuilder();
     //   sb.append(logFileToRead.getName());
        Scanner input = null;
        try {
            input = new Scanner(logFileToRead);
            while (input.hasNext()){
                String aLine = input.next();
                sb.append(aLine);
                sb.append(" ");
            }

        }catch (Exception ex){
//            Log.e(LOG_TAG, ex.getLocalizedMessage());
            logger.error(ex.getLocalizedMessage());
        }finally{
            input.close();
            logFileToRead.delete();  // delete and recreate this file to remove the logs that have now been sent to server
            try {
                logFileToRead.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return sb.toString();
    }

    static String getAllLocalLogEntries(Context context){
        StringBuilder sb = new StringBuilder();
        File localLogsFolder = Util.getLogFolder(context);
        if (localLogsFolder == null){
//            Log.d(LOG_TAG, "Error getting recordings folder");
            logger.error("Error getting recordings folder");
            return null;
        }
        File logFiles[] = localLogsFolder.listFiles();

        if (logFiles != null){
            for (int i=0;i<logFiles.length;i++){
                sb.append(readLocalLog( context,  logFiles[i]));
                // logFiles[i].delete();
            }
        }
        return sb.toString();
    }

    public static void setLogbackConfigured(boolean logbackConfigured) {
        Util.logbackConfigured = logbackConfigured;
    }

    static void sendMainActivityAMessage(Context context, String message){
        // https://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager

        Intent intent = new Intent("event");
        // You can also include some extra data.
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

    }

    public static Logger getLogger() {
        if (logger == null){

        }
        return logger;
    }

    public static void setLogger(Logger logger) {
        Util.logger = logger;
    }
}
