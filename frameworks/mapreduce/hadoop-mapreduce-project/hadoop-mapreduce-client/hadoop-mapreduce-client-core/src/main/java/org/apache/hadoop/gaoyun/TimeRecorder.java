package org.apache.hadoop.gaoyun;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TimeRecorder {
	
	public static void addMemoryRecord(String baseName){
		long total = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		Record rtotal = TimeRecorder.getRecord(baseName + "_total");
		rtotal.increValue(total);
		
		Record rfree = TimeRecorder.getRecord(baseName + "_used");
		rfree.increValue(total - free);
	}
	
	public static class Record {
		public long value;
		public long statisAccount;
		private long lastStart;
		private boolean started = false;

		private List<Long> multipleValues = new LinkedList<Long>();
		private List<Long> multipleStatisAccounts = new LinkedList<Long>();
		private long multipleValue;
		private long multipleLastStart;
		private long multiStatisAccount;
		private boolean multipleStarted = false;

		public void timerStart() {
			started = true;
			lastStart = System.currentTimeMillis();
		}

		public void timerEnd() {
			if (started) {
				started = false;
				long end = System.currentTimeMillis();
				value = value + end - lastStart;
				statisAccount += 1;
			}
		}

		public void multiTimerStart() {
			multipleStarted = true;
			multipleLastStart = System.currentTimeMillis();
			multipleValue = 0;
			multiStatisAccount = 0;
		}

		public void multiContinue() {
			if (multipleStarted) {
				multipleLastStart = System.currentTimeMillis();
			}
		}

		public void multiPause() {
			if (multipleStarted) {
				long end = System.currentTimeMillis();
				multipleValue += (end - multipleLastStart);
				++multiStatisAccount;
			}
		}

		public void multiTimerEnd() {
			if (multipleStarted) {
				multipleStarted = false;
				multipleValues.add(multipleValue);
				multipleStatisAccounts.add(multiStatisAccount);
				multiStatisAccount = 0;
			}
		}

		public void increValue(long v) {
			this.value += v;
		}

		public void addNewValue(long v) {
			multipleValues.add(v);
		}
	}

	private static Map<String, Record> values;
	static {
		values = new HashMap<String, Record>();
	}

	/**
	 * 
	 * @param name
	 * @return We return the Long
	 */
	public static Record getRecord(String name) {
		Record value = values.get(name);
		if (value == null) {
			value = new Record();
			values.put(name, value);
		}

		return value;
	}

	public static String dump() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		for (Map.Entry<String, Record> e : values.entrySet()) {

			Record r = e.getValue();
			if (r.multipleValues.size() == 0) {
				sb.append("====").append(e.getKey()).append("\t")
						.append(e.getValue().value).append("\t")
						.append(e.getValue().statisAccount);
				sb.append("\n");
			} else {
				for (int i = 0; i < r.multipleValues.size(); ++i) {
					sb.append("====").append(e.getKey()).append(":").append(i + 1).append("\t")
							.append(r.multipleValues.get(i)).append("\t")
							.append(r.multipleStatisAccounts.get(i));
					sb.append("\n");
				}

			}

		}

		return sb.toString();
	}
}
