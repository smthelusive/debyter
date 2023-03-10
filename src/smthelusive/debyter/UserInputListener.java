package smthelusive.debyter;

import smthelusive.debyter.domain.UserCommand;

public interface UserInputListener {
    void newUserCommandReceived(UserCommand userCommand);
}
