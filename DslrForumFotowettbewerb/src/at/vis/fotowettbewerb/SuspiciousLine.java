package at.vis.fotowettbewerb;

import java.net.URL;

public class SuspiciousLine {
	public SuspiciousLine(int page, URL url, String line) {
		this.page = page;
		this.url = url;
		this.line = line;
	}
	public String line;
	public int page;
	public URL url;
}
