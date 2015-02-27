## 0.0.5-SNAPSHOT

## 0.0.4 (2015-02-26)

Changes:

  - Implement MBean for monitoring
  - Properly deserialise GetTickets in RegistryClient
    to return all tickets.
    - Fixes bootstrapping and ticketregistry cleaning
  - Improve logging
    - Log server availability only if it changes.

## 0.0.3 (2014-10-08)

Changes:

  - Bootstrapping local Provider at startup.
  - Properly cleanup client socket and create a
    new one when a timeout occurs and it becomes
    'confused'.
  - Do not hard-code the requestTimeout, allow it
    to be provided via the Registry Constructor.

## 0.0.2 (2014-09-14)

Changes:

  - Reuse ZMQ Sockets... do not recreate it.
  - Use providerId to determine local ticketProvider

## 0.0.1 (2014-06-16)

Changes:

  - Initial Release
  - Implement simple TicketRegistry
