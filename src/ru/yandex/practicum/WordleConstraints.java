package ru.yandex.practicum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.yandex.practicum.WordleDictionary.WORD_LENGTH;

/**
 * Всё, что известно о загаданном слове по результатам сделанных ходов.
 * По этим знаниям отбираются подсказки: слово подходит, только если оно не противоречит ни одному ходу.
 * <p>
 * Как читается символ результата для буквы догадки на позиции i:
 * <ul>
 *     <li>{@code +} — на позиции i стоит именно эта буква;</li>
 *     <li>{@code ^} — буква есть в слове, но не на позиции i;</li>
 *     <li>{@code -} — на позиции i этой буквы нет, а всего в слове её столько, сколько раз она отмечена
 *     в этом же ходе символами {@code +} и {@code ^}. Если таких отметок нет — буквы в слове нет совсем.</li>
 * </ul>
 */
public class WordleConstraints {

    private static final char UNKNOWN = '_';

    private final char[] knownLetters = new char[WORD_LENGTH];               // нужные буквы на нужных местах
    private final List<Set<Character>> excludedLetters = new ArrayList<>();  // буквы, которых точно нет на позиции
    private final Map<Character, Integer> minCounts = new HashMap<>();       // обязательные буквы: минимум вхождений
    private final Map<Character, Integer> maxCounts = new HashMap<>();       // максимум вхождений; 0 — буквы нет

    public WordleConstraints() {
        Arrays.fill(knownLetters, UNKNOWN);
        for (int i = 0; i < WORD_LENGTH; i++) {
            excludedLetters.add(new HashSet<>());
        }
    }

    /**
     * Учитывает результат очередного хода.
     *
     * @param guess  нормализованное слово, которое проверял игрок
     * @param result результат сравнения этого слова с ответом, например {@code +^-^-}
     */
    public void addMove(String guess, String result) {
        if (guess.length() != WORD_LENGTH || result.length() != WORD_LENGTH) {
            throw new IllegalArgumentException("Слово и результат хода должны быть длиной " + WORD_LENGTH
                    + ": «" + guess + "», «" + result + "»");
        }
        Map<Character, Integer> presentCounts = new HashMap<>();  // сколько раз буква отмечена «+» или «^»
        Set<Character> absentMarked = new HashSet<>();            // буквы, хотя бы раз отмеченные «-»
        for (int i = 0; i < WORD_LENGTH; i++) {
            char letter = guess.charAt(i);
            char mark = result.charAt(i);
            switch (mark) {
                case WordleDictionary.CORRECT_POSITION -> {
                    setKnownLetter(i, letter);
                    presentCounts.merge(letter, 1, Integer::sum);
                }
                case WordleDictionary.WRONG_POSITION -> {
                    excludedLetters.get(i).add(letter);
                    presentCounts.merge(letter, 1, Integer::sum);
                }
                case WordleDictionary.ABSENT -> {
                    excludedLetters.get(i).add(letter);
                    absentMarked.add(letter);
                }
                default -> throw new IllegalArgumentException("Неизвестный символ в результате хода: " + mark);
            }
        }
        for (Map.Entry<Character, Integer> entry : presentCounts.entrySet()) {
            minCounts.merge(entry.getKey(), entry.getValue(), Math::max);
        }
        for (char letter : absentMarked) {
            maxCounts.merge(letter, presentCounts.getOrDefault(letter, 0), Math::min);
        }
        checkConsistency();
    }

    /**
     * Не противоречит ли слово ни одному учтённому ходу. Загаданное слово подходит всегда.
     */
    public boolean matches(String word) {
        if (!WordleDictionary.isValidWord(word)) {
            return false;
        }
        // 1. отсеиваем слова с буквами, которых нет в ответе, и с лишними повторами букв
        for (Map.Entry<Character, Integer> entry : maxCounts.entrySet()) {
            if (countLetter(word, entry.getKey()) > entry.getValue()) {
                return false;
            }
        }
        // 2. оставляем слова, в которых есть все обязательные буквы
        for (Map.Entry<Character, Integer> entry : minCounts.entrySet()) {
            if (countLetter(word, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        // 3. нужные буквы стоят на нужных местах, и ни одна буква не стоит там, где её точно нет
        for (int i = 0; i < WORD_LENGTH; i++) {
            char letter = word.charAt(i);
            if (knownLetters[i] != UNKNOWN && knownLetters[i] != letter) {
                return false;
            }
            if (excludedLetters.get(i).contains(letter)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder description = new StringBuilder();
        description.append("шаблон ").append(knownLetters)
                .append(", минимум вхождений ").append(minCounts)
                .append(", максимум вхождений ").append(maxCounts)
                .append(", исключены по позициям ").append(excludedLetters);
        return description.toString();
    }

    private void setKnownLetter(int position, char letter) {
        if (knownLetters[position] != UNKNOWN && knownLetters[position] != letter) {
            throw new IllegalStateException("Противоречие: на позиции " + (position + 1) + " уже известна буква «"
                    + knownLetters[position] + "», а не «" + letter + "»");
        }
        knownLetters[position] = letter;
    }

    /** Результаты честных сравнений не могут противоречить друг другу, иначе это ошибка программы. */
    private void checkConsistency() {
        for (Map.Entry<Character, Integer> entry : minCounts.entrySet()) {
            int maxCount = maxCounts.getOrDefault(entry.getKey(), WORD_LENGTH);
            if (entry.getValue() > maxCount) {
                throw new IllegalStateException("Противоречие: буква «" + entry.getKey()
                        + "» должна встречаться не меньше " + entry.getValue() + " и не больше " + maxCount + " раз");
            }
        }
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (knownLetters[i] != UNKNOWN && excludedLetters.get(i).contains(knownLetters[i])) {
                throw new IllegalStateException("Противоречие: буква «" + knownLetters[i]
                        + "» и стоит, и не стоит на позиции " + (i + 1));
            }
        }
    }

    private static int countLetter(String word, char letter) {
        int count = 0;
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == letter) {
                count++;
            }
        }
        return count;
    }
}
