package io.github.dominys.patcher.operation;


import io.github.dominys.patcher.ModelField;
import io.github.dominys.patcher.UpdateOperation;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Base class for a field update operation.
 *
 * @param <T> base type
 * @param <R> field type
 */
public abstract class BaseUpdateOperation<T, R> implements UpdateOperation<T> {

  private final ModelField field;
  private final Function<T, R> getter;
  private final BiConsumer<T, R> setter;
  private final Predicate<R> condition;

  protected BaseUpdateOperation(ModelField field, Function<T, R> getter, BiConsumer<T, R> setter,
                                Predicate<R> condition) {
    this.field = field;
    this.getter = getter;
    this.setter = setter;
    this.condition = condition;
  }

  public ModelField getField() {
    return field;
  }

  public Function<T, R> getGetter() {
    return getter;
  }

  public BiConsumer<T, R> getSetter() {
    return setter;
  }

  public Predicate<R> getCondition() {
    return condition;
  }

}
