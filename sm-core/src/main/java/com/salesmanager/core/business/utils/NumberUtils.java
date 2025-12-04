package com.salesmanager.core.business.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NumberUtils {

    public static boolean isPositive(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

    public static List<Integer> generateSequence(int start, int count) {
        List<Integer> sequence = new ArrayList<>();
        for (int i = start; i < start + count; i++) {
            sequence.add(i);
        }
        return sequence;
    }

    public static int sumRange(int limit) {
        int sum = 0;
        for (int i = 0; i < limit; i++) {
            sum += i;
        }
        return sum;
    }
}
