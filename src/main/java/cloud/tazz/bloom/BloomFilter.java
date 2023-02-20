package cloud.tazz.bloom;

import javax.annotation.Nonnull;

public interface BloomFilter<T> {
    boolean contains(@Nonnull final T value);
    void insert(@Nonnull final T value);
}