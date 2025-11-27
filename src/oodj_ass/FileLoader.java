package oodj_ass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class FileLoader {
    
    private static final String STUDENT_FILE = "data/studentInfo.txt";
    private static final String COURSE_FILE = "data/courses.txt";
    private static final String RECORDS_FILE = "data/academicRecords.txt";
    private static final String ENROLLMENT_FILE = "data/studentCourse.txt";

    private final List<Student> students = new ArrayList<>();
    private final List<Course> courseInfoList = new ArrayList<>();

    // Master Load Method
    public void loadAll() {
        System.out.println("=== Loading System Data ===\n");

        try {
            loadCourseInfo();    // courses.txt
            loadStudents();           // studentInfo.txt
            loadAcademicRecords();    // academicRecords.txt
            loadStudentCourse();   // studentCourse.txt
            
        } catch (IOException e) {
            System.err.println("Data loading error: " + e.getMessage());
        }
    }
    

    // 1. Load Course information 
    private void loadCourseInfo() throws IOException {
        System.out.println("Loading Courses...");

        try (BufferedReader br = new BufferedReader(new FileReader(COURSE_FILE))) {

            String line = br.readLine(); // skip header
            int count = 0;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",");

                // courseID,courseName,creditHours,semester,instructor,assignmentWeight,examWeight
                if (p.length < 7) continue;

                Course c = new Course(
                        p[0].trim(),                      // courseID
                        p[1].trim(),                      // courseName
                        Integer.parseInt(p[2].trim()),    // creditHours
                        p[3].trim(),                      // semester
                        p[4].trim(),                      // instructor
                        Integer.parseInt(p[5].trim()),    // assignmentWeight
                        Integer.parseInt(p[6].trim())     // examWeight
                );

                courseInfoList.add(c);
                count++;
            }

            System.out.println("Courses Loaded: " + count);
        }
    }

    

    // 2. Load Students
    private void loadStudents() throws IOException {
        System.out.println("Loading Students...");

        try (BufferedReader br = new BufferedReader(new FileReader(STUDENT_FILE))) {

            String line =br.readLine(); 
            int count = 0;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",");

                if (p.length < 5) continue;

                    Student s = new Student(p[0].trim(), p[1].trim(), p[2].trim());
                    s.setMajor(p[3].trim());
                    s.setEmail(p[4].trim());

                    students.add(s);
                    count++;
                }

            System.out.println("Students Loaded: " + count);
        }
    }
    
    

    // 3. Load academic records(academicRecords.txt)
    private void loadAcademicRecords() throws IOException {
        System.out.println("Loading Academic Records...");

        try (BufferedReader br = new BufferedReader(new FileReader(RECORDS_FILE))) {

            String line = br.readLine();
            int count = 0;
            
            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",");

                if (p.length < 4) continue;

                String studentID = p[0].trim();
                String courseID  = p[1].trim();

                Student s = findStudent(studentID);
                if (s == null) continue;

                Course c = findStudentCourse(s, courseID);
                if (c == null) {
                    // Optional debug:
                    System.out.printf("[WARN] No enrolled course %s for %s in records%n", courseID, studentID);
                    continue;
                }

                // scores
                int ass  = Integer.parseInt(p[2].trim());
                int exam = Integer.parseInt(p[3].trim());
                c.setScores(ass, exam);

                // grade (may be empty)
                if (p.length >= 5 && !p[4].trim().isEmpty()) {
                    c.setGrade(p[4].trim());
                }

                // attemptNum (optional)
                if (p.length >= 7 && !p[6].trim().isEmpty()) {
                    c.setAttemptNumber(Integer.parseInt(p[6].trim()));
                }

                count++;
            }

            System.out.println("Academic Records Loaded: " + count);
        }
    }

    
    // 4. Load student course(studentCourse.txt)
    private void loadStudentCourse() throws IOException {
        System.out.println("Loading Student Course Enrolments...");

        try (BufferedReader br = new BufferedReader(new FileReader(ENROLLMENT_FILE))) {

            String line = br.readLine(); // header
            int count = 0;
            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",");

                if (p.length < 3) continue;

                String studentID = p[0].trim();
                String courseID  = p[2].trim();

                Student s = findStudent(studentID);
                Course  base = findCourseInfo(courseID);

                if (s == null || base == null) {
                    System.out.printf("[WARN] enrolment skipped: %s - %s%n", studentID, courseID);
                    continue;
                }

                // Create a course instance for THIS student
                Course enrolled = new Course(
                        base.getCourseID(),
                        base.getCourseName(),
                        base.getCreditHours(),
                        base.getSemester(),
                        base.getCourseInstructor(),
                        base.getAssignmentWeight(),
                        base.getExamWeight()
                );

                s.getCourses().add(enrolled);
                count++;
            }

            System.out.println("Enrolments Loaded: " + count);
        }
        
    }

    Student findStudent(String studentID) {
        for (Student s : students) {
            if (s.getStudentID().equals(studentID)) return s;
        }
        return null;
    }

    private Course findCourseInfo(String courseID) {
        for (Course c : courseInfoList) {
            if (c.getCourseID().equals(courseID)) return c;
        }
        return null;
    }
    
    private Course findStudentCourse(Student s, String courseID) {
        for (Course c : s.getCourses()) {
            if (c.getCourseID().equals(courseID))
                return c;
        }
        return null;
    }

    // Getter for use by modules
    public List<Student> getStudents() {
        return students;
    }

    public List<Course> getCourseInfo() {
        return courseInfoList;
    }

 }