# rocket-chat-notifier

[![Build Status](https://travis-ci.org/chkal/rocket-chat-notifier.svg?branch=master)](https://travis-ci.org/chkal/rocket-chat-notifier)

Fork of the [rocket-chat-notifier](https://github.com/baloise/rocket-chat-notifier) plugin
by [baloise](https://github.com/baloise).

This plugin supports:

  * Configuration of a single Rocket.Chat server for the entire Jenkins instance
  * Send chat notification to a Rocket.Chat room if any build starts failing, is still
    failing or gets back to stable state.
  * Allows to notify all users by including `@all` in the notify message.

The plugin does NOT support:

  * Different notification settings for individual jobs

## Installation

Clone the project and build it:

    git clone https://github.com/chkal/rocket-chat-notifier.git
    cd rocket-chat-notifier
    mvn -DskipTests clean install
    
Now copy the resulting HPI file to the Jenkins plugins directory:

    cp target/rocket-chat-notifier.hpi $JENKINS_HOME/plugins

Restart Jenkins!

## Jenkins configuration

Visit the Jenkins configuration page and enter all the required
configuration parameters.

![configuration](https://i.imgur.com/86QqqEa.png)

A special note regarding the URL configuration parameter. You will have to enter
the API URL here. For the Rocket.Chat demo server it would be:

    https://demo.rocket.chat/api/
    
## Job configuration

Nothing to do. :)
