package hu.bme.mit.ftsrg.hypernate.middleware.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class TransactionBegin extends HypernateEvent {}