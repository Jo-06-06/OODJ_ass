/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package oodj_ass;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;    
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.JOptionPane;

/**
 *
 * @author User
 */
//10/12 11.00PM
public class APR_UI extends javax.swing.JFrame {
    private User currentUser;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(APR_UI.class.getName());
    
    private String currentStudentId = null;
    private String currentStudentName = "";
    private String currentProgram = "";
    private String currentIntake = "All";
    private String currentStudentEmail = "";
    
    // Key Achievements / Weaknesses / Recommendations
    private Map<String, StringBuilder> achievementsMap = new HashMap<>();
    private Map<String, StringBuilder> weaknessesMap  = new HashMap<>();
    private Map<String, StringBuilder> improveMap     = new HashMap<>();

    private void addSidebarHover(javax.swing.JButton btn) {

        Color normal = new Color(95,106,105);        // sidebar color (invisible)
        Color hover  = new Color(130,140,140);       // reveal color
        Color click  = new Color(160,170,170);       // click color

        btn.setBackground(normal);

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
    
    /**
     * Creates new form ARP_UI
     */
    public APR_UI(User user) {
        initComponents();
        this.currentUser = user;

        // Set header font
        apr_tabel1.getTableHeader().setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 20));

        // Center header text
        javax.swing.table.DefaultTableCellRenderer headerRenderer = 
                (javax.swing.table.DefaultTableCellRenderer) apr_tabel1.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        
        //sidebarhover
        addSidebarHover(jButtonUserManagement);
        addSidebarHover(jButtonEligibility);
        addSidebarHover(jButtonRecovery);
        addSidebarHover(jButtonAPR);
        
        //Intake Drop Down Box
        jComboBox1.removeAllItems();
        jComboBox1.setEnabled(false);

        // Intake combobox change listener
        jComboBox1.addItemListener(new ItemListener() {
             @Override
             public void itemStateChanged(ItemEvent e) {
                 if (e.getStateChange() == ItemEvent.SELECTED) {
                     if (currentStudentId == null) return;
                     String selected = (String) jComboBox1.getSelectedItem();
                     if (selected == null) return;
                     currentIntake = selected;
                     try {
                         loadAndDisplayReport(currentStudentId, selected);
                     } catch (Exception ex) {
                         ex.printStackTrace();
                         JOptionPane.showMessageDialog(APR_UI.this, "Error updating report.");
                     }
                 }
             }
         });

