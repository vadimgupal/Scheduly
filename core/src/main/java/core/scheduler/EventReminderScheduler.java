package core.scheduler;

import core.jpa.JPAServise;
import core.jpa.User;
import core.notification.NotificationBot;
import dto.EventListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@Slf4j
public class EventReminderScheduler {

    private final JPAServise jpaServise;
    private final GoogleEventFetchService googleEventFetchService;
    private final NotificationBot notificationBot;
    private final ReminderDedupService dedupService;
    private final NotificationFormatter formatter;

    public EventReminderScheduler(
            JPAServise jpaServise,
            GoogleEventFetchService googleEventFetchService,
            NotificationBot notificationBot,
            ReminderDedupService dedupService,
            NotificationFormatter formatter
    ) {
        this.jpaServise = jpaServise;
        this.googleEventFetchService = googleEventFetchService;
        this.notificationBot = notificationBot;
        this.dedupService = dedupService;
        this.formatter = formatter;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void sendEventReminders() {
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

            if (user.getDefaultCalendarId() == null || user.getDefaultCalendarId().isBlank()) {
                String key = "reminder:event:no_default_calendar:" + user.getId();

                if (!dedupService.alreadySent(key)) {
                    notificationBot.notifyBot(user.getChatId(),
                            "⚠️ Напоминания о событиях не работают, потому что календарь по умолчанию не установлен.\n\n" +
                                    "Установи его командой /setDefaultCalendar");

                    dedupService.markSentForDay(key);
                }

                continue;
            }

            ZoneId zoneId = ZoneId.of(user.getTimeZone());
            ZonedDateTime now = ZonedDateTime.now(zoneId);

            ZonedDateTime from = now.minusMinutes(5);
            ZonedDateTime to = now.plusMinutes(5);

            List<EventListItemDto> events =
                    googleEventFetchService.getEventsForUserBetween(
                            user,
                            now.toLocalDate(),
                            now.plusDays(1).toLocalDate()
                    );

            for (EventListItemDto event : events) {
                log.info("[EVENT_REMINDER] check eventId={} name={} start={} end={} reminder={}",
                        event.id(),
                        event.name(),
                        event.start(),
                        event.end(),
                        event.reminderMinutesBefore());

                if (event.start() == null) {
                    log.info("[EVENT_REMINDER] skip eventId={} reason=no_start", event.id());
                    continue;
                }

                ZonedDateTime eventStart = event.start().atZone(zoneId);

                log.info("[EVENT_REMINDER] eventId={} eventStart={} now={} customReminderTime={}",
                        event.id(),
                        eventStart,
                        now,
                        event.reminderMinutesBefore() == null
                                ? null
                                : eventStart.minusMinutes(event.reminderMinutesBefore()));

                checkAndSend(
                        user,
                        event,
                        eventStart.minusHours(1),
                        from,
                        to,
                        "hour",
                        formatter.eventOneHourReminder(event)
                );

                if (event.reminderMinutesBefore() != null) {
                    checkAndSend(
                            user,
                            event,
                            eventStart.minusMinutes(event.reminderMinutesBefore()),
                            from,
                            to,
                            "custom:" + event.reminderMinutesBefore(),
                            formatter.eventCustomReminder(event)
                    );
                }
            }
        }
    }

    private void checkAndSend(
            User user,
            EventListItemDto event,
            ZonedDateTime reminderTime,
            ZonedDateTime from,
            ZonedDateTime to,
            String type,
            String message
    ) {
        log.info("[EVENT_REMINDER] checkAndSend eventId={} type={} reminderTime={} from={} to={}",
                event.id(), type, reminderTime, from, to);

        if (reminderTime.isBefore(from) || reminderTime.isAfter(to)) {
            log.info("[EVENT_REMINDER] skip eventId={} type={} reason=outside_window",
                    event.id(), type);
            return;
        }

        String key = "reminder:event:" + type + ":" + user.getId() + ":" + event.id();

        if (dedupService.alreadySent(key)) {
            log.info("[EVENT_REMINDER] skip eventId={} type={} reason=already_sent key={}",
                    event.id(), type, key);
            return;
        }
        log.info("[EVENT_REMINDER] sending eventId={} type={} chatId={}",
                event.id(), type, user.getChatId());

        notificationBot.notifyBot(user.getChatId(), message);
        dedupService.markSent(key);
    }
}