package oodj_ass;

public class Course {
    // from courses.txt
    private String courseID;
    private String courseName;
    private int creditHours;
    private String semester;
    private String instructor;
    private int assignmentWeight;
    private int examWeight;

    // from grades.txt
    private int assScore;
    private int examScore;
    private String grade;
    private int attemptNumber;

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

        this.assScore = 0;
        this.examScore = 0;
        this.grade = "";
        this.attemptNumber = 1;
    }

    // ===== GETTERS =====
    public String getCourseID() { return courseID; }
    public String getCourseName() { return courseName; }
    public int getCreditHours() { return creditHours; }
    public String getSemester() { return semester; }
    public String getCourseInstructor() { return instructor; }
    public int getAssignmentWeight() { return assignmentWeight; }
    public int getExamWeight() { return examWeight; }

    // Student-specific
    public int getAssScore() { return assScore; }
    public int getExamScore() { return examScore; }
    public String getGrade() { return grade; }
    public int getAttemptNumber() { return attemptNumber; }

    // ===== SETTERS =====
    public void setScores(int assignment, int exam) {
        this.assScore = assignment;
        this.examScore = exam;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public void setAttemptNumber(int attempt) {
        this.attemptNumber = attempt;
    }

    // Check failed
    public boolean isFailed() {

        boolean assignmentFailed = (assignmentWeight > 0 && assScore < 50);
        boolean examFailed       = (examWeight > 0 && examScore < 50);

        // If NONE of the weighted components fail → course is passed
        return assignmentFailed || examFailed;
    }

    // FAILED COMPONENT DETECTION
    public String getFailedComponent() {

        boolean failAss = (assignmentWeight > 0 && assScore < 50);
        boolean failExam = (examWeight > 0 && examScore < 50);

        if (!failAss && !failExam) return "None";
        if (failAss && failExam) return "Both Components";
        if (failAss) return "Assignment Only";
        return "Exam Only";
    }

    // ==========================================================
    // RECOVERY REQUIREMENT
    // ==========================================================
    public String getRecoveryRequirement() {
        if (attemptNumber >= 3) {
            return "ALL Components (Full Course Retake)";
        }
        return getFailedComponent();
    }

    // For UI
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();

        info.append("#######################################\n");
        info.append("COURSE: ").append(courseID).append(" - ").append(courseName).append("\n");
        info.append("Credit Hours: ").append(creditHours).append("\n");
        info.append("Lecturer: ").append(instructor).append("\n");
        info.append("Semester Offered: ").append(semester).append("\n");
        info.append("---------------------------------------\n");
        info.append("Scores:\n");

        // Assignment
        info.append("  Assignment: ").append(assScore).append("/100");
        if (assignmentWeight > 0 && assScore < 50) info.append("  (FAILED)");
        info.append("\n");

        // Exam (only mark failed if weighted)
        info.append("  Exam:       ").append(examScore).append("/100");
        if (examWeight > 0 && examScore < 50) info.append("  (FAILED)");
        info.append("\n");

        if (!grade.isEmpty()) {
            info.append("  Final Grade: ").append(grade).append("\n");
        }

        info.append("---------------------------------------\n");
        info.append("Failed Component: ").append(getFailedComponent()).append("\n");
        info.append("Attempt Number: ").append(attemptNumber).append("\n");
        info.append("Recovery Requirement: ").append(getRecoveryRequirement()).append("\n");
        info.append("---------------------------------------\n");

        return info.toString();
        }
}
