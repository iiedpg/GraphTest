package writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class MST2EdgeValue implements Writable {
	private double value;

	public MST2EdgeValue() {
		super();
	}

	public MST2EdgeValue(double value) {
		super();
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}


	@Override
	public void readFields(DataInput in) throws IOException {		
		this.value = in.readDouble();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeDouble(this.value);
	}
}
