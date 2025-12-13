package oodj_ass;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.*;
import java.util.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;


public class CRP_UI extends javax.swing.JFrame {
    private User currentUser;
    private FileLoader fileLoader;
    private CRP crp;
   
    private static final String RECOVERY_PLAN_FILE = "data/recoveryPlans.txt";
    private static final String MILESTONE_FILE = "data/recoveryMilestones.txt";

    // key = "studentID|courseID|attemptNum"
    private Map<String, RecoveryPlan> planByKey = new LinkedHashMap<>();
    // key = planID
    private Map<String, RecoveryPlan> planByID = new LinkedHashMap<>();
    
    private RecoveryPlan currentPlan;
    private int lastPlanNumber = 0; 
    private String recommendation;
    private int hoveredRow = -1;
    private int hoveredMilestoneRow = -1;
    
    public CRP_UI(User user) {
        this.currentUser = user;
        initEverything();
    }

    // default constructor for testing
    public CRP_UI() {
        this.currentUser = null;
        initEverything();
    }

    private void initEverything() {
        fileLoader = new FileLoader();
        fileLoader.loadAll();

        Email mailer = new Email();
        crp = new CRP(fileLoader.getStudents(), mailer);

        initComponents();
        loadAllFailedStudents();
        loadRecoveryPlansFromFile();
        loadMilestonesFromFile();
        
        // Style all buttons
        styleFlatButton(btnSearch);
        styleFlatButton(btnCreatePlan);
        styleFlatButton(btnMilestoneTab);
        styleFlatButton(btnSavePlan);
        styleFlatButton(btnEditPlan);
        styleFlatButton(btnBack);
        styleFlatButton(btnCreateAllPlans);
        styleFlatButton(btnEdit);
        styleFlatButton(btnAdd);
        styleFlatButton(btnRemove);
        styleFlatButton(btnMarkAsCompleted);
        
        // Disable until a row is selected
        btnCreatePlan.setEnabled(false);
        btnSavePlan.setEnabled(false);
        btnEditPlan.setEnabled(false);
        btnCreateAllPlans.setEnabled(false);
        
        // ---- Milestone table basic setup ----
        jTableMilestones.setShowGrid(true);
        jTableMilestones.setGridColor(new Color(180, 180, 180));  // light grey
        jTableMilestones.setIntercellSpacing(new Dimension(1, 1));
        jTableMilestones.setRowHeight(30);

        // Set bounds 
        lblSelectCourse.setBounds(40, 70, 150, 25);
        comboxCourseSelector.setBounds(185, 70, 180, 25);

        // ---- Failed components table scroll + header ----
        jTableFailedComponents.setFillsViewportHeight(true);
        jTableFailedComponents.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JTableHeader hd = jTableFailedComponents.getTableHeader();
        hd.setFont(new Font("Serif", Font.BOLD, 16)); 

        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(
                JScrollPane.class, jTableFailedComponents
        );
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        
        // ==== TABLE HOVER HANDLER ====
        jTableFailedComponents.addMouseMotionListener(new MouseMotionAdapter() {
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
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                jTableFailedComponents.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int row = jTableFailedComponents.rowAtPoint(e.getPoint());
                if (row == -1) {
                    jTableFailedComponents.clearSelection();
                    currentPlan = null;
                    clearDetails();
                }
            }
        });

