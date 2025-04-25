package dev.wolfieboy09.qtech.api.annotation;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks all fields, methods and parameters as non-null
 * <br>
 * <i>Perfect for making IntelliJ be quiet with adding {@link org.jetbrains.annotations.NotNull} everywhere</i>
 */
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.CLASS)
public @interface NothingNullByDefault {
}
