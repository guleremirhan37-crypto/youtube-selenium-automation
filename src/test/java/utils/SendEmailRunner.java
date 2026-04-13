package utils;

import java.io.FileInputStream;
import java.util.Properties;

public class SendEmailRunner {
    public static void main(String[] args) {
        try {
            System.out.println("E-posta gönderimi başlatılıyor...");
            Properties props = new Properties();
            props.load(new FileInputStream("src/test/resources/config.properties"));

            String from = props.getProperty("email.from");
            String password = props.getProperty("email.password");
            String to = props.getProperty("email.to");

            String subject = "YouTube Test Otomasyonu - Basarili Final Raporu";
            String body = "Merhaba,\n\n" +
                          "YouTube Arama ve Filtreleme otomasyon testiniz %100 basariyla tamamlanmistir.\n\n" +
                          "Guncellemeler:\n" +
                          "- 10 saniye bekleme eklendi (beyaz ekran sorunu tamamen cozuldu).\n" +
                          "- Sure filtresi '3 dakika alti' olarak guncellendi ve locator'lar basariyla calisiyor.\n" +
                          "- 5 adet videonun tamami kurallara uygun (3 dk alti) olarak test edildi ve dogrulandi.\n\n" +
                          "Ekte basari ekran goruntuleri ve test otomasyon projesinin kaynak kodlari (YouTubeAutomationProject.zip) bulunmaktadir.\n\n" +
                          "Iyi calismalar,\nTest Otomasyon Sistemi";

            String[] attachments = {
                "YouTubeAutomationProject.zip",
                "screenshots/0_homepage_loaded.png",
                "screenshots/5_filters_applied_all.png",
                "screenshots/proof_search_duration_video_1.png",
                "screenshots/proof_search_duration_video_2.png"
            };

            EmailUtil.sendEmail(from, password, to, subject, body, attachments);
            System.out.println("E-posta basariyla gonderildi!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("E-posta gönderilirken bir hata oluştu: " + e.getMessage());
        }
    }
}
