import org.jgrapht.ext.VertexNameProvider;


/**
 * Our own implementation of org.jgrapht.ext.IntegerNameProvider<V>
 * @author dylan
 *
 */
public class MusicGraphIntegerNameProvider implements VertexNameProvider<MusicNode>{

	@Override
	public String getVertexName(MusicNode node) {
		// node is a MusicNode -- get id
		MusicNode nd = (MusicNode) node;
		return String.valueOf(nd.id);
	}
}
