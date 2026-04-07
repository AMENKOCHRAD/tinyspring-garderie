package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;
import com.tinyspring.garderie.entity.Events.DailyMenu;
import com.tinyspring.garderie.entity.Events.Dish;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mappeer.DishMapper;
import com.tinyspring.garderie.repository.Events.DailyMenuRepository;
import com.tinyspring.garderie.repository.Events.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DishServiceImpl implements DishService {

    private final DishRepository dishRepository;
    private final DailyMenuRepository dailyMenuRepository;
    private final DishMapper dishMapper;

    @Override
    public DishResponse create(DishRequest request) {
        if (request.getDailyMenuId() == null) {
            throw new IllegalArgumentException("dailyMenuId est obligatoire pour la création standalone d'un plat");
        }

        DailyMenu dailyMenu = getDailyMenuEntity(request.getDailyMenuId());
        Dish dish = dishMapper.toEntity(request);
        dish.setDailyMenu(dailyMenu);

        return dishMapper.toResponse(dishRepository.save(dish));
    }

    @Override
    public DishResponse update(Long id, DishRequest request) {
        Dish dish = getEntity(id);
        dishMapper.updateEntityFromRequest(request, dish);

        if (request.getDailyMenuId() != null) {
            DailyMenu dailyMenu = getDailyMenuEntity(request.getDailyMenuId());
            dish.setDailyMenu(dailyMenu);
        }

        return dishMapper.toResponse(dishRepository.save(dish));
    }

    @Override
    public void delete(Long id) {
        Dish dish = getEntity(id);
        dishRepository.delete(dish);
    }

    @Override
    public DishResponse uploadPhoto(Long id, MultipartFile file) {
        Dish dish = getEntity(id);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier image est obligatoire");
        }

        try {
            Path uploadDirectory = Path.of("uploads", "dishes");
            Files.createDirectories(uploadDirectory);

            String originalName = file.getOriginalFilename() == null ? "dish-image" : file.getOriginalFilename();
            String extension = "";
            int extensionIndex = originalName.lastIndexOf('.');
            if (extensionIndex >= 0) {
                extension = originalName.substring(extensionIndex);
            }

            String filename = UUID.randomUUID() + extension;
            Path destination = uploadDirectory.resolve(filename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            dish.setPhotoUrl("/uploads/dishes/" + filename);
            return dishMapper.toResponse(dishRepository.save(dish));
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible d'enregistrer l'image du plat", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DishResponse> getByDailyMenuId(Long dailyMenuId) {
        return dishRepository.findByDailyMenu_IdOrderByMealTypeAscNameAsc(dailyMenuId)
                .stream()
                .map(dishMapper::toResponse)
                .toList();
    }

    private Dish getEntity(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plat introuvable avec l'id : " + id));
    }

    private DailyMenu getDailyMenuEntity(Long dailyMenuId) {
        return dailyMenuRepository.findById(dailyMenuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu journalier introuvable avec l'id : " + dailyMenuId));
    }
}