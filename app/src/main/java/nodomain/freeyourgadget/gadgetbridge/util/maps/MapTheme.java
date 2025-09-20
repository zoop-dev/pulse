package nodomain.freeyourgadget.gadgetbridge.util.maps;

import androidx.annotation.Nullable;

import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;

import java.io.InputStream;

public enum MapTheme implements XmlRenderTheme {
    DEFAULT("/assets/mapsforge/default.xml"),
    BIKER("/assets/mapsforge/biker.xml"),
    DARK("/assets/mapsforge/dark.xml"),
    INDIGO("/assets/mapsforge/indigo.xml"),
    MOTORIDER("/assets/mapsforge/motorider.xml"),
    OSMARENDER("/assets/mapsforge/osmarender.xml"),
    ;

    private XmlRenderThemeMenuCallback menuCallback;
    private final String path;

    MapTheme(final String path) {
        this.path = path;
    }

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return menuCallback;
    }

    @Override
    public String getRelativePathPrefix() {
        return "/assets/";
    }

    @Override
    public InputStream getRenderThemeAsStream() {
        return getClass().getResourceAsStream(this.path);
    }

    @Nullable
    @Override
    public XmlThemeResourceProvider getResourceProvider() {
        return null;
    }

    @Override
    public void setMenuCallback(final XmlRenderThemeMenuCallback menuCallback) {
        this.menuCallback = menuCallback;
    }

    @Override
    public void setResourceProvider(final XmlThemeResourceProvider resourceProvider) {

    }

    public String getPath() {
        return path;
    }
}
