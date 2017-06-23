import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A wrapper class that contains two doubles
 * @author dylan
 *
 */
public class DoublePair extends Object implements Serializable {
	// The two doubles stored in the class
	Double a;
	Double b;

	public DoublePair(Double a, Double b) {
		this.a = a;
		this.b = b;
	}

	// custom serialization
	public void writeObject(ObjectOutputStream o) throws IOException {
		o.writeDouble(a);
		o.writeDouble(b);
	}

	// custom serialization
	public void readObject(ObjectInputStream i) throws IOException {
		a = i.readDouble();
		b = i.readDouble();
	}

	// setters
	public void setFirst(Double newA) {
		this.a = newA;
	}

	public void setSecond(Double newB) {
		this.b = newB;
	}
	
	// getters
	public Double getFirst() {
		return this.a;
	}

	public Double getSecond() {
		return this.b;
	}

	@Override
	public boolean equals(Object d) {
		// is i an IntPair?
		if (!this.getClass().isInstance(d))
			return false;
		DoublePair dp = (DoublePair) d;
		if ((this.a.equals(dp.getFirst())) && (this.b.equals(dp.getSecond()))) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "(" + this.a.toString() + ", " + this.b.toString() + ")";
	}
}
