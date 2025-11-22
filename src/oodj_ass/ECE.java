/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oodj_ass;

/**
 *
 * @author jolin
 */
import java.io.*;
import java.util.*;

public class ECE {

    // convert mark to grade
    public static class GradeConverter {

        public static String getAlphabetGrade(int marks) {
            if (marks >= 80) return "A+";
            else if (marks >= 75) return "A";
            else if (marks >= 70) return "B+";
            else if (marks >= 65) return "B";
            else if (marks >= 60) return "C+";
            else if (marks >= 55) return "C";
            else if (marks >= 50) return "C-";
            else if (marks >= 40) return "D";
            else if (marks >= 30) return "F+";
            else if (marks >= 20) return "F";
            else return "F-";
        }

        //convert grade to gradePoint
        public static double getGradePoint(String g) {
            switch (g) {
                case "A+": return 4.0;
                case "A":  return 3.7;
                case "B+": return 3.3;
                case "B":  return 3.0;
                case "C+": return 2.7;
                case "C":  return 2.3;
                case "C-": return 2.0;
                case "D":  return 1.7;
                case "F+": return 1.3;
                case "F":  return 1.0;
                case "F-": return 0.0;
                default:   return 0.0;
            }
        }
    }
    
    
    public static class Course {
        public String courseID;
        public int creditHours;
        public int assWeight;
        public int examWeight;

        public Course(String courseID, int creditHours, int assWeight, int examWeight) {
            this.courseID = courseID;
            this.creditHours = creditHours;
            this.assWeight = assWeight;
            this.examWeight = examWeight;
        }
    }
    
    public static ArrayList<String[]> loadFile(String filename, boolean hasHeader) {
        ArrayList<String[]> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;

            if (hasHeader) {
                br.readLine(); // skip header line
            }

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                list.add(parts); 
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
    
        //fine the course by courseID
        public static String[] findCourseRow(String courseID, ArrayList<String[]> courseList) {
        for (String[] row : courseList) {
            if (row[0].trim().equals(courseID)) {
                return row;
            }
        }
        return null; // not found
    }
}