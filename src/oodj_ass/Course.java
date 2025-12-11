package oodj_ass;

public class Course {
    private String courseID;
    private String courseName;
    private int creditHours;
    private String semester;
    private String instructor;
    private int assignmentWeight;
    private int examWeight;

    private int assScore;
    private int examScore;
    private String grade;
    private int attemptNumber;

    public Course(String courseID, String name, int creditHours,
                  String semester, String instructor,
                  int assignmentWeight, int examWeight) {

        this.courseID = courseID;
        this.courseName = name;
        this.creditHours = creditHours;
        this.semester = semester;
        this.instructor = instructor;
        this.assignmentWeight = assignmentWeight;
        this.examWeight = examWeight;

        this.assScore = 0;
        this.examScore = 0;
        this.grade = "";
        this.attemptNumber = 1;
    }

    // GETTERS
    public String getCourseID() { return courseID; }
    public String getCourseName() { return courseName; }
    public int getCreditHours() { return creditHours; }
    public String getSemester() { return semester; }
    public String getCourseInstructor() { return instructor; }

    public int getAssignmentWeight() { return assignmentWeight; }
    public int getExamWeight() { return examWeight; }

    public int getAssScore() { return assScore; }
    public int getExamScore() { return examScore; }
    public String getGrade() { return grade; }
    public int getAttemptNumber() { return attemptNumber; }

    // SETTERS
    public void setScores(int a, int e) {
        this.assScore = a;
        this.examScore = e;
    }

    public void setGrade(String g) { 
        this.grade = g; 
    }

    public void setAttemptNumber(int n) { 
        this.attemptNumber = n; 
    }

    // FAILURE CHECK
    public boolean isFailed() {
        boolean assFail = (assignmentWeight > 0 && assScore < 50);
        boolean examFail = (examWeight > 0 && examScore < 50);
        return assFail || examFail;
    }

    public String getFailedComponent() {
        boolean fAss = (assignmentWeight > 0 && assScore < 50);
        boolean fExam = (examWeight > 0 && examScore < 50);

        if (!fAss && !fExam) return "None";
        if (fAss && fExam) return "Both Components";
        return fAss ? "Assignment Only" : "Exam Only";
    }

    // GRADE SCALE
    public static String mapGradeFromMark(int mark) {
        if (mark >= 80) return "A+";
        else if (mark >= 75) return "A";
        else if (mark >= 70) return "B+";
        else if (mark >= 65) return "B";
        else if (mark >= 60) return "C+";
        else if (mark >= 55) return "C";
        else if (mark >= 50) return "C-";
        else if (mark >= 40) return "D";
        else if (mark >= 30) return "F+";
        else if (mark >= 20) return "F";
        return "F-";
    }

    // RECOVERY REQUIREMENT (UI use)
    public String getRecoveryRequirement() {
        if (attemptNumber >= 3) return "ALL Components (Full Retake)";
        return getFailedComponent();
    }
}
