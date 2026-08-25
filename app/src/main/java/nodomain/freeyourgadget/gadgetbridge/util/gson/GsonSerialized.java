package nodomain.freeyourgadget.gadgetbridge.util.gson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a class that is read/written by Gson reflection, so that the fields
 * are kept by R8.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface GsonSerialized {
}
