/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package graph;

import java.io.IOException;
import org.apache.giraph.aggregators.DoubleMaxAggregator;
import org.apache.giraph.aggregators.DoubleMinAggregator;
import org.apache.giraph.aggregators.LongSumAggregator;
import org.apache.giraph.combiner.Combiner;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.io.formats.TextVertexOutputFormat;
import org.apache.giraph.master.DefaultMasterCompute;
import org.apache.giraph.worker.WorkerContext;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.log4j.Logger;

/**
 * Demonstrates the basic Pregel PageRank implementation.
 */

public class PageRank extends
		Vertex<IntWritable, DoubleWritable, DoubleWritable, DoubleWritable> {
	/** Number of supersteps for this test */
	// public static final int MAX_SUPERSTEPS = 1;

	/** Logger */
	private static final Logger LOG = Logger.getLogger(PageRank.class);
	/** Sum aggregator name */
	private static String SUM_AGG = "sum";
	/** Min aggregator name */
	private static String MIN_AGG = "min";
	/** Max aggregator name */
	private static String MAX_AGG = "max";

	@Override
	public void compute(Iterable<DoubleWritable> messages) {
		// Enumeration<URL> resources = null;
		// try {
		// resources =
		// this.getClass().getClassLoader().getResources("log4j.properties");
		// while (resources.hasMoreElements()) {
		// URL url = (URL) resources.nextElement();
		// System.out.println(url.getPath());
		// }
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// LOG.info("in superstep " + getSuperstep() + ", total vertex = "
		// + this.getTotalNumVertices() + " >= 1  is "
		// + (getSuperstep() >= 1));

		int turn = this.getConf().getInt("nr.turns", 1);

		if (getSuperstep() >= 1) {
			double sum = 0;
			for (DoubleWritable message : messages) {
				sum += message.get();
			}
			DoubleWritable vertexValue = new DoubleWritable(
					(0.2f / getTotalNumVertices()) + 0.80f * sum);
			setValue(vertexValue);
		} else {
			setValue(new DoubleWritable(1.0f / getTotalNumVertices()));
		}

		if (getSuperstep() < turn) {
			long edges = getNumEdges();
			if (edges > 0) {
				sendMessageToAllEdges(new DoubleWritable(getValue().get()
						/ edges));
			}
		} else {
			voteToHalt();
		}
	}

	public static class SimplePageRankVertexCombiner extends
			Combiner<IntWritable, DoubleWritable> {

		@Override
		public void combine(IntWritable vid, DoubleWritable first,
				DoubleWritable second) {
			first.set(first.get() + second.get());
		}

		@Override
		public DoubleWritable createInitialMessage() {
			return new DoubleWritable(0);
		}
	}

	/**
	 * Worker context used with {@link PageRank}.
	 */
	public static class SimplePageRankVertexWorkerContext extends WorkerContext {
		/** Final max value for verification for local jobs */
		private static double FINAL_MAX;
		/** Final min value for verification for local jobs */
		private static double FINAL_MIN;
		/** Final sum value for verification for local jobs */
		private static long FINAL_SUM;

		public static double getFinalMax() {
			return FINAL_MAX;
		}

		public static double getFinalMin() {
			return FINAL_MIN;
		}

		public static long getFinalSum() {
			return FINAL_SUM;
		}

		@Override
		public void preApplication() throws InstantiationException,
				IllegalAccessException {
		}

		@Override
		public void postApplication() {
			FINAL_SUM = this.<LongWritable> getAggregatedValue(SUM_AGG).get();
			FINAL_MAX = this.<DoubleWritable> getAggregatedValue(MAX_AGG).get();
			FINAL_MIN = this.<DoubleWritable> getAggregatedValue(MIN_AGG).get();

			LOG.info("aggregatedNumVertices=" + FINAL_SUM);
			LOG.info("aggregatedMaxPageRank=" + FINAL_MAX);
			LOG.info("aggregatedMinPageRank=" + FINAL_MIN);
		}

		@Override
		public void preSuperstep() {
			if (getSuperstep() >= 3) {
				LOG.info("aggregatedNumVertices=" + getAggregatedValue(SUM_AGG)
						+ " NumVertices=" + getTotalNumVertices());
				if (this.<LongWritable> getAggregatedValue(SUM_AGG).get() != getTotalNumVertices()) {
					throw new RuntimeException("wrong value of SumAggreg: "
							+ getAggregatedValue(SUM_AGG) + ", should be: "
							+ getTotalNumVertices());
				}
				DoubleWritable maxPagerank = getAggregatedValue(MAX_AGG);
				LOG.info("aggregatedMaxPageRank=" + maxPagerank.get());
				DoubleWritable minPagerank = getAggregatedValue(MIN_AGG);
				LOG.info("aggregatedMinPageRank=" + minPagerank.get());
			}
		}

		@Override
		public void postSuperstep() {

		}
	}

	/**
	 * Master compute associated with {@link PageRank}. It registers required
	 * aggregators.
	 */
	public static class SimplePageRankVertexMasterCompute extends
			DefaultMasterCompute {
		@Override
		public void initialize() throws InstantiationException,
				IllegalAccessException {
			registerAggregator(SUM_AGG, LongSumAggregator.class);
			registerPersistentAggregator(MIN_AGG, DoubleMinAggregator.class);
			registerPersistentAggregator(MAX_AGG, DoubleMaxAggregator.class);
		}
	}

	/**
	 * Simple VertexOutputFormat that supports {@link PageRank}
	 */
	public static class SimplePageRankVertexOutputFormat extends
			TextVertexOutputFormat<LongWritable, DoubleWritable, FloatWritable> {
		@Override
		public TextVertexWriter createVertexWriter(TaskAttemptContext context)
				throws IOException, InterruptedException {
			return new SimplePageRankVertexWriter();
		}

		/**
		 * Simple VertexWriter that supports {@link PageRank}
		 */
		public class SimplePageRankVertexWriter extends TextVertexWriter {
			@Override
			public void writeVertex(
					Vertex<LongWritable, DoubleWritable, FloatWritable, ?> vertex)
					throws IOException, InterruptedException {
				getRecordWriter().write(new Text(vertex.getId().toString()),
						new Text(vertex.getValue().toString()));
			}
		}
	}
}
