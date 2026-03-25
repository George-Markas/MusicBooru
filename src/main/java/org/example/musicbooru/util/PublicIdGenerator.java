package org.example.musicbooru.util;

import java.security.SecureRandom;

public class PublicIdGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        StringBuilder stringBuilder  = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            stringBuilder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        return stringBuilder.toString();
    }
}
