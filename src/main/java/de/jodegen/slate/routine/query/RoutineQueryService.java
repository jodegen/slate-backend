package de.jodegen.slate.routine.query;

import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.routine.RoutineRepository;
import de.jodegen.slate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutineQueryService {

    private final RoutineRepository routineRepository;
    private final RoutineLogMapper mapper;

    @Transactional(readOnly = true)
    public List<RoutineLogView> findAll(User user) {
        return routineRepository.findByUserOrderByDateDesc(user)
                .stream()
                .map(mapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoutineLogView findByDate(User user, LocalDate date) {
        return routineRepository.findByUserAndDate(user, date)
                .map(mapper::toView)
                .orElseThrow(() -> new ResourceNotFoundException("No routine log for " + date));
    }
}
