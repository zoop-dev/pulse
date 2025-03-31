/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.ota;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class HuaweiOTAFileList {

    static public class OTAComponent {
        public String name;
        public int compress;
    }

    static public class OTAFileInfo {
        public String spath;
        public String dpath;
        public String operation;
        public String md5;
        public String sha256;
        public long size;
        public String packageName;
        public String versionName;
        public String osVersion;
        public int versionCode;
    }

    public OTAComponent component = new OTAComponent();
    public List<OTAFileInfo> files = new ArrayList<>();


    public static HuaweiOTAFileList getFileList(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(
                    xmlStr)));


            HuaweiOTAFileList ret = new HuaweiOTAFileList();

            NodeList component = doc.getElementsByTagName("component");
            if (component.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) component.item(0);
                ret.component.name = elem.getElementsByTagName("name").item(0).getTextContent();
                ret.component.compress = Integer.parseInt(elem.getElementsByTagName("compress").item(0).getTextContent());
            }

            NodeList files = doc.getElementsByTagName("file");
            for (int i = 0; i < files.getLength(); i++) {
                    Node fnode = files.item(i);

                    if (fnode.getNodeType() == Node.ELEMENT_NODE) {

                        Element elem = (Element) fnode;
                        OTAFileInfo info = new OTAFileInfo();

                        info.spath = elem.getElementsByTagName("spath").item(0).getTextContent();
                        info.dpath = elem.getElementsByTagName("dpath").item(0).getTextContent();
                        info.operation = elem.getElementsByTagName("operation").item(0).getTextContent();
                        info.md5 = elem.getElementsByTagName("md5").item(0).getTextContent();
                        info.sha256 = elem.getElementsByTagName("sha256").item(0).getTextContent();
                        info.size = Long.parseLong(elem.getElementsByTagName("size").item(0).getTextContent());
                        info.packageName = elem.getElementsByTagName("packageName").item(0).getTextContent();
                        info.versionName = elem.getElementsByTagName("versionName").item(0).getTextContent();
                        NodeList osVerNodes = elem.getElementsByTagName("osVersion");
                        if(osVerNodes.getLength() > 0) {
                            info.osVersion = osVerNodes.item(0).getTextContent();
                        }
                        info.versionCode = Integer.parseInt(elem.getElementsByTagName("versionCode").item(0).getTextContent());

                        ret.files.add(info);
                    }
                }
            return ret;
        } catch (Exception e) {
            return null;
        }

    }

}
