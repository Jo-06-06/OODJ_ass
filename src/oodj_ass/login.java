package oodj_ass;

public class login {
    
    private final UserManager userManager;
    private final String LOG_FILE = "login_log.dat"; 
    
    public login(UserManager userManager){
        this.userManager = userManager;
    }
    
    public User authenticate(String username, String password){
        if(username == null || password == null ) {
            return null;
        }

        User user = userManager.findByUsername(username.trim());

        if (user == null) return null; 
        if (!user.isActive()) return null; 
        if (!user.getPassword().equals(password)) return null; 
        
        String expectedRole;
        Logger.writeLog(user.getUsername(), "LOGIN");

        return user;
    }
}
