package basiclearner;

import java.util.Collection;
import de.learnlib.api.SUL;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.util.Properties;

public class FTPLearner
{
	public static void main(final String[] args) throws IOException {
		System.out.println("**************************");
		System.out.println("***FTP LEARNER v. 2.0*****");
		System.out.println("**************************");
		final String currentDirectory = System.getProperty("user.dir");

		Properties properties = new Properties();
		try (FileReader fileReader = new FileReader("config.properties")) {
			properties.load(fileReader);
		} catch (IOException e) {
			System.out.println("[ERROR] Problem reading config.properties file. It should be located at " + currentDirectory);
			return;
		}
		String ip;
		int port;
		int waitingTime;
		boolean debug;
		String[] commandArray;
		try {
			ip = properties.getProperty("ip");
			waitingTime = Integer.parseInt(properties.getProperty("waitingTime"));
			port = Integer.parseInt(properties.getProperty("port"));
			debug = Boolean.parseBoolean(properties.getProperty("debug"));
			final String commandsString = properties.getProperty("commands");
			commandArray = commandsString.split(",");
		}
		catch (Exception e) {
			System.out.println("[ERROR] The config.properties file requires the following fields: \n- responseToIgnore \n- ip \n- port \n- commands\n- debug\n- waitingTime");
			return;
		}

		SUL<String, String> sul = (SUL<String, String>)new FTPHandler(ip, port, debug,waitingTime);
		Collection<String> inputAlphabet = ImmutableSet.copyOf((String[])commandArray);
		System.out.println("[LOG]Learning ... ");
		BasicLearner.runControlledExperiment((SUL<String, String>)sul, BasicLearner.LearningMethod.LStar, BasicLearner.TestingMethod.RandomWalk, inputAlphabet);

	}
}