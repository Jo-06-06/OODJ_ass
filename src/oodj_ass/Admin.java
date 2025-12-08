/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oodj_ass;

/**
 *
 * @author priyanka.kannan
 */
public class Admin extends User{
    
    public Admin(String userId, String username, String password, String fullname, String email, boolean active) {
        super(userId, username, password, fullname, email, "ADMIN", active);
    }
    
}
