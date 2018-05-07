package at.vis.fotowettbewerb;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.program.Program;

public class OpenPicturePageAction extends Action implements IAction {

	private CandidateHolder cHolder;
	public static final String SHOWPOST_PREFIX = "http://www.dslr-forum.de/showpost.php?p=";


	public OpenPicturePageAction(CandidateHolder cHolder) {
		setText("Bildeintrag šffnen");
		
		this.cHolder = cHolder;
	}
	
	@Override
	public void run() {
		Candidate c = cHolder.getCurrentCandidate();
		if(c != null) {
			Program.launch(SHOWPOST_PREFIX+c.uniquePicPostId);
		}
	}
	
	
}
