# WordQuizzle

The main functions of the WordQuizzle service, the implementation choices and the communication protocol used will be described below.

## Protocol

To efficiently manage the communication between Client and Server, it was established a well-defined communication protocol, described in the classes *Message* and *Connection*.

### Message and Connection

Each message exchanged between Client and Server use functions read and write of the *Connection class*, the protocol defined is exchange the length of the message first and then the content. The messages exchanged are defined in the *Message* class and each of these consists of two fields: the *type* and the *payload*. The *type* is described by integer codes representing the request that you want to make and the answers that you can receive, these codes are contained within the *Message* class. The *payload* is the actually exchanged data, it can be *Integer*, *String* or *byte[]*. The *Message* class implements *Serializable*, as messages come serialized before sending, thanks to the *getByteFromObject* function and deserialized on arrival with *getObjectFromByte* function.

## Server

The server has been implemented through a *Selector*, to allow the distribution among various channels ready and not.

### Main behavior

When the Server is started, the content of the word dictionary is read (which will be used later in the challenge), the shared data structures are initialized, the *RMI stub* and the listening *ServerSocketChannel* are created. At connection time, each Client is accepted and placed in read mode. When a request arrives, this, with the relative attachment of the Client and the *SelectionKey*, are passed to a Thread of the *ThreadPool* that will handle the request, set the channel in write mode and notify the selector. In writing mode the selector will take care of writing the reply message to the Client.

### Shared Data structures

The Server make use of data structures and shared objects within threads:
* *writeGsonLock*: *ReentrantLock* to allow editing of the file *infoserver.json* in mutual exclusion;
* *users*: *ConcurrentHashMap* with key as username and UDP port number of the relative Client as value. It is changed at login and logout, to allow the Server a quick access what are online and non-online users;
* *fighting*: *ConcurrentHashMap* with key an object of type *ValueOne* and value an object of type *ValueHashmap*. It is used at the beginning of a challenge, we will see them in detail later;

### Client's attachment

Before moving on to describe *WorkerThread* working flow, we must talk about the Client's attachment. Each Client is "equipped" at connection time of a *ClientState* object that contains the following fields:
* *requestMessage*: request made by the Client;
* *responseMessage*: response that will be saved by the *WorkerThread*;
* *user*: object of type *User*, contains various information about the client, if it is online or not, whether in challenge or not;
* *usernameSfida*: indicates the name of the challenged Client, it is initialized at time of a challenge request;
* *words*: ArrayList of words to be translated during the challenge;
* *translatedWords*: ArrayList of translated words to verify correctness;
* *iterWords*: integer to scroll through the array of words and to manage the timer;
* *challengePoint*: integer to count points made by a Client;
* *correct*: ArrayList of two positions to count the correct words and not of a Client;
* *timer*: object of type Timer, to stop the challenge at the moment of running out of time;
In the *User* class we find information related to the name of the Client, to his online status and challenge status. As for some objects presented before, we will see in challenge section how they will come in handy.

### Worker Thread

Each time a message is read, the Server passes the "work" to be done to a Thread of the ThreadPool. The *WorkerThread* implements the *WordQuizzleInterface* interface, which contains all the functions the Server can perform. When the Thread start, the *infoserver.json* file is read and saved in an ArrayList *info*, the request message is splitted, by parsing the code of the *Message*. Fundamental is the beginning of the run method, here it is checked if the Client is in a shared ConcurrentHashMap and if it is not already in challenge. Let's talk about the methods:
* *requestnotvalid()*: called if the message sent by Client consists of a request not supported by the Server, a invalid request message;
* *savePortNumber()*: first message exchanged between Client and Server, used to save the number of the its UDP port where eventually the challenge request will be sent;
* *login(user nick, password)*: allows a user to login, check if it is contained in *info*, if it is not already online via the class *User* of the attachment and if the password is correct. If all went fine, return a *LOGINOK* message, otherwise return an error message depending on the type, wrong password, user not
registered or user already online;
* *logout(nickUser)*: allow a user to log out, check if the user is contained in *info*, if he was not already offline. If everything went successful, return a *LOGOUTOK* message, otherwise an error message, user not registered or user not online;
* *addiamico(usernameUser, nickAmico)*: allow you to add *nickAmico* to friends' list of *nickUser* and vice versa. Check that both are contained in *info* with the check support function, if all went well, friendship is added on both sides and the JSON file is rewritten, otherwise return an error message;
* *list_of_friends(user_nick)*: returns the list of friends of *user_nick* in JSON format after checking that *nickUser* is registered and online;
* *show_score(user_nick)*: return the total score of *user_nick*, retrieve the value from *info* and return it after the various checks;
* *show_ranking(username)*: return the ranking with the scores in descending order of *nickUser* and his entire list of friends;
* *translateWords(words)*: translate the words contained in the *words* array which will be used in the challenge;
* *updateDB(user, points)*: method used at the end of a challenge to update user's scores within the *infoserver.json* file;


### Challenge

