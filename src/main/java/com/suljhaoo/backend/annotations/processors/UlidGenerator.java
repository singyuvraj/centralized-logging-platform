package com.suljhaoo.backend.annotations.processors;

import com.github.f4b6a3.ulid.UlidCreator;
import java.io.Serializable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Custom Hibernate ID generator for ULID (Universally Unique Lexicographically Sortable Identifier)
 * ULID provides lexicographically sortable IDs with timestamp information
 */
public class UlidGenerator implements IdentifierGenerator {

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) {
    return UlidCreator.getUlid().toString();
  }
}
