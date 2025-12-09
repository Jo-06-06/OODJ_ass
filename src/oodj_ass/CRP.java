package oodj_ass;

import java.io.*;
import java.util.*;

public class CRP {
    private static final String PLAN_FILE_PATH      = "data/recoveryPlans.txt";
    private static final String MILESTONE_FILE_PATH = "data/recoveryMilestones.txt";
    private static final String GRADES_FILE_PATH    = "data/grades.txt";
    private static final String RESULT_FILE_PATH    = "data/result.txt";

    private final List<Student> studentPool;
    private final List<RecoveryPlan> planList = new ArrayList<>();
    private final Map<String, RecoveryPlan> planIndex = new LinkedHashMap<>();

    private int nextPlanNumber = 0;
    private Email mailer;
    
    public CRP(List<Student> students, Email mailer) {
        this.studentPool = (students != null) ? students : new ArrayList<>();
        this.mailer = mailer;

        loadRecoveryPlans();
        loadPlanMilestones();
        initialisePlanCounter();
        
    }

    // PLAN ID MANAGEMENT
    private void initialisePlanCounter() {
        int highest = 0;
        for (RecoveryPlan rp : planList) {
            String id = rp.getPlanID();
            if (id != null && id.startsWith("RP")) {
                try {
                    int n = Integer.parseInt(id.substring(2));
                    if (n > highest) highest = n;
                } catch (NumberFormatException ignore) {
                    // ignore bad IDs
                }
            }
        }
        this.nextPlanNumber = highest;
    }

    private String generatePlanID() {
        nextPlanNumber++;
        return String.format("RP%04d", nextPlanNumber);
    }

    // FILE LOADING - PLANS
    private void loadRecoveryPlans() {
        planList.clear();
        planIndex.clear();

        File f = new File(PLAN_FILE_PATH);
        if (!f.exists()) {
            System.out.println("No existing recoveryPlans.txt found (first run).");
            return;
        }

        System.out.println("Loading recoveryPlans.txt...");

        int loaded = 0;
        int skipped = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String header = br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",", -1);
                // planID,studentID,courseID,attemptNum,failureType,status,
                // recoveryGrade,createdDate,lastUpdated,recommendation
                if (p.length < 9) {
                    skipped++;
                    System.err.println("  Skipping invalid plan row: " + line);
                    continue;
                }

