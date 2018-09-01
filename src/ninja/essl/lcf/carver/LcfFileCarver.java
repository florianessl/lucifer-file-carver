package ninja.essl.lcf.carver;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ninja.essl.lcf.data.LcfFile;
import ninja.essl.lcf.data.LcfStruct;
import ninja.essl.lcf.data.LcfType;

public class LcfFileCarver {
	
	static final int FIRST_BYTE_A = 0x0A,
					 FIRST_BYTE_B = 0x0B;
	static final int MIN_HEADER_SIZE = 10,
					 MAX_HEADER_SIZE = 12;
	
	public static final Charset charset = Charset.forName("ASCII");
	public static final Map<String, LcfFile> knownFileTypes = new HashMap<>();
	public static final Map<String, LcfStruct> knownStructs = new HashMap<>();
	
	private byte[] headerBuffer = new byte[MAX_HEADER_SIZE];
	private int firstByte = -1, headerPosition = -1;
	
	private final LogPrinter log;
	private final LcfCarvingStream carvingStream;
	private final File outputDirectory;

	BufferedOutputStream out;
	
	public LcfFileCarver(InputStream in, PrintStream log, File outputDirectory) {			
		this.log = new LogPrinter(log);
		this.carvingStream = new LcfCarvingStream(in, this.log);
		this.outputDirectory = outputDirectory;
	}
	
	
	public void carve(long skip, long maxLength, boolean ignoreIllegalMethods) throws IOException {
		
		this.resetHeaderBuffer();
		
		if (skip > 0)
			this.carvingStream.skip(skip);

		if (maxLength > 0)
			this.carvingStream.setMaxLength(maxLength);
		
		log.PRINT("\n\r====================================================================");
		log.PRINT("STARTED CARVING @ " + (SimpleDateFormat.getInstance().format(new Date())));
		log.PRINT("\n\r====================================================================");
		
		DecimalFormat df = new DecimalFormat("00.0");
		
		int currentByte, foundFiles = 0;
		while ((currentByte = carvingStream.readByte()) >= 0) {
			
			if (currentByte == FIRST_BYTE_A || currentByte == FIRST_BYTE_B) {
				this.resetHeaderBuffer();
				headerPosition = 0;
				firstByte = currentByte;
			} else if (headerPosition >= 0) {
				if ((currentByte >= 65 && currentByte <= 90) || (currentByte >= 97 && currentByte <= 122))
					headerBuffer[headerPosition++] = (byte)currentByte;
				else
					this.resetHeaderBuffer();
			}
			
			if (headerPosition >= MIN_HEADER_SIZE)
				if (this.checkForHeaders(ignoreIllegalMethods))
					foundFiles++;
			
			if (headerPosition == MAX_HEADER_SIZE)
				this.resetHeaderBuffer();
			
			if (this.carvingStream.getRelativeStreamPosition() % 256 == 0)
				System.out.print(String.format("\r%s%% [%Xh / %Xh] - Found: %d", 
									df.format(((1d * this.carvingStream.getRelativeStreamPosition()) / maxLength) * 100),
									this.carvingStream.getRelativeStreamPosition(), 
									maxLength,
									foundFiles));
		}
		log.PRINT("\n\r====================================================================");
		log.PRINT("DONE @ " + (SimpleDateFormat.getInstance().format(new Date())));
		log.PRINT("\n\r====================================================================");
		
		System.out.println("\n\rDONE");
	}
	
	private void resetHeaderBuffer() {
		headerPosition = -1;
		for (int i = 0; i < MAX_HEADER_SIZE; i++) {
			this.headerBuffer[i] = 0x00;
		}
	}
	
	private boolean checkForHeaders(boolean ignoreIllegalMethods) throws IOException {
		String str = charset.decode(ByteBuffer.wrap(this.headerBuffer)).toString().trim();
		
		for (String headerName : knownFileTypes.keySet()) {
			if (str.equals(headerName)) {
				
				LcfFile fileType = knownFileTypes.get(headerName);
				LcfStruct struct = knownStructs.get(headerName);
				long pos = this.carvingStream.getAbsoluteStreamPosition() - str.length() - 1;
				
				String fileName = String.format("%s_%X.%s", headerName, pos, fileType.getExtension());
				File outputFile = new File(outputDirectory, fileName);
				out = new BufferedOutputStream(new FileOutputStream(outputFile));
				
				log.PRINT("\n\r====================================================================");
				log.PRINT("Found file of type '%s' at stream position %Xh", headerName, pos);
				log.PRINT("Output file: '%s'", outputFile.getAbsolutePath());
				log.PRINT("====================================================================\n\r");
				
				out.write(firstByte);
				out.write(charset.encode(str).array());				
				this.carvingStream.enterCarvingMode(out);
				
				boolean success = false;
				
				try {
					if (fileType.getType() == LcfType.ArrayOfStruct)
						this.carvingStream.processArrayOfStructs(ignoreIllegalMethods, headerName, struct, 0);
					else if (fileType.getType() == LcfType.Struct)
						this.carvingStream.processSingleStruct(ignoreIllegalMethods, headerName, struct, 0);
					else
						throw new LcfParsingException("Unsupported file type!: " + fileType.getType());
					
					log.PRINT("\n\r====================================================================");
					log.PRINT("\n\rSuccessfully read to end of file.");

					success = true;
				} catch (LcfParsingException e) {
					log.PRINT("\n\r====================================================================");
					log.PRINT("\n\rEXCEPTION@%Xh: %s", this.carvingStream.getAbsoluteStreamPosition(), e.getMessage());
					
					success = false;
				} finally {
					log.PRINT("====================================================================\n\r");
					
					this.carvingStream.leaveCarvingMode();
					out.flush();
					out.close();
				}
				
				if (!success) {
					File renamedFile = new File(outputDirectory, fileName + "_");
					if (renamedFile.exists())
						renamedFile.delete();
					outputFile.renameTo(renamedFile);
				}
							
				this.resetHeaderBuffer();
				
				return true;
			}
		}			
		
		return false;
	}
}
