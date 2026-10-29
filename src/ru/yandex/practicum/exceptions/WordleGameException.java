package ru.yandex.practicum.exceptions;

/**
 * Базовое исключение игровых ситуаций: некорректное слово, слово не из словаря и т.п.
 * Сообщение предназначено для игрока, а ход при таком исключении не засчитывается.
 */
public class WordleGameException extends Exception {

    private static final long serialVersionUID = 1L;

    public WordleGameException(String message) {
        super(message);
    }
}
