package ru.yandex.practicum.exceptions;

/**
 * Ввод не похож на слово для игры: пустая строка, не русские буквы или неверная длина.
 */
public class InvalidWordException extends WordleGameException {

    private static final long serialVersionUID = 1L;

    public InvalidWordException(String message) {
        super(message);
    }
}
