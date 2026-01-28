package analyser;

import analyser.types.Type;

public record Declare(Type type, String name, String value, String location) {
}
