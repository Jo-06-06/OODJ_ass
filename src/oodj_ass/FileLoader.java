package oodj_ass;

import java.io.*;
import java.util.*;

public class FileLoader {

    private static final String STUDENT_FILE = "data/students.txt";
    private static final String STUDENT_INFO_FILE  = "data/studentInfo.txt";
    private static final String COURSE_FILE = "data/courses.txt";
    private static final String ENROLL_FILE = "data/studentCourse.txt";
    private static final String GRADES_FILE = "data/grades.txt";
    private static final String CGPA_FILE = "data/result.txt";

    private final List<Student> students = new ArrayList<>();
    private final List<Course> courseInfoList = new ArrayList<>();

    public void loadAll() {
        System.out.println("=== Loading System Data ===");

        try {
            loadCourses();
            loadStudentBasic();
            loadStudentSemester();
            loadStudentEnrolments();
            loadGrades();
            loadCGPA();
        } catch (Exception e) {
            System.err.println("Error loading data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 1. Load all course info (courses.txt)
    private void loadCourses() throws IOException {
        System.out.println("Loading courses...");

        int totalRows = 0;
        int loadedRows = 0;
        int skippedRows = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(COURSE_FILE))) {
            String header = br.readLine(); // skip header
            int lineNo = 1;                // header line

            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                totalRows++;

                String[] p = line.split(",", -1);
                if (p.length < 7) {
                    skippedRows++;
                    System.err.println("[COURSES] Invalid column count at line " + lineNo + ": " + line);
                    continue;
                }

                try {
                    String courseID   = p[0].trim();
                    String courseName = p[1].trim();
                    int creditHours   = Integer.parseInt(p[2].trim());
                    String semester   = p[3].trim();
                    String instructor = p[4].trim();
                    int assWeight     = Integer.parseInt(p[5].trim());
                    int examWeight    = Integer.parseInt(p[6].trim());

                    Course c = new Course(courseID, courseName, creditHours,
                                          semester, instructor, assWeight, examWeight);

                    courseInfoList.add(c);
                    loadedRows++;

                } catch (NumberFormatException ex) {
                    skippedRows++;
                    System.err.println("[COURSES] Number parse error at line " + lineNo + ": " + ex.getMessage());
                }
            }
        }

        System.out.println("Courses loaded: " + loadedRows +
                " (total lines: " + totalRows + ", skipped: " + skippedRows + ")");
    }

