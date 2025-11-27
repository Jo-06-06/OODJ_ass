package oodj_ass;

public class RecoveryMilestone {
    private String studyWeek;
    private String task;
    private boolean isCompleted;
    public String notes;
    
    public RecoveryMilestone(String studyWeek, String task) {
        this.studyWeek = studyWeek;
        this.task = task;
        this.isCompleted = false;
        this.notes = "";
    }
    
    public String getStudyWeek() {
        return studyWeek;
    }
    
    public String getTask() {
        return task;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setStudyWeek(String studyWeek) {
        this.studyWeek = studyWeek;
    }
    
    public void setTask(String task) {
        this.task = task;
    }
    
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public void markCompleted(String notes) {
        this.isCompleted = true;
        this.notes = notes;
    }
    
    public void markCompleted() {
        this.isCompleted = true;
        this.notes = "Completed";
    }
    
    public void markIncomplete() {
        this.isCompleted = false;
        this.notes = "";
    }
    @Override 
    public String toString() {
        String checkbox = isCompleted ? "[✓]" : "[ ]";
        return checkbox + " " + studyWeek + ": " + task;
    }
    
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Study Week: ").append(studyWeek).append("\n");
        info.append("Task: ").append(task).append("\n");
        info.append("Status: ").append(isCompleted ? "✓ COMPLETED" : "○ PENDING").append("\n");
        
        if (isCompleted && notes != null && !notes.isEmpty()) {
            info.append("Notes: ").append(notes).append("\n");
        }
        
        return info.toString();
    }
    
    public String getStatusIcon() {
        return isCompleted ? "✓" : "○";
    }
    
    public String getStatusText() {
        return isCompleted ? "COMPLETED" : "PENDING";
    }
}
