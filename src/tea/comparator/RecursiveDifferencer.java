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
    public static <L, R> Differencer<L, R> compose(Differencer<L, R> ...differencers) {
        return (Path p, L left, R right) -> Arrays.stream(differencers).flatMap(d -> d.differences(p, left, right));
    }

    public static <P, C> Differencer<P, P> diffChildWithEquals(Function<P, C> f, String fieldName) {
        return diffChildAsymWithEquals(f, f, fieldName);
    }

    public static <PL, PR, CL, CR> Differencer<PL, PR> diffChildAsymWithEquals(Function<PL, CL> fL, Function<PR, CR> fR, String fieldName) {
        return diffChildAsym(fL, fR, fieldName, diffWithEquals());
    }

    public static <P, C> Differencer<P, P> diffChild(Function<P, C> f, String fieldName, Differencer<C, C> childDiff) {
        return diffChildAsym(f, f, fieldName, childDiff);
    }

    public static <PL, PR, CL, CR> Differencer<PL, PR> diffChildAsym(Function<PL, CL> fL, Function<PR, CR> fR, String fieldName, Differencer<CL, CR> childDiff) {
        return (Path path, PL left, PR right) ->
                testChild(fieldName, childDiff).differences(path, fL.apply(left), fR.apply(right));
    }

    public static <L, R> Differencer<List<L>, List<R>> diffListElements(Differencer<L, R> elementDiff) {
        return (Path path, List<L> left, List<R> right) ->
                RecursiveDifferencer.<Integer, L, R>diffMapValues(elementDiff).differences(path, listToMap(left), listToMap(right));
    }

    public static <K, V> Differencer<List<V>, List<V>> diffListElementsAsMap(Differencer<V, V> elementDiff, Function<V, K> keyMap) {
        return (Path path, List<V> left, List<V> right) ->
                RecursiveDifferencer.<K, V, V>diffMapValues(elementDiff).differences(path, listToMap(left, keyMap), listToMap(right, keyMap));
    }

    public static <K, L, R> Differencer<Map<K, L>, Map<K, R>> diffMapValues(Differencer<L, R> valueDiff) {
        return (Path path, Map<K, L> left, Map<K, R> right) -> {
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

    private static <L, R> Differencer<L, R> testChild(String fieldName, Differencer<L, R> differencer) {
        return (Path path, L left, R right) -> differencer.differences(path.sub(fieldName), left, right);
    }

    private static <K, V> Stream<Difference> diffRightOnly(Path path, Stream<K> keys, Function<K, V> right) {
        return keys.map(k -> Difference.rightOnly(path.sub(k.toString()), right.apply(k)));
    }

    private static <K, V> Stream<Difference> diffLeftOnly(Path path, Stream<K> keys, Function<K, V> left) {
        return keys.map(k -> Difference.leftOnly(path.sub(k.toString()), left.apply(k)));
    }

    private static <K, L, R> Differencer<Map<K, L>, Map<K, R>> diffInCommon(Set<K> keys, Differencer<L, R> compareValues) {
        return (Path path, Map<K, L> left, Map<K, R> right) -> keys.stream()
                .flatMap(k -> compareValues.differences(path.sub(k.toString()), left.get(k), right.get(k)));
    }

    private static <L, R> Differencer<L, R> testIfEqual(BiPredicate<L, R> equals, Differencer<L, R> differencer) {
        return (p, l, r) -> equals.test(l, r) ? Stream.empty() : differencer.differences(p, l, r);
    }


    private static <L, R> Differencer<L, R> diffWithEquals() {
        return RecursiveDifferencer.testIfEqual(Object::equals, Difference::unequal);
    }

    private static <K, V> Map<K,V> listToMap(List<V> list, Function<V, K> keyMap) {
        return list.stream().collect(Collectors.toMap(keyMap, Function.identity()));
    }

    private static <V> Map<Integer, V> listToMap(List<V> list) {
        return IntStream.range(0, list.size()).boxed().collect(Collectors.toMap(Function.identity(), list::get));
    }
}
