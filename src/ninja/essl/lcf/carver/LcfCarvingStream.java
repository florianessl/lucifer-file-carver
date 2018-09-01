package ninja.essl.lcf.carver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import ninja.essl.lcf.data.LcfField;
import ninja.essl.lcf.data.LcfStruct;
import ninja.essl.lcf.data.LcfType;

public class LcfCarvingStream extends BufferedCarvingStream {

	public static final long MAX_FILE_SIZE = 1024*1024*16;
	
	private LogPrinter log;
	
	public LcfCarvingStream(InputStream in, LogPrinter log) {
		super(in, MAX_FILE_SIZE);
		
		this.log = log;
	}	

	public /*Map<String, Object>[]*/ void processArrayOfStructs(boolean ignoreIllegalMethods, String structName, LcfStruct struct, int level) throws IOException, LcfParsingException {
		int length = this.readInteger();
		//Map<String, Object>[] result = new Map[length];
		
		for (int i = 0; i < length; i++) {
			int index = this.readInteger();
			log.PRINT(level, "- item %d (Index %d)", i, index);
			/*result[i] = */processSingleStruct(ignoreIllegalMethods, structName, struct, level+1);
		}
		
		//return result;
	}
	
	public /*Map<String, Object>*/ void  processSingleStruct(boolean ignoreIllegalMethods, String structName, LcfStruct struct, int level) throws IOException, LcfParsingException {
		//Map<String, Object> target = new HashMap<>();
		LcfStruct innerStruct;

		int fieldId = this.readInteger();
		while (fieldId > 0) {
			int bufferSize = this.readInteger();
			LcfField field = struct.getField(fieldId);
			
			if (field != null) {
				
				if (bufferSize == 0) {
					log.PRINT_FIELD(level, field, "NULL", bufferSize);
				} else {				
					switch (field.getType()) {
						case Bool:
							boolean bValue = this.readBoolean();
							log.PRINT_FIELD(level, field, bValue, bufferSize);
							break;
						case Integer:
							int iValue = this.readInteger();
							log.PRINT_FIELD(level, field, iValue, bufferSize);
							break;
						case String:
							String strValue = this.readString(bufferSize);
							log.PRINT_FIELD(level, field, strValue, bufferSize);
							break;
						case Binary:
							byte[] binary = this.readBinary(bufferSize);
							log.PRINT_FIELD(level, field, LogPrinter.bytesToHex(binary), bufferSize);
							break;
						case Struct:
							log.PRINT_FIELD(level, field, String.format("Struct '%s'", field.getStruct()), bufferSize);
							
							innerStruct = LcfFileCarver.knownStructs.get(field.getStruct());
							
							if (innerStruct != null) {
								processSingleStruct(ignoreIllegalMethods, field.getStruct(), innerStruct, level+1);
							} else {
								this.readBinary(bufferSize);
								log.PRINT(level+1, "## UNKNOWN STRUCT ##");
							}
							break;
						case ArrayOfStruct:
							log.PRINT_FIELD(level, field, String.format("Array of Struct '%s'", field.getStruct()), bufferSize);
							
							innerStruct = LcfFileCarver.knownStructs.get(field.getStruct());
							
							if (innerStruct != null) {
								processArrayOfStructs(ignoreIllegalMethods, field.getStruct(), innerStruct, level+1);
							} else {
								this.readBinary(bufferSize);
								log.PRINT(level+1, "## UNKNOWN STRUCT ##");
							}
							break;
						default:
							throw new LcfParsingException(String.format("Encountered unknown type '%s'", field.getType()));
					}
				}
			} else {
				if (ignoreIllegalMethods) {
					field = new LcfField("UnknownField#" + fieldId, LcfType.Unknown);
					
					byte[] binary = this.readBinary(bufferSize);
					log.PRINT_FIELD(level, field, LogPrinter.bytesToHex(binary), bufferSize);
					
					//target.put(fieldName, binary);
				} else {
					throw new LcfParsingException(String.format("Encountered unknown field '%s':%d (Hex: %X)", structName, fieldId, fieldId));
				}
			}
			
			fieldId = this.readInteger();
		}
		
		//return target;
	}
	
	private byte[] readBinary(int length) throws IOException {		
		byte[] arr = new byte[length];
		
		for (int i = 0; i < length; i++) {
			int j = this.readByte();
			if (j == -1) {
				//unexpected end of stream
				byte[] arr2 = new byte[i];
				System.arraycopy(arr, 0, arr2, 0, i);
				
				return arr2;
			}
			arr[i] = (byte)j;
		}
					
		return arr;
	}
	
	private String readString(int length) throws IOException {
		return LcfFileCarver.charset.decode(ByteBuffer.wrap(this.readBinary(length))).toString();
	}
	
	private boolean readBoolean() throws IOException {
		return this.readByte() == 0 ? false : true;
	}
	
	private int readInteger() throws IOException {
		int temp = this.readByte();
		int result = temp;
		
		while (temp > 127) {
			temp = this.readByte();
			if (temp == -1)
				return result;
			result = ((result - 128) << 7) + temp;
		}
		
		return result;
	}
}
