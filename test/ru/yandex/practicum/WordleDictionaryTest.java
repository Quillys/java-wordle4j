package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.exceptions.EmptyDictionaryException;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordleDictionaryTest {

    private static PrintWriter log;

    private WordleDictionary dictionary;

    @BeforeAll
    static void createLog() {
        // в тестах лог пишется в консоль, а не в файл
        log = new PrintWriter(System.out, true);
    }

    @BeforeEach
    void createDictionary() throws EmptyDictionaryException {
        dictionary = new WordleDictionary(List.of(
                "Герой", "ёжики", "кот", "ля-ля", "hello", "  гонец  ", "ГЕРОЙ", "гeрой", "щётка", "абажурчик"), log);
    }

    @Test
    void shouldKeepOnlyFiveLetterRussianWordsWithoutDuplicates() {
        // «гeрой» написано с латинской «e», поэтому не подходит
        assertEquals(List.of("герой", "ежики", "гонец", "щетка"), dictionary.getWords());
        assertEquals(4, dictionary.size());
    }

    @Test
    void containsShouldIgnoreCaseYoAndSpaces() {
        assertTrue(dictionary.contains("ГЕРОЙ"));
        assertTrue(dictionary.contains("ЁЖИКИ"));
        assertTrue(dictionary.contains(" щётка "));
        assertFalse(dictionary.contains("кот"));
        assertFalse(dictionary.contains("абвгд"));
    }

    @Test
    void wordsShouldBeUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> dictionary.getWords().add("книга"));
    }

    @Test
    void randomWordShouldBeTakenFromDictionary() {
        Random random = new Random(7);
        Set<String> drawn = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String word = dictionary.getRandomWord(random);
            assertTrue(dictionary.contains(word));
            drawn.add(word);
        }
        assertEquals(dictionary.size(), drawn.size(), "за 100 попыток должно выпасть каждое слово");
    }

    @Test
    void shouldThrowWhenThereAreNoSuitableWords() {
        assertThrows(EmptyDictionaryException.class, () -> new WordleDictionary(List.of(), log));
        assertThrows(EmptyDictionaryException.class,
                () -> new WordleDictionary(List.of("кот", "hello", "ля-ля", "абажурчик"), log));
    }

    @Test
    void normalizeShouldTrimLowerCaseAndReplaceYo() {
        assertEquals("герой", WordleDictionary.normalize("  ГеРоЙ "));
        assertEquals("елка", WordleDictionary.normalize("Ёлка"));
        assertEquals("ежики", WordleDictionary.normalize("ёЖИКИ"));
        assertEquals("", WordleDictionary.normalize("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"герой", "ежики", "щетка", "абвгд"})
    void shouldAcceptFiveRussianLetters(String word) {
        assertTrue(WordleDictionary.isValidWord(word));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "кот", "героиня", "hello", "гeрой", "ля-ля", "гер0й", "гер ой"})
    void shouldRejectInvalidWords(String word) {
        assertFalse(WordleDictionary.isValidWord(word));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ГЕРОЙ", "ёжики"})
    void shouldExpectNormalizedWords(String word) {
        assertFalse(WordleDictionary.isValidWord(word));
        assertTrue(WordleDictionary.isValidWord(WordleDictionary.normalize(word)));
    }

    @Test
    void shouldCheckThatWordConsistsOfRussianLetters() {
        assertTrue(WordleDictionary.consistsOfRussianLetters("абвгдеёжзийклмнопрстуфхцчшщъыьэюя".replace("ё", "")));
        assertFalse(WordleDictionary.consistsOfRussianLetters(""));
        assertFalse(WordleDictionary.consistsOfRussianLetters("ёж"));
        assertFalse(WordleDictionary.consistsOfRussianLetters("кот!"));
    }

    @ParameterizedTest(name = "{0} против {1} = {2}")
    @CsvSource({
            "гонец, герой, +^-^-",  // пример из задания
            "герой, герой, +++++",
            "мышка, герой, -----",
            "отбор, герой, ---+^",  // единственная «о» ответа совпала на месте, поэтому первая «о» получает «-»
            "кукла, каток, +-^-^",  // в ответе две «к»: одна на месте, вторая в другом месте
            "аббат, сосна, ^----",  // в догадке две «а», в ответе одна: «^» достаётся только первой
            "салат, сабля, ++^--",
            "колос, сокол, ^+^+^"
    })
    void compareShouldFollowWordleRules(String guess, String answer, String expected) {
        assertEquals(expected, WordleDictionary.compare(guess, answer));
    }

    @Test
    void compareShouldRejectWordsOfWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> WordleDictionary.compare("кот", "герой"));
        assertThrows(IllegalArgumentException.class, () -> WordleDictionary.compare("герой", "героиня"));
    }
}
