package com.apache.giraph.gaoyun;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TimeRecorder2 {
	
	public static void addMemoryRecord(String baseName){
		long total = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		Record2 rtotal = TimeRecorder2.getRecord(baseName + "_total");
		rtotal.increValue(total);
		
		Record2 rfree = TimeRecorder2.getRecord(baseName + "_used");
		rfree.increValue(total - free);
	}
	
	public static class Record2 {
		public long value;
		public long statisAccount;
		private long lastStart;
		private boolean started = false;

		private List<Long> multipleValues = new LinkedList<Long>();
		private List<Long> multipleStatisAccounts = new LinkedList<Long>();
		private long multipleValue;
		private long multipleLastStart;
		
		private AtomicLong multiStatisAccount = new AtomicLong(0);
		
		private boolean multipleStarted = false;

		public void timerStart() {
			started = true;
			lastStart = System.nanoTime();
		}

		public void timerEnd() {
			if (started) {
				started = false;
				long end = System.nanoTime();
				value = value + end - lastStart;
				statisAccount += 1;
			}
		}

		public void multiTimerStart() {
			multipleStarted = true;
			multipleLastStart = System.nanoTime();
			multipleValue = 0;
			multiStatisAccount.set(0);
		}

		public void multiContinue() {
			if (multipleStarted) {
				multipleLastStart = System.nanoTime();
			}
		}

		public void multiPause() {
			if (multipleStarted) {
				long end = System.nanoTime();
				multipleValue += (end - multipleLastStart);
			}
			
			multiStatisAccount.incrementAndGet();
		}

		public void multiTimerEnd() {
			if (multipleStarted) {
				multipleStarted = false;
				multipleValues.add(multipleValue);
				multipleStatisAccounts.add(multiStatisAccount.get());
				multiStatisAccount.set(0);
			}
		}

		public void increValue(long v) {
			this.value += v;
		}

		public void addNewValue(long v) {
			multipleValues.add(v);
		}
	}

	private static ConcurrentHashMap<String, Record2> values;
	static {
		values = new ConcurrentHashMap<String, Record2>();
	}

	/**
	 * 
	 * @param name
	 * @return We return the Long
	 */
	public static Record2 getRecord(String name) {
		Record2 value = values.putIfAbsent(name, new Record2());
		if(value == null){
			value = values.get(name);
		}
		return value;
	}

	public static String dump() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		for (Map.Entry<String, Record2> e : values.entrySet()) {

			Record2 r = e.getValue();
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
