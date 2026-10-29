package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordleConstraintsTest {

    private static List<String> allWords;  // настоящий словарь — чтобы сверить фильтр на всех словах

    private WordleConstraints constraints;

    @BeforeAll
    static void loadDictionary() throws Exception {
        PrintWriter log = new PrintWriter(System.out, true);
        allWords = new WordleDictionaryLoader(log).load(Wordle.DICTIONARY_FILE_NAME).getWords();
    }

    @BeforeEach
    void createConstraints() {
        constraints = new WordleConstraints();
    }

    @Test
    void withoutMovesAnyValidWordMatches() {
        assertTrue(constraints.matches("герой"));
        assertTrue(constraints.matches("мышка"));
        assertFalse(constraints.matches("кот"));
        assertFalse(constraints.matches("hello"));
    }

    @Test
    void shouldUseAllKindsOfHintsFromTaskExample() {
        constraints.addMove("гонец", "+^-^-");  // загадано «герой»

        assertTrue(constraints.matches("герой"));
        assertTrue(constraints.matches("гетто"));
        assertFalse(constraints.matches("гонец"), "проверенное слово не может быть ответом");
        assertFalse(constraints.matches("белок"), "на первом месте должна быть «г»");
        assertFalse(constraints.matches("галоп"), "в слове должна быть «е»");
        assertFalse(constraints.matches("гелий"), "в слове должна быть «о»");
        assertFalse(constraints.matches("голые"), "«о» не может стоять на втором месте");
        assertFalse(constraints.matches("геном"), "«н» в слове нет");
    }

    @Test
    void shouldRequireRepeatedLetter() {
        constraints.addMove("кукла", "+-^-^");  // загадано «каток»: в слове не меньше двух «к»

        assertTrue(constraints.matches("каток"));
        assertTrue(constraints.matches("казак"));
        assertFalse(constraints.matches("кадет"), "в слове только одна «к»");
    }

    @Test
    void shouldLimitLetterCountWhenRepeatIsMarkedAbsent() {
        constraints.addMove("отбор", "---+^");  // загадано «герой»: «о» в слове ровно одна

        assertTrue(constraints.matches("герой"));
        assertTrue(constraints.matches("игрок"));
        assertFalse(constraints.matches("город"), "вторая «о» лишняя");
    }

    @Test
    void shouldRejectMalformedMoves() {
        assertThrows(IllegalArgumentException.class, () -> constraints.addMove("гонец", "+?-^-"));
        assertThrows(IllegalArgumentException.class, () -> constraints.addMove("гонец", "+^-"));
        assertThrows(IllegalArgumentException.class, () -> constraints.addMove("кот", "+^-"));
    }

    @Test
    void shouldDetectContradictoryPositions() {
        constraints.addMove("герой", "+----");

        assertThrows(IllegalStateException.class, () -> constraints.addMove("гонец", "-----"));
    }

    @Test
    void shouldDetectContradictoryLetterCounts() {
        constraints.addMove("герой", "-^---");

        assertThrows(IllegalStateException.class, () -> constraints.addMove("пенал", "-----"));
    }

    @Test
    void shouldMatchExactlyWordsConsistentWithOneMove() {
        Random random = new Random(1);
        for (int i = 0; i < 200; i++) {
            String guess = randomWord(random);
            String answer = randomWord(random);
            String result = WordleDictionary.compare(guess, answer);
            WordleConstraints oneMove = new WordleConstraints();
            oneMove.addMove(guess, result);

            for (String word : allWords) {
                boolean consistent = WordleDictionary.compare(guess, word).equals(result);
                assertEquals(consistent, oneMove.matches(word),
                        () -> "ход «" + guess + "» → " + result + ", слово «" + word + "»");
            }
        }
    }

    @Test
    void shouldMatchExactlyWordsConsistentWithAllMoves() {
        Random random = new Random(2);
        for (int game = 0; game < 20; game++) {
            String answer = randomWord(random);
            List<String> guesses = new ArrayList<>(allWords);
            Collections.shuffle(guesses, random);
            WordleConstraints gameConstraints = new WordleConstraints();
            Map<String, String> moves = new LinkedHashMap<>();

            for (String guess : guesses.subList(0, 5)) {
                String result = WordleDictionary.compare(guess, answer);
                gameConstraints.addMove(guess, result);
                moves.put(guess, result);

                assertTrue(gameConstraints.matches(answer), "ответ «" + answer + "» отброшен после ходов " + moves);
                for (String word : allWords) {
                    assertEquals(isConsistent(word, moves), gameConstraints.matches(word),
                            () -> "ходы " + moves + ", слово «" + word + "»");
                }
            }
        }
    }

    private static boolean isConsistent(String word, Map<String, String> moves) {
        for (Map.Entry<String, String> move : moves.entrySet()) {
            if (!WordleDictionary.compare(move.getKey(), word).equals(move.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static String randomWord(Random random) {
        return allWords.get(random.nextInt(allWords.size()));
    }
}
