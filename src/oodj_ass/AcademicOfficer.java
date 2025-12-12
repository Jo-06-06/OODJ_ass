package oodj_ass;

public class AcademicOfficer extends User{
    
    public AcademicOfficer(String userId, String username, String password, String fullname, String email, boolean active) {
        super(userId, username, password, fullname, email, "ACADEMIC_OFFICER", active);
    }
    
}
