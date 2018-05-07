package at.vis.fotowettbewerb;

import java.util.Collection;
import java.util.List;

public interface ProgressCallback {
	
	public void postsProcessed(int cnt);
	
	public void errorOccured(Error error, Exception ex);
	
	public void finished(Collection<Candidate> candidates, Collection<Vote> votes, List<SuspiciousLine> suspiciousLines);
	
}
