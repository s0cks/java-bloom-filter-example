package cloud.tazz.bloom;

import java.util.BitSet;

public abstract class AbstractBloomFilter<T> implements BloomFilter<T> {
    protected final BitSet data;
    protected final int nbits;

    protected AbstractBloomFilter(final int nbits) {
        this.data = new BitSet(nbits);
        this.nbits = nbits;
    }
}