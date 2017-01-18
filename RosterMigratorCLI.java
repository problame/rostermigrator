/* RosterMigratorCLI
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Christian Schwarz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

import rocks.xmpp.core.*;
import rocks.xmpp.core.session.*;
import rocks.xmpp.core.stanza.model.*;
import rocks.xmpp.addr.*;
import rocks.xmpp.im.roster.*;
import rocks.xmpp.im.roster.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class RosterMigratorCLI {
    public static void main(String[] args) {
        InteractiveRosterMigrator m = new InteractiveRosterMigrator();
        m.run(args);
    }
}

class InteractiveRosterMigrator {

    public static RosterManager loginAndRosterManager(String jidStr, String pw) throws XmppException {
        Jid jid = Jid.of(jidStr);
        TcpConnectionConfiguration tcpConfiguration = TcpConnectionConfiguration.builder()
            .port(5222)
            .secure(true)
            .build();
        XmppSessionConfiguration config = XmppSessionConfiguration.builder()
            .authenticationMechanisms("PLAIN")
            .build();
        XmppClient c = XmppClient.create(jid.getDomain(), config, tcpConfiguration);
        c.connect();
        c.login(jid.getLocal(), pw, jid.getResource());
        return c.getManager(RosterManager.class);
    }

    public static void subscribeAll(RosterManager rmFrom, RosterManager rmTo) {
        try {
            List<Contact> fromContacts = rmFrom.requestRoster().get().getContacts();
            for (Contact c : fromContacts) {
                System.out.printf("Found contact %s@%s (%s)\n", c.getJid().getLocal(), c.getJid().getDomain(), c.getName());
                rmTo.addContact(c, false, null).get();
            }
		} catch (InterruptedException e) {
            System.out.println("interrupted"); 
        } catch (ExecutionException e) {
            System.out.println(e);
        }

    }

    public ContactProjection mapContact(Contact c) {
        ContactProjection p = new ContactProjection();
        p.contact = c;
        return p;
    }

	public void run(String[] args) {

        if (args.length < 5) {
            System.out.println("ERROR: insufficient number of command line arguments. Read the code...");
            return;
        }

        RosterManager rmTo, rmFrom;
        try {
            rmTo = loginAndRosterManager(args[2], args[3]);
            rmFrom = loginAndRosterManager(args[0], args[1]);
        } catch (XmppException e) {
            System.out.printf("ERROR connecting to servers: %s\n", e);
            return;
        }

        List<ContactProjection> fromProj, toProj; 
        try {
            fromProj = rmFrom.requestRoster().get().getContacts().stream()
                .map(elem -> mapContact(elem))
                .collect(Collectors.toList());

            toProj = rmTo.requestRoster().get().getContacts().stream()
                .map(elem -> mapContact(elem))
                .collect(Collectors.toList());
        } catch (InterruptedException e) {
            System.out.printf("ERROR: could not load rosters: %s\n", e);
            return;
        } catch (ExecutionException e) {
            System.out.printf("ERROR: could not load rosters: %s\n", e);
            return;
        }


        List<ContactProjection> onlyFrom = fromProj.stream()
            .filter(elem -> !toProj.contains(elem))
            .collect(Collectors.toList());

        Scanner keyboard  = new Scanner(System.in);
        for (ContactProjection c : onlyFrom) {
            System.out.printf("Only in source account: %s@%s (name='%s', subscription=%s)\n",
                    c.contact.getJid().getLocal(),
                    c.contact.getJid().getDomain(),
                    c.contact.getName(),
                    c.contact.getSubscription());
            System.out.print("add? [y/n] ");
            String add = keyboard.next();
            if (add.startsWith("y")) {
                System.out.println("adding contact");
                System.out.print("send subscription request? [y/n] ");
                String sub = keyboard.next();
                boolean shouldSubscribe = sub.startsWith("y");
                if (shouldSubscribe) {
                    System.out.println("subscribing to contact");
                }
                try {
                    rmTo.addContact(c.contact, shouldSubscribe, args[4]).get();
                } catch (InterruptedException e) {
                    System.out.printf("ERROR: could not subscribe: %s\n", e);
                    return;
                } catch (ExecutionException e) {
                    System.out.printf("ERROR: could not subscribe: %s\n", e);
                    return;
                }
            }

        }
	}
}

class ContactProjection {

    public Contact contact;

    private final String contactKey(Contact c) {
        return c.getJid().getLocal() + c.getJid().getDomain() + c.getName();
    }

    @Override
    public boolean equals(Object p)
    {
        return contactKey(this.contact).equals(contactKey(((ContactProjection)p).contact));
    }
}

