package org.example.math.graph;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImmutableSequencedSetTest {

    @Nested
    class TestEmpty {

        @Test
        void emptyIsConstant() {
            var set1 = ImmutableSequencedSet.empty();
            var set2 = ImmutableSequencedSet.empty();

            assertThat(set1).isSameAs(set2);
        }

        @Test
        void emptyConstantIsSequencedSetAndEmpty() {
            var set = ImmutableSequencedSet.empty();

            assertThat(set).isInstanceOf(ImmutableSequencedSet.class).isEmpty();
        }
    }

    @Nested
    class TestOf {
        @Test
        void ofCreatesAnImmutableSequencedSet() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThat(set).isInstanceOf(ImmutableSequencedSet.class);
        }

        @Test
        void ofCreatesCorrectContent() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThat(set).containsExactly("a", "b", "c");
        }

        @Test
        void ofEmptyReturnsEmptyConstant() {
            var emptyConstant = ImmutableSequencedSet.empty();
            var set = ImmutableSequencedSet.of();

            assertThat(set).isSameAs(emptyConstant);
        }

        @Test
        void throwsOnNull() {
            assertThatNullPointerException().isThrownBy(() -> ImmutableSequencedSet.of("a", null));
        }
    }

    @Nested
    class TestCopyOf {
        private SequencedSet<String> original() {
            LinkedHashSet<String> set = LinkedHashSet.newLinkedHashSet(3);
            Collections.addAll(set, "a", "b", "c");
            return set;
        }

        @Test
        void createsAnImmutableSequencedSet() {
            var original = original();
            var set = ImmutableSequencedSet.copyOf(original);

            assertThat(set).isInstanceOf(ImmutableSequencedSet.class);
        }

        @Test
        void createsCorrectContent() {
            var original = original();

            var set = ImmutableSequencedSet.copyOf(original);

            assertThat(set).containsExactly("a", "b", "c");
        }

        @Test
        void createsCopy() {
            var original = original();
            var set = ImmutableSequencedSet.copyOf(original);

            original.add("d");

            assertThat(set).isNotEqualTo(original);
        }

        @Test
        void copyOfEmptyReturnsEmptyConstant() {
            var emptyConstant = ImmutableSequencedSet.empty();
            var set1 = ImmutableSequencedSet.copyOf(new LinkedHashSet<>());
            var set2 = ImmutableSequencedSet.copyOf(emptyConstant);

            assertThat(set1).isSameAs(emptyConstant);
            assertThat(set2).isSameAs(emptyConstant);
        }

        @Test
        void isIdempotent() {
            var original = original();
            var set1 = ImmutableSequencedSet.copyOf(original);
            var set2 = ImmutableSequencedSet.copyOf(set1);

            assertThat(set1).isSameAs(set2);
        }

        @Test
        void throwsOnNull() {
            assertThatNullPointerException().isThrownBy(() -> ImmutableSequencedSet.copyOf(null));
        }

        @Test
        void throwsCollectionThatContainsNull() {
            final Collection<String> c = new ArrayList<>();
            c.add(null);

            assertThatNullPointerException().isThrownBy(() -> ImmutableSequencedSet.copyOf(c));
        }
    }

    @Nested
    class TestImmutability {

        @Test
        void addThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("d"));
            // also existing elements that wouldn't change the set should throw
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.add("a"));
        }

        @Test
        void addAllThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");
            var list = List.of("c", "d", "e");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.addAll(list));
        }

        @Test
        void addFirstThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.addFirst("0"));
        }

        @Test
        void addLastThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.addLast("d"));
        }

        @Test
        void clearThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(set::clear);
        }

        @Test
        void removeThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("a"));
            // also non-existing elements that wouldn't change the set should throw
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.remove("d"));
        }

        @Test
        void removeFirstThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(set::removeFirst);
        }

        @Test
        void removeLastThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(set::removeLast);
        }


        @Test
        void removeAllThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");
            var list = List.of("c", "d", "e");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.removeAll(list));
        }


        @Test
        void removeIfThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
                    () -> set.removeIf(str -> str.startsWith("a")));
        }

        @Test
        void retainAllThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");
            var list = List.of("a", "b");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.retainAll(list));
        }

        @Test
        void iteratorRemoveThrows() {
            var set = ImmutableSequencedSet.of("a", "b", "c");
            var iterator = set.iterator();

            iterator.next();
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(iterator::remove);
        }
    }

    @Nested
    class TestReverse {

        @Test
        void isAlsoImmutable() {
            var set = ImmutableSequencedSet.of("a", "b", "c");
            var reversed = set.reversed();

            assertThat(reversed).isInstanceOf(ImmutableSequencedSet.class);
        }

        @Test
        void isCorrect() {
            var set = ImmutableSequencedSet.of("a", "b", "c");
            var reversed = set.reversed();

            assertThat(reversed).containsExactly("c", "b", "a");
        }

        @Test
        void isInvolution() {
            var set = ImmutableSequencedSet.of("a", "b", "c");
            var reversed = set.reversed();

            assertThat(reversed.reversed()).isSameAs(set);
        }
    }
}