
/** generates ids for each graph
 * 
 * @author dylan
 *
 */
public class GraphIDGenerator {
	Integer val;
	Integer retval;
	
	public GraphIDGenerator() {
		val = 100; // arbitrary starting id
	}
	
	public Integer fetchID() {
		retval = val;
		val += 1;
		return retval;
	}
	
}
