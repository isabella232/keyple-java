/********************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.calypso.command.po.builder;


import org.eclipse.keyple.calypso.command.PoClass;
import org.eclipse.keyple.calypso.command.po.*;
import org.eclipse.keyple.calypso.command.po.parser.DecreaseRespPars;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;

/**
 * The Class DecreaseCmdBuild. This class provides the dedicated constructor to build the Decrease
 * APDU command.
 *
 */
public final class DecreaseCmdBuild extends AbstractPoUserCommandBuilder<DecreaseRespPars> {

    /** The command. */
    private static final CalypsoPoCommands command = CalypsoPoCommands.DECREASE;

    /* Construction arguments */
    private final int sfi;
    private final int counterNumber;
    private final int decValue;

    /**
     * Instantiates a new decrease cmd build from command parameters.
     *
     * @param poClass indicates which CLA byte should be used for the Apdu
     * @param sfi SFI of the file to select or 00h for current EF
     * @param counterNumber &gt;= 01h: Counters file, number of the counter. 00h: Simulated Counter
     *        file.
     * @param decValue Value to subtract to the counter (defined as a positive int &lt;= 16777215
     *        [FFFFFFh])
     * @param extraInfo extra information included in the logs (can be null or empty)
     * @throws IllegalArgumentException - if the decrement value is out of range
     * @throws IllegalArgumentException - if the command is inconsistent
     */

    public DecreaseCmdBuild(PoClass poClass, byte sfi, byte counterNumber, int decValue,
            String extraInfo) throws IllegalArgumentException {
        super(command, null);

        // only counter number >= 1 are allowed
        if (counterNumber < 1) {
            throw new IllegalArgumentException("Counter number out of range!");
        }

        // check if the incValue is in the allowed interval
        if (decValue < 0 || decValue > 0xFFFFFF) {
            throw new IllegalArgumentException("Decrement value out of range!");
        }

        // TODO complete argument checking
        byte cla = poClass.getValue();
        this.sfi = sfi;
        this.counterNumber = counterNumber;
        this.decValue = decValue;

        // convert the integer value into a 3-byte buffer
        byte[] decValueBuffer = new byte[3];
        decValueBuffer[0] = (byte) ((decValue >> 16) & 0xFF);
        decValueBuffer[1] = (byte) ((decValue >> 8) & 0xFF);
        decValueBuffer[2] = (byte) (decValue & 0xFF);

        byte p2 = (byte) (sfi * 8);

        /* this is a case4 command, we set Le = 0 */
        this.request = setApduRequest(cla, command, counterNumber, p2, decValueBuffer, (byte) 0);
        if (extraInfo != null) {
            this.addSubName(extraInfo);
        }
    }

    @Override
    public DecreaseRespPars createResponseParser(ApduResponse apduResponse) {
        return new DecreaseRespPars(apduResponse, this);
    }

    /**
     * This command consumes 6 + 3 bytes in the session buffer
     * 
     * @return 9
     */
    @Override
    public int getSessionBufferSizeConsumed() {
        return 9;
    }

    public int getSfi() {
        return sfi;
    }

    public int getCounterNumber() {
        return counterNumber;
    }

    public int getDecValue() {
        return decValue;
    }
}
