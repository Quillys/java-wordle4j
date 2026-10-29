package ru.yandex.practicum;

import ru.yandex.practicum.exceptions.DictionaryLoadException;
import ru.yandex.practicum.exceptions.EmptyDictionaryException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Загрузчик словаря. Отвечает только за работу с файлом: кодировку и формат.
 * Формат файла — текст в UTF-8, по одному слову на строку, порядок слов любой.
 * Отбор слов, подходящих для игры, выполняет {@link WordleDictionary}.
 */
public class WordleDictionaryLoader {

    private static final char BYTE_ORDER_MARK = '﻿';  // некоторые редакторы ставят её в начало UTF-8 файла

    private final PrintWriter log;

    public WordleDictionaryLoader(PrintWriter log) {
        this.log = log;
    }

    /**
     * Читает файл словаря целиком и строит по нему словарь игры.
     *
     * @throws DictionaryLoadException если файл не найден или его не удалось прочитать
     * @throws EmptyDictionaryException если в файле нет слов, подходящих для игры
     */
    public WordleDictionary load(String fileName) throws DictionaryLoadException, EmptyDictionaryException {
        log.println("Загрузка словаря из файла " + fileName);
        List<String> lines = readLines(fileName);
        log.println("Прочитано строк: " + lines.size());
        if (lines.isEmpty()) {
            throw new EmptyDictionaryException("Файл словаря пуст: " + fileName);
        }
        return new WordleDictionary(lines, log);
    }

    private List<String> readLines(String fileName) throws DictionaryLoadException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new DictionaryLoadException("Файл словаря не найден: " + fileName, e);
        } catch (IOException e) {
            throw new DictionaryLoadException("Не удалось прочитать файл словаря: " + fileName, e);
        }
        removeByteOrderMark(lines);
        return lines;
    }

    private static void removeByteOrderMark(List<String> lines) {
        if (!lines.isEmpty() && lines.get(0).startsWith(String.valueOf(BYTE_ORDER_MARK))) {
            lines.set(0, lines.get(0).substring(1));
        }
    }
}
