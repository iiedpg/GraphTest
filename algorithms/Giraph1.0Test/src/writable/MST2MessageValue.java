package writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.io.Writable;

public class MST2MessageValue implements Writable {
	public static enum MESSAGE_TYPE {
		NULL, MIN, QUESTION, ANSWER, QUESTION_ANSWER, NOTIFY_ASK, NOTIFY_REPLY
	}

	private MESSAGE_TYPE type;

	private int idSrc;
	private int idDest;
	private int idDestRoot;
	private double value;
	private List<Integer> mergedSrc = new LinkedList<Integer>();

	private boolean isSuperVertex;

	public MESSAGE_TYPE getType() {
		return type;
	}

	public void setType(MESSAGE_TYPE type) {
		this.type = type;
	}

	public int getIdSrc() {
		return idSrc;
	}

	public void setIdSrc(int idSrc) {
		this.idSrc = idSrc;
	}

	public int getIdDest() {
		return idDest;
	}

	public void setIdDest(int idDest) {
		this.idDest = idDest;
	}

	public int getIdDestRoot() {
		return idDestRoot;
	}

	public void setIdDestRoot(int idDestRoot) {
		this.idDestRoot = idDestRoot;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public boolean isSuperVertex() {
		return isSuperVertex;
	}

	public void setSuperVertex(boolean isSuperVertex) {
		this.isSuperVertex = isSuperVertex;
	}

	public List<Integer> getMergedSrc() {
		return mergedSrc;
	}

	public void setMergedSrc(List<Integer> mergedSrc) {
		this.mergedSrc = mergedSrc;
	}
	
	private void readMergedSrc(DataInput in) throws IOException{
		int size = in.readInt();
		for(int i = 0;i < size;++i){
			this.mergedSrc.add(in.readInt());
		}
	}
	
	private void writeMergedSrc(DataOutput out) throws IOException{
		out.writeInt(this.mergedSrc.size());
		for(int i : this.mergedSrc){
			out.writeInt(i);
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.type = MESSAGE_TYPE.values()[in.readInt()];
		//re init myself to null messgaes no matter who am init
		this.mergedSrc.clear();
		switch(type){
		case MIN:
			this.value = in.readDouble();
			this.idSrc = in.readInt();
			this.idDest = in.readInt();
			this.idDestRoot = in.readInt();
			break;
		case QUESTION:
			this.readMergedSrc(in);			
			break;
		case ANSWER:
			this.idSrc = in.readInt();
			this.isSuperVertex = in.readBoolean();
			break;
		case QUESTION_ANSWER:
			this.readMergedSrc(in);
			this.idSrc = in.readInt();
			this.isSuperVertex = in.readBoolean();
			break;
		case NOTIFY_ASK:
			this.readMergedSrc(in);
			break;
		case NOTIFY_REPLY:
			this.idDestRoot = in.readInt();
			break;		
		}
		
		//System.out.println("readed " + this);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.type.ordinal());
		switch(this.type){
		case MIN:
			out.writeDouble(this.value);
			out.writeInt(this.idSrc);
			out.writeInt(this.idDest);
			out.writeInt(this.idDestRoot);
			break;
		case QUESTION:
			this.writeMergedSrc(out);
			break;
		case ANSWER:
			out.writeInt(this.idSrc);
			out.writeBoolean(this.isSuperVertex);
			break;
		case QUESTION_ANSWER:
			this.writeMergedSrc(out);
			out.writeInt(this.idSrc);
			out.writeBoolean(this.isSuperVertex);
			break;
		case NOTIFY_ASK:
			this.writeMergedSrc(out);
			break;
		case NOTIFY_REPLY:
			out.writeInt(this.idDestRoot);
			break;
		}
		
		//System.out.println("write "  + this);
	}

	@Override
	public String toString() {
		return type + "," + this.idSrc + "->" + this.idDest + "(" + this.idDestRoot + ")" + ","
				+ this.isSuperVertex + "," + this.value + "..." + this.mergedSrc;
	}

}
