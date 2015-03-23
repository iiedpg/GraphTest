package main;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;

import java.io.IOException;

public class Transformer {

	public void transform(String basename) throws Exception {
		ImmutableGraph graph = ImmutableGraph.load(basename);
		NodeIterator ni = graph.nodeIterator();
		int nodeNum = graph.numNodes();

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < nodeNum; ++i) {
			if (sb.length() > 0) {
				sb.delete(0, sb.length());
			}

			int nodeId = ni.nextInt();
			sb.append(nodeId).append(':');
			LazyIntIterator ei = graph.successors(nodeId);
			int de = graph.outdegree(nodeId);
			for (int d = de; d > 0; --d) {
				int succ = ei.nextInt();
				sb.append(succ).append(",1,1:");
			}

			if (de > 0) {
				sb.deleteCharAt(sb.length() - 1);
			}

			System.out.println(sb.toString());
		}
	}

	public static void main(String args[]) {
		Transformer transformer = new Transformer();
		try {
			transformer
					.transform(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

