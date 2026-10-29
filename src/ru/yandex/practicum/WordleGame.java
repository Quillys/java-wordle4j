package ru.yandex.practicum;

import ru.yandex.practicum.exceptions.InvalidWordException;
import ru.yandex.practicum.exceptions.WordAlreadyUsedException;
import ru.yandex.practicum.exceptions.WordNotFoundInDictionaryException;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static ru.yandex.practicum.WordleDictionary.WORD_LENGTH;

/**
 * Партия Wordle: загаданное слово, оставшиеся попытки, история ходов и подбор подсказок.
 * <p>
 * Класс не работает с консолью: ввод и вывод — задача главного класса {@link Wordle}.
 * Ошибки игрока сообщаются игровыми исключениями, служебная информация пишется в лог.
 */
public class WordleGame {

    /** Сколько попыток даётся на партию. */
    public static final int MAX_STEPS = 6;

    private final WordleDictionary dictionary;
    private final Random random;
    private final PrintWriter log;
    private final String answer;

    private int stepsLeft = MAX_STEPS;
    private GameStatus status = GameStatus.IN_PROGRESS;

    private final Map<String, String> moves = new LinkedHashMap<>();        // слово → результат, в порядке ходов
    private final WordleConstraints constraints = new WordleConstraints();  // что известно о загаданном слове
    private final List<String> candidates;                                  // слова, не противоречащие ни одному ходу
    private final Set<String> suggestedWords = new HashSet<>();             // уже выданные подсказки

    /** Партия со случайным словом из словаря. */
    public WordleGame(WordleDictionary dictionary, PrintWriter log) {
        this(dictionary, new Random(), log);
    }

    /** Партия со случайным словом; генератор можно задать, чтобы игра была воспроизводимой. */
    public WordleGame(WordleDictionary dictionary, Random random, PrintWriter log) {
        this(dictionary, dictionary.getRandomWord(random), random, log);
    }

    /** Партия с заранее известным ответом — нужна для тестов. */
    WordleGame(WordleDictionary dictionary, String answer, Random random, PrintWriter log) {
        String normalizedAnswer = WordleDictionary.normalize(answer);
        if (!dictionary.contains(normalizedAnswer)) {
            throw new IllegalArgumentException("Загаданного слова «" + answer + "» нет в словаре");
        }
        this.dictionary = dictionary;
        this.random = random;
        this.log = log;
        this.answer = normalizedAnswer;
        this.candidates = new ArrayList<>(dictionary.getWords());
        log.printf("Новая партия: загадано слово «%s», слов в словаре: %d, попыток: %d%n",
                this.answer, dictionary.size(), MAX_STEPS);
    }

    /**
     * Делает ход: проверяет введённое слово и сравнивает его с загаданным.
     * Если слово не подходит по правилам, ход не засчитывается и выбрасывается игровое исключение.
     *
     * @param input слово в том виде, в каком его ввёл игрок
     * @return результат сравнения из символов {@code +}, {@code ^} и {@code -}
     * @throws InvalidWordException если ввод — не слово из пяти русских букв
     * @throws WordAlreadyUsedException если это слово уже проверялось в этой партии
     * @throws WordNotFoundInDictionaryException если слова нет в словаре
     */
    public String makeMove(String input)
            throws InvalidWordException, WordAlreadyUsedException, WordNotFoundInDictionaryException {
        checkInProgress();
        String word = WordleDictionary.normalize(input);
        validate(word);
        if (moves.containsKey(word)) {
            throw new WordAlreadyUsedException(word);
        }
        boolean guessed = word.equals(answer);  // ответ проверяем сразу: искать его в словаре незачем
        if (!guessed && !dictionary.contains(word)) {
            throw new WordNotFoundInDictionaryException(word);
        }

        String result = WordleDictionary.compare(word, answer);
        moves.put(word, result);
        stepsLeft--;
        log.printf("Ход %d: «%s», результат %s, осталось попыток: %d%n", getStepsMade(), word, result, stepsLeft);
        if (guessed) {
            status = GameStatus.WON;
        } else {
            updateCandidates(word, result);
            if (stepsLeft == 0) {
                status = GameStatus.LOST;
            }
        }
        checkState();
        if (isOver()) {
            log.printf("Партия окончена: %s, загаданное слово «%s»%n",
                    status == GameStatus.WON ? "победа" : "поражение", answer);
        }
        return result;
    }

