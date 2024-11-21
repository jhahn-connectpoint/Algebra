package org.example.math.graph;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Compensates for the lack of {@code SequencedSet.of(...)} and {@code SequencedSet.copyOf(...)}
 *
 * @param <T> the type of elements.
 */
class ImmutableSequencedSet<T> extends AbstractSet<T> implements SequencedSet<T> {
    private static final ImmutableSequencedSet<?> EMPTY = new ImmutableSequencedSet<>(
            LinkedHashSet.newLinkedHashSet(0));
    private final SequencedSet<T> delegate;
    private ImmutableSequencedSet<T> reversed = null;

    private ImmutableSequencedSet(SequencedSet<T> delegate) {
        this.delegate = delegate;
    }

    private ImmutableSequencedSet(SequencedSet<T> delegate, ImmutableSequencedSet<T> reversed) {
        this.delegate = delegate;
        this.reversed = reversed;
    }

    @SuppressWarnings("unchecked")
    public static <T> SequencedSet<T> empty() {
        return (SequencedSet<T>) EMPTY;
    }

    public static <T> SequencedSet<T> of(T... elements) {
        if (elements.length == 0) {
            return empty();
        }
        final SequencedSet<T> set = LinkedHashSet.newLinkedHashSet(elements.length);
        for (T element : elements) {
            set.add(Objects.requireNonNull(element));
        }
        return new ImmutableSequencedSet<>(set);
    }

    public static <T> SequencedSet<T> copyOf(Collection<T> c) {
        if (c instanceof ImmutableSequencedSet<T> sequencedSet) {
            return sequencedSet;
        }
        if (c.isEmpty()) { // implicit NPE
            return empty();
        }
        if (c.contains(null)) {
            throw new NullPointerException();
        }
        return new ImmutableSequencedSet<>(new LinkedHashSet<>(c));
    }

    @Override
    public Iterator<T> iterator() {
        final Iterator<T> i = delegate.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public T next() {
                return i.next();
            }
        };
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public SequencedSet<T> reversed() {
        synchronized (this) {
            if (reversed == null) {
                reversed = new ImmutableSequencedSet<>(delegate.reversed(), this);
            }
        }
        return reversed;
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFirst(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLast(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        delegate.forEach(action);
    }

    @Override
    public T getFirst() {
        return delegate.getFirst();
    }

    @Override
    public T getLast() {
        return delegate.getLast();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T removeFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T removeLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<T> parallelStream() {
        return delegate.parallelStream();
    }

    @Override
    public Stream<T> stream() {
        return delegate.stream();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return delegate.toArray(a);
    }

    @Override
    public <T1> T1[] toArray(IntFunction<T1[]> generator) {
        return delegate.toArray(generator);
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof Set<?> that && delegate.equals(that);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public static <T> Collector<T, ?, SequencedSet<T>> collector() {
        return Collector.of(LinkedHashSet::new, Set::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, (UnaryOperator<SequencedSet<T>>) Collections::unmodifiableSequencedSet);
    }
}
