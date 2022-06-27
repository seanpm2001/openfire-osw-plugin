/*
 *  Copyright 2010 Vodafone Group Services Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *    
 */
package org.onesocialweb.openfire.handler.relation;

import java.util.List;

import org.dom4j.dom.DOMDocument;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onesocialweb.model.relation.Relation;
import org.onesocialweb.openfire.manager.RelationManager;
import org.onesocialweb.xml.dom.RelationDomWriter;
import org.onesocialweb.xml.dom.imp.DefaultRelationDomWriter;
import org.w3c.dom.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

public class IQRelationQueryHandler extends IQHandler {
    private static final Logger Log = LoggerFactory.getLogger(IQRelationQueryHandler.class);
	public static final String NAME = "query";
	
	public static final String NAMESPACE = "https://igniterealtime.github.io/openfire-osw-plugin//spec/1.0/relations#query";
	
	private final IQHandlerInfo info = new IQHandlerInfo(NAME, NAMESPACE);

	private UserManager userManager; 

	public IQRelationQueryHandler() {
		super("OneSocialWeb - Query a user relations");
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}

	@SuppressWarnings( { "deprecation" })
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		JID sender = packet.getFrom();
		JID target = packet.getTo();
		
		// Process the request inside a try/catch so that unhandled exceptions
		// (oufofbounds etc...) can trigger a server error and we can send a
		// error result packet
		try {
			// If no recipient, then the target is the sender
			if (target == null || target.getNode() == null) {
				target = packet.getFrom();
			}
			
			// A valid request is an IQ of type get, for a valid and local recipient
			if (!(packet.getType().equals(IQ.Type.get) && target != null && target.getNode() != null 
					&& userManager.isRegisteredUser(target.getNode()))) {
				IQ result = IQ.createResultIQ(packet);
				result.setChildElement(packet.getChildElement().createCopy());
				result.setError(PacketError.Condition.bad_request);
				return result;
			}
			
			// We fetch the relations of the target user
			List<Relation> relations = RelationManager.getInstance().getRelations(sender.toBareJID(), target.toBareJID());
			
			// Prepare the result packet
			RelationDomWriter writer = new DefaultRelationDomWriter();
			DOMDocument domDocument = new DOMDocument();
			IQ result = IQ.createResultIQ(packet);
			Element query = (Element) domDocument.appendChild(domDocument.createElementNS(NAMESPACE, NAME));
			for (Relation relation : relations) {
				writer.toElement(relation,query);
			}
			result.setChildElement((org.dom4j.Element) query);
			// Return and send the result packet
			return result;
			
		} catch (Exception e) {
			Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
			IQ result = IQ.createResultIQ(packet);
			result.setChildElement(packet.getChildElement().createCopy());
			result.setError(PacketError.Condition.internal_server_error);
			return result;
		}
	}

	@Override
	public void initialize(XMPPServer server) {
		super.initialize(server);
		userManager = server.getUserManager();
	}
}
