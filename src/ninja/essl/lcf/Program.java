package ninja.essl.lcf;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import ninja.essl.lcf.data.LcfStruct;
import ninja.essl.lcf.data.LcfType;
import ninja.essl.lcf.carver.LcfFileCarver;
import ninja.essl.lcf.data.LcfFile;

public class Program {
	
	static Map<String, String> options = new HashMap<String, String>();
	
	static boolean getArgument(String option) {
		return options.get(option) != null;
	}
	
	static String getArgumentValue(String option) {
		String value = options.get(option);
		if (value == null || value.equals(""))
			return null;
		return value;
	}

	public static void main(String[] args) throws FileNotFoundException {

	    for (int i = 0; i < args.length; i++) {
	        switch (args[i].charAt(0)) {
		        case '-':
		            if (args[i].length() < 2)
		                throw new IllegalArgumentException("Not a valid argument: " + args[i]);
		            if (args.length - 1 > i && args[i + 1].charAt(0) != '-') {
			            options.put(args[i], args[i + 1]);
			            i++;
		            } else {
		            	options.put(args[i], "");
		            }
		            break;
		        default:
	                throw new IllegalArgumentException("Not a valid argument: " + args[i]);
	        }
	    }
	    
	    if (getArgument("-h")) {
	    	System.out.println("  -i    (REQUIRED) RAW input file");
	    	System.out.println("  -o    (REQUIRED) Output path");
	    	System.out.println("  -s    Start at specified position in file");
	    	System.out.println("  -m	Maximum length of data read");
	    	System.out.println("  -l    Log output (Default: \"-o\"/carver.log)");
	    	System.out.println("  -d    Path to custom data structure definitions");
	    	System.out.println("  -a    Ignore illegal fields");
	    	System.exit(0);
	    }
	    
	    InputStream dataStructures;
	    File inputFile = null, outputDirectory = null, logFile = null;
	    long streamPosition = 0, maxLength = -1;
	    boolean ignoreIllegalMethods = false;
	    
	    if (getArgumentValue("-i") != null) {
	    	inputFile = new File(getArgumentValue("-i"));
	    	
			if (!inputFile.exists())
				throw new FileNotFoundException(inputFile.getPath());
	    } else
	    	throw new IllegalArgumentException("No input file was specified");
	    
	    if (getArgumentValue("-o") != null) {
	    	outputDirectory = new File(getArgumentValue("-o"));
	    	
			if (!outputDirectory.exists())
				outputDirectory.mkdir();
	    } else 
	    	throw new IllegalArgumentException("No output directory was specified");

	    if (getArgumentValue("-s") != null) {
	    	streamPosition = Long.parseLong(getArgumentValue("-s"));
	    }

	    if (getArgumentValue("-m") != null) {
	    	maxLength = Long.parseLong(getArgumentValue("-m"));
	    }

	    if (getArgumentValue("-l") != null) {
	    	logFile = new File(getArgumentValue("-l"));
	    } else {
	    	logFile = new File(outputDirectory, "carver.log");
	    }

	    if (getArgumentValue("-d") != null) {
	    	dataStructures = new FileInputStream(new File(getArgumentValue("-d")));
	    } else {
	    	ClassLoader classloader = Program.class.getClassLoader();
	    	dataStructures = classloader.getResourceAsStream("data-structures.txt");
	    }

	    if (getArgument("-a")) {
	    	ignoreIllegalMethods = true;
	    }

		try {						
			loadDataStructures(dataStructures);
			
		    PrintStream logOut = new PrintStream(new FileOutputStream(logFile), true);
			
			startCarving(inputFile, outputDirectory, logOut, streamPosition, maxLength, ignoreIllegalMethods);
		} catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
			System.exit(1);
		}
	}
	
	static void loadDataStructures(InputStream in) throws IOException {		

		InputStreamReader streamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(streamReader);
		
		boolean encounteredEmptyLine = true;
		LcfStruct currentDataStructure = null;
		for (String line; (line = reader.readLine()) != null;) {
			if (line.indexOf("#") >= 0)
				line = line.substring(0, line.indexOf("#"));
			
		    if (line.trim().equals("")) {
		    	encounteredEmptyLine = true;
		    	continue;
		    }
		    
		    if (encounteredEmptyLine == true) {
		    	//expect structure name or MAIN
		    	
		    	if (!line.equals("MAIN")) {		    	
			    	currentDataStructure = new LcfStruct();
			    	String structName = line;
			    	
			    	/*if (structName.contains(":")) {
			    		String[] args = line.split(":");
			    		structName = args[0];
			    		currentDataStructure.setExtension(args[1]);
			    		if (args.length > 2 && args[2].equals("multi"))
			    			currentDataStructure.setMultiple(true);
			    	}*/
		    		LcfFileCarver.knownStructs.put(structName, currentDataStructure);
		    	}
		    	encounteredEmptyLine = false;
		    } else {
		    	//expect fields
		    	String[] args = line.trim().replace("\t",  " ").split("\\s+");
		    	
		    	if (args[0].equals("MAIN")) {
		    		if (args.length != 4)
		    			throw new RuntimeException("invalid format");
		    		
			    	LcfFile mainDataStructure = new LcfFile();
			    	mainDataStructure.setExtension(args[1]);
			    	mainDataStructure.setType(LcfType.valueOf(args[2]));
		    		LcfFileCarver.knownFileTypes.put(args[3], mainDataStructure);
		    		
		    	} else {		    	
			    	if (args.length == 3)
			    		currentDataStructure.put(Integer.parseInt(args[0]), args[1], LcfType.valueOf(args[2]));
			    	else if (args.length == 4)
			    		currentDataStructure.put(Integer.parseInt(args[0]), args[1], LcfType.valueOf(args[2]), args[3]);
			    	else
			    		throw new RuntimeException("invalid format");
		    	}
		    }
		}
	}

	static void startCarving(File inputFile, File outputDirectory, PrintStream logOut, long skip, long maxLength, boolean ignoreIllegalMethods) throws IOException {

		if (skip < 0 || skip > inputFile.length()) {
			throw new IllegalArgumentException("invalid stream position!");
		}
		
		//BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile), 8192);
		FileInputStream in = new FileInputStream(inputFile);
		
		long length = inputFile.length() - skip;
		
		if (maxLength > 0)
			length = maxLength;

		LcfFileCarver fileCarver = new LcfFileCarver(in, logOut, outputDirectory);
		
		fileCarver.carve(skip, length, ignoreIllegalMethods);
	}
}
