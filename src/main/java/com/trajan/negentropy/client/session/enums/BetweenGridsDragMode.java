package com.trajan.negentropy.client.session.enums;//package com.trajan.negentropy.client.session.enums;
//
//import lombok.Getter;
//import lombok.experimental.Accessors;
//
//import java.util.Arrays;
//import java.util.Optional;
//
//@Getter
//@Accessors(fluent = true)
//public enum BetweenGriddsDragMode {
//    COPY("Copy"),
//    MOVE("Move");
//
//    private final String value;
//
//    BetweenGridsDragMode(String value) {
//        this.value = value;
//    }
//
//    public static Optional<BetweenGridsDragMode> get(String string) {
//        return Arrays.stream(BetweenGridsDragMode.values())
//                .filter(env -> env.value.equals(string))
//                .findFirst();
//    }
//}
