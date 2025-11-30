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
    // convert mark to gpa
       public static class GradeConverter {
        // convert mark to gpa
        public static double getGradePoint(int marks) {

            if (marks >= 80) return 4.0;
            else if (marks >= 75) return 3.7;
            else if (marks >= 70) return 3.3;
            else if (marks >= 65) return 3.0;
            else if (marks >= 60) return 2.7;
            else if (marks >= 55) return 2.3;
            else if (marks >= 50) return 2.0;
            else if (marks >= 40) return 1.7;
            else if (marks >= 30) return 1.3;
            else if (marks >= 20) return 1.0;
            else return 0.0;
        }

        //convert gpa to alphabet
        public static String getAlphabetGrade(double gpa) {

            if (gpa >= 4.0) return "A+";
            else if (gpa >= 3.7) return "A";
            else if (gpa >= 3.3) return "B+";
            else if (gpa >= 3.0) return "B";
            else if (gpa >= 2.7) return "C+";
            else if (gpa >= 2.3) return "C";
            else if (gpa >= 2.0) return "C-";
            else return "F";
        }
    }
       

    // Load file
    public static ArrayList<String[]> loadFile(String filename, boolean hasHeader) {
        ArrayList<String[]> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;

            if (hasHeader) br.readLine();

            while ((line = br.readLine()) != null) {
                list.add(line.split(","));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
       
    // find course row
    public static String[] findCourse(String cid, ArrayList<String[]> courses) {
        for (String[] c : courses) {
            if (c[0].equals(cid)) return c;
        }
        return null;
    }
    
    //update grade and gpa
       public static void updateCurrentGrades() {

        ArrayList<String[]> courses = loadFile("data/courses.txt", true);
        ArrayList<String[]> grades = loadFile("data/grades.txt", true);

        for (String[] g : grades) {

            String courseID = g[1];
            int ass = Integer.parseInt(g[2]);
            int exam = Integer.parseInt(g[3]);

            String[] c = findCourse(courseID, courses);

            int assW = Integer.parseInt(c[5]);
            int examW = Integer.parseInt(c[6]);

            int finalMark = (int)Math.round((ass * assW / 100.0) + (exam * examW / 100.0));

            double gpa = GradeConverter.getGradePoint(finalMark);
            String grade = GradeConverter.getAlphabetGrade(gpa);

            g[4] = grade;
            g[5] = String.format("%.2f", gpa);
        }

        // save back
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("data/grades.txt"))) {
            bw.write("studentID,courseID,assScore,examScore,grade,gpa,attemptNum");
            bw.newLine();

            for (String[] g : grades) {
                bw.write(String.join(",", g));
                bw.newLine();
            }

        } catch (Exception e) {}
    }
    
    //calculate cgpa
    public static void calculateCurrentCGPA() {

        ArrayList<String[]> grades = loadFile("data/grades.txt", true);
        ArrayList<String[]> courses = loadFile("data/courses.txt", true);
        ArrayList<String[]> studentInfo = loadFile("data/studentInfo.txt", true);

        ArrayList<String[]> resultList = new ArrayList<>();
        
        // get current studentID + semester
        for (String[] s : studentInfo) {

        String sid = s[0];
        String sem = s[1];

        double totalGP = 0;
        int totalCH = 0;
        int failCount = 0;

        System.out.println("\n===== Student: " + sid + " (" + sem + ") =====");

        for (String[] g : grades) {

            if (!g[0].equals(sid)) continue;

            String courseID = g[1];
            String grade = g[4];
            double gpa = Double.parseDouble(g[5]);

            // match semester using courses file
            String[] c = findCourse(courseID, courses);
            if (!c[3].equals(sem)) continue;

            int credit = Integer.parseInt(c[2]);
            double gp = gpa * credit;

            totalGP += gp;
            totalCH += credit;

            if (gpa < 2.0) failCount++;

            System.out.println(
                courseID + " | Grade: " + grade +
                " (" + g[5] + ") * " + credit +
                " = " + String.format("%.2f", gp)
            );
        }

        double cgpa = totalCH == 0 ? 0 : totalGP / totalCH;

        System.out.println("Total Grade Points = " + String.format("%.2f", totalGP));
        System.out.println("Total Credits = " + totalCH);
        System.out.println("CGPA = " + String.format("%.2f", cgpa));
        System.out.println("Total Fails = " + failCount);

        boolean eligible = (cgpa >= 2.0 && failCount <= 3);
        System.out.println("Eligible Next Year: " + (eligible ? "YES" : "NO"));

        // store to result
        resultList.add(new String[]{
                sid,
                sem,
                String.format("%.2f", cgpa),
                (eligible ? "YES" : "NO")
        });
    }
        
        // save result
    try (BufferedWriter bw = new BufferedWriter(new FileWriter("data/result.txt"))) {
        bw.write("studentID,semester,CGPA,eligibility");
        bw.newLine();

        for (String[] r : resultList) {
            bw.write(String.join(",", r));
            bw.newLine();
        }
    } catch (Exception e) {}
}

    // ===================== MAIN =====================
    public static void main(String[] args) {
        updateCurrentGrades();
        calculateCurrentCGPA();
    }
        
}

