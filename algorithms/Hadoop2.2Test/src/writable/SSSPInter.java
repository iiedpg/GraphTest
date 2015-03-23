package writable;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

public class SSSPInter implements Writable {
	public static final int TYPE_GRAPH_STRUCTURE = 1;
	public static final int TYPE_SSSP_VALUE = 2;

	private IntWritable type = new IntWritable(0);
	private List<IntWritable> neighs = new ArrayList<IntWritable>();
	private List<DoubleWritable> neiWeights = new ArrayList<DoubleWritable>();

	private DoubleWritable lastValue = new DoubleWritable();
	private DoubleWritable value = new DoubleWritable();
	private IntWritable parent = new IntWritable();

	public IntWritable getType() {
		return type;
	}

	public void setType(IntWritable type) {
		this.type = type;
	}

	public List<IntWritable> getNeighs() {
		return neighs;
	}

	public void setNeighs(List<IntWritable> neighs) {
		this.neighs = neighs;
	}

	public DoubleWritable getValue() {
		return value;
	}

	public void setValue(DoubleWritable value) {
		this.value = value;
	}

	public List<DoubleWritable> getNeiWeights() {
		return neiWeights;
	}

	public void setNeiWeights(List<DoubleWritable> neiWeights) {
		this.neiWeights = neiWeights;
	}

	public IntWritable getParent() {
		return parent;
	}

	public void setParent(IntWritable parent) {
		this.parent = parent;
	}

	public DoubleWritable getLastValue() {
		return lastValue;
	}

	public void setLastValue(DoubleWritable lastValue) {
		this.lastValue = lastValue;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.type.readFields(in);
		if (this.type.get() == TYPE_GRAPH_STRUCTURE) {
			int size = in.readInt();

			this.neighs.clear();
			this.neiWeights.clear();

			for (int i = 0; i < size; ++i) {
				IntWritable neigh = new IntWritable(0);
				neigh.readFields(in);
				this.neighs.add(neigh);

				DoubleWritable neiWeigh = new DoubleWritable(1);
				neiWeigh.readFields(in);
				this.neiWeights.add(neiWeigh);
			}
		}

		this.lastValue.readFields(in);
		this.value.readFields(in);
		this.parent.readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		this.type.write(out);
		if (this.type.get() == TYPE_GRAPH_STRUCTURE) {
			out.writeInt(this.neighs.size());
			for (int i = 0; i < this.neighs.size(); ++i) {
				System.out.println("i = ," + i + "neighs.size = " + neighs.size());
				IntWritable neigh = neighs.get(i);
				DoubleWritable neiWeigh = neiWeights.get(i);

				neigh.write(out);
				neiWeigh.write(out);
			}
		}
		
		this.lastValue.write(out);
		this.value.write(out);
		this.parent.write(out);
	}

	public static class Test implements java.io.Serializable {
		public double d = 0.125;
		public int aa = 14;
	}

	public static void main(String[] args) throws IOException {
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		// DataOutputStream dos = new DataOutputStream(bos);
//		ObjectOutputStream oss = new ObjectOutputStream(bos);
//
//		long start = System.nanoTime();
//
//		for (int i = 0; i < 6000000; ++i) {
//			// PageRankInter pri = new PageRankInter();
//			// pri.type.set(PageRankInter.TYPE_PR_VALUE);
//			// pri.neighs.add(new IntWritable(1));
//			// pri.write(dos);
//			Test t = new Test();
//			oss.writeObject(t);
//		}
//
//		System.out.println(bos.toByteArray().length);
//
//		long end = System.nanoTime();
//		System.out.println((end - start));
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		
		SSSPInter si = new SSSPInter();
		si.getType().set(SSSPInter.TYPE_GRAPH_STRUCTURE);
		for(int i = 0;i < 90000;++i){
			si.neighs.add(new IntWritable(i));
			si.neiWeights.add(new DoubleWritable(i));
		}
		
		si.write(dos);
		
		System.out.println(bos.toByteArray().length);
	}
}
