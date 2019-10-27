package tea.comparator;

public class Path {
    private final String path;

    private Path(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }

    public static Path root() {
        return new Path("");
    }

    Path sub(String suffix) {
        return new Path(path + "/" + suffix);
    }
}
