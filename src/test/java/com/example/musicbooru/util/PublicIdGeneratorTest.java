package com.example.musicbooru.util;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PublicIdGeneratorTest {

    @Test
    void generate_returnsStringOfCorrectLength() {
        String id = PublicIdGenerator.generate(10, 3, s -> false);

        assertEquals(10, id.length());
    }

    @Test
    void generate_returnsBase62String() {
        String id = PublicIdGenerator.generate(100, 3, s -> false);

        assertTrue(id.matches("[A-Za-z0-9]+"));
    }

    @Test
    void generate_retriesOnCollisionAndReturnsOnSuccess() {
        Predicate<String> collisionCheck = mock();
        when(collisionCheck.test(any())).thenReturn(true, true, false);

        String id = PublicIdGenerator.generate(10, 3, collisionCheck);

        assertNotNull(id);
        verify(collisionCheck, times(3)).test(any());
    }

    @Test
    void generate_throwsRuntimeException_whenMaxRetriesReached() {
        Predicate<String> alwaysCollides = s -> true;

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> PublicIdGenerator.generate(10, 3, alwaysCollides)
        );

        assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    void generate_doesNotRetry_whenFirstAttemptSucceeds() {
        Predicate<String> collisionCheck = mock();
        when(collisionCheck.test(any())).thenReturn(false);

        PublicIdGenerator.generate(10, 3, collisionCheck);

        verify(collisionCheck, times(1)).test(any());
    }
}
