/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.commands.po.parser;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.calypso.commands.po.parser.GetDataFciRespPars;
import org.eclipse.keyple.commands.AbstractApduResponseParser;
import org.eclipse.keyple.seproxy.ApduResponse;
import org.eclipse.keyple.seproxy.SeResponse;
import org.eclipse.keyple.seproxy.SeResponseSet;
import org.junit.Assert;
import org.junit.Test;

public class GetDataRespParsTest {

    @Test
    public void digestInitRespPars() {
        List<ApduResponse> listeResponse = new ArrayList<ApduResponse>();
        ApduResponse apduResponse =
                new ApduResponse(ByteBuffer.wrap(new byte[] {(byte) 0x90, 0x00}), true);
        listeResponse.add(apduResponse);
        SeResponseSet seResponse = new SeResponseSet(new SeResponse(true, null, listeResponse));

        AbstractApduResponseParser apduResponseParser =
                new GetDataFciRespPars(seResponse.getSingleElement().getApduResponses().get(0));

        ByteBuffer responseActual = apduResponseParser.getApduResponse().getBuffer();
        Assert.assertEquals(ByteBuffer.wrap(new byte[] {(byte) 0x90, 0x00}), responseActual);
    }
}
