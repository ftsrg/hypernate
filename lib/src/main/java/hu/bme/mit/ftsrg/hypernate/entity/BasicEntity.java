package hu.bme.mit.ftsrg.hypernate.entity;

import lombok.Data;

@Data
public abstract class BasicEntity<T extends Entity<T>> implements Entity<T> {}