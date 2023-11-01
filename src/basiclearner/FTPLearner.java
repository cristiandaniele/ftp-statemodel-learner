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

		final String currentDirectory = System.getProperty("user.dir");

		Properties properties = new Properties();
		try (FileReader fileReader = new FileReader("config.properties")) {
			properties.load(fileReader);
		} catch (IOException e) {
			System.out.println("[ERROR] Problem reading config.properties file. It should be located at " + currentDirectory);
			return;
		}

		String[] responseCodes;
		String ip;
		int port;
		boolean debug;
		String[] commandArray;
		String resetCommand;
		try {
			final String responseCodesString = properties.getProperty("responsesToIgnore");
			responseCodes = responseCodesString.split(",");
			ip = properties.getProperty("ip");
			port = Integer.parseInt(properties.getProperty("port"));
			debug = Boolean.parseBoolean(properties.getProperty("debug"));
			final String commandsString = properties.getProperty("commands");
			commandArray = commandsString.split(",");
			resetCommand = properties.getProperty("resetCommand");
		}
		catch (Exception e) {
			System.out.println("[ERROR] The config.properties file requires the following fields: \n- responseToIgnore \n- ip \n- port \n- commands\n- debug");
			return;
		}

		SUL<String, String> sul = (SUL<String, String>)new FTPHandler(ip, port, responseCodes, debug,resetCommand);
		Collection<String> inputAlphabet = ImmutableSet.copyOf((String[])commandArray);
		System.out.println("[LOG]Learning ... ");
		BasicLearner.runControlledExperiment((SUL<String, String>)sul, BasicLearner.LearningMethod.LStar, BasicLearner.TestingMethod.RandomWalk, inputAlphabet);

	}
}