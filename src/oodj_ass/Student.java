package oodj_ass;

import java.util.List;
import java.util.ArrayList;

public class Student {
    private String studentID;
    private String firstName;
    private String lastName;
    private String major;
    private String email;
    private double cgpa;
    private List<Course> courses;
    
    public Student(String studentID, String firstName,String lastName) {
        this.studentID = studentID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.courses = new ArrayList<>();
    }
    
    public String getStudentID() {
        return studentID;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public String getMajor() {
        return major;
    }
    
    public String getEmail() {
        return email;
    }
    
    public double getCgpa() {
        return cgpa;
    }
    
    public List<Course> getCourses() {
        return courses;
    }
    
    public int getFailedCourseCount() {
        return getFailedCourses().size();
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }
    
    public List<Course> getFailedCourses() {
        List<Course> failedCourses = new ArrayList<>();
        for (Course course : courses) {
            if (course.isFailed()) {
                failedCourses.add(course);
            }
        }
        return failedCourses;
    }
    
    public Course getLowestGradeCourse() {
        Course lowest =  null;
        for (Course course : courses) {
            if (lowest == null) {
                lowest = course;
            } else if (getGradePoint(course.getGrade()) < getGradePoint(lowest.getGrade())) {
                lowest = course;
            }
        }
        return lowest;
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
    
    public boolean isRecoveryCandidate() {
        return(cgpa < 2.0) || (!getFailedCourses().isEmpty());
    }
    
    @Override
    public String toString() {
        return studentID + " - " + getFullName() + 
               " (CGPA: " + String.format("%.2f", cgpa) + ")";
    }
}
