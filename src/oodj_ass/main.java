package oodj_ass;

import java.util.List;
//import java.util.ArrayList;

public class main {
    private static FileLoader loader;

    public static void main(String[] args) {
        //FileHandler.createFiles();
        
        FileLoader loader = new FileLoader();
        loader.loadAll();

        Student fiona = loader.getStudents().get(0);  // assuming S001 is first
        System.out.println("\nCourses & scores for " + fiona.getFullName() + ":");
        for (Course c : fiona.getCourses()) {
            System.out.printf("%s  A:%d  E:%d  grade:%s  attempt:%d  failed? %s%n",
                    c.getCourseID(),
                    c.getAssignmentScore(),
                    c.getExamScore(),
                    c.getGrade(),
                    c.getAttemptNumber(),
                    c.isFailed());
        }


    // Provide public access if other UIs want direct loader
    /*public static FileLoader getLoader() {
        return loader;
    }*/    
    }
}
