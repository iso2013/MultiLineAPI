package net.iso2013.mlapi.util;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by iso2013 on 6/7/2018.
 */
public class RangeSeries {

    private final SortedMap<Integer, Integer> backingMap = new TreeMap<>();

    public void put(int i) {
        Integer value = backingMap.get(i - 1);
        value = value != null ? value : i;

        this.backingMap.put(i, value);
        int n = 1;
        while (backingMap.containsKey(i + n)) {
            this.backingMap.put(i + n, value);
            n++;
        }
    }

    public Collection<Range> getRanges() {
        Map<Integer, Range> ranges = new HashMap<>();

        this.backingMap.forEach((k, v) -> {
            if (!ranges.containsKey(v)) {
                ranges.put(v, new Range(v));
            }

            ranges.get(v).expand(k);
        });

        return ranges.values();
    }

    public boolean contains(int i) {
        return backingMap.containsKey(i);
    }

    public static class Range implements Iterable<Integer> {

        private int lower, upper;

        private Range(int i) {
            this.upper = lower = i;
        }

        private void expand(int i) {
            this.lower = Math.min(i, lower);
            this.upper = Math.max(i, upper);
        }

        @Override
        public Iterator<Integer> iterator() {
            return IntStream.range(lower, upper + 1).iterator();
        }

        public int getLower() {
            return lower;
        }

        public int getUpper() {
            return upper;
        }
    }
}
