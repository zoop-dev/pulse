package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.NativeFITMessage;

/**
 * This class is only used to generate code, and will not be packaged in the final apk
 *
 */
@RequiresApi(api = Build.VERSION_CODES.CUR_DEVELOPMENT)
public class FitCodeGenOld {
    public static void main(final String[] args) throws Exception {
        // To run this in Android Studio, right click and select "Run 'FitCodeGenOld.main()' with Coverage"
        // for some reason, the classpath is broken otherwise.
        checkOldVsNew();
    }

    private static void checkOldVsNew() {
        Map<Integer, NativeFITMessage> newMap = NativeFITMessage.KNOWN_MESSAGES;
        boolean foundProblem = false;
        for (final NativeFITMessage legacyMessage : NativeFITMessage.KNOWN_MESSAGES_LEGACY.values()) {
            NativeFITMessage newMessage = newMap.get(legacyMessage.getNumber());
            if (newMessage == null) {
                foundProblem = true;
                System.out.println("message not defined: " + legacyMessage.getNumber() + "/" + legacyMessage.name());
            } else {
                if (!newMessage.name().contentEquals(legacyMessage.name())) {
                    foundProblem = true;
                    System.out.println("unexpected message name: " + legacyMessage.getNumber() + "/" + legacyMessage.name()
                            + " new:" + newMessage.name());
                }
                for (final NativeFITMessage.FieldDefinitionPrimitive legacyField : legacyMessage.getFieldDefinitionPrimitives()) {
                    boolean foundField = false;
                    for (final NativeFITMessage.FieldDefinitionPrimitive newField : newMessage.getFieldDefinitionPrimitives()) {
                        if (newField.getNumber() == legacyField.getNumber()) {
                            foundField = true;
                            if (!newField.getName().contentEquals(legacyField.getName())) {
                                foundProblem = true;
                                System.out.println("unexpected field name: "
                                        + legacyMessage.getNumber() + "/" + legacyMessage.name() + " "
                                        + legacyField.getNumber() + "/" + legacyField.getName()
                                        + " new:" + newField.getName()
                                );
                            }
                            if (newField.getOffset() != legacyField.getOffset()) {
                                foundProblem = true;
                                System.out.println("unexpected field offset: "
                                        + legacyMessage.getNumber() + "/" + legacyMessage.name() + " "
                                        + legacyField.getNumber() + "/" + legacyField.getName()
                                        + " old:" + legacyField.getOffset()
                                        + " new:" + newField.getOffset()
                                );
                            }
                            if (newField.getScale() != legacyField.getScale()) {
                                foundProblem = true;
                                System.out.println("unexpected field scale: "
                                        + legacyMessage.getNumber() + "/" + legacyMessage.name() + " "
                                        + legacyField.getNumber() + "/" + legacyField.getName()
                                        + " old:" + legacyField.getScale()
                                        + " new:" + newField.getScale()
                                );
                            }
                            if (newField.getSize() != legacyField.getSize()) {
                                foundProblem = true;
                                System.out.println("unexpected field size: "
                                        + legacyMessage.getNumber() + "/" + legacyMessage.name() + " "
                                        + legacyField.getNumber() + "/" + legacyField.getName()
                                        + " old:" + legacyField.getSize()
                                        + " new:" + newField.getSize()
                                );
                            }
                            if (newField.getBaseType() != legacyField.getBaseType()) {
                                foundProblem = true;
                                System.out.println("unexpected field base type: "
                                        + legacyMessage.getNumber() + "/" + legacyMessage.name() + " "
                                        + legacyField.getNumber() + "/" + legacyField.getName()
                                        + " old:" + legacyField.getBaseType()
                                        + " new:" + newField.getBaseType()
                                );
                            }
                            if (newField.getType() != legacyField.getType()) {
                                foundProblem = true;
                                System.out.println("unexpected field type: "
                                        + legacyMessage.getNumber() + "/" + legacyMessage.name() + " "
                                        + legacyField.getNumber() + "/" + legacyField.getName()
                                        + " old:" + legacyField.getType()
                                        + " new:" + newField.getType()
                                );
                            }
                        }
                    }
                    if (!foundField) {
                        foundProblem = true;
                        System.out.println("field not defined: "
                                + legacyMessage.getNumber() + "/" + legacyMessage.name() + " "
                                + legacyField.getNumber() + "/" + legacyField.getName());
                    }
                }
            }
        }
        if (foundProblem) {
            throw new RuntimeException("error in NativeFITMessage3");
        }
    }
}
