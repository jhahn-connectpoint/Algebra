package org.example.math;

import static org.example.math.Interval.Bound.unboundedAbove;
import static org.example.math.Interval.Bound.unboundedBelow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.example.math.Interval.Bound;

/**
 * Represents a "step function", i.e. a function whose domain can be divided into a finite number of intervals so that
 * the function is constant on each interval.
 *
 * @param <T> the type of the domain.
 */
public class StepFunction<T, V> implements Function<T, V> {

    // needed for equality testing, because Comparator.nullsFirst(..) does not override #equals()
    private final Comparator<? super T> tComparator;
    // invariants:
    // 1. all values are non-null
    // 2. all keys are non-null except the first one which represents -infinity
    private final NavigableMap<T, V> values;

    private StepFunction(NavigableMap<T, V> values, Comparator<? super T> tComparator) {
        this.values = values;
        this.tComparator = tComparator;
        normalize();
    }

    private void normalize() {
        var iterator = this.values.values().iterator();
        V last = null;
        while (iterator.hasNext()) {
            var current = iterator.next();
            if (current.equals(last)) { // implicit null check for all values in the map
                // merge steps with equal values by removing intermediate entries
                iterator.remove();
                continue;
            }
            last = current;
        }
    }

    /**
     * @param <X> the type of the domain
     * @return the constant zero function
     * @see #constant(Object)
     */
    public static <X extends Comparable<? super X>> StepFunction<X, BigDecimal> zero() {
        return constant(BigDecimal.ZERO);
    }

    /**
     * @param <T> the type of the domain
     * @return the constant one function
     * @see #constant(Object)
     */
    public static <T extends Comparable<? super T>> StepFunction<T, BigDecimal> one() {
        return constant(BigDecimal.ONE);
    }

    /**
     * @param value the value of the function
     * @param <T>   the type of the domain
     * @return the constant function with the given value
     */
    public static <T extends Comparable<? super T>, V> StepFunction<T, V> constant(V value) {
        return constant(value, Comparator.naturalOrder());
    }

