package graph;

import graph.BFS.BFSFirstTurnMapper;
import graph.BFS.BFSFirstTurnReducer;
import graph.BFS.BFSMapper;
import graph.BFS.BFSReducer;

import input.WholeFileFileInputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import util.Utils;
import util.Utils.RuntimeContext;
import writable.BFSInter;

public class DirectToUnDirect {

	public static class DirectToUnDirectMapper extends
			Mapper<Object, Text, IntWritable, Text> {
		public static Random weightRand = new Random();
		
		@Override
		protected void map(
				Object key,
				Text value,
				org.apache.hadoop.mapreduce.Mapper<Object, Text, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {
			StringTokenizer tokenizer = new StringTokenizer(value.toString(),
					":");
			int vid = Integer.parseInt(tokenizer.nextToken());
			while (tokenizer.hasMoreTokens()) {
				String edgeDef = tokenizer.nextToken();
				String parts[] = edgeDef.split(",");
				
				int weigh = weightRand.nextInt(50000);
				
				context.write(new IntWritable(vid), new Text(parts[0] + "\t"
						+ weigh));
				context.write(new IntWritable(Integer.parseInt(parts[0])),
						new Text(vid + "\t" + weigh));
			}
		}
	}

	public static class SortMapper extends
			Mapper<Object, Text, IntWritable, Text> {

		protected void map(
				Object key,
				Text value,
				org.apache.hadoop.mapreduce.Mapper<Object, Text, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {
			
			String[] parts = value.toString().split("\t", 2);
			context.write(new IntWritable(Integer.parseInt(parts[0])), new Text(parts[1]));
		};
	}
	
	public static class SortReducer extends
			Reducer<IntWritable, Text, IntWritable, Text>{
		
		@Override
		protected void reduce(IntWritable key, Iterable<Text> values,
				org.apache.hadoop.mapreduce.Reducer<IntWritable, Text, IntWritable, Text>.Context context)
				throws IOException, InterruptedException {
				
			Map<Integer, Integer> added = new HashMap<Integer, Integer>();
			
			StringBuffer sb = new StringBuffer();
			for(Text t : values){
				String[] parts = t.toString().split("\t");
				int to = Integer.parseInt(parts[0]);
				int weight = Integer.parseInt(parts[1]);
				
				Integer oriValue = added.get(to);
				if(to != key.get() && (oriValue == null || oriValue > weight)){
					added.put(to, weight);
				}
			}
			
			for(Map.Entry<Integer, Integer> e : added.entrySet()){
				sb.append(e.getKey()).append(',').append(e.getValue()).append(',').append("1:");
			}
			
			
			
			if(sb.length() > 0){
				sb.deleteCharAt(sb.length() - 1);
			}
			
			context.write(key, new Text(sb.toString()));
		}
	}

	public static void main(String[] args) {
		try {
			RuntimeContext context = Utils.initRuntimeContext(args);

			Configuration conf = new Configuration();
			Job job;
			job = new Job(conf, "direct to undirect");
			job.setJarByClass(DirectToUnDirect.class);
			job.setMapperClass(DirectToUnDirectMapper.class);
			job.setMapOutputKeyClass(IntWritable.class);
			job.setMapOutputValueClass(Text.class);

			job.setNumReduceTasks(0);

			FileInputFormat.addInputPath(job, new Path(context.basePath));
			FileOutputFormat.setOutputPath(job,
					Utils.deleteIfExists(new Path(context.getPath(0))));
			

			job.waitForCompletion(true);
			
			conf.set("mapreduce.output.textoutputformat.separator", ":");
			Job thisTurnJob = new Job(conf, "sort ");
			thisTurnJob.setJarByClass(DirectToUnDirect.class);
			thisTurnJob.setMapperClass(SortMapper.class);
			thisTurnJob.setReducerClass(SortReducer.class);
			
			thisTurnJob.setMapOutputKeyClass(IntWritable.class);
			thisTurnJob.setMapOutputValueClass(Text.class);
			thisTurnJob.setOutputKeyClass(IntWritable.class);
			thisTurnJob.setOutputValueClass(Text.class);
			
			thisTurnJob.setNumReduceTasks(1);
			
			FileInputFormat.addInputPath(thisTurnJob, new Path(context.getPath(0)));
			FileOutputFormat.setOutputPath(thisTurnJob, 
					Utils.deleteIfExists(new Path(context.getPath(1))));
			
			thisTurnJob.waitForCompletion(true);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
