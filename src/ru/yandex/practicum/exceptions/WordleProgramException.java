package ru.yandex.practicum.exceptions;

/**
 * Базовое исключение ошибок работы программы, не связанных с ходами игрока:
 * проблемы с файлом словаря, пустой словарь и т.п. Подробности таких ошибок пишутся в лог.
 */
public class WordleProgramException extends Exception {

    private static final long serialVersionUID = 1L;

    public WordleProgramException(String message) {
        super(message);
    }

    public WordleProgramException(String message, Throwable cause) {
        super(message, cause);
    }
}
