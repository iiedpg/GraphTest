package input;

import java.io.IOException;
import java.util.List;

import org.apache.giraph.edge.Edge;
import org.apache.giraph.edge.EdgeFactory;
import org.apache.giraph.io.formats.TextVertexInputFormat;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import com.google.common.collect.Lists;

public class StandardAdjVertexTextInputFormat extends
		TextVertexInputFormat<IntWritable, IntWritable, DoubleWritable> {

	@Override
	public org.apache.giraph.io.formats.TextVertexInputFormat.TextVertexReader createVertexReader(
			InputSplit split, TaskAttemptContext context) throws IOException {
		return new StandardAdjVertexReader();
	}

	public class StandardAdjVertexReader extends
			TextVertexReaderFromEachLineProcessed<String[]> {

		@Override
		protected String[] preprocessLine(Text line) throws IOException {
			String[] values = line.toString().split(":");
			return values;
		}

		@Override
		protected Iterable<Edge<IntWritable, DoubleWritable>> getEdges(
				String[] values) throws IOException {
			List<Edge<IntWritable, DoubleWritable>> edges = Lists
					.newLinkedList();
			for (int i = 1; i < values.length; ++i) {
				String[] parts = values[i].split(",");
				Edge<IntWritable, DoubleWritable> edge = EdgeFactory.create(
						new IntWritable(Integer.parseInt(parts[0])),
						new DoubleWritable(Double.parseDouble(parts[1])));
				edges.add(edge);
			}
			
			return edges;
		}

		@Override
		protected IntWritable getId(String[] id) throws IOException {
			return new IntWritable(Integer.parseInt(id[0]));
		}

		@Override
		protected IntWritable getValue(String[] arg0) throws IOException {
			return new IntWritable(0);
		}
	}

}
