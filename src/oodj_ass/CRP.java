package oodj_ass;

import java.util.List;
import java.util.ArrayList;
import java.util.*;

public class CRP {
    private List<RecoveryPlan> recoveryPlans;
    private int planIDCounter;
    
    public CRP() {
        this.recoveryPlans = new ArrayList<>();
        this.planIDCounter = 1;
    }
    //---------1. List all failed courses------------
    public void listFailedComponenets(Student student) {
        List<Course> failedCourses = student.getFailedCourses();
        
        System.out.println("\n##############################################################");
        System.out.println("FAILED COMPONENTS FOR: " + student.getStudentID());
        System.out.println("Student ID: " + student.getStudentID());
        System.out.println("Major: " + student.getMajor());
        System.out.println("CGPA: " + String.format("%.2f", student.getCgpa()));
        System.out.println("##############################################################");
        
        if (failedCourses.isEmpty()) {
            System.out.println("No failed courses. Student is eligible to progress.");
            System.out.println("#############################################################");
            return;
        }
        System.out.println("\nTotal Failed Courses: " + failedCourses.size());
        System.out.println();
        
        // Table header
        System.out.printf("%-10s %-35s %-12s %-10s %-20s\n", 
            "Course", "Course Name", "Assignment", "Exam", "Failed Component");
        System.out.println("─".repeat(90));
        
        // Table rows
        for (Course course : failedCourses) {
            System.out.printf("%-10s %-35s %-12d %-10d %-20s\n",
                course.getCourseID(),
                course.getCourseName(),
                course.getAssignmentScore(),
                course.getExamScore(),
                course.getFailedComponent());
        }
        
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
    
    //Get failed courses as a List
    public List<Course> getFailedCourses(Student student) {
        return student.getFailedCourses();
    }
    
    public RecoveryPlan createRecoveryPlan(Student student, Course failedCourse) {
        String planID = String.format("RP%04d", planIDCounter++);
        RecoveryPlan plan = new RecoveryPlan(planID, student, failedCourse);
        //Add to list
        recoveryPlans.add(plan);
        return plan;
    }
    
    public void generatePlansForStudent(Student student) {
        System.out.println("\n=== Checking Recovery Needs for " + student + " ===");

        List<Course> failed = student.getFailedCourses();

        // Scenario 1,2,3
        if (!failed.isEmpty()) {
            failed.sort(Comparator.comparingDouble(course -> Student.getGradePoint(course.getGrade())));

            for (Course course : failed) {

                RecoveryPlan plan = createRecoveryPlan(student, course);

                int attempt = course.getAttemptNumber();
                String requirement = course.getRecoveryRequirement();

                autoGenerateMilestones(plan, attempt, requirement);

                System.out.println(plan.getDetailedInfo());
            }
            return;
        }

        // Scenario 4
        if (student.getCgpa() < 2.0) {
            Course weakest = student.getLowestGradeCourse();

            if (weakest != null) {
                RecoveryPlan plan = createRecoveryPlan(student, weakest);
                autoGenerateMilestones(plan, weakest.getAttemptNumber(), 
                                       "ALL Components (Full Course Retake)");

                System.out.println(plan.getDetailedInfo());
            }
        } else {
            System.out.println("Student is eligible to progress. No recovery needed.");
        }
    }
    
    private void autoGenerateMilestones(RecoveryPlan plan, int attempt, String requirement) {
        Course c = plan.getFailedCourse();
        String component = c.getFailedComponent();
    
        switch (attempt) {
            case 1:
                // Attempt 1: Component-specific recovery
                if (component.equals("Assignment Only")) {
                    plan.addMilestone("Week 1-2", "Review assignment feedback and requirements for " + c.getCourseID() + " " + c.getFailedComponent());
                    plan.addMilestone("Week 3", "Submit revised assignment for " + c.getFailedComponent());
                }
                else if (component.equals("Exam Only")) {
                    plan.addMilestone("Week 1-2", "Review course content");
                    plan.addMilestone("Week 3", "See lecturer if meeded");
                    plan.addMilestone("Week 4", "Resit examination");
                }
                else if (component.equals("Both Components")) {
                    plan.addMilestone("Week 1-2", "Work on assignment");
                    plan.addMilestone("Week 3-4", "Prepare for exam");
                    plan.addMilestone("Week 5", "Complete resit exam");
                }   break;
                
            case 2:
                // Attempt 2: Focused recovery
                plan.addMilestone("Week 1", "Lecturer consultation with " + c.getCourseInstructor());
                plan.addMilestone("Week 2-4", "Intensive preparation on " + c.getFailedComponent());
                plan.addMilestone("Week 5", "Complete assessment");
                break;
                
            default:
                // Attempt 3+: Full course retake
                plan.addMilestone("Week 1-3", "Re-attend " + c.getCourseID() +" classes again");
                plan.addMilestone("Week 4-6", "Complete all coursework");
                plan.addMilestone("Week 7-8", "Study for final exam");
                plan.addMilestone("Week 9", "Final examination");
                break;
        }

        // Short recommendation
        plan.setRecommendation(requirement + " (Attempt " + attempt + ")");
    }


    

    
    // Get plan by ID
    public RecoveryPlan getRecoveryPlan(String planID) {
        for (RecoveryPlan plan : recoveryPlans) {
            if(plan.getPlanID().equals(planID)) {
                return plan;
            }
        }
        return null;
    }
    
    //Get all plans for a specific student
    public List<RecoveryPlan> getRecoveryPansByStudent(String studentID) {
        List<RecoveryPlan> studentPlans = new ArrayList<>();
        for (RecoveryPlan plan : recoveryPlans) {
            if(plan.getStudent().getStudentID().equals(studentID)) {
                studentPlans.add(plan);
            }
        }
        return studentPlans;
    }
    
    //Get all recovery plans
    public List<RecoveryPlan> getAllRecoveryPlans() {
        return new ArrayList<>(recoveryPlans);
    }
    
    //Display all recovery plans
    public void displayAllRecoveryPlans() {
        System.out.println("\n#######################################");
        System.out.println("             ALL RECOVERY PLANS");
        System.out.println("#######################################");
        
        if (recoveryPlans.isEmpty()) {
            System.out.println("No recovery plans created yet.");
            System.out.println("═══════════════════════════════════════");
            return;
        }
        
        System.out.println("Total Plans: " + recoveryPlans.size());
        System.out.println();
        
        for (RecoveryPlan plan : recoveryPlans) {
            System.out.println(plan.toString());
        }
        
        System.out.println("-----------------------------------#");
    }
    
    //------------2. Recommendation--------------
    public boolean addRecommendation(String planID, String recommendation) {
        RecoveryPlan plan = getRecoveryPlan(planID);
        
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + "not found.");
            return false;
        }
        
