/*  Copyright (C) 2023-2026 Thomas Riedler

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class ZeppOsNotificationServiceTest extends TestBase {
    private Method getTargetDimensionsMethod() throws NoSuchMethodException {
        Method method = ZeppOsNotificationService.class.getDeclaredMethod("getTargetDimensions", int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return method;
    }

    private int[] getTargetDimensions(int requestWidth, int requestHeight, int bmpWidth, int bmpHeight) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object targetDimensions = getTargetDimensionsMethod().invoke(null, requestWidth, requestHeight, bmpWidth, bmpHeight);
        Class<?> recordClass = targetDimensions.getClass();

        int actualTargetHeight = (int) recordClass.getDeclaredMethod("targetHeight").invoke(targetDimensions);
        int actualTargetWidth = (int) recordClass.getDeclaredMethod("targetWidth").invoke(targetDimensions);
        int actualNotificationWidth = (int) recordClass.getDeclaredMethod("notificationWidth").invoke(targetDimensions);

        return new int[]{actualTargetHeight, actualTargetWidth, actualNotificationWidth};
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait2x3_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 705, 1057);
        assertEquals("Target Width mismatch", 160, dims[1]);
        assertEquals("Target Height mismatch", 240, dims[0]);
        assertEquals("Notification Width mismatch", 159, dims[2]);
        assertEquals("Notification Height mismatch", 240, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait2x3_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 705, 1057);
        assertEquals("Target Width mismatch", 432, dims[1]);
        assertEquals("Target Height mismatch", 632, dims[0]);
        assertEquals("Notification Width mismatch", 421, dims[2]);
        assertEquals("Notification Height mismatch", 632, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait3x4_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 642, 855);
        assertEquals("Target Width mismatch", 192, dims[1]);
        assertEquals("Target Height mismatch", 240, dims[0]);
        assertEquals("Notification Width mismatch", 180, dims[2]);
        assertEquals("Notification Height mismatch", 240, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait3x4_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 642, 855);
        assertEquals("Target Width mismatch", 480, dims[1]);
        assertEquals("Target Height mismatch", 632, dims[0]);
        assertEquals("Notification Width mismatch", 474, dims[2]);
        assertEquals("Notification Height mismatch", 632, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait3x5_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 686, 1143);
        assertEquals("Target Width mismatch", 144, dims[1]);
        assertEquals("Target Height mismatch", 240, dims[0]);
        assertEquals("Notification Width mismatch", 143, dims[2]);
        assertEquals("Notification Height mismatch", 240, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait3x5_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 686, 1143);
        assertEquals("Target Width mismatch", 384, dims[1]);
        assertEquals("Target Height mismatch", 632, dims[0]);
        assertEquals("Notification Width mismatch", 379, dims[2]);
        assertEquals("Notification Height mismatch", 632, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait4x5_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 756, 945);
        assertEquals("Target Width mismatch", 192, dims[1]);
        assertEquals("Target Height mismatch", 240, dims[0]);
        assertEquals("Notification Width mismatch", 191, dims[2]);
        assertEquals("Notification Height mismatch", 240, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait4x5_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 756, 945);
        assertEquals("Target Width mismatch", 512, dims[1]);
        assertEquals("Target Height mismatch", 632, dims[0]);
        assertEquals("Notification Width mismatch", 505, dims[2]);
        assertEquals("Notification Height mismatch", 632, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait9x16_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 756, 1344);
        assertEquals("Target Width mismatch", 144, dims[1]);
        assertEquals("Target Height mismatch", 240, dims[0]);
        assertEquals("Notification Width mismatch", 134, dims[2]);
        assertEquals("Notification Height mismatch", 240, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Portrait9x16_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 756, 1344);
        assertEquals("Target Width mismatch", 368, dims[1]);
        assertEquals("Target Height mismatch", 632, dims[0]);
        assertEquals("Notification Width mismatch", 355, dims[2]);
        assertEquals("Notification Height mismatch", 632, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape3x2_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 1008, 672);
        assertEquals("Target Width mismatch", 240, dims[1]);
        assertEquals("Target Height mismatch", 159, dims[0]);
        assertEquals("Notification Width mismatch", 240, dims[2]);
        assertEquals("Notification Height mismatch", 159, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape3x2_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 1008, 672);
        assertEquals("Target Width mismatch", 400, dims[1]);
        assertEquals("Target Height mismatch", 263, dims[0]);
        assertEquals("Notification Width mismatch", 396, dims[2]);
        assertEquals("Notification Height mismatch", 263, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape4x3_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 1008, 756);
        assertEquals("Target Width mismatch", 240, dims[1]);
        assertEquals("Target Height mismatch", 180, dims[0]);
        assertEquals("Notification Width mismatch", 240, dims[2]);
        assertEquals("Notification Height mismatch", 180, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape4x3_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 1008, 756);
        assertEquals("Target Width mismatch", 400, dims[1]);
        assertEquals("Target Height mismatch", 297, dims[0]);
        assertEquals("Notification Width mismatch", 396, dims[2]);
        assertEquals("Notification Height mismatch", 297, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape5x3_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 1008, 604);
        assertEquals("Target Width mismatch", 240, dims[1]);
        assertEquals("Target Height mismatch", 143, dims[0]);
        assertEquals("Notification Width mismatch", 240, dims[2]);
        assertEquals("Notification Height mismatch", 143, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape5x3_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 1008, 604);
        assertEquals("Target Width mismatch", 400, dims[1]);
        assertEquals("Target Height mismatch", 236, dims[0]);
        assertEquals("Notification Width mismatch", 396, dims[2]);
        assertEquals("Notification Height mismatch", 236, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape5x4_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 945, 756);
        assertEquals("Target Width mismatch", 240, dims[1]);
        assertEquals("Target Height mismatch", 192, dims[0]);
        assertEquals("Notification Width mismatch", 240, dims[2]);
        assertEquals("Notification Height mismatch", 192, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape5x4_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 945, 756);
        assertEquals("Target Width mismatch", 400, dims[1]);
        assertEquals("Target Height mismatch", 316, dims[0]);
        assertEquals("Notification Width mismatch", 396, dims[2]);
        assertEquals("Notification Height mismatch", 316, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape16x9_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 1008, 567);
        assertEquals("Target Width mismatch", 240, dims[1]);
        assertEquals("Target Height mismatch", 134, dims[0]);
        assertEquals("Notification Width mismatch", 240, dims[2]);
        assertEquals("Notification Height mismatch", 134, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Landscape16x9_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 1008, 567);
        assertEquals("Target Width mismatch", 400, dims[1]);
        assertEquals("Target Height mismatch", 222, dims[0]);
        assertEquals("Notification Width mismatch", 396, dims[2]);
        assertEquals("Notification Height mismatch", 222, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Quadratic_small() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(240, 240, 743, 743);
        assertEquals("Target Width mismatch", 240, dims[1]);
        assertEquals("Target Height mismatch", 240, dims[0]);
        assertEquals("Notification Width mismatch", 240, dims[2]);
        assertEquals("Notification Height mismatch", 240, dims[0]);
    }

    @Test
    public void testBalance2XTNotificationDimensions_Quadratic_full() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        int[] dims = getTargetDimensions(396, 932, 743, 743);
        assertEquals("Target Width mismatch", 400, dims[1]);
        assertEquals("Target Height mismatch", 396, dims[0]);
        assertEquals("Notification Width mismatch", 396, dims[2]);
        assertEquals("Notification Height mismatch", 396, dims[0]);
    }

}
