package oodj_ass;

public class RecoveryMilestone {
    private String studyWeek;
    private String task;
    private boolean completed;
    private String notes;
    
    public RecoveryMilestone(String studyWeek, String task) {
        this.studyWeek = studyWeek;
        this.task = task;
        this.completed = false;
        this.notes = "";
    }
    
    public String getStudyWeek() {
        return studyWeek;
    }
    
    public String getTask() {
        return task;
    }
    
    public boolean isCompleted() {
        return completed;
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
        this.completed = completed;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public void markCompleted(String notes) {
        this.completed = true;
        this.notes = notes != null ? notes : "";
    }
    
    public void markCompleted() {
        markCompleted("Completed");
    } 
    
    public void markIncomplete() {
        this.completed = false;
        this.notes = "";
    }
    
    @Override 
    public String toString() {
        String checkbox = completed ? "[✓]" : "[ ]";
        return checkbox + " " + studyWeek + ": " + task;
    }
    
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Study Week : ").append(studyWeek).append("\n");
        sb.append("Task       : ").append(task).append("\n");
        sb.append("Status     : ").append(completed ? "COMPLETED" : "PENDING").append("\n");
        if (completed && notes != null && !notes.isEmpty()) {
            sb.append("Notes      : ").append(notes).append("\n");
            }
        return sb.toString();
    }
}
