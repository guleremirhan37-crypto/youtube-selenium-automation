package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class YouTubePage extends BasePage {

    private static final Logger logger = LogManager.getLogger(YouTubePage.class);

    private By searchBox = By.name("search_query");
    private By searchButton = By.id("search-icon-legacy");
    private By filterButton = By.xpath("//button[@aria-label='Arama filtreleri' or @aria-label='Search filters']");
    private By videoTitle = By.id("video-title");
    private final By watchMetadataTitle = By.xpath("//ytd-watch-metadata//h1");
    private final By moviePlayer = By.id("movie_player");
    private final By videoRenderers = By.tagName("ytd-video-renderer");

    public YouTubePage(WebDriver driver) {
        super(driver);
    }

    public void search(String query) {
        clear(searchBox);
        type(searchBox, query);
        driver.findElement(searchBox).sendKeys(Keys.ENTER);
        wait.until(ExpectedConditions.presenceOfElementLocated(videoRenderers));
    }

    public int getVideoCount() {
        return driver.findElements(videoRenderers).size();
    }

    public void scrollDown() throws InterruptedException {
        int initialCount = getVideoCount();
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 2500);");
            Thread.sleep(4000); // 4 saniye bekle (yükleme süresi)
            int currentCount = getVideoCount();
            if (currentCount > initialCount) {
                return; // Yeni içerik yüklendi
            }
        }
    }

    public void handleCookieConsent() {
        try {
            WebElement accept = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(.,'Accept') or contains(.,'kabul') or contains(.,'Anlıyorum')]")));
            accept.click();
        } catch (Exception ignored) {}
    }

    public void applyVideoTypeFilter() throws InterruptedException {
        // UI üzerinden deneme
        try {
            jsClick(filterButton);
            String xpathFilter = "//yt-formatted-string[text()='Videolar' or text()='Video' or contains(.,'Video')]/ancestor::a[@id='endpoint']";
            WebElement videoFilter = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathFilter)));
            jsClick(videoFilter);
            Thread.sleep(3000);
        } catch (Exception e) {
            logger.warn("Video filtresi UI'dan uygulanamadı, URL'den devam edilecek.");
        }
    }

    public void applyShortDurationFilter() throws InterruptedException {
        // Bulletproof: URL'yi zorla (sp=EgQQARgE hem Video hem Short filtresini içerir)
        String currentUrl = driver.getCurrentUrl();
        String filteredUrl;
        if (currentUrl.contains("sp=")) {
            filteredUrl = currentUrl.replaceAll("sp=[^&]*", "sp=EgQQARgE");
        } else {
            filteredUrl = currentUrl + "&sp=EgQQARgE";
        }
        
        logger.info("Filtreli URL'ye gidiliyor: " + filteredUrl);
        driver.get(filteredUrl);
        Thread.sleep(5000); // Sonuçların yüklenmesi için kesin bekleme
    }

    public List<WebElement> getOrganicVideos(int targetCount, int maxMins) throws InterruptedException {
        List<String> foundLinks = new ArrayList<>();
        List<WebElement> verifiedVideos = new ArrayList<>();
        int scrollCount = 0;

        while (foundLinks.size() < targetCount && scrollCount < 15) {
            List<WebElement> allVideos = driver.findElements(videoRenderers);
            for (WebElement v : allVideos) {
                if (foundLinks.size() >= targetCount) break;

                String link;
                try {
                    WebElement titleElement = v.findElement(videoTitle);
                    link = titleElement.getAttribute("href");
                } catch (Exception e) { continue; }

                if (link == null || foundLinks.contains(link)) continue;
                if (isAdOrVerticalShorts(v)) continue;

                String duration = getDurationText(v);
                if (isDurationValid(duration, maxMins)) {
                    foundLinks.add(link);
                    verifiedVideos.add(v);
                    highlightElement(v);
                }
            }
            if (foundLinks.size() < targetCount) {
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 1500);");
                Thread.sleep(3000);
                scrollCount++;
            }
        }
        return verifiedVideos;
    }

    public void selectFirstOrganicVideo() {
        wait.until(ExpectedConditions.presenceOfElementLocated(videoRenderers));
        List<WebElement> allVideos = driver.findElements(videoRenderers);
        for (WebElement v : allVideos) {
            if (!isAdOrVerticalShorts(v)) {
                jsClick(v.findElement(videoTitle));
                return;
            }
        }
    }

    public String getVideoTitle() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(watchMetadataTitle)).getText();
    }

    public boolean isPlayerDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(moviePlayer)).isDisplayed();
    }

    public String getWatchPageVideoDuration() {
        try {
            // Video oynatıcı içindeki süreyi bul (genelde span.ytp-time-duration)
            WebElement duration = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("span.ytp-time-duration")));
            highlightElement(duration);
            return duration.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public WebElement getDurationElement(WebElement v) {
        try {
            // New YouTube UI uses badge-shape for duration
            By durationXpath = By.xpath(".//ytd-thumbnail-overlay-time-status-renderer//badge-shape/div");
            return v.findElement(durationXpath);
        } catch (Exception e) {
            return null;
        }
    }

    private String getDurationText(WebElement v) {
        try {
            By durationXpath = By.xpath(".//ytd-thumbnail-overlay-time-status-renderer//badge-shape/div");
            WebElement durationLabel = v.findElement(durationXpath);
            String text = durationLabel.getText().trim();
            if (text.isEmpty()) {
                Thread.sleep(500);
                text = durationLabel.getText().trim();
            }
            if (!text.isEmpty()) {
                logger.info("Bulunan video süresi: " + text);
            }
            return text;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isDurationValid(String duration, int maxMins) {
        if (duration.isEmpty() || duration.contains("LIVE") || duration.contains("CANLI")) return false;
        String[] parts = duration.split(":");
        try {
            // HH:MM:SS -> 3 parça ise kesinlikle uzundur
            if (parts.length > 2) return false;
            
            if (parts.length == 2) {
                int mins = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                // maxMins=4 ise, dakika 4 veya daha fazlaysa reddet (4:00, 4:20 vb.)
                return mins < maxMins;
            }
            // Sadece saniye ise (ör: 0:45) kabul et
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAdOrVerticalShorts(WebElement v) {
        try {
            String text = v.getText();
            if (text.contains("Sponsor") || text.contains("Ad") || text.contains("Reklam")) return true;
            String href = v.findElement(videoTitle).getAttribute("href");
            return href == null || href.contains("/shorts/");
        } catch (Exception e) {
            return true;
        }
    }
}
