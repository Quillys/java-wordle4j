package ru.yandex.practicum.exceptions;

/**
 * Файл словаря не найден или его не удалось прочитать.
 */
public class DictionaryLoadException extends WordleProgramException {

    private static final long serialVersionUID = 1L;

    public DictionaryLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
