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

    private int lastPlanNumber = 0; // for RP0001, RP0002, ...
    private String recommendation;
    
    
    /**
     * Creates new form CRP_UI
     */
    public CRP_UI(FileLoader loader) {
        this.fileLoader = loader;
        initComponents();
        
        jTabbedPane1.setUI(new BasicTabbedPaneUI() {
        @Override
        protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
            }
        });
        
        loadAllFailedStudents();
        loadRecoveryPlansFromFile();
        
        jTableFailedComponents.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                int row = jTableFailedComponents.getSelectedRow();
                if (row == -1) return;

                String sid = jTableFailedComponents.getValueAt(row, 0).toString();
                String cid = jTableFailedComponents.getValueAt(row, 1).toString();

                Student s = fileLoader.getStudentByID(sid);
                Course c = fileLoader.getCourseByID(cid); 

                updateStudentInfo(s);
                updateCourseInfo(c);
                txtRecommendation.setText(""); // clear previous
            }
        });


    }
    
    private void loadFailedComponents(String studentID) {
        DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();
        model.setRowCount(0);

        Student student = fileLoader.getStudentByID(studentID);

        if (student == null) {
            JOptionPane.showMessageDialog(this, "Student not found.");
            return;
        }

        for (Course c : student.getCourses()) {
            int ass  = c.getAssScore();
            int exam = c.getExamScore();

            StringBuilder failed = new StringBuilder();

            if (ass < 50) failed.append("Assignment ");
            if (exam < 50) failed.append("Exam ");

            if (failed.length() == 0) continue;

            model.addRow(new Object[] {
                studentID,
                c.getCourseID(),
                failed.toString().trim()
            });
        }

        if (model.getRowCount() == 0) {
            lblPlanID.setText(" ");
            txtRecommendation.setText(
                "Student has successfully passed all enrolled modules.\n" +
                "No recovery plan is required for this semester."
            );
            updateCourseInfo(null);
            return;
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
    
    private void updateStudentInfo(Student s) {
        if (s == null) {
            lblStudentID.setText("");
            lblInfoStudentName.setText("");
            lblInfoCGPA.setText("");
            return;
        }

        lblStudentID.setText(s.getStudentID());
        lblInfoStudentName.setText(s.getFullName());
        lblInfoCGPA.setText(String.format("%.2f", s.getCgpa()));
    }

    private void updateCourseInfo(Course c) {
        if (c == null) {
            lblInfoCourseID.setText("");
            lblInfoCourseName.setText("");
            lblInfoLecturer.setText("");
            lblInfoSemester.setText("");
            lblAttempt.setText("");
            lblInfoAssScore.setText("");
            lblInfoExamScore.setText("");
            panelFailureBadge.setBackground(new Color(230,230,230));
            return;
        }

        lblInfoCourseID.setText(c.getCourseID());
        lblInfoCourseName.setText(c.getCourseName());
        lblInfoLecturer.setText(c.getCourseInstructor());
        lblInfoSemester.setText(c.getSemester());
        lblAttempt.setText(String.valueOf(c.getAttemptNumber()));

        lblInfoAssScore.setText(String.valueOf(c.getAssScore()));
        lblInfoExamScore.setText(String.valueOf(c.getExamScore()));

        // Failure badge
        String type = c.getFailedComponent(); 
        switch (type) {
            case "Assignment Only":
                panelFailureBadge.setBackground(new Color(255, 204, 204)); // pale red  
                break;
            case "Exam Only":
                panelFailureBadge.setBackground(new Color(255, 230, 180)); // pale orange
                break;
            case "Both Components":
                panelFailureBadge.setBackground(new Color(255, 180, 180)); // stronger pale red
                break;
            default:
                panelFailureBadge.setBackground(new Color(200, 230, 200)); // pale green (passed)
                break;
        }
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
        jButton1 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
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
        panelOverview = new javax.swing.JPanel();
        txtStudentID = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        btnCreatePlan = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableFailedComponents = new javax.swing.JTable();
        OverviewTab = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblplanid = new javax.swing.JLabel();
        lblPlanID = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        priorityCombo = new javax.swing.JComboBox<>();
        btnSavePlan = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        txtRecommendation = new javax.swing.JTextArea();
        btnMilestoneTab = new javax.swing.JButton();
        panelFB = new javax.swing.JPanel();
        lblInfoCourseID = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        lblInfoCourseName = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        lblInfoSemester = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        lblAttempt = new javax.swing.JLabel();
        panelFailureBadge = new javax.swing.JPanel();
        lblInfoFailure = new javax.swing.JLabel();
        lblInfoAttempt = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        lblInfoExamScore = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        lblInfoAssScore = new javax.swing.JLabel();
        lblInfoLecturer = new javax.swing.JLabel();
        panelStudentInfo = new javax.swing.JPanel();
        lblStudentInfo = new javax.swing.JLabel();
        lblStudentID = new javax.swing.JLabel();
        lblStudentID1 = new javax.swing.JLabel();
        lblStudentID2 = new javax.swing.JLabel();
        lblInfoStudentID = new javax.swing.JLabel();
        lblInfoStudentName = new javax.swing.JLabel();
        lblInfoCGPA = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();

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

        jButton1.setBackground(new java.awt.Color(95, 106, 106));
        jButton1.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("User Management");
        jButton1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton7.setBackground(new java.awt.Color(95, 106, 106));
        jButton7.setFont(new java.awt.Font("Serif", 0, 17)); // NOI18N
        jButton7.setForeground(new java.awt.Color(255, 255, 255));
        jButton7.setText("Course Recovery Plan");
        jButton7.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButton8.setBackground(new java.awt.Color(95, 106, 106));
        jButton8.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        jButton8.setForeground(new java.awt.Color(255, 255, 255));
        jButton8.setText("Academic Performance Report");
        jButton8.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jButton2.setBackground(new java.awt.Color(95, 106, 106));
        jButton2.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setText("Eligibility Check & Enrolment");
        jButton2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jButton7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jButton8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(76, 76, 76)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(378, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 100, -1, 600));

        jPanel3.setBackground(new java.awt.Color(95, 106, 105));
        jPanel3.setPreferredSize(new java.awt.Dimension(950, 100));

        jLabel1.setFont(new java.awt.Font("Serif", 1, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Course Recovery Plan");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(516, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(312, 312, 312))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 71, Short.MAX_VALUE)
                .addGap(15, 15, 15))
        );

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-1, 0, 1170, 100));

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(870, 945));

        MilestonesTab.setPreferredSize(new java.awt.Dimension(870, 945));
        MilestonesTab.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel7.setFont(new java.awt.Font("Helvetica Neue", 0, 24)); // NOI18N
        jLabel7.setText("Milestone Tracking");
        MilestonesTab.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 43, -1, -1));

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

        MilestonesTab.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 84, 626, 320));
        MilestonesTab.add(jProgressBar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(257, 432, 470, 20));

        jLabel8.setFont(new java.awt.Font("Helvetica Neue", 0, 22)); // NOI18N
        jLabel8.setText("Progress:");
        MilestonesTab.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 425, -1, 30));

        jButton11.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jButton11.setText("Update");
        jButton11.setPreferredSize(new java.awt.Dimension(72, 29));
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        MilestonesTab.add(jButton11, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 490, 110, 30));

        jButton13.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jButton13.setText("Remove");
        jButton13.setPreferredSize(new java.awt.Dimension(72, 29));
        MilestonesTab.add(jButton13, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 490, 110, 30));

        jButton14.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jButton14.setText("Mark Completed");
        MilestonesTab.add(jButton14, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 490, 170, 30));

        jButton12.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jButton12.setText("Add");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });
        MilestonesTab.add(jButton12, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 490, 110, 30));

        btnBack.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        MilestonesTab.add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(815, 540, 100, 30));

        jTabbedPane1.addTab("Milestone", MilestonesTab);

        panelOverview.setBackground(new java.awt.Color(183, 201, 197));
        panelOverview.setPreferredSize(new java.awt.Dimension(944, 589));

        txtStudentID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtStudentIDActionPerformed(evt);
            }
        });

        jButton3.setText("Search");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        btnCreatePlan.setFont(new java.awt.Font("Helvetica Neue", 0, 15)); // NOI18N
        btnCreatePlan.setText("Create Plan");
        btnCreatePlan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreatePlanActionPerformed(evt);
            }
        });

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

        OverviewTab.setBackground(new java.awt.Color(255, 255, 255));
        OverviewTab.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel2.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Recovery Plan Details");

        lblplanid.setFont(new java.awt.Font("Helvetica Neue", 1, 14)); // NOI18N
        lblplanid.setText("Plan ID:");

        lblPlanID.setFont(new java.awt.Font("Tiro Devanagari Sanskrit", 1, 15)); // NOI18N
        lblPlanID.setText(" ");

        jLabel5.setFont(new java.awt.Font("Helvetica Neue", 1, 14)); // NOI18N
        jLabel5.setText("Recommendation:");

        jLabel6.setFont(new java.awt.Font("Helvetica Neue", 1, 14)); // NOI18N
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

        jButton10.setFont(new java.awt.Font("Helvetica Neue", 0, 14)); // NOI18N
        jButton10.setText("Edit Plan");

        txtRecommendation.setEditable(false);
        txtRecommendation.setColumns(20);
        txtRecommendation.setLineWrap(true);
        txtRecommendation.setRows(10);
        txtRecommendation.setWrapStyleWord(true);
        jScrollPane6.setViewportView(txtRecommendation);

        javax.swing.GroupLayout OverviewTabLayout = new javax.swing.GroupLayout(OverviewTab);
        OverviewTab.setLayout(OverviewTabLayout);
        OverviewTabLayout.setHorizontalGroup(
            OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(OverviewTabLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(OverviewTabLayout.createSequentialGroup()
                        .addGroup(OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblplanid)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6))
                        .addGap(21, 21, 21)
                        .addGroup(OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(priorityCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblPlanID, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(18, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, OverviewTabLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, OverviewTabLayout.createSequentialGroup()
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(84, 84, 84))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, OverviewTabLayout.createSequentialGroup()
                                .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(53, 53, 53)
                                .addComponent(btnSavePlan)
                                .addGap(74, 74, 74))))))
        );
        OverviewTabLayout.setVerticalGroup(
            OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(OverviewTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblplanid)
                    .addComponent(lblPlanID))
                .addGap(18, 18, 18)
                .addGroup(OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(OverviewTabLayout.createSequentialGroup()
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE)
                        .addGroup(OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(priorityCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(OverviewTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnSavePlan)
                            .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12))
                    .addGroup(OverviewTabLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addContainerGap(187, Short.MAX_VALUE))))
        );

        btnMilestoneTab.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        btnMilestoneTab.setText("View Milestones");
        btnMilestoneTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMilestoneTabActionPerformed(evt);
            }
        });

        panelFB.setBackground(new java.awt.Color(255, 255, 255));
        panelFB.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panelFB.setDoubleBuffered(false);

        lblInfoCourseID.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoCourseID.setText(" ");

        jLabel9.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        jLabel9.setText("Course ID:");
        jLabel9.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel9.setAlignmentY(0.0F);

        jLabel10.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel10.setText("Course Name:");
        jLabel10.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel10.setAlignmentY(0.0F);

        lblInfoCourseName.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoCourseName.setText(" ");

        jLabel11.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel11.setText("Lecturer:");
        jLabel11.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        jLabel12.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel12.setText("Semester: ");
        jLabel12.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoSemester.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoSemester.setText(" ");

        jLabel13.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel13.setText("Attempt:");
        jLabel13.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblAttempt.setText(" ");

        panelFailureBadge.setBackground(new java.awt.Color(211, 211, 211));

        lblInfoFailure.setBackground(new java.awt.Color(255, 255, 255));
        lblInfoFailure.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        lblInfoFailure.setText(" ");

        javax.swing.GroupLayout panelFailureBadgeLayout = new javax.swing.GroupLayout(panelFailureBadge);
        panelFailureBadge.setLayout(panelFailureBadgeLayout);
        panelFailureBadgeLayout.setHorizontalGroup(
            panelFailureBadgeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFailureBadgeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblInfoFailure, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addGap(22, 22, 22))
        );
        panelFailureBadgeLayout.setVerticalGroup(
            panelFailureBadgeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFailureBadgeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblInfoFailure, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblInfoAttempt.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoAttempt.setText(" ");

        jLabel4.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        jLabel4.setText("Course Information");

        jLabel15.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel15.setText("Assignment Score:");
        jLabel15.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoExamScore.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoExamScore.setText(" ");

        jLabel16.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel16.setText("Exam Score:");
        jLabel16.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoAssScore.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoAssScore.setText(" ");

        lblInfoLecturer.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoLecturer.setText(" ");

        javax.swing.GroupLayout panelFBLayout = new javax.swing.GroupLayout(panelFB);
        panelFB.setLayout(panelFBLayout);
        panelFBLayout.setHorizontalGroup(
            panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFBLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblInfoCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblInfoCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(jLabel16)
                                .addGap(47, 47, 47)
                                .addComponent(lblInfoExamScore, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelFBLayout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblInfoAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(panelFailureBadge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(46, 46, 46)
                                .addComponent(lblAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblInfoLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel12))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblInfoSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblInfoAssScore, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(panelFBLayout.createSequentialGroup()
                .addGap(86, 86, 86)
                .addComponent(jLabel4)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        panelFBLayout.setVerticalGroup(
            panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFBLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblInfoCourseID)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblInfoAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(1, 1, 1)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblInfoCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblInfoLecturer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGap(53, 53, 53)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblAttempt)
                            .addComponent(panelFailureBadge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblInfoSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblInfoAssScore, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(1, 1, 1)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblInfoExamScore))))
                .addGap(9, 9, 9))
        );

        panelStudentInfo.setBackground(new java.awt.Color(219, 230, 218));
        panelStudentInfo.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        lblStudentInfo.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        lblStudentInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblStudentInfo.setText("Student Info");

        lblStudentID.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        lblStudentID.setText("Student ID:");
        lblStudentID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentID.setAlignmentY(0.0F);

        lblStudentID1.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        lblStudentID1.setText("Student Name:");
        lblStudentID1.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentID1.setAlignmentY(0.0F);

        lblStudentID2.setFont(new java.awt.Font("Al Tarikh", 0, 13)); // NOI18N
        lblStudentID2.setText("CGPA:");
        lblStudentID2.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentID2.setAlignmentY(0.0F);

        lblInfoStudentID.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoStudentID.setText(" ");

        lblInfoStudentName.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoStudentName.setText(" ");

        lblInfoCGPA.setFont(new java.awt.Font("Helvetica Neue", 0, 13)); // NOI18N
        lblInfoCGPA.setText(" ");

        javax.swing.GroupLayout panelStudentInfoLayout = new javax.swing.GroupLayout(panelStudentInfo);
        panelStudentInfo.setLayout(panelStudentInfoLayout);
        panelStudentInfoLayout.setHorizontalGroup(
            panelStudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStudentInfoLayout.createSequentialGroup()
                .addGroup(panelStudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelStudentInfoLayout.createSequentialGroup()
                        .addGap(62, 62, 62)
                        .addComponent(lblStudentInfo))
                    .addGroup(panelStudentInfoLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblStudentID)
                        .addGap(29, 29, 29)
                        .addComponent(lblInfoStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(panelStudentInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelStudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblStudentID1)
                    .addComponent(lblStudentID2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelStudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelStudentInfoLayout.createSequentialGroup()
                        .addComponent(lblInfoCGPA, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(44, Short.MAX_VALUE))
                    .addComponent(lblInfoStudentName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        panelStudentInfoLayout.setVerticalGroup(
            panelStudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStudentInfoLayout.createSequentialGroup()
                .addComponent(lblStudentInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelStudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelStudentInfoLayout.createSequentialGroup()
                        .addComponent(lblInfoStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelStudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblStudentID1, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelStudentInfoLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(lblInfoStudentName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelStudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentID2, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblInfoCGPA))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panelOverviewLayout = new javax.swing.GroupLayout(panelOverview);
        panelOverview.setLayout(panelOverviewLayout);
        panelOverviewLayout.setHorizontalGroup(
            panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOverviewLayout.createSequentialGroup()
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addComponent(panelStudentInfo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(83, 83, 83)
                        .addComponent(btnMilestoneTab))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelOverviewLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(panelOverviewLayout.createSequentialGroup()
                                    .addComponent(txtStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(jButton3))
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 462, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(btnCreatePlan, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(OverviewTab, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelFB, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(0, 62, Short.MAX_VALUE))
        );
        panelOverviewLayout.setVerticalGroup(
            panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOverviewLayout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton3))
                        .addGap(24, 24, 24)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 264, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnCreatePlan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnMilestoneTab, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(panelStudentInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addComponent(panelFB, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(OverviewTab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(62, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Overview", panelOverview);

        jPanel1.add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 60, 950, 640));

        jButton4.setText("Course Recovery Plan");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

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
    
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

    private void txtStudentIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtStudentIDActionPerformed
        String studentID = txtStudentID.getText().trim();

        if (studentID.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Student ID.");
            loadFailedComponents(studentID);
        } else{

    loadFailedComponents(studentID);    }//GEN-LAST:event_txtStudentIDActionPerformed
    }

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton12ActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        jTabbedPane1.setSelectedIndex(0);
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnMilestoneTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMilestoneTabActionPerformed
        jTabbedPane1.setSelectedIndex(1);
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
            JOptionPane.showMessageDialog(this, "No recovery plan to save.");
            return;
        }

        // update recommendation from UI
        String recText = txtRecommendation.getText().trim();
        currentPlan.setRecommendation(recText);

        savePlansToFile();
        JOptionPane.showMessageDialog(this, "Recovery plan saved.");
    }//GEN-LAST:event_btnSavePlanActionPerformed

    private void priorityComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_priorityComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_priorityComboActionPerformed
    
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
    private javax.swing.JPanel OverviewTab;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnCreatePlan;
    private javax.swing.JButton btnMilestoneTab;
    private javax.swing.JButton btnSavePlan;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTableFailedComponents;
    private javax.swing.JLabel lblAttempt;
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
    private javax.swing.JLabel lblPlanID;
    private javax.swing.JLabel lblStudentID;
    private javax.swing.JLabel lblStudentID1;
    private javax.swing.JLabel lblStudentID2;
    private javax.swing.JLabel lblStudentInfo;
    private javax.swing.JLabel lblplanid;
    private javax.swing.JPanel panelFB;
    private javax.swing.JPanel panelFailureBadge;
    private javax.swing.JPanel panelOverview;
    private javax.swing.JPanel panelStudentInfo;
    private javax.swing.JComboBox<String> priorityCombo;
    private javax.swing.JTextArea txtRecommendation;
    private javax.swing.JTextField txtStudentID;
    // End of variables declaration//GEN-END:variables
}
