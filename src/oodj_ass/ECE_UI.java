package oodj_ass;

import java.util.ArrayList;
import java.io.*;
import java.awt.Color;
import javax.swing.JOptionPane;



public class ECE_UI extends javax.swing.JFrame {
    private User currentUser;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ECE_UI.class.getName());

   
    // Load CSV file
    public ArrayList<String[]> loadFile(String filename) {
        ArrayList<String[]> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("data/" + filename))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                list.add(line.split(","));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Find student row by ID in student.txt
    private String[] findStudentById(ArrayList<String[]> students, String sid) {
        for (String[] s : students) {
            if (s[0].equalsIgnoreCase(sid)) {   // s[0] = studentID
                return s;
            }
        }
        return null; // not found
}
    
    private void updateTable(ArrayList<String[]> data) {
        javax.swing.table.DefaultTableModel model
                = (javax.swing.table.DefaultTableModel) jTable1.getModel();

        // Clear the table COMPLETELY (fixes the missing rows issue)
        model.setRowCount(0);
        model.getDataVector().clear();
        model.fireTableDataChanged();

        for (String[] row : data) {
            // Prevent blank lines from crashing the table
            if (row.length < 4) {
                continue;
            }

            model.addRow(new Object[]{
                row[0], // Student ID
                row[1], // Semester
                row[2], // CGPA
                row[3], // Eligibility
                "View" // Button column text
            });
        }
    }

    
    private void showDetails(String studentID, String semester, String eligibility) {
        ArrayList<String[]> grades = loadFile("grades.txt");
        ArrayList<String[]> courses = loadFile("courses.txt");

        ECEView_UI view = new ECEView_UI(studentID, semester, eligibility, grades, courses);
        view.setVisible(true);
    }
    
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
    
    public ECE_UI(User user) {
        initComponents();
        this.currentUser = user;
        
        ECE.updateGrades();
        ECE.calculateCGPA();
        
        ArrayList<String[]> all = loadFile("result.txt");
        updateTable(all);
                
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = jTable1.rowAtPoint(evt.getPoint());
                int col = jTable1.columnAtPoint(evt.getPoint());
                
                
        if (col == 4) { 
            String sid = jTable1.getValueAt(row, 0).toString();
            String sem = jTable1.getValueAt(row, 1).toString();
            String eligibility = jTable1.getValueAt(row, 3).toString();

            showDetails(sid, sem, eligibility);
        }
            }
        });

        // Set header font
        jTable1.getTableHeader().setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 20));

        // Center header text
        javax.swing.table.DefaultTableCellRenderer headerRenderer = 
                (javax.swing.table.DefaultTableCellRenderer) jTable1.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        
        addSidebarHover(jButtonUserManagement);
        addSidebarHover(jButtonEligibility);
        addSidebarHover(jButtonRecovery);
        addSidebarHover(jButtonAPR);
        
        logout.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                logout.setBackground(new java.awt.Color(150,170,170)); // hover
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                logout.setBackground(new java.awt.Color(120,140,140)); // normal
            }
        });

    }

    // Send eligibility email to ALL students in result.txt
    private void sendEligibilityEmails() {

        ArrayList<String[]> resultList = loadFile("result.txt");
        ArrayList<String[]> studentList = loadFile("students.txt");

        int sentCount = 0;
        int skippedCount = 0;

        for (String[] r : resultList) {
            String sid   = r[0];
            String sem   = r[1];
            String cgpa  = r[2];
            String elig  = r[3].trim().toUpperCase();  // IMPORTANT

            String[] s = findStudentById(studentList, sid);
            if (s == null) {
                skippedCount++;
                continue;
            }

            String firstName = s[1];
            String lastName  = s[2];
            String program   = s[3];
            String email     = s[4];

            String subject;
            String body;

            // ------------ ELIGIBLE EMAIL --------------
            if (elig.equals("YES")) {

                subject = "Eligibility Confirmation – " + sid;

                body = "Dear " + firstName + " " + lastName + ",\n\n"
                     + "This email is to inform you that you are ELIGIBLE to progress to the next level of study.\n\n"
                     + "Details:\n"
                     + "Student ID : " + sid + "\n"
                     + "Programme  : " + program + "\n"
                     + "Semester   : " + sem + "\n"
                     + "CGPA       : " + cgpa + "\n"
                     + "Eligibility: " + elig + "\n\n"
                     + "Please ensure your fees are paid before the start of the intake.\n"
                     + "You may proceed with your course registration according to the academic schedule.\n\n"
                     + "If you have any questions, kindly contact the Academic Office.\n\n"
                     + "Best regards,\n"
                     + "Academic Affairs Department";

            }
            // ------------ NOT ELIGIBLE EMAIL --------------
            else {
                subject = "Eligibility Status and Course Recovery – " + sid;

                body = "Dear " + firstName + " " + lastName + ",\n\n"
                     + "This email is to inform you that you are NOT ELIGIBLE to progress to the next level of study.\n\n"
                     + "Details:\n"
                     + "Student ID : " + sid + "\n"
                     + "Programme  : " + program + "\n"
                     + "Semester   : " + sem + "\n"
                     + "CGPA       : " + cgpa + "\n"
                     + "Eligibility: " + elig + "\n\n"
                     + "You are required to follow the Course Recovery Plan (CRP) to improve your academic standing.\n"
                     + "Please meet your academic advisor or course administrator to discuss your recovery options.\n"
                     + "Please ensure all outstanding fees are paid before beginning any recovery steps.\n\n"
                     + "If you have any questions, kindly contact the Academic Office.\n\n"
                     + "Best regards,\n"
                     + "Academic Affairs Department";
            }

            // ---------- ONLY SEND ONE TIME ----------
            try {
                Email mail = new Email();
                mail.sendEmail(email, subject, body);
                sentCount++;
            } catch (Exception ex) {
                ex.printStackTrace();
                skippedCount++;
            }
        }

        JOptionPane.showMessageDialog(
                this,
                "Eligibility email process completed.\n"
              + "Successfully sent: " + sentCount + "\n"
              + "Skipped / Failed: " + skippedCount,
                "Email Status",
                JOptionPane.INFORMATION_MESSAGE
        );
    }



    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroud = new javax.swing.JPanel();
        search1 = new javax.swing.JTextField();
        dropdown = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        search2 = new javax.swing.JButton();
        title = new javax.swing.JLabel();
        ece_sid = new javax.swing.JLabel();
        eceSendEmail = new javax.swing.JButton();
        dashboard = new javax.swing.JPanel();
        jButtonUserManagement = new javax.swing.JButton();
        jButtonEligibility = new javax.swing.JButton();
        jButtonRecovery = new javax.swing.JButton();
        jButtonAPR = new javax.swing.JButton();
        logout = new javax.swing.JButton();
        btnHome = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Eligibility Check and Enrolment");
        setMinimumSize(new java.awt.Dimension(1160, 700));
        setResizable(false);

        backgroud.setBackground(new java.awt.Color(183, 201, 197));
        backgroud.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        backgroud.setMaximumSize(new java.awt.Dimension(1160, 700));
        backgroud.setMinimumSize(new java.awt.Dimension(1160, 700));
        backgroud.setPreferredSize(new java.awt.Dimension(1160, 700));
        backgroud.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        search1.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        search1.setMaximumSize(new java.awt.Dimension(100, 30));
        search1.setMinimumSize(new java.awt.Dimension(100, 30));
        search1.setPreferredSize(new java.awt.Dimension(100, 30));
        search1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                search1ActionPerformed(evt);
            }
        });
        backgroud.add(search1, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 120, -1, -1));

        dropdown.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        dropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "Eligible", "No Eligible" }));
        dropdown.setPreferredSize(new java.awt.Dimension(180, 30));
        dropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dropdownActionPerformed(evt);
            }
        });
        backgroud.add(dropdown, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 120, -1, -1));

        jTable1.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Student ID", "Semester", "CGPA", "Eligibility", "Details"
            }
        ));
        jTable1.setRowHeight(35);
        jTable1.setShowGrid(false);
        jScrollPane1.setViewportView(jTable1);

        backgroud.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 170, 870, -1));

        search2.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        search2.setBorderPainted(false);
        search2.setFocusPainted(false);
        search2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        search2.setLabel("Search");
        search2.setPreferredSize(new java.awt.Dimension(70, 30));
        search2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                search2ActionPerformed(evt);
            }
        });
        backgroud.add(search2, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 120, -1, -1));

        title.setBackground(new java.awt.Color(157, 208, 153));
        title.setFont(new java.awt.Font("Serif", 1, 36)); // NOI18N
        title.setText("Eligibility Check and Enrolment");
        title.setToolTipText("");
        title.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        backgroud.add(title, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 40, -1, -1));

        ece_sid.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        ece_sid.setText("Student ID:");
        backgroud.add(ece_sid, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 120, -1, -1));

        eceSendEmail.setFont(new java.awt.Font("Serif", 0, 18)); // NOI18N
        eceSendEmail.setText("Send Eligibility Email");
        eceSendEmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                eceSendEmailActionPerformed(evt);
            }
        });
        backgroud.add(eceSendEmail, new org.netbeans.lib.awtextra.AbsoluteConstraints(930, 620, -1, -1));

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
                .addComponent(btnHome, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)
                .addGap(53, 53, 53)
                .addComponent(jButtonUserManagement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonEligibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRecovery, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonAPR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 167, Short.MAX_VALUE)
                .addComponent(logout)
                .addGap(62, 62, 62))
        );

        backgroud.add(dashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(backgroud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(backgroud, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void search1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_search1ActionPerformed
        search2ActionPerformed(evt);
    }//GEN-LAST:event_search1ActionPerformed

    private void search2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_search2ActionPerformed
        String searchID = search1.getText().trim();

        if (searchID.isEmpty() || searchID.equals("Enter Student ID")) {
            javax.swing.JOptionPane.showMessageDialog(this, "Please enter a Student ID!");
            return;
        }

        ArrayList<String[]> resultList = loadFile("result.txt");
        ArrayList<String[]> matched = new ArrayList<>();

        for (String[] r : resultList) {
            if (r[0].equalsIgnoreCase(searchID)) {   // Match Student ID
                matched.add(r);
            }
        }

        if (matched.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Student not found!");
        }

        updateTable(matched); 
    }//GEN-LAST:event_search2ActionPerformed

    private void dropdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dropdownActionPerformed
        String selected = dropdown.getSelectedItem().toString();
        ArrayList<String[]> resultList = loadFile("result.txt");
        ArrayList<String[]> filtered = new ArrayList<>();

        for (String[] r : resultList) {
            if (selected.equals("All")) {
                filtered.add(r);
            } 
            else if (selected.equals("Eligible") && r[3].equalsIgnoreCase("YES")) {
                filtered.add(r);
            } 
            else if (selected.equals("No Eligible") && r[3].equalsIgnoreCase("NO")) {
                filtered.add(r);
            }
        }

        updateTable(filtered);
    }//GEN-LAST:event_dropdownActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed

    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void eceSendEmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_eceSendEmailActionPerformed
        int choice = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to send eligibility emails to ALL students?",
                "Confirm Email Sending",
                javax.swing.JOptionPane.YES_NO_OPTION
        );

        if (choice == javax.swing.JOptionPane.YES_OPTION) {
            sendEligibilityEmails();
        }
    }//GEN-LAST:event_eceSendEmailActionPerformed

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


    /**
     * @param args the command line arguments
     */
//    public static void main(String args[]) {
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
//
//        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(() -> new ECE_UI().setVisible(true));
//    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroud;
    private javax.swing.JButton btnHome;
    private javax.swing.JPanel dashboard;
    private javax.swing.JComboBox<String> dropdown;
    private javax.swing.JButton eceSendEmail;
    private javax.swing.JLabel ece_sid;
    private javax.swing.JButton jButtonAPR;
    private javax.swing.JButton jButtonEligibility;
    private javax.swing.JButton jButtonRecovery;
    private javax.swing.JButton jButtonUserManagement;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton logout;
    private javax.swing.JTextField search1;
    private javax.swing.JButton search2;
    private javax.swing.JLabel title;
    // End of variables declaration//GEN-END:variables
}
