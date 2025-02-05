package hu.bme.mit.ftsrg.hypernate.middleware.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class TransactionEnd extends HypernateEvent {}
