/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oodj_ass;
import javax.swing.*;        
import java.awt.event.*;    
import java.io.*;            
import java.util.StringTokenizer; 

/**
 *
 * @author jolin
 */
public class APR {
    public static void main(String[]args){
        
        JFrame window = new JFrame();
        
        window.setTitle("Student Academic Performance Reporting");
        window.setSize(600,600);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel panel = new JPanel();
        
        JLabel lbl = new JLabel ("Student ID: ");
        JTextField txt = new JTextField(12);
        
        panel.add(lbl);
        panel.add(txt);
        
        JButton btn = new JButton(" Search ");
        panel.add(btn);
        
        window.add(panel);
        window.setVisible(true);
        
        // Event Handler
        btn.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                
                String studentID = txt.getText().trim();
                
                if (studentID.equals("")){
                    JOptionPane.showMessageDialog(null, "Please enter your Student ID: "); 
                    return;
                }
                
                // Find Student Semester
                try{
                    String semester = findSemester(studentID);
                    if (semester == null) {
                        JOptionPane.showMessageDialog(null,"Student ID not found. Please try again.");
                        return;
                    }
                    
                    // Load from txt file
                    String courseList = loadCourses(semester);
                    String cgpa = loadCGPA(studentID, semester);
                    
                    // Report
                    String report = "";
                    report += "----- Academic Performance Report -----\n\n";
                    report += "Student ID: " + studentID + "\n";
                    report += "Semester: " + semester + "\n\n";
                    report += "Courses:\n" + courseList + "\n";
                    report += "CGPA: " + cgpa + "\n";
                    
                    JOptionPane.showMessageDialog(null,report);
                    
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "File error occured."); 
                }
            }
        });
    }

    // FIND SEMESTER 
    public static String findSemester(String studentID) throws IOException {
        File file = new File("data/studentInfo.txt");
        if (!file.exists()) return null;

        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        // skip header
        String line = br.readLine(); 

        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");

            if (st.countTokens() != 2) continue;

            String id = st.nextToken();
            String sem = st.nextToken();

            if (id.equals(studentID)) {
                br.close();
                fr.close();
                return sem;  
            }
        }

        br.close();
        fr.close();
        return null;
    }

    // LOAD COURSES FOR SEMESTER 
    public static String loadCourses(String semester) throws IOException {
        File file = new File("data/courses.txt");
        if (!file.exists()) return "No course file.";

        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        String line = br.readLine(); 

        String result = "";

        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");

            if (st.countTokens() < 4) continue;

            String courseID = st.nextToken();
            String courseName = st.nextToken();
            String credit = st.nextToken();
            String courseSem = st.nextToken();

            if (courseSem.equals(semester)) {
                result += courseID + " - " + courseName + " (" + credit + " credits)\n";
            }
        }

        br.close();
        fr.close();
        return result.equals("") ? "No courses found.\n" : result;
    }

    // LOAD CGPA 
    public static String loadCGPA(String studentID, String semester) throws IOException {

        File file = new File("data/result.txt");
        if (!file.exists()) return "0.00";

        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);

        String line = br.readLine(); 

        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");

            if (st.countTokens() != 3) continue;

            String id = st.nextToken();
            String sem = st.nextToken();
            String cgpa = st.nextToken();

            if (id.equals(studentID) && sem.equals(semester)) {
                br.close();
                fr.close();
                return cgpa;
            }
        }

        br.close();
        fr.close();
        return "0.00";
    }
}    
                                                           
                    
                    
               