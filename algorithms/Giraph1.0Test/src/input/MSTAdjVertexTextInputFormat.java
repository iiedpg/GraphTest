package input;

import java.io.IOException;

import org.apache.giraph.edge.Edge;
import org.apache.giraph.edge.EdgeFactory;
import org.apache.giraph.edge.HashMapEdges;
import org.apache.giraph.io.formats.TextVertexInputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import writable.MSTEdgeValue;
import writable.MSTVertexValue;

public class MSTAdjVertexTextInputFormat extends
		TextVertexInputFormat<IntWritable, MSTVertexValue, MSTEdgeValue> {

	@Override
	public org.apache.giraph.io.formats.TextVertexInputFormat<IntWritable, MSTVertexValue, MSTEdgeValue>.TextVertexReader createVertexReader(
			InputSplit split, TaskAttemptContext context) throws IOException {
		return new MyAdjVertexReader();
	}

	public class MyAdjVertexReader extends
			TextVertexReaderFromEachLineProcessed<String[]> {

		@Override
		protected String[] preprocessLine(Text line) throws IOException {
			String[] values = line.toString().split(":");
			return values;
		}

		@Override
		protected Iterable<Edge<IntWritable, MSTEdgeValue>> getEdges(
				String[] values) throws IOException {

			HashMapEdges<IntWritable, MSTEdgeValue> edges = new HashMapEdges<>();
			edges.initialize();

			for (int i = 1; i < values.length; ++i) {
				String[] parts = values[i].split(",");
				int toId = Integer.parseInt(parts[0]);
				Edge<IntWritable, MSTEdgeValue> edge = EdgeFactory.create(
						new IntWritable(toId),
						new MSTEdgeValue(toId, Double.parseDouble(parts[1])));
				edges.add(edge);
			}

			return edges;
		}

		@Override
		protected IntWritable getId(String[] id) throws IOException {
			return new IntWritable(Integer.parseInt(id[0]));
		}

		@Override
		protected MSTVertexValue getValue(String[] arg0) throws IOException {
			int vid = Integer.parseInt(arg0[0]);
			MSTVertexValue vv = new MSTVertexValue();
			vv.setTreeRoot(vid);
			vv.setSuperVertex(true);
			vv.setLastSuperVertex(true);
			vv.setGotKnowSuperVertex(true);
			return vv;
		}
	}

}
