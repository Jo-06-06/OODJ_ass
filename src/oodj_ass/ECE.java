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

    // load csv file
    public static ArrayList<String[]> loadFile(String filename, boolean hasHeader) {
        ArrayList<String[]> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;

            if (hasHeader) {
                br.readLine(); // skip header
            }

            while ((line = br.readLine()) != null) {
                list.add(line.split(","));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static String[] findCourseRow(String courseID, ArrayList<String[]> courseList) {
        for (String[] row : courseList) {
            if (row[0].trim().equals(courseID)) {
                return row;
            }
        }
        return null;
    }

    public static void saveUpdatedRecords(ArrayList<String[]> list) {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("academicRecords.txt"))) {

            bw.write("StudentID,CourseID,assResult,examResult,grade,gpa");
            bw.newLine();

            for (String[] r : list) {
                bw.write(String.join(",", r));
                bw.newLine();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update the gpa 
    public static void convertAndSave() {

        ArrayList<String[]> courseList = loadFile("courses.txt", true);
        ArrayList<String[]> recordList = loadFile("academicRecords.txt", true);

        for (String[] r : recordList) {

            String courseID = r[1].trim();
            int assMark  = Integer.parseInt(r[2]);
            int examMark = Integer.parseInt(r[3]);

            // find the course weights
            String[] course = findCourseRow(courseID, courseList);

            int assWeight  = Integer.parseInt(course[5]); // AssignmentWeight
            int examWeight = Integer.parseInt(course[6]); // ExamWeight

            // calculate weighted final mark
            double finalMark = (assMark * assWeight / 100.0) + (examMark * examWeight / 100.0);
            int rounded = (int)Math.round(finalMark);

            // convert to grade & GPA
            String grade = GradeConverter.getAlphabetGrade(rounded);
            double gpa   = GradeConverter.getGradePoint(grade);

            // update row (replace empty fields)
            r[4] = grade;
            r[5] = String.valueOf(gpa);
        }

        saveUpdatedRecords(recordList);
    }

    // ===================== MAIN =====================
    public static void main(String[] args) {
        convertAndSave(); // run STEP 1
    }
 
        
        
}