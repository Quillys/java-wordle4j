package ru.yandex.practicum.exceptions;

/**
 * Игрок повторно ввёл слово, которое уже проверял в этой партии: такой ход не даст новой информации.
 */
public class WordAlreadyUsedException extends WordleGameException {

    private static final long serialVersionUID = 1L;

    private final String word;

    public WordAlreadyUsedException(String word) {
        super("Слово «" + word + "» уже было в этой партии. Попробуйте другое.");
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}
