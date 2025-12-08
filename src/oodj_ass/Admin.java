package oodj_ass;


public class Admin extends User{
    
    public Admin(String userId, String username, String password, String fullname, String email, boolean active) {
        super(userId, username, password, fullname, email, "ADMIN", active);
    }
    
}
