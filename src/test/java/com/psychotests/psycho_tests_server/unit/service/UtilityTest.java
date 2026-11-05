package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.service.Utility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Юнит-тесты Utility.splitTokens — разбор строки на токены по пробельным
 * символам и запятым.
 *
 * <p>Что проверяем: контракт метода на входах-граничных случаях и на
 * особенностях регулярки. Пустые и null-входы дают пустой список; любые
 * сочетания пробелов, табов, переносов строк, запятых и юникодных пробелов
 * (включая неразрывный \u00A0 и узкий неразрывный \u202F) — разделители;
 * идущие подряд разделители не создают пустых токенов; ведущие и хвостовые
 * разделители отбрасываются. Юникодность регулярки обеспечена флагом (?U):
 * до этой правки \s покрывал только ASCII-пробелы, и NBSP склеивал токены —
 * теперь контракт юникод-осведомлённый, и тесты nonBreakingSpace_isSeparator
 * и narrowNoBreakSpace_isSeparator фиксируют исправленное поведение.</p>
 *
 * <p>Как работает: статический метод без зависимостей, Spring-контекст и
 * Mockito не нужны — только AssertJ. Возвращаемый список — immutable
 * (List.of/Stream.toList), это тоже часть контракта и проверяется.</p>
 *
 * <p>Граница с другими тестами: класс не имеет зависимостей и уровня
 * интеграции — все сценарии покрыты здесь.</p>
 */
class UtilityTest {

    @Nested
    class NullAndBlank {

        /** null — пустой список, без исключений. */
        @Test
        void nullInput_returnsEmptyList() {
            assertThat(Utility.splitTokens(null)).isEmpty();
        }

        /** Пустая строка — пустой список. */
        @Test
        void emptyString_returnsEmptyList() {
            assertThat(Utility.splitTokens("")).isEmpty();
        }

        /** Строка из одних разделителей — пустой список: токенов нет. */
        @Test
        void onlySeparators_returnsEmptyList() {
            assertThat(Utility.splitTokens(" \t ,,\n ")).isEmpty();
        }

        /** isBlank ловит и одиночный пробел — регрессионная проверка. */
        @Test
        void singleSpace_returnsEmptyList() {
            assertThat(Utility.splitTokens(" ")).isEmpty();
        }
    }

    @Nested
    class Separators {

        /** Одиночный пробел — базовый разделитель. */
        @Test
        void singleSpaceSplits() {
            assertThat(Utility.splitTokens("первый второй")).containsExactly("первый", "второй");
        }

        /** Любые сочетания пробельных символов и запятых — один разделитель. */
        @Test
        void mixedSeparators_collapseToSingle() {
            assertThat(Utility.splitTokens("a,\t b ,\nc ,  d"))
                    .containsExactly("a", "b", "c", "d");
        }

        /** Запятая без пробелов тоже разделяет. */
        @Test
        void commaWithoutSpaces_splits() {
            assertThat(Utility.splitTokens("раз,два,три")).containsExactly("раз", "два", "три");
        }

        /**
         * Ведущие и хвостовые разделители не создают пустых токенов.
         * Это неочевидный контракт String.split: он возвращает ведущие
         * пустые строки (",,a".split(",") → ["", "a"]), но фильтр в методе
         * их убирает. Тест фиксирует именно итоговое поведение метода.
         */
        @Test
        void leadingAndTrailingSeparators_ignored() {
            assertThat(Utility.splitTokens(" , , первый второй , "))
                    .containsExactly("первый", "второй");
        }
    }

    @Nested
    class Tokens {

        /** Один токен без разделителей — список из одного элемента. */
        @Test
        void singleToken() {
            assertThat(Utility.splitTokens("токен")).containsExactly("токен");
        }

        /** Пустые токены между разделителями не порождаются. */
        @Test
        void consecutiveSeparators_noEmptyTokens() {
            assertThat(Utility.splitTokens("a  ,,  b")).containsExactly("a", "b");
        }

        /**
         * Неразрывный пробел — разделитель: регулярка с флагом (?U)
         * считает \s юникодным \p{IsWhite_Space}, куда NBSP входит.
         * Токены разделяются, как обычным пробелом.
         */
        @Test
        void nonBreakingSpace_isSeparator() {
            assertThat(Utility.splitTokens("первый\u00A0второй"))
                    .containsExactly("первый", "второй");
        }

        /**
         * Узкий неразрывный пробел (\u202F) — тоже разделитель: входит
         * в юникодный класс пробелов. Защита от будущих «незаметных»
         * пробелов из Word и PDF-копипаста.
         */
        @Test
        void narrowNoBreakSpace_isSeparator() {
            assertThat(Utility.splitTokens("первый\u202Fвторой"))
                    .containsExactly("первый", "второй");
        }


        /** Возвращаемый список неизменяем — попытка изменить бросает UnsupportedOperationException. */
        @Test
        void result_isImmutable() {
            var tokens = Utility.splitTokens("a b");

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> tokens.add("c"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
