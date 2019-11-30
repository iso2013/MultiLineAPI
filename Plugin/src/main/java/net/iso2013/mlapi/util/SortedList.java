package net.iso2013.mlapi.util;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by iso2013 on 8/7/2018.
 */
public class SortedList<T> extends ArrayList<T> {
    private final Comparator<T> comparator;

    public SortedList(Comparator<T> comparator) {
        super();
        this.comparator = comparator;
    }

    @Override
    public boolean add(T t) {
        int idx = size();
        for (int i = 0; i < size(); i++)
            if (comparator.compare(t, get(i)) <= 0) {
                idx = i;
                break;
            }
        super.add(idx, t);
        return true;
    }
}
