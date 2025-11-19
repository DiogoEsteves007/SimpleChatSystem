# SimpleChatSystem

## Overview

SimpleChatSystem is a multi-user chat application built in Java. It features a centralized server for public group chat and incorporates Java RMI for direct, peer-to-peer private messaging. The system also includes a security layer for private messages, using RSA digital signatures to verify the sender's identity.

## Features

*   **Multi-User Public Chat:** A global chat room where all connected users can communicate.
*   **Peer-to-Peer Private Messaging:** Utilizes Java RMI to allow users to send private messages directly to each other without going through the central server.
*   **Secure Signed Messages:** Private messages can be digitally signed using an RSA key pair, allowing the recipient to verify the sender's identity and ensure message integrity.
*   **User Presence Management:** The server tracks active users, announces when users join or leave the chat, and provides a list of RMI-enabled users on demand.
*   **Inactivity Timeout:** The server automatically disconnects users after a configurable period of inactivity to maintain resources.
*   **Session Survival:** Users can issue a command (`>SUR`) to prevent being disconnected due to inactivity.
*   **Message History:** Newly connected users receive the last 10 messages from the public chat.
*   **Dynamic RMI Discovery:** The server acts as a lookup service, enabling clients to find the RMI connection details of other users for private messaging.

## How It Works

The system is composed of a central server and multiple clients that interact with each other.

### Architecture

1.  **Server (`servidorChat.java`)**: This is the main entry point for the chat server. It listens for incoming TCP connections on a specified port. For each new connection, it spins up a dedicated `UtilizadorHandler` thread.

2.  **Client Handler (`UtilizadorHandler.java`)**: Running on the server, this class manages the entire lifecycle of a single client's connection. It handles:
    *   Reading public messages from the client and broadcasting them to all other clients.
    *   Processing special commands (e.g., `@lista`, `>SUR`).
    *   Managing user session state, including inactivity timeouts.
    *   Acting as a lookup directory for RMI details (IP, port, public key) to facilitate private messaging.

3.  **Client (`agenteUser.java`)**: This is the client-side application. It fulfills two roles:
    *   **TCP Client**: It connects to the `servidorChat` to send and receive public messages.
    *   **RMI Server**: It can optionally create its own RMI registry. This allows other clients to connect to it directly to deliver private or secure messages.

### Communication Flow

#### Public Chat
- A client sends a message to the server via its TCP socket.
- The corresponding `UtilizadorHandler` on the server receives the message.
- The handler broadcasts the message to all other connected clients.

#### Private Messaging (RMI)
1.  User A wants to message User B privately for the first time (`/userB <message>`).
2.  User A's client sends a lookup request (`<PEDIDO_RMI> userB`) to the server.
3.  The server finds User B's RMI details (IP and port) and sends them back to User A.
4.  User A's client uses these details to look up User B's RMI remote object and invokes the `sendMessage` method directly.
5.  Subsequent messages between A and B can use the cached RMI connection.

#### Secure Private Messaging (RSA Signatures)
1.  A client opting for secure messaging generates an RSA key pair upon startup and sends its public key to the server.
2.  When User A sends a secure private message to User B, its client also requests User B's public key from the server.
3.  User A's client signs the message content with its **private key**.
4.  It then calls the `sendMessageSecure` method on User B's RMI object, passing the message and the digital signature.
5.  User B's client receives the message and uses User A's **public key** to verify the signature. A successful verification confirms that the message was indeed sent by User A and was not tampered with.

## Getting Started

### Prerequisites
*   Java Development Kit (JDK) installed and configured.

### Compilation
Navigate to the project's root directory and compile all the Java source files.
```sh
javac *.java
```

### Running the Application

#### 1. Start the Server
Run the `servidorChat` class. You will be prompted to enter a port number for the server to listen on and a timeout duration for inactive clients.
```sh
java servidorChat
```
*   **Port:** Enter a port number (e.g., `2222`). Use `0` for the default port (`2222`).
*   **Timeout:** Enter the inactivity timeout in seconds. Use `0` for the default (`120` seconds).

#### 2. Start a Client
In a new terminal, run the `agenteUser` class. You will be prompted for several configuration details.
```sh
java agenteUser
```
Follow the on-screen prompts:
1.  **Nickname:** Your desired username for the chat.
2.  **Receive private messages? (y/n):** Enter `y` to enable the RMI server for private messaging.
3.  **Encrypt private messages? (y/n):** (Only if you answered `y` above) Enter `y` to generate an RSA key pair for sending and verifying signed messages.
4.  **Server IP:** The IP address of the machine running the server. Use `0` for `localhost`.
5.  **Your IP:** The IP address other clients will use to connect to you for RMI. This should be your local network IP if others on the network are connecting, or `localhost` if running clients on the same machine.
6.  **Server Port:** The port the chat server is running on.
7.  **RMI Port:** (Only if RMI is enabled) The port for your local RMI registry. Use `0` for the default (`1099`).

## Chat Commands

*   `Hello everyone!` - Sends a public message to all users.
*   `/username <message>` - Sends a private message to a specific user (e.g., `/diogo Hello there!`).
*   `@lista` - Displays a list of all users who have enabled private messaging (RMI).
*   `>SUR` - Toggles the inactivity timeout for your session. When active, you will not be disconnected for being idle.
*   `*EXIT*` - Disconnects you from the chat server.
