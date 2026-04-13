# Teknik Mülakat Hazırlık Notları (QA & SQL)

## 1. Catch-Up Modülü Analizi (Bölüm 3)

### 3.1 & 3.2 - İlk Yaklaşım ve Kritik Sorular
Bir QA olarak sadece "çalışıyor mu?" diye bakmak yerine, sistemin uçtan uca bütünlüğünü sorgularım:
*   **Retention Policy (Saklama Süresi):** Üretilen Catch-Up içerikleri diskte ne kadar süre tutulacak? (Örn: 7 gün sonra otomatik silinmeli mi?)
*   **EPG Sync:** Canlı yayın (EPG) saati kayarsa veya uzarsa (örn: maçın uzatmalara gitmesi), Catch-Up kaydı otomatik olarak güncelleniyor mu?
*   **Concurrency (Eşzamanlılık):** Aynı anda yüzlerce kanal için Catch-Up üretilmesi Backend ve DB üzerinde nasıl bir yük oluşturuyor?
*   **Geoblocking & Rights:** Belirli içeriklerin Catch-Up hakları olmayabilir. "Bu içerik geriye dönük izlenemez" kuralı nasıl yönetiliyor?
*   **User Experience (UX):** Kullanıcı ileri/geri sarma (seek) yapabilecek mi? Reklamlar Catch-Up içinde nasıl davranacak?

### 3.3 - Riskler ve Açık Noktalar
*   **Storage Exhaustion:** Eğer silme kuralı (expiry) düzgün çalışmazsa veritabanı ve disk kısa sürede dolar, tüm CMS çöker.
*   **Orphan Records:** Video silindiği halde metadata veritabanında kalırsa, kullanıcılara "Video Bulunamadı" hatası döner.

### 3.4 - Test Senaryosu Tasarımı
*   **EPG'den Üretim:** Belirlenen bir canlı yayın programı bittiğinde, Catch-Up içeriğinin otomatik olarak "Active" statüsünde sisteme düştüğünü doğrula.
*   **Lifecycle (Active → Expired):** Saklama süresi (örn: 7 gün) dolan bir içeriğin statüsünün otomatik olarak "Expired" olduğunu ve yayından kaldırıldığını doğrula.
*   **Expiry Sonrası Davranış:** Süresi dolan bir içeriğe direkt link ile erişilmeye çalışıldığında 404/Video Not Found hatası verdiğini ve Client listelerinden kaybolduğunu doğrula.
*   **Client Görünürlük:** Catch-Up içeriklerinin sadece yetkili (örn: Premium) kullanıcılara ve "Catch-Up" kategorisi altında gösterildiğini doğrula.

---

## 2. SQL Hata Analizi ve Çözümü (Bölüm 4)

### Hata Analizi
Mevcut sorgudaki temel hata, `LEFT JOIN` kullanılmasına rağmen `WHERE` koşulunun siparişi olmayan kullanıcıları filtrelemesidir.
*   Siparişi olmayan kullanıcılar için `o.user_id` ve `o.created_at` değerleri `NULL` gelir.
*   `WHERE o.created_at >= '2025-01-01'` koşulu, `NULL` değerleri karşılaştıramadığı için bu kullanıcıları sonuçtan tamamen çıkarır. Bu durum, `LEFT JOIN`'i fiilen `INNER JOIN`'e dönüştürür.

### Düzeltilmiş Sorgu (Solution)
```sql
SELECT u.id, u.name, COALESCE(SUM(o.amount), 0) AS total_amount
FROM users u
LEFT JOIN orders o ON o.user_id = u.id AND o.created_at >= '2025-01-01'
GROUP BY u.id, u.name;
```

### Önemli Değişiklikler:
1.  **Koşulun Yeri:** Tarih filtresi `WHERE`'den `ON` kısmına taşındı. Böylece siparişi olmayan kullanıcılar elenmeden sadece belirlenen tarihteki siparişler toplama dahil edilir.
2.  **COALESCE:** Siparişi olmayan kullanıcılarda toplamın `NULL` yerine `0` görünmesi sağlandı.

### QA Test Verisi Senaryoları:
*   **Scenario A:** 2025 öncesi siparişi olan ama 2025 sonrası hiç siparişi olmayan kullanıcı (Sonuç: 0 dönmeli).
*   **Scenario B:** Hiçbir zaman siparişi olmamış kullanıcı (Sonuç: 0 dönmeli).
*   **Scenario C:** Sadece 2025 sonrası siparişi olan kullanıcı (Sonuç: Doğru toplam dönmeli).
41: 
42: ### 4.4 - QA Bakış Açısı ve Tespit
43: **Hatayı Nasıl Fark Ederdim?**
44: *   **Boundary Value Analysis:** 2024-12-31 ve 2025-01-01 tarihlerinde siparişi olan kullanıcılar için veriyi manuel olarak DB'den kontrol ederek.
45: *   **Data Integrity Check:** Toplam kullanıcı sayısının, sorgu sonucundaki satır sayısına eşit olup olmadığını kontrol ederek (Eşit değilse `LEFT JOIN` bozulmuştur).
46: 
47: **Problemi Ortaya Çıkaracak Test Verileri:**
48: *   **Sıfır Kayıt:** Sistemde hiç siparişi olmayan bir "Ghost User" oluştururdum. Eğer bu kullanıcı raporda yoksa bug vardır.
49: *   **Eski Kayıt:** Sadece 2023 yılında siparişleri olan bir kullanıcı. Bu kullanıcı raporda 0 ile görünmelidir.
