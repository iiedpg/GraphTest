package graph;

import input.SSSPAdjVertexTextInputFormat;
import java.io.IOException;

import org.apache.giraph.combiner.Combiner;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.io.formats.TextVertexOutputFormat;
import org.apache.giraph.worker.WorkerContext;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import writable.SSSPVertexValue;

public class SSSP extends
		Vertex<IntWritable, SSSPVertexValue, DoubleWritable, SSSPVertexValue> {

	private static final int START_VERTEX = 0;

	@Override
	public void compute(Iterable<SSSPVertexValue> messages) throws IOException {
		long superstep = getSuperstep();

		if (superstep == 0) {
			if (getId().get() == START_VERTEX) {
				getValue().setWeigh(0);
				getValue().setParent(START_VERTEX);
				
				for (Edge<IntWritable, DoubleWritable> edge : getEdges()) {
					SSSPVertexValue newValue = new SSSPVertexValue(
							START_VERTEX, edge.getValue().get());
					sendMessage(edge.getTargetVertexId(), newValue);
				}
			}
		} else {
			boolean changed = false;
			for (SSSPVertexValue mess : messages) {
				if (mess.getWeigh() < this.getValue().getWeigh()) {
					this.getValue().setWeigh(mess.getWeigh());
					this.getValue().setParent(mess.getParent());

					changed = true;
				}				
			}
			
			if (changed) {
				for (Edge<IntWritable, DoubleWritable> edge : getEdges()) {
					SSSPVertexValue newValue = new SSSPVertexValue(getId()
							.get(), getValue().getWeigh()
							+ edge.getValue().get());
					sendMessage(edge.getTargetVertexId(), newValue);
				}
			}
		}

		voteToHalt();
	}

	public static class SimpleSSSPVertexCombiner extends
			Combiner<IntWritable, SSSPVertexValue> {

		@Override
		public void combine(IntWritable vid, SSSPVertexValue first,
				SSSPVertexValue second) {
			if (second.getWeigh() < first.getWeigh()) {
				first.setWeigh(second.getWeigh());
				first.setParent(second.getParent());
			}
		}

		@Override
		public SSSPVertexValue createInitialMessage() {
			return new SSSPVertexValue(-1, Double.MAX_VALUE);
		}
	}

	public static class SimpleSSSPVertexOutputFormat
			extends
			TextVertexOutputFormat<IntWritable, SSSPVertexValue, DoubleWritable> {
		@Override
		public TextVertexWriter createVertexWriter(TaskAttemptContext context)
				throws IOException, InterruptedException {
			return new SimpleSSSPVertexWriter();
		}

		/**
		 * Simple VertexWriter that supports {@link PageRank}
		 */
		public class SimpleSSSPVertexWriter extends TextVertexWriter {
			@Override
			public void writeVertex(
					Vertex<IntWritable, SSSPVertexValue, DoubleWritable, ?> vertex)
					throws IOException, InterruptedException {
				// TODO Auto-generated method stub
				getRecordWriter().write(
						new Text(vertex.getId().toString()),
						new Text(vertex.getValue().getWeigh() + " : "
								+ vertex.getValue().getParent()));
			}
		}
	}
}
