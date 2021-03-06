package com.acs.wave.utils.cache;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CacheTest {

    @Mock
    private Supplier<String> provider;

    @Mock
    private Supplier<Long> timeProvider;

    @Test
    public void normal_mapping() {
        Cache<String> cache = new CacheBuilder<String>().build();
        String expected = "World!";
        cache.set(expected);
        String result = cache.get();
        assertEquals(expected, result);
    }

    @Test
    public void no_entry_null() {
        Cache<String> cache = new CacheBuilder<String>().build();
        String result = cache.get();
        assertEquals(null, result);
    }

    @Test
    public void no_entry_but_provider() {
        String expected = "World!";
        when(provider.get()).thenReturn(expected);
        Cache<String> cache = new CacheBuilder<String>().withProvider(provider).build();
        String result = cache.get();
        assertEquals(expected, result);
    }

    @Test
    public void no_timeout_pass() {
        when(timeProvider.get()).thenReturn(1L, 2L);
        String expected = "World!";
        Cache<String> cache = new Cache<>(10L, TimeUnit.MILLISECONDS, null, timeProvider);
        cache.set(expected);
        String result = cache.get();
        assertEquals(expected, result);
    }

    @Test
    public void timeout_pass() {
        when(timeProvider.get()).thenReturn(1L, 1000L);
        String expected = "World!";
        Cache<String> cache = new Cache<>(10L, TimeUnit.MILLISECONDS, null, timeProvider);
        cache.set(expected);
        String result = cache.get();
        assertEquals(null, result);
    }
}
