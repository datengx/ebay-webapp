import java.security.*;
import java.security.MessageDigest;
import java.io.*;
import java.util.*;

class ComputeSHA {
	// Member field containing the file content
	String m_FileContent;
	ComputeSHA () {
		m_FileContent = "";
	}
	public int readContent(String filename) {
		FileInputStream in = null;
		StringBuilder sb = new StringBuilder();
		try {
			// Create new input stream object
			in = new FileInputStream(filename);
			int c;
			while ((c = in.read()) != -1) {
				sb.append((char)c);	
			}
			m_FileContent = sb.toString();
			return 0;
		} catch (IOException ex) {
			// throw IO error
			System.err.println("IOException: " + ex.getMessage());
		}
		return -1;
	}
	
	public String returnSHA1HexString() {
		if (m_FileContent == "") {
			System.err.println("No content loaded");
		}
		MessageDigest md = null;
		byte [] contentDigest = null;
		try {
			md = MessageDigest.getInstance("SHA");
			md.update( m_FileContent.getBytes("UTF-8") );
			contentDigest = md.digest();
		} catch ( Exception ex) {
			System.err.println("Exception: " + ex.getMessage());
		}
		// Format the Byte array into Hex String using formatter
		Formatter formatter = new Formatter();
		for (byte b : contentDigest)
		{
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
	String getFileContent() {
		return m_FileContent;
	}
	public static void main(String[] args) {
		// Make sure the input argument is correct
		if (args.length != 1) {
			System.out.println("Usage: pass in the text file name that you want to compute SHA");
		}
		ComputeSHA computeSHAObj = new ComputeSHA();
		// Read the content from the text file
		if (computeSHAObj.readContent(args[0]) != 0) {
			System.out.println("Error reading content from the file.");
		}
		// Print out the Hash value
		System.out.println(computeSHAObj.returnSHA1HexString());
		
	}
}