package basiclearner;

import de.learnlib.api.exception.SULException;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import de.learnlib.api.SUL;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
public class FTPHandler implements SUL<String, String>
{
	public String ip;
	public Integer port;
	String resetCommand;
	public String[] responseCodes;
	public static Socket ftpSocket;
	public static PrintWriter out;
	public static BufferedReader in;
	public static Scanner scan;
	private boolean VERBOSE;
	FTPClient ftpClient;
	private static final String CRLF = "\r\n";

	public FTPHandler(final String ip, final Integer port, final String[] responseCodes, final boolean debug, String resetCommand) {
		this.ip = ip;
		this.port = port;
		this.responseCodes = responseCodes;
		this.VERBOSE = debug;
		this.resetCommand=resetCommand;
		
		ftpClient = new FTPClient();
	}
	
	public static void createConnection(final String ip, final Integer port, final String[] responseCodes) throws IOException {
		try {
			
			FTPHandler.ftpSocket = new Socket(ip, port);
			FTPHandler.out = new PrintWriter(FTPHandler.ftpSocket.getOutputStream(), true);
			FTPHandler.in = new BufferedReader(new InputStreamReader(FTPHandler.ftpSocket.getInputStream()));
			FTPHandler.scan = new Scanner(System.in);
		}
		catch (IOException e) {
			System.out.println("Error:" + e);
		}
	}

	public void pre() {
		if (this.VERBOSE) {
			System.out.println("[DEBUG] Starting SUL, connecting via Telnet");
		}
		try {
			createConnection(this.ip, this.port, this.responseCodes);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void post() {
		if (this.VERBOSE) {
			System.out.println("[DEBUG] Shutting down SUL");
		}
		try {
			FTPHandler.out.write(resetCommand+CRLF);
			FTPHandler.out.flush();
			FTPHandler.out.close();
			FTPHandler.in.close();
			FTPHandler.ftpSocket.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String step(final String input) throws SULException {
		String output = "";
		try {
			output = this.makeTransition(input);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	private static boolean containsResponseCode(final String response, final String[] responseCodes) {
		for (final String code : responseCodes) {
			if (response.contains(code)) {
				return true;
			}
		}
		return false;
	}

	public String makeTransition(final String input) throws IOException {
		FTPHandler.out.write(String.valueOf(input) + "\r\n");
		FTPHandler.out.flush();
		String response= FTPHandler.in.readLine();
		while ( containsResponseCode(response, this.responseCodes)) {
			response = FTPHandler.in.readLine();
		}
		if (this.VERBOSE) {
			System.out.println("[DEBUG] " + input + " -> " + response.substring(0, 3) + " (" + response + ")");
		}
		return response.substring(0, 3);
	}
}