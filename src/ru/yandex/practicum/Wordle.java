package ru.yandex.practicum;

import ru.yandex.practicum.exceptions.WordleGameException;
import ru.yandex.practicum.exceptions.WordleProgramException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import static ru.yandex.practicum.WordleDictionary.WORD_LENGTH;
import static ru.yandex.practicum.WordleGame.MAX_STEPS;

/**
 * Главный класс игры: готовит лог-файл и словарь, создаёт партию и общается с игроком через консоль.
 * С консолью работает только этот класс — остальные пишут служебные сообщения в лог.
 */
public class Wordle {

    static final String DICTIONARY_FILE_NAME = "words_ru.txt";
    static final String LOG_FILE_NAME = "wordle.log";

    private static final String PROMPT = "> ";
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public static void main(String[] args) {
        // лог дописывается в конец файла, чтобы не терять записи прошлых запусков
        try (PrintWriter log = new PrintWriter(new FileWriter(LOG_FILE_NAME, StandardCharsets.UTF_8, true), true)) {
            log.println("=== Запуск игры " + LocalDateTime.now().format(LOG_TIME_FORMAT) + " ===");
            try {
                WordleDictionaryLoader loader = new WordleDictionaryLoader(log);
                WordleDictionary dictionary = loader.load(DICTIONARY_FILE_NAME);
                WordleGame game = new WordleGame(dictionary, log);
                play(game, new Scanner(System.in, StandardCharsets.UTF_8), System.out, log);
            } catch (WordleProgramException e) {
                System.out.println("Не удалось запустить игру: " + e.getMessage());
                System.out.println("Подробности — в файле " + LOG_FILE_NAME);
                log.println("Ошибка работы программы: " + e.getMessage());
                e.printStackTrace(log);
            } catch (Exception e) {
                System.out.println("В программе произошла внутренняя ошибка. Подробности — в файле " + LOG_FILE_NAME);
                log.println("Непредвиденная ошибка:");
                e.printStackTrace(log);
            }
            log.println("=== Работа завершена ===");
        } catch (IOException e) {
            System.err.println("Не удалось создать лог-файл " + LOG_FILE_NAME + ": " + e.getMessage());
        }
    }

    /**
     * Игровой цикл: спрашивает слова, пока партия не окончится, и выводит итог.
     * Пустая строка — просьба о подсказке: компьютер сам подбирает подходящее слово и делает им ход,
     * поэтому партию можно пройти, просто нажимая Enter.
     */
    static void play(WordleGame game, Scanner input, PrintStream out, PrintWriter log) {
        printRules(out);
        while (!game.isOver()) {
            out.printf("%nПопытка %d из %d:%n", game.getStepsMade() + 1, MAX_STEPS);
            out.print(PROMPT);
            if (!input.hasNextLine()) {
                out.println();
                out.println("Ввод закончился — партия прервана.");
                log.println("Ввод закончился до конца партии");
                break;
            }
            String line = input.nextLine();
            try {
                String word = line;
                if (line.isBlank()) {
                    word = game.suggestWord();
                    out.println(PROMPT + word + "   (подсказка компьютера)");
                }
                out.println(PROMPT + game.makeMove(word));
            } catch (WordleGameException e) {
                out.println(e.getMessage());
                log.println("Ход не засчитан: " + e.getMessage());
            }
        }
        printResult(game, out);
    }

    private static void printRules(PrintStream out) {
        out.println("WORDLE — угадайте существительное из " + WORD_LENGTH + " букв за " + MAX_STEPS + " попыток.");
        out.println("После каждого хода под словом появляется подсказка:");
        out.println("  +  буква есть в слове и стоит на своём месте");
        out.println("  ^  буква есть в слове, но стоит в другом месте");
        out.println("  -  такой буквы в слове нет");
        out.println("Нажмите Enter в пустой строке, и компьютер сам подберёт подходящее слово.");
    }

    private static void printResult(WordleGame game, PrintStream out) {
        out.println();
        if (game.getStatus() == GameStatus.WON) {
            out.printf("Победа! Слово угадано с %d-й попытки.%n", game.getStepsMade());
        } else if (game.getStatus() == GameStatus.LOST) {
            out.println("Попытки закончились — слово не угадано.");
        }
        out.println("Загаданное слово: " + game.getAnswer());
    }
}
