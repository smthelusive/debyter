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
                case "bp":
                    if (command.length < 4) {
                        logger.error("please specify class, method and code index to set a breakpoint");
                    } else {
                        userInputListeners.forEach(userInputListener ->
                                userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.BREAKPOINT,
                                        new String[]{command[1], command[2], command[3]})));
                    }
                    break;
                case "step":
                    logger.info("step over...");
                    userInputListeners.forEach(userInputListener ->
                            userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.STEP_OVER, new String[0])));
                case "resume":
                    logger.info("resuming...");
                    userInputListeners.forEach(userInputListener ->
                            userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.RESUME, new String[0])));
                    break;
                case "exit":
                    logger.info("exiting...");
                    userInputListeners.forEach(userInputListener ->
                        userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.EXIT, new String[0])));
                    break;
                case "clear":
                    logger.info("clearing breakpoints...");
                    userInputListeners.forEach(userInputListener ->
                            userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.CLEAR, new String[0])));
                    break;
            }
        }
    }
}
