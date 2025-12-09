package oodj_ass;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.*;
import java.util.*;



public class CRP_UI extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CRP_UI.class.getName());
    private static final String RECOVERY_PLAN_FILE = "data/recoveryPlans.txt";

    private FileLoader fileLoader;
    private RecoveryPlan currentPlan;

    // key = "studentID|courseID|attemptNum"
    private Map<String, RecoveryPlan> planByKey = new HashMap<>();
    // key = planID
    private Map<String, RecoveryPlan> planByID = new HashMap<>();

    private int lastPlanNumber = 0; 
    private String recommendation;
    
    
    /**
     * Creates new form CRP_UI
     */
    public CRP_UI(FileLoader loader) {
        this.fileLoader = loader;
        initComponents();
        
        tabTwoWay.setSelectedIndex(0);
        
        loadAllFailedStudents();
        loadRecoveryPlansFromFile();
        
       jTableFailedComponents.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {

                // Delay selection handling by 1 event cycle
                SwingUtilities.invokeLater(() -> {
                    int row = jTableFailedComponents.getSelectedRow();
                    if (row == -1) return;

                    Course c = getSelectedFailedCourse();
                    if (c == null) return;

                    String sid = jTableFailedComponents.getValueAt(row, 0).toString();
                    Student s = fileLoader.getStudentByID(sid);

                    updateStudentInfo(s);
                    updateCourseInfo(c);

                    // load plan text if exists
                    String key = buildPlanKey(s.getStudentID(), c.getCourseID(), c.getAttemptNumber());
                    RecoveryPlan plan = planByKey.get(key);

                    txtRecommendation.setText(plan != null ? plan.getRecommendation() : "");
                });
            }
    });
}
    
    private void loadFailedComponents(String studentID) {
        DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();
        model.setRowCount(0);

        // If search box empty, just show all failed students (same as initial view)
        if (studentID == null || studentID.trim().isEmpty()) {
            loadAllFailedStudents();
            return;
        }

        Student student = fileLoader.getStudentByID(studentID.trim());

        if (student == null) {
            JOptionPane.showMessageDialog(this, "Student not found.");
            return;
        }

        for (Course c : student.getCourses()) {
            String failed = c.getFailedComponent();   // <-- now uses weights

            if ("None".equals(failed)) {
                continue; // passed, so skip
            }

            model.addRow(new Object[]{
                student.getStudentID(),
                c.getCourseID(),
                failed
            });
        }
        
        if (model.getRowCount() > 0) {
            jTableFailedComponents.setRowSelectionInterval(0, 0);

            String sid = model.getValueAt(0, 0).toString();
            String cid = model.getValueAt(0, 1).toString();

            Student s = fileLoader.getStudentByID(sid);
            Course c = fileLoader.getCourseByID(cid);

            updateStudentInfo(s);
            updateCourseInfo(c);

            // load existing plan recommendation if exists
            String key = buildPlanKey(sid, cid, c.getAttemptNumber());
            RecoveryPlan plan = planByKey.get(key);

            txtRecommendation.setText(
                plan != null ? getFormattedRecommendation(plan) : ""
            );
        }

        // If the student has no failed modules
        if (model.getRowCount() == 0) {
            lblPlanID.setText(" ");
            txtRecommendation.setText(
                "Student has successfully passed all enrolled modules.\n" +
                "No recovery plan is required for this semester."
            );
            updateCourseInfo(null);
            updateStudentInfo(student);
        }
    }


    private void loadAllFailedStudents() {
        DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();
        model.setRowCount(0);

        for (Student s : fileLoader.getStudents()) {
            for (Course c : s.getCourses()) {

                // determine failed components using your Course class
                String failed = c.getFailedComponent();
                if ("None".equals(failed)) continue;

                model.addRow(new Object[]{
                    s.getStudentID(),
                    c.getCourseID(),
                    failed
                });
            }
        }
    }
    
    private void loadRecoveryPlansFromFile() {
        planByKey.clear();
        planByID.clear();
        lastPlanNumber = 0;

        File f = new File(RECOVERY_PLAN_FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String header = br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // limit=10 so recommendation (last field) can contain commas safely
                String[] p = line.split(",", 10);
                if (p.length < 10) continue;

                String planID        = p[0].trim();
                String sid           = p[1].trim();
                String cid           = p[2].trim();
                int attempt          = Integer.parseInt(p[3].trim());
                // String failureType = p[4].trim();   // not strictly needed; we can recompute
                String status        = p[5].trim();
                String gradeText     = p[6].trim();
                String createdDate   = p[7].trim();
                String lastUpdated   = p[8].trim();
                String recommendation = p[9].trim();

                Student s = fileLoader.getStudentByID(sid);
                if (s == null) continue;

                Course course = null;
                for (Course c : s.getCourses()) {
                    if (c.getCourseID().equals(cid) &&
                        c.getAttemptNumber() == attempt) {
                        course = c;
                        break;
                    }
                }
                if (course == null) continue;

                RecoveryPlan plan = new RecoveryPlan(planID, s, course);
                plan.setCreatedDateRaw(createdDate);
                plan.setLastUpdatedRaw(lastUpdated);
                plan.setRecommendation(recommendation);
                plan.setStatus(status);
                if (!gradeText.isEmpty()) {
                    try {
                        plan.setRecoveryGrade(Double.parseDouble(gradeText));
                    } catch (NumberFormatException ignored) {}
                }

                String key = buildPlanKey(sid, cid, attempt);
                planByKey.put(key, plan);
                planByID.put(planID, plan);
                updateLastPlanNumberFromID(planID);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading recoveryPlans.txt: " + e.getMessage());
        }
    }

    public String getFormattedRecommendation(RecoveryPlan plan) {
        if (plan == null) return "";

        String raw = plan.getRecommendation();
        if (raw == null || raw.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("=== Recommendation Summary ===\n\n");

        String[] sentences = raw.split("\\. ");
        for (String s : sentences) {
            sb.append("• ").append(s.trim()).append(".\n");
        }

        sb.append("\n===============================");
        return sb.toString();
    }

    private Course getSelectedFailedCourse() {

        int row = jTableFailedComponents.getSelectedRow();
        if (row == -1) return null;

        DefaultTableModel model =
                (DefaultTableModel) jTableFailedComponents.getModel();

        String sid = model.getValueAt(row, 0).toString();
        String cid = model.getValueAt(row, 1).toString();

        Student student = fileLoader.getStudentByID(sid);
        if (student == null) return null;

        for (Course c : student.getCourses()) {
            if (c.getCourseID().equals(cid)) {
                return c;
            }
        }
        return null;
    }

    private String buildPlanKey(String sid, String cid, int attempt) {
        return sid + "|" + cid + "|" + attempt;
    }

    private void updateLastPlanNumberFromID(String planID) {
        if (planID != null && planID.startsWith("RP")) {
            try {
                int n = Integer.parseInt(planID.substring(2));
                if (n > lastPlanNumber) lastPlanNumber = n;
            } catch (NumberFormatException ignored) {}
        }
    }

    private String generatePlanID() {
        lastPlanNumber++;
        return String.format("RP%04d", lastPlanNumber);
    }

    private void savePlansToFile() {
        File f = new File(RECOVERY_PLAN_FILE);

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("planID,studentID,courseID,attemptNum,failureType,status,recoveryGrade,createdDate,lastUpdated,recommendation");

            for (RecoveryPlan plan : planByID.values()) {
                Student s = plan.getStudent();
                Course c = plan.getCourse();

                String failureType = c.getFailedComponent();
                String status = plan.getStatus();
                String gradeText = (plan.getRecoveryGrade() == null)
                        ? "" : String.valueOf(plan.getRecoveryGrade());
                String created = plan.getCreatedDate();
                String updated = plan.getLastUpdated();

                // avoid breaking CSV if recommendation contains commas
                String rec = plan.getRecommendation().replace(",", " ");

                pw.printf("%s,%s,%s,%d,%s,%s,%s,%s,%s,%s%n",
                        plan.getPlanID(),
                        s.getStudentID(),
                        c.getCourseID(),
                        c.getAttemptNumber(),
                        failureType,
                        status,
                        gradeText,
                        created,
                        updated,
                        rec);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving recoveryPlans.txt: " + e.getMessage());
        }
    }
    
    public String getFormattedRecommendation() {
        String raw = this.recommendation;

        // Add spacing and wrap long lines for readability
        StringBuilder sb = new StringBuilder();

        sb.append("=== Recommendation Summary ===\n\n");

        // Break into sentences if possible
        String[] sentences = raw.split("\\. ");
        for (String s : sentences) {
            sb.append("• ").append(s.trim()).append(".\n");
        }

        sb.append("\n===============================");

        return sb.toString();
    }
    
//    private void searchStudent() {
//        String studentID = txtStudentID.getText().trim();
//
//        if (studentID.isEmpty()) {
//            JOptionPane.showMessageDialog(this, "Please enter a Student ID.");
//            return;
//        }
//
//        Student s = fileLoader.getStudentByID(studentID);
//
//        if (s == null) {
//            JOptionPane.showMessageDialog(this,
//                    "Student ID " + studentID + " does not exist in the system.",
//                    "Invalid Student",
//                    JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        loadFailedComponents(studentID);
//
//        // If no failed records, table is empty → show message
//        if (jTableFailedComponents.getRowCount() == 0) {
//            JOptionPane.showMessageDialog(this,
//                    "Student " + studentID + " has passed all modules.",
//                    "No Failed Components",
//                    JOptionPane.INFORMATION_MESSAGE);
//        }
//    }

    private void updateStudentInfo(Student s) {
        if (s == null) {
            lblInfoStudentID.setText("");
            lblInfoStudentName.setText("");
            lblInfoCGPA.setText("");
            return;
        }

        lblInfoStudentID.setText(s.getStudentID());
        lblInfoStudentName.setText(s.getFullName());
        lblInfoCGPA.setText(String.format("%.2f", s.getCgpa()));
    }

    private void updateCourseInfo(Course c) {
        if (c == null) {
            clearDetails();
            return;
        }

        lblInfoCourseID.setText(c.getCourseID());
        lblInfoCourseName.setText(c.getCourseName());
        lblInfoLecturer.setText(c.getCourseInstructor());
        lblInfoSemester.setText(c.getSemester());
        lblInfoAttempt.setText(String.valueOf(c.getAttemptNumber()));

        lblInfoAssScore.setText(String.valueOf(c.getAssScore()));
        lblInfoExamScore.setText(String.valueOf(c.getExamScore()));

        // Failure badge
        String type = c.getFailedComponent(); 
        lblInfoFailure.setText(type);

        switch (type) {
            case "Assignment Only":
                panelFailureBadge.setBackground(new Color(176, 223, 232)); // blue 
                break;
            case "Exam Only":
                panelFailureBadge.setBackground(new Color(250, 220, 160)); // pale orange
                break;
            case "Both Components":
                panelFailureBadge.setBackground(new Color(255, 165, 156)); // stronger pale red
                break;
            default:
                panelFailureBadge.setBackground(new Color(200, 230, 200)); 
                lblInfoFailure.setText("PASSED");
                break;
        }
    }
    private void showPassedBadge() {
        lblInfoFailure.setText("PASSED");
        panelFailureBadge.setBackground(new Color(180, 220, 180)); // pale green
    }

    private void clearDetails() {
        lblInfoStudentID.setText("");
        lblInfoStudentName.setText("");
        lblInfoCGPA.setText("");

        lblInfoCourseID.setText("");
        lblInfoCourseName.setText("");
        lblInfoLecturer.setText("");
        lblInfoSemester.setText("");
        lblInfoAttempt.setText("");
        lblInfoAssScore.setText("");
        lblInfoExamScore.setText("");
        lblInfoFailure.setText("");

        panelFailureBadge.setBackground(new Color(230,230,230)); 
        txtRecommendation.setText("");
        lblPlanID.setText("");
    }
    //Validation input
    private boolean isValidStudentID(String sid) {
        // Accepts S001–S999 pattern
        return sid.matches("^S\\d{3}$");
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFrame1 = new javax.swing.JFrame();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        tabTwoWay = new javax.swing.JTabbedPane();
        panelOverview = new javax.swing.JPanel();
        txtStudentID = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        btnCreatePlan = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableFailedComponents = new javax.swing.JTable();
        RPpanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblplanid = new javax.swing.JLabel();
        lblPlanID = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        priorityCombo = new javax.swing.JComboBox<>();
        btnSavePlan = new javax.swing.JButton();
        btnEditPlan = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        txtRecommendation = new javax.swing.JTextArea();
        btnMilestoneTab = new javax.swing.JButton();
        panelFB = new javax.swing.JPanel();
        lblStudentID = new javax.swing.JLabel();
        lblInfoStudentID = new javax.swing.JLabel();
        lblStudentName = new javax.swing.JLabel();
        lblInfoStudentName = new javax.swing.JLabel();
        lblTitleDetails = new javax.swing.JLabel();
        lblCourseID = new javax.swing.JLabel();
        lblInfoCourseID = new javax.swing.JLabel();
        lblCourseName = new javax.swing.JLabel();
        lblInfoCourseName = new javax.swing.JLabel();
        lblLecturer = new javax.swing.JLabel();
        lblInfoLecturer = new javax.swing.JLabel();
        lblSemester = new javax.swing.JLabel();
        lblInfoSemester = new javax.swing.JLabel();
        lblAssScore = new javax.swing.JLabel();
        lblInfoAssScore = new javax.swing.JLabel();
        lblExamScore = new javax.swing.JLabel();
        lblAttempt = new javax.swing.JLabel();
        lblInfoAttempt = new javax.swing.JLabel();
        lblInfoExamScore = new javax.swing.JLabel();
        lblStudentID2 = new javax.swing.JLabel();
        lblInfoCGPA = new javax.swing.JLabel();
        panelFailureBadge = new javax.swing.JPanel();
        lblInfoFailure = new javax.swing.JLabel();
        lblCRP1 = new javax.swing.JLabel();
        MilestonesTab = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel8 = new javax.swing.JLabel();
        jButton11 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        lblCRP = new javax.swing.JLabel();

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(1160, 700));
        setMinimumSize(new java.awt.Dimension(1160, 700));
        setPreferredSize(new java.awt.Dimension(1160, 700));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(183, 201, 197));
        jPanel1.setPreferredSize(new java.awt.Dimension(1160, 700));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(86, 96, 95));
        jPanel2.setPreferredSize(new java.awt.Dimension(210, 700));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 210, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 700, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, 700));

        tabTwoWay.setPreferredSize(new java.awt.Dimension(870, 945));

        panelOverview.setBackground(new java.awt.Color(183, 201, 197));
        panelOverview.setPreferredSize(new java.awt.Dimension(950, 589));

        txtStudentID.setFont(new java.awt.Font("Serif", 0, 15)); // NOI18N
        txtStudentID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtStudentIDActionPerformed(evt);
            }
        });

        btnSearch.setText("Search");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        btnCreatePlan.setFont(new java.awt.Font("Helvetica Neue", 0, 15)); // NOI18N
        btnCreatePlan.setText("Create Plan");
        btnCreatePlan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreatePlanActionPerformed(evt);
            }
        });

        jTableFailedComponents.setFont(new java.awt.Font("Kohinoor Telugu", 0, 15)); // NOI18N
        jTableFailedComponents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "StudentID", "CourseID", "Failed Component(s)"
            }
        ));
        jScrollPane1.setViewportView(jTableFailedComponents);

        jScrollPane2.setViewportView(jScrollPane1);

        RPpanel.setBackground(new java.awt.Color(255, 255, 255));
        RPpanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        RPpanel.setAlignmentX(0.0F);
        RPpanel.setAlignmentY(0.0F);

        jLabel2.setFont(new java.awt.Font("Serif", 1, 20)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Recovery Plan Details");

        lblplanid.setFont(new java.awt.Font("Serif", 1, 15)); // NOI18N
        lblplanid.setText("Plan ID:");

        lblPlanID.setFont(new java.awt.Font("Tiro Devanagari Sanskrit", 1, 15)); // NOI18N
        lblPlanID.setText(" ");

        jLabel5.setFont(new java.awt.Font("Serif", 1, 15)); // NOI18N
        jLabel5.setText("Recommendation:");

        jLabel6.setFont(new java.awt.Font("Serif", 1, 15)); // NOI18N
        jLabel6.setText("Priority Level:");

        priorityCombo.setEditable(true);
        priorityCombo.setFont(new java.awt.Font("Helvetica Neue", 0, 14)); // NOI18N
        priorityCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "High", "Medium", "Low" }));
        priorityCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                priorityComboActionPerformed(evt);
            }
        });

        btnSavePlan.setFont(new java.awt.Font("Helvetica Neue", 0, 14)); // NOI18N
        btnSavePlan.setText("Save Plan");
        btnSavePlan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSavePlanActionPerformed(evt);
            }
        });

        btnEditPlan.setFont(new java.awt.Font("Helvetica Neue", 0, 14)); // NOI18N
        btnEditPlan.setText("Edit Plan");
        btnEditPlan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditPlanActionPerformed(evt);
            }
        });

        txtRecommendation.setEditable(false);
        txtRecommendation.setColumns(20);
        txtRecommendation.setLineWrap(true);
        txtRecommendation.setRows(10);
        txtRecommendation.setWrapStyleWord(true);
        jScrollPane6.setViewportView(txtRecommendation);

        javax.swing.GroupLayout RPpanelLayout = new javax.swing.GroupLayout(RPpanel);
        RPpanel.setLayout(RPpanelLayout);
        RPpanelLayout.setHorizontalGroup(
            RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RPpanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RPpanelLayout.createSequentialGroup()
                        .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblplanid)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6))
                        .addGap(21, 21, 21)
                        .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(priorityCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblPlanID, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(18, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RPpanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnEditPlan, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(53, 53, 53)
                        .addComponent(btnSavePlan)
                        .addGap(74, 74, 74))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RPpanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(73, 73, 73))
        );
        RPpanelLayout.setVerticalGroup(
            RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RPpanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblplanid)
                    .addComponent(lblPlanID))
                .addGap(18, 18, 18)
                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RPpanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(priorityCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnSavePlan)
                            .addComponent(btnEditPlan, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12))
                    .addGroup(RPpanelLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        btnMilestoneTab.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        btnMilestoneTab.setText("View Milestones");
        btnMilestoneTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMilestoneTabActionPerformed(evt);
            }
        });

        panelFB.setBackground(new java.awt.Color(255, 255, 255));
        panelFB.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelFB.setDoubleBuffered(false);

        lblStudentID.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblStudentID.setText("Student ID:");
        lblStudentID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentID.setAlignmentY(0.0F);

        lblInfoStudentID.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoStudentID.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblInfoStudentID.setText("sid");
        lblInfoStudentID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblStudentName.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblStudentName.setText("Student Name:");
        lblStudentName.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentName.setAlignmentY(0.0F);

        lblInfoStudentName.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoStudentName.setText("sname");

        lblTitleDetails.setFont(new java.awt.Font("Serif", 1, 20)); // NOI18N
        lblTitleDetails.setText("Student Details");
        lblTitleDetails.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblCourseID.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblCourseID.setText("Course ID:");
        lblCourseID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblCourseID.setAlignmentY(0.0F);

        lblInfoCourseID.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoCourseID.setText(" cid");
        lblInfoCourseID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblCourseName.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblCourseName.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblCourseName.setText("Course Name:");
        lblCourseName.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblCourseName.setAlignmentY(0.0F);

        lblInfoCourseName.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoCourseName.setText("cname");

        lblLecturer.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblLecturer.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblLecturer.setText("Lecturer:");
        lblLecturer.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoLecturer.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoLecturer.setText("clect");

        lblSemester.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblSemester.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblSemester.setText("Semester: ");
        lblSemester.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoSemester.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoSemester.setText(" sem");

        lblAssScore.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblAssScore.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblAssScore.setText("Assignment Score:");
        lblAssScore.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoAssScore.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoAssScore.setText(" ass");
        lblInfoAssScore.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblExamScore.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblExamScore.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblExamScore.setText("Exam Score:");
        lblExamScore.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblAttempt.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblAttempt.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblAttempt.setText("Attempt:");
        lblAttempt.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoAttempt.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoAttempt.setText(" atmpt");
        lblInfoAttempt.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoExamScore.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoExamScore.setText("exam");

        lblStudentID2.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        lblStudentID2.setText("CGPA:");
        lblStudentID2.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentID2.setAlignmentY(0.0F);

        lblInfoCGPA.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoCGPA.setText("cgpa");

        panelFailureBadge.setBackground(new java.awt.Color(211, 211, 211));

        lblInfoFailure.setBackground(new java.awt.Color(255, 255, 255));
        lblInfoFailure.setFont(new java.awt.Font("Serif", 1, 15)); // NOI18N
        lblInfoFailure.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoFailure.setText(" ");

        javax.swing.GroupLayout panelFailureBadgeLayout = new javax.swing.GroupLayout(panelFailureBadge);
        panelFailureBadge.setLayout(panelFailureBadgeLayout);
        panelFailureBadgeLayout.setHorizontalGroup(
            panelFailureBadgeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFailureBadgeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblInfoFailure, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelFailureBadgeLayout.setVerticalGroup(
            panelFailureBadgeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblInfoFailure, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout panelFBLayout = new javax.swing.GroupLayout(panelFB);
        panelFB.setLayout(panelFBLayout);
        panelFBLayout.setHorizontalGroup(
            panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFBLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFBLayout.createSequentialGroup()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblStudentName))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblInfoStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblInfoStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(lblSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblInfoSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(lblCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblInfoCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lblAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addGap(34, 34, 34)
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblExamScore)
                                    .addComponent(lblAssScore)
                                    .addComponent(lblStudentID2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(lblInfoAssScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblInfoExamScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addComponent(lblInfoCGPA)))
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addGap(109, 109, 109)
                                .addComponent(panelFailureBadge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(360, 360, 360))
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addComponent(lblCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblInfoCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(panelFBLayout.createSequentialGroup()
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGap(173, 173, 173)
                        .addComponent(lblTitleDetails))
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblInfoAttempt)
                            .addComponent(lblInfoLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelFBLayout.setVerticalGroup(
            panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFBLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitleDetails, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblInfoStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAssScore, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblInfoAssScore))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblInfoStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblExamScore, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblInfoExamScore))
                .addGap(8, 8, 8)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblStudentID2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblInfoSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblInfoCGPA)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblInfoCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblInfoCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblInfoLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblInfoAttempt))
                        .addGap(16, 16, 16))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFBLayout.createSequentialGroup()
                        .addComponent(panelFailureBadge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        lblCRP1.setBackground(new java.awt.Color(0, 0, 0));
        lblCRP1.setFont(new java.awt.Font("Serif", 1, 36)); // NOI18N
        lblCRP1.setText("Course Recovery Plan");

        javax.swing.GroupLayout panelOverviewLayout = new javax.swing.GroupLayout(panelOverview);
        panelOverview.setLayout(panelOverviewLayout);
        panelOverviewLayout.setHorizontalGroup(
            panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOverviewLayout.createSequentialGroup()
                .addGap(299, 299, 299)
                .addComponent(lblCRP1, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelOverviewLayout.createSequentialGroup()
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addComponent(txtStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelOverviewLayout.createSequentialGroup()
                                .addComponent(btnSearch)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnMilestoneTab)
                                .addGap(218, 218, 218))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelOverviewLayout.createSequentialGroup()
                                .addGap(663, 663, 663)
                                .addComponent(btnCreatePlan)))
                        .addGap(8, 8, 8))
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(panelOverviewLayout.createSequentialGroup()
                                .addComponent(panelFB, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(RPpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 887, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(49, 49, 49))
        );
        panelOverviewLayout.setVerticalGroup(
            panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOverviewLayout.createSequentialGroup()
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGap(120, 120, 120)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnSearch)
                            .addComponent(txtStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addComponent(lblCRP1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCreatePlan)
                            .addComponent(btnMilestoneTab, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelFB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(RPpanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(257, Short.MAX_VALUE))
        );

        tabTwoWay.addTab("Overview", panelOverview);

        MilestonesTab.setBackground(new java.awt.Color(183, 201, 197));
        MilestonesTab.setPreferredSize(new java.awt.Dimension(870, 945));
        MilestonesTab.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel7.setFont(new java.awt.Font("Helvetica Neue", 0, 24)); // NOI18N
        jLabel7.setText("Milestone Tracking");
        MilestonesTab.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 160, -1, -1));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Week", "Task", "Completed", "Notes"
            }
        ));
        jScrollPane4.setViewportView(jTable2);

        jScrollPane5.setViewportView(jScrollPane4);

        MilestonesTab.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 210, 626, 320));
        MilestonesTab.add(jProgressBar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 560, 470, 20));

        jLabel8.setFont(new java.awt.Font("Helvetica Neue", 0, 22)); // NOI18N
        jLabel8.setText("Progress:");
        MilestonesTab.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 550, -1, 30));

        jButton11.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jButton11.setText("Update");
        jButton11.setPreferredSize(new java.awt.Dimension(72, 29));
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        MilestonesTab.add(jButton11, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 600, 110, 30));

        jButton13.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jButton13.setText("Remove");
        jButton13.setPreferredSize(new java.awt.Dimension(72, 29));
        MilestonesTab.add(jButton13, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 600, 110, 30));

        jButton14.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jButton14.setText("Mark Completed");
        MilestonesTab.add(jButton14, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 600, 170, 30));

        jButton12.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jButton12.setText("Add");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        MilestonesTab.add(jButton12, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 600, 110, 30));

        btnBack.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        MilestonesTab.add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 650, 100, 30));

        lblCRP.setBackground(new java.awt.Color(0, 0, 0));
        lblCRP.setFont(new java.awt.Font("Serif", 1, 36)); // NOI18N
        lblCRP.setText("Course Recovery Plan");
        MilestonesTab.add(lblCRP, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 50, 360, 71));

        tabTwoWay.addTab("Milestone", MilestonesTab);

        jPanel1.add(tabTwoWay, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, -30, 960, 730));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        String sid = txtStudentID.getText().trim();
        
        // === VALIDATION 1: Empty ===
        if (sid.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Student ID.");
            return;
        }

        // === VALIDATION 2: Incorrect Format ===
        if (!isValidStudentID(sid)) {
            JOptionPane.showMessageDialog(this,
                "Invalid Student ID format.\n\nCorrect format: S + 3 digits (Example: S018)",
                "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // === VALIDATION 3: Student does NOT exist ===
        Student s = fileLoader.getStudentByID(sid);
        if (s == null) {
            JOptionPane.showMessageDialog(this,
                "Student ID does not exist in the system.",
                "Not Found", JOptionPane.ERROR_MESSAGE);
            clearDetails();

            DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();
            model.setRowCount(0);
            return;
        }
        
        // === CASE A: STUDENT NOT FOUND ===
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Student not found.");
            clearDetails();
            DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();
            model.setRowCount(0);
            return;
        }

        // Clear previous info
        clearDetails();
        updateStudentInfo(s);

        // Load failed components into table
        DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();
        model.setRowCount(0);

        for (Course c : s.getCourses()) {
            String failed = c.getFailedComponent();
            if (!failed.equals("None")) {
                model.addRow(new Object[]{s.getStudentID(), c.getCourseID(), failed});
            }
        }

        // === CASE B: STUDENT EXISTS BUT PASSED ALL COURSES ===
        if (model.getRowCount() == 0) {
            txtRecommendation.setText(
                "Student has successfully passed all enrolled modules.\n" +
                "No recovery plan is required for this semester."
            );
            showPassedBadge();
            return;
        }

        // === CASE C: STUDENT FAILS AT LEAST ONE COURSE ===
        jTableFailedComponents.setRowSelectionInterval(0, 0);

        Course first = getSelectedFailedCourse();
        updateCourseInfo(first);

        // Load existing plan if exists
        String key = buildPlanKey(s.getStudentID(), first.getCourseID(), first.getAttemptNumber());
        RecoveryPlan plan = planByKey.get(key);
        txtRecommendation.setText(plan != null ? plan.getRecommendation() : "");
    }//GEN-LAST:event_btnSearchActionPerformed

    private void txtStudentIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtStudentIDActionPerformed
    String studentID = txtStudentID.getText().trim();
    //searchStudent();
    loadFailedComponents(studentID);    }//GEN-LAST:event_txtStudentIDActionPerformed


    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton12ActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        tabTwoWay.setSelectedIndex(0);
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnMilestoneTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMilestoneTabActionPerformed
        tabTwoWay.setSelectedIndex(1);
    }//GEN-LAST:event_btnMilestoneTabActionPerformed

    private void btnCreatePlanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreatePlanActionPerformed
        int row = jTableFailedComponents.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a failed row.");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();

        String sid = model.getValueAt(row, 0).toString();
        String cid = model.getValueAt(row, 1).toString();

        Student student = fileLoader.getStudentByID(sid);
        if (student == null) {
            JOptionPane.showMessageDialog(this, "Student not found in memory.");
            return;
        }

        // get course object for this student + courseID
        Course failedCourse = null;
        for (Course c : student.getCourses()) {
            if (c.getCourseID().equals(cid)) {
                failedCourse = c;
                break;
            }
        }
        if (failedCourse == null) {
            JOptionPane.showMessageDialog(this, "Course not found for this student.");
            return;
        }

        int attempt = failedCourse.getAttemptNumber();
        String key = buildPlanKey(sid, cid, attempt);

        RecoveryPlan plan = planByKey.get(key);

        if (plan == null) {
            // no existing plan → create a new one
            String planID = generatePlanID();
            plan = new RecoveryPlan(planID, student, failedCourse);

            planByKey.put(key, plan);
            planByID.put(planID, plan);
        }

        currentPlan = plan;
        populatePlanUI(currentPlan);
    }//GEN-LAST:event_btnCreatePlanActionPerformed

    private void btnSavePlanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSavePlanActionPerformed
        if (currentPlan == null) {
            JOptionPane.showMessageDialog(this, "No active recovery plan to save.");
            return;
        }

        currentPlan.setRecommendation(txtRecommendation.getText());

        savePlansToFile();

        JOptionPane.showMessageDialog(this, "Plan updated successfully.");

        txtRecommendation.setEditable(false);
        priorityCombo.setEnabled(false);
    }//GEN-LAST:event_btnSavePlanActionPerformed

    private void priorityComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_priorityComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_priorityComboActionPerformed

    private void btnEditPlanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditPlanActionPerformed
        if (currentPlan == null) {
            JOptionPane.showMessageDialog(this, "No plan selected.");
            return;
        }

        txtRecommendation.setEditable(true);
        priorityCombo.setEnabled(true);
    }//GEN-LAST:event_btnEditPlanActionPerformed
    
    private void populatePlanUI(RecoveryPlan plan) {
        lblPlanID.setText(plan.getPlanID());
        txtRecommendation.setText(plan.getRecommendation());
        // You can also show student/course info if you have labels.
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        FileLoader loader = new FileLoader();
        loader.loadAll();
        /* Create and display the form */        
        new CRP_UI(loader).setVisible(true);
        }
    });
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel MilestonesTab;
    private javax.swing.JPanel RPpanel;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnCreatePlan;
    private javax.swing.JButton btnEditPlan;
    private javax.swing.JButton btnMilestoneTab;
    private javax.swing.JButton btnSavePlan;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTableFailedComponents;
    private javax.swing.JLabel lblAssScore;
    private javax.swing.JLabel lblAttempt;
    private javax.swing.JLabel lblCRP;
    private javax.swing.JLabel lblCRP1;
    private javax.swing.JLabel lblCourseID;
    private javax.swing.JLabel lblCourseName;
    private javax.swing.JLabel lblExamScore;
    private javax.swing.JLabel lblInfoAssScore;
    private javax.swing.JLabel lblInfoAttempt;
    private javax.swing.JLabel lblInfoCGPA;
    private javax.swing.JLabel lblInfoCourseID;
    private javax.swing.JLabel lblInfoCourseName;
    private javax.swing.JLabel lblInfoExamScore;
    private javax.swing.JLabel lblInfoFailure;
    private javax.swing.JLabel lblInfoLecturer;
    private javax.swing.JLabel lblInfoSemester;
    private javax.swing.JLabel lblInfoStudentID;
    private javax.swing.JLabel lblInfoStudentName;
    private javax.swing.JLabel lblLecturer;
    private javax.swing.JLabel lblPlanID;
    private javax.swing.JLabel lblSemester;
    private javax.swing.JLabel lblStudentID;
    private javax.swing.JLabel lblStudentID2;
    private javax.swing.JLabel lblStudentName;
    private javax.swing.JLabel lblTitleDetails;
    private javax.swing.JLabel lblplanid;
    private javax.swing.JPanel panelFB;
    private javax.swing.JPanel panelFailureBadge;
    private javax.swing.JPanel panelOverview;
    private javax.swing.JComboBox<String> priorityCombo;
    private javax.swing.JTabbedPane tabTwoWay;
    private javax.swing.JTextArea txtRecommendation;
    private javax.swing.JTextField txtStudentID;
    // End of variables declaration//GEN-END:variables
}
