package at.vis.fotowettbewerb;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class View extends ViewPart implements SelectionListener, ProgressCallback, CandidateHolder {
	public static final String ID = "DslrForumFotowettbewerb.view";

	private static final int COLUMNCOUNT = 5;
	
	public static final int COLUMN_USERNAME = 0;
	public static final int COLUMN_PIC_ID = 1;
	public static final int COLUMN_SENT_VOTES = 2;
	public static final int COLUMN_RECEIVED_VOTES = 3;

	private Button startButton;
	private TableViewer viewer;
	private Text picThreadText;
	private Text voteThreadText;
	private Text infoArea;
	private OpenPicturePageAction openPicturePageAction;
	
	private Candidate selectedCandidate;
	private int sortCol = COLUMN_RECEIVED_VOTES;
	private int sortDir = 1;
	
	/**
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			if (parent instanceof Object[]) {
				return (Object[]) parent;
			}
	        return new Object[0];
		}
	}

	class CandidateSorter extends ViewerSorter {
		public int compare(Viewer viewer, Object e1, Object e2) {
			Candidate c1 = (Candidate)e1;
			Candidate c2 = (Candidate)e2;
			switch(sortCol) {
			case COLUMN_USERNAME:
				return c1.nick.compareTo(c2.nick)*sortDir;
			case COLUMN_PIC_ID:
				return (c2.picPostID-c1.picPostID)*sortDir;
			case COLUMN_RECEIVED_VOTES:
				return (c2.getReceivedVoteSum()-c1.getReceivedVoteSum())*sortDir;
			case COLUMN_SENT_VOTES:
				return (c2.getSpentVoteSum()-c1.getSpentVoteSum())*sortDir;
			}
			return 0;
		}
	}
	
	
	
	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(2,false));

		Composite head = new Composite(parent, SWT.NONE);
		head.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		head.setLayout(new GridLayout(2,false));
		
		Label l = new Label(head, SWT.NONE);
		l.setText("Beiträge");
		picThreadText = new Text(head, SWT.SINGLE | SWT.BORDER);
		picThreadText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		picThreadText.setText("https://www.dslr-forum.de/showthread.php?t=841200");
		
		l = new Label(head, SWT.NONE);
		l.setText("Bewertungen");
		voteThreadText = new Text(head, SWT.SINGLE | SWT.BORDER);
		voteThreadText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		voteThreadText.setText("https://www.dslr-forum.de/showthread.php?t=858972");
		
		Composite c = new Composite(head, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		gd.heightHint = 40;
		gd.horizontalSpan=2;
		c.setLayoutData(gd);
		c.setLayout(new GridLayout(1, false));
		
		Button b = new Button(c, SWT.PUSH);
		b.setText("Start");
		b.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		b.addSelectionListener(this);
		startButton = b;
		
		infoArea = new Text(head, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan=2;
		infoArea.setLayoutData(gd);
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new GenericContentProvider());
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.setUseHashlookup(true);
		viewer.setSorter(new CandidateSorter());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				if(sel.isEmpty()) {
					selectedCandidate = null;
					openPicturePageAction.setEnabled(false);
				} else {
					selectedCandidate = (Candidate)sel.getFirstElement();
					openPicturePageAction.setEnabled(true);
				}
			}
		});
		MenuManager m = new MenuManager();
		openPicturePageAction = new OpenPicturePageAction(this);
		openPicturePageAction.setEnabled(false);
		m.add(openPicturePageAction);

		Menu menu = m.createContextMenu(viewer.getTable());
		viewer.getTable().setMenu(menu);

		for(int i = 0;i<COLUMNCOUNT;i++) {
			final int currCol = i;
			TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
			col.getColumn().setMoveable(false);
			col.getColumn().setResizable(true);
			col.getColumn().setWidth(100);
			col.getColumn().addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if(sortCol != currCol) {
						sortCol = currCol;
						sortDir = 1;
					} else {
						sortDir *= -1; 
					}
					viewer.setSorter(new CandidateSorter());
				}
				
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
			col.setLabelProvider(new CellLabelProvider() {
				public void update(ViewerCell cell) {
					Candidate c = (Candidate)cell.getElement();
					
					switch(cell.getColumnIndex()) {
						case COLUMN_USERNAME:
							cell.setText(c.nick);
							break;
						case COLUMN_PIC_ID:
							cell.setText(Integer.toString(c.picPostID));
							break;
						case COLUMN_RECEIVED_VOTES:
							cell.setText(Integer.toString(c.getReceivedVoteSum()));
							break;
						case COLUMN_SENT_VOTES:
							cell.setText(Integer.toString(c.getSpentVoteSum()));
							break;
					}
					
					
					if(c.getSpentVoteSum() == 0) {
						cell.setBackground(cell.getControl().getShell().getDisplay().getSystemColor(SWT.COLOR_YELLOW));
					}
					if(c.votesIgnored) {
						cell.setBackground(cell.getControl().getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
					}
				}
			});
			
			switch(i) {
			case COLUMN_USERNAME:
				col.getColumn().setText("Benutzer");
				break;
			case COLUMN_PIC_ID:
				col.getColumn().setText("Post-ID");
				break;
			case COLUMN_RECEIVED_VOTES:
				col.getColumn().setText("Empfangen");
				break;
			case COLUMN_SENT_VOTES:
				col.getColumn().setText("Bewertet");
				break;
			}
		}
		
		
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if(e.getSource() == startButton) {
			IRunnableWithProgress job = new DSLRForumRunner(picThreadText.getText(), voteThreadText.getText(), this);
			ProgressMonitorDialog d = new ProgressMonitorDialog(getSite().getShell());
			try {
				d.run(true, false, job);
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

	@Override
	public void postsProcessed(int cnt) {
		
	}

	@Override
	public void errorOccured(Error error, Exception ex) {
		System.out.println("View.errorOccured(): "+error.name()+", "+ex.getLocalizedMessage());
		ex.printStackTrace();
	}

	@Override
	public void finished(final Collection<Candidate> candidates, final Collection<Vote> votes, final List<SuspiciousLine> suspiciousLines) {
		getSite().getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				applyVotes(candidates, votes, suspiciousLines);
				viewer.setInput(candidates);
				
				StringBuilder sb = new StringBuilder();
				for(SuspiciousLine l :suspiciousLines) {
					if(l.url != null) {
						sb.append(l.url.toExternalForm()+"\n");
					}
					sb.append(l.line);
					sb.append("\n\n");
				}
				infoArea.setText(sb.toString());
			}
		});
		
		
	}

	private void applyVotes(Collection<Candidate> candidates, Collection<Vote> votes, List<SuspiciousLine> suspiciousLines) {
		Set<Candidate> ignoreVoteCandidates = new HashSet<Candidate>();
		for(Candidate c : candidates) {
			for(Vote v : votes) {
				if(c.userId == v.voterUserId && c.picPostID == v.electedPicId) {
					ignoreVoteCandidates.add(c);
					SuspiciousLine l = new SuspiciousLine(0, null, "Punkt an sich selbst vergeben: "+c.nick+" (#"+c.picPostID+")");
					suspiciousLines.add(l);
				} else {
					if(v.electedPicId == c.picPostID) {
						c.receivedVotes.add(v);
					}
					if(v.voterUserId == c.userId) {
						c.spentVotes.add(v);
					}
				}
			}
		}
		for(Candidate c : ignoreVoteCandidates) {
			c.ignoreSpentVotes();
		}
	}

	@Override
	public Candidate getCurrentCandidate() {
		return selectedCandidate;
	}
}