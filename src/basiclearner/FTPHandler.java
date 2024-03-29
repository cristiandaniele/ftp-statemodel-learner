package basiclearner;

import de.learnlib.api.exception.SULException;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import de.learnlib.api.SUL;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
public class FTPHandler implements SUL<String, String>
{
	public String ip;
	public Integer port;
	public Integer waitingTime;
	String resetCommand="quit";
	public String[] responseCodes;
	public static Socket ftpSocket;
	public static PrintWriter out;
	public static BufferedReader in;
	public static Scanner scan;
	private boolean VERBOSE;
	private boolean timing_info;
	FTPClient ftpClient;
	public static Socket epsvSocket;
	public static Socket pasvSocket;
	private static final String CRLF = "\r\n";

	public FTPHandler(final String ip, final Integer port, final boolean verbose, final Integer waitingTime, final boolean timing_info) {
		this.ip = ip;
		this.port = port;
		this.VERBOSE = verbose;
		this.timing_info = timing_info;
		this.waitingTime=waitingTime;
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
			System.out.println("[INFO] Starting SUL, connecting via Telnet");
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
			System.out.println("[INFO] Shutting down SUL");
		}
			System.out.println("[INFO] Sending quit command");
			FTPHandler.out.write("quit"+CRLF);
			FTPHandler.out.flush();
			System.out.println("[NETWORK] Closing all the sockets");
			try {
				if(ftpSocket!=null) {
					ftpSocket.close();
				}
				if(pasvSocket!=null) {
					pasvSocket.close();
				}
					
				if(epsvSocket!=null) {
					epsvSocket.close();
				}	
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}

	private static boolean containsResponseCode(String input) {
		   // Iterate through each character in the string
		if(input==null) {
			return false;
		}
        for (int i = 0; i < input.length() - 2; i++) {
            // Check if the current and the next two characters form a sequence of three consecutive digits
            if (Character.isDigit(input.charAt(i)) &&
                Character.isDigit(input.charAt(i + 1)) &&
                Character.isDigit(input.charAt(i + 2))) {
                return true; // If a sequence of three consecutive digits is found, return true
            }
        }
        
        return false; // No sequence of three consecutive digits found in the string
    }
	
	private String readLastNonEmptyResponse(int timeoutMillis) throws IOException {
        String lastNonEmptyResponse = null;
        long startTime = System.currentTimeMillis();
        while (true) {  
            if (in.ready()) {
                // If there is data to read, read the line
                String response=in.readLine();
                
                if(response.substring(0,3).equals("150") ) {
                	if(VERBOSE) System.out.println("[INFO] Multiple lines detected");
                	response=in.readLine();
                }
                if (response != null && !response.isEmpty()) {
                    // Update the last non-empty response
                    lastNonEmptyResponse = response;
                }
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - startTime >= timeoutMillis) {
                // If the timeout has elapsed, there are different scenarios: 
            	// 1) I got the last TRACE - fine -> break
            	// 2) I'm receiving a very long response (like after the help command) -> need to reset the counter until the end of the entire blob of date
            	if( !containsResponseCode(lastNonEmptyResponse)) {
            		if(lastNonEmptyResponse==null)
            			return null;
            		startTime = System.currentTimeMillis();
            	}
            	else {
            		return lastNonEmptyResponse;
            	}
            }
            else {
            	 // Sleep for a short duration before checking again
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
	
	
	  
public int openEpsvSocket(String epsvResponse) throws UnknownHostException, IOException {
    Pattern pattern = Pattern.compile("\\|\\|\\|(\\d+)\\|");
    Matcher matcher = pattern.matcher(epsvResponse);
    if (matcher.find()) {
        try {
            int port= Integer.parseInt(matcher.group(1));
            epsvSocket = new Socket("127.0.0.1", port);
            if (this.VERBOSE) {
    			System.out.println("[NETWORK] Opening socket epsv: 127.0.0.1" + " " + port);
    		}
            return 0;
        } catch (NumberFormatException e) {
        	System.out.println("[ERROR] Problem on epsv socket");
            e.printStackTrace();
        }
    }
return 1;
}

public int openPasvSocket(String pasvResponse) throws UnknownHostException, IOException {
    Pattern pattern = Pattern.compile("\\((\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)\\)");
    Matcher matcher = pattern.matcher(pasvResponse);
    if (matcher.find()) {
        try {
            String ipAddress = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3) + "." + matcher.group(4);
            int port = Integer.parseInt(matcher.group(5)) * 256 + Integer.parseInt(matcher.group(6));
            pasvSocket=new Socket(ipAddress,port);
            if (this.VERBOSE) {
    			System.out.println("[INFO] Opening socket pasv:" + ipAddress + " " + port);
    		}
            return 0;
        } catch (NumberFormatException e) {
        	System.out.println("[ERROR] Problem on pasv socket");
            e.printStackTrace();
        }
    }
    return 1;
}

	public String makeTransition(final String input) throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		FTPHandler.out.write(String.valueOf(input) + "\r\n");
		FTPHandler.out.flush();
		String lastNonEmptyResponse = readLastNonEmptyResponse(waitingTime);
		
		while(lastNonEmptyResponse==null) {
			//the server closed the connection (because of a QUIT command for example, I need to re-open it
			createConnection(this.ip, this.port, this.responseCodes);
			FTPHandler.out.write(String.valueOf(input) + "\r\n");
			FTPHandler.out.flush();
			lastNonEmptyResponse = readLastNonEmptyResponse(waitingTime);
		}
		 long endTime = System.currentTimeMillis();
		
		if (this.VERBOSE) {
			System.out.println("[TRACE] " + input + " -> " + lastNonEmptyResponse.substring(0, 3) + " (" + lastNonEmptyResponse + ")");
		}
		
		//handling passive modes
		if((input.contains("EPSV")||input.contains("epsv"))&&lastNonEmptyResponse.substring(0,3).equals("229")) {
			openEpsvSocket(lastNonEmptyResponse);
		}
		
		if((input.contains("PASV")||input.contains("pasv"))&&lastNonEmptyResponse.substring(0,3).equals("227")) {
			openPasvSocket(lastNonEmptyResponse);
		}
		return lastNonEmptyResponse.substring(0, 3).concat(" - "+((endTime - startTime) /100)+"s");
	}
}