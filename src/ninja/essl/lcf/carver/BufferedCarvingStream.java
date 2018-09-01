package ninja.essl.lcf.carver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BufferedCarvingStream {
	
	static final int BUFFER_SIZE = Integer.MAX_VALUE / 2;
	
	private InputStream in;
	private boolean isCarving = false;
	private OutputStream carvingOut;

	private byte[] buffer = new byte[BUFFER_SIZE];
	private int bufferPosition = 0, bufferCount = 0;

	protected long streamPosition = 0, skippedLength = 0, maxLength = 0;
	protected long maxFileSize = 0, processFileSize = 0;
	
	public BufferedCarvingStream(InputStream in, long maxFileSize) {
		this.in = in;
		this.maxFileSize = maxFileSize;
	}
	
	public void skip(long amount) throws IOException {
		this.in.skip(amount);
		
		this.skippedLength += amount;
	}
	
	public void setMaxLength(long amount) throws IOException {
		this.maxLength = amount;
	}
	
	public void enterCarvingMode(OutputStream out) {
		this.isCarving = true;
		this.processFileSize = 0;
		
		this.carvingOut = out;
	}
	
	public void leaveCarvingMode() {
		this.isCarving = false;
		this.carvingOut = null;
	}
	
	public int readByte() throws IOException {
		
		if (this.streamPosition > this.maxLength)
			return -1;
		
		if (this.bufferPosition == this.bufferCount) {		
			this.bufferCount = this.in.read(this.buffer, 0, BUFFER_SIZE);
			this.bufferPosition = 0;
			
			if (this.bufferCount == 0)
				return -1;
		}
		
		if (this.processFileSize > maxFileSize) {
			throw new IOException("File exceeded maximum length!");
		}

		int b = this.buffer[this.bufferPosition++] & 0xFF;
		this.streamPosition++;
		
		if (this.isCarving) {
			this.carvingOut.write(b);
			this.processFileSize++;
		}
		
		return b;
	}
	
	public long getRelativeStreamPosition() {
		return this.streamPosition;
	}
	
	public long getAbsoluteStreamPosition() {
		return this.streamPosition + this.skippedLength;
	}
}