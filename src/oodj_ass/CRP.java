package oodj_ass;

import java.io.*;
import java.util.*;

public class CRP {
    private static final String PLAN_FILE_PATH = "data/recoveryPlans.txt";
    private static final String MILESTONE_FILE_PATH = "data/recoveryMilestones.txt";
    private static final String GRADES_FILE_PATH = "data/grades.txt";
    private static final String RESULT_FILE_PATH = "data/result.txt";

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

    // PLAN ID HANDLING
    private void initialisePlanCounter() {
        int highest = 0;
        for (RecoveryPlan rp : planList) {
            String id = rp.getPlanID();
            if (id != null && id.startsWith("RP")) {
                try {
                    int n = Integer.parseInt(id.substring(2));
                    if (n > highest) highest = n;
                } catch (NumberFormatException ignore) {}
            }
        }
        this.nextPlanNumber = highest;
    }

    private String generatePlanID() {
        nextPlanNumber++;
        return String.format("RP%04d", nextPlanNumber);
    }

    // LOAD EXISTING PLANS
    private void loadRecoveryPlans() {
        planList.clear();
        planIndex.clear();

        File f = new File(PLAN_FILE_PATH);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            br.readLine(); // header

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",", -1);
                if (p.length < 9) continue;

                String planId = p[0].trim();
                String sid    = p[1].trim();
                String cid    = p[2].trim();
                int attempt   = Integer.parseInt(p[3].trim());
                String status = p[5].trim();
                String recGrade = p[6].trim();

                Student s = findStudent(sid);
                Course  c = findStudentCourse(s, cid);

                if (s == null || c == null) continue;
                c.setAttemptNumber(attempt);

                RecoveryPlan rp = new RecoveryPlan(planId, s, c);
                rp.setStatus(status);

                if (!recGrade.isEmpty()) {
                    try { rp.setRecoveryGrade(Double.parseDouble(recGrade)); } catch (Exception ignore) {}
                }
                if (p.length >= 10) rp.setRecommendation(p[9]);

