package writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

public class MST2VertexValue implements Writable {
	public static class EdgeAdded {
		public EdgeAdded() {
			super();
		}

		public EdgeAdded(int src, int dest, double value) {
			super();
			this.src = src;
			this.dest = dest;
			this.value = value;
		}

		public int src;
		public int dest;
		public double value;
	}

	private int treeRoot;

	private int pickedSrc;
	private int pickedDest;
	private int pickedRoot; //also used to store new_tree_root after notify
	private double pickedValue;

	private boolean isSuperVertex;
	private boolean isLastSuperVertex;
	private boolean gotKnowSuperVertex;

	private List<EdgeAdded> edges = new ArrayList<EdgeAdded>();

	public int getTreeRoot() {
		return treeRoot;
	}

	public void setTreeRoot(int treeRoot) {
		this.treeRoot = treeRoot;
	}

	public int getPickedSrc() {
		return pickedSrc;
	}

	public void setPickedSrc(int pickedSrc) {
		this.pickedSrc = pickedSrc;
	}

	public int getPickedDest() {
		return pickedDest;
	}

	public void setPickedDest(int pickedDest) {
		this.pickedDest = pickedDest;
	}

	public double getPickedValue() {
		return pickedValue;
	}

	public void setPickedValue(double pickedValue) {
		this.pickedValue = pickedValue;
	}

	public int getPickedRoot() {
		return pickedRoot;
	}

	public void setPickedRoot(int pickedRoot) {
		this.pickedRoot = pickedRoot;
	}

	public boolean isSuperVertex() {
		return isSuperVertex;
	}

	public void setSuperVertex(boolean isSuperVertex) {
		this.isSuperVertex = isSuperVertex;
	}

	public boolean isLastSuperVertex() {
		return isLastSuperVertex;
	}

	public void setLastSuperVertex(boolean isLastSuperVertex) {
		this.isLastSuperVertex = isLastSuperVertex;
	}

	public boolean isGotKnowSuperVertex() {
		return gotKnowSuperVertex;
	}

	public void setGotKnowSuperVertex(boolean gotKnowSuperVertex) {
		this.gotKnowSuperVertex = gotKnowSuperVertex;
	}

	public List<EdgeAdded> getEdges() {
		return edges;
	}

	public void setEdges(List<EdgeAdded> edges) {
		this.edges = edges;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.gotKnowSuperVertex = in.readBoolean();
		this.isSuperVertex = in.readBoolean();
		this.isLastSuperVertex = in.readBoolean();
		this.pickedDest = in.readInt();
		this.pickedSrc = in.readInt();
		this.pickedRoot = in.readInt();
		this.pickedValue = in.readDouble();
		this.treeRoot = in.readInt();
	
		int size = in.readInt();
		this.edges.clear();
		
		for (int i = 0; i < size; ++i) {
			EdgeAdded ea = new EdgeAdded();
			ea.src = in.readInt();
			ea.dest = in.readInt();
			ea.value = in.readDouble();

			this.edges.add(ea);
		}

	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeBoolean(this.gotKnowSuperVertex);
		out.writeBoolean(this.isSuperVertex);
		out.writeBoolean(this.isLastSuperVertex);
		out.writeInt(this.pickedDest);
		out.writeInt(this.pickedSrc);
		out.writeInt(this.pickedRoot);
		out.writeDouble(this.pickedValue);
		out.writeInt(this.treeRoot);
		
		out.writeInt(this.edges.size());
		for (EdgeAdded ea : edges) {
			out.writeInt(ea.src);
			out.writeInt(ea.dest);
			out.writeDouble(ea.value);
		}
	}
}
