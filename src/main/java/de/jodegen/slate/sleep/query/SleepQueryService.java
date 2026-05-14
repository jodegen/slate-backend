package de.jodegen.slate.sleep.query;

import de.jodegen.slate.sleep.SleepRepository;
import de.jodegen.slate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SleepQueryService {

    private final SleepRepository sleepRepository;
    private final SleepLogMapper mapper;

    @Transactional(readOnly = true)
    public List<SleepLogView> findAll(User user) {
        return sleepRepository.findByUserOrderByDateDesc(user)
                .stream()
                .map(mapper::toView)
                .toList();
    }
}
