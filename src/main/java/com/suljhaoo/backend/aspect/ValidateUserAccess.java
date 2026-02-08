package com.suljhaoo.backend.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to validate that the authenticated user matches the userId from a method parameter.
 *
 * <p>Usage:
 *
 * <pre>
 * {@code @ValidateUserAccess("userId")}
 * public ResponseEntity<SaleResponse> getSale(
 *     @PathVariable String userId,
 *     @PathVariable String storeId) {
 *   // Method implementation - userId will be automatically validated
 * }
 * </pre>
 *
 * <p>The aspect will automatically:
 *
 * <ul>
 *   <li>Extract the userId from the method parameter with the specified name
 *   <li>Compare it with the current authenticated user ID from SecurityContext
 *   <li>Throw RuntimeException("Unauthorized access") if they don't match
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateUserAccess {
  /**
   * The name of the method parameter that contains the userId to validate. This should match the
   * parameter name in the method signature.
   *
   * @return the parameter name containing the userId
   */
  String value() default "userId";
}
