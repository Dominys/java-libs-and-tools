package io.github.dominys.patcher;

/**
 * Interface for field update operation.
 *
 * @param <T> type
 */
@FunctionalInterface
public interface UpdateOperation<T> {

  FieldUpdateResult execute(T target, T source);

}
