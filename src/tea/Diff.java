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

        try {
            var left = AddressBook.parseFrom(new FileInputStream(args[0]));
            var right = AddressBook.parseFrom(new FileInputStream(args[1]));

            System.out.println(
                    "Diff:\n" + compareAddressBook(left, right)
                            .map(Object::toString)
                            .collect(Collectors.joining("\n", "\n", "\n")));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                diffChild(AddressBook::getPeopleList, "people", diffListElements(personDifferencer)))
                .differences(Path.root(), left, right);
    }
}
