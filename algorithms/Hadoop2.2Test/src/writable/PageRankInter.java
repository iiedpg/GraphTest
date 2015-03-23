package writable;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

public class PageRankInter implements Writable {
	public static final int TYPE_GRAPH_STRUCTURE = 1;
	public static final int TYPE_PR_VALUE = 2;

	private IntWritable type = new IntWritable(0);
	private List<IntWritable> neighs = new LinkedList<IntWritable>();
	private DoubleWritable value = new DoubleWritable();	
	
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

	@Override
	public void readFields(DataInput in) throws IOException {
		this.type.readFields(in);
		if(this.type.get() == TYPE_GRAPH_STRUCTURE){
			int size = in.readInt();
			this.neighs.clear();
			for(int i = 0;i < size;++i){
				IntWritable neigh = new IntWritable(0);
				neigh.readFields(in);
				this.neighs.add(neigh);
			}
		}
		else{
			this.value.readFields(in);
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		this.type.write(out);
		if(this.type.get() == TYPE_GRAPH_STRUCTURE){
			out.writeInt(this.neighs.size());
			for(IntWritable neigh : neighs){
				neigh.write(out);
			}
		}
		else{
			this.value.write(out);
		}
	}
	
	public static class Test implements java.io.Serializable{
		public double d = 0.125;
		public int aa = 14;
	}
	
	public static void main(String[] args) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		//DataOutputStream dos = new DataOutputStream(bos);
		ObjectOutputStream oss = new ObjectOutputStream(bos);
		
		long start = System.nanoTime();
		
		for(int i = 0;i < 6000000;++i){
			//PageRankInter pri = new PageRankInter();
			//pri.type.set(PageRankInter.TYPE_PR_VALUE);
			//pri.neighs.add(new IntWritable(1));
			//pri.write(dos);
			Test t = new Test();
			oss.writeObject(t);
		}
		
		System.out.println(bos.toByteArray().length);	
		
		long end = System.nanoTime();
		System.out.println((end - start));
	}
}
