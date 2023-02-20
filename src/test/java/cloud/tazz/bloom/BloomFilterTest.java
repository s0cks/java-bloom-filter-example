package cloud.tazz.bloom;

import com.google.common.hash.HashCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class BloomFilterTest {
    @Test()
    public void test_InsertOne_WillPass() {
        final String a = "Hello, world";

        final BloomFilter<String> filter = HashCodeBloomFilter.<String>builder()
                .insert(a)
                .build();
        Assertions.assertFalse(filter.contains("This is a test"));
        Assertions.assertFalse(filter.contains("This is another test"));
        Assertions.assertFalse(filter.contains("Hello, readers"));

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
        Assertions.assertFalse(filter.contains("This is a test"));
        Assertions.assertFalse(filter.contains("This is another test"));
        Assertions.assertFalse(filter.contains("Hello, readers"));

        Assertions.assertTrue(filter.contains(a));
        Assertions.assertTrue(filter.contains(b));
    }
}