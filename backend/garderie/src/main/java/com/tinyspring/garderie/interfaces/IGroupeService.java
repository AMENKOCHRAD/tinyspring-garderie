package com.tinyspring.garderie.interfaces;

import com.tinyspring.garderie.entity.Groupe;
import com.tinyspring.garderie.dto.ChildMatchingRequest;
import com.tinyspring.garderie.dto.GroupeSuggestionDTO;
import java.util.List;

public interface IGroupeService {
    List<GroupeSuggestionDTO> suggestGroups(ChildMatchingRequest request);
    Groupe addGroupe(Groupe groupe);
    Groupe updateGroupe(Groupe groupe);
    void deleteGroupe(Long id);
    Groupe getGroupeById(Long id);
    List<Groupe> getAllGroupes();
}
