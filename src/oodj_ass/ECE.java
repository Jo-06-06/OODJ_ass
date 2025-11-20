/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oodj_ass;

/**
 *
 * @author jolin
 */
public class ECE {
    
}

public class GradeConverter {

    // Convert marks → alphabetical grade
    public static String getAlphabetGrade(int marks) {
        if (marks >= 80 && marks <= 100) return "A+";
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

    // Convert alphabetical grade → GPA
    public static double getGradePoint(String grade) {
        switch (grade) {
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
        }
        return 0.0;
    }

    // Convert marks → GPA directly
    public static double marksToGPA(int marks) {
        String grade = getAlphabetGrade(marks);
        return getGradePoint(grade);
    }
}