        // Export PDF Button
        apr_pdfButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (currentStudentId == null) {
                    JOptionPane.showMessageDialog(APR_UI.this, "Please search a student first.");
                    return;
                }
                try {
                    String pdfFile = exportToPDF();
                    if (pdfFile != null) {
                        sendReportEmail(currentStudentEmail, pdfFile);
                    }
                    JOptionPane.showMessageDialog(APR_UI.this,
                            "PDF exported successfully.\nEmail has been sent to: " + currentStudentEmail);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(APR_UI.this, "PDF export failed or email not sent.");
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        dashboard = new javax.swing.JPanel();
        jButtonUserManagement = new javax.swing.JButton();
        jButtonEligibility = new javax.swing.JButton();
        jButtonRecovery = new javax.swing.JButton();
        jButtonAPR = new javax.swing.JButton();
        logout = new javax.swing.JButton();
        btnHome = new javax.swing.JButton();
        btnHome1 = new javax.swing.JButton();
        apr_titile = new javax.swing.JLabel();
        sid_search = new javax.swing.JTextField();
        apr_intake = new javax.swing.JLabel();
        apr_search = new javax.swing.JButton();
        apr_sid1 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        apr_tabel1 = new javax.swing.JTable();
        apr_pdfButton = new javax.swing.JButton();
        arp_jScrollPane = new javax.swing.JScrollPane();
        jTextArea_summary = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Academic Performance Report");
        setMaximumSize(new java.awt.Dimension(1160, 700));
        setMinimumSize(new java.awt.Dimension(1160, 700));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(183, 201, 197));
        jPanel1.setFocusable(false);
        jPanel1.setMaximumSize(new java.awt.Dimension(1160, 700));
        jPanel1.setMinimumSize(new java.awt.Dimension(1160, 700));
        jPanel1.setPreferredSize(new java.awt.Dimension(1160, 700));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

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
        btnHome.setBorder(null);
        btnHome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHomeActionPerformed(evt);
            }
        });

        btnHome1.setBackground(new java.awt.Color(95, 106, 105));
        btnHome1.setFont(new java.awt.Font("Serif", 1, 18)); // NOI18N
        btnHome1.setText("HOME");
        btnHome1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnHome1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHome1ActionPerformed(evt);
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
                .addGroup(dashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dashboardLayout.createSequentialGroup()
                        .addGap(53, 53, 53)
                        .addComponent(logout))
                    .addGroup(dashboardLayout.createSequentialGroup()
                        .addGap(54, 54, 54)
                        .addComponent(btnHome1, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addComponent(btnHome, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        dashboardLayout.setVerticalGroup(
            dashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(dashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnHome, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)
                    .addComponent(btnHome1, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE))
                .addGap(53, 53, 53)
                .addComponent(jButtonUserManagement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonEligibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRecovery, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonAPR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(167, 167, 167)
                .addComponent(logout)
                .addGap(62, 62, 62))
        );

        jPanel1.add(dashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        apr_titile.setFont(new java.awt.Font("Serif", 1, 36)); // NOI18N
        apr_titile.setText("Academic Performance Report");
        apr_titile.setToolTipText("");
        jPanel1.add(apr_titile, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 40, -1, -1));

        sid_search.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        sid_search.setMaximumSize(new java.awt.Dimension(100, 30));
        sid_search.setMinimumSize(new java.awt.Dimension(100, 30));
        sid_search.setPreferredSize(new java.awt.Dimension(100, 30));
        sid_search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sid_searchActionPerformed(evt);
            }
        });
        jPanel1.add(sid_search, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 120, -1, -1));

        apr_intake.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        apr_intake.setText("Intake:");
        jPanel1.add(apr_intake, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 120, -1, -1));

        apr_search.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        apr_search.setBorderPainted(false);
        apr_search.setFocusPainted(false);
        apr_search.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        apr_search.setLabel("Search");
        apr_search.setPreferredSize(new java.awt.Dimension(70, 30));
        apr_search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apr_searchActionPerformed(evt);
            }
        });
        jPanel1.add(apr_search, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 120, -1, -1));

        apr_sid1.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        apr_sid1.setText("Student ID:");
        apr_sid1.setMaximumSize(new java.awt.Dimension(86, 30));
        apr_sid1.setMinimumSize(new java.awt.Dimension(86, 30));
        apr_sid1.setPreferredSize(new java.awt.Dimension(86, 30));
        jPanel1.add(apr_sid1, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 120, -1, -1));

        jComboBox1.setFont(new java.awt.Font("Segoe UI Variable", 0, 18)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.setMaximumSize(new java.awt.Dimension(100, 30));
        jComboBox1.setMinimumSize(new java.awt.Dimension(100, 30));
        jComboBox1.setPreferredSize(new java.awt.Dimension(100, 30));
        jPanel1.add(jComboBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 120, -1, -1));

        apr_tabel1.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        apr_tabel1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Semester", "Course ID", "Course Name", "Credit Hours", "Grade", "GPA"
            }
        ));
        apr_tabel1.setRowHeight(35);
        apr_tabel1.setShowGrid(false);
        jScrollPane1.setViewportView(apr_tabel1);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 170, 870, 230));

        apr_pdfButton.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        apr_pdfButton.setText("Export PDF");
        apr_pdfButton.setToolTipText("");
        apr_pdfButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apr_pdfButtonActionPerformed(evt);
            }
        });
        jPanel1.add(apr_pdfButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(1020, 650, -1, -1));

        arp_jScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Academic Summary & Recommedations", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Serif", 0, 14))); // NOI18N

        jTextArea_summary.setEditable(false);
        jTextArea_summary.setBackground(new java.awt.Color(255, 255, 255));
        jTextArea_summary.setColumns(20);
        jTextArea_summary.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        jTextArea_summary.setLineWrap(true);
        jTextArea_summary.setRows(5);
        arp_jScrollPane.setViewportView(jTextArea_summary);

        jPanel1.add(arp_jScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 410, 870, 210));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void apr_searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apr_searchActionPerformed
        String studentID = sid_search.getText().trim();

        if (studentID.equals("")) {
            JOptionPane.showMessageDialog(this, "Please enter your Student ID.");
            return;
        }

        try {
            String[] info = loadStudentNameProgram(studentID);
            if (info == null) {
                JOptionPane.showMessageDialog(this, "Student not found. Please try again.");
                clearDisplay();
                return;
            }

            currentStudentId = studentID;
            currentStudentName = info[0];
            currentProgram = info[1];
            currentStudentEmail = loadStudentEmail(studentID);

            ArrayList<String> semList = loadAllSemesters(studentID);
            if (semList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No semester record found for this student.");
                clearDisplay();
                return;
            }

            //Combobox：sem + Year + All
            jComboBox1.removeAllItems();
            boolean hasY1 = false, hasY2 = false, hasY3 = false;

            for (String s : semList) {
                jComboBox1.addItem(s);
                if (s.startsWith("Y1")) hasY1 = true;
                else if (s.startsWith("Y2")) hasY2 = true;
                else if (s.startsWith("Y3")) hasY3 = true;
            }

            if (hasY1) jComboBox1.addItem("Year 1");
            if (hasY2) jComboBox1.addItem("Year 2");
            if (hasY3) jComboBox1.addItem("Year 3");

            jComboBox1.addItem("All");
            jComboBox1.setSelectedItem("All");
            jComboBox1.setEnabled(true);

            currentIntake = "All";

            loadAndDisplayReport(studentID, "All");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating report.");
        }
    }//GEN-LAST:event_apr_searchActionPerformed

    private void sid_searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sid_searchActionPerformed
        apr_searchActionPerformed(evt); //when keyin the sid and press enter also can search
    }//GEN-LAST:event_sid_searchActionPerformed

    private void jButtonUserManagementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUserManagementActionPerformed
        new AdminDashboard(currentUser).setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButtonUserManagementActionPerformed

    private void jButtonEligibilityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEligibilityActionPerformed
        new ECE_UI(currentUser).setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButtonEligibilityActionPerformed

    private void jButtonRecoveryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecoveryActionPerformed
        new CRP_UI(currentUser).setVisible(true);
        this.dispose();
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

    private void apr_pdfButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apr_pdfButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_apr_pdfButtonActionPerformed

    private void btnHome1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHome1ActionPerformed
        new MainDashboard(currentUser).setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnHome1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
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

        /* Create and display the form */
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
        
        //Create and Display Form
