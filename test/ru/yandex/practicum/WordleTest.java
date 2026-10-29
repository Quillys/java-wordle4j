package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.exceptions.EmptyDictionaryException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты игрового цикла главного класса: вместо консоли подставляются строка ввода и буфер вывода.
 */
class WordleTest {

    private static PrintWriter log;
    private static WordleDictionary dictionary;

    private WordleGame game;

    @BeforeAll
    static void createDictionary() throws EmptyDictionaryException {
        // в тестах лог пишется в консоль, а не в файл
        log = new PrintWriter(System.out, true);
        dictionary = new WordleDictionary(
                List.of("герой", "гонец", "голос", "город", "отбор", "сокол", "мышка", "книга", "пенал", "щетка"), log);
    }

    @BeforeEach
    void createGame() {
        game = new WordleGame(dictionary, "герой", new Random(3), log);
    }

    @Test
    void shouldPrintResultUnderEachWordAndAnswerAtTheEnd() {
        String output = play("гонец\nгерой\n");

        assertTrue(output.contains("> +^-^-"));
        assertTrue(output.contains("> +++++"));
        assertTrue(output.contains("Победа! Слово угадано с 2-й попытки."));
        assertTrue(output.contains("Загаданное слово: герой"));
        assertEquals(GameStatus.WON, game.getStatus());
    }

    @Test
    void invalidWordsShouldBeExplainedAndNotCounted() {
        String output = play("hello\nкот\nабвгд\nгонец\nГОНЕЦ\nгерой\n");

        assertTrue(output.contains("Слово должно состоять только из русских букв."));
        assertTrue(output.contains("Нужно слово из 5 букв, а введено букв: 3."));
        assertTrue(output.contains("Слова «абвгд» нет в словаре."));
        assertTrue(output.contains("Слово «гонец» уже было в этой партии."));
        assertEquals(2, game.getStepsMade());
        assertTrue(game.isWon());
    }

    @Test
    void sixWrongWordsShouldLoseGame() {
        String output = play("гонец\nголос\nгород\nотбор\nсокол\nмышка\n");

        assertTrue(output.contains("Попытка 6 из 6"));
        assertTrue(output.contains("Попытки закончились — слово не угадано."));
        assertTrue(output.contains("Загаданное слово: герой"));
        assertEquals(GameStatus.LOST, game.getStatus());
    }

    @Test
    void emptyLineShouldMakeComputerMove() {
        String output = play("\n");

        assertTrue(output.contains("(подсказка компьютера)"));
        assertEquals(1, game.getStepsMade());
    }

    @Test
    void computerShouldFinishGameWhenPlayerOnlyPressesEnter() {
        String output = play("\n".repeat(WordleGame.MAX_STEPS));

        assertTrue(game.isOver());
        assertTrue(output.contains("Загаданное слово: герой"));
    }

    @Test
    void gameShouldStopWhenInputEnds() {
        String output = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> play("гонец\n"));

        assertTrue(output.contains("Ввод закончился — партия прервана."));
        assertTrue(output.contains("Загаданное слово: герой"));
        assertFalse(game.isOver());
        assertEquals(1, game.getStepsMade());
    }

    private String play(String input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        Wordle.play(game, new Scanner(input), out, log);
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
