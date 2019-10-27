package tea;

import com.example.tutorial.AddressBookProtos.AddressBook;
import com.example.tutorial.AddressBookProtos.Person;
import tea.comparator.Difference;
import tea.comparator.Path;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tea.comparator.RecursiveDifferencer.*;


public class Diff {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: Diff <filename> <filename>");
            return;
        }

        var bono = Person.newBuilder()
                .setId(2)
                .setName("bono")
                .addPhones(Person.PhoneNumber.newBuilder()
                        .setType(Person.PhoneType.MOBILE)
                        .setNumber("0800-123-231")).build();
        var bono2 = bono.toBuilder()
                .clearPhones()
                .addPhones(Person.PhoneNumber.newBuilder()
                        .setType(Person.PhoneType.MOBILE)
                        .setNumber("0800-132-231"));
        var john = Person.newBuilder()
                .setId(3)
                .setName("john").build();
        var carla = Person.newBuilder()
                .setId(4)
                .setName("carla").build();


        var left = AddressBook.newBuilder()
                .addPeople(bono)
                .addPeople(john)
                .addPeople(carla).build();
        var right = AddressBook.newBuilder()
                .addPeople(bono2)
                .addPeople(carla).build();

        System.out.println(
                "Diff:\n" + compareAddressBook(left, right)
                        .map(Object::toString)
                        .collect(Collectors.joining("\n", "\n", "\n")));

/*        try {
            var left = AddressBook.parseFrom(new FileInputStream(args[0]));
            var right = AddressBook.parseFrom(new FileInputStream(args[1]));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }

    private static Stream<Difference> compareAddressBook(AddressBook left, AddressBook right) {
        var diffPhone = compose(
                diffChildWithEquals(Person.PhoneNumber::getNumber, "number"),
                diffChildWithEquals(Person.PhoneNumber::getType, "type"));

        var personDifferencer = compose(
                diffChildWithEquals(Person::getEmail, "email"),
                diffChildWithEquals(Person::getId, "id"),
                diffChild(Person::getPhonesList, "phones", diffListElements(diffPhone)));

        return compose(
                diffChild(AddressBook::getPeopleList, "people", diffListElementsAsMap(personDifferencer, Person::getName)))
                .differences(Path.root(), left, right);
    }
}
