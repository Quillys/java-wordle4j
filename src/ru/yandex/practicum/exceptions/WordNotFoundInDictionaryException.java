package ru.yandex.practicum.exceptions;

/**
 * Слово правильное по форме, но его нет в словаре игры.
 */
public class WordNotFoundInDictionaryException extends WordleGameException {

    private static final long serialVersionUID = 1L;

    private final String word;

    public WordNotFoundInDictionaryException(String word) {
        super("Слова «" + word + "» нет в словаре. Попробуйте другое.");
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}
