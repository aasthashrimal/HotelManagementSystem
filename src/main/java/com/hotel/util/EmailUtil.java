package com.hotel.util;

import java.util.Properties;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * WEEK 3 - MULTITHREADING: All emails sent on separate threads (async)
 */
public class EmailUtil {

    private static final String FROM_EMAIL   = "aasthashrimal22@gmail.com";
    private static final String APP_PASSWORD = "dwghxmzowwyudytf";
    private static final String SMTP_HOST    = "smtp.gmail.com";
    private static final int    SMTP_PORT    = 587;

    // ── Simple text email ────────────────────────────────────────────────────

    /**
     * Sends a plain text email asynchronously.
     * WEEK 3 - new Thread, start(), sleep()
     */
    public static void sendEmailAsync(String toEmail, String subject, String body) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(500); // WEEK 3 - sleep()
                sendTextEmail(toEmail, subject, body);
                FileLogger.logInfo("Email sent to: " + toEmail + " | " + subject);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                FileLogger.logError("Email failed [" + toEmail + "]: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.setName("EmailThread-" + System.currentTimeMillis());
        t.start(); // WEEK 3 - start()
    }

    // ── Email with PDF attachment ────────────────────────────────────────────

    /**
     * Sends email with PDF invoice attached asynchronously.
     * WEEK 3 - new Thread, start(), sleep()
     */
    public static void sendEmailWithPdfAsync(String toEmail, String subject,
                                              String body, String pdfPath) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(500); // WEEK 3 - sleep()
                sendEmailWithAttachment(toEmail, subject, body, pdfPath);
                FileLogger.logInfo("Email + PDF sent to: " + toEmail);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                FileLogger.logError("Email+PDF failed [" + toEmail + "]: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.setName("InvoiceMailThread-" + System.currentTimeMillis());
        t.start(); // WEEK 3 - start()
    }

    // ── Internal methods ─────────────────────────────────────────────────────

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    private static void sendTextEmail(String toEmail, String subject, String body)
            throws MessagingException {
        Session  session = buildSession();
        Message  msg     = new MimeMessage(session);
        setFrom(msg);
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject);
        msg.setText(body);
        Transport.send(msg);
    }

    private static void sendEmailWithAttachment(String toEmail, String subject,
                                                  String body, String pdfPath)
            throws MessagingException {
        Session session = buildSession();
        Message msg     = new MimeMessage(session);
        setFrom(msg);
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject);

        MimeMultipart multipart = new MimeMultipart();

        // Part 1: plain text body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body);
        multipart.addBodyPart(textPart);

        // Part 2: PDF attachment (if file exists)
        if (pdfPath != null && new java.io.File(pdfPath).exists()) {
            MimeBodyPart pdfPart = new MimeBodyPart();
            DataSource   source  = new FileDataSource(pdfPath);
            pdfPart.setDataHandler(new DataHandler(source));
            pdfPart.setFileName("AureliaSuites_Invoice.pdf");
            multipart.addBodyPart(pdfPart);
        }

        msg.setContent(multipart);
        Transport.send(msg);
    }

    private static void setFrom(Message msg) throws MessagingException {
        try {
            msg.setFrom(new InternetAddress(FROM_EMAIL, "Aurelia Suites"));
        } catch (java.io.UnsupportedEncodingException e) {
            msg.setFrom(new InternetAddress(FROM_EMAIL));
        }
    }

    // ── Email body builders ──────────────────────────────────────────────────

    /**
     * Booking CONFIRMATION — sent at booking time (no invoice attached).
     */
    public static String buildBookingConfirmationBody(String customerName, int bookingId,
                                                       int roomNumber, String roomType,
                                                       String checkIn) {
        return """
               Dear %s,

               Your booking at Aurelia Suites is CONFIRMED!

               ─────────────────────────────────────────
               Booking ID   : %d
               Room Number  : %d  (%s)
               Check-In     : %s
               ─────────────────────────────────────────

               Please carry a valid photo ID at check-in.
               Check-in time : 12:00 PM
               Check-out time: 11:00 AM

               For any queries:  aasthashrimal22@gmail.com

               We look forward to hosting you!

               Warm regards,
               Aurelia Suites Management
               """.formatted(customerName, bookingId, roomNumber, roomType, checkIn);
    }

    /**
     * Checkout email — PDF invoice is sent as attachment.
     */
    public static String buildCheckoutBody(String customerName) {
        return """
               Dear %s,

               Your checkout from Aurelia Suites is complete. Thank you!

               Your detailed invoice is attached as a PDF with this email.

               We'd love your feedback on your stay:
               https://forms.gle/PqAXr7aivstrgW7DA

               We hope you had a wonderful stay.
               Please do visit us again!

               Warm regards,
               Aurelia Suites Management
               """.formatted(customerName);
    }

    /**
     * Cancellation email — sent when booking is cancelled with invoice PDF.
     */
    public static String buildCancellationBody(String customerName, int bookingId,
                                                int roomNumber, String checkIn) {
        return """
               Dear %s,

               Your booking at Aurelia Suites has been CANCELLED.

               ─────────────────────────────────────────
               Booking ID   : %d
               Room Number  : %d
               Check-In     : %s
               ─────────────────────────────────────────

               Your booking cancellation has been processed.
               Your refund will be credited to your original payment method within 24-48 hours.

               Please find the detailed cancellation summary with refund amount in the attached PDF.

               If you have any questions, please contact us:
               aasthashrimal22@gmail.com

               We hope to welcome you again in the future!

               Warm regards,
               Aurelia Suites Management
               """.formatted(customerName, bookingId, roomNumber, checkIn);
    }
}
