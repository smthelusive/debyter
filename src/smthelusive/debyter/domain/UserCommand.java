package smthelusive.debyter.domain;

public record UserCommand(UserCommandType userCommandType, String[] params) {}
