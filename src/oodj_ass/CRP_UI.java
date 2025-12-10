package oodj_ass;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.*;
import java.util.*;
import javax.swing.table.DefaultTableCellRenderer;



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
    private int hoveredRow = -1;
    
    /**
     * Creates new form CRP_UI
     */
    public CRP_UI(FileLoader loader) {
        this.fileLoader = loader;
        
        initComponents();

        tabTwoWay.setSelectedIndex(0);
        
        loadAllFailedStudents();
        loadRecoveryPlansFromFile();
    

        // Apply to every button in your UI
        styleFlatButton(btnSearch);
        styleFlatButton(btnCreatePlan);
        styleFlatButton(btnMilestoneTab);
        styleFlatButton(btnSavePlan);
        styleFlatButton(btnEditPlan);
        styleFlatButton(btnBack);
        styleFlatButton(btnCreateAllPlans);
        styleFlatButton(btnUpdate);
        styleFlatButton(btnAdd);
        styleFlatButton(btnRemove);
        styleFlatButton(btnMarkAsCompleted);


        // Disable plan actions until a failed course is selected
        btnCreatePlan.setEnabled(false);
        btnSavePlan.setEnabled(false);
        btnEditPlan.setEnabled(false);
        btnCreateAllPlans.setEnabled(false);

        jTableFailedComponents.setFillsViewportHeight(true);
        jTableFailedComponents.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        JScrollPane sp = (JScrollPane) jTableFailedComponents.getParent().getParent();
        sp.getVerticalScrollBar().setUnitIncrement(16);   // faster & smoother
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        
        jTableFailedComponents.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = jTableFailedComponents.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    jTableFailedComponents.repaint();
                }
            }
        });

        jTableFailedComponents.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                // Delay selection handling by 1 event cycle
                
                SwingUtilities.invokeLater(() -> {
                    int row = jTableFailedComponents.getSelectedRow();
                    if (row == -1) return;
                    Rectangle rect = jTableFailedComponents.getCellRect(row, 0, true);
                    jTableFailedComponents.scrollRectToVisible(rect);

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
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                jTableFailedComponents.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int row = jTableFailedComponents.rowAtPoint(e.getPoint());
                if (row == -1) {
                    // Clicked on empty area → clear selection & reset buttons
                    jTableFailedComponents.clearSelection();
                    currentPlan = null;
                }
            }
             @Override
            public void mouseClicked(MouseEvent evt) {
                int row = jTableFailedComponents.getSelectedRow();
                if (row == -1) return;

                Course c = getSelectedFailedCourse();
                if (c == null) return;

                String sid = jTableFailedComponents.getValueAt(row, 0).toString();
                Student s = fileLoader.getStudentByID(sid);

                updateStudentInfo(s);
                updateCourseInfo(c);

                String key = buildPlanKey(s.getStudentID(), c.getCourseID(), c.getAttemptNumber());
                RecoveryPlan plan = planByKey.get(key);
                currentPlan = plan; // store for Edit / Save

                if (plan != null) {
                    txtRecommendation.setText(plan.getRecommendation());
                } else {
                    txtRecommendation.setText("");
                }
                    btnCreatePlan.setEnabled(true);
                    btnEditPlan.setEnabled(plan != null);
                    btnSavePlan.setEnabled(true);
//                if (jTableFailedComponents.rowAtPoint(evt.getPoint()) == -1) {
//                    jTableFailedComponents.clearSelection();
//                    clearDetails();
//                    btnCreatePlan.setEnabled(false);
//                    btnEditPlan.setEnabled(false);
//                    btnSavePlan.setEnabled(false);
//                }
//                btnCreatePlan.setEnabled(true);
//                btnEditPlan.setEnabled(plan != null);
//                btnSavePlan.setEnabled(true);
            }
            
        });
        jTableFailedComponents.setShowGrid(true);
        jTableFailedComponents.setGridColor(new Color(180,180,180));
        jTableFailedComponents.setRowHeight(26);

        jTableFailedComponents.setSelectionBackground(new Color(180, 205, 230));
        jTableFailedComponents.setSelectionForeground(Color.BLACK);
        
        // ===== Hover + selection colouring for table =====
        DefaultTableCellRenderer hoverRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

                String sid = table.getValueAt(row, 0).toString();
                String cid = table.getValueAt(row, 1).toString();

                Student s = fileLoader.getStudentByID(sid);
                Course course = null;
                for (Course cc : s.getCourses()) {
                    if (cc.getCourseID().equals(cid)) {
                        course = cc;
                        break;
                    }
                }

                String key = buildPlanKey(sid, cid,
                        course != null ? course.getAttemptNumber() : 1);

                boolean hasPlan = planByKey.containsKey(key);

                if (isSelected) {
                    c.setBackground(new Color(180, 205, 230)); // selected blue
                }
                else if (row == hoveredRow) {
                    c.setBackground(new Color(215, 225, 235)); // hover light blue
                }
                else if (hasPlan) {
                    c.setBackground(new Color(230,230,230)); // planned course highlight
                }
                else {
                    // default zebra rows
                    c.setBackground(row % 2 == 0 ? new Color(245,245,245) : Color.WHITE);
                }

                return c;
            }
        };
        
        for (int i = 0; i < jTableFailedComponents.getColumnCount(); i++) {
            jTableFailedComponents.getColumnModel()
                    .getColumn(i)
                    .setCellRenderer(hoverRenderer);
        }

        
        jTableFailedComponents.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    if (row % 2 == 0) c.setBackground(new Color(245,245,245));
                    else             c.setBackground(Color.WHITE);
                }

                return c;
            }
        });
        
        // apply renderer to all columns
        for (int i = 0; i < jTableFailedComponents.getColumnCount(); i++) {
            jTableFailedComponents.getColumnModel()
                    .getColumn(i)
                    .setCellRenderer(hoverRenderer);
        }
        jScrollPane2.setWheelScrollingEnabled(true); 
    }
    
    private void styleFlatButton(JButton btn) {

        // Light theme colors (matching "Create All Plans")
        Color normal = new Color(235, 235, 235);   // light grey
        Color hover  = new Color(215, 215, 215);   // darker grey on hover
        Color click  = new Color(195, 195, 195);   // pressed


        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(normal);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                btn.setBackground(click);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                btn.setBackground(hover);
            }
        });
    }

    // Create ALL Plans for student failed > 1 course
    private void createAllPlansForStudent(String studentID) {

        Student s = fileLoader.getStudentByID(studentID);
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Student not found.");
            return;
        }

        // Gather failed courses
        List<Course> failedCourses = new ArrayList<>();

        for (Course c : s.getCourses()) {
            if (!"None".equals(c.getFailedComponent())) {
                failedCourses.add(c);
            }
        }

        // No failed courses
        if (failedCourses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "This student passed all courses.\nNo recovery plan is required.");
            return;
        }

        int createdCount = 0;

        for (Course c : failedCourses) {

            int attempt = c.getAttemptNumber();
            String key = buildPlanKey(s.getStudentID(), c.getCourseID(), attempt);

            // SAFEGUARD: Skip if plan already exists
            if (planByKey.containsKey(key)) {
                continue;
            }

            // SAFEGUARD: disallow plan creation for attempt ≥ 4
            if (attempt >= 4) {
                JOptionPane.showMessageDialog(this,
                    "Warning: Course " + c.getCourseID() + " has reached maximum retake attempts.\n"
                  + "Please refer this case to the Programme Head.");
                continue;
            }

            // Create new recovery plan
            String planID = generatePlanID();
            RecoveryPlan newPlan = new RecoveryPlan(planID, s, c);

            planByKey.put(key, newPlan);
            planByID.put(planID, newPlan);

            createdCount++;
        }

        savePlansToFile();

        if (createdCount == 0) {
            JOptionPane.showMessageDialog(this,
                "No new plans created.\nAll recovery plans already exist for this student.");
        } else {
            JOptionPane.showMessageDialog(this,
                createdCount + " recovery plans created successfully!");
        }

        refreshFailedCoursesTableHighlight();
    }

    private void refreshFailedCoursesTableHighlight() {
        jTableFailedComponents.repaint();
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
            
                SwingUtilities.invokeLater(() -> {
                    jTableFailedComponents.dispatchEvent(
                            new MouseEvent(
                                jTableFailedComponents, 
                                MouseEvent.MOUSE_RELEASED, 
                                System.currentTimeMillis(),
                                0, 0, 0, 1, false
                            )
                    );
                });
        }

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
    
    private String formatRecommendation(String text) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("=== Recommendation Summary ===\n\n");

        String[] parts = text.split("\\. ");
        for (String p : parts) {
            p = p.trim();
            if (!p.isEmpty()) sb.append("• ").append(p).append(".\n");
        }

        sb.append("\n===============================");
        return sb.toString();
    }

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

        if (type.equals("None")) {
            lblInfoFailure.setText("PASSED");
            panelFailureBadge.setBackground(new Color(180, 230, 180));
        } else {
            lblInfoFailure.setText(type);

            switch (type) {
                case "Assignment Only":
                    panelFailureBadge.setBackground(new Color(176, 223, 232));
                    break;
                case "Exam Only":
                    panelFailureBadge.setBackground(new Color(250, 220, 160));
                    break;
                case "Both Components":
                    panelFailureBadge.setBackground(new Color(255, 165, 156));
                    break;
            }
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

        txtRecommendation.setText("");
        lblPlanID.setText("");

        panelFailureBadge.setBackground(new Color(230,230,230));
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
        panelFailureBadge = new javax.swing.JPanel();
        lblInfoFailure = new javax.swing.JLabel();
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
        jSeparator1 = new javax.swing.JSeparator();
        btnMilestoneTab = new javax.swing.JButton();
        lblCRP1 = new javax.swing.JLabel();
        btnCreateAllPlans = new javax.swing.JButton();
        MilestonesTab = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel8 = new javax.swing.JLabel();
        btnUpdate = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        btnMarkAsCompleted = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
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

        btnSearch.setFont(new java.awt.Font("Helvetica Neue", 0, 14)); // NOI18N
        btnSearch.setText("Search");
        btnSearch.setBorder(null);
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        btnCreatePlan.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        btnCreatePlan.setText("Create Plan");
        btnCreatePlan.setBorder(null);
        btnCreatePlan.setBorderPainted(false);
        btnCreatePlan.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
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
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTableFailedComponents);
        if (jTableFailedComponents.getColumnModel().getColumnCount() > 0) {
            jTableFailedComponents.getColumnModel().getColumn(0).setResizable(false);
            jTableFailedComponents.getColumnModel().getColumn(1).setResizable(false);
            jTableFailedComponents.getColumnModel().getColumn(2).setResizable(false);
        }

        RPpanel.setBackground(new java.awt.Color(255, 255, 255));
        RPpanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        RPpanel.setAlignmentX(0.0F);
        RPpanel.setAlignmentY(0.0F);

        jLabel2.setFont(new java.awt.Font("Serif", 1, 20)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Recovery Plan Details");

        lblplanid.setFont(new java.awt.Font("Serif", 1, 15)); // NOI18N
        lblplanid.setText("Plan ID:");

        lblPlanID.setFont(new java.awt.Font("Serif", 1, 15)); // NOI18N
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
        txtRecommendation.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        txtRecommendation.setLineWrap(true);
        txtRecommendation.setRows(10);
        txtRecommendation.setWrapStyleWord(true);
        jScrollPane6.setViewportView(txtRecommendation);

        panelFailureBadge.setBackground(new java.awt.Color(211, 211, 211));

        lblInfoFailure.setBackground(new java.awt.Color(255, 255, 255));
        lblInfoFailure.setFont(new java.awt.Font("Serif", 1, 13)); // NOI18N
        lblInfoFailure.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoFailure.setText(" ");

        javax.swing.GroupLayout panelFailureBadgeLayout = new javax.swing.GroupLayout(panelFailureBadge);
        panelFailureBadge.setLayout(panelFailureBadgeLayout);
        panelFailureBadgeLayout.setHorizontalGroup(
            panelFailureBadgeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFailureBadgeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblInfoFailure, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelFailureBadgeLayout.setVerticalGroup(
            panelFailureBadgeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblInfoFailure, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout RPpanelLayout = new javax.swing.GroupLayout(RPpanel);
        RPpanel.setLayout(RPpanelLayout);
        RPpanelLayout.setHorizontalGroup(
            RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RPpanelLayout.createSequentialGroup()
                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblplanid)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RPpanelLayout.createSequentialGroup()
                        .addComponent(priorityCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(panelFailureBadge, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPlanID, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RPpanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RPpanelLayout.createSequentialGroup()
                        .addComponent(btnEditPlan)
                        .addGap(56, 56, 56)
                        .addComponent(btnSavePlan)
                        .addGap(74, 74, 74))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RPpanelLayout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(73, 73, 73))))
        );
        RPpanelLayout.setVerticalGroup(
            RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RPpanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(RPpanelLayout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panelFailureBadge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(RPpanelLayout.createSequentialGroup()
                        .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblplanid)
                            .addComponent(lblPlanID, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(RPpanelLayout.createSequentialGroup()
                                .addGap(111, 111, 111)
                                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6)
                                    .addComponent(priorityCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(RPpanelLayout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addGap(117, 117, 117)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 20, Short.MAX_VALUE)
                .addGroup(RPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnEditPlan, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                    .addComponent(btnSavePlan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        panelFB.setBackground(new java.awt.Color(255, 255, 255));
        panelFB.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        panelFB.setDoubleBuffered(false);

        lblStudentID.setFont(new java.awt.Font("Serif", 0, 15)); // NOI18N
        lblStudentID.setText("Student ID:");
        lblStudentID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentID.setAlignmentY(0.0F);

        lblInfoStudentID.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoStudentID.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblInfoStudentID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblStudentName.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblStudentName.setText("Student Name:");
        lblStudentName.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentName.setAlignmentY(0.0F);

        lblInfoStudentName.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N

        lblTitleDetails.setFont(new java.awt.Font("Serif", 1, 20)); // NOI18N
        lblTitleDetails.setText("Student Details");
        lblTitleDetails.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblCourseID.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblCourseID.setText("Course ID:");
        lblCourseID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblCourseID.setAlignmentY(0.0F);

        lblInfoCourseID.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoCourseID.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblCourseName.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblCourseName.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblCourseName.setText("Course Name:");
        lblCourseName.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblCourseName.setAlignmentY(0.0F);

        lblInfoCourseName.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N

        lblLecturer.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblLecturer.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblLecturer.setText("Lecturer:");
        lblLecturer.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoLecturer.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N

        lblSemester.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblSemester.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblSemester.setText("Semester: ");
        lblSemester.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoSemester.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N

        lblAssScore.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblAssScore.setText("Assignment Score:");
        lblAssScore.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoAssScore.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoAssScore.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblExamScore.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblExamScore.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblExamScore.setText("Exam Score:");
        lblExamScore.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblAttempt.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblAttempt.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblAttempt.setText("Attempt:");
        lblAttempt.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoAttempt.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblInfoAttempt.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblInfoExamScore.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N

        lblStudentID2.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        lblStudentID2.setText("CGPA:");
        lblStudentID2.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        lblStudentID2.setAlignmentY(0.0F);

        lblInfoCGPA.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N

        jSeparator1.setToolTipText("");

        btnMilestoneTab.setFont(new java.awt.Font("Helvetica Neue", 0, 17)); // NOI18N
        btnMilestoneTab.setText("View Milestones");
        btnMilestoneTab.setBorder(null);
        btnMilestoneTab.setBorderPainted(false);
        btnMilestoneTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMilestoneTabActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelFBLayout = new javax.swing.GroupLayout(panelFB);
        panelFB.setLayout(panelFBLayout);
        panelFBLayout.setHorizontalGroup(
            panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFBLayout.createSequentialGroup()
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 454, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGap(161, 161, 161)
                        .addComponent(lblTitleDetails))
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(lblLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblInfoLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(panelFBLayout.createSequentialGroup()
                                        .addComponent(lblInfoAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnMilestoneTab, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelFBLayout.createSequentialGroup()
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
                                                .addComponent(lblInfoSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lblStudentID2)
                                            .addComponent(lblExamScore)
                                            .addComponent(lblAssScore))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(lblInfoExamScore, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                                            .addComponent(lblInfoCGPA, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(lblInfoAssScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                    .addGroup(panelFBLayout.createSequentialGroup()
                                        .addComponent(lblCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(lblInfoCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panelFBLayout.createSequentialGroup()
                                        .addComponent(lblCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(lblInfoCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(lblAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        panelFBLayout.setVerticalGroup(
            panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelFBLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitleDetails, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblInfoStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addComponent(lblAssScore, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblInfoAssScore, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblInfoStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblInfoExamScore, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblExamScore, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lblInfoCGPA, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblInfoSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblStudentID2, javax.swing.GroupLayout.Alignment.TRAILING)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblInfoCourseID, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCourseID, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblInfoCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblInfoLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(lblInfoAttempt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblAttempt, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE))
                    .addComponent(btnMilestoneTab, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        lblCRP1.setBackground(new java.awt.Color(0, 0, 0));
        lblCRP1.setFont(new java.awt.Font("Serif", 1, 36)); // NOI18N
        lblCRP1.setText("Course Recovery Plan");

        btnCreateAllPlans.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        btnCreateAllPlans.setText("Create All Plans");
        btnCreateAllPlans.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateAllPlansActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelOverviewLayout = new javax.swing.GroupLayout(panelOverview);
        panelOverview.setLayout(panelOverviewLayout);
        panelOverviewLayout.setHorizontalGroup(
            panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOverviewLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(txtStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnCreateAllPlans)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnCreatePlan, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(67, 67, 67))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelOverviewLayout.createSequentialGroup()
                .addGap(0, 20, Short.MAX_VALUE)
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 865, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addComponent(panelFB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(RPpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(65, 65, 65))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelOverviewLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblCRP1, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(264, 264, 264))
        );
        panelOverviewLayout.setVerticalGroup(
            panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelOverviewLayout.createSequentialGroup()
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGap(110, 110, 110)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(20, 20, 20))
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btnCreatePlan, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnCreateAllPlans))
                            .addGroup(panelOverviewLayout.createSequentialGroup()
                                .addGap(40, 40, 40)
                                .addComponent(lblCRP1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(RPpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelFB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        MilestonesTab.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 200, 640, 110));
        MilestonesTab.add(jProgressBar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 330, 470, 20));

        jLabel8.setFont(new java.awt.Font("Helvetica Neue", 0, 22)); // NOI18N
        jLabel8.setText("Progress:");
        MilestonesTab.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 320, -1, 30));

        btnUpdate.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnUpdate.setText("Update");
        btnUpdate.setPreferredSize(new java.awt.Dimension(72, 29));
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });
        MilestonesTab.add(btnUpdate, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 400, 110, 30));

        btnRemove.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnRemove.setText("Remove");
        btnRemove.setPreferredSize(new java.awt.Dimension(72, 29));
        MilestonesTab.add(btnRemove, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 400, 110, 30));

        btnMarkAsCompleted.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnMarkAsCompleted.setText("Mark Completed");
        MilestonesTab.add(btnMarkAsCompleted, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 400, 170, 30));

        btnAdd.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnAdd.setText("Add");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });
        MilestonesTab.add(btnAdd, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 400, 110, 30));

        btnBack.setFont(new java.awt.Font("Helvetica Neue", 0, 16)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        MilestonesTab.add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 550, 100, 30));

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


    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnAddActionPerformed

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
                JOptionPane.showMessageDialog(this, "No recovery plan selected.");
                return;
            }

            currentPlan.setRecommendation(txtRecommendation.getText());
//            currentPlan.setPriority(priorityCombo.getSelectedItem().toString());
//            currentPlan.updateLastUpdated();

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
            JOptionPane.showMessageDialog(this, "No recovery plan selected.");
            return;
        }
        txtRecommendation.setEditable(true);
        priorityCombo.setEnabled(true);
        btnSavePlan.setEnabled(true);
    }//GEN-LAST:event_btnEditPlanActionPerformed

    private void btnCreateAllPlansActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateAllPlansActionPerformed
        String studentID = lblInfoStudentID.getText().trim();
        if (studentID.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please search a student first.");
            return;
        }
        createAllPlansForStudent(studentID);
    }//GEN-LAST:event_btnCreateAllPlansActionPerformed
    
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
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnCreateAllPlans;
    private javax.swing.JButton btnCreatePlan;
    private javax.swing.JButton btnEditPlan;
    private javax.swing.JButton btnMarkAsCompleted;
    private javax.swing.JButton btnMilestoneTab;
    private javax.swing.JButton btnRemove;
    private javax.swing.JButton btnSavePlan;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSeparator jSeparator1;
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
