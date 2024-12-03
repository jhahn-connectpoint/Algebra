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
 * Represents a "step function", i.e. a function whose domain can be partitioned into a finite number of
 * {@link Interval}s such that the function is constant on each interval.
 *
 * @param <X> the type of the domain.
 */
public final class StepFunction<X, V> implements Function<X, V> {

    // needed for equality testing, because Comparator.nullsFirst(..) does not override #equals()
    private final Comparator<? super X> xComparator;
    // invariants:
    // 1. all values are non-null
    // 2. all keys are non-null except the first one which represents -infinity
    private final NavigableMap<X, V> values;

    private StepFunction(NavigableMap<X, V> values, Comparator<? super X> xComparator) {
        this.values = values;
        this.xComparator = xComparator;
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
     * @param <X> the type of the domain
     * @return the constant one function
     * @see #constant(Object)
     */
    public static <X extends Comparable<? super X>> StepFunction<X, BigDecimal> one() {
        return constant(BigDecimal.ONE);
    }

    /**
     * @param value the value of the function
     * @param <X>   the type of the domain
     * @return the constant function with the given value
     */
    public static <X extends Comparable<? super X>, V> StepFunction<X, V> constant(V value) {
        return constant(value, Comparator.naturalOrder());
    }

    /**
     * @param value       the value of the function
     * @param xComparator the order of the domain
     * @param <X>         the type of the domain
     * @return the constant function with the given value where the domain is ordered by the given comparator.
     */
    public static <X, V> StepFunction<X, V> constant(V value, Comparator<? super X> xComparator) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(xComparator);
        return new StepFunction<>(newConstantValues(value, xComparator), xComparator);
    }

    private static <X, V> TreeMap<X, V> newConstantValues(V value, Comparator<? super X> xComparator) {
        TreeMap<X, V> map = new TreeMap<>(new NullFirstComparator<>(xComparator));
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
            interval.end().getValue().ifPresent(end -> newValues.put(end, valueOutside));
        }
        return new StepFunction<>(newValues, interval.comparator());
    }

    @Override
    public V apply(X x) {
        Objects.requireNonNull(x);
        return values.floorEntry(x) // always non-null because null is always in the map and the smallest object
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
     * Returns the set where this step function differs from the given "zero" value, expressed as a list of disjoint
     * {@link Interval}s. The returned list is empty iff this differs nowhere from the given value, i.e. if this
     * function is constant and everywhere equal to the given value.
     *
     * @param zero the test value
     * @return a
     */
    public List<Interval<X>> support(V zero) {
        List<Interval<X>> result = new ArrayList<>();

        var iterator = this.values.entrySet().iterator();
        var last = iterator.next();

        Interval.Bound<X> nonZeroSince = last.getValue().equals(zero) ? null : unboundedBelow();

        while (iterator.hasNext()) {
            var current = iterator.next();
            if (current.getValue().equals(zero)) {
                if (nonZeroSince != null) {
                    result.add(new Interval<>(nonZeroSince, Bound.of(current.getKey()), xComparator));
                    nonZeroSince = null;
                }
            } else if (nonZeroSince == null) {
                nonZeroSince = Bound.of(current.getKey());
            }
            last = current;
        }

        if (nonZeroSince != null && !last.getValue().equals(zero)) {
            result.add(new Interval<>(nonZeroSince, unboundedAbove(), xComparator));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Y> StepFunction<X, Y> andThen(Function<? super V, ? extends Y> f) {
        // unchecked casts so that we can use the more efficient copy constructor here
        TreeMap<X, Object> result = new TreeMap<>(this.values);
        result.replaceAll((k, v) -> f.apply((V) v));
        return new StepFunction<>((TreeMap<X, Y>) result, xComparator);
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
     * @return a {@link UnaryOperator} that, given a step-function {@code s}, returns the step-function that results
     * from applying the given operator to the values, i.e. the function {@code x->f(s(x))}.
     */
    public static <X, Y, Z> Function<StepFunction<X, Y>, StepFunction<X, Z>> pointwise(Function<Y, Z> f) {
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
        return (s1, s2) -> new StepFunction<>(unionizer.apply(s1.values, s2.values), s1.xComparator);
    }

    /**
     * @param f    the operator
     * @param <X>  the type of the domain
     * @param <Y1> the type of the left values
     * @param <Y2> the type of the right values
     * @param <Z>  the type of the right values
     * @return a {@link BinaryOperator} that, given two step-functions {@code s1, s2}, returns the step-function that
     * results from applying the given operator to the values, i.e. the function {@code x->f(s1(x),s2(x))}.
     */
    public static <X, Y1, Y2, Z> BiFunction<StepFunction<X, Y1>, StepFunction<X, Y2>, StepFunction<X, Z>> pointwise(BiFunction<Y1, Y2, Z> f) {
        final Unionizer<X, Y1, Y2, Z> unionizer = new Unionizer<>(f);
        return (s1, s2) -> new StepFunction<>(unionizer.apply(s1.values, s2.values), s1.xComparator);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof StepFunction<?, ?> that && this.xComparator.equals(that.xComparator) && this.values.equals(
                that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xComparator, values);
    }

    /**
     * Merges two step-functions by creating a common refinement of the two partitions and applying a BiFunction to both
     * values on each part.
     */
    private static class Unionizer<X, Y1, Y2, Z>
            implements BiFunction<SortedMap<X, Y1>, SortedMap<X, Y2>, NavigableMap<X, Z>> {

        private final BiFunction<Y1, Y2, Z> action;

        private Iterator<Map.Entry<X, Y1>> aIter;
        private Iterator<Map.Entry<X, Y2>> bIter;

        private Map.Entry<X, Y1> aEntry;
        private Map.Entry<X, Y2> bEntry;

        Unionizer(BiFunction<Y1, Y2, Z> action) {
            this.action = action;
        }

        @Override
        public NavigableMap<X, Z> apply(SortedMap<X, Y1> a, SortedMap<X, Y2> b) {
            final Comparator<? super X> comparator = a.comparator();
            if (!comparator.equals(b.comparator())) { // equality from NullFirstComparator
                throw new IllegalArgumentException("Operation not applicable to incompatible step-functions");
            }

            // we iterate in decreasing order through both maps
            aIter = a.sequencedEntrySet().reversed().iterator();
            bIter = b.sequencedEntrySet().reversed().iterator();

            // both maps are non-empty; they contain null as lowest key
            aEntry = aIter.next();
            bEntry = bIter.next();

            final TreeMap<X, Z> result = new TreeMap<>(comparator);

            // zig-zag algorithm
            boolean moreToDo = true;
            while (moreToDo) {
                Z currentValue = this.action.apply(aEntry.getValue(), bEntry.getValue());

                int comp = comparator.compare(aEntry.getKey(), bEntry.getKey());
                X currentKey;
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
                    // B < A => use A key & decrease A
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

    /**
     * internal replacement for {@link Comparator#nullsFirst(Comparator)} to override equals.
     *
     * @param inner
     * @param <T>
     */
    private record NullFirstComparator<T>(Comparator<T> inner) implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            if (o1 == null) {
                return o2 == null ? 0 : -1;
            }
            if (o2 == null) {
                return 1;
            }
            return inner.compare(o1, o2);
        }
    }
}
