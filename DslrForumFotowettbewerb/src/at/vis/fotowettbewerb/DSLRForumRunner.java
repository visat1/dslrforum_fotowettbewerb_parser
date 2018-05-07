package at.vis.fotowettbewerb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class DSLRForumRunner implements IRunnableWithProgress {

	private static final int READCANDIDATES = 1;
	private static final int READVOTES      = 2;
//	private static final Pattern p = Pattern.compile("[\\s.\n\r]*#\\s*(\\d+)[ :-]+(\\d+)[.\n\r\\s]*");

	private String picThreadURL;
	private String rateThreadURL;
	private ProgressCallback callback;
	private List<Candidate> candidates = new LinkedList<Candidate>();
	private List<Vote> votes = new LinkedList<Vote>();
	private List<SuspiciousLine> suspiciousLines = new LinkedList<SuspiciousLine>();
	
	private IProgressMonitor progress;
	
	int currentPage = 0;
	URL currentURL = null;
	
	public DSLRForumRunner(String picThreadURL, String rateThreadURL, ProgressCallback callback) {
		this.picThreadURL = picThreadURL;
		this.rateThreadURL = rateThreadURL;
		this.callback = callback;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		this.progress = monitor;
		try {
			URL picURL  = new URL(picThreadURL);
			URL rateURL = new URL(rateThreadURL);
			
			read(picURL, READCANDIDATES);
			read(rateURL, READVOTES);
			progress.done();
			callback.finished(candidates, votes, suspiciousLines);
		} catch (MalformedURLException e) {
			callback.errorOccured(Error.MALFORMED_URL, e);
		} catch (IOException e) {
			callback.errorOccured(Error.IO_ERROR, e);
		} catch (Exception e) {
			callback.errorOccured(Error.UNKNOWN, e);
		}
	}

	private void read(URL originalURL, int mode) throws IOException, Exception {
		
		
		currentPage = 1;
		int pageCount   = 0;
		currentURL = null;
		InputStream is = null;
		
		
		do {
			currentURL = new URL(originalURL.toExternalForm()+"&page="+currentPage);
			progress.setTaskName("Lade "+currentURL.toExternalForm());
			is = currentURL.openStream();
			ByteArrayOutputStream tmp = new ByteArrayOutputStream();
			copy(is, tmp);
			is.close();
			byte[] data = tmp.toByteArray();
			String str = new String(data);
			
			int postIndex = -1;
			int offset = 0;
			
			while((postIndex = str.indexOf("<table id=\"post", offset)) != -1) {
				if(mode == READCANDIDATES) {
					processPost(str, postIndex);
				} else if(mode == READVOTES) {
					processVotes(str, postIndex);
				}
				offset = postIndex+1;
			}
			
			if(pageCount == 0) {
				int pageCountInfoOffset = str.indexOf("Seite 1 von ") + 12;
				pageCount = readInt(str, pageCountInfoOffset);
			}
			
			currentPage += 1;
		} while(currentPage <= pageCount);
		

	}


	private void processVotes(String content, int postOffset) {
		int id = readThreadPostId(content, postOffset);
		if(id == 1) return; //Skip introduction post
		
		int bigUNIndex = content.indexOf("<a class=\"bigusername\"", postOffset);
		int postStart = content.indexOf("<!-- message -->", postOffset) + 16;
		int postEnd   = content.indexOf("<!-- / message -->", postStart);
		
		int voterID = readUserID(content, bigUNIndex);
		String matchContent = content.substring(postStart, postEnd);
		//matchRegex(voterID, matchContent);
		matchDirect(voterID, matchContent);
		
	}

	private void matchDirect(int voterID, String matchContent) {
		String[] lines = matchContent.split("\n");
		for(String line : lines) {
			line = line.replaceAll("&#[\\d]*;", "");
			int startPos = line.indexOf('#');
			if(startPos > -1) {
				while(!Character.isDigit(line.charAt(startPos)) && startPos<line.length()-1) startPos++;
				String cidLine = null;
				String vidLine = null;
				int candidateId;
				int voteWeight;
				try {
					cidLine = line.substring(startPos);
					candidateId = readInt(line, startPos);
					
					while(Character.isDigit(line.charAt(startPos)) && startPos<line.length()-1) startPos++;
					while(!Character.isDigit(line.charAt(startPos)) && startPos<line.length()-1) startPos++;
	
					vidLine = line.substring(startPos);
					voteWeight = readInt(line, startPos);

					if(voteWeight > 4) {
						suspiciousLines.add(new SuspiciousLine(currentPage, currentURL, "Warnung: Mehr als 4 Punkte an Beitrag "+candidateId+" vergeben: ("+voteWeight+")"));
					}
					Vote v = new Vote();
					v.electedPicId = candidateId;
					v.weight = voteWeight;
					v.voterUserId = voterID;
					votes.add(v);
				} catch (StringIndexOutOfBoundsException ex) {
					suspiciousLines.add(new SuspiciousLine(currentPage, currentURL, line));
				} catch (Exception ex) {
					System.out.println("Parse error:");
					System.out.println("CID:"+cidLine);
					System.out.println("VID:"+vidLine);
					ex.printStackTrace();
				}
			}
		}
	}
//	
//	private void matchRegex(int voterID, String matchContent) {
//		Matcher m = p.matcher(matchContent);
//		while(m.find()) {
//			if(m.groupCount() == 2) {
//				int candidateID = Integer.parseInt(m.group(1));
//				int voteWeight  = Integer.parseInt(m.group(2));
//				Vote v = new Vote();
//				v.electedPicId = candidateID;
//				v.weight = voteWeight;
//				v.voterUserId = voterID;
//				votes.add(v);
//				System.out.println("VOTE ("+v.voterUserId+"): #"+v.electedPicId+": "+v.weight);
//			} else {
//				System.out.println("############# Could not read vote out of: "+matchContent);
//			}
//		}
//	}
	
	private int readInt(String str, int offset) {
		int numLen = 1;
		int val;
		while(Character.isDigit(str.charAt(offset+numLen))) {
			numLen+=1;
		}
		val = Integer.parseInt(str.substring(offset, offset+numLen));
		return val;
	}
	

	private void processPost(String content, int postOffset) {
		int id = readThreadPostId(content, postOffset);
		if(id == 1) return; //Skip introduction post
		
		int uniqueId = readUniquePostId(content, postOffset);
		
		int bigUNIndex = content.indexOf("<a class=\"bigusername\"", postOffset);
		int userID = readUserID(content, bigUNIndex);
		int closingSpanIndex = content.indexOf("</a>", bigUNIndex);
		String un = readStringBetween(content, bigUNIndex, closingSpanIndex);
		
		Candidate c = new Candidate();
		c.picPostID = id;
		c.nick      = un;
		c.userId = userID;
		c.uniquePicPostId = uniqueId;
		try {
			c.picURL = new URL(currentURL.toExternalForm()+"&postcount="+id);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		candidates.add(c);
	}

	private int readThreadPostId(String content, int postOffset) {
		int idStart = content.indexOf("#<a href=\"showpost.php", postOffset);
		int strongStart = content.indexOf("<strong>", idStart) + 8;
		
		int id = readInt(content, strongStart);
		return id;
	}

	private int readUniquePostId(String content, int postOffset) {
		int idStart = content.indexOf("#<a href=\"showpost.php", postOffset);
		int pStart  = content.indexOf("p=", idStart) + 2;
		
		int id = readInt(content, pStart);
		return id;
	}

	private int readUserID(String content, int bigUsernameOffset) {
		int idStart = content.indexOf("&amp;u=", bigUsernameOffset) + 7;
		if(idStart == -1) {
			idStart = content.indexOf("member.php?u=", bigUsernameOffset) + 13;
		}
		int id = readInt(content, idStart);
		return id;
	}

	

	private String readStringBetween(String content, int openIndex, int closingIndex) {
		if("</span>".equals(content.substring(closingIndex-7, closingIndex))) {
			closingIndex -= 7;
		}

		int offset = closingIndex;
		while('>' != content.charAt(offset-1)) {
			offset -= 1;
		}
		return content.substring(offset, closingIndex);
	}

	public static int copy(InputStream in, OutputStream out) throws IOException {
		return copy(in, out, 512);
	}

	public static int copy(InputStream in, OutputStream out, int bs)
			throws IOException {
		int count = 0;
		int i = 0;
		byte[] buf = new byte[bs];
		while ((i = in.read(buf)) != -1) {
			out.write(buf, 0, i);
			count += i;
		}
		return count;
	}



	
}
