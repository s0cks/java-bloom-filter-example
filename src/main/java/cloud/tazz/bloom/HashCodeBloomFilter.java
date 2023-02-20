package cloud.tazz.bloom;

import com.google.common.hash.HashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class HashCodeBloomFilter<T> extends AbstractBloomFilter<T> {
    public static final int DEFAULT_SIZE = Integer.MAX_VALUE;
    public static final int DEFAULT_NUMBER_OF_HASHES = 4;
    private static final Logger LOG = LoggerFactory.getLogger(HashCodeBloomFilter.class);
    private final int hashes;

    HashCodeBloomFilter(int size, int hashes) {
        super(size);
        this.hashes = hashes;
    }

    HashCodeBloomFilter() {
        this(DEFAULT_SIZE, DEFAULT_NUMBER_OF_HASHES);
    }

    @Override
    public boolean contains(@Nonnull final T value) {
        for(int h = 0; h < this.hashes; h++) {
            final long hashCode = Math.abs(Objects.hash(h, value));
            final int pos = (int) (hashCode % this.nbits);
            if(!data.get(pos))
                return false;
        }
        return true;
    }

    @Override
    public void insert(@Nonnull final T value) {
        for(int h = 0; h < this.hashes; h++) {
            final long hashCode = Math.abs(Objects.hash(h, value));
            final int pos = (int) (hashCode % this.nbits);
            LOG.info("hashCode {}; pos {} for {}", hashCode, pos, value);
            this.data.set(pos);
        }
    }

    @Nonnull
    public static <T> BloomFilter<T> of() {
        return new HashCodeBloomFilter<>();
    }

    @Nonnull
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private final BloomFilter<T> data;

        Builder() {
            this.data = HashCodeBloomFilter.of();
        }

        @Nonnull
        public Builder<T> insert(@Nonnull final T value) {
            Objects.requireNonNull(value, "expected value to not be null");
            this.data.insert(value);
            return this;
        }

        @Nonnull
        public BloomFilter<T> build() {
            return this.data;
        }
    }
}