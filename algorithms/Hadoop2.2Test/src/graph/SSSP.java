package graph;

import input.WholeFileFileInputFormat;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.gaoyun.TimeRecorder;
import org.apache.hadoop.gaoyun.TimeRecorder.Record;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import util.Utils;
import util.Utils.RuntimeContext;
import writable.SSSPInter;

public class SSSP {
	private static final String COUNTER_GROUP = "user";
	private static final String COUNTER_NR_VERTEX = "nr_vertex";
	
	private static final String COUNTER_CHANGED = "changed_vertex";
	
	private static final String CONF_TURN = "turn";
	
	private static final int ROOT_ID = 1;

	public static class SSSPFirstTurnMapper extends
			Mapper<Object, Text, IntWritable, Text> {
		
		@Override
		protected void map(
				Object key,
				Text value,
				org.apache.hadoop.mapreduce.Mapper<Object, Text, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {
			Counter vertexCounter = context.getCounter(COUNTER_GROUP, COUNTER_NR_VERTEX);
			vertexCounter.increment(1);

			StringTokenizer tokenizer = new StringTokenizer(value.toString(),
					":");

			int vid = Integer.parseInt(tokenizer.nextToken());
			
			StringBuffer sb = new StringBuffer();

			// last value & current value & parent
			if(vid != ROOT_ID){
				sb.append(-1).append(":").append(-1).append(":").append(-1).append(":");
			}
			else{
				sb.append(0).append(":").append(0).append(":").append(0).append(":");
			}

			while (tokenizer.hasMoreTokens()) {
				String edgeDef = tokenizer.nextToken();
				String parts[] = edgeDef.split(",");
				sb.append(parts[0]).append(",").append(parts[1]).append(":");
			}

			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
			}

			context.write(new IntWritable(vid), new Text(sb.toString()));
		}
	}

