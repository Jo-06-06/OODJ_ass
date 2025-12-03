/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oodj_ass;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;    
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Collections;

// iText: ONLY import the exact classes we use (avoid Font conflict)
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.PageSize;     
import com.itextpdf.text.Element;  

import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;

/**
 *
 * @author jolin
 */
public class APR {
    //GUI
    static DefaultTableModel tableModel;
    static JTable tblCourses;
    static JTextArea txtSummary;
    static JLabel lblCgpaValue;
    static JComboBox<String> cbIntake;
    static JSplitPane mainSplit; 

    static String currentStudentId = null;
    static String currentStudentName = "";
    static String currentProgram = "";
    static String currentIntake = "All";
    
    //Main UI
   public static void main(String[] args) {

        JFrame frame = new JFrame("Student Academic Performance Reporting");
        frame.setSize(950, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        //Top BG
        frame.getContentPane().setBackground(new Color(245, 246, 250));

        
        //StudentID, Intake, Search
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        topPanel.setBackground(new Color(248, 249, 252));

        JLabel lblID = new JLabel("Student ID: ");
        lblID.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JTextField txtID = new JTextField(10);
        txtID.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblIntake = new JLabel("   Intake: ");
        lblIntake.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        cbIntake = new JComboBox<>();
        cbIntake.setEnabled(false);
        cbIntake.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSearch.setFocusPainted(false);

        topPanel.add(lblID);
        topPanel.add(txtID);
        topPanel.add(lblIntake);
        topPanel.add(cbIntake);
        topPanel.add(btnSearch);

        frame.add(topPanel, BorderLayout.NORTH);

        //JTable, SummaryText
        tableModel = new DefaultTableModel(
                new Object[]{"Semester", "Course Code", "Course Title", "Credit", "Grade", "GPA"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblCourses = new JTable(tableModel);
        styleCoursesTable(); 
        JScrollPane tableScroll = new JScrollPane(tblCourses);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        txtSummary = new JTextArea(8, 80);
        txtSummary.setEditable(false);
        txtSummary.setLineWrap(true);
        txtSummary.setWrapStyleWord(true);
        txtSummary.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSummary.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane summaryScroll = new JScrollPane(txtSummary);
        summaryScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 214, 222)),
                "Academic Summary & Recommendations",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 12)));

        // Main split pane (top = tables, bottom = summary)
        mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, summaryScroll);
        mainSplit.setDividerLocation(320);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setBorder(null);

        frame.add(mainSplit, BorderLayout.CENTER);

        //CGPA, Export PDF
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        bottomPanel.setBackground(new Color(248, 249, 252));

        JPanel cgpaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cgpaPanel.setOpaque(false);
        JLabel lblCgpaText = new JLabel("CGPA: ");
        lblCgpaText.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCgpaValue = new JLabel("-");
        lblCgpaValue.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCgpaValue.setForeground(new Color(40, 167, 69));
        cgpaPanel.add(lblCgpaText);
        cgpaPanel.add(lblCgpaValue);

        JButton btnPDF = new JButton("Export PDF");
        btnPDF.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPDF.setFocusPainted(false);

        bottomPanel.add(cgpaPanel, BorderLayout.WEST);
        bottomPanel.add(btnPDF, BorderLayout.EAST);

        frame.add(bottomPanel, BorderLayout.SOUTH);
   
        // Search Button
        btnSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String studentID = txtID.getText().trim();

                if (studentID.equals("")) {
                    JOptionPane.showMessageDialog(frame, "Please enter your Student ID.");
                    return;
                }

                try {

                    String[] info = loadStudentNameProgram(studentID);
                    if (info == null) {
                        JOptionPane.showMessageDialog(frame, "Student not found. Please try again.");
                        clearDisplay();
                        return;
                    }

                    currentStudentId = studentID;
                    currentStudentName = info[0];
                    currentProgram = info[1];

                    ArrayList<String> semList = loadAllSemesters(studentID);
                    if (semList.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "No semester record found for this student.");
                        clearDisplay();
                        return;
                    }

                    cbIntake.removeAllItems();
                    for (String s : semList) {
                        cbIntake.addItem(s);
                    }
                    cbIntake.addItem("All");
                    cbIntake.setSelectedItem("All");
                    cbIntake.setEnabled(true);

                    currentIntake = "All";

                    loadAndDisplayReport(studentID, "All");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error generating report.");
                }
            }
        });
        
        //Intake List
        cbIntake.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (currentStudentId == null) return;

                    String selected = (String) cbIntake.getSelectedItem();
                    if (selected == null) return;

                    currentIntake = selected;

                    try {
                        loadAndDisplayReport(currentStudentId, selected);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Error updating report.");
                    }
                }
            }
        });
        
        //Export PDF Button
        btnPDF.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentStudentId == null) {
                    JOptionPane.showMessageDialog(frame, "Please search a student first.");
                    return;
                }
                try {
                    exportToPDF();
                    JOptionPane.showMessageDialog(frame, "PDF exported successfully.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "PDF export failed.");
                }
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
        //JTable
        private static void styleCoursesTable() {
             tblCourses.setRowHeight(24);
             tblCourses.setFont(new Font("Segoe UI", Font.PLAIN, 12));
             tblCourses.setGridColor(new Color(230, 232, 240));
             tblCourses.setShowGrid(true);
             tblCourses.setSelectionBackground(new Color(220, 235, 252));
             tblCourses.setSelectionForeground(Color.BLACK);

             JTableHeader header = tblCourses.getTableHeader();
             header.setReorderingAllowed(false);
             header.setFont(new Font("Segoe UI", Font.BOLD, 12));
             header.setBackground(new Color(245, 247, 250));

             DefaultTableCellRenderer center = new DefaultTableCellRenderer();
             center.setHorizontalAlignment(SwingConstants.CENTER);

        // Semester, Course Code, Credit, Grade, GPA to Center
        tblCourses.getColumnModel().getColumn(0).setCellRenderer(center);
        tblCourses.getColumnModel().getColumn(1).setCellRenderer(center);
        tblCourses.getColumnModel().getColumn(3).setCellRenderer(center);
        tblCourses.getColumnModel().getColumn(4).setCellRenderer(center);
        tblCourses.getColumnModel().getColumn(5).setCellRenderer(center);
    }

        //Clear Display
        private static void clearDisplay() {
            tableModel.setRowCount(0);
            txtSummary.setText("");
            lblCgpaValue.setText("-");
            cbIntake.removeAllItems();
            cbIntake.setEnabled(false);
            currentStudentId = null;
            currentStudentName = "";
            currentProgram = "";
            currentIntake = "All";
    }
    
        //Main Logic 
        public static void loadAndDisplayReport(String studentID, String intakeSelection) throws Exception {

            String[] stu = loadStudentNameProgram(studentID);
            if (stu == null) throw new Exception("Student not found.");

            //Load all semesters
            ArrayList<String> semesters = loadAllSemesters(studentID);
            if (semesters.isEmpty()) {
                throw new Exception("No semester found.");
            }

            //Filter
            ArrayList<String> filtered = new ArrayList<>();
            if (intakeSelection.equalsIgnoreCase("All")) {
                filtered.addAll(semesters);
            } else {
                for (String s : semesters) {
                    if (s.equals(intakeSelection)) filtered.add(s);
                }
            }

            //Table
            tableModel.setRowCount(0);  

            double totalCGPA = 0;
            int semCount = 0;

            StringBuilder achievements = new StringBuilder();
            StringBuilder weaknesses = new StringBuilder();
            StringBuilder improvements = new StringBuilder();

            boolean repeatSemester = false;
            int failTotal = 0;

            for (String sem : filtered) {

                ArrayList<String[]> courseList = loadStudentCourses(studentID, sem);
                double semCGPA = loadCGPA(studentID, sem);

                if (semCGPA > 0) {
                    totalCGPA += semCGPA;
                    semCount++;
                }

                int failCounter = 0;

                for (String[] c : courseList) {
                    String code = c[0];
                    String titleTxt = c[1];
                    String credit = c[2];

                    String[] gradeData = loadGrade(studentID, code, sem);
                    String grade = gradeData[0];
                    String gpa = gradeData[1];
                    double ass = Double.parseDouble(gradeData[2]);
                    double exam = Double.parseDouble(gradeData[3]);

                    // Add JTable
                    tableModel.addRow(new Object[]{sem, code, titleTxt, credit, grade, gpa});

                    if (grade.startsWith("A")) {
                        achievements.append("- Strong performance in ").append(code).append(".\n");
                    }

                    double gpaValue = 0;
                    try { gpaValue = Double.parseDouble(gpa); } catch (Exception ignore) {}

                    if (gpaValue < 2.00) {
                        weaknesses.append("- Weak performance in ").append(code).append(".\n");
                        failCounter++;
                    }

                    if (ass < 50) improvements.append("- Resit assignment for " + code + ".\n");
                    if (exam < 50) improvements.append("- Resit examination for " + code + ".\n");
                }

                failTotal += failCounter;
                if (semCGPA < 2.00 && semCGPA > 0) repeatSemester = true;
            }

            // CGPA
            double finalCGPA = (semCount > 0) ? totalCGPA / semCount : 0;
            lblCgpaValue.setText(String.format("%.2f", finalCGPA));

            // Summary
            StringBuilder summary = new StringBuilder();
            summary.append("Academic Performance Report\n\n");
            summary.append("Student Name : ").append(stu[0]).append("\n");
            summary.append("Student ID   : ").append(studentID).append("\n");
            summary.append("Program      : ").append(stu[1]).append("\n");
            summary.append("Intake       : ").append(intakeSelection).append("\n\n");

            summary.append("Summary of Progress:\n");
            if (finalCGPA >= 3.50)
                summary.append("- The student demonstrates excellent academic performance.\n\n");
            else if (finalCGPA >= 3.00)
                summary.append("- The student shows good consistent progress.\n\n");
            else if (finalCGPA >= 2.00)
                summary.append("- The student achieved satisfactory performance, improvement needed.\n\n");
            else
                summary.append("- The student requires significant academic improvement.\n\n");

            if (achievements.length() > 0)
                summary.append("Key Achievements:\n").append(achievements).append("\n");

            if (weaknesses.length() > 0)
                summary.append("Areas to Improve:\n").append(weaknesses).append("\n");

            summary.append("Recommendations of Improvement:\n");
            if (improvements.length() > 0)
                summary.append(improvements);
            else
                summary.append("- The student has shown good learning attitude and consistent effort.\n");

            if (failTotal >= 4 || repeatSemester)
                summary.append("- Repeating the semester is recommended.\n");

            txtSummary.setText(summary.toString());
        }

        //File Loading Functions
        public static String[] loadStudentNameProgram(String id) throws IOException {
            BufferedReader br = new BufferedReader(new FileReader("data/students.txt"));
            br.readLine(); 

            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                String sid = st.nextToken();
                String fname = st.nextToken();
                String lname = st.nextToken();
                String major = st.nextToken();

                if (sid.equals(id)) {
                    br.close();
                    return new String[]{fname + " " + lname, major};
                }
            }
            br.close();
            return null;
        }

        //Read all semester in studentInfo.txt file + result + archive + gradeArchive
        public static ArrayList<String> loadAllSemesters(String id) throws IOException {
            ArrayList<String> list = new ArrayList<>();

        //studentInfo.txt
        BufferedReader br = new BufferedReader(new FileReader("data/studentInfo.txt"));
        br.readLine();
        String line;
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");
            String sid = st.nextToken();
            String sem = st.nextToken();
            if (sid.equals(id) && !list.contains(sem)) {
                list.add(sem);
            }
        }
        br.close();

        //result.txt
        addSemestersFromResultFile(list, id, "data/result.txt");

        //resultArchive.txt
        addSemestersFromResultFile(list, id, "data/resultArchive.txt");

        //gradeArchive.txt 
        File gArch = new File("data/gradeArchive.txt");
        if (gArch.exists()) {
            br = new BufferedReader(new FileReader(gArch));
            br.readLine(); 
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                if (!st.hasMoreTokens()) continue;
                String sid = st.nextToken();     // studentID
                if (!st.hasMoreTokens()) continue;
                st.nextToken();                  // courseID
                if (!st.hasMoreTokens()) continue;
                String sem = st.nextToken();     // semester
                if (sid.equals(id) && !list.contains(sem)) {
                    list.add(sem);
                }
            }
            br.close();
        }

        Collections.sort(list); 

        return list;
    }

        private static void addSemestersFromResultFile(ArrayList<String> list, String id, String filename) throws IOException {
            File f = new File(filename);
            if (!f.exists()) return;

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = br.readLine(); 
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                if (!st.hasMoreTokens()) continue;
                String sid = st.nextToken();
                if (!st.hasMoreTokens()) continue;
                String sem = st.nextToken();
                if (sid.equals(id) && !list.contains(sem)) {
                    list.add(sem);
                }
            }
            br.close();
        }

        //Read course in courses.txt file
        public static String[] loadCourseInfo(String code) throws IOException {
            BufferedReader br = new BufferedReader(new FileReader("data/courses.txt"));
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                String c = st.nextToken();
                String title = st.nextToken();
                String credit = st.nextToken();
                st.nextToken(); 

                if (c.equals(code)) {
                    br.close();
                    return new String[]{c, title, credit};
                }
            }
            br.close();
            return new String[]{code, "Unknown Course", "0"};
        }

        // Student Courses in studentCourse.txt
        public static ArrayList<String[]> loadStudentCourses(String id, String sem) throws IOException {
            ArrayList<String[]> list = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader("data/studentCourse.txt"));
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                String sid = st.nextToken();
                String s = st.nextToken();
                String code = st.nextToken();

                if (sid.equals(id) && s.equals(sem)) {
                    list.add(loadCourseInfo(code));
                }
            }
            br.close();
            return list;
        }

        //Read Grade from file 
        private static String[] loadGradeFromFile(String id, String code, String sem,
                                                  String filename, boolean hasSemesterColumn) throws IOException {
            File f = new File(filename);
            if (!f.exists()) return null;

            BufferedReader br = new BufferedReader(new FileReader(f));
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");

                if (!st.hasMoreTokens()) continue;
                String sid = st.nextToken();
                if (!st.hasMoreTokens()) continue;
                String courseID = st.nextToken();

                String semFromFile = null;
                if (hasSemesterColumn) {
                    if (!st.hasMoreTokens()) continue;
                    semFromFile = st.nextToken(); 
                }

                if (!sid.equals(id) || !courseID.equals(code)) {
                    continue;
                }
                if (hasSemesterColumn && semFromFile != null && !semFromFile.equals(sem)) {
                    continue;
                }

                // ass, exam, grade, gpa
                if (!st.hasMoreTokens()) continue;
                String ass = st.nextToken();
                if (!st.hasMoreTokens()) continue;
                String exam = st.nextToken();
                if (!st.hasMoreTokens()) continue;
                String grade = st.nextToken();
                if (!st.hasMoreTokens()) continue;
                String gpa = st.nextToken();

                br.close();
                return new String[]{grade, gpa, ass, exam};
            }

            br.close();
            return null;
        }

        //Grades + gradeArchive
        public static String[] loadGrade(String id, String code, String sem) throws IOException {

            String[] data = loadGradeFromFile(id, code, sem, "data/grades.txt", false);
            if (data != null) return data;

            data = loadGradeFromFile(id, code, sem, "data/gradeArchive.txt", true);
            if (data != null) return data;

            return new String[]{"-", "0.00", "0", "0"};
        }

        //Read CGPA
        private static Double loadCGPAFromFile(String id, String sem, String filename) throws IOException {
            File f = new File(filename);
            if (!f.exists()) return null;

            BufferedReader br = new BufferedReader(new FileReader(f));
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                String sid = st.nextToken();
                String s = st.nextToken();
                String cgpaStr = st.nextToken();

                if (sid.equals(id) && s.equals(sem)) {
                    br.close();
                    try {
                        return Double.valueOf(Double.parseDouble(cgpaStr));
                    } catch (Exception ex) {
                        return 0.0;
                    }
                }
            }
            br.close();
            return null;
        }

        //Result + resultArchive
        public static double loadCGPA(String id, String sem) throws IOException {
            Double v = loadCGPAFromFile(id, sem, "data/result.txt");
            if (v != null) return v.doubleValue();

            v = loadCGPAFromFile(id, sem, "data/resultArchive.txt");
            if (v != null) return v.doubleValue();

            return 0.0;
        }

        //PDF Export
        public static void exportToPDF() throws Exception {

        if (currentStudentId == null) {
            return;
        }

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        String fileName = "AcademicReport_" + currentStudentId + ".pdf";
        PdfWriter.getInstance(doc, new FileOutputStream(fileName));
        doc.open();

        com.itextpdf.text.Font titleFont =
                new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA,
                        16,
                        com.itextpdf.text.Font.BOLD
                );

        com.itextpdf.text.Font infoFont =
                new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA,
                        11,
                        com.itextpdf.text.Font.NORMAL
                );

        com.itextpdf.text.Font semTitleFont =
                new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA,
                        12,
                        com.itextpdf.text.Font.BOLD
                );

        com.itextpdf.text.Font headerFont =
                new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA,
                        11,
                        com.itextpdf.text.Font.BOLD
                );

        // Report Title
        Paragraph title = new Paragraph("Academic Performance Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15f);
        doc.add(title);

        // Student Info
        doc.add(new Paragraph("Student Name : " + currentStudentName, infoFont));
        doc.add(new Paragraph("Student ID   : " + currentStudentId, infoFont));
        doc.add(new Paragraph("Program      : " + currentProgram, infoFont));
        doc.add(new Paragraph("CGPA         : " + lblCgpaValue.getText(), infoFont));
        doc.add(Chunk.NEWLINE);

        //Generate tables by sem
        ArrayList<String> semesters = loadAllSemesters(currentStudentId);
        if (!"All".equals(currentIntake)) {

            semesters.clear();
            semesters.add(currentIntake);
        }

        for (String sem : semesters) {

            //Sem Title
            Paragraph semTitle = new Paragraph("Semester: " + sem, semTitleFont);
            semTitle.setSpacingBefore(8f);
            semTitle.setSpacingAfter(4f);
            doc.add(semTitle);

            //Table
            PdfPTable pdfTable = new PdfPTable(5);
            pdfTable.setWidthPercentage(100);
            pdfTable.setWidths(new float[]{15f, 45f, 8f, 10f, 10f});

            String[] headers = {"Course Code", "Course Title", "Credit", "Grade", "GPA"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                pdfTable.addCell(cell);
            }

            //Course by Sem
            ArrayList<String[]> courseList = loadStudentCourses(currentStudentId, sem);
            for (String[] c : courseList) {
                String code = c[0];
                String titleTxt = c[1];
                String credit = c[2];

                String[] gradeData = loadGrade(currentStudentId, code, sem);
                String grade = gradeData[0];
                String gpa = gradeData[1];

                pdfTable.addCell(new Phrase(code, infoFont));
                pdfTable.addCell(new Phrase(titleTxt, infoFont));
                pdfTable.addCell(new Phrase(credit, infoFont));
                pdfTable.addCell(new Phrase(grade, infoFont));
                pdfTable.addCell(new Phrase(gpa, infoFont));
            }

            doc.add(pdfTable);
            doc.add(Chunk.NEWLINE);
        }

        // Summary & Recommendations
        String fullSummary = txtSummary.getText();
        String summarySection = fullSummary;

        int idx = fullSummary.indexOf("Summary of Progress:");
        if (idx >= 0) {
            summarySection = fullSummary.substring(idx);
        }

             doc.add(new Paragraph(summarySection, infoFont));

            doc.close();
        }
    }                 