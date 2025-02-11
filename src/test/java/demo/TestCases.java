package demo;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
// import io.github.bonigarcia.wdm.WebDriverManager;
import demo.wrappers.Wrappers;

public class TestCases {
    ChromeDriver driver;
    Wrappers wrapper;
    ObjectMapper mapper = new ObjectMapper();
    List<Map<String, Object>> hockeyData = new ArrayList<>();
    List<Map<String, Object>> oscarData = new ArrayList<>();

    /*
     * TODO: Write your tests here with testng @Test annotation.
     * Follow `testCase01` `testCase02`... format or what is provided in
     * instructions
     */

    /*
     * Do not change the provided methods unless necessary, they will help in
     * automation and assessment
     */
    @BeforeTest
    public void startBrowser() throws IOException {
        System.setProperty("java.util.logging.config.file", "logging.properties");

        // NOT NEEDED FOR SELENIUM MANAGER
        // WebDriverManager.chromedriver().timeout(30).setup();

        ChromeOptions options = new ChromeOptions();
        LoggingPreferences logs = new LoggingPreferences();

        logs.enable(LogType.BROWSER, Level.ALL);
        logs.enable(LogType.DRIVER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logs);
        options.addArguments("--remote-allow-origins=*");

        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, "build/chromedriver.log");
        File outputDir = new File("./output");
        if (!outputDir.exists()) {
            boolean dirCreated = outputDir.mkdirs();
            if (!dirCreated) {
                throw new IOException("Failed to create output directory");
            }
        }

        driver = new ChromeDriver(options);
        wrapper = new Wrappers(driver);

        driver.manage().window().maximize();
    }

    @AfterTest
    public void endTest() {
        driver.close();
        driver.quit();

    }

    @Test
    public void testCase01() throws Exception {
        driver.get("https://www.scrapethissite.com/pages/");
        wrapper.click(By.xpath("//a[text()='Hockey Teams: Forms, Searching and Pagination']"));
        Thread.sleep(2000);
        // Loop through the first 4 pages
        for (int page = 1; page <= 4; page++) {
            // Find all rows representing teams
            List<WebElement> rows = wrapper.findElements(By.xpath("//tr[@class='team']"));
            for (WebElement row : rows) {
                double winPct = Double.parseDouble(row
                        .findElement(By.xpath(
                                ".//td[contains(@class,'pct text-success') or contains(@class,'pct text-danger')]"))
                        .getText());
                // Filter teams with a win percentage less than 0.40
                if (winPct < 0.40) {
                    Map<String, Object> record = new LinkedHashMap<>();
                    record.put("epoch", System.currentTimeMillis());
                    record.put("team", row.findElement(By.xpath(".//td[@class='name']")).getText().trim());
                    record.put("year", row.findElement(By.xpath(".//td[@class='year']")).getText());
                    record.put("winPct", winPct);
                    hockeyData.add(record);
                }
            }
            WebElement nextPage = driver.findElement(By.xpath("//a[@aria-label='Next']"));
            nextPage.click();
        }
        // Write the collected data to a JSON file
        mapper.writeValue(new File("./output/hockey-team-data.json"), hockeyData);
    }

    @Test
    public void testCase02() throws Exception {
        driver.get("https://www.scrapethissite.com/pages/");
        wrapper.click(By.xpath("//a[text()='Oscar Winning Films: AJAX and Javascript']"));
        Thread.sleep(2000);
        // Find all year links
        List<WebElement> yearLinks = wrapper.findElements(By.xpath("//a[@class='year-link']"));
        for (WebElement yearLink : yearLinks) {
            String year = yearLink.getText();
            yearLink.click();
            Thread.sleep(4000);
            // Get the first 5 movies listed
            List<WebElement> movies = wrapper.findElements(By.xpath("//tr[@class='film']")).subList(0, 5);

            for (WebElement movie : movies) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("epoch", System.currentTimeMillis());
                record.put("year", year);
                record.put("title", movie.findElement(By.xpath(".//td[@class='film-title']")).getText());
                record.put("nomination", movie.findElement(By.xpath(".//td[@class='film-nominations']")).getText());
                record.put("awards", movie.findElement(By.xpath(".//td[@class='film-awards']")).getText());
                record.put("isWinner",
                        movie.findElements(By.xpath(".//i[@class='glyphicon glyphicon-flag']")).size() > 0);
                oscarData.add(record);
            }
        }
        // Write the collected data to a JSON file
        mapper.writeValue(new File("./output/oscar-winner-data.json"), oscarData);
    }
}