        // ==== SINGLE SELECTION LISTENER (MAIN LOGIC) ====
        jTableFailedComponents.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleFailedRowSelection();
            }
        });

        // ==== TABLE GRID + ROW COLOUR RENDERER ====
        jTableFailedComponents.setShowGrid(true);
        jTableFailedComponents.setGridColor(new Color(180, 180, 180));
        jTableFailedComponents.setRowHeight(26);
        jTableFailedComponents.setSelectionBackground(new Color(180, 205, 230));
        jTableFailedComponents.setSelectionForeground(Color.BLACK);

        DefaultTableCellRenderer rowRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                String sid = table.getValueAt(row, 0).toString();
                String cid = table.getValueAt(row, 1).toString();

                Student s = fileLoader.getStudentByID(sid);
                Course course = null;
                if (s != null) {
                    for (Course cc : s.getCourses()) {
                        if (cc.getCourseID().equals(cid)) {
                            course = cc;
                            break;
                        }
                    }
                }

                String key = buildPlanKey(
                        sid,
                        cid,
                        (course != null ? course.getAttemptNumber() : 1)
                );

                boolean hasPlan = planByKey.containsKey(key);

                // Priority: selected → hover → planned → default
                if (isSelected) {
                    c.setBackground(new Color(180,205,230)); // selected
                }
                else if (row == hoveredRow) {
                    c.setBackground(new Color(215,225,235)); // hover
                }
                else if (hasPlan) {
                    c.setBackground(new Color(235,235,237)); // planned grey
                }
                else {
                    c.setBackground(Color.WHITE); // default
                }

                return c;
            }
        };
        for (int i = 0; i < jTableFailedComponents.getColumnCount(); i++) {
            jTableFailedComponents.getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
        }

        // === MILESTONE TABLE HEADERS ===
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Study Week", "Task Description", "Status", "Remarks"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };
        
        jTableMilestones.setModel(model);
        JTableHeader header = jTableMilestones.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 32)); 
        header.setFont(new Font("Serif", Font.BOLD, 16)); 
        jTableMilestones.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableColumnModel col = jTableMilestones.getColumnModel();
        col.getColumn(0).setMinWidth(120);  // Week
        col.getColumn(0).setMaxWidth(140);
        col.getColumn(1).setPreferredWidth(390); // Task
        col.getColumn(1).setMinWidth(320);
        col.getColumn(2).setMinWidth(120); // Status
        col.getColumn(2).setMaxWidth(135);
        col.getColumn(3).setPreferredWidth(150); // Remarks
        
        jTableMilestones.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshMilestoneButtons();
            }
        });

        jTableMilestones.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredMilestoneRow = -1;
                jTableMilestones.repaint();
            }
        });

        DefaultTableCellRenderer milestoneRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {

                JLabel c = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);

                c.setOpaque(true);

                // --- Read status of this row ---
                String status = table.getValueAt(row, 2).toString().trim();
                boolean isCompleted = status.equalsIgnoreCase("Completed");
                boolean isHover = (row == hoveredMilestoneRow);

                // selected → completed → hover → default
                if (isSelected) {
                    c.setBackground(new Color(150,170,200));   // selected blue
                    c.setForeground(Color.WHITE);
                    return c;
                }

                if (isCompleted) {
                    c.setBackground(new Color(210,240,210));   // pale green
                    c.setForeground(Color.BLACK);
                    return c;
                }

                if (isHover) {
                    c.setBackground(new Color(235,235,235));   // pale grey hover
                    c.setForeground(Color.BLACK);
                    return c;
                }

                // Default: ALWAYS white
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);

                return c;
            }
        };
        // Apply to every column
        for (int i = 0; i < jTableMilestones.getColumnCount(); i++) {
            jTableMilestones.getColumnModel().getColumn(i)
                    .setCellRenderer(milestoneRenderer);
        }
        
        // ==== Course selector behaviour ====
        initCourseSelectorListener();

        // ==== Hover for milestone rows ====
        jTableMilestones.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoveredMilestoneRow = jTableMilestones.rowAtPoint(e.getPoint());
                jTableMilestones.repaint();
            }
        });
        
        jTableMilestones.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredMilestoneRow = -1;
                jTableMilestones.repaint();
            }
        });
        refreshPlanButtons();
        refreshMilestoneButtons();
        updateMilestoneProgressBar();
        // make the progress bar show text
        progressBarMilestones.setStringPainted(true);
        progressBarMilestones.setValue(0);
        progressBarMilestones.setString("0%");

    }
    
    private void initCourseSelectorListener() {
        comboxCourseSelector.addActionListener(e -> {

            if (comboxCourseSelector.getSelectedItem() == null) return;
            if (currentPlan == null) return;   // IMPORTANT: avoid NPE

            String label = comboxCourseSelector.getSelectedItem().toString();

            // Extract course ID before " (A-"
            String cid = label.split(" ")[0].trim();

            Student s = currentPlan.getStudent();
            if (s == null) return;

            Course targetCourse = null;
            for (Course c : s.getCourses()) {
                if (c.getCourseID().equals(cid)) {
                    targetCourse = c;
                    break;
                }
            }
            if (targetCourse == null) return;

            String key = buildPlanKey(
                    s.getStudentID(),
                    cid,
                    targetCourse.getAttemptNumber()
            );

            RecoveryPlan selectedPlan = planByKey.get(key);
            if (selectedPlan == null) {
                JOptionPane.showMessageDialog(this,
                    "No recovery plan created for " + cid);
                return;
            }

            currentPlan = selectedPlan;

            // Sync UI with selected course plan
            populatePlanUI(selectedPlan);
            populateMilestoneTable(selectedPlan);
            refreshMilestoneButtons();
        });
    }
    
    private void styleFlatButton(JButton btn) {
        Color normal = new Color(235, 235, 235);  // base
        Color hover = new Color(215, 215, 215);  // on hover
        Color click = new Color(195, 195, 195);  // on click
        Color disabled = new Color(230, 230, 230);  // when setEnabled(false)

        btn.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normal);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                btn.setBackground(click);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // return to hover only if mouse still over button
                if (btn.contains(e.getPoint())) {
                    btn.setBackground(hover);
                } else {
                    btn.setBackground(normal);
                }
            }
        });
    }

    private void refreshUIControllers() {
        refreshPlanButtons();
        refreshMilestoneButtons();
        refreshCourseSelector();
    }
        
    private void refreshPlanButtons() {
        boolean rowSelected = jTableFailedComponents.getSelectedRow() != -1;
        boolean hasPlan = (currentPlan != null);

        // Always allowed if a failed course is selected
        btnCreatePlan.setEnabled(rowSelected && !hasPlan);

        // Edit only if an existing plan exists
        btnEditPlan.setEnabled(hasPlan && !txtRecommendation.isEditable());

        // Save only when in edit mode
        btnSavePlan.setEnabled(txtRecommendation.isEditable());

        // Create All Plans only when there is at least 1 failed course shown
        boolean studentLoaded = !lblInfoStudentID.getText().trim().isEmpty();
        boolean hasFailedCourses = jTableFailedComponents.getRowCount() > 0;
        btnCreateAllPlans.setEnabled(studentLoaded && hasFailedCourses);    }

    private void refreshMilestoneButtons() {
        boolean onMilestoneTab = (tabTwoWay.getSelectedIndex() == 1);
        boolean hasPlan = (currentPlan != null);

        if (!onMilestoneTab || !hasPlan) {
            btnAdd.setEnabled(false);
            btnEdit.setEnabled(false);
            btnRemove.setEnabled(false);
            btnMarkAsCompleted.setEnabled(false);
            btnUpdateGrade.setEnabled(false);
            return;
        }

        String status = currentPlan.getStatus();
        boolean awaiting = status.equals("AWAITING_GRADE");
        boolean completed = status.startsWith("COMPLETED");

        int row = jTableMilestones.getSelectedRow();
        boolean rowSelected = (row != -1);

        // milestone cannot be changed if awaiting/complete
        boolean canEditStructure = !(awaiting || completed);

        btnAdd.setEnabled(canEditStructure);
        btnEdit.setEnabled(canEditStructure && rowSelected);
        btnRemove.setEnabled(canEditStructure && rowSelected);

        // Mark as completed allowed ONLY while in progress
        btnMarkAsCompleted.setEnabled(!awaiting && !completed && rowSelected);

        // update grade only when ALL milestones completed
        btnUpdateGrade.setEnabled(awaiting);
    }

    
    private void refreshButtonsByStatus() {
        if (currentPlan == null) return;

        String st = currentPlan.getStatus();

        boolean awaiting = st.equals("AWAITING_GRADE");
        boolean completed = st.startsWith("COMPLETED");

        // Milestones cannot be changed anymore once awaiting/complete
        boolean milestoneEditable = !(awaiting || completed);

        btnAdd.setEnabled(milestoneEditable);
        btnEdit.setEnabled(milestoneEditable);
        btnRemove.setEnabled(milestoneEditable);
        btnMarkAsCompleted.setEnabled(milestoneEditable);

        // Grade button logic
        if (awaiting) {
            btnUpdateGrade.setEnabled(true);
        } else {
            btnUpdateGrade.setEnabled(false);
        }
    }
    
    private void refreshUpdateGradeButton() {
        if (currentPlan == null) {
            btnUpdateGrade.setEnabled(false);
            return;
        }

        boolean allCompleted = currentPlan.areAllMilestonesCompleted();
        String status = currentPlan.getStatus();

        btnUpdateGrade.setEnabled(
                allCompleted &&
                "AWAITING_GRADE".equalsIgnoreCase(status)
        );
    }

    private void refreshCourseSelector() {
        comboxCourseSelector.removeAllItems();

        if (currentPlan == null) return;

        Student s = currentPlan.getStudent();

        for (Course c : s.getCourses()) {
            if (!"None".equals(c.getFailedComponent())) {
                String label = c.getCourseID() + " (A-" + c.getAttemptNumber() + ")";
                comboxCourseSelector.addItem(label);
            }
        }

        // Set to currently selected course
        String currentLabel =
            currentPlan.getCourse().getCourseID() +
            " (A-" + currentPlan.getCourse().getAttemptNumber() + ")";

        comboxCourseSelector.setSelectedItem(currentLabel);
    }
    
    private Course getSelectedCourseFromCombo(Student s) {
        String selected = (String) comboxCourseSelector.getSelectedItem();
        if (selected == null) return null;

        String cid = selected.split(" ")[0]; // extract ME207

        for (Course c : s.getCourses()) {
            if (c.getCourseID().equals(cid)) return c;
        }
        return null;
    }

    private void populateCourseSelector(Student s) {
        comboxCourseSelector.removeAllItems();

        for (Course c : s.getCourses()) {
            if (!"None".equals(c.getFailedComponent())) {
                comboxCourseSelector.addItem(
                    c.getCourseID() + " (Attempt " + c.getAttemptNumber() + ")"
                );
            }
        }
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
            
            newPlan.generateDefaultMilestones();
            
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
    
    private void handleFailedRowSelection() {
        int row = jTableFailedComponents.getSelectedRow();
        
        if (row == -1) {
            currentPlan = null;
            clearDetails();
            populateMilestoneTable(null); 
            refreshMilestoneButtons();
            return;
        }

        DefaultTableModel model =
                (DefaultTableModel) jTableFailedComponents.getModel();

        String sid = model.getValueAt(row, 0).toString();
        String cid = model.getValueAt(row, 1).toString();

        Student student = fileLoader.getStudentByID(sid);
        if (student == null) {
            currentPlan = null;
            clearDetails();
            return;
        }

        // find the course for this student
        Course failedCourse = null;
        for (Course c : student.getCourses()) {
            if (c.getCourseID().equals(cid)) {
                failedCourse = c;
                break;
            }
        }
        if (failedCourse == null) {
            currentPlan = null;
            clearDetails();
            return;
        }
        comboxCourseSelector.setVisible(true);
        lblSelectCourse.setVisible(true);

        // Update Student & Course panels
        updateStudentInfo(student);
        updateCourseInfo(failedCourse);

        // Look for an existing plan for this sid+cid+attempt
        String key = buildPlanKey(
                sid,
                cid,
                failedCourse.getAttemptNumber()
        );
        RecoveryPlan plan = planByKey.get(key);
        currentPlan = plan; 

        populatePlanUI(plan);
        populateMilestoneTable(plan);
        
        refreshPlanButtons();
        refreshCourseSelectorForStudent(student);
        refreshMilestoneButtons();
        updateMilestoneProgressBar();
        refreshUpdateGradeButton();

        tabTwoWay.setSelectedIndex(0);
    }

    private void loadFailedComponents(String studentID) {
        DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();
        model.setRowCount(0);

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
            String failed = c.getFailedComponent();

            if ("None".equals(failed)) {
                continue;
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
        refreshPlanButtons();
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
            String header = br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",", 10);
                if (p.length < 10) continue;

                String planID        = p[0].trim();
                String sid           = p[1].trim();
                String cid           = p[2].trim();
                int attempt          = Integer.parseInt(p[3].trim());
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

    // Milestone logic
    private void loadMilestonesFromFile() {
        File f = new File(MILESTONE_FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",", 5);
                if (p.length < 5) continue;

                String planID = p[0];
                String week = p[1];
                String task = p[2];
                boolean completed = Boolean.parseBoolean(p[3]);
                String notes = p[4];

                RecoveryPlan plan = planByID.get(planID);
                if (plan == null) continue;

                RecoveryMilestone m = new RecoveryMilestone(week, task);
                m.setCompleted(completed);
                m.setNotes(notes);

                plan.getMilestones().add(m);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading milestones: " + e.getMessage());
        }
    }

    private void updateMilestoneProgressBar() {
        if (currentPlan == null) {
            progressBarMilestones.setValue(0);
            progressBarMilestones.setString("0%");
            progressBarMilestones.setForeground(new Color(0, 122, 204));
            progressBarMilestones.setStringPainted(true);
            progressBarMilestones.setFont(new Font("Serif", Font.BOLD, 14));
            progressBarMilestones.setForeground(new Color(200, 30, 30));
            progressBarMilestones.setUI(new javax.swing.plaf.basic.BasicProgressBarUI());
            return;
        }

        int progress = (int) Math.round(currentPlan.getProgressPercentage());
        progressBarMilestones.setValue(progress);
        progressBarMilestones.setString(progress + "%");
        progressBarMilestones.setStringPainted(true);

        // Border
        progressBarMilestones.setBorderPainted(true);
        progressBarMilestones.setBorder(
                javax.swing.BorderFactory.createLineBorder(Color.DARK_GRAY, 1)
        );

        // Font for visibility
        progressBarMilestones.setFont(new Font("Serif", Font.BOLD, 14));

        // --- Bar colour ---
        if (progress == 100) {
            progressBarMilestones.setForeground(new Color(0, 170, 0));       // green
        } else if (progress >= 50) {
            progressBarMilestones.setForeground(new Color(255, 153, 0));     // orange
        } else {
            progressBarMilestones.setForeground(new Color(200, 30, 30));     // red
        }

        // 🔹 IMPORTANT: text colour stays BLACK
        progressBarMilestones.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected Color getSelectionForeground() {
                return Color.BLACK;   // text colour
            }

            @Override
            protected Color getSelectionBackground() {
                return Color.BLACK;
            }
        });

        refreshUpdateGradeButton();
    }



    private void saveMilestonesToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(MILESTONE_FILE))) {

            pw.println("planID,studyWeek,task,isCompleted,notes");

            // Ensure saving in ascending planID order
            List<String> orderedKeys = new ArrayList<>(planByID.keySet());
            Collections.sort(orderedKeys);

            for (String pid : orderedKeys) {
                RecoveryPlan plan = planByID.get(pid);

                for (RecoveryMilestone m : plan.getMilestones()) {
                    pw.printf("%s,%s,%s,%s,%s%n",
                            pid,
                            m.getStudyWeek(),
                            m.getTask().replace(",", " "),
                            m.isCompleted(),
                            m.getNotes().replace(",", " ")
                    );
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error saving milestones: " + e.getMessage());
        }
    }
    
    private void loadCourseSelectorForStudent(Student s) {
        comboxCourseSelector.removeAllItems();

        List<Course> failed = new ArrayList<>();

        for (Course c : s.getCourses()) {
            if (!"None".equals(c.getFailedComponent())) {
                comboxCourseSelector.addItem(c.getCourseID());
                failed.add(c);
            }
        }

        if (failed.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "This student has no failed courses.");
            comboxCourseSelector.setVisible(false);
            lblSelectCourse.setVisible(false);
            return;
        }

        // Set current course in selector
        comboxCourseSelector.setSelectedItem(currentPlan.getCourse().getCourseID());
    }

    private void refreshCourseSelectorForStudent(Student s) {
        comboxCourseSelector.removeAllItems();

            if (s == null) {
                comboxCourseSelector.setVisible(false);
                lblSelectCourse.setVisible(false);
                return;
            }

            boolean hasFailed = false;

            for (Course c : s.getCourses()) {
                if (!"None".equalsIgnoreCase(c.getFailedComponent())) {
                    String label = c.getCourseID() + " (A-" + c.getAttemptNumber() + ")";
                    comboxCourseSelector.addItem(label);
                    hasFailed = true;
                }
            }
            
            comboxCourseSelector.setVisible(hasFailed);
            lblSelectCourse.setVisible(hasFailed);
            // If student has only 1 failed course → auto-select it
            // Auto-select the only failed course if just one
            if (hasFailed && comboxCourseSelector.getItemCount() == 1) {
                comboxCourseSelector.setSelectedIndex(0);
            }
    }
    
    private String buildSinglePlanEmail(Student stu, RecoveryPlan plan) {
        StringBuilder sb = new StringBuilder();

        sb.append("Dear ").append(stu.getFullName()).append(",\n\n");
        sb.append("This is to inform you that a Recovery Plan has been issued for the following course:\n\n");

        sb.append("Course Code: ").append(plan.getCourse().getCourseID()).append("\n");
        sb.append("Course Name: ").append(plan.getCourse().getCourseName()).append("\n");
        sb.append("Attempt: A-").append(plan.getCourse().getAttemptNumber()).append("\n");
        sb.append("Failed Component: ").append(plan.getCourse().getFailedComponent()).append("\n\n");

        sb.append("Summary of Recommendation:\n");
        sb.append(plan.getRecommendation()).append("\n\n");

        sb.append("Required Milestones:\n");
        for (RecoveryMilestone m : plan.getMilestones()) {
            sb.append(" • ").append(m.getStudyWeek())
              .append(" – ").append(m.getTask()).append("\n");
        }

        sb.append("\nPlease follow the above milestones and complete all required tasks within the allocated timeframe.");
        sb.append("\nIf you require academic support, kindly reach out to your lecturer or programme office.\n\n");

        sb.append("Regards,\n");
        sb.append("APU Academic Support Team\n");
        sb.append("Asia Pacific University of Technology & Innovation (APU)");

        return sb.toString();
    }

    private String buildMultiPlanEmail(Student stu, List<RecoveryPlan> plans) {
        StringBuilder sb = new StringBuilder();

        sb.append("Dear ").append(stu.getFullName()).append(",\n\n");
        sb.append("This is to notify you that Recovery Plans have been issued for the following courses:\n\n");

        for (RecoveryPlan p : plans) {

            sb.append("---------------------------------------------------\n");
            sb.append("Course Code : ").append(p.getCourse().getCourseID()).append("\n");
            sb.append("Course Name : ").append(p.getCourse().getCourseName()).append("\n");
            sb.append("Attempt     : A-").append(p.getCourse().getAttemptNumber()).append("\n");
            sb.append("Failed Component : ").append(p.getCourse().getFailedComponent()).append("\n\n");

            sb.append("Recommendation:\n");
            sb.append(p.getRecommendation()).append("\n\n");

            sb.append("Milestones:\n");
            for (RecoveryMilestone m : p.getMilestones()) {
                sb.append(" • ").append(m.getStudyWeek())
                  .append(" – ").append(m.getTask()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("---------------------------------------------------\n");
        sb.append("Please ensure that you follow all milestones carefully.\n");
        sb.append("If you require further assistance, kindly contact your lecturer or programme office.\n\n");

        sb.append("Regards,\n");
        sb.append("APU Academic Support Team\n");
        sb.append("Asia Pacific University of Technology & Innovation (APU)");

        return sb.toString();
    }

    private boolean showEmailPreview(String subject, String body, String recipient) {
        JTextArea previewArea = new JTextArea(20, 50);
        previewArea.setText(
            "To: " + recipient + "\n" +
            "Subject: " + subject + "\n\n" +
            body
        );
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(previewArea);

        int option = JOptionPane.showConfirmDialog(
                this,
                scrollPane,
                "Email Preview",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        return option == JOptionPane.OK_OPTION;
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
        milestoneTab = new javax.swing.JPanel();
        lblMilestonesTable = new javax.swing.JLabel();
        jMilestoneScrollPane = new javax.swing.JScrollPane();
        jTableMilestones = new javax.swing.JTable();
        progressBarMilestones = new javax.swing.JProgressBar();
        lblProgress = new javax.swing.JLabel();
        btnAdd = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        btnMarkAsCompleted = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        lblCRP = new javax.swing.JLabel();
        comboxCourseSelector = new javax.swing.JComboBox<>();
        lblSelectCourse = new javax.swing.JLabel();
        btnSaveChanges = new javax.swing.JButton();
        btnUpdateGrade = new javax.swing.JButton();
        dashboard = new javax.swing.JPanel();
        jButtonUserManagement = new javax.swing.JButton();
        jButtonEligibility = new javax.swing.JButton();
        jButtonRecovery = new javax.swing.JButton();
        jButtonAPR = new javax.swing.JButton();
        logout = new javax.swing.JButton();
        btnHome = new javax.swing.JButton();

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
        setMinimumSize(new java.awt.Dimension(1160, 700));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(183, 201, 197));
        jPanel1.setPreferredSize(new java.awt.Dimension(1160, 700));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tabTwoWay.setPreferredSize(new java.awt.Dimension(870, 945));

        panelOverview.setBackground(new java.awt.Color(183, 201, 197));
        panelOverview.setPreferredSize(new java.awt.Dimension(950, 600));

        txtStudentID.setFont(new java.awt.Font("Serif", 0, 15)); // NOI18N
        txtStudentID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtStudentIDActionPerformed(evt);
            }
        });

        btnSearch.setFont(new java.awt.Font("Helvetica Neue", 0, 14)); // NOI18N
        btnSearch.setText("Search");
        btnSearch.setBorder(null);
        btnSearch.setOpaque(true);
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
        btnCreatePlan.setOpaque(true);
        btnCreatePlan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreatePlanActionPerformed(evt);
            }
        });

        jTableFailedComponents.setFont(new java.awt.Font("Helvetica Neue", 0, 15)); // NOI18N
        jTableFailedComponents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Student ID", "Course ID", "Failed Component"
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
                            .addComponent(jLabel5))))
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
        btnMilestoneTab.setOpaque(true);
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
                .addContainerGap()
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addComponent(lblLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(lblInfoLecturer, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(lblInfoAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnMilestoneTab, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelFBLayout.createSequentialGroup()
                                .addComponent(lblCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblInfoCourseName, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lblAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                                        .addComponent(lblInfoSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panelFBLayout.createSequentialGroup()
                                        .addComponent(lblCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(lblInfoCourseID, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblStudentID2)
                                    .addComponent(lblExamScore)
                                    .addComponent(lblAssScore))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(lblInfoExamScore, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                                    .addComponent(lblInfoCGPA, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblInfoAssScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addGap(0, 0, Short.MAX_VALUE))))
            .addGroup(panelFBLayout.createSequentialGroup()
                .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 454, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelFBLayout.createSequentialGroup()
                        .addGap(161, 161, 161)
                        .addComponent(lblTitleDetails)))
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
                    .addComponent(lblInfoExamScore, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelFBLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblInfoStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelOverviewLayout.createSequentialGroup()
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(txtStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCreateAllPlans, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnCreatePlan, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelOverviewLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 865, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(panelOverviewLayout.createSequentialGroup()
                                .addComponent(panelFB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(RPpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(65, 65, 65))
            .addGroup(panelOverviewLayout.createSequentialGroup()
                .addGap(280, 280, 280)
                .addComponent(lblCRP1, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblCRP1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(38, 38, 38)
                        .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCreatePlan, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnCreateAllPlans, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(panelOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(RPpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelFB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(53, Short.MAX_VALUE))
        );

        tabTwoWay.addTab("Overview", panelOverview);

        milestoneTab.setBackground(new java.awt.Color(183, 201, 197));
        milestoneTab.setPreferredSize(new java.awt.Dimension(945, 600));
        milestoneTab.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblMilestonesTable.setBackground(new java.awt.Color(255, 255, 255));
        lblMilestonesTable.setFont(new java.awt.Font("Serif", 1, 28)); // NOI18N
        lblMilestonesTable.setText("Milestone Table");
        milestoneTab.add(lblMilestonesTable, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 130, 210, -1));

        jTableMilestones.setFont(new java.awt.Font("Serif", 0, 16)); // NOI18N
        jTableMilestones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Student Week", "Task", "Status", "Remarks"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, false, true, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableMilestones.setRowHeight(25);
        jTableMilestones.setShowGrid(false);
        jMilestoneScrollPane.setViewportView(jTableMilestones);
        if (jTableMilestones.getColumnModel().getColumnCount() > 0) {
            jTableMilestones.getColumnModel().getColumn(0).setResizable(false);
            jTableMilestones.getColumnModel().getColumn(1).setResizable(false);
            jTableMilestones.getColumnModel().getColumn(2).setResizable(false);
            jTableMilestones.getColumnModel().getColumn(3).setResizable(false);
        }

        milestoneTab.add(jMilestoneScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 200, 770, 170));
        milestoneTab.add(progressBarMilestones, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 400, 470, 20));

        lblProgress.setFont(new java.awt.Font("Serif", 1, 22)); // NOI18N
        lblProgress.setText("Progress:");
        lblProgress.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        milestoneTab.add(lblProgress, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 390, -1, 30));

        btnAdd.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnAdd.setText("Add ");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });
        milestoneTab.add(btnAdd, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 460, 110, 30));

        btnEdit.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnEdit.setText("Edit");
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });
        milestoneTab.add(btnEdit, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 460, 110, 30));

        btnRemove.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnRemove.setText("Delete");
        btnRemove.setPreferredSize(new java.awt.Dimension(72, 29));
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });
        milestoneTab.add(btnRemove, new org.netbeans.lib.awtextra.AbsoluteConstraints(750, 460, 110, 30));

        btnMarkAsCompleted.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnMarkAsCompleted.setText("Mark as Completed");
        btnMarkAsCompleted.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMarkAsCompletedActionPerformed(evt);
            }
        });
        milestoneTab.add(btnMarkAsCompleted, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 460, 210, 30));

        btnBack.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        milestoneTab.add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(750, 510, 110, 30));

        lblCRP.setBackground(new java.awt.Color(0, 0, 0));
        lblCRP.setFont(new java.awt.Font("Serif", 1, 36)); // NOI18N
        lblCRP.setText("Course Recovery Plan");
        milestoneTab.add(lblCRP, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 40, 360, 71));

        comboxCourseSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboxCourseSelectorActionPerformed(evt);
            }
        });
        milestoneTab.add(comboxCourseSelector, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 160, 130, 30));

        lblSelectCourse.setFont(new java.awt.Font("Serif", 0, 15)); // NOI18N
        lblSelectCourse.setText("Select Course:");
        milestoneTab.add(lblSelectCourse, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 170, -1, -1));

        btnSaveChanges.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnSaveChanges.setText("Save");
        btnSaveChanges.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveChangesActionPerformed(evt);
            }
        });
        milestoneTab.add(btnSaveChanges, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 460, 110, 30));

        btnUpdateGrade.setFont(new java.awt.Font("Helvetica Neue", 0, 18)); // NOI18N
        btnUpdateGrade.setText("Update Grade");
        btnUpdateGrade.setPreferredSize(new java.awt.Dimension(72, 29));
        btnUpdateGrade.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateGradeActionPerformed(evt);
            }
        });
        milestoneTab.add(btnUpdateGrade, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 395, 150, 30));

        tabTwoWay.addTab("Milestone", milestoneTab);

        jPanel1.add(tabTwoWay, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, -30, 960, 730));

        dashboard.setBackground(new java.awt.Color(95, 106, 105));
        dashboard.setPreferredSize(new java.awt.Dimension(210, 700));

        jButtonUserManagement.setBackground(new java.awt.Color(95, 106, 105));
        jButtonUserManagement.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jButtonUserManagement.setForeground(new java.awt.Color(255, 255, 255));
        jButtonUserManagement.setText("User Management");
        jButtonUserManagement.setToolTipText("");
        jButtonUserManagement.setBorderPainted(false);
        jButtonUserManagement.setContentAreaFilled(false);
        jButtonUserManagement.setFocusPainted(false);
        jButtonUserManagement.setOpaque(true);
        jButtonUserManagement.setPreferredSize(new java.awt.Dimension(210, 70));
        jButtonUserManagement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUserManagementActionPerformed(evt);
            }
        });

        jButtonEligibility.setBackground(new java.awt.Color(95, 106, 105));
        jButtonEligibility.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jButtonEligibility.setForeground(new java.awt.Color(255, 255, 255));
        jButtonEligibility.setText("<html><center>Eligibility Check<br>and Enrolment</center></html> ");
        jButtonEligibility.setToolTipText("");
        jButtonEligibility.setBorderPainted(false);
        jButtonEligibility.setContentAreaFilled(false);
        jButtonEligibility.setFocusPainted(false);
        jButtonEligibility.setOpaque(true);
        jButtonEligibility.setPreferredSize(new java.awt.Dimension(210, 70));
        jButtonEligibility.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEligibilityActionPerformed(evt);
            }
        });

        jButtonRecovery.setBackground(new java.awt.Color(95, 106, 105));
        jButtonRecovery.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jButtonRecovery.setForeground(new java.awt.Color(255, 255, 255));
        jButtonRecovery.setText("Course Recovery Plan");
        jButtonRecovery.setToolTipText("");
        jButtonRecovery.setBorderPainted(false);
        jButtonRecovery.setContentAreaFilled(false);
        jButtonRecovery.setFocusPainted(false);
        jButtonRecovery.setOpaque(true);
        jButtonRecovery.setPreferredSize(new java.awt.Dimension(210, 70));
        jButtonRecovery.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecoveryActionPerformed(evt);
            }
        });

        jButtonAPR.setBackground(new java.awt.Color(95, 106, 105));
        jButtonAPR.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        jButtonAPR.setForeground(new java.awt.Color(255, 255, 255));
        jButtonAPR.setText("<html><center>Academic<br>Performance Report</center></html> ");
        jButtonAPR.setBorderPainted(false);
        jButtonAPR.setContentAreaFilled(false);
        jButtonAPR.setFocusPainted(false);
        jButtonAPR.setOpaque(true);
        jButtonAPR.setPreferredSize(new java.awt.Dimension(210, 70));
        jButtonAPR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAPRActionPerformed(evt);
            }
        });

        logout.setFont(new java.awt.Font("Serif", 0, 20)); // NOI18N
        logout.setText("Log Out");
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutActionPerformed(evt);
            }
        });

        btnHome.setBackground(new java.awt.Color(95, 106, 105));
        btnHome.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnHome.setText("HOME");
        btnHome.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnHome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHomeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dashboardLayout = new javax.swing.GroupLayout(dashboard);
        dashboard.setLayout(dashboardLayout);
        dashboardLayout.setHorizontalGroup(
            dashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardLayout.createSequentialGroup()
                .addGroup(dashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonRecovery, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonUserManagement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonAPR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonEligibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(dashboardLayout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addComponent(logout)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dashboardLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnHome, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        dashboardLayout.setVerticalGroup(
            dashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addComponent(btnHome, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                .addGap(53, 53, 53)
                .addComponent(jButtonUserManagement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonEligibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRecovery, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonAPR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 172, Short.MAX_VALUE)
                .addComponent(logout)
                .addGap(62, 62, 62))
        );

        jPanel1.add(dashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, 710));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 1166, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 710, Short.MAX_VALUE)
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
        refreshPlanButtons();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void txtStudentIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtStudentIDActionPerformed
    String studentID = txtStudentID.getText().trim();
    loadFailedComponents(studentID);    }//GEN-LAST:event_txtStudentIDActionPerformed


    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        if (currentPlan == null) return;

        int row = jTableMilestones.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a milestone.");
            return;
        }

        String newWeek = JOptionPane.showInputDialog(this, "New Study Week:", 
                currentPlan.getMilestones().get(row).getStudyWeek());
        if (newWeek == null) return;

        String newTask = JOptionPane.showInputDialog(this, "New Task:", 
                currentPlan.getMilestones().get(row).getTask());
        if (newTask == null) return;

        currentPlan.updateMilestone(row, newWeek, newTask);
        
        saveMilestonesToFile();
        populateMilestoneTable(currentPlan);
        refreshMilestoneButtons();
        updateMilestoneProgressBar();
        checkPlanCompletion();

        jTableMilestones.repaint();
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        if (currentPlan == null) {
            JOptionPane.showMessageDialog(this, "No plan selected.");
            return;
        }

        String week = JOptionPane.showInputDialog(this, "Enter Study Week:");
        if (week == null || week.trim().isEmpty()) return;

        String task = JOptionPane.showInputDialog(this, "Enter Task Description:");
        if (task == null || task.trim().isEmpty()) return;

        currentPlan.addMilestone(week, task);

        saveMilestonesToFile();
        populateMilestoneTable(currentPlan);
        
        updateMilestoneProgressBar();
        checkPlanCompletion();
        refreshMilestoneButtons();

        jTableMilestones.repaint();

    }//GEN-LAST:event_btnAddActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        tabTwoWay.setSelectedIndex(0);

        // Hide course selector in overview 
        comboxCourseSelector.setVisible(false);
        lblSelectCourse.setVisible(false);

        // Refresh the failed course table (remove passed courses)
        loadAllFailedStudents();

        // If the plan was completed, remove UI plan details
        if (currentPlan != null && currentPlan.getStatus().startsWith("COMPLETED")) {
            currentPlan = null;
            clearDetails();
        }

        // Reselect first row if exists
        if (jTableFailedComponents.getRowCount() > 0) {
            jTableFailedComponents.setRowSelectionInterval(0, 0);
            handleFailedRowSelection();   // your own method
        } else {
            clearDetails();
        }

        refreshButtonsByStatus();
        handleFailedRowSelection();
        refreshUpdateGradeButton();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnMilestoneTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMilestoneTabActionPerformed
        if (currentPlan == null) {
            JOptionPane.showMessageDialog(this, "Please select a recovery plan.");
            return;
        }

        // populate table
        populateMilestoneTable(currentPlan);

        // update progress bar
        updateMilestoneProgressBar();  
        tabTwoWay.setSelectedIndex(1);
        refreshMilestoneButtons();
    }//GEN-LAST:event_btnMilestoneTabActionPerformed

    private void btnCreatePlanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreatePlanActionPerformed
        int row = jTableFailedComponents.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a failed course.");
                return;
            }

            DefaultTableModel model = (DefaultTableModel) jTableFailedComponents.getModel();
            String sid = model.getValueAt(row, 0).toString();
            String cid = model.getValueAt(row, 1).toString();

            Student student = fileLoader.getStudentByID(sid);
            if (student == null) {
                JOptionPane.showMessageDialog(this, "Student not found.");
                return;
            }

            Course failedCourse = null;
            for (Course c : student.getCourses()) {
                if (c.getCourseID().equals(cid)) {
                    failedCourse = c;
                    break;
                }
            }
                
            if (failedCourse == null) {
                JOptionPane.showMessageDialog(this, "Course not found.");
                return;
            }   
            int attempt = failedCourse.getAttemptNumber();
            String key = buildPlanKey(sid, cid, attempt);

            RecoveryPlan plan = planByKey.get(key);

            boolean isNewPlan = false;

            if (plan == null) {
                String planID = generatePlanID();
                plan = new RecoveryPlan(planID, student, failedCourse);

                plan.generateDefaultMilestones();

                planByID.put(planID, plan);
                planByKey.put(key, plan);

                savePlansToFile();
                saveMilestonesToFile();
                isNewPlan = true;
            }
                       
            savePlansToFile();
            saveMilestonesToFile();
            
            if (isNewPlan) {
                String subject = "APU Notification: Recovery Plan Created for " + failedCourse.getCourseID();
                String body = buildSinglePlanEmail(student, plan);

                // PREVIEW BEFORE SENDING
                if (showEmailPreview(subject, body, student.getEmail())) {
                    boolean sent = new Email().sendEmail(student.getEmail(), subject, body);

                    if (!sent) {
                        JOptionPane.showMessageDialog(this,
                            "Recovery Plan was created, but the email failed to send.",
                            "Email Error", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Email sending cancelled.",
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            comboxCourseSelector.setVisible(true);
            lblSelectCourse.setVisible(true);

            currentPlan = plan;

            // UI updates
            populatePlanUI(plan);
            populateMilestoneTable(plan);
            refreshFailedCoursesTableHighlight();
            refreshMilestoneButtons();
            refreshCourseSelectorForStudent(student);

            // auto-select the newly created course
            String label = failedCourse.getCourseID() + " (A-" + attempt + ")";
            comboxCourseSelector.setSelectedItem(label);

            // NEW PLAN → allow editing immediately
            txtRecommendation.setEditable(isNewPlan);
            priorityCombo.setEnabled(isNewPlan);
            btnSavePlan.setEnabled(isNewPlan);
            btnEditPlan.setEnabled(!isNewPlan);   
            
            refreshPlanButtons();
            refreshMilestoneButtons();
            updateMilestoneProgressBar();
    }//GEN-LAST:event_btnCreatePlanActionPerformed

    private void btnSavePlanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSavePlanActionPerformed
        if (currentPlan == null) {
        JOptionPane.showMessageDialog(this, "No recovery plan selected.");
            return;
        }

        currentPlan.setRecommendation(txtRecommendation.getText());
        savePlansToFile();
        saveMilestonesToFile();

        JOptionPane.showMessageDialog(this, "Plan updated successfully.");

        txtRecommendation.setEditable(false);
        priorityCombo.setEnabled(false);

        refreshPlanButtons();
    }//GEN-LAST:event_btnSavePlanActionPerformed

    private void priorityComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_priorityComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_priorityComboActionPerformed

    private void btnEditPlanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditPlanActionPerformed
        if (currentPlan == null) {
        JOptionPane.showMessageDialog(this, "Select a plan first.");
            return;
        }

        txtRecommendation.setEditable(true);
        priorityCombo.setEnabled(true);

        refreshPlanButtons();
    }//GEN-LAST:event_btnEditPlanActionPerformed

    private void btnCreateAllPlansActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateAllPlansActionPerformed
        String studentID = lblInfoStudentID.getText().trim();

        if (studentID.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a student.");
            return;
        }
         
        Student student = fileLoader.getStudentByID(studentID);
        if (student == null) {
            JOptionPane.showMessageDialog(this, "Student not found.");
            return;
        }
        List<RecoveryPlan> newPlans = new ArrayList<>();

        for (Course c : student.getCourses()) {
            int attempt = c.getAttemptNumber();
            String key = buildPlanKey(student.getStudentID(), c.getCourseID(), attempt);

            RecoveryPlan rp = planByKey.get(key);
            if (rp != null && rp.getMilestones().size() > 0) {
                newPlans.add(rp);
            }
        }
        if (!newPlans.isEmpty()) {
            String subject = "APU Notification: Your Recovery Plans Have Been Issued";
            String body = buildMultiPlanEmail(student, newPlans);

            if (showEmailPreview(subject, body, student.getEmail())) {
                new Email().sendEmail(student.getEmail(), subject, body);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Email sending cancelled.",
                    "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        createAllPlansForStudent(studentID);

        savePlansToFile();
        saveMilestonesToFile();

        refreshFailedCoursesTableHighlight();
        refreshPlanButtons();
        handleFailedRowSelection();
        refreshMilestoneButtons();
        refreshCourseSelectorForStudent(student);

        JOptionPane.showMessageDialog(this,
                "All recovery plans for this student have been processed.");
    }//GEN-LAST:event_btnCreateAllPlansActionPerformed

    private void btnMarkAsCompletedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMarkAsCompletedActionPerformed
        if (currentPlan == null) return;

        int row = jTableMilestones.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a milestone.");
            return;
        }

        String notes = JOptionPane.showInputDialog(this, 
                "Completion Notes (optional):");

        currentPlan.markMilestoneCompleted(row, notes);
        saveMilestonesToFile();
        populateMilestoneTable(currentPlan);
        refreshMilestoneButtons();
        updateMilestoneProgressBar();
        refreshUpdateGradeButton();
        checkPlanCompletion();

        jTableMilestones.repaint();
    }//GEN-LAST:event_btnMarkAsCompletedActionPerformed

    private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
        if (currentPlan == null) return;

        int row = jTableMilestones.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a milestone to delete.");
            return;
        }

        currentPlan.removeMilestone(row);
        
        saveMilestonesToFile();
        populateMilestoneTable(currentPlan);
        refreshMilestoneButtons();
        updateMilestoneProgressBar();
        checkPlanCompletion();

        jTableMilestones.repaint();
    }//GEN-LAST:event_btnRemoveActionPerformed

    private void comboxCourseSelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboxCourseSelectorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboxCourseSelectorActionPerformed

    private void btnSaveChangesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveChangesActionPerformed
        saveMilestonesToFile();
        populateMilestoneTable(currentPlan);
        refreshMilestoneButtons();
        updateMilestoneProgressBar();
    }//GEN-LAST:event_btnSaveChangesActionPerformed

    private void btnUpdateGradeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateGradeActionPerformed
        if (currentPlan == null) return;

        Course c = currentPlan.getCourse();
        String failType = c.getFailedComponent();

        Integer newAssRaw = null;
        Integer newExamRaw = null;

        // ---- Assignment Only ----
        if (failType.equals("Assignment Only") || failType.equals("Both Components")) {
            String in = JOptionPane.showInputDialog(
                    this,
                    "Enter new ASSIGNMENT grade (0–100):",
                    "Update Assignment Grade",
                    JOptionPane.PLAIN_MESSAGE
            );
            if (in == null) return;
            newAssRaw = Integer.parseInt(in.trim());
        }

        // ---- Exam Only ----
        if (failType.equals("Exam Only") || failType.equals("Both Components")) {
            String in = JOptionPane.showInputDialog(
                    this,
                    "Enter new EXAM grade (0–100):",
                    "Update Exam Grade",
                    JOptionPane.PLAIN_MESSAGE
            );
            if (in == null) return;
            newExamRaw = Integer.parseInt(in.trim());
        }
        
        // ---- Pass values to CRP ----
        boolean ok = crp.enterRecoveryGrade(
            currentPlan,
            newAssRaw,
            newExamRaw
    );

        if (!ok) {
            JOptionPane.showMessageDialog(this, "Grade update failed.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String finalStatus = currentPlan.getStatus();
        JOptionPane.showMessageDialog(this,
                "Grades updated.\nFinal Status: " + finalStatus,
                "Message", JOptionPane.INFORMATION_MESSAGE);

        refreshPlanButtons();
        refreshFailedCoursesTableHighlight();
        populatePlanUI(currentPlan);
        populateMilestoneTable(currentPlan);
        refreshButtonsByStatus();
    }//GEN-LAST:event_btnUpdateGradeActionPerformed

    private void jButtonUserManagementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUserManagementActionPerformed
        new AdminDashboard(currentUser).setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButtonUserManagementActionPerformed

    private void jButtonEligibilityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEligibilityActionPerformed
        new ECE_UI(currentUser).setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButtonEligibilityActionPerformed

    private void jButtonRecoveryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecoveryActionPerformed
        if (currentUser.getRole().equalsIgnoreCase("ADMIN")) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "Access Denied: Administrators are not allowed to access the Course Recovery Plan.", 
                "Restricted Access", 
                javax.swing.JOptionPane.WARNING_MESSAGE);
        } else {
            new CRP_UI(currentUser).setVisible(true);
            this.dispose();
        }
    }//GEN-LAST:event_jButtonRecoveryActionPerformed

    private void jButtonAPRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAPRActionPerformed
        new APR_UI(currentUser).setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButtonAPRActionPerformed

    private void logoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutActionPerformed
        Logger.writeLog(currentUser.getUsername(), "LOGOUT");
        new LoginUI().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_logoutActionPerformed

    private void btnHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHomeActionPerformed
        new MainDashboard(currentUser).setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnHomeActionPerformed
    
    private void populatePlanUI(RecoveryPlan plan) {
        if (plan == null) {
            lblPlanID.setText("—");
            txtRecommendation.setText("");
            txtRecommendation.setEditable(false);
            priorityCombo.setEnabled(false);
            return;
        }

        lblPlanID.setText(plan.getPlanID());

        // Show formatted recommendation
        txtRecommendation.setText(plan.getRecommendation());
        txtRecommendation.setCaretPosition(0);

        txtRecommendation.setEditable(false);
        priorityCombo.setEnabled(false);
        refreshButtonsByStatus();
        refreshUpdateGradeButton();
    }
    
    private void checkPlanCompletion() {
        if (currentPlan == null) return;

        boolean allDone = currentPlan.getMilestones()
                                     .stream()
                                     .allMatch(m -> m.isCompleted());

        if (allDone) {
            // Move plan state into the correct phase
            if (!"COMPLETED-PASSED".equals(currentPlan.getStatus()) &&
                !"COMPLETED-FAILED".equals(currentPlan.getStatus())) {

                currentPlan.setStatus("AWAITING_GRADE");
            }
        }

        // Always keep UI synced
        updateMilestoneProgressBar();
        refreshMilestoneButtons();
    }

    private void populateMilestoneTable(RecoveryPlan plan) {

        DefaultTableModel model = (DefaultTableModel) jTableMilestones.getModel();
        model.setRowCount(0); 

        if (plan == null) return;

        for (RecoveryMilestone m : plan.getMilestones()) {
            model.addRow(new Object[]{
                m.getStudyWeek(),
                m.getTask(),
                m.isCompleted() ? "COMPLETED" : "PENDING",
                m.getNotes()
            });
        }

        updateMilestoneProgressBar();
        jTableMilestones.repaint();
    }

