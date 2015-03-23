package main;

import it.unimi.dsi.fastutil.io.BinIO;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class SizeTransformer {

	private final static String SCC_SUFFIX = "scc";
	private final static String SCCSIZES_SUFFIX = "sccsizes";

	public void transform(String dirname) {
		File dir = new File(dirname);
		String fname = dir.getName();

		File sccFile = FileUtils.getFile(dir.getAbsoluteFile(), fname + "."
				+ SCC_SUFFIX);
		File sccsizesFile = FileUtils.getFile(dir.getAbsoluteFile(), fname
				+ "." + SCCSIZES_SUFFIX);
		
		System.err.println("scc = " + sccFile);
		System.err.println("sccFile = " + sccsizesFile);
		
		
		try {
			int[] component = BinIO.loadInts(sccFile.getAbsolutePath());
			int[] size = BinIO.loadInts(sccsizesFile.getAbsolutePath());
			
			for(int i = 0;i < size.length;++i){
				System.out.println(i + ":" + size[i]);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SizeTransformer st = new SizeTransformer();
		st.transform(args[0]);
	}

}

