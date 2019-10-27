package tea;

import com.example.tutorial.AddressBookProtos.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

interface Differencer<V> {
    Stream<Difference> differences(V left, V right);
}

interface DifferencerForKey<K, V> {
    Differencer<V> forKey(K key);
}

class Difference {
    private final String msg;

    public Difference(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return msg;
    }

    public static <K, T> Difference rightOnly(K key, T right) {
        return new Difference("" + key + " = " + right.toString() + " in right only!");
    }

    public static <K, T> Difference leftOnly(K key, T left) {
        return new Difference("" + key + " = " + left.toString() + " in left only!");
    }

    public static <T, K> Differencer<T> unequalKey(K key) {
        return (T left, T right) -> Stream.of(new Difference("" + key + ": " + left.toString() + " != " + right.toString()));
    }
}

public class Diff {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: Diff <filename> <filename>");
            return;
        }

        try {
            var left = AddressBook.parseFrom(new FileInputStream(args[0]));
            var right = AddressBook.parseFrom(new FileInputStream(args[1]));

            System.out.println(
                    "Diff:\n" + compare(left, right)
                            .map(Object::toString)
                            .collect(Collectors.joining("--------------\n", "\n", "\n")));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Difference> compare(AddressBook left, AddressBook right) {
        return compareList(left.getPeopleList(), right.getPeopleList());

    }

    private static Stream<Difference> compareList(List<Person> left, List<Person> right) {
        return compareList(left, right, Diff.ifTestKey(Person::equals, Difference::unequalKey));
    }

    private static <V> Stream<Difference> compareList(List<V> left, List<V> right, DifferencerForKey<Integer, V> differencer) {
        return compareMap(differencer).differences(listToMap(left), listToMap(right));
    }

    private static <K, V> Differencer<Map<K, V>> compareMap(DifferencerForKey<K, V> differencer) {
        return (Map<K, V> left, Map<K, V> right) -> {
            var commonKeys = left.keySet().stream().filter(right::containsKey).collect(Collectors.toSet());
            var leftOnlyKeys = left.keySet().stream().filter(Predicate.not(commonKeys::contains));
            var rightOnlyKeys = right.keySet().stream().filter(Predicate.not(commonKeys::contains));
            return Stream.concat(
                    diffInCommon(commonKeys, left, right, differencer),
                    Stream.concat(
                            diffLeftOnly(leftOnlyKeys, left::get),
                            diffRightOnly(rightOnlyKeys, right::get)));
        };
    }

    private static <K, V> Stream<Difference> diffRightOnly(Stream<K> keys, Function<K, V> right) {
        return keys.map(k -> Difference.rightOnly(k, right.apply(k)));
    }

    private static <K, V> Stream<Difference> diffLeftOnly(Stream<K> keys, Function<K, V> left) {
        return keys.map(k -> Difference.leftOnly(k, left.apply(k)));
    }

    private static <K, V> DifferencerForKey<K, V> ifTestKey(BiPredicate<V, V> equals, DifferencerForKey<K, V> differencer) {
        return k -> ifTest(equals, differencer.forKey(k));
    }

    private static <V> Differencer<V> ifTest(BiPredicate<V, V> equals, Differencer<V> differencer) {
        return (l, r) -> equals.test(l, r) ? Stream.empty() : differencer.differences(l, r);
    }

    private static <K, V> Stream<Difference> diffInCommon(
            Set<K> keys,
            Map<K, V> left,
            Map<K, V> right,
            DifferencerForKey<K, V> compareValues) {
        return keys.stream()
                .flatMap(k -> compareValues.forKey(k).differences(left.get(k), right.get(k)));
    }

    private static <V> Map<Integer, V> listToMap(List<V> list) {
        return IntStream.range(0, list.size()).boxed().collect(Collectors.toMap(Function.identity(), list::get));
    }


}
