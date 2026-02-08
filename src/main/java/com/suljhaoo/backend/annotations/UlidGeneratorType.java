package com.suljhaoo.backend.annotations;

import com.suljhaoo.backend.annotations.processors.UlidGenerator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.hibernate.annotations.IdGeneratorType;

/**
 * Custom annotation for ULID generator using Hibernate 6's modern approach Replaces the
 * deprecated @GenericGenerator annotation
 */
@IdGeneratorType(UlidGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface UlidGeneratorType {}
