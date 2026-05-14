package de.jodegen.slate.weight.query;

import de.jodegen.slate.user.User;
import de.jodegen.slate.weight.WeightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeightQueryService {

    private final WeightRepository weightRepository;
    private final WeightEntryMapper mapper;

    @Transactional(readOnly = true)
    public List<WeightEntryView> findAll(User user) {
        return weightRepository.findByUserOrderByDateDesc(user)
                .stream()
                .map(mapper::toView)
                .toList();
    }
}
