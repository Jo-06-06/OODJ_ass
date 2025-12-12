package oodj_ass;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RecoveryPlan {
    private String status;
    private final String planID;
    private final Student student;
    private final Course course;

    private String recommendation;
    private List<RecoveryMilestone> milestones;

    private Double recoveryGrade;
    private String recoveryGradeString;
    private String createdDate;
    private String lastUpdated;

    public RecoveryPlan(String planID, Student student, Course failedCourse) {
        this.planID  = planID;
        this.student = student;
        this.course  = failedCourse;

        this.milestones    = new ArrayList<>();
        this.status        = "PENDING";
        this.recoveryGrade = null;

        String nowText = now();
        this.createdDate = nowText;
        this.lastUpdated = nowText;

        // Auto-generate an initial recommendation
        this.recommendation = buildInitialRecommendation();
    }

    // Getters
    public String getPlanID()            { return planID; }
    public Student getStudent()          { return student; }
    public Course getCourse()            { return course; }
    public String getRecommendation()    { return recommendation; }
    public List<RecoveryMilestone> getMilestones() { return milestones; }
    public String getStatus()            { return status; }
    public Double getRecoveryGrade()     { return recoveryGrade; }
    public String getCreatedDate()       { return createdDate; }
    public String getLastUpdated()       { return lastUpdated; }

    // Setters
    public void setRecommendation(String recommendation) {
        this.recommendation = (recommendation != null ? recommendation : "");
        updateTimestamp();
    }

    public void setStatus(String status) {
        this.status = status;
        updateTimestamp();
    }

    public void setRecoveryGrade(Double grade) {
        this.recoveryGrade = grade;
        updateTimestamp();
    }
    
    public void setRecoveryGradeString(String s) {
        this.recoveryGradeString = (s == null ? "" : s);
        updateTimestamp();
    }

    public String getRecoveryGradeString() {
        return recoveryGradeString;
    }

    // used only when loading from file – no validation
    public void setCreatedDateRaw(String createdDate) {
        if (createdDate != null && !createdDate.trim().isEmpty()) {
            this.createdDate = createdDate.trim();
        }
    }

    public void setLastUpdatedRaw(String lastUpdated) {
        if (lastUpdated != null && !lastUpdated.trim().isEmpty()) {
            this.lastUpdated = lastUpdated.trim();
        }
    }

    // Milestone management
    public void addMilestone(RecoveryMilestone milestone) {
        if (milestone == null) return;

        milestones.add(milestone);

        // First milestone created: PENDING → IN_PROGRESS
        if ("PENDING".equals(status) && !milestones.isEmpty()) {
            this.status = "IN_PROGRESS";
        }
        updateTimestamp();
    }

    public void addMilestone(String studyWeek, String task) {
        addMilestone(new RecoveryMilestone(studyWeek, task));
    }

    public boolean updateMilestone(int index, String newStudyWeek, String newTask) {
        if (index < 0 || index >= milestones.size()) return false;

        RecoveryMilestone m = milestones.get(index);
        m.setStudyWeek(newStudyWeek);
        m.setTask(newTask);

        updateTimestamp();
        return true;
    }

    public boolean removeMilestone(int index) {
        if (index < 0 || index >= milestones.size()) return false;

        milestones.remove(index);
        updateTimestamp();
        return true;
    }

    /**
     * Mark a specific milestone as completed with optional notes.
     * Status will auto-move to AWAITING_GRADE if all milestones are done.
     */
    public boolean markMilestoneCompleted(int index, String notes) {
        if (index < 0 || index >= milestones.size()) return false;

        milestones.get(index).markCompleted(notes);

        // Auto-update status if all milestones are completed
        if (areAllMilestonesCompleted()
                && !"COMPLETED-PASSED".equals(status)
                && !"COMPLETED-FAILED".equals(status)) {
            this.status = "AWAITING_GRADE";
        }

        updateTimestamp();
        return true;
    }

    public boolean areAllMilestonesCompleted() {
        if (milestones.isEmpty()) return false;
        for (RecoveryMilestone m : milestones) {
            if (!m.isCompleted()) return false;
        }
        return true;
    }

    public int getMilestoneCount() {
        return milestones.size();
    }

    public int getCompletedMilestoneCount() {
        int count = 0;
        for (RecoveryMilestone m : milestones) {
            if (m.isCompleted()) count++;
        }
        return count;
    }

    public double getProgressPercentage() {
        if (milestones.isEmpty()) return 0.0;
        return (getCompletedMilestoneCount() * 100.0) / milestones.size();
    }

    // Serialization helpers (CSV)
    public void setMilestones(List<RecoveryMilestone> newList) {
        this.milestones = (newList != null ? newList : new ArrayList<>());
        updateTimestamp();
    }

    public List<String> toMilestoneCsvLines() {
        List<String> lines = new ArrayList<>();
        for (RecoveryMilestone m : milestones) {
            lines.add(
                planID + "," +
                m.getStudyWeek() + "," +
                m.getTask().replace(",", " ") + "," +
                m.isCompleted() + "," +
                (m.getNotes() == null ? "" : m.getNotes().replace(",", " "))
            );
        }
        return lines;
    }

    // Default milestones
    public void generateDefaultMilestones() {
        milestones.clear();

        String failType = course.getFailedComponent();
        int attempt     = course.getAttemptNumber();
        int wAss        = course.getAssignmentWeight();
        int wExam       = course.getExamWeight();

        // FYP-style: 100% assignment
        if (wAss == 100 && wExam == 0) {
            addMilestone("Week 1-2",   "Meet supervisor and clarify project requirements.");
            addMilestone("Week 3-5",   "Submit initial project outline or proposal draft.");
            addMilestone("Week 6",     "Mid-progress checkpoint with supervisor.");
            addMilestone("Week 10",    "Submit final report or final deliverables.");
            addMilestone("Week 11-12", "Prepare presentation / documentation and final submission.");
            return;
        }

        // Attempt ≥ 3 → full retake
        if (attempt >= 3) {
            addMilestone("Week 1",   "Register for full course retake and review syllabus.");
            addMilestone("Week 2",   "Attend first consultation to plan retake strategy.");
            addMilestone("Week 3-5", "Submit compulsory coursework with improvement plan.");
            addMilestone("Week 6-11","Attend revision session for overall course topics.");
            addMilestone("Week 12",  "Attempt final assessment for retake course.");
            return;
        }

        // Normal cases (based on failed component)
        switch (failType) {
            case "Assignment Only":
                addMilestone("Week 1", "Review assignment feedback and identify weak areas.");
                addMilestone("Week 2", "Attend academic consultation session.");
                addMilestone("Week 3", "Submit improved assignment draft for lecturer review.");
                addMilestone("Week 4", "Submit final assignment reattempt.");
                break;

            case "Exam Only":
                addMilestone("Week 1", "Analyse exam mistakes and revise core chapters.");
                addMilestone("Week 2", "Attend revision or tutorial class for exam topics.");
                addMilestone("Week 3", "Complete 3 sets of past-year practice questions.");
                addMilestone("Week 4", "Attempt mock test and evaluate performance.");
                break;

            case "Both Components":
                addMilestone("Week 1", "Review feedback for both assignment and exam.");
                addMilestone("Week 2-3", "Attend consultation for coursework + exam planning.");
                addMilestone("Week 4", "Submit coursework improvement draft.");
                addMilestone("Week 5-6", "Complete revision of exam chapters and attempt practice test.");
                addMilestone("Week 7", "Final assignment submission + exam reattempt preparation.");
                break;

            default:
                // if somehow failType is "None" just keep empty or one generic milestone
                break;
        }
    }

    // Text summary helpers
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        sb.append("--------------------------------------------------\n");
        sb.append("RECOVERY PLAN: ").append(planID).append("\n");
        sb.append("--------------------------------------------------\n");
        sb.append("Student: ").append(student.getFullName())
          .append(" (").append(student.getStudentID()).append(")\n");
        sb.append("Course: ").append(course.getCourseID())
          .append(" - ").append(course.getCourseName()).append("\n");
        sb.append("Attempt: ").append(course.getAttemptNumber()).append("\n");
        sb.append("Failed Component: ").append(course.getFailedComponent()).append("\n");
        sb.append("Requirement: ").append(course.getRecoveryRequirement()).append("\n");
        sb.append("Status: ").append(status).append("\n\n");

        sb.append("Progress: ")
          .append(String.format("%.1f", getProgressPercentage()))
          .append("%\n");
        sb.append("Completed: ")
          .append(getCompletedMilestoneCount())
          .append("/")
          .append(getMilestoneCount())
          .append("\n");

        if (recoveryGrade != null) {
            sb.append("Recovery Grade: ")
              .append(String.format("%.2f", recoveryGrade))
              .append("\n");
        }

        sb.append("Created: ").append(createdDate).append("\n");
        sb.append("Last Updated: ").append(lastUpdated).append("\n");
        sb.append("--------------------------------------------------\n");

        return sb.toString();
    }

    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSummary());

        if (recommendation != null && !recommendation.isEmpty()) {
            sb.append("\nRECOMMENDATION:\n");
            sb.append(recommendation).append("\n");
        }

        sb.append("\nMILESTONES:\n");
        sb.append("-----------------------------------------------\n");
        if (milestones.isEmpty()) {
            sb.append("No milestones set.\n");
        } else {
            for (int i = 0; i < milestones.size(); i++) {
                sb.append(i + 1).append(". ")
                  .append(milestones.get(i).toString())
                  .append("\n");
            }
        }
        sb.append("-----------------------------------------------\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Plan %s: %s - %s [%s]",
                planID,
                student.getFullName(),
                course.getCourseID(),
                status);
    }

    // Internal helpers
    private void updateTimestamp() {
        this.lastUpdated = now();
    }

    private String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /** Builds initial recommendation text based on component, attempt, and CGPA. */
    private String buildInitialRecommendation() {
        String failedComponent = course.getFailedComponent();
        int attempt = course.getAttemptNumber();
        double cgpa = student.getCgpa();

        StringBuilder rec = new StringBuilder();
        rec.append("Student ").append(student.getFullName())
           .append(" is advised to focus on ");

        switch (failedComponent) {
            case "Assignment Only":
                rec.append("improving assignment performance for ")
                   .append(course.getCourseID())
                   .append(" by attending consultation, revising feedback, ")
                   .append("and submitting higher quality written work.");
                break;
            case "Exam Only":
                rec.append("exam preparation for ")
                   .append(course.getCourseID())
                   .append(" through extra past-year practice, revision classes, ")
                   .append("and better time management during the exam.");
                break;
            case "Both Components":
                rec.append("both coursework and examination for ")
                   .append(course.getCourseID())
                   .append(". The student should strengthen understanding of core topics, ")
                   .append("seek lecturer guidance, and plan weekly study tasks to close the gap.");
                break;
            default:
                rec.append(course.getCourseID())
                   .append(". No failed component is detected at this moment. ")
                   .append("This plan can be used to monitor additional support activities if needed.");
        }

        rec.append(" ");

        if (attempt >= 3) {
            rec.append("Since this is attempt ")
               .append(attempt)
               .append(", a full course retake is recommended with close monitoring of progress.");
        } else {
            rec.append("This is attempt ")
               .append(attempt)
               .append(", so targeted recovery on the failed component(s) is sufficient at this stage.");
        }

        if (cgpa > 0 && cgpa < 2.0) {
            rec.append(" The student is currently at academic risk (CGPA below 2.00), ")
               .append("so additional follow-up sessions and mentoring are strongly encouraged.");
        }

        return rec.toString();
    }
}
