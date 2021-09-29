package com.qiniu.droid.rtc.demo.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MyHashMap<K, V> extends HashMap<K, V> {

    private final List<V> mOrderedValues = new LinkedList<>();

    public List<V> getOrderedValues() {
        return new ArrayList<>(mOrderedValues);
    }

    public List<V> getOrderedValues(V exclude) {
        LinkedList<V> result = new LinkedList<>(mOrderedValues);
        result.remove(exclude);
        return result;
    }

    public V put(K key, V value, boolean insertToFirst) {
        // in case replace key
        mOrderedValues.remove(get(key));
        // in case replace value
        mOrderedValues.remove(value);

        if (insertToFirst) {
            mOrderedValues.add(0, value);
        } else {
            mOrderedValues.add(value);
        }
        return super.put(key, value);
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, false);
    }

    @Override
    public V remove(Object key) {
        V result = super.remove(key);
        mOrderedValues.remove(result);
        return result;
    }

    @Override
    public void clear() {
        mOrderedValues.clear();
        super.clear();
    }
}
