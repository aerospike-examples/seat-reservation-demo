package com.aerospike.demos.seat_reservation_demo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.aerospike.demos.seat_reservation_demo.model.Event;
import com.aerospike.demos.seat_reservation_demo.model.Section;
import com.aerospike.demos.seat_reservation_demo.services.EventService;

@Controller
@RequestMapping("/")
public class HomeController {
    
    @Autowired
    private EventService eventService;
    
    @GetMapping
    public String index(Model model) {
        return "index";
    }
    
    @GetMapping("/live-shows")
    public String listLiveShows(Model model) {
        List<Event> liveShows = eventService.getAllEvents();
        Map<String, List<Event>> liveShowsByArtist = liveShows.stream()
                .collect(Collectors.groupingBy(Event::getArtist));
        model.addAttribute("liveShowsByArtist", liveShowsByArtist);
        return "pages/live-show-list";
    }

    @GetMapping("/live-shows/{id}")
    public String viewLiveShow(@PathVariable String id, Model model) {
        Event liveShow = eventService.loadEvent(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LiveShow not found"));
        List<Section> sections = liveShow.getVenue().getSeatMap();

        model.addAttribute("liveShow", liveShow);
        model.addAttribute("sectionsWithSeats", sections);
        return "pages/live-show"; 
    }
}