    /**
     * @param value       the value of the function
     * @param tComparator the order of the domain
     * @param <T>         the type of the domain
     * @return the constant function with the given value where the domain is ordered by the given comparator.
     */
    public static <T, V> StepFunction<T, V> constant(V value, Comparator<? super T> tComparator) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(tComparator);
        return new StepFunction<>(newConstantValues(value, tComparator), tComparator);
    }

    private static <T, V> TreeMap<T, V> newConstantValues(V value, Comparator<? super T> tComparator) {
        TreeMap<T, V> map = new TreeMap<>(Comparator.nullsFirst(tComparator));
        map.put(null, value);
        return map;
    }

    /**
     * @param interval an interval
     * @param value    a value
     * @param <X>      the type of the domain
     * @return a step function that has the value {@link BigDecimal#ZERO} outside of the given interval and
     * {@code value} within.
     */
    public static <X> StepFunction<X, BigDecimal> singleStep(Interval<X> interval, BigDecimal value) {
        return singleStep(interval, value, BigDecimal.ZERO);
    }

    /**
     * @param interval     an interval
     * @param valueInside  a value
     * @param valueOutside a value
     * @param <X>          the type of the domain
     * @return a step function that has the value {@code valueOutside} outside of the given interval and
     * {@code valueInside} within.
     */
    public static <X, V> StepFunction<X, V> singleStep(Interval<X> interval, V valueInside, V valueOutside) {
        Objects.requireNonNull(interval);
        Objects.requireNonNull(valueInside);

        final TreeMap<X, V> newValues = newConstantValues(valueOutside, interval.comparator());
        if (!interval.isEmpty()) {
            // if start is infinite, then it is negative infinity which we represent in this class with null
            X start = interval.start().getValue().orElse(null);
            newValues.put(start, valueInside);

            // if end is not infinite, then we put a zero after the interval
            interval.end().getValue().ifPresent(e -> newValues.put(e, valueOutside));
        }
        return new StepFunction<>(newValues, interval.comparator());
    }

    @Override
    public V apply(T t) {
        Objects.requireNonNull(t);
        return values.floorEntry(t) // always non-null because null is always in the map and the smallest object
                     .getValue();
    }

    /**
     * @return {@code true} iff this function is constant.
     */
    public boolean isConstant() {
        return values.size() == 1;
    }

    /**
     * @param value a test value
     * @return {@code true} iff this function is constant equal to the given value.
     */
    public boolean isConstant(final V value) {
        return isConstant() && values.get(null).equals(value);
    }

    /**
     * @return the list of this function's values, ordered by the given order on the domain beginning with the value at
     * "-infinity" and ending with the value at "+infinity".
     */
    public List<V> values() {
        return List.copyOf(values.values());
    }

    /**
     * Returns the set where this step function differs from the given value, expressed as a list of disjoint
     * {@link Interval}s. The returned list is empty iff this differs nowhere from the given value, i.e. if this
     * function is constant and everywhere equal to the given value. {@code null} is returned to represent the whole
     * domain {@code X} (there is no {@link Interval} that would correspond to that if {@code X} is unbounded in the
     * given order).
     *
     * @param zero the test value
     * @return a
     */
    public List<Interval<T>> support(V zero) {
        if (this.isConstant()) {
            return this.isConstant(zero) ? List.of() : null;
        }
        List<Interval<T>> result = new ArrayList<>();

        var iterator = this.values.entrySet().iterator();
        var last = iterator.next();

        Interval.Bound<T> nonZeroSince = last.getValue().equals(zero) ? null : unboundedBelow();

        while (iterator.hasNext()) {
            var current = iterator.next();
            if (current.getValue().equals(zero)) {
                if (nonZeroSince != null) {
                    result.add(new Interval<>(nonZeroSince, Bound.of(current.getKey()), tComparator));
                    nonZeroSince = null;
                }
            } else if (nonZeroSince == null) {
                nonZeroSince = Bound.of(current.getKey());
            }
            last = current;
        }

        if (nonZeroSince != null && !last.getValue().equals(zero)) {
            result.add(new Interval<>(nonZeroSince, unboundedAbove(), tComparator));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V1> StepFunction<T, V1> andThen(Function<? super V, ? extends V1> f) {
        // unchecked casts so that we can use the more efficient copy constructor here
        TreeMap<T, Object> result = new TreeMap<>(this.values);
        result.replaceAll((k, v) -> f.apply((V) v));
        return new StepFunction<>((TreeMap<T, V1>) result, tComparator);
    }


    /**
     * @param f   the operator
     * @param <X> the type of the domain
     * @param <Y> the type of the values
     * @return a {@link UnaryOperator} that, given a step-function {@code s}, returns the step-function that results
     * from applying the given operator to the values, i.e. the function {@code x->f(s(x))}.
     */
    public static <X, Y> UnaryOperator<StepFunction<X, Y>> pointwise(UnaryOperator<Y> f) {
        return s -> s.andThen(f);
    }

    /**
     * @param f   the operator
     * @param <X> the type of the domain
     * @param <Y> the type of the values
     * @return a {@link BinaryOperator} that, given two step-functions {@code s1, s2}, returns the step-function that
     * results from applying the given operator to the values, i.e. the function {@code x->f(s1(x),s2(x))}.
     */
    public static <X, Y> BinaryOperator<StepFunction<X, Y>> pointwise(BinaryOperator<Y> f) {
        final Unionizer<X, Y, Y, Y> unionizer = new Unionizer<>(f);
        return (s1, s2) -> {
            requireCompatibility(s1, s2);
            return new StepFunction<>(unionizer.apply(s1.values, s2.values), s1.tComparator);
        };
    }

    private static <T, V> void requireCompatibility(StepFunction<T, V> s1, StepFunction<T, V> s2) {
        if (!s1.tComparator.equals(s2.tComparator)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof StepFunction<?, ?> that && this.tComparator.equals(that.tComparator) && this.values.equals(
                that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tComparator, values);
    }

    /**
     * Merges two sorted maps by creating a common refinement of the two partitions and applying a BiFunction to both
     * values on each part.
     */
    private static class Unionizer<T, V1, V2, V>
            implements BiFunction<SortedMap<T, V1>, SortedMap<T, V2>, NavigableMap<T, V>> {

        private final BiFunction<V1, V2, V> action;

        private Iterator<Map.Entry<T, V1>> aIter;
        private Iterator<Map.Entry<T, V2>> bIter;

        private Map.Entry<T, V1> aEntry;
        private Map.Entry<T, V2> bEntry;

        Unionizer(BiFunction<V1, V2, V> action) {
            this.action = action;
        }

        @Override
        public NavigableMap<T, V> apply(SortedMap<T, V1> a, SortedMap<T, V2> b) {
            // we iterate in decreasing order through both maps
            aIter = a.sequencedEntrySet().reversed().iterator();
            assert aIter.hasNext();
            aEntry = aIter.next();

            bIter = b.sequencedEntrySet().reversed().iterator();
            assert bIter.hasNext();
            bEntry = bIter.next();

            final Comparator<? super T> mapComparator = a.comparator();
            final TreeMap<T, V> result = new TreeMap<>(mapComparator);

            // zig-zag algorithm
            boolean moreToDo = true;
            while (moreToDo) {
                V currentValue = this.action.apply(aEntry.getValue(), bEntry.getValue());

                int comp = mapComparator.compare(aEntry.getKey(), bEntry.getKey());
                T currentKey;
                if (comp < 0) {
                    // A < B => use B key & decrease B
                    currentKey = bEntry.getKey();
                    moreToDo = decreaseB();
                } else if (comp == 0) {
                    // A == B => use any key & decrease both
                    //
                    // Note that both maps have null as their lowest key, so we will inevitably end the loop with this
                    // branch.
                    currentKey = aEntry.getKey();
                    boolean moreA = decreaseA();
                    boolean moreB = decreaseB();
                    moreToDo = moreA && moreB;
                } else {
                    // B < A => use A key & decrease left
                    currentKey = aEntry.getKey();
                    moreToDo = decreaseA();
                }
                result.put(currentKey, currentValue);
            }

            return result;
        }

        private boolean decreaseA() {
            if (aIter.hasNext()) {
                aEntry = aIter.next();
                return true;
            } else {
                return false;
            }
        }

        private boolean decreaseB() {
            if (bIter.hasNext()) {
                bEntry = bIter.next();
                return true;
            } else {
                return false;
            }
        }
    }
}
