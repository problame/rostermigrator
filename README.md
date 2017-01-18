An interactive CLI for migrating your Jabber account's roster.

**This tool was a quick hack for personal needs and is unmaintained.**

## Dependencies
Babbler ~0.7.2 [https://bitbucket.org/sco0ter/babbler/downloads]

## Build

```bash
javac -cp babbler-0.7.2.jar:./ RosterMigratorCLI.java
```

## Run

```
java -cp babbler-0.7.2.jar:./ Client <src_jid> <src_pw> <dest_jid> <dest_pw> <subscription_status_text>

Usage:
  src_jid    The Jabber ID at the source server (host name will be derived from domain-part of jid).
             You can include a resource (user@domain/resource), e.g. /export
  src_pw     The password at the src server
  dest_jid   The Jabber ID at the destination server (host name will be derived from domain-part of jid)
             You can include a resource (user@domain/resource), e.g. /import
  subscription_status_text
             Text sent as part of a subscription request.

```

The tool builds a diff of the contacts only present on the `src` roster.

It then prompts interactively whether

  a) the account should be added to the dest roster
  b) (if the above option is answered with 'y') whether a subscription request should be sent

A contact is identified by the attributes listed in `ContactProjection.equals()`:

```java
  c.getJid().getLocal() + c.getJid().getDomain() + c.getName()
```

## License
See source file.