                try {
                    String planId       = p[0].trim();
                    String sid          = p[1].trim();
                    String cid          = p[2].trim();
                    int attemptNum      = Integer.parseInt(p[3].trim());
                    // String failureType  = p[4].trim(); 
                    String status       = p[5].trim();
                    String recGradeText = p[6].trim();
                    String createdDate  = p[7].trim();
                    String lastUpdated  = p[8].trim();
                    String recommendation = (p.length >= 10) ? p[9] : "";

                    Student s = findStudent(sid);
                    Course  c = findStudentCourse(s, cid);

                    if (s == null || c == null) {
                        skipped++;
                        System.err.println("  Skipping plan (student/course not found): " + line);
                        continue;
                    }

                    // keep attempt number consistent with file
                    c.setAttemptNumber(attemptNum);

                    RecoveryPlan rp = new RecoveryPlan(planId, s, c);
                    rp.setStatus(status);
                    if (!recGradeText.isEmpty()) {
                        try {
                            rp.setRecoveryGrade(Double.parseDouble(recGradeText));
                        } catch (NumberFormatException ignore) {
                            // ignore invalid stored value
                        }
                    }
                    if (!createdDate.isEmpty()) {
                        rp.setCreatedDateRaw(createdDate);
                    }
                    if (!lastUpdated.isEmpty()) {
                        rp.setLastUpdatedRaw(lastUpdated);
                    }
                    if (recommendation != null && !recommendation.isEmpty()) {
                        rp.setRecommendation(recommendation);
                    }

                    planList.add(rp);
                    planIndex.put(planId, rp);
                    loaded++;

                } catch (Exception ex) {
                    skipped++;
                    System.err.println("  Error parsing plan row: " + line);
                }
            }

        } catch (IOException ioEx) {
            System.err.println("Failed to load recoveryPlans.txt: " + ioEx.getMessage());
        }

        System.out.println("Recovery plans loaded: " + loaded + " (Skipped: " + skipped + ")");
    }

    // FILE LOADING - MILESTONES
    private void loadPlanMilestones() {
        File f = new File(MILESTONE_FILE_PATH);
        if (!f.exists()) {
            System.out.println("No existing recoveryMilestones.txt found (first run).");
            return;
        }

        System.out.println("Loading recoveryMilestones.txt...");

        int loaded = 0;
        int skipped = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String header = br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",", -1);
                // planID,studyWeek,task,isCompleted,notes
                if (p.length < 4) {
                    skipped++;
                    System.err.println("  Skipping invalid milestone row: " + line);
                    continue;
                }

                String planId       = p[0].trim();
                String studyWeek    = p[1].trim();
                String task         = p[2].trim();
                boolean isCompleted = Boolean.parseBoolean(p[3].trim());
                String notes        = (p.length >= 5) ? p[4] : "";

                RecoveryPlan rp = planIndex.get(planId);
                if (rp == null) {
                    skipped++;
                    continue;
                }

                // *** IMPORTANT: do not use rp.addMilestone() here
                // to avoid changing status when loading – just attach directly.
                RecoveryMilestone ms = new RecoveryMilestone(studyWeek, task);
                if (isCompleted) {
                    ms.markCompleted(notes);
                } else {
                    ms.setNotes(notes);
                }
                rp.getMilestones().add(ms);

                loaded++;
            }

        } catch (IOException ioEx) {
            System.err.println("Failed to load recoveryMilestones.txt: " + ioEx.getMessage());
        }

        System.out.println("Milestones loaded: " + loaded + " (Skipped: " + skipped + ")");
    }

    // FILE SAVING
    public void saveRecoveryPlans() {
        try (PrintWriter out = new PrintWriter(new FileWriter(PLAN_FILE_PATH))) {

            out.println("planID,studentID,courseID,attemptNum,failureType,status," +
                        "recoveryGrade,createdDate,lastUpdated,recommendation");

            for (RecoveryPlan rp : planList) {
                String line = String.join(",",
                        rp.getPlanID(),
                        rp.getStudent().getStudentID(),
                        rp.getCourse().getCourseID(),
                        String.valueOf(rp.getCourse().getAttemptNumber()),
                        rp.getCourse().getFailedComponent(),
                        rp.getStatus(),
                        (rp.getRecoveryGrade() == null ? "" : rp.getRecoveryGrade().toString()),
                        rp.getCreatedDate(),
                        rp.getLastUpdated(),
                        rp.getRecommendation().replace(",", " ")
                );
                out.println(line);
            }

        } catch (IOException ioEx) {
            System.err.println("Error saving recoveryPlans.txt: " + ioEx.getMessage());
        }
    }

    public void savePlanMilestones() {
        try (PrintWriter out = new PrintWriter(new FileWriter(MILESTONE_FILE_PATH))) {

            out.println("planID,studyWeek,task,isCompleted,notes");

            for (RecoveryPlan rp : planList) {
                for (RecoveryMilestone ms : rp.getMilestones()) {
                    String line = String.join(",",
                            rp.getPlanID(),
                            ms.getStudyWeek(),
                            ms.getTask().replace(",", " "),
                            String.valueOf(ms.isCompleted()),
                            ms.getNotes().replace(",", " ")
                    );
                    out.println(line);
                }
            }

        } catch (IOException ioEx) {
            System.err.println("Error saving recoveryMilestones.txt: " + ioEx.getMessage());
        }
    }

    // HELPERS - SEARCH
    private Student findStudent(String id) {
        if (id == null) return null;
        for (Student s : studentPool) {
            if (id.equals(s.getStudentID())) return s;
        }
        return null;
    }

    private Course findStudentCourse(Student s, String courseId) {
        if (s == null || courseId == null) return null;
        for (Course c : s.getCourses()) {
            if (courseId.equals(c.getCourseID())) return c;
        }
        return null;
    }

    // PLAN CREATION (AUTO)
    public RecoveryPlan createRecoveryPlan(Student stu, Course course) {
        if (stu == null || course == null) return null;

        if (!course.isFailed() || "None".equals(course.getFailedComponent())) {
            return null;
        }

        String id = generatePlanID();
        RecoveryPlan rp = new RecoveryPlan(id, stu, course);

        // milestones will be auto-generated by CRP (not here)

        planList.add(rp);
        planIndex.put(id, rp);

        // auto-generate default milestones
        autoGenerateMilestones(rp);

        saveRecoveryPlans();
        savePlanMilestones();

        sendRecoveryPlanEmail(rp);
        
        return rp;
    }

    /**
     * Automatically create recommended milestones based on attempt + failed component.
     */
    private void autoGenerateMilestones(RecoveryPlan rp) {
        Course c = rp.getCourse();
        String failedComponent = c.getFailedComponent();
        int attempt = c.getAttemptNumber();
        String cid = c.getCourseID();
        String clect = c.getCourseInstructor();

        // Basic intro step
        rp.addMilestone("Week 1", "Discuss recovery plan for " + cid +
                " with lecturer " + clect + " and confirm required components.");

        // Attempt >= 3 → treat as full retake
        if (attempt >= 3) {
            rp.addMilestone("Week 2-4", "Re-attend classes / revision sessions for " + cid);
            rp.addMilestone("Week 5-6", "Complete all recovery coursework and continuous assessment.");
            rp.addMilestone("Week 7-8", "Intensive exam preparation for full retake of " + cid);
            rp.addMilestone("Week 9",   "Sit for final examination or final assessment for " + cid);
            return;
        }

        // Targeted recovery
        switch (failedComponent) {
            case "Assignment Only":
                rp.addMilestone("Week 2-3", "Revise assignment requirements and correct previous mistakes.");
                rp.addMilestone("Week 4",   "Submit improved assignment / project for " + cid);
                break;

            case "Exam Only":
                rp.addMilestone("Week 2-3", "Study weak topics using notes and past-year questions.");
                rp.addMilestone("Week 4",   "Sit for recovery exam for " + cid);
                break;

            case "Both Components":
                rp.addMilestone("Week 2-3", "Revise coursework and clarify doubts with lecturer.");
                rp.addMilestone("Week 4-5", "Prepare for recovery exam while working on assignment.");
                rp.addMilestone("Week 6",   "Complete both coursework submission and exam.");
                break;

            case "None":
            default:
                // should not really happen because createRecoveryPlan guards "None"
                break;
        }
    }

    // RECOMMENDATION MANAGEMENT
    public boolean updateRecommendation(String planId, String newText) {
        RecoveryPlan rp = planIndex.get(planId);
        if (rp == null) {
            System.out.println("Plan not found.");
            return false;
        }
        rp.setRecommendation(newText != null ? newText : "");
        saveRecoveryPlans();
        return true;
    }

    public boolean clearRecommendation(String planId) {
        return updateRecommendation(planId, "");
    }

    // MILESTONE MANAGEMENT
    public boolean addCustomMilestone(String planId, String week, String task) {
        RecoveryPlan rp = planIndex.get(planId);
        if (rp == null) {
            System.out.println("Plan not found.");
            return false;
        }
        rp.addMilestone(week, task);
        savePlanMilestones();
        saveRecoveryPlans();
        return true;
    }

    public boolean modifyMilestone(String planId, int index,
                                   String newWeek, String newTask) {
        RecoveryPlan rp = planIndex.get(planId);
        if (rp == null) {
            System.out.println("Plan not found.");
            return false;
        }
        boolean ok = rp.updateMilestone(index, newWeek, newTask);
        if (ok) {
            savePlanMilestones();
            saveRecoveryPlans();
        }
        return ok;
    }

    public boolean removeMilestone(String planId, int index) {
        RecoveryPlan rp = planIndex.get(planId);
        if (rp == null) {
            System.out.println("Plan not found.");
            return false;
        }
        boolean ok = rp.removeMilestone(index);
        if (ok) {
            savePlanMilestones();
            saveRecoveryPlans();
        }
        return ok;
    }

    public void displayPlanMilestones(String planId) {
        RecoveryPlan rp = planIndex.get(planId);
        if (rp == null) {
            System.out.println("Plan not found.");
            return;
        }

        System.out.println("\n════════════════════════════════════════");
        System.out.println("MILESTONES for Plan " + planId);
        System.out.println("Student: " + rp.getStudent().getFullName());
        System.out.println("Course : " + rp.getCourse().getCourseID());
        System.out.println("════════════════════════════════════════");

        List<RecoveryMilestone> list = rp.getMilestones();
        if (list.isEmpty()) {
            System.out.println("No milestones defined yet.");
        } else {
            for (int i = 0; i < list.size(); i++) {
                System.out.println(i + ". " + list.get(i));
            }
            System.out.println("Progress: " +
                    String.format("%.1f", rp.getProgressPercentage()) + "%");
        }
        System.out.println("════════════════════════════════════════");
    }

    public boolean markMilestoneCompleted(String planId, int index, String notes) {
        RecoveryPlan rp = planIndex.get(planId);
        if (rp == null) {
            System.out.println("Plan not found.");
            return false;
        }

        boolean ok = rp.markMilestoneCompleted(index, notes);
        if (ok) {
            savePlanMilestones();
            saveRecoveryPlans();
            System.out.println("Milestone " + index + " marked as completed.");
        }
        sendRecoveryPlanEmail(rp);
        return ok;
    }

    // ENTER RECOVERY GRADE (NEW ATTEMPT)
    /**
     * Enter recovery grade (0-100) for a plan.
     *
     * Rules:
     * - We DO NOT delete old attempts from grades.txt
     * - We append a NEW row with attemptNum = currentAttempt + 1
     * - For any recovered component (ass or exam), score is capped at 50
     * - RecoveryPlan.recoveryGrade keeps the *raw* mark entered by staff
     * - Then we recalc CGPA and update result.txt
     */
    public boolean enterRecoveryGrade(String planId, double rawScore) {
        if (rawScore < 0 || rawScore > 100) {
            System.out.println("Error: Recovery mark must be between 0 and 100.");
            return false;
        }

        RecoveryPlan rp = planIndex.get(planId);
        if (rp == null) {
            System.out.println("Error: Plan ID not found.");
            return false;
        }

        Student stu  = rp.getStudent();
        Course  c    = rp.getCourse();

        // Original scores
        int oldAss  = c.getAssScore();
        int oldExam = c.getExamScore();

        boolean assFailed  = (c.getAssignmentWeight() > 0 && oldAss  < 50);
        boolean examFailed = (c.getExamWeight()      > 0 && oldExam < 50);

        // New scores (with cap 50 for recovered components)
        int newAss  = oldAss;
        int newExam = oldExam;

        if (assFailed)  newAss  = 50;  // **** CRITICAL: recovery component capped at 50
        if (examFailed) newExam = 50;  // **** CRITICAL: recovery component capped at 50

        // Weighted final mark
        double finalMark =
                (newAss  * c.getAssignmentWeight() / 100.0) +
                (newExam * c.getExamWeight() / 100.0);

        // Map to grade + GPA
        String newGrade = GradeScaleHelper.getAlphabetFromMark((int)Math.round(finalMark));
        double newGpa   = Student.getGradePoint(newGrade);

        int newAttempt = c.getAttemptNumber() + 1;

        // Update in-memory course object
        c.setScores(newAss, newExam);
        c.setGrade(newGrade);
        c.setAttemptNumber(newAttempt);

        // Update plan (store rawScore, but grade in file is capped)
        rp.setRecoveryGrade(rawScore);

        if (!c.isFailed() && newGpa >= 2.0) {
            rp.setStatus("COMPLETED-PASSED");
        } else {
            rp.setStatus("COMPLETED-FAILED");
        }

        // Append NEW attempt record into grades.txt
        String newRow = stu.getStudentID() + "," +
                        c.getCourseID()    + "," +
                        c.getSemester()    + "," +
                        newAss             + "," +
                        newExam            + "," +
                        newGrade           + "," +
                        String.format("%.2f", newGpa) + "," +
                        newAttempt;

        appendSingleLine(GRADES_FILE_PATH, newRow);

        // Recalculate CGPA + update result.txt
        recalculateCgpaAndResult(stu);

        // Persist CRP files (status, recoveryGrade, timestamps, etc.)
        saveRecoveryPlans();
        savePlanMilestones();

        System.out.println("Recovery mark successfully recorded and grade updated.");
        return true;
    }

    // CGPA & RESULT.TXT UPDATE
    private void recalculateCgpaAndResult(Student stu) {
        double totalPoints = 0.0;
        int totalCredits   = 0;

        for (Course c : stu.getCourses()) {
            String g = c.getGrade();
            if (g == null || g.isEmpty()) continue;

            double gp = Student.getGradePoint(g);
            totalPoints += gp * c.getCreditHours();
            totalCredits += c.getCreditHours();
        }

        double newCgpa = (totalCredits > 0) ? (totalPoints / totalCredits) : 0.0;
        stu.setCgpa(newCgpa);

        String eligibility = (newCgpa >= 2.0 && stu.getFailedCourses().isEmpty())
                ? "YES" : "NO";

        List<String> newLines = new ArrayList<>();
        boolean replaced = false;

        File f = new File(RESULT_FILE_PATH);

        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String header = br.readLine();
                if (header != null && !header.trim().isEmpty()) {
                    newLines.add(header);
                } else {
                    newLines.add("studentID,semester,CGPA,eligibility");
                }

                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] p = line.split(",", -1);
                    if (p.length < 4) {
                        newLines.add(line);
                        continue;
                    }

                    String sid = p[0].trim();
                    String sem = p[1].trim();

                    if (sid.equals(stu.getStudentID()) &&
                        sem.equals(stu.getCurrentSemester())) {

                        String updated = sid + "," + sem + "," +
                                String.format("%.2f", newCgpa) + "," +
                                eligibility;
                        newLines.add(updated);
                        replaced = true;
                    } else {
                        newLines.add(line);
                    }
                }

            } catch (IOException ioEx) {
                System.err.println("Error reading result.txt: " + ioEx.getMessage());
            }
        } else {
            // Initial header if file did not exist
            newLines.add("studentID,semester,CGPA,eligibility");
        }

        if (!replaced) {
            String row = stu.getStudentID() + "," +
                         stu.getCurrentSemester() + "," +
                         String.format("%.2f", newCgpa) + "," +
                         eligibility;
            newLines.add(row);
        }

        rewriteWholeFile(RESULT_FILE_PATH, newLines);
    }

    // FILE HELPERS
    private void appendSingleLine(String filePath, String line) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath, true))) {
            out.println(line);
        } catch (IOException ioEx) {
            System.err.println("Failed to append to " + filePath + ": " + ioEx.getMessage());
        }
    }

    private void rewriteWholeFile(String filePath, List<String> lines) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath))) {
            for (String l : lines) {
                out.println(l);
            }
        } catch (IOException ioEx) {
            System.err.println("Failed to write file " + filePath + ": " + ioEx.getMessage());
        }
    }

    // DISPLAY / ACCESSORS
    public void displayAllRecoveryPlans() {
        System.out.println("\n========== ALL RECOVERY PLANS ==========");
        if (planList.isEmpty()) {
            System.out.println("No recovery plans available.");
        } else {
            for (RecoveryPlan rp : planList) {
                System.out.println(rp.getSummary());
            }
        }
        System.out.println("========================================");
    }

    public RecoveryPlan getPlanById(String id) {
        return planIndex.get(id);
    }

    public List<RecoveryPlan> getAllPlans() {
        return new ArrayList<>(planList);
    }

    // =========================
    // GRADE SCALE HELPER
    // =========================
    /**
     * Uses your mapping:
     * 80-100 : A+ (4.0)
     * 75-79  : A  (3.7)
     * 70-74  : B+ (3.3)
     * 65-69  : B  (3.0)
     * 60-64  : C+ (2.7)
     * 55-59  : C  (2.3)
     * 50-54  : C- (2.0)
     * 40-49  : D  (1.7)
     * 30-39  : F+ (1.3)
     * 20-29  : F  (1.0)
     * 0-19   : F- (0.0)
     */
    private static class GradeScaleHelper {
        public static String getAlphabetFromMark(int mark) {
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
            else return "F-";
        }
    }


    private void sendRecoveryPlanEmail(RecoveryPlan rp) {

        String to   = rp.getStudent().getEmail();       // student email address
        String name = rp.getStudent().getFullName();
        String cid  = rp.getCourse().getCourseID();

        String subject = "Your Course Recovery Plan for " + cid;

        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(name).append(",\n\n");
        body.append("Your recovery plan for the course ").append(cid)
            .append(" has been created.\n\n");
        body.append("Milestones:\n");

        for (RecoveryMilestone m : rp.getMilestones()) {
            body.append("- ").append(m.getStudyWeek())
                .append(": ").append(m.getTask())
                .append(" [").append(m.isCompleted() ? "Completed" : "Pending")
                .append("]\n");
        }

        body.append("\nPlease follow your recovery plan to stay on track.\n");
        body.append("\nRegards,\nCourse Recovery System");

        mailer.sendEmail(to, subject, body.toString());
    }
}

