package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.yandex.practicum.exceptions.DictionaryLoadException;
import ru.yandex.practicum.exceptions.EmptyDictionaryException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordleDictionaryLoaderTest {

    private static WordleDictionaryLoader loader;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void createLoader() {
        // в тестах лог пишется в консоль, а не в файл
        loader = new WordleDictionaryLoader(new PrintWriter(System.out, true));
    }

    @Test
    void shouldLoadDictionaryFromRepository() throws Exception {
        WordleDictionary dictionary = loader.load(Wordle.DICTIONARY_FILE_NAME);

        assertTrue(dictionary.size() > 1000, "в словаре слишком мало слов: " + dictionary.size());
        assertTrue(dictionary.contains("герой"));
        assertTrue(dictionary.contains("гонец"));
        for (String word : dictionary.getWords()) {
            assertTrue(WordleDictionary.isValidWord(word), "неподходящее слово в словаре: " + word);
        }
    }

    @Test
    void shouldReadUtf8FileAndKeepOnlySuitableWords() throws Exception {
        Path file = writeFile("Герой", "ёжики", "кот", "", "а-ля фуршет", "  гонец  ", "ГЕРОЙ");

        assertEquals(List.of("герой", "ежики", "гонец"), loader.load(file.toString()).getWords());
    }

    @Test
    void shouldIgnoreByteOrderMark() throws Exception {
        Path file = writeFile("﻿герой", "гонец");

        assertEquals(List.of("герой", "гонец"), loader.load(file.toString()).getWords());
    }

    @Test
    void shouldSupportWindowsLineEndings() throws Exception {
        Path file = tempDir.resolve("windows.txt");
        Files.write(file, "герой\r\nгонец\r\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("герой", "гонец"), loader.load(file.toString()).getWords());
    }

    @Test
    void shouldThrowWhenFileNotFound() {
        String missingFile = tempDir.resolve("missing.txt").toString();

        DictionaryLoadException exception = assertThrows(DictionaryLoadException.class,
                () -> loader.load(missingFile));
        assertTrue(exception.getCause() instanceof FileNotFoundException);
    }

    @Test
    void shouldThrowWhenFileIsEmpty() throws IOException {
        Path file = writeFile();

        assertThrows(EmptyDictionaryException.class, () -> loader.load(file.toString()));
    }

    @Test
    void shouldThrowWhenFileHasNoSuitableWords() throws IOException {
        Path file = writeFile("кот", "hello", "абажурчик");

        assertThrows(EmptyDictionaryException.class, () -> loader.load(file.toString()));
    }

    private Path writeFile(String... lines) throws IOException {
        Path file = tempDir.resolve("words.txt");
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
        return file;
    }
}
