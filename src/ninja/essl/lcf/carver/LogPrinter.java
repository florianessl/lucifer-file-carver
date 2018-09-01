package ninja.essl.lcf.carver;

import java.io.PrintStream;

import ninja.essl.lcf.data.LcfField;

public class LogPrinter {
	PrintStream log;
	
	public LogPrinter(PrintStream log) {
		this.log = log;
	}


	public void PRINT(String format, Object... args) {
		this.log.println(String.format(format, args));
	}
	
	public void PRINT(int level, String format, Object... args) {
		for (int i = 0; i < level; i++)
			this.log.print("  ");
		this.log.println(String.format(format, args));
	}
	
	public void PRINT_FIELD(int level, LcfField field, Object value, int bufferSize) {
		PRINT(level, "%-" + (32-level*2) + "s (%02d) : %-40s  | Type: '%-6s' |", field.getName(), bufferSize, value, field.getType());
	}
	
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
