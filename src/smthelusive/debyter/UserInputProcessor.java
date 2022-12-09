package smthelusive.debyter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smthelusive.debyter.domain.UserCommand;
import smthelusive.debyter.domain.UserCommandType;

import java.util.Scanner;

public class UserInputProcessor extends Thread {
    private final Scanner sc = new Scanner(System.in);
    private boolean processingOn = true;

    private final UserInputListener userInputListener;
    public UserInputProcessor(UserInputListener userInputListener) {
        this.userInputListener = userInputListener;
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
                    if (command.length < 3) {
                        logger.error("please specify method name and code index to set a breakpoint");
                    } else {
                        userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.BREAKPOINT,
                                new String[]{command[1], command[2]}));
                    }
                    break;
                case "step":
                    logger.info("step over...");
                    userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.STEP_OVER, new String[0]));
                case "resume":
                    logger.info("resuming...");
                    userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.RESUME, new String[0]));
                    break;
                case "exit":
                    logger.info("exiting...");
                    userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.EXIT, new String[0]));
                    break;
                case "clear":
                    logger.info("clearing breakpoints...");
                    userInputListener.addEventToTheQueue(new UserCommand(UserCommandType.CLEAR, new String[0]));
            }
        }
    }
}
