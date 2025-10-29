package oodj_ass;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author jolin
 */
public class FileHandler {
    public static void createFiles() {
        String[] filenames = {
            "data/users.txt",
            "data/courses.txt",
            "data/grades.txt",
            "data/logs.txt",
            "data/recoverplans.txt",
            "data/eligibility.txt",
            "data/reportHistory.txt",
            "data/emailRecords.txt",
        };
        
        for (String name : filenames) {
            try {
                File file = new File(name);
                if(file.getParentFile() != null) 
                    file.getParentFile().mkdirs();
                if (file.createNewFile()) {
                    System.out.println(name + " created");
                } else {
                    System.out.println(name + " already exists. ");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
