package tea.comparator;

import java.util.stream.Stream;

public interface Differencer<L, R> {
    Stream<Difference> differences(Path path, L left, R right);
}
