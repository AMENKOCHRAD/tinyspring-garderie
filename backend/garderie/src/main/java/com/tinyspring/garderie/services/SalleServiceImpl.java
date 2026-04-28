package com.tinyspring.garderie.services;

import com.tinyspring.garderie.interfaces.ISalleService;
import org.springframework.stereotype.Service;
import com.tinyspring.garderie.entity.Salle;
import com.tinyspring.garderie.repository.SalleRepository;

import java.util.List;

@Service
public class SalleServiceImpl implements ISalleService {

    private final SalleRepository salleRepository;

    public SalleServiceImpl(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    @Override
    public Salle addSalle(Salle salle) {
        return salleRepository.save(salle);
    }

    @Override
    public Salle updateSalle(Salle salle) {
        return salleRepository.save(salle);
    }

    @Override
    public void deleteSalle(Long id) {
        salleRepository.deleteById(id);
    }

    @Override
    public Salle getSalleById(Long id) {
        return salleRepository.findById(id).orElse(null);
    }

    @Override
    public List<Salle> getAllSalles() {
        return salleRepository.findAll();
    }
}