    // 2. Load students basic info (students.txt)
    private void loadStudentBasic() throws IOException {
        System.out.println("Loading student basic info...");

        int totalRows = 0;
        int loadedRows = 0;
        int skippedRows = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(STUDENT_FILE))) {
            String header = br.readLine(); // skip header
            int lineNo = 1;

            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                totalRows++;

                String[] p = line.split(",", -1);
                // students.txt: studentID,firstName,lastName,major,email
                if (p.length < 5) {
                    skippedRows++;
                    System.err.println("[STUDENTS] Invalid column count at line " + lineNo + ": " + line);
                    continue;
                }

                String sid       = p[0].trim();
                String firstName = p[1].trim();
                String lastName  = p[2].trim();
                String major     = p[3].trim();
                String email     = p[4].trim();

                Student s = new Student(sid, firstName, lastName);
                s.setMajor(major);
                s.setEmail(email);

                students.add(s);
                loadedRows++;
            }
        }

        System.out.println("Student basic loaded: " + loadedRows +
                " (total lines: " + totalRows + ", skipped: " + skippedRows + ")");
    }

    // 3. Load student semester (studentInfo.txt)
    private void loadStudentSemester() throws IOException {
        System.out.println("Loading student semester...");

        int totalRows = 0;
        int loadedRows = 0;
        int skippedRows = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(STUDENT_INFO_FILE))) {
            String header = br.readLine(); // skip header
            int lineNo = 1;

            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                totalRows++;

                String[] p = line.split(",", -1);
                // studentInfo.txt: studentID,semester
                if (p.length < 2) {
                    skippedRows++;
                    System.err.println("[STUDENT_INFO] Invalid column count at line " + lineNo + ": " + line);
                    continue;
                }

                String sid = p[0].trim();
                String sem = p[1].trim();

                Student s = findStudent(sid);
                if (s == null) {
                    skippedRows++;
                    System.err.println("[STUDENT_INFO] Student not found for ID " + sid +
                                       " at line " + lineNo);
                    continue;
                }

                s.setCurrentSemester(sem);
                loadedRows++;
            }
        }

        System.out.println("Student semester records loaded: " + loadedRows +
                " (total lines: " + totalRows + ", skipped: " + skippedRows + ")");
    }

    // 4. Load enrolled courses (studentCourse.txt)
    private void loadStudentEnrolments() throws IOException {
        System.out.println("Loading student enrolments...");

        int totalRows = 0;
        int loadedRows = 0;
        int skippedRows = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(ENROLL_FILE))) {
            String header = br.readLine(); 
            int lineNo = 1;

            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                totalRows++;

                String[] p = line.split(",", -1);
                // studentCourse.txt: studentID,semester,courseID
                if (p.length < 3) {
                    skippedRows++;
                    System.err.println("[ENROL] Invalid column count at line " + lineNo + ": " + line);
                    continue;
                }

                String sid      = p[0].trim();
                String sem      = p[1].trim(); 
                String courseId = p[2].trim();

                Student s = findStudent(sid);
                Course template = findCourseInfo(courseId);

                if (s == null || template == null) {
                    skippedRows++;
                    System.err.println("[ENROL] Missing student or course (sid=" + sid +
                                       ", course=" + courseId + ") at line " + lineNo);
                    continue;
                }

                Course c = new Course(
                        template.getCourseID(),
                        template.getCourseName(),
                        template.getCreditHours(),
                        template.getSemester(),
                        template.getCourseInstructor(),
                        template.getAssignmentWeight(),
                        template.getExamWeight()
                );

                s.getCourses().add(c);
                loadedRows++;
            }
        }

        System.out.println("Student enrolments loaded: " + loadedRows +
                " (total lines: " + totalRows + ", skipped: " + skippedRows + ")");
    }

    // 5. Load grades (grades.txt)
    private void loadGrades() throws IOException {
        System.out.println("Loading grades (multi-attempt)...");

        File gradeFile = new File(GRADES_FILE);
        if (!gradeFile.exists()) {
            System.out.println("grades.txt not found, skipping.");
            return;
        }

        // Inner helper structure to hold the latest attempt per (student,course)
        class GradeRecord {
            String semester;
            int assScore;
            int examScore;
            String grade;
            double gpa;
            int attemptNum;
        }

        // key = "studentID|courseID"
        Map<String, GradeRecord> latestGradeMap = new HashMap<>();

        int totalRows   = 0;
        int usedRows    = 0;
        int skippedRows = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(gradeFile))) {

            String header = br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                totalRows++;
                String[] p = line.split(",", -1);

                if (p.length < 8) {
                    skippedRows++;
                    System.err.println("  Skipping invalid grade row (columns < 8): " + line);
                    continue;
                }

                String sid = p[0].trim();
                String cid = p[1].trim();
                String sem = p[2].trim();

                try {
                    int assScore   = Integer.parseInt(p[3].trim());
                    int examScore  = Integer.parseInt(p[4].trim());
                    String grade   = p[5].trim();
                    double gpa     = Double.parseDouble(p[6].trim());
                    int attemptNum = Integer.parseInt(p[7].trim());

                    String key = sid + "|" + cid;
                    GradeRecord current = latestGradeMap.get(key);

                    // *** CHANGED: keep ONLY the highest attemptNum for each (sid,cid)
                    if (current == null || attemptNum > current.attemptNum) {
                        GradeRecord gr = new GradeRecord();
                        gr.semester   = sem;
                        gr.assScore   = assScore;
                        gr.examScore  = examScore;
                        gr.grade      = grade;
                        gr.gpa        = gpa;
                        gr.attemptNum = attemptNum;
                        latestGradeMap.put(key, gr);
                    }

                } catch (NumberFormatException nfe) {
                    skippedRows++;
                    System.err.println("  Skipping grade row (number format error): " + line);
                }
            }
        }

        int courseUpdated = 0;

        for (Student s : students) {
            for (Course c : s.getCourses()) {
                String key = s.getStudentID() + "|" + c.getCourseID();
                GradeRecord gr = latestGradeMap.get(key);

                if (gr != null) {
                    c.setScores(gr.assScore, gr.examScore);
                    c.setGrade(gr.grade);
                    c.setAttemptNumber(gr.attemptNum);
                    courseUpdated++;
                }
            }
        }

        System.out.println("  Grades rows read   : " + totalRows);
        System.out.println("  Courses updated    : " + courseUpdated);
        System.out.println("  Rows skipped       : " + skippedRows);
    }

    // 6. Load CGPA (result.txt)
    private void loadCGPA() throws IOException {
        System.out.println("Loading CGPA...");

        File f = new File(CGPA_FILE);
        if (!f.exists()) {
            System.out.println("result.txt not found – skipping CGPA load.");
            return;
        }

        int totalRows = 0;
        int loadedRows = 0;
        int skippedRows = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(CGPA_FILE))) {
            String header = br.readLine(); // skip header
            int lineNo = 1;

            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                totalRows++;

                String[] p = line.split(",", -1);
                // result.txt: studentID,semester,CGPA,eligibility
                if (p.length < 4) {
                    skippedRows++;
                    System.err.println("[CGPA] Invalid column count at line " + lineNo + ": " + line);
                    continue;
                }

                String sid = p[0].trim();
                String sem = p[1].trim();
                String cgpaText = p[2].trim();
                // eligibility = p[3].trim(); // not needed here

                Student s = findStudent(sid);
                if (s == null) {
                    skippedRows++;
                    System.err.println("[CGPA] Student not found for ID " + sid + " at line " + lineNo);
                    continue;
                }

                try {
                    double cgpa = Double.parseDouble(cgpaText);
                    s.setCgpa(cgpa);
                    loadedRows++;
                } catch (NumberFormatException ex) {
                    skippedRows++;
                    System.err.println("[CGPA] Number parse error at line " + lineNo + ": " + ex.getMessage());
                }
            }
        }

        System.out.println("CGPA records loaded: " + loadedRows +
                " (total lines: " + totalRows + ", skipped: " + skippedRows + ")");
    }

    // Helper search methods
    private Student findStudent(String id) {
        for (Student s : students) {
            if (s.getStudentID().equals(id)) return s;
        }
        return null;
    }

    private Course findCourseInfo(String id) {
        for (Course c : courseInfoList) {
            if (c.getCourseID().equals(id)) return c;
        }
        return null;
    }

    private Course findStudentCourse(Student s, String id) {
        for (Course c : s.getCourses()) {
            if (c.getCourseID().equals(id)) return c;
        }
        return null;
    }

    public Student getStudentByID(String id) {
        for (Student s : students) {
            if (s.getStudentID().equals(id)) {
                return s;
            }
        }
        return null;
    }
    
    public Course getCourseByID(String id) {
        for (Course c : courseInfoList) {
            if (c.getCourseID().equals(id)) return c;
        }
        return null;
    }
    
    public List<Student> getStudents() {
        return students;
    }

    public List<Course> getCourseInfo() {
        return courseInfoList;
    }
}
