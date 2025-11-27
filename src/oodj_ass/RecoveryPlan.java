package oodj_ass;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RecoveryPlan {
    private String planID;
    private Student student;
    private Course failedCourse;
    private String recommendation;
    private List<RecoveryMilestone> milestones;
    private String status;
    private Double recoveryGrade;
    private String createdDate;
    private String lastUpdated;
    
    public RecoveryPlan(String planID, Student student, Course failedCourse) {
        this.planID = planID;
        this.student = student;
        this.failedCourse = failedCourse;
        this.milestones = new ArrayList<>();
        this.status = "PENDING";
        this.recommendation = "";
        this.recoveryGrade = null;
        this.createdDate = getCurrentDateTime();
        this.lastUpdated = getCurrentDateTime();
    }
    
     public String getPlanID() {
        return planID;
    }
    
    public Student getStudent() {
        return student;
    }
    
    public Course getFailedCourse() {
        return failedCourse;
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
        this.recommendation = recommendation;
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
    
    public void addMilestone(RecoveryMilestone milestone) {
        milestones.add(milestone);
        //Update status if first milestone added
        if (status.equals("PENDING") && (!milestones.isEmpty())) {
            this.status = "IN_PROGRESS";
        }
        updateTimestamp();
    }
    
    public void addMilestone(String studyWeek, String task) {
        RecoveryMilestone milestone = new RecoveryMilestone(studyWeek, task);
        addMilestone(milestone);
    }
    
    public boolean updateMilestone(int index, String newStudyWeek, String newTask) {
        if (index >= 0 && index < milestones.size()) {
            RecoveryMilestone milestone = milestones.get(index);
            milestone.setStudyWeek(newStudyWeek);
            milestone.setTask(newTask);
            updateTimestamp();
            return true;
        }
        return false;
    }
    
    public boolean removeMilestone(int index) {
        if (index >= 0 && index < milestones.size()) {
            milestones.remove(index);
            updateTimestamp();
            return true;
        }
        return false;
    }
    

    public RecoveryMilestone getMilestone(int index) {
        if (index >= 0 && index < milestones.size()) {
            return milestones.get(index);
        }
        return null;
    }
    
    public int getMilestoneCount() {
        return milestones.size();
    }
    
    public double getProgressPercentage() {
        if(milestones.isEmpty()) {
            return 0.0;
        }
        int completedCount = 0;
        for (RecoveryMilestone milestone : milestones) {
            if (milestone.isCompleted()) {
                completedCount++;
            }
        }
        return (completedCount * 100.0) / milestones.size();
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
    
    public boolean areAllMilestonesCompleted() {
        if (milestones.isEmpty()) {
            return false;
        }
        for (RecoveryMilestone milestone : milestones) {
            if (!milestone.isCompleted()) {
                return false;
            }
        }
        return true;
    }
    // mark a specific milestone as completed
    public boolean markMilestoneCompleted(int index, String notes) {
        if (index >= 0 && index < milestones.size()) {
            milestones.get(index).markCompleted(notes);
            
            // Auto-update status if all milestones completed
            if (areAllMilestonesCompleted()) {
                this.status = "AWAITING_GRADE";
            }
            
            updateTimestamp();
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("Plan %s: %s - %s [%s]", 
            planID, 
            student.getFullName(), 
            failedCourse.getCourseID(), 
            status);
    }
    
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("-------------------------------------------------\n");
        summary.append("RECOVERY PLAN: ").append(planID).append("\n");
        summary.append("-------------------------------------------------\n");
        summary.append("Student: ").append(student.getFullName())
               .append(" (").append(student.getStudentID()).append(")\n");
        summary.append("Course: ").append(failedCourse.getCourseID())
               .append(" - ").append(failedCourse.getCourseName()).append("\n");
        summary.append("Failed Component: ").append(failedCourse.getFailedComponent()).append("\n");
        summary.append("Status: ").append(status).append("\n");
        summary.append("Progress: ").append(String.format("%.1f", getProgressPercentage())).append("%");
        summary.append(" (").append(getCompletedMilestoneCount()).append("/")
               .append(getMilestoneCount()).append(" completed)\n");
        
        if (recoveryGrade != null) {
            summary.append("Recovery Grade: ").append(String.format("%.2f", recoveryGrade)).append("\n");
        }
        
        summary.append("Created: ").append(createdDate).append("\n");
        summary.append("Last Updated: ").append(lastUpdated).append("\n");
        summary.append("======================================\n");
        
        return summary.toString();
    }
    
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        
        info.append(getSummary());
        
        // Recommendation
        if (recommendation != null && !recommendation.isEmpty()) {
            info.append("\nRECOMMENDATION:\n");
            info.append(recommendation).append("\n");
        }
        
        // Milestones
        info.append("\nACTION PLAN (MILESTONES):\n");
        info.append("───────────────────────────────────────\n");
        
        if (milestones.isEmpty()) {
            info.append("No milestones set yet.\n");
        } else {
            for (int i = 0; i < milestones.size(); i++) {
                RecoveryMilestone m = milestones.get(i);
                info.append(String.format("%d. %s\n", i + 1, m.toString()));
            }
        }
        
        info.append("───────────────────────────────────────\n");
        
        return info.toString();
    }
    
    private void updateTimestamp() {
        this.lastUpdated = getCurrentDateTime();
    }
     
    private String getCurrentDateTime() {        
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
  
}
