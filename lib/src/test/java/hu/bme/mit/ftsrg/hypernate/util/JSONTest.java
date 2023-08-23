/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.util;

import static org.assertj.core.api.Assertions.*;

import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import lombok.Data;
import org.junit.jupiter.api.Test;

public class JSONTest {

  @Test
  public void givenObject_whenSerialize_thenReturnJSONString() throws SerializationException {
    /* --- given --- */
    var obj = new Foo("abc");

    /* --- when --- */
    String json = JSON.serialize(obj);

    /* --- then --- */
    assertThat(json).isEqualToIgnoringWhitespace("""
{"string": "abc"}
""");
  }

  @Test
  public void givenObject_whenSerialize_thenReturnJSONStringWithAlphabeticallyOrderedKeys()
      throws SerializationException {
    /* --- given --- */
    var obj = new Bar("abc", 100);

    /* --- when --- */
    String json = JSON.serialize(obj);

    /* --- then --- */
    assertThat(json).isEqualToIgnoringWhitespace("""
{"number": 100, "string": "abc"}
""");
  }

  @Data
  private static final class Foo {

    private final String string;

    public Foo(final String string) {
      this.string = string;
    }

    public String getString() {
      return string;
    }
  }

  @Data
  private static final class Bar {

    private final String string;
    private final int number;

    public Bar(final String string, final int number) {
      this.string = string;
      this.number = number;
    }

    public String getString() {
      return string;
    }

    public int getNumber() {
      return number;
    }
  }
}
