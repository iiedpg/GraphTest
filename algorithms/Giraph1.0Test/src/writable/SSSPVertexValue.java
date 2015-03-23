package writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class SSSPVertexValue implements Writable {
	private int parent;
	private double weigh;

	public SSSPVertexValue() {
		this(-1, Double.MAX_VALUE);
	}

	public SSSPVertexValue(int parent, double weigh) {
		super();
		this.parent = parent;
		this.weigh = weigh;
	}

	public int getParent() {
		return parent;
	}

	public void setParent(int parent) {
		this.parent = parent;
	}

	public double getWeigh() {
		return weigh;
	}

	public void setWeigh(double weigh) {
		this.weigh = weigh;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.parent = in.readInt();
		this.weigh = in.readDouble();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.parent);
		out.writeDouble(this.weigh);
	}

	@Override
	public String toString() {
		return weigh + " : " + parent;
	}
}