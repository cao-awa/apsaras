package com.github.zhuaidadaya.rikaishinikui.handler.reader.string.iterator;

import java.util.*;

public final class StringReadStreamIterator implements Iterator<String> {
    private final String str;
    private int index;

    public StringReadStreamIterator(String tokenizer) {
        this.str = tokenizer;
    }

    @Override
    public boolean hasNext() {
        return str.length() > index;
    }

    @Override
    public String next() {
        return String.valueOf(str.charAt(index++));
    }
}
