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

    // load file
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
    
    public static void saveFile(String filename, String header, ArrayList<String[]> data) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            bw.write(header);
            bw.newLine();
            for (String[] row : data) {
                bw.write(String.join(",", row));
                bw.newLine();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static String[] findCourse(String id, ArrayList<String[]> courses) {
        for (String[] c : courses)
            if (c[0].equals(id)) return c;
        return null;
    }

    public static String getCourseSem(String id, ArrayList<String[]> courses) {
        for (String[] c : courses)
            if (c[0].equals(id)) return c[3];
        return "";
    }

    public static String getCurrentSem(String sid, ArrayList<String[]> info) {
        String last = "";
        for (String[] s : info)
            if (s[0].equals(sid)) last = s[1];
        return last;
    }
    
    //update grade and gpa 
    public static void updateGrades() {

        ArrayList<String[]> records = loadFile("data/grades.txt", true);
        ArrayList<String[]> courses = loadFile("data/courses.txt", true);

        for (String[] r : records) {

            String courseID = r[1];
            int ass = Integer.parseInt(r[2]);
            int exam = Integer.parseInt(r[3]);

            String[] c = findCourse(courseID, courses);

            int assW = Integer.parseInt(c[5]);
            int examW = Integer.parseInt(c[6]);

            double finalMark = (ass * assW / 100.0) + (exam * examW / 100.0);
            int round = (int) Math.round(finalMark);

            String grade = GradeConverter.getAlphabetGrade(round);
            double gpa = GradeConverter.getGradePoint(grade);

            r[4] = grade;
            r[5] = String.format("%.2f", gpa);
        }

        saveFile("data/grades.txt",
                "studentID,courseID,assScore,examScore,grade,gpa,attemptNum",
                records);
    }
    
    //calculate cgpa
    public static double calculateSemesterCGPA(String sid, String sem, ArrayList<String[]> grades, ArrayList<String[]> courses) {

        double totalGP = 0;
        int totalCH = 0;

        for (String[] row : grades) {
            if (!row[0].equals(sid)) continue;

            String courseID = row[1];
            if (!getCourseSem(courseID, courses).equals(sem)) continue;

            String[] c = findCourse(courseID, courses);
            int credit = Integer.parseInt(c[2]);

            double gpa = Double.parseDouble(row[5]);
            totalGP += gpa * credit;
            totalCH += credit;
        }

        if (totalCH == 0) return 0;
        return totalGP / totalCH;
    }
    
    //update cgpa
    public static void updateCGPA() {

        ArrayList<String[]> result = loadFile("data/result.txt", true);
        ArrayList<String[]> grades = loadFile("data/grades.txt", true);
        ArrayList<String[]> courses = loadFile("data/courses.txt", true);

        for (int i = 0; i < result.size(); i++) {

            String[] r = result.get(i);

            if (r.length < 3)
                r = Arrays.copyOf(r, 3);

            String sid = r[0];
            String sem = r[1];

            double cgpa = calculateSemesterCGPA(sid, sem, grades, courses);
            r[2] = String.format("%.2f", cgpa);

            result.set(i, r);
        }

        saveFile("data/result.txt",
                "StudentID,Semester,CGPA",
                result);
    }
    
    //display eligibility
    public static void displayEligibility() {

        ArrayList<String[]> grades = loadFile("data/grades.txt", true);
        ArrayList<String[]> courses = loadFile("data/courses.txt", true);
        ArrayList<String[]> info = loadFile("data/studentInfo.txt", true);

        HashSet<String> students = new HashSet<>();
        for (String[] r : grades) students.add(r[0]);

        for (String sid : students) {

            String sem = getCurrentSem(sid, info);

            double totalGP = 0;
            int totalCH = 0;
            int fail = 0;

            System.out.println("\n===== Student: " + sid + " (" + sem + ") =====");

            for (String[] r : grades) {

                if (!r[0].equals(sid)) continue;

                String courseID = r[1];
                if (!getCourseSem(courseID, courses).equals(sem)) continue;

                double gpa = Double.parseDouble(r[5]);
                String grade = r[4];
                int credit = Integer.parseInt(findCourse(courseID, courses)[2]);
                double gp = gpa * credit;

                totalGP += gp;
                totalCH += credit;

                if (grade.equals("D") || grade.equals("F+") || grade.equals("F") || grade.equals("F-"))
                    fail++;

                System.out.println(courseID + " | Grade: " + grade +
                        " (" + gpa + ") * " + credit + " = " + String.format("%.2f", gp));
            }

            double cgpa = totalCH == 0 ? 0 : totalGP / totalCH;

            System.out.println("Total Grade Points = " + String.format("%.2f", totalGP));
            System.out.println("Total Credits = " + totalCH);
            System.out.println("CGPA = " + String.format("%.2f", cgpa));

            boolean eligible = (cgpa >= 2.0 && fail <= 3);
            System.out.println("Eligible Next Year: " + (eligible ? "YES" : "NO"));
        }
    }
    
    // ===================== MAIN =====================
    public static void main(String[] args) {
        updateGrades();
        updateCGPA();
        displayEligibility();
    }
        
}