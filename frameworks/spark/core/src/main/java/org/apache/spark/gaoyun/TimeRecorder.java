package org.apache.spark.gaoyun;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TimeRecorder {
	public static void addMemoryRecord(String baseName){
		long totalMem = Runtime.getRuntime().totalMemory();
		long freeMem = Runtime.getRuntime().freeMemory();

		Record total = TimeRecorder.getRecord(baseName + "_total");
		total.increValue(totalMem);
		
		Record used = TimeRecorder.getRecord(baseName + "_used");
		used.increValue(totalMem - freeMem);
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
			lastStart = System.nanoTime();
		}

		public void timerEnd() {
			if (started) {
				started = false;
				long end = System.nanoTime();
				value = value + (long)((end - lastStart));
				statisAccount += 1;
			}
		}

		public void multiTimerStart() {
			multipleStarted = true;
			multipleLastStart = System.nanoTime();
			multipleValue = 0;
			multiStatisAccount = 0;
		}

		public void multiContinue() {
			if (multipleStarted) {
				multipleLastStart = System.nanoTime();
			}
		}

		public void multiPause() {
			if (multipleStarted) {
				long end = System.nanoTime();
				multipleValue += (long)((end - multipleLastStart));
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

	public static void clear(){
		values.clear();
	}

	public static String dump() {
		StringBuffer sb = new StringBuffer();
		sb.append("Time Records:\n");
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

