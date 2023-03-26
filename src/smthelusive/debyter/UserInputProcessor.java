package smthelusive.debyter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smthelusive.debyter.domain.UserCommand;
import smthelusive.debyter.domain.UserCommandType;

import java.util.ArrayList;
import java.util.Scanner;

public class UserInputProcessor extends Thread {
    private final Scanner sc = new Scanner(System.in);
    private boolean processingOn = true;

    private final ArrayList<UserInputListener> userInputListeners = new ArrayList<>();
    public void addListener(UserInputListener listener) {
        userInputListeners.add(listener);
    }

    private static final Logger logger = LoggerFactory.getLogger(UserInputProcessor.class);
    public void finishProcessing() {
        this.processingOn = false;
    }


    @Override
    public void run() {
        while (processingOn) {
            String[] command = sc.nextLine().split("\\s");
            switch (command[0]) {
                case "bp", "breakpoint":
                    if (command.length < 4) {
                        logger.error("please specify class, method and code index to set a breakpoint");
                    } else {
                        userInputListeners.forEach(userInputListener ->
                                userInputListener.newUserCommandReceived(new UserCommand(UserCommandType.BREAKPOINT,
                                        new String[]{command[1], command[2], command[3]})));
                    }
                    break;
                case "rm", "remove":
                    if (command.length < 4) {
                        logger.error("please specify class, method and code index to remove a breakpoint");
                    } else {
                        userInputListeners.forEach(userInputListener ->
                                userInputListener.newUserCommandReceived(new UserCommand(UserCommandType.REMOVE,
                                        new String[]{command[1], command[2], command[3]})));
                    }
                    break;
                case "step":
                    logger.info("step over");
                    notifyListenersOfSimpleUserCommandNoParams(UserCommandType.STEP_OVER);
                    break;
                case "resume", "run":
                    logger.info("resuming");
                    notifyListenersOfSimpleUserCommandNoParams(UserCommandType.RESUME);
                    break;
                case "exit":
                    logger.info("exiting debugger. the debuggee application will continue execution");
                    notifyListenersOfSimpleUserCommandNoParams(UserCommandType.EXIT);
                    break;
                case "clear":
                    logger.info("clearing breakpoints");
                    notifyListenersOfSimpleUserCommandNoParams(UserCommandType.CLEAR);
                    break;
                case "stop", "terminate":
                    logger.info("terminating the debuggee app");
                    notifyListenersOfSimpleUserCommandNoParams(UserCommandType.STOP_APP);
                    break;
                case "suspend", "pause":
                    logger.info("suspending execution of the debuggee app");
                    notifyListenersOfSimpleUserCommandNoParams(UserCommandType.SUSPEND);
                    break;
            }
        }
    }

    private void notifyListenersOfSimpleUserCommandNoParams(UserCommandType type) {
        userInputListeners.forEach(userInputListener ->
                userInputListener.newUserCommandReceived(new UserCommand(type, new String[0])));
    }
}
