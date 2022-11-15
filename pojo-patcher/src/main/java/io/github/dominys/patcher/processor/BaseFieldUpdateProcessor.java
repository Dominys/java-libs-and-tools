package io.github.dominys.patcher.processor;


import io.github.dominys.patcher.FieldUpdateProcessor;
import io.github.dominys.patcher.FieldUpdateResult;
import io.github.dominys.patcher.ModelField;
import io.github.dominys.patcher.UpdateOperation;
import io.github.dominys.patcher.operation.FieldUpdateOperation;
import io.github.dominys.patcher.operation.ListUpdateOperation;
import org.apache.commons.collections4.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Base class for field update processors.
 */
public class BaseFieldUpdateProcessor<T> implements FieldUpdateProcessor<T> {

  private final List<UpdateOperation<T>> operations = new LinkedList<>();

  protected List<UpdateOperation<T>> getOperations() {
    return List.copyOf(operations);
  }

  @Override
  public FieldUpdateResult execute(T target, T source) {
    return FieldUpdateResult.builder()
        .children(operations.stream()
            .map(operation -> operation.execute(target, source))
            .filter(Objects::nonNull)
            .collect(Collectors.toList()))
        .build();
  }

  protected <R> void mapAlways(ModelField field, Function<T, R> getter, BiConsumer<T, R> setter) {
    mapIf(field, getter, setter, r -> true, null);
  }

  protected <R> void map(ModelField field, Function<T, R> getter, BiConsumer<T, R> setter) {
    mapIf(field, getter, setter, Objects::nonNull, null);
  }

  protected <R> void map(ModelField field,
                             Function<T, R> getter,
                             BiConsumer<T, R> setter,
                             FieldUpdateProcessor<R> updateProcessor) {
    mapIf(field, getter, setter, Objects::nonNull, updateProcessor);
  }

  protected <R, K> void mergeList(ModelField field,
                                  Function<T, List<R>> getter,
                                  BiConsumer<T, List<R>> setter,
                                  Function<R, K> keyProvider) {
    mergeList(field, getter, setter, keyProvider, null, CollectionUtils::isNotEmpty);
  }

  protected <R, K> void mergeList(ModelField field,
                               Function<T, List<R>> getter,
                               BiConsumer<T, List<R>> setter,
                               Function<R, K> keyProvider,
                               FieldUpdateProcessor<R> updateProcessor) {
    mergeList(field, getter, setter, keyProvider, updateProcessor, CollectionUtils::isNotEmpty);
  }

  protected <R, K> void mergeList(ModelField field,
                               Function<T, List<R>> getter,
                               BiConsumer<T, List<R>> setter,
                               Function<R, K> keyProvider,
                               FieldUpdateProcessor<R> updateProcessor,
                               Predicate<List<R>> condition) {
    operations.add(new ListUpdateOperation<>(field, getter, setter, condition, keyProvider,
        updateProcessor));
  }

  protected <R> void mapIf(ModelField field,
                        Function<T, R> getter,
                        BiConsumer<T, R> setter,
                        Predicate<R> condition) {
    mapIf(field, getter, setter, condition, null);
  }

  protected <R> void mapIf(ModelField field,
                                   Function<T, R> getter,
                                   BiConsumer<T, R> setter,
                                   Predicate<R> condition,
                                   FieldUpdateProcessor<R> updateProcessor) {
    operations.add(new FieldUpdateOperation<>(field, getter, setter, condition, updateProcessor));
  }

}
