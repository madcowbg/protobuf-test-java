package tea.comparator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RecursiveDifferencer {

    @SafeVarargs
    public static <V> Differencer<V> compose(Differencer<V> ...differencers) {
        return (Path p, V left, V right) -> Arrays.stream(differencers).flatMap(d -> d.differences(p, left, right));
    }

    public static <P, C> Differencer<P> diffChildWithEquals(Function<P, C> f, String fieldName) {
        return diffChild(f, fieldName, diffWithEquals());
    }

    public static <P, C> Differencer<P> diffChild(Function<P, C> f, String fieldName, Differencer<C> childDiff) {
        return (Path path, P left, P right) ->
                testChild(fieldName, childDiff).differences(path, f.apply(left), f.apply(right));
    }

    public static <V> Differencer<List<V>> diffListElements(Differencer<V> elementDiff) {
        return (Path path, List<V> left, List<V> right) ->
                RecursiveDifferencer.<Integer, V>diffMapValues(elementDiff).differences(path, listToMap(left), listToMap(right));
    }

    public static <K, V> Differencer<List<V>> diffListElementsAsMap(Differencer<V> elementDiff, Function<V, K> keyMap) {
        return (Path path, List<V> left, List<V> right) ->
                RecursiveDifferencer.<K, V>diffMapValues(elementDiff).differences(path, listToMap(left, keyMap), listToMap(right, keyMap));
    }

    public static <K, V> Differencer<Map<K, V>> diffMapValues(Differencer<V> valueDiff) {
        return (Path path, Map<K, V> left, Map<K, V> right) -> {
            var commonKeys = left.keySet().stream().filter(right::containsKey).collect(Collectors.toSet());
            var leftOnlyKeys = left.keySet().stream().filter(Predicate.not(commonKeys::contains));
            var rightOnlyKeys = right.keySet().stream().filter(Predicate.not(commonKeys::contains));
            return Stream.concat(
                    diffInCommon(commonKeys, valueDiff).differences(path, left, right),
                    Stream.concat(
                            diffLeftOnly(path, leftOnlyKeys, left::get),
                            diffRightOnly(path, rightOnlyKeys, right::get)));
        };
    }

    private static <V> Differencer<V> testChild(String fieldName, Differencer<V> differencer) {
        return (Path path, V left, V right) -> differencer.differences(path.sub(fieldName), left, right);
    }

    private static <K, V> Stream<Difference> diffRightOnly(Path path, Stream<K> keys, Function<K, V> right) {
        return keys.map(k -> Difference.rightOnly(path.sub(k.toString()), right.apply(k)));
    }

    private static <K, V> Stream<Difference> diffLeftOnly(Path path, Stream<K> keys, Function<K, V> left) {
        return keys.map(k -> Difference.leftOnly(path.sub(k.toString()), left.apply(k)));
    }

    private static <K, V> Differencer<Map<K, V>> diffInCommon(Set<K> keys, Differencer<V> compareValues) {
        return (Path path, Map<K, V> left, Map<K, V> right) -> keys.stream()
                .flatMap(k -> compareValues.differences(path.sub(k.toString()), left.get(k), right.get(k)));
    }

    private static <V> Differencer<V> testIfEqual(BiPredicate<V, V> equals, Differencer<V> differencer) {
        return (p, l, r) -> equals.test(l, r) ? Stream.empty() : differencer.differences(p, l, r);
    }


    private static <V> Differencer<V> diffWithEquals() {
        return RecursiveDifferencer.testIfEqual(Object::equals, Difference::unequal);
    }

    private static <K, V> Map<K,V> listToMap(List<V> list, Function<V, K> keyMap) {
        return list.stream().collect(Collectors.toMap(keyMap, Function.identity()));
    }

    private static <V> Map<Integer, V> listToMap(List<V> list) {
        return IntStream.range(0, list.size()).boxed().collect(Collectors.toMap(Function.identity(), list::get));
    }
}
