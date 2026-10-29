package ru.yandex.practicum;

import ru.yandex.practicum.exceptions.EmptyDictionaryException;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Словарь игры — слова, подходящие для Wordle: ровно пять русских букв в нижнем регистре, «ё» заменена на «е».
 * <p>
 * Его методы похожи на методы списка, но учитывают особенности игры: например, {@link #contains(String)}
 * не зависит от регистра и буквы «ё». Кроме того, здесь собраны статические методы для работы со словами:
 * нормализация, проверка и побуквенное сравнение догадки с ответом.
 */
public class WordleDictionary {

    /** Длина слова в игре. */
    public static final int WORD_LENGTH = 5;

    /** Буква есть в загаданном слове и стоит на своём месте. */
    public static final char CORRECT_POSITION = '+';

    /** Буква есть в загаданном слове, но в другом месте. */
    public static final char WRONG_POSITION = '^';

    /** Буквы нет в загаданном слове (или все её вхождения уже отмечены другими символами). */
    public static final char ABSENT = '-';

    private final List<String> words;   // в порядке файла; список нужен для выбора случайного слова по индексу
    private final Set<String> wordSet;  // те же слова; проверка «есть ли слово в словаре» за O(1)

    /**
     * Строит словарь из «сырых» строк: нормализует их, оставляет только слова из пяти русских букв
     * и убирает повторы.
     *
     * @throws EmptyDictionaryException если ни одна строка не подошла
     */
    public WordleDictionary(Collection<String> rawWords, PrintWriter log) throws EmptyDictionaryException {
        Set<String> uniqueWords = new LinkedHashSet<>();  // убирает повторы и сохраняет порядок слов
        int rejected = 0;
        for (String rawWord : rawWords) {
            String word = normalize(rawWord);
            if (isValidWord(word)) {
                uniqueWords.add(word);
            } else {
                rejected++;
            }
        }
        if (uniqueWords.isEmpty()) {
            throw new EmptyDictionaryException("В словаре нет ни одного слова из " + WORD_LENGTH + " русских букв");
        }
        words = List.copyOf(uniqueWords);
        wordSet = Set.copyOf(uniqueWords);
        int duplicates = rawWords.size() - rejected - words.size();
        log.printf("Словарь игры: %d слов (всего строк: %d, не подходят для игры: %d, повторы: %d)%n",
                words.size(), rawWords.size(), rejected, duplicates);
    }

    /**
     * Проверяет, есть ли слово в словаре. Слово сначала нормализуется,
     * поэтому регистр, «ё» и пробелы по краям значения не имеют.
     */
    public boolean contains(String word) {
        return wordSet.contains(normalize(word));
    }

    public int size() {
        return words.size();
    }

    /** Все слова словаря; список нельзя изменить. */
    public List<String> getWords() {
        return words;
    }

    /** Случайное слово из словаря. */
    public String getRandomWord(Random random) {
        return words.get(random.nextInt(words.size()));
    }

    /**
     * Приводит слово к виду, в котором оно хранится в словаре:
     * без пробелов по краям, в нижнем регистре и с «е» вместо «ё».
     */
    public static String normalize(String word) {
        Objects.requireNonNull(word, "Слово не может быть null");
        return word.strip()
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е');
    }

    /** Состоит ли строка только из строчных русских букв. Пустая строка словом не считается. */
    public static boolean consistsOfRussianLetters(String word) {
        if (word.isEmpty()) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);
            if (letter < 'а' || letter > 'я') {
                return false;
            }
        }
        return true;
    }

    /** Подходит ли нормализованное слово для игры: ровно пять русских букв. */
    public static boolean isValidWord(String word) {
        return word.length() == WORD_LENGTH && consistsOfRussianLetters(word);
    }

    /**
     * Сравнивает догадку с загаданным словом по правилам Wordle.
     * Возвращает строку из пяти символов: {@code +} — буква на своём месте,
     * {@code ^} — буква есть, но в другом месте, {@code -} — буквы нет.
     * <p>
     * Повторяющиеся буквы учитываются честно: каждая буква ответа «закрывает» не больше одной буквы догадки,
     * и сначала засчитываются совпадения на своих местах. Например, для ответа «герой» и догадки «отбор»
     * результат будет {@code ---+^}: единственная «о» ответа уже совпала на четвёртой позиции,
     * поэтому первая «о» догадки получает {@code -}.
     */
    public static String compare(String guess, String answer) {
        checkLength(guess);
        checkLength(answer);
        // буквы ответа, не совпавшие по позициям: только они могут дать «^»
        Map<Character, Integer> unmatchedLetters = new HashMap<>();
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guess.charAt(i) != answer.charAt(i)) {
                unmatchedLetters.merge(answer.charAt(i), 1, Integer::sum);
            }
        }
        StringBuilder result = new StringBuilder(WORD_LENGTH);
        for (int i = 0; i < WORD_LENGTH; i++) {
            char letter = guess.charAt(i);
            if (letter == answer.charAt(i)) {
                result.append(CORRECT_POSITION);
            } else if (unmatchedLetters.getOrDefault(letter, 0) > 0) {
                result.append(WRONG_POSITION);
                unmatchedLetters.merge(letter, -1, Integer::sum);
            } else {
                result.append(ABSENT);
            }
        }
        return result.toString();
    }

    private static void checkLength(String word) {
        if (word.length() != WORD_LENGTH) {
            throw new IllegalArgumentException("Для сравнения нужно слово из " + WORD_LENGTH + " букв: «" + word + "»");
        }
    }
}
