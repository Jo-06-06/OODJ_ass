package oodj_ass;


public class User {
    private String userId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String role;
    private boolean active;
    
    public User(String userId, String username, String password, String fullname, String email, String role, boolean active){
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullname;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String toFileString() {
        return userId + "," + username + "," + password + "," + fullName + "," + email + "," + role + "," + active;
    }
}
