package utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import java.io.File;

public class EmailUtil {

    public static void sendEmail(String fromEmail, String password, String toEmail, String subject, String body, String[] attachments) {
        
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.debug", "false");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            if (attachments != null) {
                for (String filePath : attachments) {
                    MimeBodyPart attachPart = new MimeBodyPart();
                    try {
                        attachPart.attachFile(new File(filePath));
                        multipart.addBodyPart(attachPart);
                    } catch (Exception e) {
                        System.out.println("Could not attach file: " + filePath);
                    }
                }
            }

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("Email sent successfully to " + toEmail);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
