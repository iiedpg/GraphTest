package writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class EdgeInfoWritable implements WritableComparable<EdgeInfoWritable> {
	private int from;
	private int to;

	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public int getTo() {
		return to;
	}

	public void setTo(int to) {
		this.to = to;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.from = in.readInt();
		this.to = in.readInt();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.from);
		out.writeInt(this.to);
	}

	@Override
	public int compareTo(EdgeInfoWritable o) {
		if(this.from != o.from){
			return this.from - o.from;
		}
		
		return this.to - o.to;
	}
}
