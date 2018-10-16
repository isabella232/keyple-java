/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.calypso.command.po;

import org.eclipse.keyple.command.AbstractIso7816CommandBuilder;
import org.eclipse.keyple.seproxy.ApduRequest;

/**
 * Class to build custom (non-referenced) read PO commands
 */
public class PoCustomReadCommandBuilder extends AbstractIso7816CommandBuilder
        implements PoSendableInSession {

    protected PoRevision defaultRevision = PoRevision.REV3_1;

    /**
     * Constructor dedicated to the construction of user-defined commands.
     *
     * Caveat:
     * <ul>
     * <li>the caller has to provide all the command data.</li>
     * <li>Using this method bypasses the security and functional verification mechanisms of the
     * PoTransaction API.
     * <p>
     * It is done at the user's risk.</li>
     * </ul>
     * 
     * @param name the name of the command (will appear in the ApduRequest log)
     * @param request the ApduRequest (the correct instruction byte must be provided)
     */
    public PoCustomReadCommandBuilder(String name, ApduRequest request) {
        super("PO Custom Read Command: " + name, request);
    }
}