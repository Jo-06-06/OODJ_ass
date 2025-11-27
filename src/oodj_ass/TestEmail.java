/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package oodj_ass;

/**
 * Test class to verify EmailService is working correctly
 * Run this to test email functionality before integrating with other modules
 * 
 * @author Your Team
 */
public class TestEmail {
    
    public static void main(String[] args) {
        System.out.println("=== Course Recovery System - Email Service Test ===\n");
        
        // STEP 1: Configure Email Service
        // For Gmail: 
        // 1. Enable 2-Step Verification
        // 2. Generate App Password: https://myaccount.google.com/apppasswords
        // 3. Use that App Password below (NOT your regular password)
        
        String smtpHost = "smtp.gmail.com";
        int port = 587;
        String yourEmail = "YOUR_EMAIL@gmail.com";  // ⚠️ CHANGE THIS
        String yourAppPassword = "YOUR_APP_PASSWORD";  // ⚠️ CHANGE THIS
        
        System.out.println("📧 Initializing Email Service...");
        EmailService emailService = new EmailService(smtpHost, port, yourEmail, yourAppPassword);
        
        // STEP 2: Test Connection
        System.out.println("\n🔌 Testing SMTP Connection...");
        if (!emailService.testConnection()) {
            System.err.println("\n❌ Connection failed! Please check:");
            System.err.println("   - SMTP host and port are correct");
            System.err.println("   - Email and password are correct");
            System.err.println("   - For Gmail: Use App Password, not regular password");
            System.err.println("   - Firewall is not blocking port 587");
            return;
        }
        
        // STEP 3: Test Different Email Types
        String testRecipient = "student@example.com";  // ⚠️ CHANGE THIS to your test email
        
        System.out.println("\n📨 Testing Email Sending...\n");
        
        // Test 1: Account Creation Email
        System.out.println("1️⃣ Testing Account Creation Email...");
        boolean test1 = emailService.sendAccountCreationEmail(
            testRecipient,
            "Alex Tan",
            "Temp@123456"
        );
        System.out.println(test1 ? "   ✅ Success\n" : "   ❌ Failed\n");
        
        // Test 2: Password Recovery Email
        System.out.println("2️⃣ Testing Password Recovery Email...");
        boolean test2 = emailService.sendPasswordRecoveryEmail(
            testRecipient,
            "Alex Tan",
            "RESET-TOKEN-ABC123"
        );
        System.out.println(test2 ? "   ✅ Success\n" : "   ❌ Failed\n");
        
        // Test 3: Recovery Plan Email
        System.out.println("3️⃣ Testing Recovery Plan Email...");
        String actionPlan = 
            "Week 1-2: Review all lecture topics and complete practice exercises\n" +
            "Week 3: Attend consultation session with module lecturer\n" +
            "Week 4: Submit recovery assignment\n" +
            "Week 5: Take recovery examination";
        
        boolean test3 = emailService.sendRecoveryPlanEmail(
            testRecipient,
            "Alex Tan",
            "Object Oriented Programming (OOP)",
            actionPlan
        );
        System.out.println(test3 ? "   ✅ Success\n" : "   ❌ Failed\n");
        
        // Test 4: Performance Report Email
        System.out.println("4️⃣ Testing Performance Report Email...");
        boolean test4 = emailService.sendPerformanceReportEmail(
            testRecipient,
            "Alex Tan",
            3.25,
            "Semester 1, 2024/2025",
            "You have shown excellent progress this semester. Keep up the good work!"
        );
        System.out.println(test4 ? "   ✅ Success\n" : "   ❌ Failed\n");
        
        // Test 5: Eligibility Notification (Eligible)
        System.out.println("5️⃣ Testing Eligibility Notification (Eligible)...");
        boolean test5 = emailService.sendEligibilityNotification(
            testRecipient,
            "Alex Tan",
            true,  // is eligible
            3.25,
            0,  // no failed courses
            "Congratulations! You have met all requirements to progress to the next semester."
        );
        System.out.println(test5 ? "   ✅ Success\n" : "   ❌ Failed\n");
        
        // Test 6: Eligibility Notification (Not Eligible)
        System.out.println("6️⃣ Testing Eligibility Notification (Not Eligible)...");
        boolean test6 = emailService.sendEligibilityNotification(
            testRecipient,
            "Alex Tan",
            false,  // not eligible
            1.85,
            2,  // 2 failed courses
            "You need to complete the course recovery plan to improve your CGPA and clear failed modules."
        );
        System.out.println(test6 ? "   ✅ Success\n" : "   ❌ Failed\n");
        
        // Summary
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 TEST SUMMARY");
        System.out.println("=".repeat(50));
        int passed = (test1 ? 1 : 0) + (test2 ? 1 : 0) + (test3 ? 1 : 0) + 
                     (test4 ? 1 : 0) + (test5 ? 1 : 0) + (test6 ? 1 : 0);
        System.out.println("✅ Tests Passed: " + passed + "/6");
        System.out.println("❌ Tests Failed: " + (6 - passed) + "/6");
        
        if (passed == 6) {
            System.out.println("\n🎉 All tests passed! Email service is ready to use.");
            System.out.println("💡 You can now integrate EmailService with your other modules:");
            System.out.println("   - User Management (account creation, password reset)");
            System.out.println("   - Course Recovery Plan (recovery plan notifications)");
            System.out.println("   - Academic Performance Reporting (report emails)");
            System.out.println("   - Eligibility Check (eligibility notifications)");
        } else {
            System.out.println("\n⚠️ Some tests failed. Please check the error messages above.");
        }
    }
}