package com.prime.player.core.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

final class ListUtils {
    // Suppresses default constructor, ensuring non-instantiability.
    private ListUtils() {
    }

    public static @Nullable
    <T> T find(@NonNull List<T> sortedList, long key,
               @NonNull LongKeyRetriever<T> keyRetriever) {
        int index = binarySearch(sortedList, key, keyRetriever);
        if (index >= 0) {
            return sortedList.get(index);
        }
        return null;
    }

    public static @Nullable
    <T> T findU(@NonNull List<T> unsortedList, long key,
                @NonNull LongKeyRetriever<T> keyRetriever) {
        int index = indexOf(unsortedList, key, keyRetriever);
        if (index >= 0) {
            return unsortedList.get(index);
        }
        return null;
    }

    public static <T> int indexOf(@NonNull List<T> unsortedList,
                                  long key,
                                  @NonNull LongKeyRetriever<T> keyRetriever) {
        for (int i = 0; i < unsortedList.size(); i++) {
            T item = unsortedList.get(i);
            long iKey = keyRetriever.getKey(item);
            if (iKey == key)
                return i;
        }
        return -1;
    }


    public static <T> int binarySearch(@NonNull List<T> sortedList, long key,
                                       @NonNull LongKeyRetriever<T> keyRetriever) {
        int lo = 0;
        int hi = sortedList.size() - 1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            long midKey = keyRetriever.getKey(sortedList.get(mid));

            if (midKey < key) {
                lo = mid + 1;
            } else if (midKey > key) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return ~lo;
    }

    @FunctionalInterface
    public interface LongKeyRetriever<T> {
        long getKey(@NonNull T value);
    }

}
