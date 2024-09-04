package dev.aerospike.ticketfaster.controller;

import org.springframework.stereotype.Controller;

@Controller
public class LiveShowController_Evan {
/*
    private final LiveShowService liveShowService;

    // Constructor injection of the LiveShowService
    public LiveShowController(LiveShowService liveShowService) {
        this.liveShowService = liveShowService;
    }

    @GetMapping("/")
    public String redirectToLiveShowList() {
        return "redirect:/live-shows";
    }
    

    @GetMapping("/live-shows")
    public String listLiveShows(Model model) {
        List<LiveShow> liveShows = liveShowService.findAll();
        Map<String, List<LiveShow>> liveShowsByArtist = liveShows.stream()
                .collect(Collectors.groupingBy(LiveShow::getArtist));
        model.addAttribute("liveShowsByArtist", liveShowsByArtist);
        return "pages/live-show-list";
    }

    @GetMapping("/live-shows/{id}")
    public String viewLiveShow(@PathVariable Long id, Model model) {
        LiveShow liveShow = liveShowService.findLiveShowById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LiveShow not found"));
        List<Section> sectionsWithSeats = liveShowService.findSectionsWithSeatsByLiveShowId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sections not found for LiveShow"));

        model.addAttribute("liveShow", liveShow);
        model.addAttribute("sectionsWithSeats", sectionsWithSeats);
        return "pages/live-show"; 
    }

    // get Section (seat?) status. Push?
*/
}