package de.jodegen.slate.routine.command;

import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.routine.RoutineLog;
import de.jodegen.slate.routine.RoutineRepository;
import de.jodegen.slate.routine.query.RoutineLogMapper;
import de.jodegen.slate.routine.query.RoutineLogView;
import de.jodegen.slate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class RoutineCommandService {

    private final RoutineRepository routineRepository;
    private final RoutineLogMapper mapper;

    @Transactional
    public RoutineLogView addItem(User user, LocalDate date, AddRoutineItemCommand cmd) {
        RoutineLog log = findOrCreate(user, date);
        if (!log.getCompletedItems().contains(cmd.item())) {
            log.getCompletedItems().add(cmd.item());
        }
        return mapper.toView(routineRepository.save(log));
    }

    @Transactional
    public RoutineLogView removeItem(User user, LocalDate date, RemoveRoutineItemCommand cmd) {
        RoutineLog log = findOrCreate(user, date);
        log.getCompletedItems().remove(cmd.item());
        return mapper.toView(routineRepository.save(log));
    }

    @Transactional
    public RoutineLogView setItems(User user, LocalDate date, SetRoutineItemsCommand cmd) {
        RoutineLog log = findOrCreate(user, date);
        log.setCompletedItems(new ArrayList<>(cmd.items()));
        return mapper.toView(routineRepository.save(log));
    }

    @Transactional
    public void deleteLog(User user, LocalDate date) {
        RoutineLog log = routineRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new ResourceNotFoundException("No routine log for " + date));
        routineRepository.delete(log);
    }

    private RoutineLog findOrCreate(User user, LocalDate date) {
        return routineRepository.findByUserAndDate(user, date)
                .orElseGet(() -> RoutineLog.builder()
                        .user(user)
                        .date(date)
                        .completedItems(new ArrayList<>())
                        .build());
    }
}
