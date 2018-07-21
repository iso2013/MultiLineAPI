package net.blitzcube.mlapi.util;


import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by iso2013 on 6/7/2018.
 */
public class RangeSeries {
    private final SortedMap<Integer, Integer> backingMap = new TreeMap<>();

    public void put(int i) {
        Integer v = backingMap.get(i - 1);
        v = v != null ? v : i;
        backingMap.put(i, v);
        int n = 1;
        while (backingMap.containsKey(i + n)) backingMap.put(i + n++, v);
    }

    public Collection<Range> getRanges() {
        Map<Integer, Range> r = new HashMap<>();
        backingMap.forEach((k, v) -> {
            if (!r.containsKey(v)) r.put(v, new Range(v));
            r.get(v).expand(k);
        });
        return r.values();
    }

    public boolean contains(int idx) {
        return backingMap.containsKey(idx);
    }

    public static class Range implements Iterable<Integer> {
        private int lower;
        private int upper;

        Range(int i) { lower = upper = i; }

        private void expand(int i) {
            this.lower = Math.min(i, lower);
            this.upper = Math.max(i, upper);
        }

        public int size() { return (upper - lower) + 1; }

        @Override
        public Iterator<Integer> iterator() {
            return IntStream.range(lower, upper + 1).iterator();
        }

        public int getLower() {
            return lower;
        }

        public int getUpper() { return upper; }
    }
}
