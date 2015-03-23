#!/usr/bin/env bash

COUNT=1
#2.5 kill all nethogs & slaves
while read mac;do
	ssh -n root@$mac "killall $PERF_TEST_DIR/nethogs/nethogs"
	echo "ssh -n root@$mac \"kill -9 ${SLAVE_IDS[$COUNT]}\""
	ssh -n root@$mac "kill -9 ${SLAVE_IDS[$COUNT]}"
	COUNT=$((COUNT + 1))
done< <(cat $PERF_TEST_MACHINE_FILE)

#3. collect result
echo "collecting result..."
mkdir -p "$OUTPUT_DIR"
cat $PERF_TEST_MACHINE_FILE |
	while read mac;do
		scp -r $PERF_TEST_USER@$mac:$PERF_OUTPUT_DIR "$OUTPUT_DIR/$mac"
	done
echo "result collected & perf stopped"
