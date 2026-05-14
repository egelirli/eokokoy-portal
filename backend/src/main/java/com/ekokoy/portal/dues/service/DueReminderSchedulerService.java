package com.ekokoy.portal.dues.service;

import com.ekokoy.portal.dues.entity.Due;
import com.ekokoy.portal.dues.repository.DueRepository;
import com.ekokoy.portal.user.entity.PropertyUser;
import com.ekokoy.portal.user.entity.RelationType;
import com.ekokoy.portal.user.repository.PropertyUserRepository;
import com.ekokoy.portal.user.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DueReminderSchedulerService {

    private final DueRepository dueRepository;
    private final PropertyUserRepository propertyUserRepository;
    private final EmailService emailService;

    public DueReminderSchedulerService(DueRepository dueRepository,
                                       PropertyUserRepository propertyUserRepository,
                                       EmailService emailService) {
        this.dueRepository = dueRepository;
        this.propertyUserRepository = propertyUserRepository;
        this.emailService = emailService;
    }

    /** Her gün 09:00'da aidat hatırlatmalarını gönderir. */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendReminders() {
        LocalDate today = LocalDate.now();

        // 3 gün önce: vade tarihi = bugün + 3
        List<Due> upcoming = dueRepository.findByDueDateAndStatusUnpaid(today.plusDays(3));
        for (Due due : upcoming) {
            notifyOwners(due, ReminderType.UPCOMING_3_DAYS);
        }

        // Vade günü
        List<Due> dueToday = dueRepository.findByDueDateAndStatusUnpaid(today);
        for (Due due : dueToday) {
            notifyOwners(due, ReminderType.DUE_TODAY);
        }

        // 7 gün geçmiş: vade tarihi = bugün - 7
        List<Due> overdue7 = dueRepository.findByDueDateAndStatusUnpaid(today.minusDays(7));
        for (Due due : overdue7) {
            notifyOwnersAndAdmin(due);
        }
    }

    private void notifyOwners(Due due, ReminderType type) {
        List<PropertyUser> owners = propertyUserRepository.findActiveByPropertyId(due.getProperty().getId())
                .stream().filter(pu -> pu.getRelationType() == RelationType.ev_sahibi).toList();
        for (PropertyUser pu : owners) {
            emailService.sendDueReminderEmail(
                    pu.getUser().getEmail(),
                    pu.getUser().getFirstName(),
                    due.getProperty().getNumber(),
                    due.getPeriodYear(),
                    due.getPeriodMonth(),
                    due.getAmount(),
                    due.getPaidAmount(),
                    due.getDueDate(),
                    type.label
            );
        }
    }

    private void notifyOwnersAndAdmin(Due due) {
        notifyOwners(due, ReminderType.OVERDUE_7_DAYS);
    }

    private enum ReminderType {
        UPCOMING_3_DAYS("3 gün içinde vadesi dolacak"),
        DUE_TODAY("bugün vadesi dolan"),
        OVERDUE_7_DAYS("7 gün önce vadesi geçmiş");

        final String label;
        ReminderType(String label) { this.label = label; }
    }
}
