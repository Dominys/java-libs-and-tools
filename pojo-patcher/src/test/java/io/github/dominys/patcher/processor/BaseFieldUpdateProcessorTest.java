package io.github.dominys.patcher.processor;

import io.github.dominys.patcher.FieldUpdateException;
import io.github.dominys.patcher.FieldUpdateProcessor;
import io.github.dominys.patcher.FieldUpdateResult;
import io.github.dominys.patcher.ModelField;
import io.github.dominys.patcher.operation.FieldUpdateOperation;
import io.github.dominys.patcher.operation.ListUpdateOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class BaseFieldUpdateProcessorTest {

  private static final TestField FIELD_ONE = new TestField("fieldOne");
  private static final TestField STRING_LIST_FIELD = new TestField("stringList");
  private static final TestField POJO_LIST_FIELD = new TestField("pojoList");

  @Test
  public void testExecute() {
    BaseFieldUpdateProcessor<TestPOJO> processor = new BaseFieldUpdateProcessor<>();
    processor.map(FIELD_ONE, TestPOJO::getFieldOne, TestPOJO::setFieldOne);
    processor.mergeList(STRING_LIST_FIELD, TestPOJO::getStringList, TestPOJO::setStringList,
        s -> s.charAt(0));

    TestPOJO target = new TestPOJO();
    target.setFieldOne("originalValue");
    target.setStringList(List.of("1_1", "2_1", "3_1"));

    TestPOJO source = new TestPOJO();
    source.setFieldOne("updatedValue");
    source.setStringList(List.of("1_2", "4_2"));

    FieldUpdateResult fieldUpdateResult = processor.execute(target, source);

    assertThat(fieldUpdateResult.hasUpdates()).isTrue();
    assertThat(fieldUpdateResult).hasToString("{fieldOne,stringList{[0],[3]}}");

    assertThat(target.getFieldOne()).isEqualTo("updatedValue");
    assertThat(target.getStringList()).isEqualTo(List.of("1_2", "2_1", "3_1", "4_2"));
  }

  @Test
  public void testMappingException() {
    BaseFieldUpdateProcessor<TestPOJO> processor = new BaseFieldUpdateProcessor<>();
    processor.map(FIELD_ONE, TestPOJO::getFieldOne, TestPOJO::setFieldOne);
    processor.map(STRING_LIST_FIELD, TestPOJO::getStringList, TestPOJO::setStringList);
    processor.mergeList(POJO_LIST_FIELD, TestPOJO::getPojoList, TestPOJO::setPojoList,
        TestPOJO::getFieldOne, processor);

    TestPOJO exceptionPojo = mock(TestPOJO.class, Mockito.CALLS_REAL_METHODS);
    exceptionPojo.setFieldOne("someVal");
    exceptionPojo.setStringList(List.of("2"));

    TestPOJO target = new TestPOJO();
    target.setPojoList(List.of(exceptionPojo));

    TestPOJO sourcePojo = new TestPOJO();
    sourcePojo.setFieldOne("someVal");
    sourcePojo.setStringList(List.of("1"));

    TestPOJO source = new TestPOJO();
    source.setPojoList(List.of(sourcePojo));

    RuntimeException exception = new RuntimeException("test exception");
    doThrow(exception).when(exceptionPojo).setStringList(anyList());

    assertThatThrownBy(() -> processor.execute(target, source))
        .isInstanceOf(FieldUpdateException.class)
        .hasMessage("Failed to update field: pojoList[0].stringList")
        .hasCauseInstanceOf(RuntimeException.class)
        .hasRootCause(exception);
  }


  @Test
  public void testMapAlways() {
    BaseFieldUpdateProcessor<TestPOJO> processor = new BaseFieldUpdateProcessor<>();
    processor.mapAlways(FIELD_ONE, TestPOJO::getFieldOne, TestPOJO::setFieldOne);

    assertThat(processor.getOperations()).hasSize(1);
    FieldUpdateOperation<TestPOJO, String> operation =
        (FieldUpdateOperation<TestPOJO, String>) processor.getOperations().get(0);

    TestPOJO pojo = new TestPOJO();
    pojo.setFieldOne("value");

    assertThat(operation.getField()).isEqualTo(FIELD_ONE);
    assertThat(operation.getCondition().test(null)).isTrue();
    assertThat(operation.getFieldUpdateProcessor()).isNull();
    assertThat(operation.getField()).isEqualTo(FIELD_ONE);
    assertThat(operation.getGetter().apply(pojo)).isEqualTo("value");
    operation.getSetter().accept(pojo, "newValue");
    assertThat(pojo.getFieldOne()).isEqualTo("newValue");
  }

  @Test
  public void testMap() {
    FieldUpdateProcessor<String> fieldUpdateProcessor = mock(FieldUpdateProcessor.class);

    BaseFieldUpdateProcessor<TestPOJO> processor = new BaseFieldUpdateProcessor<>();
    processor.map(FIELD_ONE, TestPOJO::getFieldOne, TestPOJO::setFieldOne, fieldUpdateProcessor);

    assertThat(processor.getOperations()).hasSize(1);
    FieldUpdateOperation<TestPOJO, String> operation =
        (FieldUpdateOperation<TestPOJO, String>) processor.getOperations().get(0);

    TestPOJO pojo = new TestPOJO();
    pojo.setFieldOne("value");

    assertThat(operation.getField()).isEqualTo(FIELD_ONE);
    assertThat(operation.getFieldUpdateProcessor()).isEqualTo(fieldUpdateProcessor);
    assertThat(operation.getField()).isEqualTo(FIELD_ONE);
    assertThat(operation.getGetter().apply(pojo)).isEqualTo("value");
    operation.getSetter().accept(pojo, "newValue");
    assertThat(pojo.getFieldOne()).isEqualTo("newValue");
    assertThat(operation.getCondition().test("")).isTrue();
    assertThat(operation.getCondition().test(null)).isFalse();
  }

  @Test
  public void testMapIf() {
    FieldUpdateProcessor<String> fieldUpdateProcessor = mock(FieldUpdateProcessor.class);

    BaseFieldUpdateProcessor<TestPOJO> processor = new BaseFieldUpdateProcessor<>();
    processor.mapIf(FIELD_ONE, TestPOJO::getFieldOne, TestPOJO::setFieldOne, "someVal"::equals,
        fieldUpdateProcessor);

    assertThat(processor.getOperations()).hasSize(1);
    FieldUpdateOperation<TestPOJO, String> operation =
        (FieldUpdateOperation<TestPOJO, String>) processor.getOperations().get(0);

    TestPOJO pojo = new TestPOJO();
    pojo.setFieldOne("value");

    assertThat(operation.getField()).isEqualTo(FIELD_ONE);
    assertThat(operation.getFieldUpdateProcessor()).isEqualTo(fieldUpdateProcessor);
    assertThat(operation.getField()).isEqualTo(FIELD_ONE);
    assertThat(operation.getGetter().apply(pojo)).isEqualTo("value");
    operation.getSetter().accept(pojo, "newValue");
    assertThat(pojo.getFieldOne()).isEqualTo("newValue");
    assertThat(operation.getCondition().test("")).isFalse();
    assertThat(operation.getCondition().test(null)).isFalse();
    assertThat(operation.getCondition().test("someVal")).isTrue();
  }

  @Test
  public void testMergeList() {
    FieldUpdateProcessor<String> fieldUpdateProcessor = mock(FieldUpdateProcessor.class);

    BaseFieldUpdateProcessor<TestPOJO> processor = new BaseFieldUpdateProcessor<>();
    processor.mergeList(FIELD_ONE, TestPOJO::getStringList, TestPOJO::setStringList,
        f -> f.charAt(2),
        fieldUpdateProcessor,
        s -> CollectionUtils.size(s) == 2);

    assertThat(processor.getOperations()).hasSize(1);
    ListUpdateOperation<TestPOJO, String, Character> operation =
        (ListUpdateOperation<TestPOJO, String, Character>) processor.getOperations().get(0);

    List<String> strings = List.of("someVal");

    TestPOJO pojo = new TestPOJO();
    pojo.setStringList(strings);

    assertThat(operation.getField()).isEqualTo(FIELD_ONE);
    assertThat(operation.getFieldUpdateProcessor()).isEqualTo(fieldUpdateProcessor);
    assertThat(operation.getGetter().apply(pojo)).isEqualTo(strings);
    operation.getSetter().accept(pojo, List.of());
    assertThat(pojo.getStringList()).isEqualTo(List.of());

    assertThat(operation.getCondition().test(null)).isFalse();
    assertThat(operation.getCondition().test(List.of())).isFalse();
    assertThat(operation.getCondition().test(List.of("1", "2"))).isTrue();

    assertThat(operation.getKeyProvider()).isNotNull();

    assertThat(operation.getKeyProvider().apply("someString")).isEqualTo('m');
  }


  public static class TestPOJO {
    private String fieldOne;
    private List<String> stringList;

    private List<TestPOJO> pojoList;

    public String getFieldOne() {
      return fieldOne;
    }

    public void setFieldOne(String fieldOne) {
      this.fieldOne = fieldOne;
    }

    public List<String> getStringList() {
      return stringList;
    }

    public void setStringList(List<String> stringList) {
      this.stringList = stringList;
    }

    public List<TestPOJO> getPojoList() {
      return pojoList;
    }

    public void setPojoList(List<TestPOJO> pojoList) {
      this.pojoList = pojoList;
    }
  }

  public static class TestField implements ModelField {

    private final String fieldName;

    public TestField(String fieldName) {
      this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
      return fieldName;
    }
  }
}
