package tea.comparator;

import java.util.stream.Stream;

public class Difference {
    private final String msg;

    private Difference(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return msg;
    }

    static <T> Difference rightOnly(Path path, T right) {
        return new Difference(path.toString() + " = " + right.toString() + " exists in right only!");
    }

    static <T> Difference leftOnly(Path path, T left) {
        return new Difference(path.toString() + " = " + left.toString() + " exists in left only!");
    }

    static <T> Stream<Difference> unequal(Path path, T left, T right) {
        return Stream.of(new Difference(path.toString() + ":\n\t" + left.toString() + " != " + right.toString()));
    }
}
