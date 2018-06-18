/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.examples.pc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;
import org.eclipse.keyple.example.common.HoplinkSampleCommands;
import org.eclipse.keyple.plugin.pcsc.PcscPlugin;
import org.eclipse.keyple.plugin.pcsc.PcscProtocolSetting;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.seproxy.*;
import org.eclipse.keyple.seproxy.event.AbstractObservableReader;
import org.eclipse.keyple.seproxy.event.ReaderEvent;
import org.eclipse.keyple.seproxy.exception.IOReaderException;
import org.eclipse.keyple.seproxy.local.AbstractLocalReader;
import org.eclipse.keyple.seproxy.protocol.ContactlessProtocols;
import org.eclipse.keyple.seproxy.protocol.SeProtocolSettings;
import org.eclipse.keyple.util.ByteBufferUtils;
import org.eclipse.keyple.util.Observable;

/**
 * This code demonstrates the multi-protocols capability of the Keyple SeProxy:
 * <ul>
 * <li>instantiates a PC/SC plugin for a reader which name matches the regular expression provided
 * by poReaderName.</li>
 * <li>uses the observable mechanism to handle SE insertion/detection</li>
 * <li>expects SE with various protocols (technologies)</li>
 * <li>shows the identified protocol when a SE is detected</li>
 * <li>executes a simple Hoplink reading when a Hoplink SE is identified</li>
 * </ul>
 * The program spends most of its time waiting for a Enter key before exit. The actual SE processing
 * is mainly event driven through the observability.
 */

/**
 * This class handles the reader events generated by the SeProxyService
 */