//    /**
//     * @param args the command line arguments
//     */
  public static void main(String args[]) {
//      java.awt.EventQueue.invokeLater(() -> {
//        new CRP_UI().setVisible(true);
//      });
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
//            logger.log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//        FileLoader loader = new FileLoader();
//        loader.loadAll();
//        /* Create and display the form */        
//        new CRP_UI().setVisible(true);

//        }
//    });
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel RPpanel;
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnCreateAllPlans;
    private javax.swing.JButton btnCreatePlan;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnEditPlan;
    private javax.swing.JButton btnHome;
    private javax.swing.JButton btnMarkAsCompleted;
    private javax.swing.JButton btnMilestoneTab;
    private javax.swing.JButton btnRemove;
    private javax.swing.JButton btnSaveChanges;
    private javax.swing.JButton btnSavePlan;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdateGrade;
    private javax.swing.JComboBox<String> comboxCourseSelector;
    private javax.swing.JPanel dashboard;
    private javax.swing.JButton jButtonAPR;
    private javax.swing.JButton jButtonEligibility;
    private javax.swing.JButton jButtonRecovery;
    private javax.swing.JButton jButtonUserManagement;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jMilestoneScrollPane;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTableFailedComponents;
    private javax.swing.JTable jTableMilestones;
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
    private javax.swing.JLabel lblMilestonesTable;
    private javax.swing.JLabel lblPlanID;
    private javax.swing.JLabel lblProgress;
    private javax.swing.JLabel lblSelectCourse;
    private javax.swing.JLabel lblSemester;
    private javax.swing.JLabel lblStudentID;
    private javax.swing.JLabel lblStudentID2;
    private javax.swing.JLabel lblStudentName;
    private javax.swing.JLabel lblTitleDetails;
    private javax.swing.JLabel lblplanid;
    private javax.swing.JButton logout;
    private javax.swing.JPanel milestoneTab;
    private javax.swing.JPanel panelFB;
    private javax.swing.JPanel panelFailureBadge;
    private javax.swing.JPanel panelOverview;
    private javax.swing.JComboBox<String> priorityCombo;
    private javax.swing.JProgressBar progressBarMilestones;
    private javax.swing.JTabbedPane tabTwoWay;
    private javax.swing.JTextArea txtRecommendation;
    private javax.swing.JTextField txtStudentID;
    // End of variables declaration//GEN-END:variables
}
