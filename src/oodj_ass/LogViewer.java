package oodj_ass;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

public class LogViewer {
    
    public static void main(String[] args) {
        System.out.println("--- READING BINARY ACTIVITY LOGS ---");
        
        try (DataInputStream dis = new DataInputStream(new FileInputStream("data/activity_log.dat"))) {
            
            while (dis.available() > 0) {
                String username = dis.readUTF();       
                String activity = dis.readUTF();       
                long timestamp = dis.readLong();       
                
                System.out.println(String.format("[%s] User: %s | Action: %s", 
                        new Date(timestamp).toString(), username, activity));
            }
        } catch (IOException e) {
            System.out.println("No logs found yet. (Try logging in and out first!)");
        }
        System.out.println("--- END OF LOGS ---");
    }
}