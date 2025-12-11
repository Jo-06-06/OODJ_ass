package oodj_ass;

import java.util.List;

public class main {

    public static void main(String[] args) {

        Email mailer = new Email();

        FileLoader fl = new FileLoader();
        fl.loadAll();

        List<Student> students = fl.getStudents();
        
        CRP crp = new CRP(students, mailer);

        // Launch UI
        CRP_UI ui = new CRP_UI(fl, crp);
        ui.setVisible(true);
    }
}
