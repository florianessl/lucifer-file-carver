package ninja.essl.lcf.data;

public class LcfField {
	String name, struct;
	LcfType type;
	boolean multiple;

	public LcfField(String name, LcfType type) {
		this(name, type, null);
	}
	
	public LcfField(String name, LcfType type, String struct) {
		this.name = name;
		this.type = type;
		this.struct = struct;		
	}
	
	public String getName() {
		return name;
	}
	public String getStruct() {
		return struct;
	}
	public LcfType getType() {
		return type;
	}
}