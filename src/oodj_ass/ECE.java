package oodj_ass;

import java.io.*;
import java.util.*;

public class ECE {

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

        // convert gpa to alphabet
        public static String getAlphabetGrade(double gpa) {

            if (gpa == 4.0) return "A+";
            else if (gpa >= 3.7) return "A";
            else if (gpa >= 3.3) return "B+";
            else if (gpa >= 3.0) return "B";
            else if (gpa >= 2.7) return "C+";
            else if (gpa >= 2.3) return "C";
            else if (gpa >= 2.0) return "C-";
            else return "F"; 
        }
    }

    // load files
    public static ArrayList<String[]> loadFile(String file, boolean header) {
        ArrayList<String[]> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("data/" + file))) {
            String line;
            if (header) br.readLine();

            while ((line = br.readLine()) != null)
                list.add(line.split(","));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void saveGrades(ArrayList<String[]> list) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("data/grades.txt"))) {

            bw.write("studentID,courseID,semester,assScore,examScore,grade,gpa,attemptNum\n");

            for (String[] r : list)
                bw.write(String.join(",", r) + "\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveResults(ArrayList<String[]> list) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("data/result.txt"))) {

            bw.write("studentID,semester,CGPA,eligibility\n");

            for (String[] r : list)
                bw.write(String.join(",", r) + "\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // course
    public static String[] findCourse(String courseID, ArrayList<String[]> courses) {
        for (String[] c : courses)
            if (c[0].equals(courseID))
                return c;
        return null;
    }

    // update grade
    public static void updateGrades() {

        ArrayList<String[]> grades = loadFile("grades.txt", true);
        ArrayList<String[]> studentInfo = loadFile("studentInfo.txt", true);
        ArrayList<String[]> courses = loadFile("courses.txt", true);
        
        //match studentid with current semester
        HashMap<String, String> currentSemMap = new HashMap<>();
        for (String[] s : studentInfo)
            currentSemMap.put(s[0], s[1]); 

        for (String[] g : grades) {

            String sid = g[0];
            String sem = g[2];

            // skip past semesters
            if (!currentSemMap.containsKey(sid)) continue;
            if (!currentSemMap.get(sid).equals(sem)) continue;

            // course data
            String courseID = g[1];
            String[] c = findCourse(courseID, courses);
            if (c == null) continue;

            int assW = Integer.parseInt(c[5]);
            int examW = Integer.parseInt(c[6]);

            int ass = Integer.parseInt(g[3]);
            int exam = Integer.parseInt(g[4]);

            double finalMark = (ass * assW / 100.0) + (exam * examW / 100.0);
            int rounded = (int) Math.round(finalMark);

            double gpa = GradeConverter.getGradePoint(rounded);
            String alpha = GradeConverter.getAlphabetGrade(gpa);

            g[5] = alpha;
            g[6] = String.format("%.2f", gpa);
        }

        saveGrades(grades);
    }

    // calculate cgpa
    public static void calculateCGPA() {

        ArrayList<String[]> grades = loadFile("grades.txt", true);
        ArrayList<String[]> studentInfo = loadFile("studentInfo.txt", true);
        ArrayList<String[]> courses = loadFile("courses.txt", true);

        ArrayList<String[]> results = new ArrayList<>();

        for (String[] s : studentInfo) {

            String sid = s[0];
            String currentSem = s[1];

            double totalGP = 0;
            int totalCH = 0;
            int failCount = 0;

            for (String[] g : grades) {

                if (!g[0].equals(sid)) continue;
                if (!g[2].equals(currentSem)) continue;

                String courseID = g[1];
                String[] c = findCourse(courseID, courses);
                if (c == null) continue;

                int credit = Integer.parseInt(c[2]);
                double gpa = Double.parseDouble(g[6]);

                totalGP += gpa * credit;
                totalCH += credit;

                if (gpa < 2.0)
                    failCount++;
            }

            double cgpa = totalCH == 0 ? 0 : totalGP / totalCH;
            boolean eligible = (cgpa >= 2.0 && failCount <= 3);

            results.add(new String[]{
                    sid,
                    currentSem,
                    String.format("%.2f", cgpa),
                    eligible ? "YES" : "NO"
            });
        }

        saveResults(results);
    }

}


// =============================== MAIN ================================

//    public static void main(String[] args) {
//        updateGrades();
//        calculateCGPA();
//    }
//}
