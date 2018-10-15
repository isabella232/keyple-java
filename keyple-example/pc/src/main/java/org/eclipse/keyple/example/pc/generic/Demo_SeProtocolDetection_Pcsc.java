/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License version 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 */

package org.eclipse.keyple.example.pc.generic;

import static org.eclipse.keyple.example.common.generic.DemoHelpers.getReaderByName;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import org.eclipse.keyple.example.common.generic.CustomProtocolSetting;
import org.eclipse.keyple.example.common.generic.Demo_SeProtocolDetectionEngine;
import org.eclipse.keyple.plugin.pcsc.PcscPlugin;
import org.eclipse.keyple.plugin.pcsc.PcscProtocolSetting;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.seproxy.*;
import org.eclipse.keyple.seproxy.event.ObservableReader;
import org.eclipse.keyple.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.seproxy.protocol.ContactlessProtocols;
import org.eclipse.keyple.seproxy.protocol.SeProtocolSetting;

/**
 * This class handles the reader events generated by the SeProxyService
 */
public class Demo_SeProtocolDetection_Pcsc {

    public Demo_SeProtocolDetection_Pcsc() {
        super();
    }

    /**
     * Application entry
     *
     * @param args the program arguments
     * @throws IllegalArgumentException in case of a bad argument
     * @throws KeypleBaseException if a reader error occurs
     */
    public static void main(String[] args) throws IllegalArgumentException, KeypleBaseException {
        /* get the SeProxyService instance */
        SeProxyService seProxyService = SeProxyService.getInstance();

        /* add the PcscPlugin to the SeProxyService */
        PcscPlugin pcscPlugin = PcscPlugin.getInstance();

        seProxyService.addPlugin(pcscPlugin);

        /* attempt to get the ProxyReader (the right reader should be ready here) */
        ProxyReader poReader =
                getReaderByName(seProxyService, PcscReadersSettings.PO_READER_NAME_REGEX);

        if (poReader == null) {
            throw new IllegalStateException("Bad PO/CSM setup");
        }

        System.out.println("PO Reader  : " + poReader.getName());
        poReader.setParameter(PcscReader.SETTING_KEY_LOGGING, "true");

        /* create an observer class to handle the SE operations */
        Demo_SeProtocolDetectionEngine observer = new Demo_SeProtocolDetectionEngine();

        observer.setReader(poReader);

        /* configure reader */
        poReader.setParameter(PcscReader.SETTING_KEY_PROTOCOL, PcscReader.SETTING_PROTOCOL_T1);

        // Protocol detection settings.
        // add 8 expected protocols with three different methods:
        // - using addSeProtocolSetting
        // - using a custom enum
        // - using a protocol map and addSeProtocolSetting
        // A real application should use only one method.

        // Method 1
        // add protocols individually
        poReader.addSeProtocolSetting(
                new SeProtocolSetting(PcscProtocolSetting.SETTING_PROTOCOL_MEMORY_ST25));


        poReader.addSeProtocolSetting(
                new SeProtocolSetting(PcscProtocolSetting.SETTING_PROTOCOL_ISO14443_4));


        // Method 2
        // add all settings at once with setting enum
        poReader.addSeProtocolSetting(new SeProtocolSetting(CustomProtocolSetting.values()));

        // Method 3
        // create and fill a protocol map
        Map<SeProtocol, String> protocolsMap = new HashMap<SeProtocol, String>();

        protocolsMap.put(ContactlessProtocols.PROTOCOL_MIFARE_CLASSIC,
                PcscProtocolSetting.ProtocolSetting.REGEX_PROTOCOL_MIFARE_CLASSIC);

        protocolsMap.put(ContactlessProtocols.PROTOCOL_MIFARE_UL,
                PcscProtocolSetting.ProtocolSetting.REGEX_PROTOCOL_MIFARE_UL);

        // provide the reader with the map
        poReader.addSeProtocolSetting(new SeProtocolSetting(protocolsMap));

        // Set terminal as Observer of the first reader
        ((ObservableReader) poReader).addObserver(observer);

        // wait for Enter key to exit.
        System.out.println("Press Enter to exit");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            int c = 0;
            try {
                c = br.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (c == 0x0A) {
                System.out.println("Exiting...");
                System.exit(0);
            }
        }
    }
}
