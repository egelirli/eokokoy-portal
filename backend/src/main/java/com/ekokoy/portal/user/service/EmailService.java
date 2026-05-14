package com.ekokoy.portal.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
    }

    /** Başvuru onaylandığında giriş bilgisi e-postası gönderir. */
    @Async
    public void sendApprovalEmail(String toEmail, String firstName) {
        String subject = "Ekoköy Portalı — Başvurunuz Onaylandı";
        String body = String.format("""
                Sayın %s,

                Ekoköy Portalı'na başvurunuz onaylanmıştır.
                Artık sisteme giriş yapabilirsiniz:

                %s/login

                Saygılarımızla,
                Ekoköy Yönetim Kurulu
                """, firstName, frontendUrl);
        sendSimpleEmail(toEmail, subject, body);
    }

    /** Başvuru reddedildiğinde bilgi e-postası gönderir. */
    @Async
    public void sendRejectionEmail(String toEmail, String firstName, String reason) {
        String subject = "Ekoköy Portalı — Başvurunuz Hakkında";
        String reasonText = (reason != null && !reason.isBlank())
                ? "\nRed gerekçesi: " + reason
                : "";
        String body = String.format("""
                Sayın %s,

                Ekoköy Portalı başvurunuz incelenmiş, ancak onaylanamamıştır.%s

                Daha fazla bilgi için yönetim kurulu ile iletişime geçebilirsiniz.

                Saygılarımızla,
                Ekoköy Yönetim Kurulu
                """, firstName, reasonText);
        sendSimpleEmail(toEmail, subject, body);
    }

    /** Ek bilgi talep edildiğinde bilgilendirme e-postası gönderir. */
    @Async
    public void sendRequestInfoEmail(String toEmail, String firstName, String message) {
        String subject = "Ekoköy Portalı — Başvurunuz İçin Ek Bilgi Gerekiyor";
        String body = String.format("""
                Sayın %s,

                Ekoköy Portalı başvurunuzla ilgili ek bilgiye ihtiyaç duyulmaktadır.

                %s

                Lütfen bu e-postaya yanıt vererek gerekli bilgileri sağlayınız.

                Saygılarımızla,
                Ekoköy Yönetim Kurulu
                """, firstName, message != null ? message : "");
        sendSimpleEmail(toEmail, subject, body);
    }

    /** Davet e-postası gönderir. rawToken URL'e eklenmek üzere ham token'dır. */
    @Async
    public void sendInvitationEmail(String toEmail, String rawToken, String roleDisplayName) {
        String subject = "Ekoköy Portalı — Davet";
        String link = frontendUrl + "/register?token=" + rawToken;
        String body = String.format("""
                Ekoköy Portalı'na davet edildiniz.

                Rol: %s

                Kayıt olmak için aşağıdaki bağlantıya tıklayınız (48 saat geçerlidir):
                %s

                Bu bağlantı yalnızca tek kullanımlıktır.

                Saygılarımızla,
                Ekoköy Yönetim Kurulu
                """, roleDisplayName, link);
        sendSimpleEmail(toEmail, subject, body);
    }

    /** Şifre sıfırlama bağlantısı e-postası gönderir. rawToken URL'e eklenmek üzere ham token'dır. */
    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String rawToken) {
        String subject = "Ekoköy Portalı — Şifre Sıfırlama";
        String link = frontendUrl + "/reset-password?token=" + rawToken;
        String body = String.format("""
                Sayın %s,

                Şifre sıfırlama talebiniz alınmıştır.

                Yeni şifrenizi belirlemek için aşağıdaki bağlantıya tıklayınız (1 saat geçerlidir):
                %s

                Bu işlemi siz yapmadıysanız bu e-postayı dikkate almayınız.

                Saygılarımızla,
                Ekoköy Yönetim Kurulu
                """, firstName, link);
        sendSimpleEmail(toEmail, subject, body);
    }

    /** Acil (urgent) duyuru bildirimi gönderir. */
    @Async
    public void sendAnnouncementEmail(String toEmail, String firstName, String title, String body) {
        String subject = "Ekoköy Portalı — Acil Duyuru: " + title;
        String text = String.format("""
                Sayın %s,

                Acil bir duyuru yayınlandı:

                %s

                %s

                Saygılarımızla,
                Ekoköy Yönetim Kurulu
                """, firstName, title, body);
        sendSimpleEmail(toEmail, subject, text);
    }

    private void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException e) {
            // Mail hatası uygulamayı durdurmamalı; log yeterli
        }
    }
}
