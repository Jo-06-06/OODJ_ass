package oodj_ass;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RecoveryPlan {
    private String planID;
    private Student student;
    private Course course;
    private String recommendation;
    private List<RecoveryMilestone> milestones;

    /** PENDING → IN_PROGRESS → AWAITING_GRADE → COMPLETED-PASSED / COMPLETED-FAILED */
    private String status;
    private Double recoveryGrade;
    private String createdDate;
    private String lastUpdated;

    public RecoveryPlan(String planID, Student student, Course failedCourse) {
        this.planID = planID;
        this.student = student;
        this.course = failedCourse;

        this.milestones = new ArrayList<>();
        this.status = "PENDING";
        this.recoveryGrade = null;

        String nowText = now();
        this.createdDate = nowText;
        this.lastUpdated = nowText;

        // **** NEW: auto-generate a clear, initial recommendation text
        this.recommendation = buildInitialRecommendation();
    }

    public String getPlanID() {
        return planID;
    }

    public Student getStudent() {
        return student;
    }

    public Course getCourse() {
        return course;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public List<RecoveryMilestone> getMilestones() {
        return milestones;
    }

    public String getStatus() {
        return status;
    }

    public Double getRecoveryGrade() {
        return recoveryGrade;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

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

    // **** OPTIONAL (for loading from file: CRP can use these to keep timestamps) ****
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

    // MILESTONE MANAGEMENT
    public void addMilestone(RecoveryMilestone milestone) {
        if (milestone == null) return;

        milestones.add(milestone);

        // When first milestone is created, move PENDING → IN_PROGRESS
        if ("PENDING".equals(status) && !milestones.isEmpty()) {
            this.status = "IN_PROGRESS";
        }
        updateTimestamp();
    }

    public void addMilestone(String studyWeek, String task) {
        addMilestone(new RecoveryMilestone(studyWeek, task));
    }

    public boolean updateMilestone(int index, String newStudyWeek, String newTask) {
        if (index < 0 || index >= milestones.size()) {
            return false;
        }
        RecoveryMilestone milestone = milestones.get(index);
        milestone.setStudyWeek(newStudyWeek);
        milestone.setTask(newTask);
        updateTimestamp();
        return true;
    }

    public boolean removeMilestone(int index) {
        if (index < 0 || index >= milestones.size()) {
            return false;
        }
        milestones.remove(index);
        updateTimestamp();
        return true;
    }

    /**
     * Mark a specific milestone as completed with optional notes.
     * **** FIXED INDEX CHECK: previously used (index < 0 && index >= size) which was always false.
     */
    public boolean markMilestoneCompleted(int index, String notes) {
        if (index < 0 || index >= milestones.size()) {
            return false;
        }

        milestones.get(index).markCompleted(notes);

        // Auto-update status if all milestones are completed
        if (areAllMilestonesCompleted() && !"COMPLETED-PASSED".equals(status)
                                        && !"COMPLETED-FAILED".equals(status)) {
            this.status = "AWAITING_GRADE";
        }

        updateTimestamp();
        return true;
    }

    public boolean areAllMilestonesCompleted() {
        if (milestones.isEmpty()) return false;

        for (RecoveryMilestone milestone : milestones) {
            if (!milestone.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public int getMilestoneCount() {
        return milestones.size();
    }

    public int getCompletedMilestoneCount() {
        int count = 0;
        for (RecoveryMilestone milestone : milestones) {
            if (milestone.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public double getProgressPercentage() {
        if (milestones.isEmpty()) return 0.0;
        return (getCompletedMilestoneCount() * 100.0) / milestones.size();
    }

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

        sb.append("Progress: ").append(String.format("%.1f", getProgressPercentage())).append("%\n");
        sb.append("Completed: ").append(getCompletedMilestoneCount())
          .append("/").append(getMilestoneCount()).append("\n");

        if (recoveryGrade != null) {
            sb.append("Recovery Grade: ").append(String.format("%.2f", recoveryGrade)).append("\n");
        }

        sb.append("Created: ").append(createdDate).append("\n");
        sb.append("Last Updated: ").append(lastUpdated).append("\n");
        sb.append("--------------------------------------------------\n");

        return sb.toString();
    }

    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSummary());

        // Recommendation
        if (recommendation != null && !recommendation.isEmpty()) {
            sb.append("\nRECOMMENDATION:\n");
            sb.append(recommendation).append("\n");
        }

        // Milestones
        sb.append("\nMILESTONES:\n");
        sb.append("-----------------------------------------------\n");

        if (milestones.isEmpty()) {
            sb.append("No milestones set.\n");
        } else {
            for (int i = 0; i < milestones.size(); i++) {
                sb.append((i + 1)).append(". ").append(milestones.get(i).toString()).append("\n");
            }
        }

        sb.append("-----------------------------------------------\n");
        return sb.toString();
    }

    private void updateTimestamp() {
        this.lastUpdated = now();
    }

    private String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * **** NEW: Auto-generate a clear, human recommendation based on:
     * - failed component type
     * - attempt number
     * - student CGPA risk (simple heuristic)
     */
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

            case "None":
            default:
                rec.append(course.getCourseID())
                   .append(". No failed component is detected at this moment. ")
                   .append("This plan can be used to monitor additional support activities if needed.");
                break;
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
               .append("therefore additional follow-up sessions and mentoring are strongly encouraged.");
        }

        return rec.toString();
    }

    @Override
    public String toString() {
        return String.format("Plan %s: %s - %s [%s]",
                planID,
                student.getFullName(),
                course.getCourseID(),
                status);
    }
}
