package com.tourism.platform.service.impl;

import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelerSummaryDto;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;

    @Override
    @Transactional(readOnly = true)
    public TravelerSummaryDto getTravelerSummary(Long travelerId) {
        User user = userRepository.findById(travelerId)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found with ID: " + travelerId));

        String bio = user.getBio() == null ? "" : user.getBio().trim();
        if (bio.length() > 160) {
            bio = bio.substring(0, 160) + "...";
        }

        return TravelerSummaryDto.builder()
                .userId(user.getId())
                .displayName((user.getFirstName() + " " + user.getLastName()).trim())
                .bioShort(bio)
                .profileImageUrl(user.getProfileImageUrl())
                .interests("travel, culture")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TravelConnectionDto> getAcceptedConnectionsByTravelPlan(Long travelPlanId) {
        if (!travelPlanRepository.existsById(travelPlanId)) {
            throw new ResourceNotFoundException("Travel plan not found with ID: " + travelPlanId);
        }

        return userRepository.findAll().stream().limit(3).map(user -> TravelConnectionDto.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .status("ACCEPTED")
                        .build())
                .toList();
    }
}