    /**
     * Подбирает подсказку — случайное слово, которое не противоречит ни одному сделанному ходу.
     * Загаданное слово всегда остаётся среди вариантов, поэтому подсказка найдётся всегда.
     * Одно и то же слово не предлагается дважды, пока есть другие подходящие варианты.
     */
    public String suggestWord() {
        checkInProgress();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Не осталось подходящих слов, хотя слово «" + answer + "» не угадано");
        }
        List<String> freshCandidates = new ArrayList<>();
        for (String candidate : candidates) {
            if (!suggestedWords.contains(candidate)) {
                freshCandidates.add(candidate);
            }
        }
        // все подходящие слова уже предлагались: повторяем их, иначе подсказка потеряла бы ответ
        List<String> pool = freshCandidates.isEmpty() ? candidates : freshCandidates;
        String hint = pool.get(random.nextInt(pool.size()));
        suggestedWords.add(hint);
        log.printf("Подсказка: «%s» (подходящих слов: %d, из них ещё не предлагались: %d)%n",
                hint, candidates.size(), freshCandidates.size());
        return hint;
    }

    public boolean isOver() {
        return status != GameStatus.IN_PROGRESS;
    }

    public boolean isWon() {
        return status == GameStatus.WON;
    }

    public GameStatus getStatus() {
        return status;
    }

    public int getStepsLeft() {
        return stepsLeft;
    }

    public int getStepsMade() {
        return MAX_STEPS - stepsLeft;
    }

    public String getAnswer() {
        return answer;
    }

    /** История партии: слово → результат, в порядке ходов. */
    public Map<String, String> getMoves() {
        return Collections.unmodifiableMap(moves);
    }

    /** Слова, которые ещё могут оказаться ответом. */
    public List<String> getCandidates() {
        return Collections.unmodifiableList(candidates);
    }

    /** Общая проверка ввода — до поиска в словаре. */
    private static void validate(String word) throws InvalidWordException {
        if (word.isEmpty()) {
            throw new InvalidWordException("Введите слово из " + WORD_LENGTH + " букв.");
        }
        if (!WordleDictionary.consistsOfRussianLetters(word)) {
            throw new InvalidWordException("Слово должно состоять только из русских букв.");
        }
        if (word.length() != WORD_LENGTH) {
            throw new InvalidWordException("Нужно слово из " + WORD_LENGTH + " букв, а введено букв: "
                    + word.length() + ".");
        }
    }

    private void updateCandidates(String word, String result) {
        constraints.addMove(word, result);
        candidates.removeIf(candidate -> !constraints.matches(candidate));
        if (!constraints.matches(answer)) {
            throw new IllegalStateException("Фильтр подсказок отбросил загаданное слово «" + answer + "»: "
                    + constraints);
        }
        log.printf("Подходящих слов осталось: %d; известно о слове: %s%n", candidates.size(), constraints);
    }

    private void checkInProgress() {
        if (isOver()) {
            throw new IllegalStateException("Партия уже окончена: " + status);
        }
    }

    /** Проверка состояния после хода: если программа работает правильно, эти условия никогда не выполняются. */
    private void checkState() {
        if (stepsLeft < 0 || stepsLeft > MAX_STEPS) {
            throw new IllegalStateException("Неверное число оставшихся попыток: " + stepsLeft);
        }
        if (moves.size() != getStepsMade()) {
            throw new IllegalStateException("Ходов в истории " + moves.size() + ", а потрачено попыток: "
                    + getStepsMade());
        }
        if (status == GameStatus.IN_PROGRESS && stepsLeft == 0) {
            throw new IllegalStateException("Попытки закончились, а партия не завершена");
        }
    }
}
