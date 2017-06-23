/**
 * A wrapper class that contains two doubles
 * @author dylan
 *
 */
public class IntPair extends Object {
	// The two doubles stored in the class
	Integer a;
	Integer b;

	public IntPair(Integer a, Integer b) {
		this.a = a;
		this.b = b;
	}
	
	// setters
	public void setFirst(Integer newA) {
		this.a = newA;
	}

	public void setSecond(Integer newB) {
		this.b = newB;
	}
	
	// getters
	public Integer getFirst() {
		return this.a;
	}

	public Integer getSecond() {
		return this.b;
	}
	
	@Override
	public boolean equals(Object i) {
		// is i an IntPair?
		if (!this.getClass().isInstance(i))
			return false;
		IntPair ip = (IntPair) i;
		if ((this.a.equals(ip.getFirst())) && (this.b.equals(ip.getSecond()))) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "(" + this.a.toString() + ", " + this.b.toString() + ")";
	}
}
