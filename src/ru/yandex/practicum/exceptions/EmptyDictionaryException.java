package ru.yandex.practicum.exceptions;

/**
 * В словаре нет ни одного слова, подходящего для игры.
 */
public class EmptyDictionaryException extends WordleProgramException {

    private static final long serialVersionUID = 1L;

    public EmptyDictionaryException(String message) {
        super(message);
    }
}