	public static class SSSPFirstTurnReducer extends
			Reducer<IntWritable, Text, IntWritable, Text> {
		@Override
		protected void reduce(
				IntWritable key,
				Iterable<Text> values,
				org.apache.hadoop.mapreduce.Reducer<IntWritable, Text, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {
			Iterator<Text> iter = values.iterator();
			Text onlyValue = iter.next();

			context.write(key, onlyValue);
		}
	}

	public static class SSSPMapper extends
			Mapper<Object, Text, IntWritable, SSSPInter> {
		
		private int turn;

		@Override
		protected void setup(org.apache.hadoop.mapreduce.Mapper.Context context)
				throws IOException, InterruptedException {
			turn = context.getConfiguration().getInt(CONF_TURN, 0);
		}

		@Override
		protected void map(
				Object key,
				Text value,
				org.apache.hadoop.mapreduce.Mapper<Object, Text, IntWritable, SSSPInter>.Context context)
				throws IOException, InterruptedException {

			SSSPInter graphStructureValue = new SSSPInter();
			graphStructureValue.getType().set(
					SSSPInter.TYPE_GRAPH_STRUCTURE);

			// input format: key\t<last value>:<current value>:parent:<edge1>,<weight1>:...
			StringTokenizer tokenizer = new StringTokenizer(value.toString(),
					",\t:");
			int vid = Integer.parseInt(tokenizer.nextToken());
			
			double lastValue = Double.parseDouble(tokenizer.nextToken());
			double currentValue = Double.parseDouble(tokenizer.nextToken());
			int parent = Integer.parseInt(tokenizer.nextToken());

			while (tokenizer.hasMoreTokens()) {
				int edgeTo = Integer.parseInt(tokenizer.nextToken());
				double weigh = Double.parseDouble(tokenizer.nextToken());
								
				graphStructureValue.getNeighs().add(new IntWritable(edgeTo));
				graphStructureValue.getNeiWeights().add(new DoubleWritable(weigh));
			}
			
			graphStructureValue.getLastValue().set(lastValue);
			graphStructureValue.getValue().set(currentValue);
			graphStructureValue.getParent().set(parent);
			
			boolean sendMessage = (turn == 1 && currentValue >= 0) || Utils.isMinThan(currentValue, lastValue);
			
			if(sendMessage){
				for(int i = 0;i < graphStructureValue.getNeighs().size();++i){
					IntWritable neigh = graphStructureValue.getNeighs().get(i);
					double weight = graphStructureValue.getNeiWeights().get(i).get();
					
					SSSPInter si = new SSSPInter();
					si.getType().set(SSSPInter.TYPE_SSSP_VALUE);
					si.getValue().set(currentValue + weight);
					si.getParent().set(vid);					
					
					context.write(neigh, si);
				}
				
				graphStructureValue.getLastValue().set(graphStructureValue.getValue().get());
			}
			
			context.write(new IntWritable(vid), graphStructureValue);
		}
	}
	
	
	public static class SSSPCombiner extends 
			Reducer<IntWritable, SSSPInter, IntWritable, SSSPInter>{
		@Override
		protected void reduce(IntWritable key, Iterable<SSSPInter> values,
				org.apache.hadoop.mapreduce.Reducer<IntWritable, SSSPInter, IntWritable, SSSPInter>.Context context)
				throws IOException, InterruptedException {
			
			SSSPInter newValue = new SSSPInter();
			newValue.getType().set(SSSPInter.TYPE_SSSP_VALUE);
			newValue.getLastValue().set(-1);
			newValue.getValue().set(-1);
			newValue.getParent().set(-1);
			
			for(SSSPInter inter : values){
				if(inter.getType().get() == SSSPInter.TYPE_GRAPH_STRUCTURE){
					context.write(key, inter);
				}
				else{
					if(Utils.isMinThan(inter.getValue().get(), newValue.getValue().get())){
						newValue.setLastValue(inter.getLastValue());
						newValue.setValue(inter.getValue());
						newValue.setParent(inter.getParent());
					}
				}
			}
			
			context.write(key, newValue);
		}		
		
	}

	public static class SSSPReducer extends
			Reducer<IntWritable, SSSPInter, IntWritable, Text> {

		@Override
		protected void setup(
				org.apache.hadoop.mapreduce.Reducer<IntWritable, SSSPInter, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {			
		}

		@Override
		protected void reduce(
				IntWritable key,
				Iterable<SSSPInter> values,
				org.apache.hadoop.mapreduce.Reducer<IntWritable, SSSPInter, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {

			StringBuffer sb = new StringBuffer();

			double newValue = -1;
			int newParent = -1;
			
			double lastValue = -1;
			double lastLastValue = -1;
			int lastParent = -1;

			for (SSSPInter value : values) {
				if (value.getType().get() == SSSPInter.TYPE_GRAPH_STRUCTURE) {
					for(int i = 0;i < value.getNeighs().size(); ++i){
						IntWritable neigh = value.getNeighs().get(i);
						DoubleWritable neiWeight = value.getNeiWeights().get(i);
						
						sb.append(neigh.get()).append(",").append(neiWeight.get()).append(":");
					}
					
					lastValue = value.getValue().get();
					lastLastValue = value.getLastValue().get();
					lastParent = value.getParent().get();
					
				} else {
					if(Utils.isMinThan(value.getValue().get(), newValue)){
						newValue = value.getValue().get();
						newParent = value.getParent().get();
					}
				}
			}

			
			if(Utils.isMinThan(newValue, lastValue)){
				sb.insert(0, lastValue + ":" + newValue + ":" + newParent + ":");
				Counter vertexCounter = context.getCounter(COUNTER_GROUP, COUNTER_CHANGED);
				vertexCounter.increment(1);
			}
			else{
				sb.insert(0, lastLastValue + ":" + lastValue + ":" + lastParent + ":");
			}
			
			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
			}

			context.write(key, new Text(sb.toString()));
		}
	}

	public static void main(String[] args) throws Exception{
		RuntimeContext context = Utils.initRuntimeContext(args);

		Configuration conf = new Configuration();
		Job job;
		job = new Job(conf, "SSSP Partition");
		job.setJarByClass(SSSP.class);
		job.setMapperClass(SSSPFirstTurnMapper.class);
		job.setReducerClass(SSSPFirstTurnReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(context.nrSplit);	
		
		FileInputFormat.addInputPath(job, new Path(context.basePath));
		FileOutputFormat.setOutputPath(job, 
				Utils.deleteIfExists(new Path(context.getPath(0))));
		
		System.out.println("before split");
		job.waitForCompletion(true);
				
		long nrVertex = 0;
		Counters counters = job.getCounters();
		CounterGroup cg = counters.getGroup(COUNTER_GROUP);
		for(Counter c : cg){
			if(c.getName().equals(COUNTER_NR_VERTEX)){
				nrVertex = c.getValue();
				break;
			}
		}
		
		if(nrVertex <= 0){
			throw new RuntimeException("vertex = 0");
		}
		
		System.out.println("after split");
		
		long jobStart = System.currentTimeMillis();
		
		
		for(int i = 1;true;++i){
			long turnStart = System.currentTimeMillis();
			
			Configuration prConf = new Configuration();
			prConf.setInt(CONF_TURN, i);
			
			Job thisTurnJob = new Job(prConf, "turn " + i);
			thisTurnJob.setJarByClass(SSSP.class);
			thisTurnJob.setMapperClass(SSSPMapper.class);
			thisTurnJob.setReducerClass(SSSPReducer.class);
			
			thisTurnJob.setMapOutputKeyClass(IntWritable.class);
			thisTurnJob.setMapOutputValueClass(SSSPInter.class);
			thisTurnJob.setOutputKeyClass(IntWritable.class);
			thisTurnJob.setOutputValueClass(Text.class);
			
			thisTurnJob.setNumReduceTasks(context.nrSplit);
			thisTurnJob.setInputFormatClass(WholeFileFileInputFormat.class);
			
			thisTurnJob.setCombinerClass(SSSPCombiner.class);					
			
			FileInputFormat.addInputPath(thisTurnJob, new Path(context.getPath(i - 1)));
			FileOutputFormat.setOutputPath(thisTurnJob, 
					Utils.deleteIfExists(new Path(context.getPath(i))));
			
			thisTurnJob.waitForCompletion(true);
			
			long turnEnd = System.currentTimeMillis();
			System.out.println("turn " + i + " time = " + (turnEnd - turnStart) + "ms");
			
			nrVertex = 0;
			counters = thisTurnJob.getCounters();
			cg = counters.getGroup(COUNTER_GROUP);
			for(Counter c : cg){
				if(c.getName().equals(COUNTER_CHANGED)){
					nrVertex = c.getValue();
					break;
				}
			}
			
			if(nrVertex == 0){
				break;
			}
		}

		long jobEnd = System.currentTimeMillis();
		System.out.println(" job time = " + (jobEnd - jobStart) + "ms");
	}
}