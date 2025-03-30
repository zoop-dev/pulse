package nodomain.freeyourgadget.gadgetbridge.activities.licenses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class LicensesActivityTest extends TestBase {
    @Test
    public void ensureAllLicenseFilesExist() throws IOException {
        final Type listType = new TypeToken<List<License>>() {}.getType();
        final InputStream in = getContext().getAssets().open("licenses/licenses.json");
        final List<License> licenses = new Gson().fromJson(new InputStreamReader(in), listType);
        Assert.assertFalse("Licenses shouldn't be empty", licenses.isEmpty());
        for (final License license : licenses) {
            if (license.getPath() != null) {
                Assert.assertNotNull(getContext().getAssets().open(license.getPath()));
            }
        }
    }
}
