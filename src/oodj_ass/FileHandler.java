package oodj_ass;

import java.io.File;
import java.io.IOException;


public class FileHandler {
    public static void createFiles() {
        String[] filenames = {
            "data/users.txt",
            "data/students.txt",
            "data/courses.txt",
            "data/logs.txt",
            "data/recoveryPlans.txt",
            "data/result.txt",
            "data/resultArchive.txt",
            "data/emailRecords.txt",
            "data/recoveryMilestones.txt",
            "data/gradeArchive.txt",
            "data/studentInfo.txt",
            "data/studentCourse.txt"
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
                    System.err.println("Error creating file: " + name);

                e.printStackTrace();
            }
        }
    }
}
