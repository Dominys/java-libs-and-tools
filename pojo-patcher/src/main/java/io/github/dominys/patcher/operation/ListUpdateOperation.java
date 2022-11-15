package io.github.dominys.patcher.operation;

import com.google.common.collect.Streams;
import io.github.dominys.patcher.FieldUpdateException;
import io.github.dominys.patcher.FieldUpdateProcessor;
import io.github.dominys.patcher.FieldUpdateResult;
import io.github.dominys.patcher.ModelField;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.function.Function.identity;

/**
 * List field update operation.
 *
 * @param <T> base type
 * @param <R> field type
 */
public class ListUpdateOperation<T, R, K> extends BaseUpdateOperation<T, List<R>> {

  private final Function<R, K> keyProvider;
  private final FieldUpdateProcessor<R> fieldUpdateProcessor;

  /**
   * Constructor.
   *
   * @param field field name
   * @param getter field getter
   * @param setter field setter
   * @param condition source field check condition
   * @param keyProvider joining key provider
   * @param fieldUpdateProcessor field update processor
   */
  public ListUpdateOperation(
      ModelField field, Function<T, List<R>> getter, BiConsumer<T, List<R>> setter,
      Predicate<List<R>> condition, Function<R, K> keyProvider,
      FieldUpdateProcessor<R> fieldUpdateProcessor) {
    super(field, getter, setter, condition);
    this.keyProvider = keyProvider;
    this.fieldUpdateProcessor = fieldUpdateProcessor;
  }

  public Function<R, K> getKeyProvider() {
    return keyProvider;
  }

  public FieldUpdateProcessor<R> getFieldUpdateProcessor() {
    return fieldUpdateProcessor;
  }

  @Override
  public FieldUpdateResult execute(T target, T source) {

    List<R> val = getGetter().apply(source);
    if (getCondition().negate().test(val)) {
      return null;
    }

    List<R> targetVal = getGetter().apply(target);
    if (Objects.equals(val, targetVal)) {
      return null;
    }

    if (targetVal == null) {
      getSetter().accept(target, val);
      return FieldUpdateResult.builder()
          .field(getField())
          .build();
    }

    List<Pair<R, FieldUpdateResult>> results = mergeCollections(getGetter().apply(target),
        getGetter().apply(source));

    getSetter().accept(target, results.stream()
        .map(Pair::getLeft)
        .collect(Collectors.toList()));

    List<FieldUpdateResult> updateResults = results.stream()
        .map(Pair::getRight)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    return FieldUpdateResult.builder()
        .field(getField())
        .children(updateResults)
        .build();
  }

  private List<Pair<R, FieldUpdateResult>> mergeCollections(
      Collection<R> target, Collection<R> source) {

    Map<K, R> targetMap = aggregateByJoiningKey(target);

    Map<K, R> sourceMap = aggregateByJoiningKey(source);

    Set<K> keys = new LinkedHashSet<>(targetMap.keySet());
    keys.addAll(sourceMap.keySet());

    return Streams.zip(keys.stream(), IntStream.range(0, keys.size()).boxed(), Pair::of)
        .map(f -> updateElement(targetMap, sourceMap, f))
        .collect(Collectors.toList());
  }

  private LinkedHashMap<K, R> aggregateByJoiningKey(Collection<R> target) {
    return target.stream()
        .collect(Collectors.toMap(keyProvider, identity(), (f1, f2) -> f1, LinkedHashMap::new));
  }

  private Pair<R, FieldUpdateResult> updateElement(
      Map<K, R> targetMap, Map<K, R> sourceMap, Pair<K, Integer> orderedKey) {
    try {
      return updateElementUnchecked(targetMap, sourceMap, orderedKey);
    } catch (FieldUpdateException ex) {
      throw new FieldUpdateException(getField().getFieldName() + '[' + orderedKey.getRight() + "]."
         + ex.getField(), ex.getCause());
    } catch (Exception ex) {
      throw new FieldUpdateException(getField().getFieldName() + '[' + orderedKey.getRight() + "]",
          ex);
    }
  }

  private Pair<R, FieldUpdateResult> updateElementUnchecked(
      Map<K, R> targetMap, Map<K, R> sourceMap, Pair<K, Integer> orderedKey) {

    R targetItem = targetMap.get(orderedKey.getLeft());
    R sourceItem = sourceMap.get(orderedKey.getLeft());
    if (sourceItem == null) {
      return ImmutablePair.left(targetItem);
    }
    if (targetItem == null || fieldUpdateProcessor == null) {
      return ImmutablePair.of(sourceItem,
          FieldUpdateResult.builder()
              .index(orderedKey.getRight())
              .build());
    }

    FieldUpdateResult updateResult = fieldUpdateProcessor.execute(targetItem, sourceItem)
        .toBuilder()
        .index(orderedKey.getRight())
        .build();
    return ImmutablePair.of(targetItem, updateResult);
  }
}
