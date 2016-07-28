# rocket-chat-notifier

Fork of the [rocket-chat-notifier](https://github.com/baloise/rocket-chat-notifier) plugin
by [baloise](https://github.com/baloise).

## Installation

Clone the project and build it:

    git clone https://github.com/chkal/rocket-chat-notifier.git
    cd rocket-chat-notifier && mvn install -DskipTests
    
Now copy the resulting HPI file to the Jenkins plugins directory:

    cp cp target/rocket-chat-notifier.hpi $JENKINS_HOME/plugins

Restart Jenkins!

## Jenkins configuration

Visit the Jenkins configuration page and enter all the required
configuration parameters.

A special note regarding the URL configuration parameter. You will have to enter
the API URL here. For the Rocket.Chat demo server it would be:

    https://demo.rocket.chat/api/
    
## Job configuration

Nothing to do. :)
