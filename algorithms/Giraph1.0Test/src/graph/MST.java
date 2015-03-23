package graph;

import java.awt.TrayIcon.MessageType;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import input.MSTAdjVertexTextInputFormat;
import org.apache.giraph.aggregators.IntMinAggregator;
import org.apache.giraph.combiner.Combiner;
import org.apache.giraph.edge.Edge;
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

import writable.MSTEdgeValue;
import writable.MSTMessageValue;
import writable.MSTMessageValue.MESSAGE_TYPE;
import writable.MSTVertexValue;
import writable.MSTVertexValue.EdgeAdded;

public class MST extends
		Vertex<IntWritable, MSTVertexValue, MSTEdgeValue, MSTMessageValue> {

	/** Logger */
	private static final Logger LOG = Logger.getLogger(PageRank.class);
	/** Sum aggregator name */
	private static final String TINY_STEP_AGG = "tiny_step";
	/** Min aggregator name */
	private static final String ACTION_AGG = "action";

	private static enum ACTIONS {
		PICKING_MIN, SUPER_VERTEX_FINDING, NOTIFY, RELABELING
	}

	private void pickingMin(Iterable<MSTMessageValue> messages)
			throws IOException {
		int tinyStep = ((IntWritable) getAggregatedValue(TINY_STEP_AGG)).get();
		// //System.out.println("tiny step = " + tinyStep);

		if (tinyStep == 0) {
			double minValue = Double.MAX_VALUE;
			int minId = Integer.MAX_VALUE;
			int minRoot = Integer.MAX_VALUE;

			if (!getValue().isSuperVertex()) {
				for (Edge<IntWritable, MSTEdgeValue> e : getEdges()) {
					if (e.getValue().getTargetRoot() == getValue()
							.getTreeRoot()) {
						continue;
					}

					if (e.getValue().getValue() < minValue
							|| (e.getValue().getValue() == minValue && e
									.getValue().getTargetRoot() < minRoot)) {
						minValue = e.getValue().getValue();
						minId = e.getTargetVertexId().get();
						minRoot = e.getValue().getTargetRoot();
					}
				}

				if (minId == Integer.MAX_VALUE) {
					voteToHalt();
					return;
				}

				MSTMessageValue minMess = new MSTMessageValue();
				minMess.setType(MESSAGE_TYPE.MIN);
				minMess.setValue(minValue);
				minMess.setIdSrc(getId().get());
				minMess.setIdDest(minId);
				minMess.setIdDestRoot(minRoot);
				sendMessage(new IntWritable(getValue().getTreeRoot()), minMess);
			}

			aggregate(ACTION_AGG,
					new IntWritable(ACTIONS.PICKING_MIN.ordinal()));
			aggregate(TINY_STEP_AGG, new IntWritable(1));

		} else {
			if (getValue().isSuperVertex()) {
				double minValue = Double.MAX_VALUE;
				int minSrc = Integer.MAX_VALUE;
				int minId = Integer.MAX_VALUE;
				int minRoot = Integer.MAX_VALUE;

				for (Edge<IntWritable, MSTEdgeValue> e : getEdges()) {
					// System.out.println("e.dest = "
					// + e.getTargetVertexId().get() + ", dest root = "
					// + e.getValue().getTargetRoot() + ", inner = "
					// + e.getValue().isInner());
					if (e.getValue().getTargetRoot() == getValue()
							.getTreeRoot()) {
						continue;
					}

					if (e.getValue().getValue() < minValue
							|| (e.getValue().getValue() == minValue && e
									.getValue().getTargetRoot() < minRoot)) {

						minValue = e.getValue().getValue();
						minSrc = getId().get();
						minId = e.getTargetVertexId().get();
						minRoot = e.getValue().getTargetRoot();
					}
				}

				for (MSTMessageValue message : messages) {
					if (message.getValue() < minValue
							|| (message.getValue() == minValue && message
									.getIdDestRoot() < minRoot)) {
						minValue = message.getValue();
						minSrc = message.getIdSrc();
						minId = message.getIdDest();
						minRoot = message.getIdDestRoot();
					}
				}

				getValue().setPickedSrc(minSrc);
				getValue().setPickedDest(minId);
				getValue().setPickedRoot(minRoot);
				getValue().setPickedValue(minValue);

				getValue().setLastSuperVertex(true);
				getValue().setGotKnowSuperVertex(false);
				getValue().setSuperVertex(false);

//				System.out.println(getId() + " picked " + minRoot
//						+ " with src = " + minSrc + ", dest = " + minId + ", value = " + minValue);

				// only last super vertex participating in master selection
				MSTMessageValue message = new MSTMessageValue();
				message.setType(MESSAGE_TYPE.QUESTION);
				// message.setIdSrc(getId().get());
				message.getMergedSrc().add(getId().get());
				sendMessage(new IntWritable(minRoot), message);
				// System.out.println("==>" + minRoot + "," + message);
			} else {
				getValue().setLastSuperVertex(false);
				getValue().setGotKnowSuperVertex(false);
				getValue().setSuperVertex(false);
			}

			aggregate(ACTION_AGG,
					new IntWritable(ACTIONS.SUPER_VERTEX_FINDING.ordinal()));
			aggregate(TINY_STEP_AGG, new IntWritable(0));
		}
	}

	private void findSuperVertex(Iterable<MSTMessageValue> messages)
			throws IOException {
		if (!getValue().isLastSuperVertex()) {// not super from last, sleep
			return;
		}

		int tinyStep = ((IntWritable) getAggregatedValue(TINY_STEP_AGG)).get();
		// //System.out.println("tiny step = " + tinyStep);
		if (tinyStep == 0) {
			for (MSTMessageValue message : messages) {
				// System.out.println(message);
				// then message.type must be question
				for (int src : message.getMergedSrc()) {
					if (src == getValue().getPickedRoot()) {
						if (getId().get() < getValue().getPickedRoot()) {
							//System.out.println(getId() + " is super vertex");
							getValue().setSuperVertex(true);
							getValue().setGotKnowSuperVertex(true);

							getValue().getEdges().add(
									new EdgeAdded(getValue().getPickedSrc(),
											getValue().getPickedDest(),
											getValue().getPickedValue()));

							getValue().setPickedRoot(getValue().getTreeRoot());

						} else {
							// System.out.println(getId() + " know "
							// + getValue().getPickedRoot()
							// + " is super vertex");
							getValue().setSuperVertex(false);
							getValue().setGotKnowSuperVertex(true);
							// getValue().setTreeRoot(getValue().getPickedRoot());
						}

						break;
					}
				}
			}

			if (!getValue().isGotKnowSuperVertex()) {
				getValue().getEdges().add(
						new EdgeAdded(getValue().getPickedSrc(), getValue()
								.getPickedDest(), getValue().getPickedValue()));
				// getValue().setTreeRoot(getValue().getPickedRoot());
				// getValue().setPickedRoot(getVal)
			}
		} else {
			List<Integer> cachedSrc = new LinkedList<Integer>();
			boolean answerReaded = getValue().isGotKnowSuperVertex(); // if we
																		// have
																		// known
																		// the
																		// super
																		// vertex,
			// we won't get answer message but we can broadcast our knowledge
			// safely

			for (MSTMessageValue message : messages) {
				// System.out.println(message);
				if (message.getType() == MESSAGE_TYPE.ANSWER
						|| message.getType() == MESSAGE_TYPE.QUESTION_ANSWER) {
					if (message.isSuperVertex()) {
						// System.out.println(getId() + "get supervertex");
						getValue().setGotKnowSuperVertex(true);
					}

					// getValue().setTreeRoot(message.getIdSrc());
					getValue().setPickedRoot(message.getIdSrc());

					// answer accumulated question
					answerReaded = true;

					MSTMessageValue answerMessage = new MSTMessageValue();
					answerMessage.setType(MESSAGE_TYPE.ANSWER);
					answerMessage.setIdSrc(getValue().getPickedRoot());
					answerMessage.setSuperVertex(getValue()
							.isGotKnowSuperVertex());
					for (int src : cachedSrc) {
						sendMessage(new IntWritable(src), answerMessage);
						// System.out.println("==> " + src + "," +
						// answerMessage);
					}
					cachedSrc.clear();
				}

				if (message.getType() == MESSAGE_TYPE.QUESTION
						|| message.getType() == MESSAGE_TYPE.QUESTION_ANSWER) {

					if (answerReaded) {
						MSTMessageValue answerMessage = new MSTMessageValue();
						answerMessage.setType(MESSAGE_TYPE.ANSWER);
						answerMessage.setIdSrc(getValue().getPickedRoot());
						answerMessage.setSuperVertex(getValue()
								.isGotKnowSuperVertex());

						for (int src : message.getMergedSrc()) {
							sendMessage(new IntWritable(src), answerMessage);
							// System.out.println("==> " + src + ","
							// + answerMessage);
						}
					} else {
						for (int src : message.getMergedSrc()) {
							cachedSrc.add(src);
						}
					}
				}
			}

			if (!answerReaded) {
				MSTMessageValue answerMessage = new MSTMessageValue();
				answerMessage.setType(MESSAGE_TYPE.ANSWER);
				answerMessage.setIdSrc(getValue().getPickedRoot());
				answerMessage.setSuperVertex(getValue().isGotKnowSuperVertex());
				for (int src : cachedSrc) {
					sendMessage(new IntWritable(src), answerMessage);
					// System.out.println("==> " + src + "," + answerMessage);
				}
				cachedSrc.clear();
			}
		}

		if (getValue().isGotKnowSuperVertex()) {
			aggregate(ACTION_AGG, new IntWritable(ACTIONS.NOTIFY.ordinal()));
			aggregate(TINY_STEP_AGG, new IntWritable(1));
		} else {
//			System.out.println("unreaded vertex i am " + getId().get() + " to "
//					+ getValue().getPickedRoot());
			MSTMessageValue askMessage = new MSTMessageValue();
			askMessage.setType(MESSAGE_TYPE.QUESTION);
			// askMessage.setIdSrc(getId().get());
			askMessage.getMergedSrc().add(getId().get());
			// sendMessage(new IntWritable(getValue().getTreeRoot()),
			// askMessage);
			sendMessage(new IntWritable(getValue().getPickedRoot()), askMessage);

			aggregate(ACTION_AGG,
					new IntWritable(ACTIONS.SUPER_VERTEX_FINDING.ordinal()));
			aggregate(TINY_STEP_AGG, new IntWritable(1));
		}
	}

	public void notify(Iterable<MSTMessageValue> messages) throws IOException {
		int tinyStep = ((IntWritable) getAggregatedValue(TINY_STEP_AGG)).get();
		// System.out.println("tiny step = " + tinyStep);
		if (tinyStep == 1) {
			if (!getValue().isLastSuperVertex()) {
				MSTMessageValue message = new MSTMessageValue();
				message.setType(MESSAGE_TYPE.NOTIFY_ASK);
				// message.setIdSrc(getId().get());
				message.getMergedSrc().add(getId().get());

				sendMessage(new IntWritable(getValue().getTreeRoot()), message);
			}

			aggregate(ACTION_AGG, new IntWritable(ACTIONS.NOTIFY.ordinal()));
			aggregate(TINY_STEP_AGG, new IntWritable(2));
		} else if (tinyStep == 2) {
			if (getValue().isLastSuperVertex()) {
				MSTMessageValue reply = new MSTMessageValue();
				reply.setType(MESSAGE_TYPE.NOTIFY_REPLY);
				// reply.setIdDestRoot(getValue().getTreeRoot());
				reply.setIdDestRoot(getValue().getPickedRoot());

				for (MSTMessageValue message : messages) {
					// System.out.println(message);
					// System.out.println(reply);

					for (int src : message.getMergedSrc()) {
						sendMessage(new IntWritable(src), reply);
					}
				}
			}

			aggregate(ACTION_AGG, new IntWritable(ACTIONS.RELABELING.ordinal()));
			aggregate(TINY_STEP_AGG, new IntWritable(0));
		}
	}

	public void relabel(Iterable<MSTMessageValue> messages) throws IOException {
		int tinyStep = ((IntWritable) getAggregatedValue(TINY_STEP_AGG)).get();
		// System.out.println("tiny step = " + tinyStep);
		if (tinyStep == 0) {
			if (!getValue().isLastSuperVertex()) {
				for (MSTMessageValue message : messages) {
					// System.out.println(message);
					// handling notify messages
					getValue().setPickedRoot(message.getIdDestRoot());
				}
			}

			for (Edge<IntWritable, MSTEdgeValue> edge : getEdges()) {
				if (edge.getValue().getTargetRoot() != getValue().getTreeRoot()) {
					MSTMessageValue reMess = new MSTMessageValue();
					reMess.setType(MESSAGE_TYPE.RELABEL);
					reMess.getMergedSrc().add(getId().get());
					reMess.getMergedSrc().add(getValue().getPickedRoot());
					// reMess.setIdSrc(getId().get());
					// reMess.setIdDestRoot(getValue().getTreeRoot());

					sendMessage(edge.getTargetVertexId(), reMess);
				} else {
					// The other side is on the same tree with me. then on the
					// next turn,
					// it will update its new root to picked root as me
					edge.getValue().setTargetRoot(getValue().getPickedRoot());
				}
			}

			aggregate(ACTION_AGG, new IntWritable(ACTIONS.RELABELING.ordinal()));
			aggregate(TINY_STEP_AGG, new IntWritable(1));
		} else {
			int nrOtherEdge = 0;

			for (MSTMessageValue message : messages) {
				Iterator<Integer> ids = message.getMergedSrc().iterator();
				while (ids.hasNext()) {
					int src = ids.next();
					int srcRoot = ids.next();

					getEdgeValue(new IntWritable(src)).setTargetRoot(srcRoot);
					if (srcRoot != getValue().getPickedRoot()) {
						++nrOtherEdge;
					}
				}
			}

			getValue().setTreeRoot(getValue().getPickedRoot());

			// System.out.println("root = " + getValue().getTreeRoot());
			for (Edge<IntWritable, MSTEdgeValue> edge : getEdges()) {
				// System.out.println("to = " + edge.getTargetVertexId()
				// + ",root = " + edge.getValue().getTargetRoot());
			}

			if (nrOtherEdge == 0) {
				voteToHalt();
			} else {
				aggregate(ACTION_AGG,
						new IntWritable(ACTIONS.PICKING_MIN.ordinal()));
				aggregate(TINY_STEP_AGG, new IntWritable(0));
			}
		}
	}

	@Override
	public void compute(Iterable<MSTMessageValue> messages) throws IOException {
		long superstep = getSuperstep();
		if (superstep == 0) {
			// getValue().setTreeRoot(getId().get());

			// if i have no neighbors, i am a connect component and i can stop
			// here now
			boolean hasEdges = false;
			for (Edge<IntWritable, MSTEdgeValue> edge : getEdges()) {
				hasEdges = true;
				break;
			}
			if (!hasEdges) {
				// say goodbye
				voteToHalt();
			}

			aggregate(TINY_STEP_AGG, new IntWritable(0));
			aggregate(ACTION_AGG,
					new IntWritable(ACTIONS.PICKING_MIN.ordinal()));
		} else {
			int actionV = ((IntWritable) getAggregatedValue(ACTION_AGG)).get();
			actionV %= ACTIONS.values().length;

			ACTIONS action = ACTIONS.values()[actionV];

			// System.out.println("=============================id = "
			// + getId().get() + ", superstep = " + getSuperstep()
			// + ",action = " + action);

			if (action == ACTIONS.PICKING_MIN) {
				pickingMin(messages);
			} else if (action == ACTIONS.SUPER_VERTEX_FINDING) {
				findSuperVertex(messages);
			} else if (action == ACTIONS.NOTIFY) {
				notify(messages);
			} else if (action == ACTIONS.RELABELING) {
				relabel(messages);
			}
		}
	}

	public static class MSTCombiner extends
			Combiner<IntWritable, MSTMessageValue> {

		@Override
		public void combine(IntWritable vv, MSTMessageValue origin,
				MSTMessageValue second) {

			// System.out.println("merge " + origin + "," + second + ",for " +
			// vv);
			if (second.getType() == MESSAGE_TYPE.NULL) {
				return;
			}

			// second is not null
			if (origin.getType() == MESSAGE_TYPE.NULL) {
				// just copy every thing
				origin.setType(second.getType());
				origin.setIdDest(second.getIdDest());
				origin.setIdDestRoot(second.getIdDestRoot());
				origin.setIdSrc(second.getIdSrc());
				origin.getMergedSrc().clear();
				for (int i : second.getMergedSrc()) {
					origin.getMergedSrc().add(i);
				}
				origin.setSuperVertex(second.isSuperVertex());
				origin.setValue(second.getValue());

				return;
			}

			MESSAGE_TYPE otype = origin.getType();
			MESSAGE_TYPE stype = second.getType();

			// now both not null.
			if (otype == MESSAGE_TYPE.MIN && stype == MESSAGE_TYPE.MIN) {
				if (second.getValue() < origin.getValue()) {
					origin.setValue(second.getValue());
					origin.setIdSrc(second.getIdSrc());
					origin.setIdDest(second.getIdDest());
					origin.setIdDestRoot(second.getIdDestRoot());
				}
			}
			// question & answer
			else if (otype == MESSAGE_TYPE.QUESTION
					&& stype == MESSAGE_TYPE.QUESTION) {
				for (int i : second.getMergedSrc()) {
					origin.getMergedSrc().add(i);
				}
			} else if ((otype == MESSAGE_TYPE.ANSWER || otype == MESSAGE_TYPE.QUESTION_ANSWER)
					&& stype == MESSAGE_TYPE.QUESTION) {
				for (int i : second.getMergedSrc()) {
					origin.getMergedSrc().add(i);
				}
				origin.setType(MESSAGE_TYPE.QUESTION_ANSWER);
			} else if (otype == MESSAGE_TYPE.QUESTION
					&& stype == MESSAGE_TYPE.ANSWER) {
				origin.setIdSrc(second.getIdSrc());
				origin.setSuperVertex(second.isSuperVertex());
				origin.setType(MESSAGE_TYPE.QUESTION_ANSWER);
			} else if (otype == MESSAGE_TYPE.QUESTION
					&& stype == MESSAGE_TYPE.QUESTION_ANSWER) {
				for (int i : second.getMergedSrc()) {
					origin.getMergedSrc().add(i);
				}

				origin.setIdSrc(second.getIdSrc());
				origin.setSuperVertex(second.isSuperVertex());
				origin.setType(MESSAGE_TYPE.QUESTION_ANSWER);
			}
			// reply
			else if (otype == MESSAGE_TYPE.NOTIFY_ASK
					&& stype == MESSAGE_TYPE.NOTIFY_ASK) {
				for (int src : second.getMergedSrc()) {
					origin.getMergedSrc().add(src);
				}
			}
			// relabel
			else if (otype == MESSAGE_TYPE.RELABEL
					&& stype == MESSAGE_TYPE.RELABEL) {
				for (int src : second.getMergedSrc()) {
					origin.getMergedSrc().add(src);
				}
			} else {
				throw new RuntimeException("Unexpected message type: origin = "
						+ origin + ", second = " + second + ", id = " + vv);
			}

			// System.out.println("merge to " + origin);
		}

		@Override
		public MSTMessageValue createInitialMessage() {
			MSTMessageValue mess = new MSTMessageValue();
			mess.setType(MESSAGE_TYPE.NULL);

			return mess;
		}
	}

	public static class MSTMasterCompute extends DefaultMasterCompute {
		@Override
		public void initialize() throws InstantiationException,
				IllegalAccessException {
			registerAggregator(TINY_STEP_AGG, IntMinAggregator.class);
			registerAggregator(ACTION_AGG, IntMinAggregator.class);
		}
	}

	public static class MSTWorkerContext extends WorkerContext {

		@Override
		public void postApplication() {
		}

		@Override
		public void postSuperstep() {
		}

		@Override
		public void preApplication() throws InstantiationException,
				IllegalAccessException {
		}

		@Override
		public void preSuperstep() {
			int actionV = ((IntWritable) getAggregatedValue(ACTION_AGG)).get();
			int tinyStep = ((IntWritable) getAggregatedValue(TINY_STEP_AGG))
					.get();

			actionV %= ACTIONS.values().length;
			ACTIONS action = ACTIONS.values()[actionV];

			System.out.println("action = " + action + ", tiny step = "
					+ tinyStep);
		}

	}

	public static class MSTOutputFormat extends
			TextVertexOutputFormat<IntWritable, MSTVertexValue, MSTEdgeValue> {
		@Override
		public TextVertexWriter createVertexWriter(TaskAttemptContext context)
				throws IOException, InterruptedException {
			return new MSTWriter();
		}

		/**
		 * Simple VertexWriter that supports {@link PageRank}
		 */
		public class MSTWriter extends TextVertexWriter {
			@Override
			public void writeVertex(
					Vertex<IntWritable, MSTVertexValue, MSTEdgeValue, ?> vertex)
					throws IOException, InterruptedException {
				for (EdgeAdded ea : vertex.getValue().getEdges()) {
					getRecordWriter().write(new Text(ea.src + ""),
							new Text(ea.dest + "\t" + ea.value));
				}
			}
		}
	}
}
