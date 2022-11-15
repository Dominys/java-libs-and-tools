package io.github.dominys.patcher;

/**
 * Interface for a field update processors.
 *
 * @param <T> type
 */
@FunctionalInterface
public interface FieldUpdateProcessor<T> {

  FieldUpdateResult execute(T target, T source);

}
