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

    public static String[] findCourseRow(String courseID, ArrayList<String[]> courseList) {
        for (String[] row : courseList) {
            if (row[0].trim().equals(courseID)) {
                return row;
            }
        }
        return null;
    }

    public static void saveUpdatedRecords(ArrayList<String[]> list) {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("data/grades.txt"))) {

            bw.write("studentID,courseID,assScore,examScore,grade,gpa,attemptNum");
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

    ArrayList<String[]> courseList = loadFile("data/courses.txt", true);
    ArrayList<String[]> recordList = loadFile("data/grades.txt", true);

    for (int i = 0; i < recordList.size(); i++) {

        String[] r = recordList.get(i);

        r = Arrays.copyOf(r, 7);

        recordList.set(i, r);

        String courseID = r[1].trim();
        int assMark  = Integer.parseInt(r[2].trim());
        int examMark = Integer.parseInt(r[3].trim());

        String[] course = findCourseRow(courseID, courseList);

        int assWeight  = Integer.parseInt(course[5].trim());
        int examWeight = Integer.parseInt(course[6].trim());

        double finalMark = (assMark * assWeight / 100.0) + (examMark * examWeight / 100.0);
        int rounded = (int)Math.round(finalMark);

        String grade = GradeConverter.getAlphabetGrade(rounded);
        double gpa   = GradeConverter.getGradePoint(grade);

        r[4] = grade;
        r[5] = String.format("%.2f", gpa);
    }

    saveUpdatedRecords(recordList);
}
 
    //calculate cgpa 
    public static ArrayList<String[]> cgpaList = new ArrayList<>();

    public static void calculateCGPA() {

        ArrayList<String[]> records = loadFile("data/grades.txt", true);
        ArrayList<String[]> courses = loadFile("data/courses.txt", true);

        ArrayList<String> studentIDs = new ArrayList<>();
        cgpaList.clear(); // reset old results

        // Collect unique student IDs
        for (String[] r : records) {
            String sid = r[0];
            if (!studentIDs.contains(sid)) {
                studentIDs.add(sid);
            }
        }

        // Calculate CGPA for each student
        for (String studentID : studentIDs) {

            double totalGradePoints = 0;
            int totalCredits = 0;
            int failCount = 0;

            System.out.println("\n===== Student: " + studentID + " =====");

            for (String[] row : records) {

                if (!row[0].equals(studentID)) continue;

                String courseID = row[1];
                String grade = row[4];
                double gpa = Double.parseDouble(row[5]);

                String[] courseRow = findCourseRow(courseID, courses);
                int credit = Integer.parseInt(courseRow[2]);

                double gp = gpa * credit;

                totalGradePoints += gp;
                totalCredits += credit;

                if (grade.equals("D") || grade.equals("F+") || grade.equals("F") || grade.equals("F-")) {
                    failCount++;
                }

                System.out.println(
                    courseID + " | Grade: " + grade +
                    " (" + String.format("%.2f", gpa) + ") * " +
                    credit + " credits = " + String.format("%.2f", gp)
                );
            }

            double cgpa = totalCredits == 0 ? 0 : totalGradePoints / totalCredits;

            System.out.println("Total Grade Points = " + String.format("%.2f", totalGradePoints));
            System.out.println("Total Credit Hours = " + totalCredits);
            System.out.println("CGPA = " + String.format("%.2f", cgpa));

            boolean eligible = (cgpa >= 2.0 && failCount <= 3);

            System.out.println("Failed courses = " + failCount);
            System.out.println("Eligible Next Year: " + (eligible ? "YES" : "NO"));

            // Store CGPA (backend)
            cgpaList.add(new String[]{
                studentID,
                String.format("%.2f", cgpa)
            });
        }
    }

    //Save CGPA
    public static void saveCGPA() {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("data/result.txt"))) {

            bw.write("StudentID,CGPA");
            bw.newLine();

            for (String[] row : cgpaList) {
                bw.write(row[0] + "," + row[1]);
                bw.newLine();
            }

        } catch (Exception e) {
        }
    }

 
    // ===================== MAIN =====================
    public static void main(String[] args) {
        convertAndSave(); 
        calculateCGPA();
        saveCGPA(); 
    }
        
}