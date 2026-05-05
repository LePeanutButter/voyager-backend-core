package com.tourism.platform.integration.amadeus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Deterministic Amadeus-shaped mock catalog (flights v2, hotels by-city, hotel-offers v3, activities v1).
 *
 * <p>Populates 540+ records across cities and dates from {@value #MOCK_WINDOW_START} to {@value #MOCK_WINDOW_END}
 * so clients can migrate to real Amadeus without changing parsing code.
 */
@Component
public class AmadeusMockCatalogData {

    static final String MOCK_WINDOW_START = "2026-05-25";
    static final String MOCK_WINDOW_END = "2027-01-31";
    private static final int FLIGHT_OFFER_COUNT = 220;
    private static final int HOTEL_REF_COUNT = 200;
    private static final int ACTIVITY_COUNT = 140;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter AT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String K_DEPARTURE = "departure";
    private static final String K_IATA_CODE = "iataCode";
    private static final String K_ARRIVAL = "arrival";
    private static final String K_NUMBER_OF_STOPS = "numberOfStops";
    private static final String K_GEO_CODE = "geoCode";
    private static final String K_LATITUDE = "latitude";
    private static final String K_LONGITUDE = "longitude";
    private static final String K_ITINERARIES = "itineraries";
    private static final String K_SEGMENTS = "segments";
    private static final String K_DURATION = "duration";
    private static final String K_AIRCRAFT = "aircraft";
    private static final String K_PRICE = "price";
    private static final String K_TOTAL = "total";
    private static final String K_CURRENCY = "currency";
    private static final String K_HOTEL_ID = "hotelId";
    private static final String K_CHAIN_CODE = "chainCode";
    private static final String K_CITY_CODE = "cityCode";
    private static final String K_ADDRESS = "address";
    private static final String K_COUNTRY_CODE = "countryCode";

    private final ObjectMapper mapper;

    private ArrayNode allFlights;
    private ArrayNode allHotelRefs;
    private ArrayNode allActivities;
    /** hotelId -> template row for offer synthesis */
    private final Map<String, ObjectNode> hotelRefById = new HashMap<>();

    public AmadeusMockCatalogData(ObjectMapper mapper) {
        this.mapper = mapper;
        List<City> cities = cities();
        long days = ChronoUnit.DAYS.between(LocalDate.parse(MOCK_WINDOW_START), LocalDate.parse(MOCK_WINDOW_END)) + 1;
        Random rng = new Random(42L);

        allFlights = buildFlights(cities, days, rng);
        allHotelRefs = buildHotelRefs(cities, rng);
        allActivities = buildActivities(cities, rng);
    }

    /** Flight Offers Search v2-shaped response (filtered). */
    public ObjectNode searchFlightOffers(FlightOffersSearchRequest request) {
        String currencyCode = request.currencyCode();
        String cur = StringUtils.hasText(currencyCode) ? currencyCode.toUpperCase(Locale.ROOT) : "EUR";
        Integer maxOffers = request.maxOffers();
        int cap = maxOffers == null ? 50 : Math.min(250, Math.max(1, maxOffers));

        ArrayNode filtered = mapper.createArrayNode();
        String o = request.originLocationCode().toUpperCase(Locale.ROOT);
        String d = request.destinationLocationCode().toUpperCase(Locale.ROOT);
        FlightCopyArgs copyArgs =
                new FlightCopyArgs(cur, request.adults(), request.children(), request.returnDate(), d, o);

        collectPrimaryMatches(
                filtered,
                new FlightPrimarySearchCriteria(o, d, request.departureDate(), request.nonStop(), request.travelClass(), cap),
                copyArgs);
        if (filtered.isEmpty()) {
            collectFallbackMatches(filtered, o, d, request.nonStop(), cap, copyArgs);
        }

        ObjectNode root = mapper.createObjectNode();
        root.set("meta", metaNode(filtered.size()));
        root.set("data", filtered);
        root.set("dictionaries", flightDictionaries(o, d));
        return root;
    }

    private void collectPrimaryMatches(
            ArrayNode filtered, FlightPrimarySearchCriteria criteria, FlightCopyArgs copyArgs) {

        int i = 0;
        while (i < allFlights.size() && filtered.size() < criteria.cap()) {
            ObjectNode offer = (ObjectNode) allFlights.get(i++);
            if (matchesFlight(
                    offer,
                    criteria.origin(),
                    criteria.dest(),
                    criteria.departureDay(),
                    criteria.nonStop(),
                    criteria.travelClass())) {
                addFlightCopy(filtered, offer, copyArgs);
            }
        }
    }

    private record FlightPrimarySearchCriteria(
            String origin,
            String dest,
            String departureDay,
            Boolean nonStop,
            String travelClass,
            int cap) {}

    private void collectFallbackMatches(
            ArrayNode filtered, String o, String d, Boolean nonStop, int cap, FlightCopyArgs copyArgs) {

        int i = 0;
        while (i < allFlights.size() && filtered.size() < cap) {
            ObjectNode offer = (ObjectNode) allFlights.get(i++);
            tryAddFallbackOffer(filtered, offer, o, d, nonStop, copyArgs);
        }
    }

    private void tryAddFallbackOffer(
            ArrayNode filtered, ObjectNode offer, String o, String d, Boolean nonStop, FlightCopyArgs copyArgs) {

        ObjectNode seg = firstSegment(offer);
        if (seg == null) {
            return;
        }
        if (!o.equals(seg.path(K_DEPARTURE).path(K_IATA_CODE).asText())
                || !d.equals(seg.path(K_ARRIVAL).path(K_IATA_CODE).asText())) {
            return;
        }
        if (Boolean.TRUE.equals(nonStop) && seg.path(K_NUMBER_OF_STOPS).asInt(0) > 0) {
            return;
        }
        addFlightCopy(filtered, offer, copyArgs);
    }

    private void addFlightCopy(ArrayNode filtered, ObjectNode offer, FlightCopyArgs args) {
        ObjectNode copy = offer.deepCopy();
        adjustPriceForCurrency(copy, args.currency(), args.adults(), args.children() == null ? 0 : args.children());
        if (StringUtils.hasText(args.returnDate())) {
            copy.put("oneWay", false);
            appendReturnStub(copy, args.destIata(), args.originIata(), args.returnDate());
        }
        filtered.add(copy);
    }

    private record FlightCopyArgs(
            String currency, int adults, Integer children, String returnDate, String destIata, String originIata) {}

    /** GET /v1/reference-data/locations/hotels/by-city */
    public ObjectNode hotelsByCity(String cityCode) {
        String cc = cityCode.toUpperCase(Locale.ROOT);
        ArrayNode rows = mapper.createArrayNode();
        for (int i = 0; i < allHotelRefs.size(); i++) {
            ObjectNode h = (ObjectNode) allHotelRefs.get(i);
            if (cc.equals(h.path(K_IATA_CODE).asText())) {
                rows.add(h);
            }
        }
        ObjectNode root = mapper.createObjectNode();
        root.set("meta", metaNode(rows.size()));
        root.set("data", rows);
        return root;
    }

    /** GET /v3/shopping/hotel-offers (simplified but structurally compatible). */
    public ObjectNode hotelOffers(
            String hotelIdsCsv,
            String checkInDate,
            String checkOutDate,
            int adults,
            int roomQuantity,
            String currencyCode) {

        String cur = StringUtils.hasText(currencyCode) ? currencyCode.toUpperCase(Locale.ROOT) : "EUR";
        Set<String> wanted = Arrays.stream(hotelIdsCsv.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));

        ArrayNode data = mapper.createArrayNode();
        for (String hid : wanted) {
            ObjectNode ref = hotelRefById.get(hid);
            if (ref == null) {
                continue;
            }
            data.add(buildHotelOfferNode(ref, checkInDate, checkOutDate, adults, roomQuantity, cur));
        }

        ObjectNode root = mapper.createObjectNode();
        root.set("data", data);
        return root;
    }

    /** GET /v1/shopping/activities */
    public ObjectNode activities(double latitude, double longitude, double radiusKm, String radiusUnit) {
        double r = radiusKm <= 0 ? 5 : radiusKm;
        ArrayNode within = mapper.createArrayNode();
        for (int i = 0; i < allActivities.size(); i++) {
            ObjectNode act = (ObjectNode) allActivities.get(i);
            double la = act.path(K_GEO_CODE).path(K_LATITUDE).asDouble();
            double lo = act.path(K_GEO_CODE).path(K_LONGITUDE).asDouble();
            if (haversineKm(latitude, longitude, la, lo) <= r) {
                within.add(act);
            }
        }
        ObjectNode root = mapper.createObjectNode();
        root.set("data", within);
        ObjectNode meta = root.putObject("meta");
        meta.put("count", within.size());
        meta.put("radius", r);
        meta.put("radiusUnit", radiusUnit == null ? "KM" : radiusUnit);
        return root;
    }

    int totalMockRecords() {
        return allFlights.size() + allHotelRefs.size() + allActivities.size();
    }

    private static boolean matchesFlight(
            ObjectNode offer,
            String origin,
            String dest,
            String departureDay,
            Boolean nonStop,
            String travelClass) {

        ObjectNode seg = firstSegment(offer);
        if (seg == null) {
            return false;
        }
        String dep = seg.path(K_DEPARTURE).path(K_IATA_CODE).asText();
        String arr = seg.path(K_ARRIVAL).path(K_IATA_CODE).asText();
        String at = seg.path(K_DEPARTURE).path("at").asText();
        if (at.length() < 10) {
            return false;
        }
        String day = at.substring(0, 10);
        if (!origin.equals(dep) || !dest.equals(arr) || !departureDay.equals(day)) {
            return false;
        }
        if (StringUtils.hasText(travelClass) && !cabinMatchesRequestedClass(offer, travelClass)) {
            return false;
        }
        return !Boolean.TRUE.equals(nonStop) || seg.path(K_NUMBER_OF_STOPS).asInt(0) <= 0;
    }

    private static boolean cabinMatchesRequestedClass(ObjectNode offer, String travelClass) {
        String want = travelClass.trim().toUpperCase(Locale.ROOT);
        var tp = offer.path("travelerPricings");
        if (!tp.isArray() || tp.isEmpty()) {
            return true;
        }
        var fds = tp.get(0).path("fareDetailsBySegment");
        if (!fds.isArray() || fds.isEmpty()) {
            return true;
        }
        String cabin = fds.get(0).path("cabin").asText("");
        return want.equals(cabin.toUpperCase(Locale.ROOT));
    }

    private static ObjectNode firstSegment(ObjectNode offer) {
        var itin = offer.path(K_ITINERARIES);
        if (!itin.isArray() || itin.isEmpty()) {
            return null;
        }
        var segs = itin.get(0).path(K_SEGMENTS);
        if (!segs.isArray() || segs.isEmpty()) {
            return null;
        }
        return (ObjectNode) segs.get(0);
    }

    private void appendReturnStub(ObjectNode offer, String from, String to, String returnDay) {
        ArrayNode itins = (ArrayNode) offer.get(K_ITINERARIES);
        ObjectNode back = mapper.createObjectNode();
        back.put(K_DURATION, "PT3H15M");
        ArrayNode segs = mapper.createArrayNode();
        LocalDateTime dep = LocalDate.parse(returnDay, DAY).atTime(14, 30);
        LocalDateTime arr = dep.plusHours(3).plusMinutes(15);
        ObjectNode s = mapper.createObjectNode();
        s.set(K_DEPARTURE, point(from, dep.format(AT)));
        s.set(K_ARRIVAL, point(to, arr.format(AT)));
        s.put("carrierCode", "IB");
        s.put("number", "9" + (offer.path("id").asInt() % 900 + 100));
        s.set(K_AIRCRAFT, mapper.createObjectNode().put("code", "321"));
        s.put(K_DURATION, "PT3H15M");
        s.put(K_NUMBER_OF_STOPS, 0);
        segs.add(s);
        back.set(K_SEGMENTS, segs);
        itins.add(back);
    }

    private void adjustPriceForCurrency(ObjectNode offer, String currency, int adults, int children) {
        ObjectNode price = (ObjectNode) offer.get(K_PRICE);
        BigDecimal mult = BigDecimal.valueOf(adults + children * 0.75);
        BigDecimal base = new BigDecimal(price.path("base").asText("100")).multiply(mult).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = new BigDecimal(price.path(K_TOTAL).asText("150")).multiply(mult).setScale(2, RoundingMode.HALF_UP);
        if (!"EUR".equals(currency)) {
            BigDecimal fx = BigDecimal.valueOf(0.92 + (currency.hashCode() & 0x7) / 100.0);
            base = base.multiply(fx).setScale(2, RoundingMode.HALF_UP);
            total = total.multiply(fx).setScale(2, RoundingMode.HALF_UP);
        }
        price.put(K_CURRENCY, currency);
        price.put("base", base.toPlainString());
        price.put(K_TOTAL, total.toPlainString());
        price.put("grandTotal", total.toPlainString());
    }

    private ObjectNode buildHotelOfferNode(
            ObjectNode ref,
            String checkIn,
            String checkOut,
            int adults,
            int rooms,
            String currency) {

        String hid = ref.path(K_HOTEL_ID).asText();
        ObjectNode wrap = mapper.createObjectNode();
        wrap.put("type", "hotel-offers");
        ObjectNode hotel = mapper.createObjectNode();
        hotel.put("type", "hotel");
        hotel.put(K_HOTEL_ID, hid);
        hotel.put(K_CHAIN_CODE, ref.path(K_CHAIN_CODE).asText());
        hotel.put("name", ref.path("name").asText());
        hotel.put(K_CITY_CODE, ref.path(K_IATA_CODE).asText());
        hotel.set(K_ADDRESS, ref.get(K_ADDRESS));
        hotel.set(K_GEO_CODE, ref.get(K_GEO_CODE));
        wrap.set("hotel", hotel);
        wrap.put("available", true);

        ArrayNode offers = mapper.createArrayNode();
        ObjectNode off = mapper.createObjectNode();
        off.put("id", "MOCK-" + hid + "-" + checkIn);
        off.put("checkInDate", checkIn);
        off.put("checkOutDate", checkOut);
        ObjectNode price = mapper.createObjectNode();
        int nights = (int) Math.max(1, ChronoUnit.DAYS.between(LocalDate.parse(checkIn), LocalDate.parse(checkOut)));
        BigDecimal nightly = BigDecimal.valueOf(80L + (hid.hashCode() & 0xFF)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total =
                nightly.multiply(BigDecimal.valueOf((long) nights * Math.max(1, rooms))).setScale(2, RoundingMode.HALF_UP);
        if (!"EUR".equals(currency)) {
            total = total.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP);
        }
        price.put(K_CURRENCY, currency);
        price.put("base", nightly.toPlainString());
        price.put(K_TOTAL, total.toPlainString());
        off.set(K_PRICE, price);
        ObjectNode room = mapper.createObjectNode();
        room.put("type", "ROOM");
        room.set("typeEstimated", mapper.createObjectNode().put("category", "STANDARD_ROOM"));
        off.set("room", room);
        ObjectNode guests = mapper.createObjectNode();
        guests.put("adults", adults);
        off.set("guests", guests);
        offers.add(off);
        wrap.set("offers", offers);
        return wrap;
    }

    private ArrayNode buildFlights(List<City> cities, long daySpan, Random rng) {
        ArrayNode data = mapper.createArrayNode();
        LocalDate start = LocalDate.parse(MOCK_WINDOW_START);
        String[] carriers = {"IB", "BA", "AF", "LH", "KL", "UX", "VY", "AA", "UA", "AV", "LA", "AM", "CM", "TK", "EK"};
        String[] cabins = {"ECONOMY", "ECONOMY", "BUSINESS"};

        for (int i = 0; i < FLIGHT_OFFER_COUNT; i++) {
            City o = cities.get(i % cities.size());
            City dest = cities.get((i * 7 + 3) % cities.size());
            if (o.iata.equals(dest.iata)) {
                dest = cities.get((i + 1) % cities.size());
            }
            LocalDate day = start.plusDays(rng.nextInt((int) daySpan));
            int hh = 6 + rng.nextInt(14);
            int mm = rng.nextBoolean() ? 0 : 30;
            LocalDateTime dep = day.atTime(hh, mm);
            int durH = 1 + rng.nextInt(11);
            int durM = rng.nextBoolean() ? 0 : 45;
            LocalDateTime arr = dep.plusHours(durH).plusMinutes(durM);

            String carrier = carriers[rng.nextInt(carriers.length)];
            String cabin = cabins[rng.nextInt(cabins.length)];
            int stops = rng.nextInt(10) < 8 ? 0 : 1;
            String durationIso = durM == 0 ? ("PT" + durH + "H") : ("PT" + durH + "H" + durM + "M");

            ObjectNode offer = mapper.createObjectNode();
            offer.put("type", "flight-offer");
            offer.put("id", String.valueOf(10_000 + i));
            offer.put("source", "GDS");
            offer.put("instantTicketingRequired", false);
            offer.put("nonHomogeneous", false);
            offer.put("oneWay", true);
            offer.put("lastTicketingDate", day.minusDays(1).format(DAY));
            offer.put("lastTicketingDateTime", day.minusDays(1).atTime(23, 59).format(AT) + "Z");
            offer.put("numberOfBookableSeats", 3 + rng.nextInt(7));

            ObjectNode itin = mapper.createObjectNode();
            itin.put(K_DURATION, durationIso);
            ArrayNode segments = mapper.createArrayNode();
            ObjectNode seg = mapper.createObjectNode();
            seg.set(K_DEPARTURE, point(o.iata, dep.format(AT)));
            seg.set(K_ARRIVAL, point(dest.iata, arr.format(AT)));
            seg.put("carrierCode", carrier);
            seg.put("number", String.valueOf(100 + rng.nextInt(899)));
            seg.set(K_AIRCRAFT, mapper.createObjectNode().put("code", rng.nextBoolean() ? "320" : "738"));
            seg.put(K_DURATION, durationIso);
            seg.put(K_NUMBER_OF_STOPS, stops);
            segments.add(seg);
            itin.set(K_SEGMENTS, segments);

            offer.set(K_ITINERARIES, mapper.createArrayNode().add(itin));

            BigDecimal base = BigDecimal.valueOf(45 + rng.nextDouble() * 400).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = base.multiply(BigDecimal.valueOf(1.12 + rng.nextDouble() * 0.15)).setScale(2, RoundingMode.HALF_UP);
            ObjectNode price = mapper.createObjectNode();
            price.put(K_CURRENCY, "EUR");
            price.put("base", base.toPlainString());
            price.put(K_TOTAL, total.toPlainString());
            price.set("fees", mapper.createArrayNode());
            price.put("grandTotal", total.toPlainString());
            offer.set(K_PRICE, price);

            ObjectNode po = mapper.createObjectNode();
            po.set("fareType", mapper.createArrayNode().add("PUBLISHED"));
            po.put("includedCheckedBagsOnly", true);
            offer.set("pricingOptions", po);
            offer.set("validatingAirlineCodes", mapper.createArrayNode().add(carrier));

            ObjectNode tp = mapper.createObjectNode();
            tp.put("travelerId", "1");
            tp.put("fareOption", "STANDARD");
            tp.put("travelerType", "ADULT");
            ArrayNode fds = mapper.createArrayNode();
            ObjectNode fd = mapper.createObjectNode();
            fd.put("segmentId", "1");
            fd.put("cabin", cabin);
            fd.put("fareBasis", "Y26LGTN8");
            fd.set("class", mapper.createObjectNode().put("code", "Y"));
            fds.add(fd);
            tp.set("fareDetailsBySegment", fds);
            offer.set("travelerPricings", mapper.createArrayNode().add(tp));

            data.add(offer);
        }
        return data;
    }

    private ArrayNode buildHotelRefs(List<City> cities, Random rng) {
        ArrayNode data = mapper.createArrayNode();
        String[] chains = {"RT", "HI", "MC", "BW", "YX"};
        for (int i = 0; i < HOTEL_REF_COUNT; i++) {
            City c = cities.get(i % cities.size());
            String chain = chains[rng.nextInt(chains.length)];
            String hid = String.format(Locale.ROOT, "%sMOK%03d", c.iata, i % 1000);
            ObjectNode h = mapper.createObjectNode();
            h.put(K_CHAIN_CODE, chain);
            h.put(K_IATA_CODE, c.iata);
            h.put("dupeId", 700_000_000 + i);
            h.put("name", mockHotelName(c.name, i));
            h.put(K_HOTEL_ID, hid);
            ObjectNode geo = mapper.createObjectNode();
            geo.put(K_LATITUDE, c.lat + (rng.nextDouble() - 0.5) * 0.08);
            geo.put(K_LONGITUDE, c.lon + (rng.nextDouble() - 0.5) * 0.08);
            h.set(K_GEO_CODE, geo);
            ObjectNode addr = mapper.createObjectNode();
            addr.set("lines", mapper.createArrayNode().add("Mock Street " + (i % 200 + 1)));
            addr.put("cityName", c.name.toUpperCase(Locale.ROOT));
            addr.put(K_COUNTRY_CODE, c.country);
            h.set(K_ADDRESS, addr);
            data.add(h);
            hotelRefById.put(hid, h);
        }
        return data;
    }

    private ArrayNode buildActivities(List<City> cities, Random rng) {
        ArrayNode data = mapper.createArrayNode();
        String[] themes = {
            "Walking tour", "Food & market", "Museum pass", "Bike experience",
            "Sunset cruise", "Cooking class", "Wine tasting", "Street art route",
            "Kayak / nature", "Historical core", "Rooftop & skyline", "Family zoo day"
        };
        for (int i = 0; i < ACTIVITY_COUNT; i++) {
            City c = cities.get((i * 11) % cities.size());
            ObjectNode a = mapper.createObjectNode();
            a.put("id", "ACT-MOCK-" + i);
            a.put("type", "activity");
            a.put("name", themes[i % themes.length] + " — " + c.name);
            a.put("shortDescription", "Mock Amadeus-shaped activity for demos (" + c.country + ").");
            ObjectNode geo = mapper.createObjectNode();
            geo.put(K_LATITUDE, c.lat + (rng.nextDouble() - 0.5) * 0.06);
            geo.put(K_LONGITUDE, c.lon + (rng.nextDouble() - 0.5) * 0.06);
            a.set(K_GEO_CODE, geo);
            a.put("rating", String.format(Locale.ROOT, "%.1f", 3.5 + rng.nextDouble() * 1.4));
            ArrayNode pics = mapper.createArrayNode();
            pics.add("https://picsum.photos/seed/mockact" + i + "/800/600");
            a.set("pictures", pics);
            a.put("bookingLink", "https://example.com/mock-activities/" + i);
            a.put("minimumDuration", "PT" + (2 + rng.nextInt(6)) + "H");
            ObjectNode price = mapper.createObjectNode();
            price.put("currencyCode", "EUR");
            price.put("amount", String.format(Locale.ROOT, "%.2f", 15 + rng.nextDouble() * 120));
            a.set(K_PRICE, price);
            data.add(a);
        }
        return data;
    }

    private ObjectNode point(String iata, String at) {
        ObjectNode n = mapper.createObjectNode();
        n.put(K_IATA_CODE, iata);
        n.put("at", at);
        return n;
    }

    private ObjectNode metaNode(int count) {
        ObjectNode m = mapper.createObjectNode();
        m.put("count", count);
        m.put("source", "MOCK_AMADEUS_COMPAT");
        return m;
    }

    private ObjectNode flightDictionaries(String o, String d) {
        ObjectNode dict = mapper.createObjectNode();
        ObjectNode loc = mapper.createObjectNode();
        loc.set(o, mapper.createObjectNode().put(K_CITY_CODE, o).put(K_COUNTRY_CODE, "XX"));
        loc.set(d, mapper.createObjectNode().put(K_CITY_CODE, d).put(K_COUNTRY_CODE, "XX"));
        dict.set("locations", loc);
        dict.set("carriers", mapper.createObjectNode().put("IB", "IBERIA"));
        dict.set(K_AIRCRAFT, mapper.createObjectNode().put("320", "AIRBUS A320"));
        return dict;
    }

    private static String mockHotelName(String city, int i) {
        String[] adj = {"Central", "Grand", "Urban", "Riverside", "Plaza", "Garden", "Skyline"};
        return adj[i % adj.length] + " Hotel " + city;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371.0;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);
        double x = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.asin(Math.min(1.0, Math.sqrt(x)));
    }

    private static List<City> cities() {
        List<City> c = new ArrayList<>();
        c.add(new City("MAD", "Madrid", "ES", 40.4168, -3.7038));
        c.add(new City("BCN", "Barcelona", "ES", 41.3874, 2.1686));
        c.add(new City("PAR", "Paris", "FR", 48.8566, 2.3522));
        c.add(new City("LON", "London", "GB", 51.5074, -0.1278));
        c.add(new City("LIS", "Lisbon", "PT", 38.7223, -9.1393));
        c.add(new City("FCO", "Rome", "IT", 41.9028, 12.4964));
        c.add(new City("AMS", "Amsterdam", "NL", 52.3676, 4.9041));
        c.add(new City("BER", "Berlin", "DE", 52.5200, 13.4050));
        c.add(new City("VIE", "Vienna", "AT", 48.2082, 16.3738));
        c.add(new City("ZRH", "Zurich", "CH", 47.3769, 8.5417));
        c.add(new City("NYC", "New York", "US", 40.7128, -74.0060));
        c.add(new City("MIA", "Miami", "US", 25.7617, -80.1918));
        c.add(new City("BOG", "Bogota", "CO", 4.7110, -74.0721));
        c.add(new City("LIM", "Lima", "PE", -12.0464, -77.0428));
        c.add(new City("MEX", "Mexico City", "MX", 19.4326, -99.1332));
        c.add(new City("GRU", "Sao Paulo", "BR", -23.5505, -46.6333));
        c.add(new City("EZE", "Buenos Aires", "AR", -34.6037, -58.3816));
        c.add(new City("SCL", "Santiago", "CL", -33.4489, -70.6693));
        c.add(new City("NRT", "Tokyo", "JP", 35.6762, 139.6503));
        c.add(new City("ICN", "Seoul", "KR", 37.5665, 126.9780));
        c.add(new City("SIN", "Singapore", "SG", 1.3521, 103.8198));
        c.add(new City("DXB", "Dubai", "AE", 25.2048, 55.2708));
        c.add(new City("CAI", "Cairo", "EG", 30.0444, 31.2357));
        c.add(new City("JNB", "Johannesburg", "ZA", -26.2041, 28.0473));
        c.add(new City("SYD", "Sydney", "AU", -33.8688, 151.2093));
        c.add(new City("AKL", "Auckland", "NZ", -36.8485, 174.7633));
        return c;
    }

    private record City(String iata, String name, String country, double lat, double lon) {}
}
