package io.github.dominys.patcher;

/**
 * The exception class for field update errors.
 */
public class FieldUpdateException extends RuntimeException {

  private final String field;

  public FieldUpdateException(String field) {
    super();
    this.field = field;
  }

  public FieldUpdateException(String field, Throwable cause) {
    super(cause);
    this.field = field;
  }

  public String getField() {
    return field;
  }

  @Override
  public String getMessage() {
    return "Failed to update field: " + field;
  }
}
