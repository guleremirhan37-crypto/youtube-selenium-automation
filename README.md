# YouTube Automation Test Suite

Bu proje, YouTube üzerinde belirli arama kriterlerine göre video sonuçlarını filtreleyen ve doğrulayan profesyonel bir Selenium otomasyon framework'üdür. Teknik mülakat gereksinimlerini karşılamak üzere **Page Object Model (POM)** tasarım deseni kullanılarak geliştirilmiştir.

## 🚀 Özellikler

-   **Page Object Model (POM):** Sürdürülebilir ve okunabilir kod yapısı.
-   **Akıllı Filtreleme:** YouTube üzerindeki reklamları (Sponsorlu içerikler) ve "Shorts" videolarını otomatik olarak eler.
-   **Otomatik Kanıt Toplama:** Her test adımında ekran görüntüsü (Screenshot) alır ve tüm süreci video (.avi) olarak kaydeder.
-   **Allure Reporting:** Detaylı adım adım raporlama ve dokümantasyon desteği.
-   **Config Yönetimi:** Test parametreleri (arama terimi, video sayısı vb.) `config.properties` üzerinden kolayca yönetilebilir.
-   **Linux/WSL2 Desteği:** Headless mode desteği ile CI/CD ortamlarına hazırdır.

## 🛠 Teknoloji Yığını

-   **Java 1.8+**
-   **Selenium WebDriver 4**
-   **JUnit 5**
-   **Maven**
-   **Allure Reports**
-   **Log4j2**

## 📂 Proje Yapısı

```text
src/test/java/
├── pages/          # Page Object Sınıfları (BasePage, YouTubePage)
├── tests/          # Test Senaryoları (YouTubeTest)
└── utils/          # Yardımcı Araçlar (Email, ScreenRecorder)
src/test/resources/
└── config.properties # Test konfigürasyonları
```

## 🏁 Çalıştırma

### Standart Çalıştırma:
```bash
mvn test
```

### Headless Mode (Arayüzsüz) Çalıştırma:
```bash
mvn test -Dheadless=true
```

## 📊 Raporlama

Test sonrası Allure raporlarını görüntülemek için:
```bash
allure serve allure-results
```

---
*Bu proje, emirhan güler tarafından teknik mülakat çalışması olarak hazırlanmıştır.*
