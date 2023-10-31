package seleniumsul;

import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.learnlib.api.SUL;
import de.learnlib.api.exception.SULException;

/**
 * Created by ramon on 12-12-16.
 */
public class SeleniumSUL implements SUL<String, String>, AutoCloseable {

    private final String URI;
    private final WebDriver driver;

    public SeleniumSUL(String uri, WebDriver driver) {
        URI = uri;

        this.driver = driver;
        driver.get(URI);
    }

    @Override
    public void pre() {
    }

    @Override
    public void post() {
        if (!this.clickById("reset")) {
            throw new SULException(new RuntimeException("Cannot reset SUL"));
        }
    }

    @Override
    public void close() throws Exception {
        driver.quit();
    }

    @Nullable
    @Override
    public String step(@Nullable String input) throws SULException {
        if (handleInput(input)) {
            return "OK";
        } else {
            return "NOK";
        }
    }

    private boolean handleInput(String input) {
        switch (input) {
            case "5ct":
            case "10ct":
                return clickById(input);
            case "mars":
            case "twix":
            case "snickers":
            	boolean stepped = clickById(input);
            	boolean cont = clickById("continue");
                return stepped && cont;
            default:
                throw new SULException(new RuntimeException("Unknown input " + input));
        }
    }

    private boolean clickById(String id) {
        WebElement element = driver.findElement(By.id(id));
        if (element != null && element.isDisplayed()) {
            element.click();
            return true;
        } else {
            return false;
        }
    }
}
