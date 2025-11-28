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

            String grade = GradeConverter.getAlphabetGrade(finalMark);
            double gpa = GradeConverter.getGradePoint(grade);

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

