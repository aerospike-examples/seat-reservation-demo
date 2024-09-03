package com.aerospike.demos.seat_reservation_demo.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.BatchResults;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.Txn;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.CTX;
import com.aerospike.client.cdt.ListOrder;
import com.aerospike.client.cdt.ListPolicy;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.client.cdt.ListWriteFlags;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.cdt.MapWriteFlags;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Exp.Type;
import com.aerospike.client.exp.ExpOperation;
import com.aerospike.client.exp.ExpReadFlags;
import com.aerospike.client.exp.ExpWriteFlags;
import com.aerospike.client.exp.ListExp;
import com.aerospike.client.exp.MapExp;
import com.aerospike.client.policy.BatchDeletePolicy;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.demos.seat_reservation_demo.model.Customer;
import com.aerospike.demos.seat_reservation_demo.model.Event;
import com.aerospike.demos.seat_reservation_demo.model.Seat;
import com.aerospike.demos.seat_reservation_demo.model.SeatStatus;
import com.aerospike.demos.seat_reservation_demo.model.Section;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart.Status;
import com.aerospike.demos.seat_reservation_demo.model.Venue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class AerospikeService {
    private static final String HOSTNAME = "localhost";
    public static final String NAMESPACE = "test";
    
    public static final String CUSTOMER_SET = "customer";
    public static final String EVENT_SET = "event";
    public static final String VENUE_SET = "venue";
    public static final String EVENT_SEAT_SET = "eventSeats";
    public static final String SHOPPING_CART_SET = "cart";
    
    private IAerospikeClient client;
    
    @PostConstruct
    private void connectToDatabase() {
        this.client = new AerospikeClient(HOSTNAME, 3000);
    }
    
    @PreDestroy
    private void cleanup() {
        if (this.client != null) {
            this.client.close();
        }
    }
    
    // Convenience methods for mapping to/from database. Might replace with java object mapper
    // once MRT is supported
    public static long toAerospike(Date date) {
        return date == null ? 0L : date.getTime();
    }
    
    public static Date fromAerospike(long date) {
        return date == 0 ? null : new Date(date);
    }
    
    public void resetAll() {
        client.truncate(null, NAMESPACE, CUSTOMER_SET, null);
        client.truncate(null, NAMESPACE, EVENT_SET, null);
        client.truncate(null, NAMESPACE, EVENT_SEAT_SET, null);
        client.truncate(null, NAMESPACE, SHOPPING_CART_SET, null);
        client.truncate(null, NAMESPACE, VENUE_SET, null);
    }

    public void save(WritePolicy wp, Customer customer, Txn txn) {
        if (wp == null && txn != null) {
            wp = client.copyWritePolicyDefault();
            wp.txn = txn;
        }
        wp.sendKey = true;
        Key key = new Key(NAMESPACE, CUSTOMER_SET, customer.getId());
        client.put(wp, key, 
                new Bin("dob", toAerospike(customer.getDateOfBirth())),
                new Bin("firstName", customer.getFirstName()),
                new Bin("lastName", customer.getLastName())
            );
    }
    

    public void save(WritePolicy wp, Event event, Txn txn) {
        if (wp == null) {
            wp = client.copyWritePolicyDefault();
            wp.txn = txn;
        }
        wp.sendKey = true;
        Key key = new Key(NAMESPACE, EVENT_SET, event.getId());
        client.put(wp, key,
                new Bin("artist", event.getArtist()),
                new Bin("category", event.getCategory()),
                new Bin("date", toAerospike(event.getDate())),
                new Bin("id", event.getId()),
                new Bin("subCat", event.getSubCategory()),
                new Bin("title", event.getTitle()),
                new Bin("url", event.getUrl()),
                new Bin("venue", event.getVenue() == null ? "" : event.getVenue().getId())
            );
    }
    
    private Optional<Event> eventFromRecord(Record thisRecord, boolean loadVenue) {
        if (thisRecord == null) {
            return Optional.empty();
        }
        else {
            Event result = Event.builder()
                    .artist(thisRecord.getString("artist"))
                    .category(thisRecord.getString("category"))
                    .date(fromAerospike(thisRecord.getLong("date")))
                    .subCategory(thisRecord.getString("subCat"))
                    .title(thisRecord.getString("title"))
                    .url(thisRecord.getString("url"))
                    .id(thisRecord.getString("id"))
                    .build();
            
            if (loadVenue) {
                String venueId = thisRecord.getString("venue");
                if (venueId != null) {
                    result.setVenue(this.readVenue(venueId).get());
                }
            }
            return Optional.of(result);
        }
    }
    
    public Optional<Event> readEvent(String id) {
        Record thisRecord = client.get(null, new Key(NAMESPACE, EVENT_SET, id));
        return eventFromRecord(thisRecord, true);
    }

    public List<Event> getEventsInDateRange(Date startDate, Date endDate) {
        Statement stmt = new Statement();
        stmt.setNamespace(NAMESPACE);
        stmt.setSetName(EVENT_SET);
        stmt.setFilter(Filter.range("date", toAerospike(startDate), toAerospike(endDate)));
        QueryPolicy qp = new QueryPolicy();
        RecordSet recordSet = client.query(qp, stmt);
        List<Event> results = new ArrayList<>();
        while (recordSet.next()) {
            Record thisRecord = recordSet.getRecord();
            Optional<Event> thisEvent = eventFromRecord(thisRecord, true); 
            if (thisEvent.isPresent()) {
                results.add(thisEvent.get());
            }
        }
        results.sort((a,b) -> (int)(a.getDate().getTime() - b.getDate().getTime()));
        return results;
    }
    
    private Key getRowOfSeatsKey(String eventId, int section, int row) {
        return new Key(NAMESPACE, EVENT_SEAT_SET, eventId + "|" + section + "|"+ row);
    }
    
    /**
     * Remove all the seat reservations for an event. This only removes the seats, no other
     * @param event
     */
    public void clearSeatStatusesForEvent(Event event) {
        // TODO: The MRT is throwing an exception here if used, not sure why. I suspect it's because
        // of the RECORD_NOT_FOUND on some of the records?
        List<Key> keys = new ArrayList<>();
        List<Section> sections = event.getVenue().getSeatMap();
        for (Section thisSection : sections) {
            int rowsThisSection = thisSection.getNumRows();
            for (int i = 0; i < rowsThisSection; i++) {
                keys.add(getRowOfSeatsKey(event.getId(), thisSection.getSectionId(), i));
            }
        }
        BatchPolicy batchPolicy = client.copyBatchPolicyDefault();
//        batchPolicy.txn = new Txn();
        batchPolicy.sendKey = true;
        BatchDeletePolicy batchDeletePolicy = client.getBatchDeletePolicyDefault();
        batchDeletePolicy.durableDelete = true;
        try {
            BatchResults results = client.delete(batchPolicy, batchDeletePolicy, keys.toArray(new Key[0]));
            for (int i = 0; i < keys.size(); i++) {
                switch (results.records[i].resultCode) {
                case ResultCode.OK:
                case ResultCode.KEY_NOT_FOUND_ERROR:
                    break;
                default:
                    String error = String.format("Result code of %s (%d) unexpected on key %s during clearing of seat statuses\n,",
                            ResultCode.getResultString(results.records[i].resultCode), results.records[i].resultCode, keys.get(i));
                    throw new AerospikeException(error);
                }
            }
//            client.commit(batchPolicy.txn);
        }
        catch (RuntimeException e) {
//            client.abort(batchPolicy.txn);
            throw e;
        }
    }
    
    /**
     * Get the status of the seats for an event. The result will be an array (sections) with an array (number of rows in the venue),
     * and each item in the array will be an array of the seat statuses in that row. The seat statuses will be downcast
     * to a byte for more efficient storage
     * <p>
     * For for a small theater with one section, 4 rows and 5 seats per row, the result must be something like:
     * <pre>
     * 0: 0 0 1 2 0
     * 1: 2 2 2 2 0
     * 2: 1 1 1 2 2
     * 3: 0 0 0 0 0
     * </pre>
     * Here, there are 3 vacant seats in the first one, one reserved and not purchased and one purchased.
     * See {@code SeatStatus} for a list of possible statuses. 
     * @param event
     * @return
     */
    public byte[][][] getEventSeatStatus(Event event) {
        if (event.getVenue() == null) {
            throw new IllegalArgumentException("Event must be associated with a venue");
        }
        List<Section> sections = event.getVenue().getSeatMap();
        byte[][][] seatStatuses = new byte[sections.size()][][];
        
        int totalKeys = event.getVenue().calcNumRowsOfSeatsInVenue();
        Key[] keys = new Key[totalKeys];
        int keyNum = 0;
        for (Section thisSection : sections) {
            int sectionId = thisSection.getSectionId();
            if (sectionId < 0 || sectionId >= sections.size()) {
                throw new IllegalStateException(String.format("Section id must be in the range 0->%d, but found %d", sections.size(), sectionId));
            }
            
            int rows = thisSection.getNumRows();
            for (int i = 0; i < rows; i++) {
                keys[keyNum++] = getRowOfSeatsKey(event.getId(), sectionId, i);
            }
        }
        Record[] records = client.get(null, keys);
        keyNum = 0;
        for (Section thisSection : sections) {
            int sectionId = thisSection.getSectionId();
            int rows = thisSection.getNumRows();
            int seatsPerRow = thisSection.getSeatsPerRow();
            seatStatuses[sectionId] = new byte[rows][];
            for (int row = 0; row < rows; row++) {
                seatStatuses[sectionId][row] = new byte[seatsPerRow];
                Record thisRecord = records[keyNum++];
                if (thisRecord != null) {
                    Map<Long, Object> seatMap = (Map<Long, Object>) thisRecord.getMap("seats");
                    if (seatMap != null) {
                        for (long thisSeat : seatMap.keySet()) {
                            List<Object> seatData = (List<Object>) seatMap.get(thisSeat);
                            int seatStatus = ((Long)seatData.get(0)).intValue();
                            seatStatuses[sectionId][row][(int)thisSeat] = (byte)seatStatus;
                        }
                    }
                }
            }
        }
        return seatStatuses;
    }
    
    /**
     * Update the status of a seat for a customer. Seats must follow the lifecycle of 
     * AVAILABLE -> RESERVED -> PURCHASED [-> AVAILABLE] or AVAILABLE -> RESERVED -> AVAILABLE
     * <p>
     * Transitions out of RESERVED and PURCHASED statuses require the passed user to be the owner of that seat. 
     * @param event
     * @param custId
     * @param row
     * @param seatNumber
     * @param newStatus
     * @param txn
     */
    public void setSeatStatus(String eventId, String custId, int sectionId, int row, int seatNumber, SeatStatus newStatus, Txn txn) {
        final String SEAT_BIN = "seats";
        
        Map<Integer, Object> emptyMap = new HashMap<>();
        MapPolicy mp = new MapPolicy(MapOrder.KEY_ORDERED, MapWriteFlags.DEFAULT);
        ListPolicy lp = new ListPolicy(ListOrder.UNORDERED, ListWriteFlags.DEFAULT);
        
        List<Object> seatData = new ArrayList<>();
        seatData.add(newStatus.getValue());
        seatData.add(custId);
        
        // Seats are stored per row in a sub-ordinate object
        Key key = getRowOfSeatsKey(eventId, sectionId, row);
        Exp mapExists = Exp.binExists(SEAT_BIN);
        Exp seatExists = MapExp.getByKey(MapReturnType.EXISTS, Type.BOOL, Exp.val(seatNumber), Exp.mapBin(SEAT_BIN));
        
        // This logic assumes the seat exists
        Exp seatReplacementLogic = Exp.let(
                Exp.def("seat", MapExp.getByKey(MapReturnType.VALUE, Type.LIST, Exp.val(seatNumber), Exp.mapBin(SEAT_BIN))),
                Exp.def("status", ListExp.getByIndex(ListReturnType.VALUE, Type.INT, Exp.val(0), Exp.var("seat"))),
                Exp.def("customer", ListExp.getByIndex(ListReturnType.VALUE, Type.STRING, Exp.val(1), Exp.var("seat"))),
                Exp.cond(
                        // Resetting the seat back to AVAILABLE from the purchaser in state of RESERVED or PURCHASED
                        Exp.and(
                                // New status is to be set to available
                                Exp.eq(Exp.val(newStatus.getValue()), Exp.val(SeatStatus.AVAILABLE.getValue())),
                                // AND the current status is RESERVED or PURCHASED
                                Exp.or(
                                        Exp.eq(Exp.var("status"), Exp.val(SeatStatus.RESERVED.getValue())),
                                        Exp.eq(Exp.var("status"), Exp.val(SeatStatus.PURCHASED.getValue()))
                                ),
                                // AND the current owner of the seat is the same as the customer
                                Exp.eq(Exp.var("customer"), Exp.val(custId))
                        ),
                        MapExp.removeByKey(Exp.val(seatNumber), Exp.mapBin(SEAT_BIN)),
                        
                        // Setting status to RESERVED from AVAILBLE (not existing)
                        Exp.and(
                                // New status is to be set to reserved
                                Exp.eq(Exp.val(newStatus.getValue()), Exp.val(SeatStatus.RESERVED.getValue())),
                                Exp.not(seatExists)
                        ),
                        MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.mapBin(SEAT_BIN)),
                        
                        // Setting the status to PURCHASED from RESERVED with the same ID
                        Exp.and(
                                // New status is to be set to PURCHASED
                                Exp.eq(Exp.val(newStatus.getValue()), Exp.val(SeatStatus.PURCHASED.getValue())),
                                // AND the existing status is RESERVED
                                Exp.eq(Exp.var("status"), Exp.val(SeatStatus.RESERVED.getValue())),
                                // AND the customer is the same customer id
                                Exp.eq(Exp.var("customer"), Exp.val(custId))
                        ),
//                        MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.mapBin(SEAT_BIN)),
                        ListExp.set(lp, Exp.val(0), Exp.val(SeatStatus.PURCHASED.getValue()), Exp.mapBin(SEAT_BIN), CTX.mapKey(Value.get(seatNumber))),
                        
                        // Else it's an error
                        Exp.unknown()
                )
            );
//        Exp seatReplacementLogic = Exp.cond(Exp.eq(Exp.val(newStatus.getValue()), Exp.val(SeatStatus.AVAILABLE.getValue())), 
//                MapExp.removeByKey(Exp.val(seatNumber), Exp.mapBin(SEAT_BIN)),
//                MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.mapBin(SEAT_BIN))
//        );
        Exp createNewSeatInNewMap = MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.val(emptyMap));
        Exp createNewSeatInExistingMap = MapExp.put(mp, Exp.val(seatNumber), Exp.val(seatData), Exp.mapBin(SEAT_BIN));
        
        // If the seat exists, write seat data, otherwise create a new seat. 
        Exp seatLogic = Exp.cond(seatExists, seatReplacementLogic, createNewSeatInExistingMap);
        
        // If the map exists, check the seat logic, otherwise, create a new map with the seat data.
        Exp checkSeatExists = Exp.cond(mapExists, seatLogic, createNewSeatInNewMap);
        
        WritePolicy wp = client.copyWritePolicyDefault();
        wp.sendKey = true;
        wp.txn = txn;
        client.operate(wp, key, 
                ExpOperation.write("seats", Exp.build(checkSeatExists), ExpWriteFlags.DEFAULT)
            );
    }

    /**
     * Save a venue. The key here will be the same as the name so we will not use sendKey = true
     * @param wp - Write policy to use. Can be null
     * @param venue - Venue to save. Should not be null
     * @param txn - Txn id. Can be null. If passed, then txn on the write policy will be set to this value
     */
    public void save(WritePolicy wp, Venue venue, Txn txn) {
        if (wp == null && txn != null) {
            wp = client.copyWritePolicyDefault();
            wp.txn = txn;
        }
        Key key = new Key(NAMESPACE, VENUE_SET, venue.getId());
        List<Map<String, Object>> sectionMap = new ArrayList<>();
        for (Section section : venue.getSeatMap()) {
            Map<String, Object> map = Map.of(
                    "name", section.getName(),  
                    "numRows", section.getNumRows(),
                    "seatsPerRow", section.getSeatsPerRow(),
                    "id", section.getSectionId());
            sectionMap.add(map);
        }
        client.put(wp, key, 
                new Bin("address", venue.getAddress()),
                new Bin("city", venue.getCity()),
                new Bin("country", venue.getCountry()),
                new Bin("desc", venue.getDescription()),
                new Bin("name", venue.getName()),
                new Bin("postCode", venue.getPostalCode()),
                new Bin("seatMap", sectionMap)
            );
    }
    
    private int asInt(Object obj) {
        Number number = (Number)obj;
        return number.intValue();
    }
    
    /**
     * Read the venue details, including the seat map
     * @param id
     * @return
     */
    public Optional<Venue> readVenue(String id) {
        Record thisRecord = client.get(null, new Key(NAMESPACE, VENUE_SET, id));
        if (thisRecord == null) {
            return Optional.empty();
        }
        else {
            Venue result = new Venue();
            result.setAddress(thisRecord.getString("address"));
            result.setCity(thisRecord.getString("city"));
            result.setCountry(thisRecord.getString("country"));
            result.setDescription(thisRecord.getString("desc"));
            result.setName(thisRecord.getString("name"));
            result.setId(id);
            result.setPostalCode(thisRecord.getString("postCode"));
            List<Map<String, Object>> sectionMaps = (List<Map<String, Object>>) thisRecord.getList("seatMap");
            for (Map<String, Object> thisSectionMap : sectionMaps) {
                Section section = Section.builder()
                        .name((String)thisSectionMap.get("name"))
                        .numRows(asInt(thisSectionMap.get("numRows")))
                        .seatsPerRow(asInt(thisSectionMap.get("seatsPerRow")))
                        .sectionId(asInt(thisSectionMap.get("id")))
                        .build();
                result.getSeatMap().add(section);
            }
            return Optional.of(result);
        }
    }
    
    /**
     * Save a booking. 
     * @param wp
     * @param shoppingCart
     * @param txn
     */
    public void save(WritePolicy wp, ShoppingCart shoppingCart, Txn txn) {
        if (wp == null && txn != null) {
            wp = client.copyWritePolicyDefault();
            wp.txn = txn;
        }
        Key key = new Key(NAMESPACE, SHOPPING_CART_SET, shoppingCart.getId());
        List<String> seats = new ArrayList<>(shoppingCart.getSeats().size());
        for (Seat thisSeat: shoppingCart.getSeats()) {
            seats.add(thisSeat.getSectionId() + "-" + thisSeat.getRow() + "-" + thisSeat.getSeatNumber());
        }
        client.put(wp, key, 
                new Bin("created", toAerospike(shoppingCart.getCreated())),
                new Bin("custId", shoppingCart.getCustId()),
                new Bin("eventId", shoppingCart.getEventId()),
                new Bin("status", shoppingCart.getStatus().toString()),
                new Bin("seats", seats)
            );
    }
    
    public Optional<ShoppingCart> readCart(String cartId) {
        Record thisRecord = client.get(null, new Key(NAMESPACE, SHOPPING_CART_SET, cartId));
        if (thisRecord == null) {
            return Optional.empty();
        }
        else {
            ShoppingCart result = new ShoppingCart();
            result.setCreated(fromAerospike(thisRecord.getLong("created")));
            result.setCustId(thisRecord.getLong("custId"));
            result.setEventId(thisRecord.getString("eventId"));
            result.setStatus(Status.valueOf(thisRecord.getString("status")));
            result.setId(cartId);
            List<String> seatStrings = (List<String>) thisRecord.getList("seats");
            for (String thisSeatString : seatStrings) {
                result.getSeats().add(Seat.fromString(thisSeatString));
            }
            return Optional.of(result);
        }
    }
    
    public void commitTxn(Txn txn) {
        this.client.commit(txn);
    }
    
    public void rollbackTxn(Txn txn) {
        this.client.abort(txn);
    }
}
