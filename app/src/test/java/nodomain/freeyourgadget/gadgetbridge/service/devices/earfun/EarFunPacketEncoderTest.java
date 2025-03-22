package nodomain.freeyourgadget.gadgetbridge.service.devices.earfun;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Equalizer;
import nodomain.freeyourgadget.gadgetbridge.service.devices.earfun.prefs.Interactions;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class EarFunPacketEncoderTest {
    Prefs mockPrefs = mock(Prefs.class);

    @Test
    public void testEncodeSetGesture() {
        when(mockPrefs.getString(anyString(), anyString())).thenReturn("1");

        byte[] expected = new EarFunPacket(EarFunPacket.Command.SET_TOUCH_ACTION, new byte[]{1, 1, 0, 0}).encode();
        assertArrayEquals(expected, EarFunPacketEncoder.encodeSetGesture(mockPrefs, "anyKey",
                Interactions.InteractionType.SINGLE,
                Interactions.Position.LEFT)
        );
    }

    @Test
    public void testEncodeSetEqualizerSixBands() {
        for (Equalizer.BandConfig bandConfig : Equalizer.SixBandEqualizer) {
            if (bandConfig.key != null) {
                when(mockPrefs.getInt(bandConfig.key, 0)).thenReturn(5);
            }
        }

        byte[] result = EarFunPacketEncoder.encodeSetEqualizerSixBands(mockPrefs);
        assertNotNull(result);
        assertEquals(170, result.length);
    }

    @Test
    public void testEncodeSetEqualizerTenBands() {
        for (Equalizer.BandConfig bandConfig : Equalizer.TenBandEqualizer) {
            if (bandConfig.key != null) {
                when(mockPrefs.getInt(bandConfig.key, 0)).thenReturn(-5);
            }
        }

        byte[] result = EarFunPacketEncoder.encodeSetEqualizerTenBands(mockPrefs);
        assertNotNull(result);
        assertEquals(170, result.length);
    }

    @Test
    public void testEncodeFirmwareVersionReq() {
        byte[] encoded = EarFunPacketEncoder.encodeFirmwareVersionReq();

        assertNotNull(encoded);
        assertEquals(8, encoded.length);
    }

    @Test
    public void testJoinPacketsMultipleArrays() {
        byte[] array1 = {1, 2, 3};
        byte[] array2 = {4, 5, 6};
        byte[] array3 = {7, 8, 9};
        byte[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        byte[] result = EarFunPacketEncoder.joinPackets(array1, array2, array3);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testJoinPacketsEmptyArray() {
        byte[] array1 = {};
        byte[] array2 = {1, 2, 3};
        byte[] expected = {1, 2, 3};

        byte[] result = EarFunPacketEncoder.joinPackets(array1, array2);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testJoinPacketsSingleArray() {
        byte[] array1 = {1, 2, 3};
        byte[] expected = {1, 2, 3};

        byte[] result = EarFunPacketEncoder.joinPackets(array1);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testJoinPacketsNoArrays() {
        byte[] expected = {};

        byte[] result = EarFunPacketEncoder.joinPackets();
        assertArrayEquals(expected, result);
    }

    @Test
    public void testJoinPacketsDifferentLengths() {
        byte[] array1 = {1, 2};
        byte[] array2 = {3, 4, 5, 6};
        byte[] array3 = {7};
        byte[] expected = {1, 2, 3, 4, 5, 6, 7};

        byte[] result = EarFunPacketEncoder.joinPackets(array1, array2, array3);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testJoinPacketsListInput() {
        List<byte[]> arrays = Arrays.asList(new byte[]{1, 2}, new byte[]{3, 4, 5}, new byte[]{6, 7, 8, 9});
        byte[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        byte[] result = EarFunPacketEncoder.joinPackets(arrays);
        assertArrayEquals(expected, result);
    }
}
