package tea.comparator;

import java.util.stream.Stream;

public interface Differencer<V> {
    Stream<Difference> differences(Path path, V left, V right);
}
