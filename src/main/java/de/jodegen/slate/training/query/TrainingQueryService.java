package de.jodegen.slate.training.query;

import de.jodegen.slate.common.exception.ResourceNotFoundException;
import de.jodegen.slate.training.TrainingDayRepository;
import de.jodegen.slate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingQueryService {

    private final TrainingDayRepository trainingDayRepository;
    private final TrainingDayMapper trainingDayMapper;

    @Transactional(readOnly = true)
    public List<TrainingDaySummaryView> findAll(User user) {
        return trainingDayMapper.toSummaryViewList(
                trainingDayRepository.findByUserOrderByDateDesc(user)
        );
    }

    @Transactional(readOnly = true)
    public TrainingDayView findByDate(User user, LocalDate date) {
        return trainingDayRepository.findByUserAndDate(user, date)
                .map(trainingDayMapper::toView)
                .orElseThrow(() -> new ResourceNotFoundException("No training day for " + date));
    }
}
