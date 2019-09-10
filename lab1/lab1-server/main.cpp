#include <iostream>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <string>

// man socket
// man getnameinfo
// man memset

// telnet [ip] [port]

int main(int argv, char* args[]) {

    /// Create a socket

    /// AF_INET is an address family that is used to designate the type of addresses
    /// that your socket can communicate with (in this case, Internet Protocol v4 addresses).
    /// When you create a socket, you have to specify its address family,
    /// and then you can only use addresses of that type with the socket.
    int listening = socket(AF_INET, SOCK_STREAM, 0);
    if (listening == -1) {
        std::cerr << "Cannot create socket";
        return -1;
    }

    /// Bind the socket to IP / port
    sockaddr_in hint;
    hint.sin_family = AF_INET;
    hint.sin_port = htons(54001);
    inet_pton(AF_INET, "0.0.0.0", &hint.sin_addr);  // 127.0.0.1

    if (bind(listening, (struct sockaddr*) &hint, sizeof(hint)) < 0) {
        std::cerr << "Cannot bind to IP/port";
        return -2;
    }

    /// Mark the socket for listening in
    if (listen(listening, SOMAXCONN) == -1) {
        std::cerr << "Cannot listen";
        return -3;
    }

    /// Accept the call
    sockaddr_in client{};
    socklen_t clientSize = sizeof(client);
    char host[NI_MAXHOST];
    char svc[NI_MAXSERV];

    int clientSocket = accept(listening, (sockaddr*) &client, &clientSize);
    if (clientSocket == -1) {
        std::cerr << "There is a problem with client connection";
        return -4;
    }

    /// Close the listening socket
    close(listening);

    memset(host, 0, NI_MAXHOST);
    memset(svc, 0, NI_MAXSERV);

    int result = getnameinfo((sockaddr*) &client,
                             sizeof(client),
                             host,
                             NI_MAXHOST,
                             svc,
                             NI_MAXSERV,
                             0);

    if (result) {
        std::cout << host << " connected on " << svc <<  std::endl;
    } else {
        inet_ntop(AF_INET, &client.sin_addr, host, NI_MAXHOST);
        std::cout << host << " connected on " << ntohs(client.sin_port) <<  std::endl;
    }



    /// While receiving - display messages, echo message
    char buff[4096];
    while (true) {
        // Clear the buffer
        memset(buff, 0, 4096);
        // Wait for a message
        int bytesRecv = recv(clientSocket, buff, 4096, 0);
        if (bytesRecv == -1) {
            std::cerr << "There was a connection issue";
            break;
        }
        if (bytesRecv == 0) {
            std::cout << "The client disconnected" <<  std::endl;
            break;
        }

        // Display message
        std::cout << "Received: " <<  std::string(buff, 0, bytesRecv) <<  std::endl;

        // Resend message
        send(clientSocket, buff, bytesRecv + 1, 0);
    }

    /// Close socket
    close(clientSocket);

    return 0;
}