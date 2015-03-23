package util;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class Utils {
	private static FileSystem fs;
	static{
		Configuration conf = new Configuration();
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static class RuntimeContext{
		public String basePath;
		public String outPath;
		
		public int nrSplit;
		public int turn;
		
		public String getPath(int turn){
			return outPath + "/" + turn;
		}
	}
	
	public static FileSystem getFs(){
		return fs;
	}
	
	public static Path deleteIfExists(Path path) throws Exception{
		if(fs.exists(path)){
			fs.delete(path, true);
		}
		
		return path;
	}
	
	public static double unsignedMin(double first, double second){
		if(first < 0){
			return second;
		}		
		
		if(second < 0){
			return first;
		}
		
		return first <= second ? first : second;
	}
	
	public static boolean isMinThan(double first, double second){
		if(first < 0){
			return false;			
		}
		
		if(second < 0){
			return true;
		}
		
		return first < second;
	}
	
	public static RuntimeContext initRuntimeContext(String args[]){
		RuntimeContext context = new RuntimeContext();
		context.basePath = args[0];
		context.outPath = args[1];
		
		context.nrSplit = Integer.parseInt(args[2]);
		context.turn = Integer.parseInt(args[3]);
		
		return context;
	}
	
	public static void main(String[] args) {
		System.out.println(unsignedMin(-1, -1));
		System.out.println(unsignedMin(-1, 2));
		System.out.println(unsignedMin(3, -1));
		System.out.println(unsignedMin(3, 4));
		System.out.println(unsignedMin(4, 3));
	}
}
