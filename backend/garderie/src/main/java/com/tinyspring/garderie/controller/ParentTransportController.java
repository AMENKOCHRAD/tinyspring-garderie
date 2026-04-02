package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.transport.CreateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.parent.ParentDemandeTransportForm;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.Trajet;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.TrajetRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.TransportService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/parent")
public class ParentTransportController {

    private static final String MOCK_PARENT_EMAIL = "parent@garderie.com";

    private final TransportService transportService;
    private final TrajetRepository trajetRepository;
    private final EnfantRepository enfantRepository;
    private final UserRepository userRepository;

    public ParentTransportController(TransportService transportService,
                                     TrajetRepository trajetRepository,
                                     EnfantRepository enfantRepository,
                                     UserRepository userRepository) {
        this.transportService = transportService;
        this.trajetRepository = trajetRepository;
        this.enfantRepository = enfantRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/trajets")
    public String listTrajets(Model model) {
        User parent = getMockParent();
        model.addAttribute("parent", parent);
        model.addAttribute("trajets", trajetRepository.findAll().stream()
                .filter(trajet -> trajet.getDateTrajet() != null && !trajet.getDateTrajet().isBefore(LocalDate.now().plusDays(1)))
                .toList());
        return "parent/trajets";
    }

    @GetMapping("/demande")
    public String demandeForm(@RequestParam(required = false) Long trajetId, Model model) {
        User parent = getMockParent();

        ParentDemandeTransportForm form = new ParentDemandeTransportForm();
        form.setTrajetId(trajetId);

        model.addAttribute("parent", parent);
        model.addAttribute("demandeForm", form);
        addFormLists(model, parent);
        return "parent/demande";
    }

    @PostMapping("/demande")
    public String submitDemande(@Valid @ModelAttribute("demandeForm") ParentDemandeTransportForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User parent = getMockParent();

        if (bindingResult.hasErrors()) {
            model.addAttribute("parent", parent);
            addFormLists(model, parent);
            return "parent/demande";
        }

        CreateDemandeTransportRequest request = new CreateDemandeTransportRequest();
        request.setEnfantId(form.getEnfantId());
        request.setTrajetId(form.getTrajetId());
        request.setPointRamassage(form.getPointRamassage());

        transportService.creerDemandeTransport(parent.getId(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Votre demande de transport a ete enregistree.");
        return "redirect:/parent/demandes";
    }

    @GetMapping("/demandes")
    public String listDemandes(Model model) {
        User parent = getMockParent();
        model.addAttribute("parent", parent);
        model.addAttribute("demandes", transportService.listerDemandesParParent(parent.getId()));
        return "parent/demandes";
    }

    private void addFormLists(Model model, User parent) {
        List<Enfant> enfants = enfantRepository.findByParentId(parent.getId());
        List<Trajet> trajets = trajetRepository.findAll().stream()
                .filter(trajet -> trajet.getDateTrajet() != null && !trajet.getDateTrajet().isBefore(LocalDate.now().plusDays(1)))
                .toList();
        model.addAttribute("enfants", enfants);
        model.addAttribute("trajets", trajets);
    }

    private User getMockParent() {
        return userRepository.findByEmail(MOCK_PARENT_EMAIL)
                .orElseThrow(() -> new IllegalStateException("Parent de test introuvable"));
    }
}
