package writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class MSTEdgeValue implements Writable {
	private int targetRoot;
	private double value;

	public MSTEdgeValue() {
		super();
	}

	public MSTEdgeValue(int targetRoot, double value) {
		super();
		this.targetRoot = targetRoot;
		this.value = value;
	}

	public int getTargetRoot() {
		return targetRoot;
	}

	public void setTargetRoot(int targetRoot) {
		this.targetRoot = targetRoot;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}


	@Override
	public void readFields(DataInput in) throws IOException {
		this.targetRoot = in.readInt();
		this.value = in.readDouble();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.targetRoot);
		out.writeDouble(this.value);
	}
}
