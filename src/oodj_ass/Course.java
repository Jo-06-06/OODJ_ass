package oodj_ass;

public class Course {
    private String courseID;
    private String courseName;
    private int creditHours;
    private String semester;
    private String instructor;
    private int assignmentWeight;
    private int examWeight;
    
    private int assignmentScore;
    private int examScore;
    private String grade;
    private int attemptNumber;;
    
    public Course(String courseID, String courseName, int creditHours,
              String semester, String instructor,
              int assignmentWeight, int examWeight) {
        this.courseID = courseID;
        this.courseName = courseName;
        this.creditHours = creditHours;
        this.semester = semester;
        this.instructor = instructor;
        this.assignmentWeight = assignmentWeight;
        this.examWeight = examWeight;
        this.assignmentScore = 0;
        this.examScore = 0;
        this.grade = "";
        this.attemptNumber = 1;
        }
    
    //Getters
    public String getCourseID() { 
        return courseID; 
    }
    
    public String getCourseName() { 
        return courseName; 
    }
    
    public int getCreditHours() {
        return creditHours;
    }
    
    public String getSemester() { 
        return semester; 
    }
    
    public String getCourseInstructor() {
        return instructor; 
    }
    
    public int getAssignmentWeight() { 
        return assignmentWeight; 
    }
    
    public int getExamWeight() { 
        return examWeight; 
    }
    
    public int getAssignmentScore() { 
        return assignmentScore; 
    }
    
    public int getExamScore() { 
        return examScore; 
    }
    
    public String getGrade() {
        return grade;
    }
    
    public int getAttemptNumber() {
        return attemptNumber;
    }
    
    //Setters
    public void setScores(int assignment, int exam) {
        this.assignmentScore = assignment;
        this.examScore = exam;
    }
    
    public void setGrade(String grade) {
        this.grade = grade;
    }
    
    public void setAttemptNumber(int attempt) {
        this.attemptNumber = attempt;
    }
    
    public boolean isFailed() {
        if (grade != null && !grade.isEmpty()){
            return grade.equals("F");
        }
        return (assignmentScore < 50) || (examScore < 50);
    }
    
    public String getFailedComponent() {
        boolean assignmentFailed = assignmentScore < 50;
        boolean examFailed = examScore < 50;
        
        if (assignmentFailed && examFailed) {
            return "Both Components";
        } else if (assignmentFailed) {
            return "Assignment Only";
        } else if (examFailed) {
            return "Exam Only";
        } else {
            return "None";
        }
    }
    
    public boolean isAssignmentFailed() {
        return assignmentScore < 50;
    }
    
    public boolean isExamFailed() {
        return examScore <50;
    }
    
    public String getRecoveryRequirement() {
        if (attemptNumber >= 3) {
            return "ALL Components (Full Course Retake)";
        } else {
            return getFailedComponent();
        }
    }
    
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("#######################################\n");
        info.append("COURSE: ").append(courseID).append(" - ").append(courseName).append("\n");
        info.append("Credit Hours: ").append(creditHours).append("\n");
        info.append("───────────────────────────────────────\n");
        info.append("SCORES:\n");
        info.append("  Assignment: ").append(assignmentScore).append("/100");
        if (assignmentScore < 50) info.append("FAILED");
        info.append("\n");
        info.append("  Exam:       ").append(examScore).append("/100");
        if (examScore < 50) info.append("FAILED");
        info.append("\n");
        
        if (!grade.isEmpty()) {
            info.append("  Final Grade: ").append(grade).append("\n");
        }
        
        info.append("───────────────────────────────────────\n");
        info.append("RECOVERY INFO:\n");
        info.append("Failed Component: ").append(getFailedComponent()).append("\n");
        info.append("Attempt Number: ").append(attemptNumber).append("\n");
        info.append("Recovery Requirement: ").append(getRecoveryRequirement()).append("\n");
        info.append("#######################################\n");
        
        return info.toString();
    }
}
