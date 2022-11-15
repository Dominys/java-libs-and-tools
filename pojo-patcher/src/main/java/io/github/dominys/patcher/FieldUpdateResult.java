package io.github.dominys.patcher;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Field update result.
 */
public class FieldUpdateResult {

  private final ModelField field;

  private final Integer index;

  private final List<FieldUpdateResult> children;

  private FieldUpdateResult(Builder builder) {
    field = builder.field;
    index = builder.index;
    children = builder.children;
  }

  public ModelField getField() {
    return field;
  }

  public Integer getIndex() {
    return index;
  }

  public List<FieldUpdateResult> getChildren() {
    return children;
  }

  public boolean hasUpdates() {
    return field != null || index != null || CollectionUtils.isNotEmpty(children);
  }

  /**
   * To builder method.
   *
   * @return Builder
   */
  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.field = getField();
    builder.index = getIndex();
    builder.children = getChildren();
    return builder;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FieldUpdateResult that = (FieldUpdateResult) o;
    return Objects.equals(field, that.field) && Objects.equals(children, that.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, children);
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    if (field != null) {
      stringBuilder.append(field.getFieldName());
    } else if (index != null) {
      stringBuilder.append('[');
      stringBuilder.append(index);
      stringBuilder.append(']');
    }
    if (CollectionUtils.isNotEmpty(children)) {
      stringBuilder.append(children.stream()
          .map(FieldUpdateResult::toString)
          .collect(Collectors.joining(",", "{", "}")));
    }
    return stringBuilder.toString();
  }

  /**
   * Builder class.
   */
  public static final class Builder {
    private ModelField field;

    private Integer index;
    private List<FieldUpdateResult> children;

    private Builder() {
    }

    public Builder field(ModelField field) {
      this.field = field;
      return this;
    }

    public Builder index(Integer index) {
      this.index = index;
      return this;
    }

    public Builder children(List<FieldUpdateResult> children) {
      this.children = children;
      return this;
    }

    public FieldUpdateResult build() {
      return new FieldUpdateResult(this);
    }
  }
}
