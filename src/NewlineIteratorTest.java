import static org.junit.Assert.*;

import org.junit.Test;


public class NewlineIteratorTest {

	@Test
	public void testNext() {
		String test = "HELLO\nWORLD\nHOW'S IT\nGOING?";
		NewlineIterator i = new NewlineIterator(test);
		String line1 = "HELLO";
		assertEquals(line1, i.next());
		String line2 = "WORLD";
		assertEquals(line2, i.next());
		String line3 = "HOW'S IT";
		assertEquals(line3, i.next());
		String line4 = "GOING?";
		assertEquals(line4, i.next());
		assertNull(i.next());
	}

}
