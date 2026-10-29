package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.exceptions.InvalidWordException;
import ru.yandex.practicum.exceptions.WordAlreadyUsedException;
import ru.yandex.practicum.exceptions.WordNotFoundInDictionaryException;
import ru.yandex.practicum.exceptions.WordleGameException;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordleGameTest {

    private static final List<String> WORDS = List.of(
            "герой", "гонец", "голос", "город", "отбор", "сокол", "мышка", "книга", "пенал", "щетка");

    private static PrintWriter log;
    private static WordleDictionary dictionary;      // маленький словарь, где результаты легко посчитать вручную
    private static WordleDictionary fullDictionary;  // настоящий словарь игры

    private WordleGame game;

    @BeforeAll
    static void createDictionaries() throws Exception {
        // в тестах лог пишется в консоль, а не в файл
        log = new PrintWriter(System.out, true);
        dictionary = new WordleDictionary(WORDS, log);
        fullDictionary = new WordleDictionaryLoader(log).load(Wordle.DICTIONARY_FILE_NAME);
    }

    @BeforeEach
    void createGame() {
        game = new WordleGame(dictionary, "герой", new Random(1), log);
    }

    @Test
    void newGameShouldHaveAllStepsAndNoMoves() {
        assertEquals(WordleGame.MAX_STEPS, game.getStepsLeft());
        assertEquals(0, game.getStepsMade());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertFalse(game.isOver());
        assertTrue(game.getMoves().isEmpty());
        assertEquals(WORDS, game.getCandidates());
    }

    @Test
    void moveShouldReturnResultAndSpendStep() throws WordleGameException {
        assertEquals("+^-^-", game.makeMove("гонец"));

        assertEquals(WordleGame.MAX_STEPS - 1, game.getStepsLeft());
        assertEquals(Map.of("гонец", "+^-^-"), game.getMoves());
        assertFalse(game.isOver());
    }

    @Test
    void guessingAnswerShouldWinGame() throws WordleGameException {
        game.makeMove("гонец");

        assertEquals("+++++", game.makeMove("герой"));
        assertTrue(game.isWon());
        assertTrue(game.isOver());
        assertEquals(2, game.getStepsMade());
    }

    @Test
    void inputShouldBeNormalized() throws WordleGameException {
        assertEquals("-+---", game.makeMove(" ЩЁТКА "));
        assertTrue(game.getMoves().containsKey("щетка"));

        assertEquals("+++++", game.makeMove("Герой"));
        assertTrue(game.isWon());
    }

    @Test
    void sixWrongWordsShouldLoseGame() throws WordleGameException {
        for (String word : List.of("гонец", "голос", "город", "отбор", "сокол", "мышка")) {
            game.makeMove(word);
        }

        assertEquals(GameStatus.LOST, game.getStatus());
        assertEquals(0, game.getStepsLeft());
        assertTrue(game.isOver());
        assertFalse(game.isWon());
    }

    @Test
    void guessOnLastStepShouldWinGame() throws WordleGameException {
        for (String word : List.of("гонец", "голос", "город", "отбор", "сокол")) {
            game.makeMove(word);
        }
        game.makeMove("герой");

        assertEquals(GameStatus.WON, game.getStatus());
        assertEquals(0, game.getStepsLeft());
    }

    @Test
    void finishedGameShouldNotAcceptMovesAndHints() throws WordleGameException {
        game.makeMove("герой");

        assertThrows(IllegalStateException.class, () -> game.makeMove("гонец"));
        assertThrows(IllegalStateException.class, () -> game.suggestWord());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "hello", "кот", "героиня", "гер0й", "ля-ля", "г е р о й", "гeрой"})
    void invalidInputShouldNotSpendStep(String input) {
        assertThrows(InvalidWordException.class, () -> game.makeMove(input));

        assertEquals(WordleGame.MAX_STEPS, game.getStepsLeft());
        assertTrue(game.getMoves().isEmpty());
    }

    @Test
    void invalidInputShouldBeExplained() {
        assertEquals("Слово должно состоять только из русских букв.",
                assertThrows(InvalidWordException.class, () -> game.makeMove("hello")).getMessage());
        assertEquals("Нужно слово из 5 букв, а введено букв: 3.",
                assertThrows(InvalidWordException.class, () -> game.makeMove("кот")).getMessage());
    }

    @Test
    void wordNotFromDictionaryShouldNotSpendStep() {
        WordNotFoundInDictionaryException exception = assertThrows(WordNotFoundInDictionaryException.class,
                () -> game.makeMove("Абвгд"));

        assertEquals("абвгд", exception.getWord());
        assertEquals(WordleGame.MAX_STEPS, game.getStepsLeft());
    }

    @Test
    void repeatedWordShouldNotSpendStep() throws WordleGameException {
        game.makeMove("гонец");

        assertThrows(WordAlreadyUsedException.class, () -> game.makeMove("ГОНЕЦ"));
        assertEquals(WordleGame.MAX_STEPS - 1, game.getStepsLeft());
    }

    @Test
    void answerShouldBeTakenFromDictionary() {
        assertThrows(IllegalArgumentException.class, () -> new WordleGame(dictionary, "абвгд", new Random(), log));
        for (int seed = 0; seed < 20; seed++) {
            assertTrue(dictionary.contains(new WordleGame(dictionary, new Random(seed), log).getAnswer()));
        }
    }

    @Test
    void historyAndCandidatesShouldBeUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> game.getMoves().put("гонец", "-----"));
        assertThrows(UnsupportedOperationException.class, () -> game.getCandidates().clear());
    }

    @Test
    void firstHintCanBeAnyWordAndShouldNotSpendStep() {
        String hint = game.suggestWord();

        assertTrue(dictionary.contains(hint));
        assertEquals(WordleGame.MAX_STEPS, game.getStepsLeft());
    }

    @Test
    void candidatesShouldShrinkButKeepAnswer() throws WordleGameException {
        game.makeMove("мышка");  // -----: отпадают слова с буквами «м», «ы», «ш», «к», «а»
        assertEquals(List.of("герой", "гонец", "голос", "город", "отбор"), game.getCandidates());

        game.makeMove("гонец");  // +^-^-: подходит только ответ
        assertEquals(List.of("герой"), game.getCandidates());
        assertEquals("герой", game.suggestWord());
    }

    @Test
    void hintsShouldNotRepeatWhileThereAreOtherCandidates() {
        Set<String> hints = new HashSet<>();
        for (int i = 0; i < WORDS.size(); i++) {
            assertTrue(hints.add(game.suggestWord()), "подсказка повторилась: " + hints);
        }
        assertEquals(new HashSet<>(WORDS), hints);

        // все варианты уже предлагались — подсказка всё равно находится, ответ не теряется
        assertTrue(dictionary.contains(game.suggestWord()));
    }

    @Test
    void candidatesShouldBeExactlyWordsConsistentWithAllMoves() throws WordleGameException {
        Random random = new Random(5);
        PrintWriter silentLog = new PrintWriter(Writer.nullWriter());  // сотни ходов не засоряют вывод тестов
        for (int i = 0; i < 15; i++) {
            WordleGame randomGame = new WordleGame(fullDictionary, random, silentLog);
            List<String> guesses = new ArrayList<>(fullDictionary.getWords());
            Collections.shuffle(guesses, random);

            for (String guess : guesses) {
                if (randomGame.isOver()) {
                    break;
                }
                randomGame.makeMove(guess);
                if (!randomGame.isOver()) {
                    assertEquals(consistentWords(randomGame.getMoves()), randomGame.getCandidates());
                }
            }
        }
    }

    @Test
    void computerShouldPlayWholeGameUsingOnlyHints() throws WordleGameException {
        Random random = new Random(2026);
        PrintWriter silentLog = new PrintWriter(Writer.nullWriter());  // сотни партий не засоряют вывод тестов
        int games = 300;
        int wins = 0;
        for (int i = 0; i < games; i++) {
            WordleGame autoGame = new WordleGame(fullDictionary, random, silentLog);
            while (!autoGame.isOver()) {
                String hint = autoGame.suggestWord();
                assertFalse(autoGame.getMoves().containsKey(hint), "подсказка повторяет сделанный ход");
                for (Map.Entry<String, String> move : autoGame.getMoves().entrySet()) {
                    assertEquals(move.getValue(), WordleDictionary.compare(move.getKey(), hint),
                            "подсказка «" + hint + "» противоречит ходу «" + move.getKey() + "»");
                }
                autoGame.makeMove(hint);
                assertTrue(autoGame.isOver() || autoGame.getCandidates().contains(autoGame.getAnswer()),
                        "среди вариантов потерялся ответ «" + autoGame.getAnswer() + "»");
            }
            if (autoGame.isWon()) {
                wins++;
            }
        }
        // подсказки выбираются случайно, поэтому иногда шести попыток не хватает, но такое бывает редко
        assertTrue(wins >= games * 0.9, "компьютер выиграл только " + wins + " партий из " + games);
    }

    private static List<String> consistentWords(Map<String, String> moves) {
        List<String> result = new ArrayList<>();
        for (String word : fullDictionary.getWords()) {
            boolean consistent = true;
            for (Map.Entry<String, String> move : moves.entrySet()) {
                if (!WordleDictionary.compare(move.getKey(), word).equals(move.getValue())) {
                    consistent = false;
                    break;
                }
            }
            if (consistent) {
                result.add(word);
            }
        }
        return result;
    }
}
