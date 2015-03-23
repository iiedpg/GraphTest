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
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.log4j.Logger;

/**
 * Demonstrates the basic Pregel PageRank implementation.
 */
public class Standard extends
		Vertex<IntWritable, IntWritable, DoubleWritable, IntWritable> {

	@Override
	public void compute(Iterable<IntWritable> messages) throws IOException {
		int turn = this.getConf().getInt("nr.turns", 1);
		
		if (getSuperstep() >= 1) {
			int sum = 0;
			
			for(IntWritable i : messages){
				sum += i.get();
			}
			
			setValue(new IntWritable(sum));
		}
		
		if (getSuperstep() < turn) {
			long edges = getNumEdges();
			if (edges > 0) {
				sendMessageToAllEdges(getId());
			}
		} else {
			voteToHalt();
		}
	}
	
}
