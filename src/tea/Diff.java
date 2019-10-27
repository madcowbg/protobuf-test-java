package tea;

import com.example.tutorial.AddressBookProtos.*;

import java.io.FileInputStream;
import java.io.IOException;
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


class Path {
    private final String path;

    Path(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }

    public Path sub(String suffix) {
        return new Path(path + "/" + suffix);
    }
}

interface Differencer<V> {
    Stream<Difference> differences(Path path, V left, V right);
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

    public static <T> Difference rightOnly(Path path, T right) {
        return new Difference(path.toString() + " = " + right.toString() + " exists in right only!");
    }

    public static <T> Difference leftOnly(Path path, T left) {
        return new Difference(path.toString() + " = " + left.toString() + " exists in left only!");
    }

    public static <T> Stream<Difference> unequal(Path path, T left, T right) {
        return Stream.of(new Difference(path.toString() + ":\n\t" + left.toString() + " != " + right.toString()));
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
                            .collect(Collectors.joining("\n", "\n", "\n")));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Difference> compare(AddressBook left, AddressBook right) {
        return compareList(left.getPeopleList(), right.getPeopleList());

    }

    private static Stream<Difference> compareList(List<Person> left, List<Person> right) {
        var diffPhone = compose(
                diffChildAsValue(Person.PhoneNumber::getNumber, "number"),
                diffChildAsValue(Person.PhoneNumber::getType, "type"));

        var personDifferencer = compose(
                diffChildAsValue(Person::getEmail, "email"),
                diffChildAsValue(Person::getId, "id"),
                diffChild(Person::getPhonesList, "phones", compareList(diffPhone)));

        return testChild("persons", compareList(personDifferencer))
                .differences(new Path(""), left, right);
    }


    @SafeVarargs
    private static <V> Differencer<V> compose(Differencer<V> ...differencers) {
        return (Path p, V left, V right) -> Arrays.stream(differencers).flatMap(d -> d.differences(p, left, right));
    }

    private static <P, C> Differencer<P> diffChildAsValue(Function<P, C> f, String fieldName) {
        return diffChild(f, fieldName, diffAsValue());
    }

    private static <P, C> Differencer<P> diffChild(Function<P, C> f, String fieldName, Differencer<C> childDifferencer) {
       return (Path path, P left, P right) ->
               testChild(fieldName, childDifferencer).differences(path, f.apply(left), f.apply(right));
    }

    private static <V> Differencer<List<V>> compareList(Differencer<V> elementDifferencer) {
        return (Path path, List<V> left, List<V> right) ->
                Diff.<Integer, V>compareMap(elementDifferencer).differences(path, listToMap(left), listToMap(right));
    }

    private static <K, V> Differencer<Map<K, V>> compareChildMap(String mapName, Differencer<V> valueDifferencer) {
        return testChild(mapName, compareMap(valueDifferencer));
    }

    private static <V> Differencer<V> testChild(String fieldName, Differencer<V> differencer) {
        return (Path path, V left, V right) -> differencer.differences(path.sub(fieldName), left, right);
    }

    private static <K, V> Differencer<Map<K, V>> compareMap(Differencer<V> valueDifferencer) {
        return (Path path, Map<K, V> left, Map<K, V> right) -> {
            var commonKeys = left.keySet().stream().filter(right::containsKey).collect(Collectors.toSet());
            var leftOnlyKeys = left.keySet().stream().filter(Predicate.not(commonKeys::contains));
            var rightOnlyKeys = right.keySet().stream().filter(Predicate.not(commonKeys::contains));
            return Stream.concat(
                    diffInCommon(path, commonKeys, left, right, valueDifferencer),
                    Stream.concat(
                            diffLeftOnly(path, leftOnlyKeys, left::get),
                            diffRightOnly(path, rightOnlyKeys, right::get)));
        };
    }

    private static <K, V> Stream<Difference> diffRightOnly(Path path, Stream<K> keys, Function<K, V> right) {
        return keys.map(k -> Difference.rightOnly(path.sub(k.toString()), right.apply(k)));
    }

    private static <K, V> Stream<Difference> diffLeftOnly(Path path, Stream<K> keys, Function<K, V> left) {
        return keys.map(k -> Difference.leftOnly(path.sub(k.toString()), left.apply(k)));
    }

    private static <K, V> Stream<Difference> diffInCommon(
            Path path,
            Set<K> keys,
            Map<K, V> left,
            Map<K, V> right,
            Differencer<V> compareValues) {
        return keys.stream()
                .flatMap(k -> compareValues.differences(path.sub(k.toString()), left.get(k), right.get(k)));
    }

    private static <V> Differencer<V> testIfEqual(BiPredicate<V, V> equals, Differencer<V> differencer) {
        return (p, l, r) -> equals.test(l, r) ? Stream.empty() : differencer.differences(p, l, r);
    }


    private static <V> Differencer<V> diffAsValue() {
        return Diff.testIfEqual(Object::equals, Difference::unequal);
    }

    private static <V> Map<Integer, V> listToMap(List<V> list) {
        return IntStream.range(0, list.size()).boxed().collect(Collectors.toMap(Function.identity(), list::get));
    }
}
