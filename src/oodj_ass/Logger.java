package oodj_ass;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class Logger {
    private static final String LOG_FILE = "data/activity_log.dat"; 

    public static void writeLog(String username, String activityType) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(LOG_FILE, true))) {
            dos.writeUTF(username);               
            dos.writeUTF(activityType);           
            dos.writeLong(new Date().getTime());  
        } catch (IOException e) {
            System.out.println("Error logging: " + e.getMessage());
        }
    }
}