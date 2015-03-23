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
import writable.PageRankInter;

public class PageRank {
	private static final String COUNTER_GROUP = "user";
	private static final String COUNTER_NR_VERTEX = "nr_vertex";
	private static final String CONF_RAND_JUMP_VALUE = "rand_jump_value";
	private static final String CONF_TURN = "turn";
	private static final double NEIGH_WEIGHT = 0.8;

	private static final int MAX_TURN = 1;
	private static final int NR_SPLIT = 2;

	public static class PageRankFirstTurnMapper extends
			Mapper<Object, Text, IntWritable, Text> {		
		
		@Override
		protected void map(
				Object key,
				Text value,
				org.apache.hadoop.mapreduce.Mapper<Object, Text, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {
			Counter vertexCounter = context.getCounter("user", "nr_vertex");
			vertexCounter.increment(1);

			StringTokenizer tokenizer = new StringTokenizer(value.toString(),
					":");

			int vid = Integer.parseInt(tokenizer.nextToken());
			
			StringBuffer sb = new StringBuffer();

			// current value
			sb.append(0).append(",");

			while (tokenizer.hasMoreTokens()) {
				String edgeDef = tokenizer.nextToken();
				String parts[] = edgeDef.split(",");
				sb.append(parts[0]).append(",");
			}

			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
			}

			context.write(new IntWritable(vid), new Text(sb.toString()));
		}
	}

	public static class PageRankFirstTurnReducer extends
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

	public static class PageRankMapper extends
			Mapper<Object, Text, IntWritable, PageRankInter> {

		private double randomJumpValue = 0;
		private int turn;
		
		private Record vr = TimeRecorder.getRecord("num_vertexes");
		private Record er = TimeRecorder.getRecord("num_edges");

		@Override
		protected void setup(org.apache.hadoop.mapreduce.Mapper.Context context)
				throws IOException, InterruptedException {
			turn = context.getConfiguration().getInt(CONF_TURN, 0);
			randomJumpValue = context.getConfiguration().getDouble(
					CONF_RAND_JUMP_VALUE, 0);
		}

		@Override
		protected void map(
				Object key,
				Text value,
				org.apache.hadoop.mapreduce.Mapper<Object, Text, IntWritable, PageRankInter>.Context context)
				throws IOException, InterruptedException {

			PageRankInter graphStructureValue = new PageRankInter();
			graphStructureValue.getType().set(
					PageRankInter.TYPE_GRAPH_STRUCTURE);

			// input format: key\t<current value>,<edge1>,<edge2>...
			StringTokenizer tokenizer = new StringTokenizer(value.toString(),
					",\t");
			int vid = Integer.parseInt(tokenizer.nextToken());
			double currentValue = Double.parseDouble(tokenizer.nextToken());
			if (turn == 1) {
				currentValue = randomJumpValue;
			}

			while (tokenizer.hasMoreTokens()) {
				int edgeTo = Integer.parseInt(tokenizer.nextToken());
				graphStructureValue.getNeighs().add(new IntWritable(edgeTo));
			}

			context.write(new IntWritable(vid), graphStructureValue);

			double eachNeighPiece = currentValue
					/ graphStructureValue.getNeighs().size();

			for (IntWritable neigh : graphStructureValue.getNeighs()) {
				PageRankInter pri = new PageRankInter();
				pri.getType().set(PageRankInter.TYPE_PR_VALUE);
				pri.getValue().set(eachNeighPiece);

				context.write(neigh, pri);
			}
			
			vr.increValue(1);
			er.increValue(graphStructureValue.getNeighs().size());
		}
	}

	public static class PageRankReducer extends
			Reducer<IntWritable, PageRankInter, IntWritable, Text> {
		private double randomJumpValue = 0;

		@Override
		protected void setup(
				org.apache.hadoop.mapreduce.Reducer<IntWritable, PageRankInter, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {
			randomJumpValue = context.getConfiguration().getDouble(
					CONF_RAND_JUMP_VALUE, 0);
		}

		@Override
		protected void reduce(
				IntWritable key,
				Iterable<PageRankInter> values,
				org.apache.hadoop.mapreduce.Reducer<IntWritable, PageRankInter, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {

			StringBuffer sb = new StringBuffer();

			double newValue = 0;

			for (PageRankInter value : values) {
				if (value.getType().get() == PageRankInter.TYPE_GRAPH_STRUCTURE) {
					for (IntWritable neigh : value.getNeighs()) {
						sb.append(neigh.get()).append(',');
					}					
				} else {
					newValue += value.getValue().get();
				}
			}

			newValue = newValue * NEIGH_WEIGHT + (1 - NEIGH_WEIGHT) * randomJumpValue;

			sb.insert(0, newValue + ",");
			
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
		job = new Job(conf, "Page Rank Partition");
		job.setJarByClass(PageRank.class);
		job.setMapperClass(PageRankFirstTurnMapper.class);
		job.setReducerClass(PageRankFirstTurnReducer.class);
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
		
		double randomJumpValue = 1.0 / nrVertex;
		
		long jobStart = System.currentTimeMillis();
		
		for(int i = 1;i <= context.turn;++i){
			long turnStart = System.currentTimeMillis();
			
			Configuration prConf = new Configuration();
			prConf.setDouble(CONF_RAND_JUMP_VALUE, randomJumpValue);
			prConf.setInt(CONF_TURN, i);
			
			Job thisTurnJob = new Job(prConf, "turn " + i);
			thisTurnJob.setJarByClass(PageRank.class);
			thisTurnJob.setMapperClass(PageRankMapper.class);
			thisTurnJob.setReducerClass(PageRankReducer.class);
			
			thisTurnJob.setMapOutputKeyClass(IntWritable.class);
			thisTurnJob.setMapOutputValueClass(PageRankInter.class);
			thisTurnJob.setOutputKeyClass(IntWritable.class);
			thisTurnJob.setOutputValueClass(Text.class);
			
			thisTurnJob.setNumReduceTasks(context.nrSplit);
			thisTurnJob.setInputFormatClass(WholeFileFileInputFormat.class);
			
			FileInputFormat.addInputPath(thisTurnJob, new Path(context.getPath(i - 1)));
			FileOutputFormat.setOutputPath(thisTurnJob, 
					Utils.deleteIfExists(new Path(context.getPath(i))));
			
			thisTurnJob.waitForCompletion(true);
			
			long turnEnd = System.currentTimeMillis();
			System.out.println("turn " + i + " time = " + (turnEnd - turnStart) + "ms");
		}

		long jobEnd = System.currentTimeMillis();
		System.out.println(" job time = " + (jobEnd - jobStart) + "ms");
	}
}