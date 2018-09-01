package ninja.essl.lcf.data;

import java.util.HashMap;
import java.util.Map;

public class LcfStruct {
	
	private final Map<Integer, LcfField> fields = new HashMap<>();

	public void put (int index, String name, LcfType type) {
		this.fields.put(index, new LcfField(name, type));
	}

	public void put (int index, String name, LcfType type, String struct) {
		this.fields.put(index, new LcfField(name, type, struct));
	}
	
	public void put (int index, LcfField field) {
		this.fields.put(index, field);
	}
	
	public LcfField getField(int index) {
		return this.fields.get(index);
	}	
}
