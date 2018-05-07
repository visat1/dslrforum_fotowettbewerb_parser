package at.vis.fotowettbewerb;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class Candidate {
	public String nick;
	public int userId;
	public int picPostID;
	public URL picURL;
	public int uniquePicPostId;
	public boolean votesIgnored;
	
	public List<Vote> receivedVotes = new LinkedList<Vote>();
	public List<Vote> spentVotes = new LinkedList<Vote>();
	
	public int getSpentVoteSum() {
		return countVotes(spentVotes);
	}
	public int getReceivedVoteSum() {
		return countVotes(receivedVotes);
	}
	
	public void ignoreSpentVotes() {
		for(Vote v : spentVotes) {
			v.ignore = true;
		}
		votesIgnored = true;
	}
	
	public static final int countVotes(List<Vote> voteList) {
		int sum = 0;
		if(voteList != null) {
			for(Vote v : voteList) {
				if(v.ignore) continue;
				sum += v.weight;
			}
		}
		return sum;
	}
}