                planList.add(rp);
                planIndex.put(planId, rp);
            }

        } catch (IOException e) {
            System.err.println("Error loading plans: " + e.getMessage());
        }
    }

    // LOAD MILESTONES
    private void loadPlanMilestones() {

        File f = new File(MILESTONE_FILE_PATH);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            br.readLine(); // header

            String line;
            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);
                if (p.length < 4) continue;

                String planId    = p[0].trim();
                String week      = p[1].trim();
                String task      = p[2].trim();
                boolean done     = Boolean.parseBoolean(p[3].trim());
                String notes     = (p.length >= 5 ? p[4] : "");

                RecoveryPlan rp = planIndex.get(planId);
                if (rp == null) continue;

                RecoveryMilestone ms = new RecoveryMilestone(week, task);
                if (done) ms.markCompleted(notes);
                else ms.setNotes(notes);

                rp.getMilestones().add(ms);
            }

        } catch (IOException e) {
            System.err.println("Error loading milestones: " + e.getMessage());
        }
    }

    // SAVE FILES
    public void saveRecoveryPlans() {
        try (PrintWriter out = new PrintWriter(new FileWriter(PLAN_FILE_PATH))) {

            out.println("planID,studentID,courseID,attemptNum,failureType,status,recoveryGrade,createdDate,lastUpdated,recommendation");

            for (RecoveryPlan rp : planList) {
                out.println(String.join(",",
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
                ));
            }

        } catch (IOException e) {
            System.err.println("Error saving plans: " + e.getMessage());
        }
    }

    public void savePlanMilestones() {
        try (PrintWriter out = new PrintWriter(new FileWriter(MILESTONE_FILE_PATH))) {
            out.println("planID,studyWeek,task,isCompleted,notes");

            for (RecoveryPlan rp : planList) {
                for (RecoveryMilestone m : rp.getMilestones()) {
                    out.println(String.join(",",
                            rp.getPlanID(),
                            m.getStudyWeek(),
                            m.getTask().replace(",", " "),
                            String.valueOf(m.isCompleted()),
                            m.getNotes().replace(",", " ")
                    ));
                }
            }

        } catch (IOException e) {
            System.err.println("Error saving milestones: " + e.getMessage());
        }
    }

    // FIND HELPERS
    private Student findStudent(String id) {
        for (Student s : studentPool) if (s.getStudentID().equals(id)) return s;
        return null;
    }

    private Course findStudentCourse(Student s, String cid) {
        if (s == null) return null;
        for (Course c : s.getCourses()) if (c.getCourseID().equals(cid)) return c;
        return null;
    }

    // CREATE RECOVERY PLAN
    public RecoveryPlan createRecoveryPlan(Student stu, Course c) {
        if (stu == null || c == null) return null;
        if (!c.isFailed() || "None".equals(c.getFailedComponent())) return null;

        RecoveryPlan rp = new RecoveryPlan(generatePlanID(), stu, c);

        planList.add(rp);
        planIndex.put(rp.getPlanID(), rp);

        autoGenerateMilestones(rp);

        saveRecoveryPlans();
        savePlanMilestones();
        return rp;
    }

    private void autoGenerateMilestones(RecoveryPlan rp) {
        Course c = rp.getCourse();
        int attempt = c.getAttemptNumber();
        String cid = c.getCourseID();
        String clect = c.getCourseInstructor();

        // Basic intro step
        rp.addMilestone("Week 1", "Discuss recovery plan for " + cid +
                " with lecturer " + clect + " and confirm required components.");

        if (attempt >= 3) {
            rp.addMilestone("Week 2-4", "Attend full retake revision sessions for " + cid);
            rp.addMilestone("Week 5-6", "Complete all recovery coursework and continuous assessment.");
            rp.addMilestone("Week 7-8", "Intensive exam preparation for full retake of " + cid);
            rp.addMilestone("Week 9",   "Sit for final assessment for " + cid);
            return;
        }
        switch (c.getFailedComponent()) {
            case "Assignment Only":
                rp.addMilestone("Week 2-3", "Revise assignment requirements and correct previous mistakes.");
                rp.addMilestone("Week 4",   "Submit improved assignment for " + cid);
                break;

            case "Exam Only":
                rp.addMilestone("Week 2-3", "Study weak topics using notes and past-year questions.");
                rp.addMilestone("Week 4",   "Sit for recovery exam for " + cid);
                break;

            case "Both Components":
                rp.addMilestone("Week 2-3", "Revise coursework and clarify doubts.");
                rp.addMilestone("Week 4-5", "Prepare for recovery exam while working on assignment.");
                rp.addMilestone("Week 6",   "Submit coursework and sit for exam.");
                break;
        }
    }

    // MARK MILESTONE COMPLETED
    public boolean markMilestoneCompleted(String planId, int index, String notes) {
        RecoveryPlan rp = planIndex.get(planId);
        if (rp == null) return false;

        boolean ok = rp.markMilestoneCompleted(index, notes);
        if (ok) {
            savePlanMilestones();
            saveRecoveryPlans();
        }
        return ok;
    }

    public boolean enterRecoveryGrade(RecoveryPlan rp, Integer newAssRaw, Integer newExamRaw) {
        if (rp == null) return false;

        Student stu = rp.getStudent();
        Course  c   = rp.getCourse();
        if (stu == null || c == null) return false;

        int oldAss  = c.getAssScore();
        int oldExam = c.getExamScore();

        boolean assFailed  = (c.getAssignmentWeight() > 0 && oldAss  < 50);
        boolean examFailed = (c.getExamWeight()      > 0 && oldExam < 50);

        int newAss  = oldAss;
        int newExam = oldExam;

        if (assFailed && newAssRaw != null) {
            newAss = Math.min(newAssRaw, 50); 
        }
        if (examFailed && newExamRaw != null) {
            newExam = Math.min(newExamRaw, 50);
        }

        double finalMark =
                (newAss  * c.getAssignmentWeight() / 100.0) +
                (newExam * c.getExamWeight()      / 100.0);

        String newGrade = Course.mapGradeFromMark((int) Math.round(finalMark));
        double newGpa   = Student.getGradePoint(newGrade);
        int newAttempt  = c.getAttemptNumber() + 1;

        // update course
        c.setScores(newAss, newExam);
        c.setGrade(newGrade);
        c.setAttemptNumber(newAttempt);

        // store raw entered values for reference
        String rawCombined =
                "ASS=" + (newAssRaw  != null ? newAssRaw  : oldAss) +
                ", EXAM=" + (newExamRaw != null ? newExamRaw : oldExam);
        rp.setRecoveryGradeString(rawCombined);

        // set plan status here (single source of truth)
        rp.setStatus(!c.isFailed() && newGpa >= 2.0
                ? "COMPLETED-PASSED"
                : "COMPLETED-FAILED");

        // append to grades.txt
        appendSingleLine(GRADES_FILE_PATH,
                stu.getStudentID() + "," +
                c.getCourseID() + "," +
                c.getSemester() + "," +
                newAss + "," + newExam + "," +
                newGrade + "," +
                String.format("%.2f", newGpa) + "," +
                newAttempt);

        // recalc CGPA + eligibility
        recalculateCgpaAndResult(stu);

        // persist plans + milestones
        saveRecoveryPlans();
        savePlanMilestones();

        return true;
    }

    // CGPA UPDATE
    private void recalculateCgpaAndResult(Student stu) {
        double totalPts = 0;
        int totalCredits = 0;

        for (Course c : stu.getCourses()) {
            String g = c.getGrade();
            if (g == null || g.isEmpty()) continue;

            totalPts += Student.getGradePoint(g) * c.getCreditHours();
            totalCredits += c.getCreditHours();
        }

        double cgpa = (totalCredits == 0 ? 0 : totalPts / totalCredits);
        stu.setCgpa(cgpa);

        String eligibility = (cgpa >= 2.0 && stu.getFailedCourses().isEmpty()) ? "YES" : "NO";

        List<String> newLines = new ArrayList<>();
        boolean updated = false;

        File f = new File(RESULT_FILE_PATH);

        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {

                String header = br.readLine();
                newLines.add((header == null || header.isEmpty())
                        ? "studentID,semester,CGPA,eligibility"
                        : header);

                String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.split(",", -1);

                    if (p.length >= 4 &&
                            p[0].trim().equals(stu.getStudentID()) &&
                            p[1].trim().equals(stu.getCurrentSemester())) {

                        newLines.add(stu.getStudentID() + "," +
                                stu.getCurrentSemester() + "," +
                                String.format("%.2f", cgpa) + "," +
                                eligibility);

                        updated = true;

                    } else {
                        newLines.add(line);
                    }
                }

            } catch (IOException e) {
                System.err.println("Error reading result.txt");
            }
        }

        if (!updated) {
            newLines.add(stu.getStudentID() + "," +
                    stu.getCurrentSemester() + "," +
                    String.format("%.2f", cgpa) + "," +
                    eligibility);
        }

        rewriteWholeFile(RESULT_FILE_PATH, newLines);
    }

    // FILE HELPERS
    private void appendSingleLine(String path, String row) {
        try (PrintWriter out = new PrintWriter(new FileWriter(path, true))) {
            out.println(row);
        } catch (IOException e) {
            System.err.println("Append failed: " + e.getMessage());
        }
    }

    private void rewriteWholeFile(String path, List<String> lines) {
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            for (String l : lines) out.println(l);
        } catch (IOException e) {
            System.err.println("Rewrite failed: " + e.getMessage());
        }
    }

    // ACCESSORS
    public RecoveryPlan getPlanById(String id) { 
        return planIndex.get(id); 
    }

    public List<RecoveryPlan> getAllPlans() {
        return new ArrayList<>(planList);
    }

    // EMAIL SEND (UI may call)
    private void sendRecoveryPlanEmail(RecoveryPlan rp) {
        String to   = rp.getStudent().getEmail();
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
    }
}
