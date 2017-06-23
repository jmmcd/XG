import java.util.Iterator;

/**
 * A wrapper for a body of text -- provides iterator-like support
 * @author dylan
 *
 */
public class NewlineIterator implements Iterator{
	String[] lines; 
	Integer index;
	String ret;
	
	public NewlineIterator(String text){
		if (text == null) {
			System.out.println("ERROR IN NEWLINEITERATOR: TEXT IS NULL");
			Thread.dumpStack();
		}
		lines = text.split("\\n");
		System.out.println("NewlineIterator lines: " + lines.length);
		index = -1;
	}

	public String readLine() {
		index += 1;
		if (index >= lines.length) {
			return null;
		}
		return lines[index];
	}

	public String next() {
		return readLine();
	}
	
	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		if (index > lines.length) {
			return false;
		}
		return true;
	}

	@Override
	public void remove() {
		// nothing here
	}
	
}
