package oodj_ass;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;


public class Email {
    private final String fromEmail = "wongjolin0217@gmail.com";
    private final String password  = "ptzvabojtjzppndv";
    private final Properties props;

    public Email() {
        props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
    }

    private Session getSession() {
        return Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });
    }

    // Send email to target recipient
    public boolean sendEmail(String toEmail, String subject, String messageText) {
        try {
            Message msg = new MimeMessage(getSession());
            msg.setFrom(new InternetAddress(fromEmail));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            msg.setSubject(subject);
            msg.setText(messageText);

            Transport.send(msg);
            System.out.println("Email sent successfully to " + toEmail);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}