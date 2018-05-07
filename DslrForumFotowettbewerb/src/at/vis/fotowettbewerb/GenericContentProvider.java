package at.vis.fotowettbewerb;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class GenericContentProvider implements IStructuredContentProvider {

    Object[] elements;
    public Object[] getElements( final Object inputElement ) {
      return elements;
    }

    @SuppressWarnings("rawtypes")
	public void inputChanged( final Viewer viewer,
                              final Object oldInput,
                              final Object newInput )
    {
      if( newInput == null ) {
        elements = new Object[ 0 ];
      } else {
        java.util.List elementsList = ( java.util.List )newInput;
        elements = elementsList.toArray();
      }
    }

    public void dispose() {
      // do nothing
    }
}
