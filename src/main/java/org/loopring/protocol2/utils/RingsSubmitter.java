package org.loopring.protocol2.utils;

import java.io.InputStream;
import java.util.Properties;
import org.loopring.protocol2.pojos.*;

public class RingsSubmitter {

    private String propsFile = "/contract-addrs.properties";
    private String lrcAddress;
    private RingsEncoder encoder;

    public RingsSubmitter() {
        loadProperties(propsFile);
        encoder = new RingsEncoder(lrcAddress);
    }

    public void submitRings(Rings rings) {

    }

    private void loadProperties(String file) {
        try {
            InputStream ins = this.getClass().getResourceAsStream(file);
            Properties props = new Properties();
            props.load(ins);
            lrcAddress = props.getProperty("lrc_address");
            System.out.println("lrcAddress:" + lrcAddress);
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

}