        plan.setRecommendation(recommendation);
        System.out.println("Recommendation added to plan " + planID);
        return true;
    }
    
    //Update recommendation (overwrites)
    public boolean updateRecommendation(String planID, String newRecommendation) {
        RecoveryPlan plan = getRecoveryPlan(planID);
        
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + "not found.");
            return false;
        }
        String oldRec = plan.getRecommendation();
        plan.setRecommendation(newRecommendation);
        
        System.out.println("Recommendation updated for plan " + planID);
        System.out.println("Old: " + (oldRec.isEmpty() ? "(empty)" : oldRec.substring(0, Math.min(50, oldRec.length())) + "..."));
        System.out.println("New: " + newRecommendation.substring(0, Math.min(50, newRecommendation.length())) + "...");
        return true;
    }
    
    //remove recommendation
    public boolean removeRecommendation(String planID) {
        RecoveryPlan plan = getRecoveryPlan(planID);
        
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + " not found.");
            return false;
        }
        
        plan.setRecommendation("");
        System.out.println("Recommendation removed from plan " + planID);
        return true;
    } 
    
    //------------3. Milestone--------------
    public boolean addMilestone(String planID, String studyWeek, String task) {
        RecoveryPlan plan = getRecoveryPlan(planID);
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + " not found.");
            return false;
        }
        
        plan.addMilestone(studyWeek, task);
        System.out.println("Milestone added to plan " + planID);
        System.out.println("Week: " + studyWeek);
        System.out.println("Task: " + task);
        return true;
    }
    
    public boolean updateMilestone(String planID, int milestoneIndex, String newStudyWeek, String newTask) {
        RecoveryPlan plan = getRecoveryPlan(planID);
        
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + " not found.");
            return false;
        }
        
        if (plan.updateMilestone(milestoneIndex, newStudyWeek, newTask)) {
            System.out.println("Milestone " + milestoneIndex + " updated in plan " + planID);
            return true;
        } else {
            System.out.println("Error: Invalid milestone index " + milestoneIndex);
            return false;
        }
    }
    
    public boolean removeMilestone(String planID, int milestoneIndex) {
        RecoveryPlan plan = getRecoveryPlan(planID);
        
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + " not found.");
            return false;
        }
        if (plan.removeMilestone(milestoneIndex)) {
            System.out.println("Milestone " + milestoneIndex + " removed from plan " + planID);
            return true;
        } else {
            System.out.println("Error: Invalid milestone index " + milestoneIndex);
            return false;
        }
    }
    
    public void displayMilestones(String planId) {
        RecoveryPlan plan = getRecoveryPlan(planId);
        
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planId + " not found.");
            return;
        }
        
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("MILESTONES FOR PLAN: " + planId);
        System.out.println("Student: " + plan.getStudent().getFullName());
        System.out.println("Course: " + plan.getFailedCourse().getCourseID());
        System.out.println("═══════════════════════════════════════");
        
        List<RecoveryMilestone> milestones = plan.getMilestones();
        
        if (milestones.isEmpty()) {
            System.out.println("No milestones set yet.");
        } else {
            System.out.println();
            for (int i = 0; i < milestones.size(); i++) {
                System.out.println(i + ". " + milestones.get(i));
            }
            System.out.println();
            System.out.println("Progress: " + String.format("%.1f", plan.getProgressPercentage()) + "%");
        }
        
        System.out.println("═══════════════════════════════════════");
    }
    
    //------------4. Progress Tracking--------------
    public boolean markMilestoneCompleted(String planID, int milestoneIndex, String notes) {
        RecoveryPlan plan = getRecoveryPlan(planID);
        
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + " not found.");
            return false;
        }
        
        if (plan.markMilestoneCompleted(milestoneIndex, notes)) {
            System.out.println("Milestone " + milestoneIndex + "marked as completed.");
            System.out.println("Progress " + String.format("%.1f", plan.getProgressPercentage()) + "%");
            return true;
        } else {
            System.out.println("Error: Invalid milestone index " + milestoneIndex);
            return false;
        }
    }
    
    //Enter recovery grade for a plan
    public boolean enterRecoveryGrade(String planID, double grade) {
        RecoveryPlan plan = getRecoveryPlan(planID);

        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + " not found.");
            return false;
        }
        
        if (grade < 0 || grade > 100) {
            System.out.println("Error: Grade must be between 0 and 100.");
            return false;
        }
        
        plan.setRecoveryGrade(grade);
        
        if (grade >= 50) {
            plan.setStatus("COMPLETED - PASSED");
            System.out.println("Recovery grade entered: " + grade);
            System.out.println("Status: PASSED");
        } else {
            plan.setStatus("COMPLETED - FAILED");
            System.out.println("Recovery grade entered: " + grade);
            System.out.println("Status: FAILED");
        }
        return false;
    }
    
    //Track and Display progress for a recovery plan
    public void trackProgress(String planID) {
        RecoveryPlan plan = getRecoveryPlan(planID);

        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + " not found.");
            return;
        }
        
        System.out.println("\n#######################################");
        System.out.println("RECOVERY PROGRESS REPORT");
        System.out.println("#######################################");
        System.out.println("Plan ID: " + plan.getPlanID());
        System.out.println("Student: " + plan.getStudent().getFullName() + 
                          " (" + plan.getStudent().getStudentID() + ")");
        System.out.println("Course: " + plan.getFailedCourse().getCourseID() + 
                          " - " + plan.getFailedCourse().getCourseName());
        System.out.println("Failed Component: " + plan.getFailedCourse().getFailedComponent());
        System.out.println("Status: " + plan.getStatus());
        System.out.println();
        
        // Progress bar
        double progress = plan.getProgressPercentage();
        int bars = (int)(progress / 10);
        System.out.print("Progress: [");
        for (int i = 0; i < 10; i++) {
            System.out.print(i < bars ? "█" : "░");
        }
        System.out.println("] " + String.format("%.1f", progress) + "%");
        System.out.println();
        
        // Milestone breakdown
        System.out.println("Milestones: " + plan.getCompletedMilestoneCount() + 
                          "/" + plan.getMilestoneCount() + " completed");
        
        List<RecoveryMilestone> milestones = plan.getMilestones();
        for (int i = 0; i < milestones.size(); i++) {
            RecoveryMilestone m = milestones.get(i);
            System.out.println("  " + i + ". " + m);
        }
        
        System.out.println();
        
         // Recovery grade
        if (plan.getRecoveryGrade() != null) {
            System.out.println("Recovery Grade: " + String.format("%.2f", plan.getRecoveryGrade()));
        } else {
            System.out.println("Recovery Grade: Not yet entered");
        }
        
        System.out.println("═══════════════════════════════════════");
    }
    
    //Display complete recovery plan details
     public void displayRecoveryPlan(String planID) {
        RecoveryPlan plan = getRecoveryPlan(planID);
        
        if (plan == null) {
            System.out.println("Error: Recovery plan " + planID + " not found.");
            return;
        }
        
        System.out.println(plan.getDetailedInfo());
    }
     
     //Get total num of recovery plans
     public int getTotalPlans() {
         return recoveryPlans.size();
     }
     
     //Get plans by status
     public List<RecoveryPlan> getPlansByStatus(String status) {
        List<RecoveryPlan> filtered = new ArrayList<>();
        
        for (RecoveryPlan plan : recoveryPlans) {
            if (plan.getStatus().equals(status)) {
                filtered.add(plan);
            }
        }
        return filtered;
    }
}       
