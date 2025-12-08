package oodj_ass;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class UserManager {
    
    private final String FILE_NAME = "data/users.txt";
    private List<User> users;

    public UserManager(){
        this.users = new ArrayList<>();
        loadUsersFromFile(); 
    }
    
    public List<User> getAllUsers() {
        return users;
    }

    public void loadUsersFromFile() {
        if (users != null) {
            users.clear();
        } else {
            users = new ArrayList<>();
        }

        File file = new File(FILE_NAME);
        if (!file.exists()) {
            createDefaultAdmin(); 
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 7) { 
                    String id = data[0];
                    String user = data[1];
                    String pass = data[2];
                    String name = data[3];
                    String email = data[4];
                    String role = data[5];
                    boolean active = Boolean.parseBoolean(data[6]);

                    if (role.equals("ADMIN")) {
                        users.add(new Admin(id, user, pass, name, email, active));
                    } else if (role.equals("ACADEMIC_OFFICER")) {
                        users.add(new AcademicOfficer(id, user, pass, name, email, active));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
    }
    
    public User findByUsername(String username){
        for (User u : users){
            if(u.getUsername().equalsIgnoreCase(username)){
                return u;
            }
        }
        return null;
    }
    
    private void createDefaultAdmin() {
        users.add(new Admin("ADM001", "admin", "admin123", "Super Admin", "admin@crs.edu", true));
        saveUsersToFile(); 
    }

    public void saveUsersToFile() {
        try {
            File file = new File(FILE_NAME);
        
            System.out.println("Saving users to: " + file.getAbsolutePath());

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (User u : users) {
                    bw.write(u.toFileString());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    }
}