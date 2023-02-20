Let's suppose you're working on some logic that requires testing a rather large set for inclusion of an object.

One could of course, do something like:
```java
final Set<String> myLargeSet = new HashSet<>();
for(int idx = 0; idx < Short.MAX_VALUE; idx++) {
    myLargeSet.add(String.format("value-%d", idx));
}
```

However, depending on the parameterized type being used for the Set this could be computationally expensive and slow.

This is where a fascinating data structure called a "Bloom Filter" comes into play.

# What is a Bloom Filter?

To quote wikipedia:

`A Bloom Filter is a probabilistic data structure that is based on hashing.
It is extremely space efficient, and is typically used to add elements to a set and test if an element is in a set.`

More concretely:

A Bloom Filter uses hashing and some fancy bit manipulation to improve performance in testing large sets of data.

## Yea, but why?

As discussed above, the Bloom Filter is extremely space efficient - it can also be quite fast. 

A great and modern use case of the Bloom Filter is in bitcoin.
The bitcoin ledger uses a bloom filter in a lot of different areas, most notably in determining if a transaction is in a block or not. 

This is because the raw transaction data can be encoded and hashed allowing for usage with a bloom filter - saving precious CPU cycles, and time in the process.

# The Interface

Let's define a simple interface:

## BloomFilter.java
```java
package cloud.tazz.bloom;

import javax.annotation.Nonnull;

public interface BloomFilter<T> {
boolean contains(@Nonnull final T value);
void insert(@Nonnull final T value);
}
```

## AbstractBloomFilter.java
```java
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
```

# Some Simple Unit Tests (Java / JUnit)

Let's start by creating some unit tests for our bloom filter to define its behavior:

## BloomFilterTest.java
```java
package cloud.tazz.bloom;

import com.google.common.hash.HashCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// *Note #1* 
// We will assume that in a production environment there will be many more objects involved than the 
// below tests have
public final class BloomFilterTest {
    @Test()
    public void test_InsertOne_WillPass() {
        final String a = "Hello, world"; // let's create a friendly string to test our filter
        final BloomFilter<String> filter = HashCodeBloomFilter.<String>builder()
                .insert(a)
                .build();
        // we can assume the following are definitely not in there:
        Assertions.assertFalse(filter.contains("This is a test"));
        Assertions.assertFalse(filter.contains("This is another test"));
        Assertions.assertFalse(filter.contains("Hello, readers"));

        // we can assume that our a value is probably in there
        Assertions.assertTrue(filter.contains(a));
    }

    @Test
    public void test_InsertMultiple_WillPass() {
        final String a = "Hello, world";
        final String b = "Hello, again";

        final BloomFilter<String> filter = HashCodeBloomFilter.<String>builder()
                .insert(a)
                .insert(b)
                .build();
        // we can assume the following are definitely not in there:
        Assertions.assertFalse(filter.contains("This is a test"));
        Assertions.assertFalse(filter.contains("This is another test"));
        Assertions.assertFalse(filter.contains("Hello, readers"));

        // we can assume that our a & b values are probably in there:
        Assertions.assertTrue(filter.contains(a));
        Assertions.assertTrue(filter.contains(b));
    }
}
```

# A Naive Implementation (Java / HashCode)

So for the implementation, we are going to choose a "naive" approach. By "naive" I mean **don't use it in production**, read further for what to do a production use case.

Our approach is as follows:

- Create an implementation of a BloomFilter using Java's hashCode as our hash.

The usage of hashCode in this implementation is *why it shouldn't necessarily be used in production* 
as Java's hashCode can be prone collisions - or where 2 separate objects have the same hashCode.
A simple fix for this is to use a better hashing algorithm - such as: SHA256 / SHA512

## HashCodeBloomFilter.java
```java
package cloud.tazz.bloom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class HashCodeBloomFilter<T> extends AbstractBloomFilter<T> {
    public static final int DEFAULT_SIZE = Integer.MAX_VALUE;
    public static final int DEFAULT_NUMBER_OF_HASHES = 4;
    
    private static final Logger LOG = LoggerFactory.getLogger(HashCodeBloomFilter.class);
    private final int hashes;

    /**
     * see: Tuning Our Implementation for more about these parameter values.
     * @param size - The amount of bits in this filter.
     * @param hashes - The amount of times an object should be hashed.
     */
    HashCodeBloomFilter(int size, int hashes) {
        super(size);
        this.hashes = hashes;
    }

    HashCodeBloomFilter() {
        this(DEFAULT_SIZE, DEFAULT_NUMBER_OF_HASHES);
    }

    /**
     * Function to determine if a value might be in this filter.
     * 
     * @param value - The value to test
     * @return True if the value might be in this filter. False if it is definitely not in the filter.
     */
    @Override
    public boolean contains(@Nonnull final T value) {
        Objects.requireNonNull(value, "expected value to not be null");
        for(int h = 0; h < this.hashes; h++) {
            final long hashCode = Math.abs(Objects.hash(h, value));
            final int pos = (int) (hashCode % this.nbits);
            if(!data.get(pos))
                return false;
        }
        return true;
    }

    /**
     * Function to insert a value into this filter.
     * 
     * @param value - The value to insert
     */
    @Override
    public void insert(@Nonnull final T value) {
        Objects.requireNonNull(value, "expected value to not be null");
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

    // let's also add a simple builder class to compose our bloom filters easily:
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
```

### Tuning Our Implementation

You might have noticed earlier that the Bloom Filter is a probabilistic data structure.
This means that testing the filter boils down to:

* If the result is true, then the object *might* be in the set
* If the result is false, then the object is definitely not in the set.

This is important because our filter can sometimes return a true value when being tested. 
This mostly ends up being caused by collisions in our hash algorithm. 
We can mitigate these false positives by tuning the size of the filter and the number of times our hash is used.

#### The size Parameter

One way to decrease false positives is to increase the amount of bits in our bloom filter.

In our naive implementation this is done by changing the `size` parameter. By default, we use Integer.MAX_VALUE.

*Note*: Keep in mind that the goal of a bloom filter is to be space efficient, so it can take some testing and discovery to identify a good value.

#### The hashes Parameter

Another way to decrease false positives is to increase the amount of hashes done on our values.

In our naive implementation this is done by changing the `hashes` parameter. By default, we use 4.

*Note*: Keep in mind that the goal of a bloom filter is to be space efficient, so it can take some testing and discovery to identify a good value.

# References & Notes

- [A brilliant.org article on Bloom Filters](https://brilliant.org/wiki/bloom-filter/#:~:text=A%20bloom%20filter%20is%20a,is%20added%20to%20the%20set)
- [A wikipedia.org article on Bloom Filters](https://en.wikipedia.org/wiki/Bloom_filter)
- [A C++ Implementation](https://github.com/s0cks/token/blob/develop/tkn-utils/include/bloom.h)
- [The Java Implementation](https://github.com/s0cks/java-bloom-filter-example)