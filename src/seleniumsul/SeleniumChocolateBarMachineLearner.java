package seleniumsul;

import com.google.common.collect.ImmutableSet;
import de.learnlib.api.SUL;
import basiclearner.BasicLearner;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by ramon on 13-12-16.
 */
//@SuppressWarnings("unused")
public class SeleniumChocolateBarMachineLearner {
    public static void main(String[] args) throws IOException {
        /* Ensure that the following parameters are correctly set! */
//        String candyMachineURI = "file://path/to/chocolatebarwebsite/website.htm";
        String candyMachineURI = "file:///Users/bharatgarhewal/eclipse-workspace/tt_learning/candy/website.htm";
        
//        String geckoDriverLocation = "path/to/webdriver"; // e.g. for firefox, the path of the geckodriver-file
//        System.setProperty("webdriver.gecko.driver", geckoDriverLocation);
        WebDriver driver = new ChromeDriver();

        /* If all is set, we can start learning */
        Collection<String> inputAlphabet = ImmutableSet.of("5ct", "10ct", "mars", "snickers", "twix");
        SUL<String, String> sul = new SeleniumSUL(candyMachineURI, driver);
        BasicLearner.runControlledExperiment(
                sul,
                BasicLearner.LearningMethod.RivestSchapire,
                BasicLearner.TestingMethod.UserQueries,
                inputAlphabet);

        driver.close();

//        Class c = sul.interfaces.Action.class;
    }

}