Regarding the challenge, the *WorkerThread* use three methods to handle it: the *challenge(user_nick, friend_nick)* method, called by *nick_user* when he wants to start a challenge against *nickAmico*, the *sfidaduring* method to manage the transfer of words during the challenge, and finally *checkFinished*, to synchronize the end of the challenge for both. When *nick_user* start a challenge, after the various registration checks, login, and if *nickAmico* is online, a Thread is started which will take care of sending the UDP request to the Client. If no response is given within 10 seconds, the challenge is considered rejected. If, on the other hand, the response is of type 0, then the Client refused the challenge. If 2, then *nickAmico* is already engaged in a challenge and we must wait. If 1 instead, the challenge has been accepted and we can start the initialization of the various parameters. This is where the hashmap *fighting* comes in. The  challenging Client adds his name as a key and the name of the challenged as a value, along with other informations about the challenge, as well as the list of words to be translated and those translated. This will be needed by the challenged Client at the time of the request to start the challenge. Indeed, as mentioned before, at the beginning of the run method is checked if the name of the currently active Client is in the hashmap, if this happens, the game is also initialized for him by taking the words to be translated and those translated by the value of the hashmap. <br />
Once the challenge has started, the Server checks the validity of the translation with the *sfidaduring* method, if successful it increases the *challengePoint* value of the attachment by 2, otherwise it decreases it by 1. In the same way is the value of the *pointsSfida* parameters changed in the key or value of *fighting* depending on the user, the value at address 0 or 1 of the correct ArrayList is also increased depending on whether the answer is correct or not. An important parameter here is *iterWords* contained in the attachment. If this value is less than 7, then points are counted like said above, otherwise if it is 7 or 9, it means respectively that the words are terminated or the timer has expired. In these cases the Client checks that the other has finished, through the values contained in the *fighting* value *hasFinished* and *hasFinishedFirst*. If both of these values are true, then both have finished the challenge and the challenge report is returned, with the scored points, correct and wrong answers and the outcome of the match. If not, then a message of type *CHALLENGEWAITING* is returned to the Client. When a Client see arriving this message, then it sends requests every *x* seconds to the Server to find out if the other opponent has finished. This is checked via the *checkFinished* method, which controls the opponent's termination and in case of end returns the report, otherwise continue sending messages of type *SFIDAWAITING*. At the end of the challenge, all items used in the challenge are reset.

## Client

### Main behavior

The main classes used on Client side are the *ClientManager* and the various GUI form. First, the *ClientManager* class is instantiated, connects to the Server and exchanges the UDP port of the DatagramSocket created earlier and retrieve also the RMI stub.
The first form shown to the Client is the *StartForm*, where it can login or register. Once logged in, it comes out the *HomeForm*, where you can perform the various operations described previously on the Server. If he decides to challenge a friend, the *ChallengeFrame* comes in where he will insert the translation of the words and the possibility to send them.

### ClientManager

ClientManager class deals with the logic of the Client, it contains various methods that are called depending on the operation you want to perform, at start creates the DatagramSocket listening to challenge requests, connects to the Server and retrieves the RMI stub. The various methods and how they work are the following:
* *login(username, password)*: send a *LOGINCODE* message with *username* and *password* to the server and waits for response, if it is of type *LOGINOK* the Client username is set. Returns the code response;
* *register(username, password)*: calls the *registranuovoutente* function of the RMI stub, returns 1 on success, 0 otherwise;
* *addFriend(name)*: send a message of type *ADDFRIENDCODE* to Server and returns the code of the reply message;
* *showFriends()*: send a message with *FRIENDSLISTCODE* code to Server and returns the reply message;
* *showScore()*: send a message with the *SHOWPOINTSCODE* code to Server and returns the reply message;
* *showScoreBoard()*: sends a message with the code *SHOWSCORECODE* to Server and returns the reply message;
* *logout()*: sends a message with *LOGOUTCODE* code to Server and returns the reply message;
* *challenge(name)*: send a message with code *SFIDACODE* to Server and returns the reply message;
* *sendReceive(word)*: send the translated word to Server with code *SFIDADURING* and receives the next word or termination;
* *receiveWord()*: used in the initial challenge phase to read the first word sent by server;
* *waitFriend()*: send a message with the *WAITFRIEND* code if it has received an equal response. Used to wait for the termination of opponent's challenge;
* *reqNotValid()*: send a *REQUESTNOTVALID* message. Needed in the phase of acceptance of the challenge by the challenged one;

### UDP Thread

The UDP thread is launched upon login of a Client, it takes care of listen to any challenge requests. When a request arrives, the receive "unlocks" and an RMI stub method is called, *notificationClient*, to notify the Client of the challenge request. If the challenge is accepted, it answers to Server with 1 and adds a value to a shared queue *queue*. This *queue* is for the *HomeForm* to know if there are any pending challenge requests, and so to start them. If the challenge is rejected the answer to Server will be 0, if the Client is already engaged in a challenge it will be 2.

### GUI

The Client chooses the operations to be done through three GUIs that exchange themselves depending on Client's "state".

#### StartForm

StartForm is the GUI shown at startup, there are two text fields, one for the username and one for the password, and two buttons, one for login and one for registration. In case of login, the *login()* method is called contained in the *ClientManager*, in case of registration, the function *registranuovoutente* of the stub is called, which we will discuss later. If the login was successful, the Client is sent to the *HomeForm*.

