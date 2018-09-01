package ninja.essl.lcf.data;

public class LcfFile {

	private String extension;

	private LcfType type;

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}
	
	public LcfType getType() {
		return type;
	}

	public void setType(LcfType type) {
		this.type = type;
	}
}
