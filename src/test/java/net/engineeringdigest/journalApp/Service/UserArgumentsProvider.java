package net.engineeringdigest.journalApp.Service;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import net.engineeringdigest.journalApp.entity.User;

import java.util.stream.Stream;

public class UserArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
        return Stream.of(
                Arguments.of(new User("Prince", "Prince")),
                Arguments.of(new User("Aman", "Aman"))
        );
    }
}
