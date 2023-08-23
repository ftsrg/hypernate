/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

/**
 * A generic database entity that can be serialized to JSON and to byte arrays.
 *
 * @param <Type> the type of the entity (required because of {@link
 *     SerializableEntity#getFactory()})
 * @see SerializableEntityBase
 */
public interface SerializableEntity<Type extends SerializableEntity<Type>> {

  /**
   * Get the type of this entity; essentially a table name.
   *
   * <p>Usually this is an all-caps string, such as <code>CUSTOMER</code>.
   *
   * @return the arbitrary identifier of this entity type
   * @see SerializableEntityBase#getType()
   */
  String getType();

  /**
   * Get the composite key for this entity.
   *
   * <p>The composite key is an array of strings comprising the primary key fields of the entity (ie
   * those annotated with {@link KeyPart}).
   *
   * @return the composite key of this entity
   * @see SerializableEntityBase#getKeyParts()
   */
  String[] getKeyParts();

  /**
   * Serialize this entity into a byte array.
   *
   * <p>The implementation is up to the developer, but it is recommended to simply return the
   * encoded version of {@link SerializableEntity#toJson()}.
   *
   * @return this entity serialized into a byte array
   * @see SerializableEntity#fromBuffer(byte[])
   * @see SerializableEntityBase#toBuffer()
   */
  byte[] toBuffer();

  /**
   * Deserialize this entity from a byte array.
   *
   * <p>Naturally, this should do the inverse of {@link SerializableEntity#toBuffer()}
   *
   * @param buffer the buffer to parse
   * @see SerializableEntity#toBuffer()
   * @see SerializableEntityBase#fromBuffer(byte[])
   */
  void fromBuffer(byte[] buffer);

  /**
   * Serialize this entity into a JSON string.
   *
   * @return this entity serialized into a JSON string
   * @see SerializableEntity#fromJson(String)
   * @see SerializableEntityBase#toJson()
   */
  String toJson();

  /**
   * Deserialize this entity from a JSON string.
   *
   * <p>Naturally, this should do the inverse of {@link SerializableEntity#toJson()}
   *
   * @param json the JSON string to parse
   * @see SerializableEntity#toJson()
   * @see SerializableEntityBase#fromJson(String)
   */
  void fromJson(String json);

  /**
   * Get a factory for this entity type.
   *
   * @return A factory that can be used to create empty instances of this entity
   * @see SerializableEntityBase#getFactory()
   */
  EntityFactory<Type> getFactory();
}
