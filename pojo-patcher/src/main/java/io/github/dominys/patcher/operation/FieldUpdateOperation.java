package io.github.dominys.patcher.operation;

import io.github.dominys.patcher.FieldUpdateException;
import io.github.dominys.patcher.FieldUpdateProcessor;
import io.github.dominys.patcher.FieldUpdateResult;
import io.github.dominys.patcher.ModelField;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Field update operation.
 *
 * @param <T> base type
 * @param <R> field type
 */
public class FieldUpdateOperation<T, R> extends BaseUpdateOperation<T, R> {

  private final FieldUpdateProcessor<R> fieldUpdateProcessor;

  /**
   * Constructor.
   *
   * @param field                field name
   * @param getter               field getter
   * @param setter               field setter
   * @param condition            source field check condition
   * @param fieldUpdateProcessor field update processor
   */
  public FieldUpdateOperation(
      ModelField field, Function<T, R> getter, BiConsumer<T, R> setter,
      Predicate<R> condition, FieldUpdateProcessor<R> fieldUpdateProcessor) {
    super(field, getter, setter, condition);
    this.fieldUpdateProcessor = fieldUpdateProcessor;
  }

  public FieldUpdateProcessor<R> getFieldUpdateProcessor() {
    return fieldUpdateProcessor;
  }

  @Override
  public FieldUpdateResult execute(T target, T source) {
    try {
      return doExecute(target, source);
    } catch (FieldUpdateException ex) {
      throw new FieldUpdateException(getField().getFieldName() + "." + ex.getField(),
          ex.getCause());
    } catch (Exception ex) {
      throw new FieldUpdateException(getField().getFieldName(), ex);
    }
  }

  private FieldUpdateResult doExecute(T target, T source) {
    R val = getGetter().apply(source);
    if (getCondition().negate().test(val)) {
      return null;
    }

    R targetVal = getGetter().apply(target);
    if (Objects.equals(val, targetVal)) {
      return null;
    }

    if (targetVal == null || fieldUpdateProcessor == null) {
      getSetter().accept(target, val);
      return FieldUpdateResult.builder()
          .field(getField())
          .build();
    }

    return fieldUpdateProcessor
        .execute(getGetter().apply(target), getGetter().apply(source))
        .toBuilder()
        .field(getField())
        .build();
  }

}
