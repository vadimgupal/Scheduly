package bot.commands.google.calendar.action;

import bot.commands.CommandHandler;
import bot.dto.UserMessage;
import org.springframework.stereotype.Component;

@Component
public class UpdateDefaultCalendarCommand implements CommandHandler {

    private final SetDefaultCalendarCommand setDefaultCalendarCommand;

    public UpdateDefaultCalendarCommand(SetDefaultCalendarCommand setDefaultCalendarCommand) {
        this.setDefaultCalendarCommand = setDefaultCalendarCommand;
    }

    @Override
    public String command() {
        return "updateDefaultCalendar";
    }

    @Override
    public String name() {
        return "Update default calendar";
    }

    @Override
    public void handle(UserMessage msg) {
        setDefaultCalendarCommand.handle(msg);
    }
}