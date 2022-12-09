package smthelusive.debyter;

import smthelusive.debyter.domain.UserCommand;

public interface UserInputListener {
//    void userRequestBreakpoint(String methodName, long codeIndex);
//    void userRequestStepOver();
//    void userRequestResume();
//    void userRequestExit();
//    void userRequestClear();
    void addEventToTheQueue(UserCommand userCommand);
}
