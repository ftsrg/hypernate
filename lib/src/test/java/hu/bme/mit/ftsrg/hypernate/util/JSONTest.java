/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.util;

import static org.assertj.core.api.Assertions.*;

import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import org.junit.jupiter.api.Test;

public class JSONTest {

  @Test
  public void givenObject_whenSerialize_thenReturnJSONString() throws SerializationException {
    /* --- given --- */
    Foo obj = new Foo("abc");

    /* --- when --- */
    String json = JSON.serialize(obj);

    /* --- then --- */
    assertThat(json).isEqualToIgnoringWhitespace("{\"string\": \"abc\"}");
  }

  @Test
  public void givenObject_whenSerialize_thenReturnJSONStringWithAlphabeticallyOrderedKeys()
      throws SerializationException {
    /* --- given --- */
    Bar obj = new Bar("abc", 100);

    /* --- when --- */
    String json = JSON.serialize(obj);

    /* --- then --- */
    assertThat(json).isEqualToIgnoringWhitespace("{\"number\": 100, \"string\": \"abc\"}");
  }

  private static class Foo {

    private final String string;

    Foo(final String string) {
      this.string = string;
    }

    public String string() {
      return string;
    }
  }

  private static class Bar {

    private final String string;
    private final int number;

    Bar(final String string, final int number) {
      this.string = string;
      this.number = number;
    }

    public String string() {
      return string;
    }

    public int number() {
      return number;
    }
  }
}
