package core.scheduler;

import core.jpa.JPAServise;
import core.jpa.Task;
import core.jpa.User;
import core.notification.NotificationBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@Slf4j
public class TaskReminderScheduler {

    private final JPAServise jpaServise;
    private final NotificationBot notificationBot;
    private final ReminderDedupService dedupService;
    private final NotificationFormatter formatter;

    public TaskReminderScheduler(
            JPAServise jpaServise,
            NotificationBot notificationBot,
            ReminderDedupService dedupService,
            NotificationFormatter formatter
    ) {
        this.jpaServise = jpaServise;
        this.notificationBot = notificationBot;
        this.dedupService = dedupService;
        this.formatter = formatter;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void sendTaskReminders() {
        for (User user : jpaServise.findAllUsers()) {
            if (user.getTimeZone() == null || user.getTimeZone().isBlank()) {
                String key = "reminder:user:no_timezone:" + user.getId();

                if (!dedupService.alreadySent(key)) {
                    notificationBot.notifyBot(user.getChatId(),
                            "⚠️ У тебя не установлена timezone.\n\n" +
                                    "Из-за этого напоминания и сводки могут работать некорректно.\n" +
                                    "Установи timezone командой /setTimezone");

                    dedupService.markSentForDay(key);
                }

                continue;
            }

            ZoneId zoneId = ZoneId.of(user.getTimeZone());
            ZonedDateTime now = ZonedDateTime.now(zoneId);

            ZonedDateTime from = now.plusHours(1).minusMinutes(5);
            ZonedDateTime to = now.plusHours(1).plusMinutes(5);

            for (Task task : jpaServise.findTasksByUser(user)) {
                if (task.getDeadline() == null) {
                    continue;
                }

                ZonedDateTime deadline = task.getDeadline().atZoneSameInstant(zoneId);

                if (deadline.isBefore(from) || deadline.isAfter(to)) {
                    continue;
                }

                String key = "reminder:task:hour:" + task.getId();

                if (dedupService.alreadySent(key)) {
                    continue;
                }

                notificationBot.notifyBot(
                        user.getChatId(),
                        formatter.taskOneHourReminder(task, deadline)
                );

                dedupService.markSent(key);
            }
        }
    }
}