//        java.awt.EventQueue.invokeLater(() -> new APR_UI().setVisible(true));
    }

    //Table Summary ComboBox
    private void clearDisplay() {
        javax.swing.table.DefaultTableModel model =
                (javax.swing.table.DefaultTableModel) apr_tabel1.getModel();
        model.setRowCount(0);
        jTextArea_summary.setText("");
        jComboBox1.removeAllItems();
        jComboBox1.setEnabled(false);

        currentStudentId = null;
        currentStudentName = "";
        currentProgram = "";
        currentIntake = "All";
        currentStudentEmail = "";
        achievementsMap.clear();
        weaknessesMap.clear();
        improveMap.clear();
    }

    //Table + Summary
    private void loadAndDisplayReport(String studentID, String intakeSelection) throws Exception {

        String[] stu = loadStudentNameProgram(studentID);
        if (stu == null) throw new Exception("Student not found.");

        //Load all semesters
        ArrayList<String> semesters = loadAllSemesters(studentID);
        if (semesters.isEmpty()) {
            throw new Exception("No semester found.");
        }

        achievementsMap.clear();
        weaknessesMap.clear();
        improveMap.clear();

        //Filter
        ArrayList<String> filtered = new ArrayList<>();

        if ("All".equalsIgnoreCase(intakeSelection)) {
            filtered.addAll(semesters);

        } else if (intakeSelection.startsWith("Year")) {

            //Year 1 → Y1S1 Y1S2
            String yearNum = intakeSelection.substring(5).trim();
            for (String s : semesters) {
                if (s.startsWith("Y" + yearNum)) {
                    filtered.add(s);
                }
            }

        } else {
            //Single Semester (Y1S1)
            for (String s : semesters) {
                if (s.equals(intakeSelection)) {
                    filtered.add(s);
                }
            }
        }

        if (filtered.isEmpty()) {
            filtered.addAll(semesters);
        }

        javax.swing.table.DefaultTableModel model =
                (javax.swing.table.DefaultTableModel) apr_tabel1.getModel();
        model.setRowCount(0);  // clear table

        double totalCGPA = 0;
        int semCount = 0;
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

            StringBuilder achievements = new StringBuilder();
            StringBuilder weaknesses = new StringBuilder();
            StringBuilder improvements = new StringBuilder();

            for (String[] c : courseList) {
                String code = c[0];
                String titleTxt = c[1];
                String credit = c[2];

                String[] gradeData = loadGrade(studentID, code, sem);
                String grade = gradeData[0];
                String gpa = gradeData[1];
                double ass = Double.parseDouble(gradeData[2]);
                double exam = Double.parseDouble(gradeData[3]);

                //Add to JTable
                model.addRow(new Object[]{sem, code, titleTxt, credit, grade, gpa});

                //Achievements
                if (grade.startsWith("A")) {
                    achievements.append("- Strong performance in ").append(code).append(".\n");
                }

                //Weakness
                double gpaValue = 0;
                try {
                    gpaValue = Double.parseDouble(gpa);
                } catch (Exception ignore) {}

                if (gpaValue < 2.00) {
                    weaknesses.append("- Weak performance in ").append(code).append(".\n");
                    failCounter++;
                }

                //Improvements
                if (ass < 50) improvements.append("- Resit assignment for " + code + ".\n");
                if (exam < 50) improvements.append("- Resit examination for " + code + ".\n");
            }

            if (semCGPA < 2.00 && semCGPA > 0) {
                repeatSemester = true;
            }
            failTotal += failCounter;

            achievementsMap.put(sem, achievements);
            weaknessesMap.put(sem, weaknesses);
            improveMap.put(sem, improvements);
        }

        //Overall CGPA
        double finalCGPA = (semCount > 0) ? totalCGPA / semCount : 0;

        //Summary
        StringBuilder summary = new StringBuilder();
        summary.append("Academic Performance Report\n\n");
        summary.append("Student Name : ").append(stu[0]).append("\n");
        summary.append("Student ID   : ").append(studentID).append("\n");
        summary.append("Program      : ").append(stu[1]).append("\n");
        summary.append("Intake       : ").append(intakeSelection).append("\n\n");

        if ("All".equalsIgnoreCase(intakeSelection) || intakeSelection.startsWith("Year")) {

            //List all sem CGPA
            for (String sem : filtered) {
                double semCGPA = loadCGPA(studentID, sem);
                summary.append("Semester : ").append(sem).append("\n");
                summary.append("CGPA     : ").append(String.format("%.2f", semCGPA)).append("\n\n");
            }

            summary.append("Summary of Progress:\n");
            if (finalCGPA >= 3.50)
                summary.append("- The student demonstrates excellent academic performance.\n\n");
            else if (finalCGPA >= 3.00)
                summary.append("- The student shows good consistent progress.\n\n");
            else if (finalCGPA >= 2.00)
                summary.append("- The student achieved satisfactory performance, improvement needed.\n\n");
            else
                summary.append("- The student requires significant academic improvement.\n\n");

            //Achievements / Weakness / Recommendations for each sem
            for (String sem : filtered) {
                StringBuilder ach = achievementsMap.get(sem);
                StringBuilder weak = weaknessesMap.get(sem);
                StringBuilder impr = improveMap.get(sem);

                summary.append("Semester : ").append(sem).append("\n");

                if (ach != null && ach.length() > 0)
                    summary.append("Key Achievements:\n").append(ach).append("\n");

                if (weak != null && weak.length() > 0)
                    summary.append("Areas to Improve:\n").append(weak).append("\n");

                summary.append("Recommendations of Improvement:\n");
                if (impr != null && impr.length() > 0)
                    summary.append(impr);
                else
                    summary.append("- The student has shown good learning attitude and consistent effort.\n");

                summary.append("\n");
            }

            if (failTotal >= 4 || repeatSemester) {
                summary.append("Note:\n");
                summary.append("- Repeating the semester is recommended based on overall performance.\n");
            }

        } else {
            //summary
            summary.append("Summary of Progress:\n");
            if (finalCGPA >= 3.50)
                summary.append("- The student demonstrates excellent academic performance.\n\n");
            else if (finalCGPA >= 3.00)
                summary.append("- The student shows good consistent progress.\n\n");
            else if (finalCGPA >= 2.00)
                summary.append("- The student achieved satisfactory performance, improvement needed.\n\n");
            else
                summary.append("- The student requires significant academic improvement.\n\n");

            String sem = filtered.get(0);
            StringBuilder ach = achievementsMap.get(sem);
            StringBuilder weak = weaknessesMap.get(sem);
            StringBuilder impr = improveMap.get(sem);

            if (ach != null && ach.length() > 0)
                summary.append("Key Achievements:\n").append(ach).append("\n");

            if (weak != null && weak.length() > 0)
                summary.append("Areas to Improve:\n").append(weak).append("\n");

            summary.append("Recommendations of Improvement:\n");
            if (impr != null && impr.length() > 0)
                summary.append(impr);
            else
                summary.append("- The student has shown good learning attitude and consistent effort.\n");

            if (failTotal >= 4 || repeatSemester)
                summary.append("- Repeating the semester is recommended.\n");
        }

        jTextArea_summary.setText(summary.toString());
    }

    //File loading function 

    public String[] loadStudentNameProgram(String id) throws IOException {
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

    public String loadStudentEmail(String id) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("data/students.txt"));
        br.readLine(); 

        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length >= 5) {
                String sid = parts[0];
                String email = parts[4];
                if (sid.equals(id)) {
                    br.close();
                    return email;
                }
            }
        }
        br.close();
        return null;
    }

    public ArrayList<String> loadAllSemesters(String id) throws IOException {
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

        addSemestersFromResultFile(list, id, "data/result.txt");
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

    private void addSemestersFromResultFile(ArrayList<String> list, String id, String filename) throws IOException {
        File f = new File(filename);
        if (!f.exists()) return;

        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();  // skip header
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");
            if (!st.hasMoreTokens())
                continue;
            String sid = st.nextToken();
            if (!st.hasMoreTokens())
                continue;
            String sem = st.nextToken();
            if (sid.equals(id) && !list.contains(sem)) {
                list.add(sem);
            }
        }
        br.close();
    }

    public String[] loadCourseInfo(String code) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("data/courses.txt"));
        br.readLine();
        String line;

        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length < 7) continue;

            if (parts[0].equals(code)) {
                return new String[]{parts[0], parts[1], parts[2]};
            }
        }
        br.close();
        return new String[]{code, "Unknown Course", "0"};
    }

    public ArrayList<String[]> loadStudentCourses(String id, String sem) throws IOException {
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

    private String[] loadGradeFromFile(String id, String code, String sem,
                                       String filename, boolean hasSemesterColumn) throws IOException {
        File f = new File(filename);
        if (!f.exists()) return null;

        BufferedReader br = new BufferedReader(new FileReader(f));
        br.readLine();
        String line;

        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");

            if (!st.hasMoreTokens())
                continue;
            String sid = st.nextToken();
            if (!st.hasMoreTokens())
                continue;
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

    public String[] loadGrade(String id, String code, String sem) throws IOException {

        String[] data = loadGradeFromFile(id, code, sem, "data/grades.txt", true);
        if (data != null) return data;

        return new String[]{"-", "0.00", "0", "0"};
    }

    private Double loadCGPAFromFile(String id, String sem, String filename) throws IOException {
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

    public double loadCGPA(String id, String sem) throws IOException {
        Double v = loadCGPAFromFile(id, sem, "data/result.txt");
        if (v != null) return v.doubleValue();

        v = loadCGPAFromFile(id, sem, "data/resultArchive.txt");
        if (v != null) return v.doubleValue();

        return 0.0;
    }

    //PDF Export
    public String exportToPDF() throws Exception {

        if (currentStudentId == null) {
            return null;
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

        //Report Title
        Paragraph title = new Paragraph("Academic Performance Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15f);
        doc.add(title);

        //Student Info
        doc.add(new Paragraph("Student Name : " + currentStudentName, infoFont));
        doc.add(new Paragraph("Student ID   : " + currentStudentId, infoFont));
        doc.add(new Paragraph("Program      : " + currentProgram, infoFont));
        doc.add(Chunk.NEWLINE);

        //Generate tables by sem
        ArrayList<String> semesters = loadAllSemesters(currentStudentId);
        ArrayList<String> filtered = new ArrayList<>();

        if (currentIntake == null || "All".equalsIgnoreCase(currentIntake)) {
            filtered.addAll(semesters);
        } else if (currentIntake.startsWith("Year")) {
            String yearNum = currentIntake.substring(5).trim();
            for (String s : semesters) {
                if (s.startsWith("Y" + yearNum)) filtered.add(s);
            }
        } else {
            for (String s : semesters) {
                if (s.equals(currentIntake)) filtered.add(s);
            }
        }
        if (filtered.isEmpty()) filtered.addAll(semesters);

        for (String sem : filtered) {

            //Sem Title
            Paragraph semTitle = new Paragraph("Semester: " + sem, semTitleFont);
            semTitle.setSpacingBefore(8f);
            semTitle.setSpacingAfter(4f);
            doc.add(semTitle);

            //Course Table
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
            int totalCreditHours = 0;

            for (String[] c : courseList) {
                String code = c[0];
                String titleTxt = c[1];
                String credit = c[2];

                String[] gradeData = loadGrade(currentStudentId, code, sem);
                String grade = gradeData[0];
                String gpa = gradeData[1];

                try {
                    totalCreditHours += Integer.parseInt(credit.trim());
                } catch (Exception ignore) {}

                pdfTable.addCell(new Phrase(code, infoFont));
                pdfTable.addCell(new Phrase(titleTxt, infoFont));
                pdfTable.addCell(new Phrase(credit, infoFont));
                pdfTable.addCell(new Phrase(grade, infoFont));
                pdfTable.addCell(new Phrase(gpa, infoFont));
            }

            doc.add(pdfTable);

            //Summary rows under table: Total Credit Hours + CGPA
            double semCGPA = loadCGPA(currentStudentId, sem);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(40);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setSpacingBefore(4f);

            PdfPCell c1 = new PdfPCell(new Phrase("Total Credit Hours", headerFont));
            PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(totalCreditHours), infoFont));
            c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
            c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(c1);
            summaryTable.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase("CGPA", headerFont));
            PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.2f", semCGPA), infoFont));
            c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(c3);
            summaryTable.addCell(c4);

            doc.add(summaryTable);

            //Summary Section
            Paragraph semSummaryTitle = new Paragraph("Summary for " + sem, semTitleFont);
            semSummaryTitle.setSpacingBefore(5f);
            semSummaryTitle.setSpacingAfter(2f);
            doc.add(semSummaryTitle);

            Paragraph p = new Paragraph();
            p.setFont(infoFont);

            p.add("Summary of Progress:\n");
            if (semCGPA >= 3.50)
                p.add("- The student demonstrates excellent academic performance.\n\n");
            else if (semCGPA >= 3.00)
                p.add("- The student shows good consistent progress.\n\n");
            else if (semCGPA >= 2.00)
                p.add("- The student achieved satisfactory performance, improvement needed.\n\n");
            else
                p.add("- The student requires significant academic improvement.\n\n");

            StringBuilder ach = (achievementsMap != null) ? achievementsMap.get(sem) : null;
            if (ach != null && ach.length() > 0) {
                p.add("Key Achievements:\n");
                p.add(ach.toString());
                p.add("\n");
            }

            StringBuilder impr = (improveMap != null) ? improveMap.get(sem) : null;
            p.add("Recommendations of Improvement:\n");
            if (impr != null && impr.length() > 0) {
                p.add(impr.toString());
                p.add("\n");
            } else {
                p.add("- The student has shown good learning attitude and consistent effort.\n");
            }

            doc.add(p);
            doc.add(Chunk.NEWLINE);
        }

        doc.close();

        return fileName;
    }

    //Email function
    public void sendReportEmail(String toEmail, String pdfPath) {
            if (toEmail == null || toEmail.isEmpty()) {
                System.out.println("No email address, skip sending.");
                return;
            }

            final String fromEmail = "wongjolin0217@gmail.com";
            final String password  = "ptzvabojtjzppndv";

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail, "Academic System"));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(toEmail));
                message.setSubject("Your Academic Performance Report");

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(
                        "Dear " + currentStudentName + ",\n\n"
                      + "Attached is your Academic Performance Report.\n\n"
                      + "Regards,\nUniversity");

                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new FileDataSource(pdfPath);
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName(new java.io.File(pdfPath).getName());

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(textPart);
                multipart.addBodyPart(attachmentPart);

                message.setContent(multipart);

                Transport.send(message);
                System.out.println("Email sent to " + toEmail);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel apr_intake;
    private javax.swing.JButton apr_pdfButton;
    private javax.swing.JButton apr_search;
    private javax.swing.JLabel apr_sid1;
    private javax.swing.JTable apr_tabel1;
    private javax.swing.JLabel apr_titile;
    private javax.swing.JScrollPane arp_jScrollPane;
    private javax.swing.JButton btnHome;
    private javax.swing.JButton btnHome1;
    private javax.swing.JPanel dashboard;
    private javax.swing.JButton jButtonAPR;
    private javax.swing.JButton jButtonEligibility;
    private javax.swing.JButton jButtonRecovery;
    private javax.swing.JButton jButtonUserManagement;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea_summary;
    private javax.swing.JButton logout;
    private javax.swing.JTextField sid_search;
    // End of variables declaration//GEN-END:variables
}
