package hu.bme.mit.ftsrg.hypernate.middleware.event;

import lombok.Data;
import lombok.Value;

@Data
public abstract class HypernateEvent {

  protected final String type;
}