public class KeypleGenericDemo_SeProtocolDetection
        implements AbstractObservableReader.Observer<ReaderEvent> {
    private ProxyReader poReader, csmReader;

    public KeypleGenericDemo_SeProtocolDetection() {
        super();
    }

    /**
     * This method is called whenever a reader event occurs.
     * 
     * @param observable
     * @param event
     */
    @Override
    public void update(Observable observable, ReaderEvent event) {
        switch (event) {
            case SE_INSERTED:
                System.out.println("SE INSERTED");
                System.out.println("\nStart processing of a PO");
                operatePoTransaction();
                break;
            case SE_REMOVAL:
                System.out.println("SE REMOVED");
                System.out.println("\nWait for PO");
                break;
            default:
                System.out.println("IO Error");
        }
    }

    /**
     * This method is called when a SE is inserted (or presented to the reader's antenna). It
     * executes a SeRequestSet and processes the SeResponseSet showing the APDUs exchanges
     */
    public void operatePoTransaction() {

        try {
            // create a request set:
            // * getting the SE UID for all SE protocols except ISO14443-4
            // * executing a Hoplink simple read scenario for ISO14443-4

            // create a list of requests
            Set<SeRequest> poRequests = new LinkedHashSet<SeRequest>();

            ApduRequest pcscContactlessReaderGetData =
                    new ApduRequest(ByteBufferUtils.fromHex("FFCA000000"), false);
            List<ApduRequest> pcscContactlessReaderGetDataList = new ArrayList<ApduRequest>();
            pcscContactlessReaderGetDataList.add(pcscContactlessReaderGetData);

            // process SDK defined protocols
            for (ContactlessProtocols protocol : ContactlessProtocols.values()) {
                switch (protocol) {
                    case PROTOCOL_ISO14443_4:
                        // get Apdu list from HoplinkSampleCommands class
                        // List<ApduRequest> poApduRequestList = new ArrayList<ApduRequest>();
                        List<ApduRequest> poApduRequestList = new ArrayList<ApduRequest>();
                        // add common get UID command
                        poApduRequestList.addAll(pcscContactlessReaderGetDataList);
                        // add Hoplink specific commands
                        poApduRequestList.addAll(HoplinkSampleCommands.getApduList());
                        // add a SeRequest with the AID from HoplinkSampleCommands and the requests
                        // list
                        poRequests.add(new SeRequest(HoplinkSampleCommands.getAid(),
                                poApduRequestList, false, protocol));
                        break;
                    case PROTOCOL_ISO14443_3A:
                    case PROTOCOL_ISO14443_3B:
                        // not handled in this demo code
                        break;
                    case PROTOCOL_MIFARE_DESFIRE:
                    case PROTOCOL_B_PRIME:
                        // intentionally ignored for demo purpose
                        break;
                    default:
                        poRequests.add(new SeRequest(null, pcscContactlessReaderGetDataList, false,
                                protocol));
                        break;
                }
            }

            // process application specific protocols
            for (CustomProtocols protocol : CustomProtocols.values()) {
                poRequests.add(
                        new SeRequest(null, pcscContactlessReaderGetDataList, false, protocol));
            }

            // create a SeRequestSet from the SeRequest list
            SeRequestSet poRequest = new SeRequestSet(poRequests);

            // execute request and get response
            SeResponseSet poResponse = poReader.transmit(poRequest);

            // output results
            Iterator<SeRequest> seReqIterator = poRequests.iterator();
            int requestIndex = 0;
            for (SeResponse seResponse : poResponse.getResponses()) {
                SeRequest seRequest = seReqIterator.next();

                if (seResponse != null) {
                    System.out.println("Protocol \"" + seRequest.getProtocolFlag().getName()
                            + "\" matched for request number " + String.valueOf(requestIndex));
                    List<ApduRequest> poApduRequestList = seRequest.getApduRequests();
                    List<ApduResponse> poApduResponseList = seResponse.getApduResponses();
                    for (int i = 0; i < poApduResponseList.size(); i++) {
                        System.out.println(" CMD: "
                                + ByteBufferUtils.toHex(poApduRequestList.get(i).getBytes()));
                        System.out.println("RESP: "
                                + ByteBufferUtils.toHex(poApduResponseList.get(i).getBytes()));
                    }
                }
                requestIndex++;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * This is where you should add patterns of readers you want to use for tests
     */

    private static final String poReaderName = ".*(ASK|ACS|SCM).*";

    /**
     * Get the terminal which names match the expected pattern
     *
     * @param seProxyService SE Proxy service
     * @param pattern Pattern
     * @return ProxyReader
     * @throws IOReaderException Any error with the card communication TODO make this method a
     *         generic helper?
     */
    private static ProxyReader getReaderByName(SeProxyService seProxyService, String pattern)
            throws IOReaderException {
        Pattern p = Pattern.compile(pattern);
        for (ReadersPlugin plugin : seProxyService.getPlugins()) {
            for (ProxyReader reader : plugin.getReaders()) {
                if (p.matcher(reader.getName()).matches()) {
                    return reader;
                }
            }
        }
        return null;
    }

    /**
     * Application entry
     * 
     * @param args
     * @throws IOException
     * @throws IOReaderException
     * @throws InterruptedException
     */
    public static void main(String[] args)
            throws IOException, IOReaderException, InterruptedException {
        // get the SeProxyService instance
        SeProxyService seProxyService = SeProxyService.getInstance();

        // add the PcscPlugin to the SeProxyService
        SortedSet<ReadersPlugin> pluginsSet = new ConcurrentSkipListSet<ReadersPlugin>();
        pluginsSet.add(PcscPlugin.getInstance().setLogging(false));
        seProxyService.setPlugins(pluginsSet);

        // attempt to get the ProxyReader (the right reader should be ready here)
        ProxyReader poReader = getReaderByName(seProxyService, poReaderName);

        if (poReader == null) {
            throw new IllegalStateException("Bad PO/CSM setup");
        }

        System.out.println("PO Reader  : " + poReader.getName());

        // create an observer class to handle the SE operations
        KeypleGenericDemo_SeProtocolDetection observer =
                new KeypleGenericDemo_SeProtocolDetection();

        // configure reader
        observer.poReader = poReader;
        observer.poReader.setParameter(PcscReader.SETTING_KEY_PROTOCOL,
                PcscReader.SETTING_PROTOCOL_T1);

        // Protocol detection settings.
        // add 8 expected protocols with three different methods:
        // - using addSeProtocolSetting
        // - using a custom enum
        // - using a protocol map and addSeProtocolSetting
        // A real application should use only one method.

        // Method 1
        // add protocols individually
        ((AbstractLocalReader) observer.poReader)
                .addSeProtocolSetting(PcscProtocolSetting.SETTING_PROTOCOL_MEMORY_ST25);


        ((AbstractLocalReader) observer.poReader)
                .addSeProtocolSetting(PcscProtocolSetting.SETTING_PROTOCOL_ISO14443_4);


        // Method 2
        // add all settings at once with setting enum
        ((AbstractLocalReader) observer.poReader).addSeProtocolSetting(CustomProtocolSetting.class);

        // Method 3
        // create and fill a protocol map
        Map<SeProtocol, String> protocolsMap = new HashMap<SeProtocol, String>();

        protocolsMap.put(ContactlessProtocols.PROTOCOL_MIFARE_CLASSIC,
                PcscProtocolSetting.ProtocolSetting.REGEX_PROTOCOL_MIFARE_CLASSIC);

        protocolsMap.put(ContactlessProtocols.PROTOCOL_MIFARE_UL,
                PcscProtocolSetting.ProtocolSetting.REGEX_PROTOCOL_MIFARE_UL);

        // provide the reader with the map
        ((AbstractLocalReader) observer.poReader).addSeProtocolSetting(protocolsMap);

        // Set terminal as Observer of the first reader
        ((AbstractObservableReader) observer.poReader).addObserver(observer);

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


/**
 * Custom protocol definitions to illustrate the extension of the Keyple SDK definitions
 */
enum CustomProtocols implements SeProtocol {
    CUSTOM_PROTOCOL_B_PRIME("Custom Old Calypso B prime"), CUSTOM_PROTOCOL_MIFARE_DESFIRE(
            "Custom Mifare DESFire");

    /** The protocol name. */
    private String name;

    CustomProtocols(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}


/**
 * Custom protocol setting definitions to illustrate the extension of the Keyple SDK definitions
 */
enum CustomProtocolSetting implements SeProtocolSettings {
    CUSTOM_SETTING_PROTOCOL_B_PRIME(CustomProtocols.CUSTOM_PROTOCOL_B_PRIME,
            "3B8F8001805A0A0103200311........829000.."), CUSTOM_SETTING_PROTOCOL_ISO14443_4(
                    CustomProtocols.CUSTOM_PROTOCOL_MIFARE_DESFIRE, "3B8180018080");

    /* the protocol flag */
    SeProtocol flag;

    /* the protocol setting value */
    String value;

    CustomProtocolSetting(SeProtocol flag, String value) {
        this.flag = flag;
        this.value = value;
    }

    @Override
    public SeProtocol getFlag() {
        return flag;
    }

    @Override
    public String getValue() {
        return value;
    }
}
