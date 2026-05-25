package core.scheduler;

import core.jpa.JPAServise;
import core.jpa.Task;
import core.jpa.User;
import core.notification.NotificationBot;
import dto.EventListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@Slf4j
public class SummaryScheduler {

    private final JPAServise jpaServise;
    private final GoogleEventFetchService googleEventFetchService;
    private final NotificationBot notificationBot;
    private final ReminderDedupService dedupService;
    private final NotificationFormatter formatter;

    public SummaryScheduler(
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
    public void sendSummaries() {
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

            ZoneId zoneId;

            try {
                zoneId = ZoneId.of(user.getTimeZone());
            } catch (Exception e) {
                log.warn("[SUMMARY] invalid timezone userId={}", user.getId());
                continue;
            }

            ZonedDateTime now = ZonedDateTime.now(zoneId);

            if (now.getHour() != 8 || now.getMinute() >= 5) {
                continue;
            }

            if (now.getDayOfWeek() == DayOfWeek.MONDAY) {
                sendWeeklySummary(user, now);
            } else {
                sendDailySummary(user, now);
            }
        }
    }

    private void sendWeeklySummary(User user, ZonedDateTime now) {
        LocalDate weekStart = now.toLocalDate();
        LocalDate weekEnd = weekStart.plusDays(6);

        List<EventListItemDto> events = googleEventFetchService
                .getEventsForUserBetween(user, weekStart, weekEnd);

        List<Task> tasks = jpaServise.findTasksByUser(user);

        String message = formatter.weeklySummary(weekStart, weekEnd, events, tasks);

        notificationBot.notifyBot(user.getChatId(), message);
    }

    private void sendDailySummary(User user, ZonedDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate weekLimit = today.plusDays(7);

        List<EventListItemDto> events = googleEventFetchService
                .getEventsForUserBetween(user, today, today);

        List<Task> tasks = jpaServise.findTasksByUser(user)
                .stream()
                .filter(t -> t.getDeadline() != null)
                .filter(t -> {
                    LocalDate deadlineDate = t.getDeadline()
                            .atZoneSameInstant(now.getZone())
                            .toLocalDate();

                    return !deadlineDate.isBefore(today)
                            && !deadlineDate.isAfter(weekLimit);
                })
                .toList();

        if (events.isEmpty() && tasks.isEmpty()) {
            return;
        }

        String message = formatter.dailySummary(today, events, tasks);

        notificationBot.notifyBot(user.getChatId(), message);
    }
}